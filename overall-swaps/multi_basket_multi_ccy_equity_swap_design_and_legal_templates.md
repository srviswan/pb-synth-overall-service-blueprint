# Multi-Basket Multi-CCY Equity Swap Design with Common Settlement Currency

## 1. Objective

Design a CDM-aligned model for an equity swap where:

- There are multiple baskets.
- Each basket is associated with its own currency context.
- Each currency context has its own funding interest leg and rate specification.
- All leg-level cashflows are converted and netted into one common settlement currency for a single payment.

This document provides:

- A design model and data rules.
- A full JSON sample (trade model + settlement event model).
- A ready-to-use term sheet skeleton.
- A ready-to-use confirmation skeleton.

## 2. Scope and Assumptions

- Product representation uses CDM constructs (`TradeState`, `Trade`, `TradableProduct`, `EconomicTerms`, `Payout`, `PerformancePayout`, `InterestRatePayout`, `Transfer`).
- Legal drafting uses modular structure: common terms + per-leg schedules + FX/net settlement schedule.
- This is a technical and operational template and not legal advice.

## 3. Design Principles

### 3.1 Economic truth vs settlement truth

- Economic truth remains at leg level in local currencies.
- Settlement truth is one net payable amount in a common settlement currency.

### 3.2 Payout structure

- `EconomicTerms.payout` is multi-leg and supports multiple payouts.
- Use one `PerformancePayout` per basket/currency context.
- Use one `InterestRatePayout` per currency context.
- Pair each performance leg with its corresponding funding leg using a deterministic `legPairId`.

### 3.3 FX conversion stage

- Convert only at payment generation time.
- Keep leg calculations in original currency.
- Apply agreed fixing source/date/time and fallback waterfall for conversion.

### 3.4 Single settlement instruction

- Generate one final `Transfer` in common settlement currency per payment date after conversion and netting.

## 4. Canonical CDM Model Pattern

- `TradeState.trade.tradableProduct.product.economicTerms.payout[]`
  - `PerformancePayout` (for basket return leg)
  - `InterestRatePayout` (for funding leg)
- `TradeState.trade.tradableProduct.tradeLot[].priceQuantity[]`
  - lot-level quantities/notionals and effective dates
- `TradeState.transferHistory[]`
  - leg-level and/or final net transfer lineage

Recommended extension metadata (implementation-level):

- `x-legPairId`: binds one return leg and one funding leg.
- `x-legCurrency`: local leg currency.
- `x-commonSettlementPolicy`: global conversion/netting policy.
- `x-fxRuleId`: conversion instruction lookup key.

## 5. Validation Rules (Implementation)

1. Each `PerformancePayout` must have exactly one matching `InterestRatePayout` by `x-legPairId`.
2. Each `x-legCurrency` must have one FX conversion rule to common settlement currency.
3. Funding leg `priceQuantity.quantitySchedule.unit.currency` must match `x-legCurrency`.
4. `payRelativeTo`, payment frequency, and date conventions must be explicitly defined per leg.
5. Netting must be deterministic:
   - Convert each leg amount to settlement currency.
   - Apply rounding policy.
   - Sum with sign based on payer/receiver direction.
6. A single final `Transfer` must be generated per settlement date and currency when netting policy is full-net single-payment.

## 6. Full JSON Sample

The sample below includes:

- Full trade structure with three baskets (`EUR`, `JPY`, `GBP`).
- Three funding legs with separate rate specifications.
- Common settlement policy in `USD`.
- Example settlement event with converted leg cashflows and one final net transfer.

