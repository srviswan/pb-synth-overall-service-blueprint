import http from "k6/http";
import { check, sleep } from "k6";
import { SharedArray } from "k6/data";

export const options = {
  scenarios: {
    mixed: {
      executor: "ramping-vus",
      startVUs: 1,
      stages: [
        { duration: "20s", target: 5 },
        { duration: "40s", target: 10 },
        { duration: "20s", target: 0 }
      ],
      gracefulRampDown: "10s"
    }
  },
  thresholds: {
    http_req_failed: ["rate<0.05"],
    http_req_duration: ["p(95)<1500"]
  }
};

const baseUrl = __ENV.BASE_URL || "http://localhost:8085/trade-capture-service/v1";

const symbols = new SharedArray("symbols", () => ["AAPL", "MSFT", "NVDA", "GOOGL", "AMZN"]);

function randomInt(max) {
  return Math.floor(Math.random() * max);
}

function tradePayload(i, nonce) {
  const symbol = symbols[randomInt(symbols.length)];
  return JSON.stringify({
    sourceSystem: "AMS",
    tradeExternalId: `TRD-${nonce}-${i}`,
    accountId: `ACC-${(randomInt(5) + 1)}`,
    bookId: `BOOK-${(randomInt(3) + 1)}`,
    securityId: symbol,
    direction: randomInt(2) === 0 ? "BUY" : "SELL",
    quantity: randomInt(9000) + 1,
    price: Number((Math.random() * 200 + 1).toFixed(2)),
    tradeDate: "2026-03-03"
  });
}

function captureSingle() {
  const nonce = `${Date.now()}-${Math.random().toString(16).slice(2)}-${__VU}-${__ITER}`;
  const idempotencyKey = `idem-single-${nonce}`;
  const correlationId = `corr-single-${nonce}`;
  const res = http.post(`${baseUrl}/trades`, tradePayload(0, nonce), {
    headers: {
      "Content-Type": "application/json",
      "Idempotency-Key": idempotencyKey,
      "X-Correlation-Id": correlationId
    }
  });
  check(res, {
    "single accepted": (r) => r.status === 202
  });
  return res;
}

function captureBatch() {
  const nonce = `${Date.now()}-${Math.random().toString(16).slice(2)}-${__VU}-${__ITER}`;
  const idempotencyKey = `idem-batch-${nonce}`;
  const correlationId = `corr-batch-${nonce}`;
  const req = JSON.stringify({
    sourceSystem: "AMS",
    trades: [JSON.parse(tradePayload(1, nonce)), JSON.parse(tradePayload(2, nonce))]
  });
  const res = http.post(`${baseUrl}/trades:batch`, req, {
    headers: {
      "Content-Type": "application/json",
      "Idempotency-Key": idempotencyKey,
      "X-Correlation-Id": correlationId
    }
  });
  check(res, {
    "batch accepted": (r) => r.status === 202
  });
}

function getStatus(ingestionId) {
  if (!ingestionId) return;
  const res = http.get(`${baseUrl}/trades/${ingestionId}`);
  check(res, {
    "status ok": (r) => r.status === 200
  });
}

export default function () {
  const branch = randomInt(10);
  if (branch < 6) {
    const res = captureSingle();
    if (res.status === 202) {
      const body = res.json();
      getStatus(body.ingestionId);
    }
  } else if (branch < 9) {
    captureBatch();
  }
  sleep(0.5);
}
