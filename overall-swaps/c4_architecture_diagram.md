# Equity Swap Service - C4 Architecture Diagrams

This document provides C4 (Context, Container, Component) diagrams for the Equity Swap Lifecycle Management System, derived from the [architectural_blueprint.md](architectural_blueprint.md).

---

## C4 Level 1: System Context Diagram

Shows the Equity Swap Lifecycle Management System and its relationships with users and external systems.

```mermaid
C4Context
    title System Context Diagram - Equity Swap Lifecycle Management

    Person(trader, "Trader", "Front office user capturing and managing equity swap trades")
    Person(ops, "Operations User", "Middle/back office user for positions, settlements, reporting")
    Person(admin, "System Admin", "IT administrator managing users and entitlements")

    System_Boundary(equity_swap_system, "Equity Swap Lifecycle Management System") {
        System(swap_sys, "Equity Swap Lifecycle Management System", "Processes and manages equity swap trades throughout their lifecycle - from trade capture through settlement, valuation, and reporting. Handles 2M+ trades/day with CDM compliance.")
    }

    System_Ext(ams, "Allocation Management System", "External AMS that allocates trades and submits to Trade Capture")
    System_Ext(md_src, "Market Data Sources", "Equity prices, FX rates, indices, dividends")
    System_Ext(ref_src, "Reference Data Sources", "Securities, accounts, books, parties")
    System_Ext(idp, "Identity Provider", "SSO/LDAP/SAML for authentication")
    System_Ext(reg_rep, "Regulatory Reporting", "EMIR, MiFID II, CFTC reporting")
    System_Ext(fin_rep, "Finance Reporting", "P&L, finance analytics")
    System_Ext(risk_rep, "Risk Reporting", "Market risk, credit risk analytics")

    Rel(trader, swap_sys, "Captures trades, views positions")
    Rel(ops, swap_sys, "Manages lifecycle, settlements, valuations")
    Rel(admin, swap_sys, "Manages users, entitlements")

    Rel(ams, swap_sys, "Submits allocated trades")
    Rel(md_src, swap_sys, "Provides market data")
    Rel(ref_src, swap_sys, "Provides reference data")
    Rel(idp, swap_sys, "Authenticates users")
    Rel(swap_sys, reg_rep, "Regulatory reports (15-min SLA)")
    Rel(swap_sys, fin_rep, "Finance reports")
    Rel(swap_sys, risk_rep, "Risk reports")
```

---

## C4 Level 2: Container Diagram

Shows the high-level technical building blocks (containers) inside the Equity Swap Lifecycle Management System.