```json
{
  "tradeState": {
    "trade": {
      "tradeIdentifier": [
        {
          "assignedIdentifier": {
            "identifier": "EQSWAP-MULTI-CCY-0001"
          },
          "identifierType": "UniqueTransactionIdentifier"
        }
      ],
      "tradeDate": "2026-02-28",
      "party": [
        {
          "partyId": [
            {
              "identifier": {
                "value": "LEI-PARTY-A"
              }
            }
          ],
          "name": {
            "value": "Dealer A"
          }
        },
        {
          "partyId": [
            {
              "identifier": {
                "value": "LEI-PARTY-B"
              }
            }
          ],
          "name": {
            "value": "Fund B"
          }
        }
      ],
      "tradableProduct": {
        "product": {
          "identifier": [
            {
              "assignedIdentifier": {
                "identifier": "PRD-EQSWAP-MULTI-001"
              }
            }
          ],
          "economicTerms": {
            "effectiveDate": {
              "adjustableDate": {
                "unadjustedDate": "2026-03-02"
              }
            },
            "terminationDate": {
              "adjustableDate": {
                "unadjustedDate": "2027-03-02"
              }
            },
            "payout": [
              {
                "PerformancePayout": {
                  "payerReceiver": {
                    "payer": "Party1",
                    "receiver": "Party2"
                  },
                  "underlier": {
                    "Observable": {
                      "Basket": {
                        "basketConstituent": [
                          {
                            "Asset": {
                              "Instrument": {
                                "identifier": [
                                  {
                                    "identifier": {
                                      "value": "AIR.PA"
                                    }
                                  }
                                ]
                              }
                            },
                            "quantity": [
                              {
                                "value": 1000,
                                "unit": {
                                  "financialUnit": "Share"
                                }
                              }
                            ]
                          },
                          {
                            "Asset": {
                              "Instrument": {
                                "identifier": [
                                  {
                                    "identifier": {
                                      "value": "SAP.DE"
                                    }
                                  }
                                ]
                              }
                            },
                            "quantity": [
                              {
                                "value": 800,
                                "unit": {
                                  "financialUnit": "Share"
                                }
                              }
                            ]
                          }
                        ]
                      }
                    }
                  },
                  "returnTerms": {
                    "priceReturnTerms": {
                      "returnType": "Total"
                    },
                    "dividendReturnTerms": {
                      "dividendReinvestment": false,
                      "dividendEntitlement": "ExDate"
                    }
                  },
                  "valuationDates": {
                    "finalValuationDate": {
                      "determinationMethod": "Closing",
                      "valuationDate": {
                        "adjustableDate": {
                          "unadjustedDate": "2027-02-26"
                        }
                      }
                    }
                  },
                  "paymentDates": {
                    "paymentFrequency": {
                      "periodMultiplier": 1,
                      "period": "M"
                    },
                    "payRelativeTo": "CalculationPeriodEndDate"
                  },
                  "priceQuantity": {
                    "quantitySchedule": {
                      "value": 50000000,
                      "unit": {
                        "currency": "EUR"
                      }
                    },
                    "reset": true
                  },
                  "x-legPairId": "LP-EUR-001",
                  "x-legCurrency": "EUR",
                  "x-fxRuleId": "FX-EUR-USD"
                }
              },
              {
                "PerformancePayout": {
                  "payerReceiver": {
                    "payer": "Party1",
                    "receiver": "Party2"
                  },
                  "underlier": {
                    "Observable": {
                      "Basket": {
                        "basketConstituent": [
                          {
                            "Asset": {
                              "Instrument": {
                                "identifier": [
                                  {
                                    "identifier": {
                                      "value": "7203.T"
                                    }
                                  }
                                ]
                              }
                            },
                            "quantity": [
                              {
                                "value": 2000,
                                "unit": {
                                  "financialUnit": "Share"
                                }
                              }
                            ]
                          },
                          {
                            "Asset": {
                              "Instrument": {
                                "identifier": [
                                  {
                                    "identifier": {
                                      "value": "6758.T"
                                    }
                                  }
                                ]
                              }
                            },
                            "quantity": [
                              {
                                "value": 1500,
                                "unit": {
                                  "financialUnit": "Share"
                                }
                              }
                            ]
                          }
                        ]
                      }
                    }
                  },
                  "returnTerms": {
                    "priceReturnTerms": {
                      "returnType": "Total"
                    },
                    "dividendReturnTerms": {
                      "dividendReinvestment": false,
                      "dividendEntitlement": "ExDate"
                    }
                  },
                  "valuationDates": {
                    "finalValuationDate": {
                      "determinationMethod": "Closing",
                      "valuationDate": {
                        "adjustableDate": {
                          "unadjustedDate": "2027-02-26"
                        }
                      }
                    }
                  },
                  "paymentDates": {
                    "paymentFrequency": {
                      "periodMultiplier": 1,
                      "period": "M"
                    },
                    "payRelativeTo": "CalculationPeriodEndDate"
                  },
                  "priceQuantity": {
                    "quantitySchedule": {
                      "value": 7000000000,
                      "unit": {
                        "currency": "JPY"
                      }
                    },
                    "reset": true
                  },
                  "x-legPairId": "LP-JPY-001",
                  "x-legCurrency": "JPY",
                  "x-fxRuleId": "FX-JPY-USD"
                }
              },
              {
                "PerformancePayout": {
                  "payerReceiver": {
                    "payer": "Party1",
                    "receiver": "Party2"
                  },
                  "underlier": {
                    "Observable": {
                      "Basket": {
                        "basketConstituent": [
                          {
                            "Asset": {
                              "Instrument": {
                                "identifier": [
                                  {
                                    "identifier": {
                                      "value": "SHEL.L"
                                    }
                                  }
                                ]
                              }
                            },
                            "quantity": [
                              {
                                "value": 1200,
                                "unit": {
                                  "financialUnit": "Share"
                                }
                              }
                            ]
                          },
                          {
                            "Asset": {
                              "Instrument": {
                                "identifier": [
                                  {
                                    "identifier": {
                                      "value": "BP.L"
                                    }
                                  }
                                ]
                              }
                            },
                            "quantity": [
                              {
                                "value": 1600,
                                "unit": {
                                  "financialUnit": "Share"
                                }
                              }
                            ]
                          }
                        ]
                      }
                    }
                  },
                  "returnTerms": {
                    "priceReturnTerms": {
                      "returnType": "Total"
                    },
                    "dividendReturnTerms": {
                      "dividendReinvestment": false,
                      "dividendEntitlement": "ExDate"
                    }
                  },
                  "valuationDates": {
                    "finalValuationDate": {
                      "determinationMethod": "Closing",
                      "valuationDate": {
                        "adjustableDate": {
                          "unadjustedDate": "2027-02-26"
                        }
                      }
                    }
                  },
                  "paymentDates": {
                    "paymentFrequency": {
                      "periodMultiplier": 1,
                      "period": "M"
                    },
                    "payRelativeTo": "CalculationPeriodEndDate"
                  },
                  "priceQuantity": {
                    "quantitySchedule": {
                      "value": 42000000,
                      "unit": {
                        "currency": "GBP"
                      }
                    },
                    "reset": true
                  },
                  "x-legPairId": "LP-GBP-001",
                  "x-legCurrency": "GBP",
                  "x-fxRuleId": "FX-GBP-USD"
                }
              },
              {
                "InterestRatePayout": {
                  "payerReceiver": {
                    "payer": "Party2",
                    "receiver": "Party1"
                  },
                  "priceQuantity": {
                    "quantitySchedule": {
                      "value": 50000000,
                      "unit": {
                        "currency": "EUR"
                      }
                    }
                  },
                  "rateSpecification": {
                    "FloatingRateSpecification": {
                      "floatingRate": {
                        "rateOption": {
                          "floatingRateIndex": "EUR-EURIBOR-Reuters",
                          "indexTenor": {
                            "periodMultiplier": 1,
                            "period": "M"
                          }
                        },
                        "spread": {
                          "value": 0.0015,
                          "unit": {
                            "currency": "EUR"
                          },
                          "priceType": "InterestRate"
                        }
                      }
                    }
                  },
                  "dayCountFraction": "ACT_360",
                  "calculationPeriodDates": {
                    "effectiveDate": {
                      "adjustableDate": {
                        "unadjustedDate": "2026-03-02"
                      }
                    },
                    "terminationDate": {
                      "adjustableDate": {
                        "unadjustedDate": "2027-03-02"
                      }
                    },
                    "calculationPeriodFrequency": {
                      "periodMultiplier": 1,
                      "period": "M"
                    }
                  },
                  "paymentDates": {
                    "paymentFrequency": {
                      "periodMultiplier": 1,
                      "period": "M"
                    },
                    "payRelativeTo": "CalculationPeriodEndDate"
                  },
                  "resetDates": {
                    "resetRelativeTo": "CalculationPeriodStartDate",
                    "resetFrequency": {
                      "periodMultiplier": 1,
                      "period": "M"
                    }
                  },
                  "x-legPairId": "LP-EUR-001",
                  "x-legCurrency": "EUR",
                  "x-fxRuleId": "FX-EUR-USD"
                }
              },
              {
                "InterestRatePayout": {
                  "payerReceiver": {
                    "payer": "Party2",
                    "receiver": "Party1"
                  },
                  "priceQuantity": {
                    "quantitySchedule": {
                      "value": 7000000000,
                      "unit": {
                        "currency": "JPY"
                      }
                    }
                  },
                  "rateSpecification": {
                    "FloatingRateSpecification": {
                      "floatingRate": {
                        "rateOption": {
                          "floatingRateIndex": "JPY-TONA-OIS-COMPOUND",
                          "indexTenor": {
                            "periodMultiplier": 1,
                            "period": "M"
                          }
                        },
                        "spread": {
                          "value": 0.0010,
                          "unit": {
                            "currency": "JPY"
                          },
                          "priceType": "InterestRate"
                        }
                      }
                    }
                  },
                  "dayCountFraction": "ACT_365F",
                  "calculationPeriodDates": {
                    "effectiveDate": {
                      "adjustableDate": {
                        "unadjustedDate": "2026-03-02"
                      }
                    },
                    "terminationDate": {
                      "adjustableDate": {
                        "unadjustedDate": "2027-03-02"
                      }
                    },
                    "calculationPeriodFrequency": {
                      "periodMultiplier": 1,
                      "period": "M"
                    }
                  },
                  "paymentDates": {
                    "paymentFrequency": {
                      "periodMultiplier": 1,
                      "period": "M"
                    },
                    "payRelativeTo": "CalculationPeriodEndDate"
                  },
                  "resetDates": {
                    "resetRelativeTo": "CalculationPeriodStartDate",
                    "resetFrequency": {
                      "periodMultiplier": 1,
                      "period": "M"
                    }
                  },
                  "x-legPairId": "LP-JPY-001",
                  "x-legCurrency": "JPY",
                  "x-fxRuleId": "FX-JPY-USD"
                }
              },
              {
                "InterestRatePayout": {
                  "payerReceiver": {
                    "payer": "Party2",
                    "receiver": "Party1"
                  },
                  "priceQuantity": {
                    "quantitySchedule": {
                      "value": 42000000,
                      "unit": {
                        "currency": "GBP"
                      }
                    }
                  },
                  "rateSpecification": {
                    "FloatingRateSpecification": {
                      "floatingRate": {
                        "rateOption": {
                          "floatingRateIndex": "GBP-SONIA-COMPOUND",
                          "indexTenor": {
                            "periodMultiplier": 1,
                            "period": "M"
                          }
                        },
                        "spread": {
                          "value": 0.0018,
                          "unit": {
                            "currency": "GBP"
                          },
                          "priceType": "InterestRate"
                        }
                      }
                    }
                  },
                  "dayCountFraction": "ACT_365F",
                  "calculationPeriodDates": {
                    "effectiveDate": {
                      "adjustableDate": {
                        "unadjustedDate": "2026-03-02"
                      }
                    },
                    "terminationDate": {
                      "adjustableDate": {
                        "unadjustedDate": "2027-03-02"
                      }
                    },
                    "calculationPeriodFrequency": {
                      "periodMultiplier": 1,
                      "period": "M"
                    }
                  },
                  "paymentDates": {
                    "paymentFrequency": {
                      "periodMultiplier": 1,
                      "period": "M"
                    },
                    "payRelativeTo": "CalculationPeriodEndDate"
                  },
                  "resetDates": {
                    "resetRelativeTo": "CalculationPeriodStartDate",
                    "resetFrequency": {
                      "periodMultiplier": 1,
                      "period": "M"
                    }
                  },
                  "x-legPairId": "LP-GBP-001",
                  "x-legCurrency": "GBP",
                  "x-fxRuleId": "FX-GBP-USD"
                }
              }
            ]
          }
        },
        "tradeLot": [
          {
            "lotIdentifier": [
              {
                "assignedIdentifier": {
                  "identifier": "LOT-001"
                }
              }
            ],
            "priceQuantity": [
              {
                "quantity": [
                  {
                    "value": 50000000,
                    "unit": {
                      "currency": "EUR"
                    }
                  }
                ],
                "effectiveDate": {
                  "adjustableDate": {
                    "unadjustedDate": "2026-03-02"
                  }
                }
              },
              {
                "quantity": [
                  {
                    "value": 7000000000,
                    "unit": {
                      "currency": "JPY"
                    }
                  }
                ],
                "effectiveDate": {
                  "adjustableDate": {
                    "unadjustedDate": "2026-03-02"
                  }
                }
              },
              {
                "quantity": [
                  {
                    "value": 42000000,
                    "unit": {
                      "currency": "GBP"
                    }
                  }
                ],
                "effectiveDate": {
                  "adjustableDate": {
                    "unadjustedDate": "2026-03-02"
                  }
                }
              }
            ]
          }
        ],
        "counterparty": [
          {
            "role": "Party1",
            "partyReference": "LEI-PARTY-A"
          },
          {
            "role": "Party2",
            "partyReference": "LEI-PARTY-B"
          }
        ],
        "x-commonSettlementPolicy": {
          "commonSettlementCurrency": "USD",
          "nettingMode": "FULL_NET_SINGLE_TRANSFER",
          "roundingPolicy": {
            "roundingDirection": "Nearest",
            "precision": 2
          },
          "fxRules": [
            {
              "fxRuleId": "FX-EUR-USD",
              "sourceCurrency": "EUR",
              "targetCurrency": "USD",
              "rateSourcePrimary": "ECB",
              "rateSourceSecondary": "DealerPoll",
              "fixingDateRule": "PaymentDate-2BD",
              "fixingTime": "16:00:00Z"
            },
            {
              "fxRuleId": "FX-JPY-USD",
              "sourceCurrency": "JPY",
              "targetCurrency": "USD",
              "rateSourcePrimary": "WM_REUTERS",
              "rateSourceSecondary": "DealerPoll",
              "fixingDateRule": "PaymentDate-2BD",
              "fixingTime": "16:00:00Z"
            },
            {
              "fxRuleId": "FX-GBP-USD",
              "sourceCurrency": "GBP",
              "targetCurrency": "USD",
              "rateSourcePrimary": "BOE",
              "rateSourceSecondary": "DealerPoll",
              "fixingDateRule": "PaymentDate-2BD",
              "fixingTime": "16:00:00Z"
            }
          ]
        }
      }
    },
    "state": {
      "positionState": "Formed"
    }
  },
  "x-settlementEventExample": {
    "settlementDate": "2026-06-30",
    "legCashflowsLocal": [
      {
        "legPairId": "LP-EUR-001",
        "currency": "EUR",
        "amount": 1250000.45,
        "direction": "Party1PaysParty2"
      },
      {
        "legPairId": "LP-JPY-001",
        "currency": "JPY",
        "amount": -88500000.0,
        "direction": "Party2PaysParty1"
      },
      {
        "legPairId": "LP-GBP-001",
        "currency": "GBP",
        "amount": 410000.12,
        "direction": "Party1PaysParty2"
      }
    ],
    "convertedToUSD": [
      {
        "legPairId": "LP-EUR-001",
        "usdAmount": 1362500.49,
        "fxRate": 1.09
      },
      {
        "legPairId": "LP-JPY-001",
        "usdAmount": -597375.0,
        "fxRate": 0.00675
      },
      {
        "legPairId": "LP-GBP-001",
        "usdAmount": 516600.15,
        "fxRate": 1.26
      }
    ],
    "netUSD": 1281725.64,
    "finalTransfer": {
      "transfer": {
        "identifier": [
          {
            "value": "TRF-USD-NET-2026-06-30"
          }
        ],
        "payerReceiver": {
          "payer": "Party1",
          "receiver": "Party2"
        },
        "transferExpression": {
          "scheduledTransfer": {
            "transferType": "Performance"
          }
        },
        "asset": {
          "Cash": {
            "currency": "USD"
          }
        },
        "quantity": {
          "value": 1281725.64,
          "unit": {
            "currency": "USD"
          }
        },
        "settlementDate": {
          "adjustedDate": "2026-06-30"
        }
      }
    }
  }
}
```

