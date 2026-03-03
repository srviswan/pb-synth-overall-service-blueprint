# API Contracts Module

This module is the contract-first source of truth for inter-service APIs in the equity swap platform.

## Folder Layout

- `openapi/` - OpenAPI 3.0.3 specs per service
- `schemas/dto/` - JSON Schema DTO artifacts per domain

## Included Specs

- `openapi/lifecycle-api.yaml`
- `openapi/trade-capture-api.yaml`
- `openapi/position-api.yaml`
- `openapi/contract-api.yaml`
- `openapi/cashflow-api.yaml`
- `openapi/reset-api.yaml`
- `openapi/valuation-api.yaml`
- `openapi/iam-api.yaml`

## Included DTO Schemas

- `schemas/dto/common.schema.json`
- `schemas/dto/lifecycle.schema.json`
- `schemas/dto/trade-capture.schema.json`
- `schemas/dto/position.schema.json`
- `schemas/dto/contract.schema.json` (planned)
- `schemas/dto/cashflow.schema.json`
- `schemas/dto/reset.schema.json`
- `schemas/dto/valuation.schema.json`
- `schemas/dto/iam.schema.json`

## Contract Conventions

- Required headers on write APIs:
  - `Idempotency-Key`
  - `X-Correlation-Id`
- Optional context header:
  - `X-User-Context`
- Standard error envelope:
  - `ErrorResponse { code, message, correlationId, timestamp, details[] }`

## Why This Module Exists

- Prevents interface drift between services
- Enables independent service implementation against frozen contracts
- Supports automated client generation and contract testing in CI

## Next Technical Additions

1. Spectral linting for all OpenAPI specs
2. Backward-compatibility checks in CI
3. Generated typed clients for service modules