```mermaid
C4Container
    title Container Diagram - Equity Swap Lifecycle Management System

    Person(trader, "Trader", "Captures and manages trades")

    System_Boundary(equity_swap_system, "Equity Swap Lifecycle Management System") {

        Container_Boundary(ingestion, "Ingestion Layer") {
            Container(gateway, "API Gateway", "Kong/Envoy", "Routing, auth, rate limiting")
            Container(tc, "Trade Capture Service", "Java/Spring Boot", "Receives allocated trades from AMS, translates to CDM instructions (optimistic pattern)")
        }

        Container_Boundary(orchestration, "Orchestration") {
            Container(engine, "Trade Lifecycle Engine", "Java/Spring Boot", "Stateless CDM primitive processor, Saga pattern for transaction integrity")
        }

        Container_Boundary(core_services, "Core Domain Services") {
            Container(pos, "Position Service", "Java/Spring Boot", "Source of truth for trade lots, positions, UTI lifecycle, settlement date tracking")
            Container(contract, "Contract Service", "Java/Spring Boot", "Product definitions, economic terms, legal agreements")
            Container(cf, "Cashflow Service", "Java/Spring Boot", "Equity leg + interest leg calculations, settlement date-based accrual")
            Container(reset, "Reset Service", "Java/Spring Boot", "Market observations, reset history for calculations")
            Container(val, "Valuation Service", "Java/Spring Boot", "MTM and cost basis valuations")
            Container(basket, "Basket / Custom Index Service", "Java/Spring Boot", "Dynamic baskets with CVT, rebalance instructions")
        }

        Container_Boundary(supporting, "Supporting Services") {
            Container(md_svc, "Market Data Service", "Java/Spring Boot", "Ingests prices, rates, indices; data quality")
            Container(ref_svc, "Reference Data Service", "Java/Spring Boot", "Securities, accounts, books, parties")
            Container(iam, "IAM Service", "Java/Spring Boot", "Authentication, RBAC, ABAC, entitlements")
        }

        Container_Boundary(messaging, "Messaging & Data") {
            ContainerQueue(event_bus, "Event Bus", "Kafka/Solace", "Business event streaming")
            ContainerDb(ods, "Data Product Hub (ODS)", "PostgreSQL/TimescaleDB", "Operational data store for reporting")
        }

        Container_Boundary(storage, "Storage") {
            ContainerDb(tc_db, "Trade Capture DB", "DynamoDB/Cassandra/PostgreSQL", "High-throughput trade queue")
            ContainerDb(pos_db, "Position DB", "PostgreSQL + TimescaleDB", "Trade lots, positions, UTI")
            ContainerDb(contract_db, "Contract DB", "PostgreSQL", "Products, legal agreements")
            ContainerDb(basket_db, "Basket DB", "MS SQL", "Basket definitions, constituents")
            ContainerDb(iam_db, "IAM DB", "PostgreSQL", "Users, roles, entitlements")
            Container(cache, "Cache", "Redis", "Authorization, position lookups, reference data")
        }
    }

    System_Ext(ams, "AMS", "Allocation Management System")
    System_Ext(idp, "Identity Provider", "SSO/LDAP")

    Rel(trader, gateway, "HTTPS")
    Rel(gateway, iam, "AuthZ check")
    Rel(gateway, tc, "Routes trades")
    Rel(ams, gateway, "Submits trades")
    Rel(idp, iam, "Authentication")

    Rel(tc, engine, "CDM instructions")
    Rel(tc, tc_db, "Reads/writes")
    Rel(tc, cache, "Cache")

    Rel(engine, pos, "Writes position state")
    Rel(engine, contract, "Writes product refs")
    Rel(engine, reset, "Writes observations")
    Rel(engine, cf, "Writes cashflows")
    Rel(engine, event_bus, "Publishes events")
    Rel(engine, basket, "Basket instructions")

    Rel(event_bus, ods, "Feeds ODS")
    Rel(event_bus, val, "Triggers valuation")
    Rel(event_bus, cf, "Triggers recalc")

    Rel(val, pos, "Reads positions")
    Rel(val, reset, "Reads resets")
    Rel(val, md_svc, "Reads prices")
    Rel(cf, contract, "Reads terms")
    Rel(cf, reset, "Reads observations")
    Rel(cf, pos, "Reads positions")

    Rel(pos, pos_db, "Reads/writes")
    Rel(pos, cache, "Cache")
    Rel(contract, contract_db, "Reads/writes")
    Rel(contract, cache, "Cache")
    Rel(basket, basket_db, "Reads/writes")
    Rel(iam, iam_db, "Reads/writes")
    Rel(iam, cache, "Auth cache")
    Rel(md_svc, ref_src, "Ingests")
    Rel(ref_svc, ref_src, "Syncs")
```

---

## C4 Level 3: Component Diagrams

### Trade Capture Service - Components

```mermaid
C4Component
    title Trade Capture Service - Component Diagram

    Container_Boundary(tc, "Trade Capture Service") {
        Component(ingestion_api, "Ingestion API", "REST", "Receives allocated trades from AMS")
        Component(transformer, "CDM Transformer", "Java", "Translates to ExecutionInstruction")
        Component(business_rules, "Business Rules Engine", "Java", "Eco/Non-Eco classification")
        Component(engine_client, "Lifecycle Engine Client", "HTTP/gRPC", "Sends instructions in batches")
        Component(idempotency, "Idempotency Handler", "Java", "Prevents duplicate processing")
    }

    ContainerDb(tc_db, "Trade Capture DB", "DynamoDB/PostgreSQL", "Trade queue")

    Rel(ingestion_api, business_rules, "Validates")
    Rel(business_rules, transformer, "Transforms")
    Rel(transformer, idempotency, "Checks duplicate")
    Rel(idempotency, tc_db, "Stores")
    Rel(idempotency, engine_client, "Sends batch")
```

### Trade Lifecycle Engine - Components

