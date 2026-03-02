# Market Data & Reference Data Services - High-Level Architecture

## Architecture Overview

This document provides a high-level design for Market Data and Reference Data services, including file-based connectors, data quality checks, and reliability/resilience mechanisms.

---

## High-Level Architecture Diagram

```mermaid
graph TB
    subgraph "Golden Sources"
        direction TB
        subgraph "Market Data Sources"
            EQ_SRC[Equity Pricing<br/>System<br/>API/File]
            FX_SRC[FX Rates<br/>System<br/>API/File]
            IDX_SRC[Index Rates<br/>System<br/>API/File]
            DIV_SRC[Dividend<br/>Data<br/>File]
        end
        
        subgraph "Reference Data Sources"
            SEC_SRC[Security Master<br/>System<br/>API/File]
            ACC_SRC[Account Master<br/>System<br/>API/File]
            BOOK_SRC[Book Master<br/>System<br/>File]
            PARTY_SRC[Party Master<br/>System<br/>API/File]
        end
    end
    
    subgraph "Data Ingestion Layer"
        direction TB
        subgraph "Market Data Connectors"
            EQ_CONN[Equity Price<br/>Connector<br/>API/File]
            FX_CONN[FX Rate<br/>Connector<br/>API/File]
            IDX_CONN[Index Rate<br/>Connector<br/>API/File]
            DIV_CONN[Dividend<br/>Connector<br/>File]
        end
        
        subgraph "Reference Data Connectors"
            SEC_CONN[Security<br/>Connector<br/>API/File]
            ACC_CONN[Account<br/>Connector<br/>API/File]
            BOOK_CONN[Book<br/>Connector<br/>File]
            PARTY_CONN[Party<br/>Connector<br/>API/File]
        end
        
        subgraph "File Processing"
            FILE_WATCH[File Watcher<br/>Service]
            FILE_PARSER[File Parser<br/>Service]
            FILE_VALIDATOR[File Format<br/>Validator]
        end
    end
    
    subgraph "Data Quality & Validation Layer"
        direction TB
        DQ_ENGINE[Data Quality<br/>Engine]
        DQ_RULES[Data Quality<br/>Rules Engine]
        DQ_MONITOR[Data Quality<br/>Monitor]
        DQ_ALERTS[Data Quality<br/>Alerting]
    end
    
    subgraph "Reliability & Resilience Layer"
        direction TB
        RETRY[Retry<br/>Mechanism]
        CIRCUIT[Circuit<br/>Breaker]
        DEAD_LETTER[Dead Letter<br/>Queue]
        BACKUP[Backup<br/>Connector]
        HEALTH[Health<br/>Check]
    end
    
    subgraph "Market Data Service"
        direction TB
        MD_PROCESSOR[Market Data<br/>Processor]
        MD_VALIDATOR[Market Data<br/>Validator]
        MD_ENRICHER[Market Data<br/>Enricher]
        MD_STORAGE[(Time-Series<br/>Storage<br/>TimescaleDB)]
        MD_CACHE[(Redis<br/>Cache)]
        MD_API[Market Data<br/>API<br/>REST/gRPC]
    end
    
    subgraph "Reference Data Service"
        direction TB
        RD_PROCESSOR[Reference Data<br/>Processor]
        RD_VALIDATOR[Reference Data<br/>Validator]
        RD_ENRICHER[Reference Data<br/>Enricher]
        RD_STORAGE[(Relational<br/>Storage<br/>PostgreSQL)]
        RD_CACHE[(Redis<br/>Cache)]
        RD_API[Reference Data<br/>API<br/>REST/gRPC]
    end
    
    subgraph "Consuming Services"
        TC[Trade Capture<br/>Service]
        LE[Lifecycle<br/>Engine]
        VAL[Valuation<br/>Service]
        CF[Cashflow<br/>Service]
        RESET[Reset<br/>Service]
    end
    
    %% Golden Sources to Connectors
    EQ_SRC -->|API/File| EQ_CONN
    FX_SRC -->|API/File| FX_CONN
    IDX_SRC -->|API/File| IDX_CONN
    DIV_SRC -->|File| DIV_CONN
    
    SEC_SRC -->|API/File| SEC_CONN
    ACC_SRC -->|API/File| ACC_CONN
    BOOK_SRC -->|File| BOOK_CONN
    PARTY_SRC -->|API/File| PARTY_CONN
    
    %% File Processing
    DIV_SRC -->|File| FILE_WATCH
    BOOK_SRC -->|File| FILE_WATCH
    FILE_WATCH --> FILE_PARSER
    FILE_PARSER --> FILE_VALIDATOR
    
    %% Connectors to Quality Layer
    EQ_CONN --> DQ_ENGINE
    FX_CONN --> DQ_ENGINE
    IDX_CONN --> DQ_ENGINE
    DIV_CONN --> DQ_ENGINE
    FILE_VALIDATOR --> DQ_ENGINE
    
    SEC_CONN --> DQ_ENGINE
    ACC_CONN --> DQ_ENGINE
    BOOK_CONN --> DQ_ENGINE
    PARTY_CONN --> DQ_ENGINE
    
    %% Quality Layer Components
    DQ_ENGINE --> DQ_RULES
    DQ_RULES --> DQ_MONITOR
    DQ_MONITOR --> DQ_ALERTS
    
    %% Reliability Layer
    EQ_CONN -.->|Failure| RETRY
    FX_CONN -.->|Failure| RETRY
    IDX_CONN -.->|Failure| RETRY
    SEC_CONN -.->|Failure| RETRY
    
    RETRY --> CIRCUIT
    CIRCUIT -->|Open| DEAD_LETTER
    CIRCUIT -->|Fallback| BACKUP
    
    HEALTH --> EQ_CONN
    HEALTH --> FX_CONN
    HEALTH --> SEC_CONN
    
    %% Quality to Services
    DQ_ENGINE -->|Valid Data| MD_PROCESSOR
    DQ_ENGINE -->|Valid Data| RD_PROCESSOR
    
    %% Market Data Service Flow
    MD_PROCESSOR --> MD_VALIDATOR
    MD_VALIDATOR --> MD_ENRICHER
    MD_ENRICHER --> MD_STORAGE
    MD_STORAGE --> MD_CACHE
    MD_CACHE --> MD_API
    
    %% Reference Data Service Flow
    RD_PROCESSOR --> RD_VALIDATOR
    RD_VALIDATOR --> RD_ENRICHER
    RD_ENRICHER --> RD_STORAGE
    RD_STORAGE --> RD_CACHE
    RD_CACHE --> RD_API
    
    %% Services to Consumers
    MD_API --> TC
    MD_API --> LE
    MD_API --> VAL
    MD_API --> RESET
    
    RD_API --> TC
    RD_API --> LE
    RD_API --> VAL
    RD_API --> CF
    
    %% Styling
    classDef goldenSource fill:#e1f5ff,stroke:#01579b,stroke-width:2px
    classDef connector fill:#fff3e0,stroke:#e65100,stroke-width:2px
    classDef quality fill:#f3e5f5,stroke:#4a148c,stroke-width:2px
    classDef resilience fill:#ffebee,stroke:#b71c1c,stroke-width:2px
    classDef service fill:#e8f5e9,stroke:#1b5e20,stroke-width:2px
    classDef storage fill:#fff9c4,stroke:#f57f17,stroke-width:2px
    classDef consumer fill:#e0f2f1,stroke:#004d40,stroke-width:2px
    
    class EQ_SRC,FX_SRC,IDX_SRC,DIV_SRC,SEC_SRC,ACC_SRC,BOOK_SRC,PARTY_SRC goldenSource
    class EQ_CONN,FX_CONN,IDX_CONN,DIV_CONN,SEC_CONN,ACC_CONN,BOOK_CONN,PARTY_CONN,FILE_WATCH,FILE_PARSER,FILE_VALIDATOR connector
    class DQ_ENGINE,DQ_RULES,DQ_MONITOR,DQ_ALERTS quality
    class RETRY,CIRCUIT,DEAD_LETTER,BACKUP,HEALTH resilience
    class MD_PROCESSOR,MD_VALIDATOR,MD_ENRICHER,RD_PROCESSOR,RD_VALIDATOR,RD_ENRICHER service
    class MD_STORAGE,MD_CACHE,RD_STORAGE,RD_CACHE storage
    class TC,LE,VAL,CF,RESET consumer
```

