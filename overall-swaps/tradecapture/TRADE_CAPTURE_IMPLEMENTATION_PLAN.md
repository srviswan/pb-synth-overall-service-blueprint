# Trade Capture Service - Implementation Plan (Production-Ready v1)

## Goal

Implement `trade-capture-service` as the first production-ready service, aligned to:

- `overall-swaps/api-contracts/openapi/trade-capture-api.yaml`
- `overall-swaps/consolidated_equity_swap_service_guide.md`
- `overall-swaps/pre_build_design_artifact.md`

## Scope

- Contract-compliant API for single and batch trade intake
- Idempotent ingestion with durable status tracking
- Async dispatch to lifecycle engine with retry and DLQ
- Observability, health endpoints, and baseline load test evidence

## Delivery Phases

### Phase A - API + Validation

- Implement endpoints:
  - `POST /v1/trades`
  - `POST /v1/trades:batch`
  - `GET /v1/trades/{ingestionId}`
- Enforce headers:
  - `Idempotency-Key` (required on POST)
  - `X-Correlation-Id` (required on POST)
  - `X-User-Context` (optional on POST)
- Validate payloads:
  - contract-required fields
  - `quantity > 0`
  - `price >= 0`
  - non-blank IDs (`tradeExternalId`, `accountId`, `bookId`, `securityId`)

### Phase B - Ingestion Model + Idempotency

- Persist intake records with immutable request snapshot
- Store lifecycle status transitions:
  - `ACCEPTED -> QUEUED -> SENT_TO_LIFECYCLE -> FAILED`
- Enforce dedupe by unique idempotency key
- Add status lookup by `ingestionId`

### Phase C - Async Dispatch + Retry + DLQ

- Build async dispatcher to lifecycle API (`POST /v1/instructions`)
- Batch processing with configurable size and interval
- Retry policy: exponential backoff + max attempts
- DLQ persistence for poison records and replay tooling hooks

### Phase D - Observability + NFR

- Metrics:
  - ingestion throughput
  - validation failures
  - duplicate idempotency hits
  - batch size/latency
  - retry count
  - DLQ count
- Structured logs with correlation IDs
- Readiness/liveness/health
- Load test report for 2M/day intake profile assumptions

## Implementation Outputs

- Spring Boot service scaffold in `trade-capture-service`
- Contract + architecture + backlog + NFR docs in `overall-swaps/tradecapture`
- Automated unit and integration tests

## Definition of Done

- All three endpoints implemented and contract-tested
- Idempotency behavior deterministic for single and batch intake
- Dispatcher supports retries and DLQ with observable transitions
- Health and metrics exposed
- Test evidence documented in NFR checklist
