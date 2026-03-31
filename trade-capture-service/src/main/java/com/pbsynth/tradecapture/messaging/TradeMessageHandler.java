package com.pbsynth.tradecapture.messaging;

public interface TradeMessageHandler {
    void handle(TradeMessage message);
}
