# Trade Capture Service - NFR Checklist

## Throughput and Latency

- [ ] Sustained throughput model validated for 2M trades/day profile
- [ ] `POST /v1/trades` acceptance latency benchmark captured (P95, P99)
- [ ] Batch ingestion latency benchmark captured (P95, P99)

## Reliability

- [ ] Idempotency verified under retries and duplicate submissions
- [ ] Status transitions are monotonic and auditable
- [ ] Retry policy tested against transient lifecycle failures
- [ ] DLQ path tested for poison records

## Consistency and Correctness

- [ ] Request-to-CDM mapping validated with sample fixtures
- [ ] Validation errors mapped to canonical error codes
- [ ] Correlation IDs are preserved across logs and dispatch calls

## Observability

- [ ] Metrics exposed: intake rate, validation failures, dedupe hits
- [ ] Metrics exposed: dispatch latency, retries, DLQ count
- [ ] Structured logs include `ingestionId` and `correlationId`
- [ ] Health/readiness endpoints integrated

## Security

- [ ] Required headers enforced (`Idempotency-Key`, `X-Correlation-Id`)
- [ ] Optional `X-User-Context` propagated if present
- [ ] Sensitive request fields redacted in logs

## Test Evidence

- [ ] Unit test coverage report generated
- [ ] Integration test report generated
- [ ] Contract test report generated
- [ ] Load test report generated

## Release Gate

- [ ] All critical checklist items complete
- [ ] Known issues documented with mitigation
- [ ] Operational handoff notes prepared