---

## Component Details

### 1. Data Ingestion Layer

#### File-Based Connectors

**File Watcher Service**
- Monitors file drop locations (SFTP, S3, NFS)
- Detects new files, file modifications
- Triggers file processing pipeline
- Handles file naming conventions and patterns

**File Parser Service**
- Parses various file formats (CSV, XML, JSON, Fixed-width)
- Handles encoding (UTF-8, ASCII, EBCDIC)
- Extracts data based on file schemas
- Validates file structure and format

**File Format Validator**
- Validates file headers/footers
- Checks record counts
- Validates file integrity (checksums)
- Detects file format version changes

#### API-Based Connectors

**Real-time Connectors**
- REST API connectors
- WebSocket connectors
- Message queue connectors (JMS, Kafka)
- Polling connectors (scheduled)

**Batch Connectors**
- Scheduled API calls
- Bulk data retrieval
- Incremental data sync

---

### 2. Data Quality & Validation Layer

#### Data Quality Engine

**Validation Rules**
- **Completeness**: Required fields present
- **Accuracy**: Values within expected ranges
- **Consistency**: Cross-field validation
- **Timeliness**: Data freshness checks
- **Uniqueness**: Duplicate detection
- **Format**: Data type and format validation

**Market Data Quality Rules**
```java
// Example: Equity Price Quality Rules
- Price > 0
- Price within reasonable range (e.g., $0.01 to $10,000)
- Currency code valid (ISO 4217)
- Date not in future
- Security ID exists in reference data
- Price change from previous day within threshold (e.g., ±50%)
```

