# 📊 Real-Time Market Data Candle Aggregation System

A high-performance backend system that:

-   Consumes real-time bid/ask tick data
-   Aggregates ticks into OHLC candlesticks
-   Stores historical candle data efficiently
-   Publishes aggregated candles
-   Exposes history APIs for frontend charting systems

Designed for trading dashboards, TradingView Lightweight Charts, quant
platforms, and real-time analytics systems.

------------------------------------------------------------------------

# 🏗 Architecture Overview

Tick Producers → Kafka → Aggregator Service → QuestDB → History API →
Frontend

------------------------------------------------------------------------

# 🧩 Services

## 1️⃣ Aggregator Service

Responsible for:

-   Consuming tick stream from Kafka
-   Aggregating into OHLC candles
-   Handling multiple symbols and intervals
-   Writing to QuestDB
-   Publishing finalized candles to Kafka
-   Exposing metrics via Actuator + OpenTelemetry

Stateless processing logic with partition-based sharding.

------------------------------------------------------------------------

## 2️⃣ History API Service (separate service)

Reads historical candle data from QuestDB and exposes:

GET api/v1/history/candles?symbol=ETH-USD&interval=1m&from=1772299670&to=1772474376&limit=100

Separation ensures ingestion and read workloads scale independently.

------------------------------------------------------------------------

# 🧠 Design Decisions

## Why Kafka?

-   High-throughput streaming backbone
-   Partition-based horizontal scaling
-   Ordering guarantees per symbol
-   Consumer group parallelism
-   Durable log storage

Partition key = symbol\
Ensures in-order processing without locks.

------------------------------------------------------------------------

## Why QuestDB?

-   Optimized for time-series data
-   Extremely fast ingestion
-   Efficient range queries
-   Lightweight deployment
-   SQL support

Ideal for OHLC historical storage.

------------------------------------------------------------------------

# 🏎 Performance Design

## Sharded Aggregation Engine

Each Kafka partition maps to a shard:

ShardState\
├── CandleShard (aggregation engine)\
└── QuestDbSink

Benefits:

-   No shared mutable state
-   No locks
-   Linear scalability
-   Thread affinity per partition

------------------------------------------------------------------------

## Watermark-Based Finalization

Uses:

maxSeenSec - allowedLateness

Allows handling of out-of-order ticks safely.

------------------------------------------------------------------------

## Multi-Interval Aggregation

Single tick updates multiple intervals:

-   1s
-   5s
-   1m
-   15m
-   1h

Efficient bucket management prevents object explosion.

------------------------------------------------------------------------

# 📈 Scalability Model

## Horizontal Scaling

Increase Kafka partitions and run multiple aggregator-service instances.

Kafka handles partition rebalancing automatically.

------------------------------------------------------------------------

# 🧪 Observability & Monitoring

## Spring Boot Actuator

Actuator is enabled on port 9081.

Base URL:

http://localhost:9081/actuator

Available endpoints:

-   /actuator/health
-   /actuator/metrics
-   /actuator/info
-   /actuator/beans

### Check Health

curl http://localhost:9081/actuator/health

### View Metrics List

curl http://localhost:9081/actuator/metrics

### View Specific Metric

curl http://localhost:9081/actuator/metrics/jvm.memory.used

------------------------------------------------------------------------

## Prometheus Metrics

Metrics pipeline:

Spring Boot → OTLP → OpenTelemetry Collector → Prometheus → Grafana

Prometheus UI:

http://localhost:9090/query?g0.expr=candle_candles_finalized_total&g0.show_tree=0&g0.tab=graph&g0.range_input=1h&g0.res_type=fixed&g0.res_step=10&g0.display_mode=lines&g0.show_exemplars=0

Useful queries:

jvm_memory_used_bytes\
process_cpu_usage\
http_server_requests_seconds_count

Custom metrics example:

candle_candles_finalized_total
rate(aggregator_ticks_total\[1m\])


curl http://localhost:9464/metrics (optl)

------------------------------------------------------------------------

# 🗄 How to Check QuestDB Data

## Open QuestDB Web Console

http://localhost:9000

## List Tables

SHOW TABLES;

## Check Candle Data