```mermaid
C4Component
    title Trade Lifecycle Engine - Component Diagram

    Container_Boundary(engine, "Trade Lifecycle Engine") {
        Component(instruction_router, "Instruction Router", "Java", "Routes by instruction type")
        Component(primitive_processor, "Primitive Processor", "Java", "Executes CDM Create_* functions")
        Component(saga_coordinator, "Saga Coordinator", "Java", "Orchestrates distributed transactions")
        Component(conflict_resolver, "Conflict Resolver", "Java", "ExecutionInstruction → QuantityChangeInstruction")
        Component(event_publisher, "Event Publisher", "Java", "Publishes BusinessEvent")
    }

    Container(pos, "Position Service", "Java", "Positions")
    Container(contract, "Contract Service", "Java", "Products")
    Container(reset, "Reset Service", "Java", "Resets")
    Container(cf, "Cashflow Service", "Java", "Cashflows")
    ContainerQueue(kafka, "Event Bus", "Kafka", "Events")

    Rel(instruction_router, primitive_processor, "Processes")
    Rel(primitive_processor, saga_coordinator, "Coordinates")
    Rel(primitive_processor, conflict_resolver, "On conflict")
    Rel(primitive_processor, event_publisher, "Emits")
    Rel(saga_coordinator, pos, "Writes")
    Rel(saga_coordinator, contract, "Writes")
    Rel(saga_coordinator, reset, "Writes")
    Rel(saga_coordinator, cf, "Writes")
    Rel(event_publisher, kafka, "Publishes")
```

### Position Service - Components

```mermaid
C4Component
    title Position Service - Component Diagram

    Container_Boundary(pos, "Position Service") {
        Component(position_api, "Position API", "REST", "CRUD for positions and lots")
        Component(lot_manager, "Trade Lot Manager", "Java", "Creates/updates TradeLots")
        Component(aggregator, "Position Aggregator", "Java", "Aggregates lots by Account+Book+Security+Direction")
        Component(uti_manager, "UTI Lifecycle Manager", "Java", "UTI assignment, merge history")
        Component(settlement_tracker, "Settlement Date Tracker", "Java", "Tracks settlement dates for interest accrual")
        Component(batch_query, "Batch Query API", "Java", "1000+ keys in single query")
    }

    ContainerDb(pos_db, "Position DB", "PostgreSQL + TimescaleDB", "Trade lots, positions")
    Container(cache, "Redis Cache", "Redis", "Position lookups")

    Rel(position_api, lot_manager, "Create/update")
    Rel(lot_manager, aggregator, "Aggregates")
    Rel(lot_manager, uti_manager, "UTI")
    Rel(lot_manager, settlement_tracker, "Settlement dates")
    Rel(position_api, batch_query, "Batch lookup")
    Rel(batch_query, cache, "Cache lookup")
    Rel(batch_query, pos_db, "DB query")
    Rel(lot_manager, pos_db, "Reads/writes")
```

### Cashflow Service - Components

```mermaid
C4Component
    title Cashflow Service - Component Diagram

    Container_Boundary(cf, "Cashflow Service") {
        Component(equity_leg, "Equity Leg Calculator", "Java", "Price return + dividends")
        Component(interest_leg, "Interest Leg Calculator", "Java", "Fixed/floating interest")
        Component(netting, "Cashflow Netting", "Java", "Nets equity - interest")
        Component(underlier_resolver, "Underlier Resolver", "Java", "Single, index, basket resolution")
        Component(price_strategy, "Price Resolution Strategy", "Java", "SingleName, Index, Basket strategies")
    }

    Container(pos, "Position Service", "Java", "Settled quantity, settlement dates")
    Container(contract, "Contract Service", "Java", "Economic terms")
    Container(reset, "Reset Service", "Java", "Reset observations")

    Rel(equity_leg, underlier_resolver, "Resolves underlier")
    Rel(underlier_resolver, price_strategy, "Gets price")
    Rel(equity_leg, interest_leg, "Both legs")
    Rel(interest_leg, netting, "Net cashflow")
    Rel(equity_leg, pos, "Reads settled quantity")
    Rel(interest_leg, pos, "Reads settlement date")
    Rel(equity_leg, contract, "Reads payoff terms")
    Rel(interest_leg, contract, "Reads financing terms")
    Rel(equity_leg, reset, "Reads observations")
```

---

## C4 Level 4: Deployment Diagram (Optional)

High-level deployment view of the system.