**Reference Data Quality Rules**
```java
// Example: Security Quality Rules
- ISIN format valid (12 characters, alphanumeric)
- CUSIP format valid (9 characters)
- Security name not empty
- Currency code valid
- Security type valid
- No duplicate identifiers
```

#### Data Quality Monitor

**Real-time Monitoring**
- Tracks data quality metrics
- Generates quality scores
- Identifies quality trends
- Tracks quality degradation

**Quality Metrics**
- **Completeness Rate**: % of records with all required fields
- **Accuracy Rate**: % of records passing validation rules
- **Timeliness**: Average delay from source to service
- **Error Rate**: % of records with quality issues

#### Data Quality Alerting

**Alert Types**
- **Critical**: Data quality below threshold (e.g., < 95%)
- **Warning**: Quality degradation detected
- **Info**: Quality issues resolved

**Alert Channels**
- Email notifications
- Slack/Teams integration
- PagerDuty integration
- Dashboard alerts

---

### 3. Reliability & Resilience Layer

#### Retry Mechanism

**Retry Strategy**
- **Exponential Backoff**: Increasing delay between retries
- **Max Retries**: Configurable per connector
- **Retryable Errors**: Network errors, timeouts, temporary failures
- **Non-Retryable Errors**: Authentication failures, invalid data

**Implementation**
```java
@Retryable(
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000, multiplier = 2)
)
public List<Price> fetchEquityPrices(LocalDate date) {
    // Fetch from golden source
}
```

#### Circuit Breaker

**Circuit States**
- **Closed**: Normal operation, requests pass through
- **Open**: Failures detected, requests fail fast
- **Half-Open**: Testing if service recovered