SELECT count(), min(timestamp), max(timestamp) FROM candles;

## View Latest Candles

SELECT \* FROM candles ORDER BY timestamp DESC LIMIT 20;

## Filter by Symbol and Interval

SELECT \* FROM candles WHERE symbol = 'BTC-USD' AND interval = '1m'
ORDER BY timestamp DESC LIMIT 100;

------------------------------------------------------------------------

# ⚙️ Tech Stack

Technology       Purpose
  ---------------- --------------------------------------
- Java 25          Modern concurrency (Virtual Threads)
- Spring Boot 4    Framework
- Kafka            Streaming backbone
- QuestDB          Time-series storage
- Micrometer       Metrics
- OpenTelemetry    Observability
- Prometheus       Metrics storage
- Grafana          Visualization
- Log4j2 Async     High-performance logging
- Docker Compose   Local orchestration

------------------------------------------------------------------------

# 🚀 How to Run

1.  Start infrastructure: 
```bash
docker compose up -d
```

2.  Run application:

```bash
mvn clean package 
java -jar target/aggregator-service-0.0.1-SNAPSHOT.jar
```

3.  Access:

- Actuator: http://localhost:9081/actuator\
- Prometheus: http://localhost:9090\
- Grafana: http://localhost:3000\
- QuestDB: http://localhost:9000

------------------------------------------------------------------------

# 🏗 Clean Separation of Concerns

Layer            Responsibility
  ---------------- -----------------------
- TickSource       Ingestion abstraction
- TickProcessor    Business processing
- CandleShard      Aggregation engine
- CandleSink       Persistence
- KafkaPublisher   Distribution
- History API      Read side

Candle logic is fully decoupled from ingestion source.

------------------------------------------------------------------------

# 🎯 Summary

This system is:

-   Event-driven
-   Horizontally scalable
-   Lock-free per shard
-   Time-series optimized
-   Production observable
-   Cleanly separated between write and read models

Designed to resemble real-world exchange-grade backend architecture.

------------------------------------------------------------------------


# history-api-service

A lightweight **read/query API** for candle data produced by `aggregator-service` and stored in **QuestDB**.  
It exists to keep the write-heavy aggregation pipeline isolated from read-heavy API traffic, and to provide a clean interface for dashboards, UIs, and other services.

---

## Why a separate service?

### 1) Workload isolation (writes vs reads)
`aggregator-service` is optimized for:
- high-throughput tick ingestion (Kafka consumers)
- candle aggregation (in-memory)
- fast writes to QuestDB (ILP)

`history-api-service` is optimized for:
- ad-hoc queries (filtering, paging, sorting)
- REST/HTTP concerns (timeouts, validation)
- API schema & documentation (Swagger/OpenAPI)

Separating the services means a spike in API traffic **does not** slow down candle ingestion, and a spike in ticks **does not** degrade API latency.

### 2) Independent scaling
You can scale them independently:
- `aggregator-service`: scale by Kafka partitions / consumer concurrency and shard count
- `history-api-service`: scale by HTTP load (replicas behind a load balancer)

### 3) Cleaner boundaries
- Aggregator emits facts: *finalized candles*
- History API serves facts: *query candles*

This boundary keeps the candle calculation logic decoupled from API concerns.

---

## What this service does

Typical responsibilities:
- Exposes REST endpoints to query candle history by:
  - `symbol`
  - `interval` (e.g., `1s`, `5s`, `1m`, `15m`, `1h`)
  - time window (`from`, `to`)
- Provides pagination/limits
- Validates inputs and returns consistent error responses
- Publishes OpenAPI docs via Swagger UI
- Exposes health/metrics endpoints (Actuator), ready for Prometheus + Grafana

---

## Architecture

```
Clients (UI / dashboards / other services)
          |
          v
  history-api-service  (HTTP REST + Swagger + Actuator)
          |
          v
       QuestDB (candles table)
          ^
          |
  aggregator-service (Kafka -> aggregate -> QuestDB + Kafka publish)
```

---

## Tech stack (typical)

- **Spring Boot 4** (web + actuator)
- **Java 25/26** (virtual threads optional)
- **QuestDB** as the time-series store
- **Micrometer** + **Actuator** for metrics
- **springdoc-openapi** for Swagger UI