## 7. Ready-to-Use Term Sheet Skeleton

Use this as a pre-confirmation commercial template.

```text
TERM SHEET
Multi-Basket Multi-CCY Equity Swap with Single-Currency Net Settlement

1. Parties
- Party A:
- Party B:
- Trade Date:
- Effective Date:
- Scheduled Termination Date:

2. Governing Framework
- Master Agreement:
- Product Definitions:
- Credit Support Annex:
- Governing Law:

3. Common Economic Terms
- Common Settlement Currency: [USD]
- Netting Election: [Full net, single transfer per payment date]
- Calculation Agent:
- Business Day Convention:
- Payment Calendar(s):
- Disruption Fallback Waterfall:

4. Basket Return Leg Schedule (repeat per basket)
- Basket Leg ID:
- Leg Pair ID:
- Basket Name:
- Basket Constituents and Weights:
- Basket Currency:
- Return Type: [Price / Total]
- Dividend Treatment:
- Valuation Dates / Time / Exchange:
- Payment Frequency:
- Payer:
- Receiver:

5. Funding Leg Schedule (repeat per currency)
- Funding Leg ID:
- Leg Pair ID:
- Funding Currency:
- Notional:
- Floating/Fixed:
- Index (if floating):
- Tenor:
- Spread:
- Day Count Fraction:
- Reset Frequency:
- Payment Frequency:
- Payer:
- Receiver:

6. FX Conversion and Net Settlement
- FX Rule ID:
- Source Currency:
- Target Currency:
- Rate Source Primary:
- Rate Source Secondary:
- Fixing Date Rule:
- Fixing Time:
- Rounding Precision:
- Netting Formula:

7. Fees and Charges
- Upfront Fee:
- Unwind Fee:
- Other Fees:

8. Corporate Actions and Extraordinary Events
- Corporate Action Methodology:
- Extraordinary Event Elections:
- Determining Party:

9. Optional Early Termination
- Optional Early Termination:
- Electing Party:
- Notice Period:
- Break Cost Treatment:

10. Representations and Confirmations
- Tax representations:
- Capacity and authority:
- Sanctions and compliance:

11. Signatures
- Party A:
- Party B:
```