**Circuit Breaker Configuration**
- **Failure Threshold**: % of requests that must fail to open circuit
- **Timeout**: Duration before attempting half-open
- **Success Threshold**: % of requests that must succeed to close circuit

**Implementation**
```java
@CircuitBreaker(
    name = "equityPriceConnector",
    fallbackMethod = "fallbackFetchEquityPrices"
)
public List<Price> fetchEquityPrices(LocalDate date) {
    // Fetch from golden source
}

public List<Price> fallbackFetchEquityPrices(LocalDate date) {
    // Fallback to backup source or cached data
}
```

#### Dead Letter Queue

**Purpose**
- Stores failed messages that cannot be processed
- Enables manual investigation and reprocessing
- Prevents data loss

**DLQ Storage**
- Database table for failed records
- Message queue (Kafka DLQ topic)
- File system (failed files archive)

#### Backup Connector

**Backup Sources**
- Secondary data providers
- Historical data cache
- Last known good values
- Default values (with alerting)

**Fallback Strategy**
```java
public List<Price> fetchEquityPricesWithFallback(LocalDate date) {
    try {
        return primaryConnector.fetch(date);
    } catch (Exception e) {
        log.warn("Primary connector failed, using backup", e);
        return backupConnector.fetch(date);
    }
}
```

#### Health Check

**Health Endpoints**
- **Liveness**: Service is running
- **Readiness**: Service can accept requests
- **Health**: Detailed health status

**Health Checks**
- Connector connectivity
- Database connectivity
- Cache connectivity
- Data freshness
- Quality metrics

---

### 4. Market Data Service

#### Market Data Processor

**Responsibilities**
- Receives validated data from connectors
- Transforms data to internal format
- Handles data normalization
- Manages data versioning

**Processing Flow**
1. Receive raw data from connector
2. Transform to internal format
3. Apply business rules
4. Enrich with metadata
5. Store in time-series database

#### Market Data Validator

**Validation Checks**
- Price ranges
- Currency codes
- Date validity
- Security ID existence
- Data type validation

#### Market Data Enricher

**Enrichment**
- Add metadata (source, timestamp, quality flags)
- Calculate derived fields (returns, changes)
- Link to reference data (security details)
- Add audit information

#### Storage Layer

**Time-Series Database (TimescaleDB)**
- Optimized for time-series data
- Automatic partitioning by time
- Efficient range queries
- Compression for historical data

**Schema**
```sql
CREATE TABLE equity_prices (
    time TIMESTAMPTZ NOT NULL,
    security_id VARCHAR(255) NOT NULL,
    price DECIMAL(20, 8) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    source VARCHAR(100),
    quality_flag VARCHAR(50),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (time, security_id)
);

SELECT create_hypertable('equity_prices', 'time');
```

#### Cache Layer

**Redis Cache**
- Latest prices (TTL: 1 hour)
- Frequently accessed securities
- Bulk query results
- Rate limit counters

**Cache Strategy**
- **Write-through**: Write to DB and cache simultaneously
- **Write-behind**: Write to cache, async write to DB
- **Cache-aside**: Application manages cache

#### API Layer

**REST API**
- Standard HTTP/REST endpoints
- JSON responses
- Pagination support
- Filtering and sorting

**gRPC API**
- High-performance binary protocol
- Streaming support
- Strong typing
- Low latency

---

### 5. Reference Data Service

#### Reference Data Processor

**Responsibilities**
- Receives validated data from connectors
- Handles full sync and incremental sync
- Manages data relationships
- Tracks data changes

**Processing Flow**
1. Receive raw data from connector
2. Detect changes (new, updated, deleted)
3. Validate relationships
4. Enrich with metadata
5. Store in relational database

#### Reference Data Validator

**Validation Checks**
- Identifier format (ISIN, CUSIP, etc.)
- Required fields present
- Relationship integrity
- Business rule validation
- Duplicate detection

#### Reference Data Enricher

**Enrichment**
- Add metadata (source, timestamp, version)
- Link relationships (account → books)
- Calculate derived fields
- Add audit information

