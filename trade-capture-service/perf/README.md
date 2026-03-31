# Trade Capture k6 Load Test

## Prerequisites

- k6 installed
- Trade Capture Service running on `http://localhost:8085`

## Run

```bash
k6 run perf/k6-trade-capture.js
```

To use a different base URL:

```bash
BASE_URL=http://localhost:8085/trade-capture-service/v1 k6 run perf/k6-trade-capture.js
```
