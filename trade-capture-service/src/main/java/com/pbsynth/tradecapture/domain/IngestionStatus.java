package com.pbsynth.tradecapture.domain;

public enum IngestionStatus {
    ACCEPTED,
    QUEUED,
    DISPATCHING,
    SENT,
    PARTIALLY_SENT,
    FAILED
}