#### Storage Layer

**Relational Database (PostgreSQL)**
- Normalized schema
- Foreign key relationships
- ACID transactions
- Full-text search support

**Schema**
```sql
CREATE TABLE securities (
    security_id VARCHAR(255) PRIMARY KEY,
    isin VARCHAR(12),
    cusip VARCHAR(9),
    ticker VARCHAR(20),
    security_name VARCHAR(500),
    currency VARCHAR(3),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_securities_isin ON securities(isin);
CREATE INDEX idx_securities_ticker ON securities(ticker);
```

#### Cache Layer

**Redis Cache**
- Frequently accessed securities (TTL: 24 hours)
- Account hierarchies
- Book mappings
- Bulk query results

**Cache Strategy**
- **Cache-aside**: Application manages cache
- **Write-through**: Write to DB and cache
- **Invalidation**: On data updates

#### API Layer

**REST API**
- CRUD operations
- Search and filtering
- Relationship queries
- Bulk operations

**gRPC API**
- High-performance lookups
- Streaming support
- Strong typing
- Low latency

---

## Data Flow Examples

### Example 1: File-Based Market Data Ingestion

```mermaid
sequenceDiagram
    participant GS as Golden Source<br/>(File System)
    participant FW as File Watcher
    participant FP as File Parser
    participant DQ as Data Quality Engine
    participant MD as Market Data Service
    participant DB as TimescaleDB
    participant Cache as Redis Cache
    participant API as Market Data API
    participant Consumer as Consuming Service
    
    GS->>FW: New file detected
    FW->>FP: Trigger file processing
    FP->>FP: Parse file (CSV/XML)
    FP->>DQ: Raw data
    DQ->>DQ: Validate data quality
    DQ->>DQ: Apply quality rules
    alt Data Quality Pass
        DQ->>MD: Valid data
        MD->>MD: Process & enrich
        MD->>DB: Store in time-series DB
        MD->>Cache: Cache latest prices
        MD->>API: Data available
        Consumer->>API: Query price
        API->>Cache: Check cache
        Cache-->>API: Return cached price
        API-->>Consumer: Price data
    else Data Quality Fail
        DQ->>DQ: Generate alert
        DQ->>DLQ: Send to dead letter queue
    end
```

### Example 2: API-Based Reference Data Sync

```mermaid
sequenceDiagram
    participant GS as Golden Source<br/>(API)
    participant Connector as Reference Data Connector
    participant Retry as Retry Mechanism
    participant Circuit as Circuit Breaker
    participant DQ as Data Quality Engine
    participant RD as Reference Data Service
    participant DB as PostgreSQL
    participant Cache as Redis Cache
    participant API as Reference Data API
    participant Consumer as Consuming Service
    
    Connector->>GS: API call (fetch securities)
    alt API Success
        GS-->>Connector: Security data
        Connector->>DQ: Raw data
        DQ->>DQ: Validate data quality
        alt Data Quality Pass
            DQ->>RD: Valid data
            RD->>RD: Process & enrich
            RD->>DB: Store in relational DB
            RD->>Cache: Invalidate cache
            RD->>API: Data available
            Consumer->>API: Query security
            API->>Cache: Check cache
            alt Cache Hit
                Cache-->>API: Return cached data
            else Cache Miss
                API->>DB: Query database
                DB-->>API: Security data
                API->>Cache: Update cache
            end
            API-->>Consumer: Security data
        else Data Quality Fail
            DQ->>DQ: Generate alert
            DQ->>DLQ: Send to dead letter queue
        end
    else API Failure
        Connector->>Retry: Retry request
        Retry->>Circuit: Check circuit state
        alt Circuit Closed
            Circuit->>GS: Retry API call
        else Circuit Open
            Circuit->>Backup: Use backup connector
            Backup-->>Connector: Fallback data
        end
    end
```

---

## Key Design Principles