```mermaid
C4Deployment
    title Deployment Diagram - Equity Swap System

    Deployment_Node(lb, "Load Balancer", "NGINX/HAProxy", "Routes traffic")

    Deployment_Node(app_cluster, "Application Cluster", "Kubernetes/ECS") {
        Deployment_Node(tc_node, "Trade Capture", "Docker") {
            Container(tc, "Trade Capture Service", "Java", "")
        }
        Deployment_Node(engine_node, "Lifecycle Engine", "Docker") {
            Container(engine, "Lifecycle Engine", "Java", "")
        }
        Deployment_Node(pos_node, "Position Service", "Docker") {
            Container(pos, "Position Service", "Java", "")
        }
        Deployment_Node(domain_nodes, "Other Domain Services", "Docker", "Contract, Cashflow, Reset, Valuation")
    }

    Deployment_Node(db_cluster, "Database Cluster") {
        Deployment_Node(dynamo, "Trade Capture DB", "DynamoDB/Cassandra") {
            ContainerDb(tc_db, "Trade Queue", "", "")
        }
        Deployment_Node(pg_primary, "PostgreSQL Primary", "PostgreSQL") {
            ContainerDb(pos_db, "Position DB", "", "")
            ContainerDb(contract_db, "Contract DB", "", "")
        }
        Deployment_Node(tsdb, "TimescaleDB", "TimescaleDB") {
            ContainerDb(tsdb_store, "Time-Series", "", "")
        }
        Deployment_Node(redis, "Redis Cluster", "Redis") {
            Container(cache, "Cache", "Redis", "")
        }
    }

    Deployment_Node(msg_cluster, "Messaging Cluster") {
        Deployment_Node(kafka, "Kafka", "Kafka") {
            ContainerQueue(event_bus, "Event Bus", "", "")
        }
    }

    Rel(lb, app_cluster, "HTTPS")
    Rel(tc_node, dynamo, "Reads/writes")
    Rel(pos_node, pg_primary, "Reads/writes")
    Rel(pos_node, tsdb, "Time-series")
    Rel(app_cluster, redis, "Cache")
    Rel(engine_node, kafka, "Publishes")
```

---

## Sequence Diagrams – Service Boundaries

Sequence diagrams showing synchronous and asynchronous interactions between services with clear boundaries (using Mermaid `box` to group participants by layer).

### 1. New Trade Capture Flow (Optimistic Pattern)

AMS → API Gateway (auth) → Trade Capture → Lifecycle Engine → Domain services.

```mermaid
sequenceDiagram
    autonumber
    box rgba(200,220,255) External
        participant AMS as AMS
    end
    box rgba(255,220,200) Ingestion
        participant GW as API Gateway
        participant TC as Trade Capture
    end
    box rgba(200,255,220) Orchestration
        participant Engine as Lifecycle Engine
    end
    box rgba(255,255,200) Supporting
        participant IAM as IAM Service
    end
    box rgba(220,200,255) Core Domain
        participant Pos as Position Service
        participant Contract as Contract Service
        participant Reset as Reset Service
        participant CF as Cashflow Service
    end
    box rgba(200,200,200) Messaging
        participant Kafka as Event Bus
    end

    AMS->>GW: POST /trades (allocated trade)
    GW->>IAM: Validate JWT + Check trade:create
    IAM-->>GW: Authorized
    GW->>TC: Forward (user context)
    TC->>TC: Create ExecutionInstruction
    TC->>Engine: CDM ExecutionInstruction (async batch)
    TC-->>GW: 202 Accepted
    GW-->>AMS: 202 Accepted
    Engine->>Pos: Create trade lot
    Pos-->>Engine: TradeLot created
    Engine->>Contract: Link product reference
    Engine->>Reset: Record observation (if any)
    Engine->>CF: Create cashflow schedule
    Engine->>Kafka: Publish BusinessEvent (NEW_TRADE)
```

### 2. Conflict Resolution (Optimistic Pattern)

When Position Service detects existing position, Engine retries as QuantityChangeInstruction.

```mermaid
sequenceDiagram
    autonumber
    box rgba(255,220,200) Ingestion
        participant TC as Trade Capture
    end
    box rgba(200,255,220) Orchestration
        participant Engine as Lifecycle Engine
    end
    box rgba(220,200,255) Core Domain
        participant Pos as Position Service
    end

    TC->>Engine: ExecutionInstruction (assume new)
    Engine->>Pos: Create trade lot
    Pos-->>Engine: Conflict: position exists
    Engine->>Engine: Convert to QuantityChangeInstruction
    Engine->>Pos: Update quantity (Increase)
    Pos-->>Engine: Position updated
```

