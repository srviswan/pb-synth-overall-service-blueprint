CREATE TABLE IF NOT EXISTS ingestion_record (
    ingestion_id VARCHAR(255) PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    trade_external_id VARCHAR(255) NOT NULL,
    source_system VARCHAR(255) NOT NULL,
    partition_key VARCHAR(255) NOT NULL,
    status VARCHAR(64) NOT NULL,
    request_payload_json TEXT NOT NULL,
    instruction_envelope_json TEXT,
    error_code VARCHAR(255),
    error_message VARCHAR(2048),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    correlation_id VARCHAR(255),
    user_context VARCHAR(2048)
);

CREATE TABLE IF NOT EXISTS dispatch_record (
    id VARCHAR(255) PRIMARY KEY,
    ingestion_id VARCHAR(255) NOT NULL,
    destination_id VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP WITH TIME ZONE,
    last_error VARCHAR(2048),
    sent_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_dispatch_ingestion_destination UNIQUE (ingestion_id, destination_id)
);

CREATE INDEX IF NOT EXISTS idx_dispatch_status_next_attempt
    ON dispatch_record (status, next_attempt_at);

CREATE INDEX IF NOT EXISTS idx_dispatch_ingestion_id
    ON dispatch_record (ingestion_id);

CREATE TABLE IF NOT EXISTS dlq_record (
    id VARCHAR(255) PRIMARY KEY,
    ingestion_id VARCHAR(255) NOT NULL,
    destination_id VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    failure_reason VARCHAR(2048) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
