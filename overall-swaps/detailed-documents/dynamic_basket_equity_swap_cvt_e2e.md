# Dynamic Basket Equity Swap with CVT
## End-to-End Technical Integration (CDM, CQRS, ODS, Risk, Reg Reporting)

This document consolidates all requested artifacts for integrating a client-specified, dynamically rebalanced custom basket equity swap with a Cash Value Ticker (CVT) into an enterprise-grade equity swap lifecycle management platform.

It is written to be **implementation-ready** and aligned with:
- **CDM semantics**
- **Event sourcing + CQRS**
- **Real-time ODS with selective replay**
- **Regulatory-safe trade lifecycle handling**

---

## 1. Canonical Design Rules (Invariants)

These invariants must hold system-wide:

1. **One legal trade, one UTI** – Rebalances do not create new trades.
2. **Basket rebalancing is product mechanics, not trade lifecycle** – Rebalance produces no TradeState mutation.
3. **Trade economics are immutable unless legally amended** – Notional, payoff, financing, reset conventions change only via lifecycle primitives (e.g. TermsChange).
4. **Basket composition is versioned, point-in-time state** – Each rebalance creates a new basket version; history is auditable.
5. **CVT is a residual basket constituent, never a traded underlier** – CVT = notional − Σ(real underlyer MV).
6. **Index MTM = Σ constituent MTM + CVT** (within tolerance) – Enforced at valuation and control checks.

**Violating any of these breaks regulatory, margin, or P&L integrity.**

---

## 2. Physical Data Model (MS SQL – snake_case)

### 2.1 Trade (existing)

```
trade
-----
trade_id (pk)
product_type          -- EQUITY_INDEX_TRS
notional_amount
currency
effective_date
maturity_date
financing_terms_json
trade_status
```

### 2.2 Basket Definition (NEW)

```
basket_definition
-----------------
basket_id
basket_version
trade_id
effective_from
rebalance_rule        -- NOTIONAL_PRESERVING
client_specified_flag -- true
status                -- ACTIVE / SUPERSEDED
created_at
created_by
```

**Composite PK**: `(basket_id, basket_version)`

### 2.3 Basket Constituents

```
basket_constituent
------------------
basket_id
basket_version
instrument_id
ticker
weight
notional_amount
is_cash               -- true for CVT
pricing_attributes_json
```

### 2.4 Basket Instructions (Audit + Replay)

```
basket_instruction
------------------
instruction_id
basket_id
instruction_type      -- REBALANCE
requested_effective_date
instruction_payload_json
requested_by          -- CLIENT / SYSTEM
instruction_status
created_at
```

---

## 3. CDM Mapping (Precise)

### 3.1 Trade

| Platform | CDM |
|----------|-----|
| trade | TradeState |
| product_type | EquityIndexPayout |
| financing_terms | InterestRatePayout |

### 3.2 Basket (Extension – Product Model)

Basket lives **inside the product**, not lifecycle:

```
Product
└─ Underlier
   └─ CustomBasket (extension)
      ├─ basketId
      ├─ basketVersion
      ├─ components[]
      │  ├─ Equity        (AssetPayout)
      │  └─ CashProduct   (CVT)
      └─ rebalanceRule
```

**CDM object types**: AssetPayout (Equity), CashProduct (CVT).

### 3.3 Rebalance

Rebalance is captured as:
- **Instruction** (non-lifecycle)
- **Produces no TradeState mutation**

---

## 4. Dynamic Rebalance – Event Flow

### 4.1 Trigger Sources

- Client reweight instruction
- Corporate action normalization
- Hedge execution drift

### 4.2 Event Sequence

```
BasketRebalanceRequested
  → BasketRebalanceValidated
  → BasketVersionCreated
  → Projections Updated
```

**No lifecycle events emitted.**

---

## 5. Valuation & CVT Mechanics (Pseudocode)

### 5.1 Basket Valuation

```
function valueBasket(trade, basketVersion, marketData):
  equity_value = 0
  for c in basketVersion.constituents:
    if not c.is_cash:
      equity_value += c.weight * trade.notional * marketData.price(c)
  cvt_value = trade.notional - equity_value
  return equity_value + cvt_value
```

### 5.2 Rebalance Logic (Zero P&L)

```
function rebalanceBasket(oldBasket, instruction, prices):
  newBasket = copy(oldBasket)
  applyWeightChanges(newBasket, instruction)
  for c in newBasket.constituents:
    c.notional = c.weight * trade.notional
  cvt.notional = trade.notional - sum(non_cash_notionals)
  assert abs(oldMTM - newMTM) < tolerance
  return newBasket
```

---

## 6. P&L Attribution Model

| Component | P&L Type |
|-----------|----------|
| Equity constituents | Market P&L |
| CVT | Funding / carry |
| Rebalance | Zero |
| Reset | Realization |

**P&L is always explainable across basket versions.**

---

## 7. Risk Management Integration

### 7.1 Market Risk

- Look-through to constituents
- Aggregate back to basket
- **CVT delta = 0**

### 7.2 Credit & Funding Risk

- CVT tracked as unsecured funding
- Limits enforced:
  - Max CVT %
  - Max negative CVT

---

## 8. Margin & Collateral

### 8.1 Initial Margin

- Computed on basket volatility
- Constituent look-through for concentration add-ons

### 8.2 Variation Margin

- Based on basket MTM
- CVT implicitly included
- CSA references trade MTM only

---

## 9. Regulatory Reporting Mapping

### 9.1 Reported

| Field | Value |
|-------|-------|
| Product | Equity Swap |
| Underlier | Custom Basket |
| Client Specified | TRUE |
| Notional | Trade notional |
| Price | Basket price |
| UTI | Single |

### 9.2 Not Reported

- Constituents
- CVT
- Basket versions
- Rebalance events

### 9.3 Reportable Modifications (Thresholds)

A rebalance becomes reportable **only** if it changes:
- Notional
- Payoff structure
- Financing terms
- Reset conventions

**Otherwise: not reportable.**

---

## 10. Numeric Walkthrough (Condensed)

- **Notional**: $100mm
- **Basket v1**: 90% equities / 10% CVT
- **Basket v2**: 100% equities / 0% CVT
- **MTM before rebalance = MTM after rebalance**
- **Only future P&L distribution changes.**

---

## 11. Selective Replay (ODS)

- **Basket versions** are replayable independently (e.g. for projection fixes).
- **TradeState is never replayed** for rebalance correction.
- **Projections** recompute deterministically from basket events + lifecycle events.

---

## 12. Control Checks (Production)

| Check | Purpose |
|-------|---------|
| Σ weights = 1.0 | Basket composition consistency |
| Σ notionals = trade notional | Notional-neutral invariant |
| CVT within limits | Max CVT %, max negative CVT (if disallowed) |
| Index MTM reconciliation | Index MTM = Σ constituent MTM + CVT (within tolerance) |

**Alerts raised on breach.**

---

## 13. Why This Survives Audit

- Clear separation of **contract vs mechanics**
- Deterministic basket rules
- Full lineage
- Minimal regulatory surface

---

## 14. Final Architecture Position

**Dynamic baskets with CVT are:**
- **Product mechanics**
- **Versioned state**
- **Non-lifecycle**

This design is consistent with Tier-1 prime brokerage equity derivatives platforms.

---

## References

- [Architectural Blueprint](../architectural_blueprint.md) – Dynamic Basket with CVT section
- [Consolidated Equity Swap Service Guide](../consolidated_equity_swap_service_guide.md) – Basket/underlier handling, CVT cashflow
