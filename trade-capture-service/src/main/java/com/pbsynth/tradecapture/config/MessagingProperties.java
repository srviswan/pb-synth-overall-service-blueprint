package com.pbsynth.tradecapture.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tradecapture.messaging")
public class MessagingProperties {
    private String brokerType = "inmemory";
    private int partitionCountMax = 64;
    private int queueCapacityPerPartition = 10_000;
    private Solace solace = new Solace();

    public String getBrokerType() {
        return brokerType;
    }

    public void setBrokerType(String brokerType) {
        this.brokerType = brokerType;
    }

    public int getPartitionCountMax() {
        return partitionCountMax;
    }

    public void setPartitionCountMax(int partitionCountMax) {
        this.partitionCountMax = partitionCountMax;
    }

    public int getQueueCapacityPerPartition() {
        return queueCapacityPerPartition;
    }

    public void setQueueCapacityPerPartition(int queueCapacityPerPartition) {
        this.queueCapacityPerPartition = queueCapacityPerPartition;
    }

    public Solace getSolace() {
        return solace;
    }

    public void setSolace(Solace solace) {
        this.solace = solace;
    }

    public static class Solace {
        private boolean enabled;
        private String queue = "trade/capture/input";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getQueue() {
            return queue;
        }

        public void setQueue(String queue) {
            this.queue = queue;
        }
    }
}