---

## Prerequisites

- Java 25+ installed (or use Docker)
- Maven (if running locally)
- Docker + Docker Compose (recommended)
- QuestDB running (local or Docker)

---

## Run with Docker Compose (recommended)

If your repository already has a compose file that starts QuestDB, reuse it.
Otherwise, this is a minimal example:

```yaml
services:
  questdb:
    image: questdb/questdb:latest
    container_name: questdb
    ports:
      - "9000:9000"   # Web console
      - "8812:8812"   # Postgres wire
      - "9009:9009"   # ILP (if used)
```

Start:

```bash
docker compose up -d 
```

---

## Run locally (Maven)

From the `history-api-service/` directory:

```bash
mvn clean spring-boot:run  OR java -jar target/history-api-service-0.0.1-SNAPSHOT.jar
```

By default the service should be available at:

- API base: `http://localhost:8082`
- Swagger UI: `http://localhost:8082/swagger-ui/index.html`

> If your port is different, check `server.port` in `application.yml`.

---

## Swagger / OpenAPI

Swagger UI:

- `http://localhost:8082/swagger-ui/index.html`

OpenAPI JSON (commonly one of these depending on springdoc config):

- `http://localhost:8082/api-docs`
- `http://localhost:8082/api-docs.yaml`

If Swagger does not load:
1. Confirm `springdoc-openapi-starter-webmvc-ui` dependency exists.
2. Confirm your app is actually running on `8082`.
3. Check logs for a missing Jackson module (common when mixing Jackson versions).

---

## How to access QuestDB and verify data

### 1) QuestDB Web Console
Open:

- `http://localhost:9000`

Run:

```sql
SELECT count(), min(timestamp), max(timestamp) FROM candles;
```

To inspect recent candles:

```sql
SELECT * FROM candles
WHERE symbol = 'BTC-USD' AND interval = '1s'
ORDER BY timestamp DESC
LIMIT 50;
```

### 2) Postgres wire (optional)
QuestDB supports Postgres wire protocol on `8812`.

Example (psql):

```bash
psql -h localhost -p 8812 -U admin -d qdb
```

---

## Actuator (health & metrics)

If actuator is enabled, typical endpoints:

- Base: `http://localhost:8082/actuator`
- Health: `http://localhost:8082/actuator/health`
- Metrics: `http://localhost:8082/actuator/metrics`


## Example query patterns (API)

Exact routes depend on your controller design, but common patterns are:

### History API
```
GET http://localhost:8082/api/v1/history/candles?symbol=ETH-USD&interval=1m&from=1772299670&to=1872299690&limit=100
```

## Scaling notes

- **QuestDB**: reads scale well when queries are time-bounded and include limits.
- **history-api-service**:
  - is stateless → scale horizontally (multiple replicas)
  - put a load balancer in front
  - add caching for “latest” endpoints if needed
- Keep `aggregator-service` isolated so ingestion is never blocked by slow queries.

---

## Operational notes / good production defaults

Recommended:
- Request timeouts and max rows (to prevent runaway queries)
- Input validation (interval whitelist, symbol format, max time window)
- Rate limiting / auth (if exposed publicly)
- Separate management port for actuator (optional)

---

## Troubleshooting

### Swagger page loads but no endpoints appear
- Ensure your controllers are picked up by component scanning.
- Ensure you’re using the correct springdoc starter for Spring MVC (`webmvc`) or WebFlux (`webflux`).

### Queries are slow
- Make sure queries include a time range.
- Add a hard `LIMIT`.
- Consider pre-aggregated tables/materialized views for large ranges.

### No data returned
- Verify `candles` table contains rows in QuestDB:
  ```sql
  SELECT count() FROM candles;
  ```
- Verify symbols/intervals match what `aggregator-service` writes (case-sensitive).

---

## Related services

- **aggregator-service**  
  Ingests ticks from Kafka, aggregates OHLCV candles, writes to QuestDB, and optionally republishes candles to Kafka.

- **history-api-service**  
  Serves candle data over HTTP for dashboards/clients.



#### Note: Additional unit test scenarios can be added, Due to time constraints, I covered the minimum.