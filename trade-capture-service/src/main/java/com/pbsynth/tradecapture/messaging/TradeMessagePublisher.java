package com.pbsynth.tradecapture.messaging;

public interface TradeMessagePublisher {
    void publish(TradeMessage message);
}
