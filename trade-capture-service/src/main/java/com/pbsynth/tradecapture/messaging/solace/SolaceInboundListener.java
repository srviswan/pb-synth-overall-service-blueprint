package com.pbsynth.tradecapture.messaging.solace;

import com.pbsynth.tradecapture.config.MessagingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "tradecapture.messaging.solace.enabled", havingValue = "true")
public class SolaceInboundListener {
    private static final Logger log = LoggerFactory.getLogger(SolaceInboundListener.class);

    public SolaceInboundListener(MessagingProperties properties) {
        log.info(
                "Solace inbound listener stub enabled for queue={}. Real adapter should convert incoming messages to TradeMessage and publish to internal broker.",
                properties.getSolace().getQueue()
        );
    }
}
