package com.pbsynth.tradecapture.messaging.inmemory;

import com.pbsynth.tradecapture.config.MessagingProperties;
import com.pbsynth.tradecapture.messaging.PartitionedMessageConsumer;
import com.pbsynth.tradecapture.messaging.TradeMessage;
import com.pbsynth.tradecapture.messaging.TradeMessageHandler;
import com.pbsynth.tradecapture.messaging.TradeMessagePublisher;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "tradecapture.messaging.broker-type", havingValue = "inmemory", matchIfMissing = true)
public class InMemoryPartitionedBroker implements TradeMessagePublisher, PartitionedMessageConsumer {
    private static final Logger log = LoggerFactory.getLogger(InMemoryPartitionedBroker.class);

    private final TradeMessageHandler messageHandler;
    private final MessagingProperties properties;
    private final MeterRegistry meterRegistry;

    private final Map<String, BlockingQueue<TradeMessage>> queues = new ConcurrentHashMap<>();
    private final Map<String, Future<?>> workers = new ConcurrentHashMap<>();
    private ExecutorService executorService;
    private volatile boolean running;

    public InMemoryPartitionedBroker(
            TradeMessageHandler messageHandler,
            MessagingProperties properties,
            MeterRegistry meterRegistry
    ) {
        this.messageHandler = messageHandler;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void init() {
        this.executorService = Executors.newCachedThreadPool();
        Gauge.builder("trade.messaging.partitions.active", workers, Map::size).register(meterRegistry);
        Gauge.builder("trade.messaging.queue.depth", this, InMemoryPartitionedBroker::totalQueueDepth).register(meterRegistry);
        start();
    }

    @Override
    public void start() {
        this.running = true;
    }

    @Override
    public void stop() {
        this.running = false;
    }

    @PreDestroy
    public void shutdown() {
        stop();
        workers.values().forEach(f -> f.cancel(true));
        executorService.shutdownNow();
    }

    @Override
    public void publish(TradeMessage message) {
        if (!running) {
            throw new IllegalStateException("InMemoryPartitionedBroker is not running");
        }
        ensurePartition(message.partitionKey());
        BlockingQueue<TradeMessage> queue = queues.get(message.partitionKey());
        boolean offered = queue.offer(message);
        if (!offered) {
            throw new IllegalStateException("Partition queue is full for key " + message.partitionKey());
        }
    }

    public int totalQueueDepth() {
        return queues.values().stream().mapToInt(BlockingQueue::size).sum();
    }

    private void ensurePartition(String partitionKey) {
        queues.computeIfAbsent(partitionKey, key -> new LinkedBlockingQueue<>(properties.getQueueCapacityPerPartition()));
        workers.computeIfAbsent(partitionKey, key -> executorService.submit(() -> runPartitionConsumer(key)));
    }

    private void runPartitionConsumer(String partitionKey) {
        BlockingQueue<TradeMessage> queue = queues.get(partitionKey);
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                TradeMessage message = queue.poll(500, TimeUnit.MILLISECONDS);
                if (message == null) {
                    continue;
                }
                messageHandler.handle(message);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                log.error("Partition consumer failed for partitionKey={}", partitionKey, ex);
            }
        }
        log.info("Stopping partition consumer for partitionKey={}", partitionKey);
    }
}