## 8. Ready-to-Use Confirmation Skeleton

Use this as a legal confirmation template structure.

```text
CONFIRMATION
Multi-Basket Multi-CCY Equity Swap with Common Settlement Currency

Date:
To: [Counterparty]
From: [Entity]

This Confirmation supplements, forms part of, and is subject to the [ISDA Master Agreement date] between [Party A] and [Party B].

Section 1. Transaction Overview
1.1 Trade Identifier:
1.2 Trade Date:
1.3 Effective Date:
1.4 Termination Date:
1.5 Product:
1.6 Common Settlement Currency:
1.7 Netting Election:

Section 2. Governing Documents and Precedence
2.1 Master Agreement:
2.2 Product Definitions:
2.3 CSA:
2.4 Precedence (highest to lowest):
    (a) This Confirmation
    (b) Election Tables and Schedules
    (c) Product Definitions
    (d) Master Agreement

Section 3. Common Elections
3.1 Calculation Agent:
3.2 Business Day Convention:
3.3 Disruption Fallback Waterfall:
3.4 Rounding and Precision:
3.5 Tax and withholding:

Section 4. Basket Return Leg Schedule
For each Basket Return Leg:
4.x.1 Basket Leg ID:
4.x.2 Leg Pair ID:
4.x.3 Underlyer Basket and Constituents:
4.x.4 Basket Currency:
4.x.5 Return Type:
4.x.6 Dividend Terms:
4.x.7 Valuation Dates and Time:
4.x.8 Payment Dates:
4.x.9 Payer/Receiver:

Section 5. Funding Leg Schedule
For each Funding Leg:
5.x.1 Funding Leg ID:
5.x.2 Leg Pair ID:
5.x.3 Currency:
5.x.4 Notional:
5.x.5 Rate Specification:
5.x.6 Day Count Fraction:
5.x.7 Reset Dates/Frequency:
5.x.8 Payment Dates/Frequency:
5.x.9 Payer/Receiver:

Section 6. FX Conversion and Common Settlement
6.1 Conversion Principle:
    Each leg cashflow is first computed in its local currency, then converted to [Common Settlement Currency].
6.2 FX Source and Fixing:
    Table of FX Rule ID, source/target currency, fixing source, fixing date rule, fixing time.
6.3 Fallback Waterfall:
    Primary source -> Secondary source -> Dealer poll -> Calculation Agent determination.
6.4 Netting:
    Converted amounts are netted into a single amount per payment date.
6.5 Single Transfer:
    One final settlement transfer in [Common Settlement Currency].

Section 7. Corporate Actions and Extraordinary Events
7.1 Corporate action adjustments:
7.2 Extraordinary events:
7.3 Determination party and dispute process:

Section 8. Optional Early Termination and Break Costs
8.1 Optional Early Termination election:
8.2 Electing party:
8.3 Notice mechanics:
8.4 Break funding / close-out mechanics:

Section 9. Operational Provisions
9.1 Notice details:
9.2 Matching and reconciliation:
9.3 Reporting identifiers (UTI/USI etc.):
9.4 Settlement instructions:

Section 10. Representations
10.1 Capacity and authority:
10.2 No conflict:
10.3 Tax representations:
10.4 Sanctions and compliance:

Section 11. Signatures
Accepted and agreed:
[Party A signatory]
[Party B signatory]
```

## 9. Implementation Notes

- Keep legal IDs (`LegPairId`, `FxRuleId`, Basket IDs) identical in:
  - term sheet,
  - confirmation schedules,
  - trade JSON,
  - lifecycle events.
- Compute and store both:
  - local-currency leg cashflows,
  - converted common-currency amounts,
  for full auditability.
- Persist the FX fixing evidence used for each conversion.

## 10. Next Build Steps

1. Create schema validations for the rules in section 5.
2. Implement deterministic conversion and netting function.
3. Generate confirmation and term sheet from a single canonical trade JSON.
4. Add reconciliation reports: local legs vs converted vs final net transfer.