### 3. Reset + Transfer (Period-End Settlement)

```mermaid
sequenceDiagram
    autonumber
    box rgba(200,255,220) Orchestration
        participant Engine as Lifecycle Engine
    end
    box rgba(220,200,255) Core Domain
        participant Reset as Reset Service
        participant CF as Cashflow Service
        participant Pos as Position Service
    end
    box rgba(200,200,200) Messaging
        participant Kafka as Event Bus
    end

    Note over Engine: ResetInstruction + TransferInstruction
    Engine->>Reset: Record reset observation
    Reset-->>Engine: OK
    Engine->>CF: Settle cashflow
    CF->>Pos: Read settled quantity
    Pos-->>CF: Quantity, settlement date
    CF-->>Engine: OK
    Engine->>Kafka: Publish (RESET, SETTLEMENT)
```

### 4. Event-Driven Valuation Flow

```mermaid
sequenceDiagram
    autonumber
    box rgba(200,200,200) Messaging
        participant Kafka as Event Bus
    end
    box rgba(220,220,255) Analytics
        participant Val as Valuation Service
    end
    box rgba(220,200,255) Core Domain
        participant Pos as Position Service
        participant Reset as Reset Service
    end
    box rgba(255,220,220) Supporting
        participant MD as Market Data Service
    end

    Kafka->>Val: BusinessEvent (NEW_TRADE / POSITION_UPDATED)
    Val->>Pos: Get trade lots, positions
    Pos-->>Val: Lots, quantities
    Val->>Reset: Get reset history
    Reset-->>Val: Observations
    Val->>MD: Get market prices
    MD-->>Val: Prices, rates
    Val->>Val: Calculate MTM / Store snapshot
```

### 5. Cashflow Calculation (Read Across Services)

```mermaid
sequenceDiagram
    autonumber
    box rgba(220,200,255) Core Domain
        participant CF as Cashflow Service
        participant Contract as Contract Service
        participant Reset as Reset Service
        participant Pos as Position Service
    end

    CF->>Contract: Get economic terms
    Contract-->>CF: PerformancePayout, InterestRatePayout
    CF->>Pos: Get settled quantity, settlement date
    Pos-->>CF: Quantity, settlement date
    CF->>Reset: Get reset observations
    Reset-->>CF: Observations
    CF->>CF: Equity leg + Interest leg = Net cashflow
```

### 6. Authorization Flow

```mermaid
sequenceDiagram
    autonumber
    participant Client as Client
    box rgba(255,220,200) Edge
        participant GW as API Gateway
    end
    box rgba(255,255,200) IAM
        participant IAM as IAM Service
    end
    box rgba(220,200,255) Domain
        participant Svc as Domain Service
    end

    Client->>GW: Request + JWT
    GW->>IAM: Validate token + Check entitlement
    IAM-->>GW: Authorized / Denied
    alt Authorized
        GW->>Svc: Forward (user context)
        Svc-->>GW: Response
        GW-->>Client: 200 OK
    else Denied
        GW-->>Client: 403 Forbidden
    end
```

---

## Diagram Summary

| Level | Diagram | Purpose |
|-------|---------|---------|
| **1** | System Context | Shows the system boundary and external actors (users, AMS, market data, reporting) |
| **2** | Container | Shows major services (Trade Capture, Lifecycle Engine, Position, Contract, Cashflow, etc.) and their interactions |
| **3** | Component | Shows internal components of key services (Trade Capture, Lifecycle Engine, Position, Cashflow) |
| **4** | Deployment | Shows deployment topology (load balancer, app cluster, databases, messaging) |
| **—** | **Sequence** | Shows end-to-end flows with clear service boundaries (New Trade, Conflict Resolution, Reset+Transfer, Valuation, Cashflow, Authorization) |

---

## References

- [architectural_blueprint.md](architectural_blueprint.md) - Full architectural specification
- [Mermaid C4 Syntax](https://mermaid.js.org/syntax/c4.html) - C4 diagram rendering
- [C4 Model](https://c4model.com/) - C4 architecture documentation
