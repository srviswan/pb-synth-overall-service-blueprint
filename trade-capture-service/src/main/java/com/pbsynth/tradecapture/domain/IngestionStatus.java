package com.pbsynth.tradecapture.domain;

public enum IngestionStatus {
    ACCEPTED,
    QUEUED,
    SENT_TO_LIFECYCLE,
    FAILED
}
