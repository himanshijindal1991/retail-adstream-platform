# Retail AdStream — Real-Time Retail Advertising Analytics Platform

## 1. Project Overview

Retail AdStream is a **proposed** event-driven analytics platform for retail advertising networks such as Target Roundel, Walmart Connect, and Amazon Ads. It captures ad impressions, clicks, product views, cart actions, and purchases, and provides near-real-time campaign insights to authorized retailers and marketers.

**Business questions:** How many unique customers saw or clicked a campaign? How many added an eligible product to their basket after clicking? How do these metrics change over time?

**Scope:** The high-level and deployment architectures are design proposals. The working code in [`insights-api/`](insights-api/) demonstrates the reporting API using Spring Boot and sample pre-aggregated data in H2; the full Kafka/Flink pipeline and production security are not implemented end-to-end.

## 2. High-Level Architecture

![High-Level Architecture](docs/high-level-architecture.png)

[View editable architecture in Excalidraw](https://excalidraw.com/#json=is2KS1DHUmoRiu9BU8zgO,cRjW5l8Kz5v2Tb7uexFYjA)

Retail AdStream uses an event-driven architecture to capture retail advertising interactions and provide near-real-time campaign insights.

**End-to-end flow:** Retail Website / App → API Gateway → Ad Placement / Click Tracking / Event Ingestion → Kafka → Flink → PostgreSQL → Insights API → Marketers and Retailers.

**Supporting components:** Redis handles short-lived tracking metadata; S3-compatible storage archives raw events and Flink checkpoints; reconciliation jobs correct historical aggregates. Tenant isolation, security, monitoring, and fault tolerance span the architecture.

### Detailed HLD Walkthrough

**[Read the step-by-step High-Level Architecture Walkthrough](docs/HLD-DETAILED-WALKTHROUGH.md)**

The walkthrough explains each component, Kafka topics, both Flink processors, Target/Walmart examples, click-to-basket attribution, historical queries, security, reliability, and architecture trade-offs.

> **Implementation note:** The HLD is a proposed production design. The working `insights-api` module uses sample aggregated metrics in H2; the Kafka/Flink pipeline and production security are not implemented end-to-end.

## 3. Technology Choices

| Component | Technology | Purpose |
|---|---|---|
| Backend services | Java + Spring Boot | REST APIs, validation, business logic |
| Edge | Load Balancer / API Gateway | Routing, TLS, token validation, rate limiting |
| Cache | Redis | Impression metadata, token lookup, idempotency TTL |
| Event streaming | Apache Kafka | Durable partitioned event logs and replay |
| Stream processing | Apache Flink | Stateful deduplication, distinct counting, attribution |
| Serving database | PostgreSQL | Indexed pre-aggregated campaign metrics |
| Historical storage | S3-compatible object storage | Raw archive, checkpoints, backfill |
| Containerization / orchestration | Docker / Kubernetes | Packaging, rollout, horizontal scaling |
| CI/CD | GitHub + Jenkins | Build, test, publish and deploy |
| Observability | Prometheus, Grafana, OpenTelemetry, ELK/OpenSearch, Alertmanager | Metrics, traces, logs, alerts |
| Possible future OLAP | ClickHouse | Large analytical scans and complex historical reporting |

## 4. Deployment Architecture

![Deployment Architecture](docs/deployment-architecture.png)

**Proposed deployment flow:** Developer → GitHub → Jenkins build/tests → Docker image → Container Registry → Kubernetes rolling deployment. API Gateway/Load Balancer routes to independently scalable Ad Placement, Click Tracking, Event Ingestion, and Insights API pods. Kafka runs as a replicated broker cluster; Flink runs stateful processing jobs; PostgreSQL uses a primary/standby topology; Redis and object storage support tracking and recovery. Prometheus/Grafana/Alertmanager provide operational visibility.

**Reliability plan:** Kafka replication factor 3; Flink checkpointing to S3 and automatic restart; deterministic PostgreSQL UPSERTs; Kafka archival to S3 and batch reconciliation; readiness/liveness probes and controlled rollbacks. The diagram is a **deployment proposal**, not evidence that all infrastructure is running.

## 5. Insights API — Working Implementation

The implementation is in [`insights-api/`](insights-api/).

| Method | Endpoint | Meaning |
|---|---|---|
| GET | `/ad/{campaignId}/clicks` | Stored click metric for a tenant/campaign |
| GET | `/ad/{campaignId}/impressions` | Stored impression metric for a tenant/campaign |
| GET | `/ad/{campaignId}/clickToBasket` | Stored attributed cart metric for a tenant/campaign |

**Stack:** Java, Spring Boot, Spring Data JPA/Hibernate, H2 local demo database, Maven, JUnit/Mockito, Swagger/OpenAPI. PostgreSQL is the proposed production database.

**Run locally** (with a compatible Java JDK and Maven wrapper):

```bash
cd insights-api
./mvnw spring-boot:run
```

**Example:**

```bash
curl -H "X-Tenant-Id: T001" http://localhost:8080/ad/CAMP101/clicks
```

```json
{
  "tenantId": "T001",
  "campaignId": "CAMP101",
  "clicks": 12000
}
```

This `12000` is **sample data loaded by `DataInitializer.java` into H2**, not a value computed by a running Flink job. The demo repository filters by tenant/campaign and sums stored window counts; it does not itself guarantee distinct customers over arbitrary time ranges.

**Run tests:** `cd insights-api && ./mvnw clean test` (from the repository root). Swagger UI is configured for local API exploration.

## 6. Implementation Scope & Limitations

**Implemented in the demo:** Three Insights API endpoints; controller/service/repository/entity separation; H2 sample data; tenant-scoped repository queries; missing-campaign error handling; automated tests; API documentation.

**Proposed, not implemented end-to-end:** Ad Placement, Click Tracking, Event Ingestion, Kafka topics, Flink jobs, Redis, PostgreSQL production deployment, S3 archival/reconciliation, JWT/RBAC, Kubernetes/Jenkins operations, historical `from`/`to` query handling.

**Accuracy limitation:** Summing window-level unique counts can double-count customers across windows. Exact arbitrary historical distinct counts need a suitable customer-level aggregation or recomputation strategy. Late-event and correction behavior must be defined in the production implementation.

## 7. Future Enhancements

1. Connect real Kafka/Flink event streams to PostgreSQL aggregates.
2. Add authenticated tenant context and Spring Security authorization.
3. Implement historical date-range queries with a correct distinct-count contract.
4. Add integration, load, failure-recovery, and tenant-isolation tests.
5. Automate archival, reconciliation, CI/CD, and monitoring.
6. Evaluate ClickHouse if complex analytical reporting outgrows PostgreSQL serving queries.