### 1. Separation of Concerns
- **Market Data Service**: Time-series data, frequent updates
- **Reference Data Service**: Relational data, infrequent updates

### 2. Reliability
- **Retry Mechanism**: Handle transient failures
- **Circuit Breaker**: Prevent cascading failures
- **Dead Letter Queue**: Capture failed messages
- **Backup Connectors**: Fallback data sources

### 3. Data Quality
- **Validation Rules**: Comprehensive quality checks
- **Quality Monitoring**: Real-time quality metrics
- **Alerting**: Proactive quality issue detection

### 4. Resilience
- **Health Checks**: Monitor service health
- **Graceful Degradation**: Fallback mechanisms
- **Error Handling**: Comprehensive error management

### 5. Performance
- **Caching**: Reduce database load
- **Bulk Operations**: Efficient batch processing
- **Optimized Storage**: Specialized databases

### 6. Scalability
- **Horizontal Scaling**: Multiple service instances
- **Partitioning**: Data partitioning strategies
- **Load Balancing**: Distribute requests

---

## Deployment Architecture

```mermaid
graph TB
    subgraph "Load Balancer"
        LB[Load Balancer<br/>NGINX/HAProxy]
    end
    
    subgraph "Market Data Service Cluster"
        MD1[Market Data<br/>Service<br/>Instance 1]
        MD2[Market Data<br/>Service<br/>Instance 2]
        MD3[Market Data<br/>Service<br/>Instance N]
    end
    
    subgraph "Reference Data Service Cluster"
        RD1[Reference Data<br/>Service<br/>Instance 1]
        RD2[Reference Data<br/>Service<br/>Instance 2]
        RD3[Reference Data<br/>Service<br/>Instance N]
    end
    
    subgraph "Storage Layer"
        TSDB[(TimescaleDB<br/>Primary)]
        TSDB_REPLICA[(TimescaleDB<br/>Replica)]
        PG[(PostgreSQL<br/>Primary)]
        PG_REPLICA[(PostgreSQL<br/>Replica)]
        REDIS[(Redis<br/>Cluster)]
    end
    
    LB --> MD1
    LB --> MD2
    LB --> MD3
    LB --> RD1
    LB --> RD2
    LB --> RD3
    
    MD1 --> TSDB
    MD2 --> TSDB
    MD3 --> TSDB
    MD1 --> REDIS
    MD2 --> REDIS
    MD3 --> REDIS
    
    RD1 --> PG
    RD2 --> PG
    RD3 --> PG
    RD1 --> REDIS
    RD2 --> REDIS
    RD3 --> REDIS
    
    TSDB --> TSDB_REPLICA
    PG --> PG_REPLICA
```

---

## Monitoring & Observability

### Metrics
- **Ingestion Metrics**: Records processed, errors, latency
- **Quality Metrics**: Quality scores, validation failures
- **Performance Metrics**: API latency, throughput, cache hit rate
- **Reliability Metrics**: Circuit breaker state, retry counts, DLQ size

### Logging
- **Structured Logging**: JSON format, correlation IDs
- **Log Levels**: DEBUG, INFO, WARN, ERROR
- **Log Aggregation**: Centralized log management (ELK, Splunk)

### Tracing
- **Distributed Tracing**: Request tracing across services
- **Performance Tracing**: Identify bottlenecks
- **Error Tracing**: Track error propagation

---

## Summary

This architecture provides:

1. **Separation**: Market Data and Reference Data as separate services
2. **File Support**: File-based connectors with file processing pipeline
3. **Data Quality**: Comprehensive quality checks and monitoring
4. **Reliability**: Retry, circuit breaker, dead letter queue, backup connectors
5. **Resilience**: Health checks, graceful degradation, error handling
6. **Performance**: Caching, optimized storage, bulk operations
7. **Scalability**: Horizontal scaling, load balancing, partitioning

The architecture is designed to handle both file-based and API-based data sources, with robust data quality checks and reliability/resilience mechanisms in place.

