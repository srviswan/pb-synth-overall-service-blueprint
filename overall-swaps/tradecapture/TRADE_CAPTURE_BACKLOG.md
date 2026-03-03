# Trade Capture Service - Backlog

## Epic 1 - API and Validation

1. Implement `POST /v1/trades`
2. Implement `POST /v1/trades:batch`
3. Implement `GET /v1/trades/{ingestionId}`
4. Enforce required headers and correlation propagation
5. Add payload validation and business rules (`quantity > 0`, `price >= 0`)
6. Return canonical `ErrorResponse.code` values

## Epic 2 - Idempotent Ingestion

1. Create ingestion persistence model
2. Enforce unique idempotency key
3. Duplicate semantics:
   - single request duplicate -> `409`
   - batch duplicate item -> include item-level rejection and aggregate response
4. Add ingestion status transitions and timestamps

## Epic 3 - Async Dispatch and Reliability

1. Create internal queue abstraction
2. Map request to CDM `ExecutionInstruction`
3. Dispatch to lifecycle API
4. Implement retry with backoff and max attempts
5. Persist DLQ records for terminal failures
6. Add replay helper endpoint (admin scope) in later increment

## Epic 4 - Observability and Operations

1. Add Micrometer metrics counters/timers
2. Add structured logging with `X-Correlation-Id`
3. Add health/liveness/readiness endpoints
4. Add operational runbook notes

## Epic 5 - Testing and NFR

1. Unit tests for validators/mappers/status transitions
2. Integration tests for controller + persistence + dispatcher
3. Contract tests for OpenAPI behavior
4. Retry/DLQ behavior tests
5. Load test with target profile assumptions

## Sprintable Story Order (Recommended)

1. API scaffolding + validation
2. Persistence + idempotency
3. Queue + lifecycle dispatch
4. Retry + DLQ
5. Observability and hardening
6. NFR test pass and release note
