# Retail AdStream — Detailed High-Level Architecture Walkthrough

> **Document purpose:** Explain the proposed real-time retail advertising analytics architecture, component by component, using practical examples. This is a **system design proposal**, not a claim that the entire pipeline is deployed. The working code in `insights-api/` is a Spring Boot reporting API backed by sample H2 data.

[← Back to main README](../README.md)

![High-Level Architecture](high-level-architecture.png)

## 1. Business problem and expected outcome

Retail advertising networks such as Target Roundel and Walmart Connect serve advertisements while customers browse and shop. Advertisers need to understand **how many distinct customers viewed an ad, clicked it, and added an eligible product to a basket after clicking**. These metrics should be available quickly, while historical data must remain available for analysis and correction.

**Running example:** Target serves a Nike ad under campaign `CAMP101`. Customer `U1` views the ad, clicks it, and later adds the advertised Nike shoes to their cart. The platform must track these as separate events, attribute the cart action correctly, and expose campaign metrics through APIs.

### End-to-end flow at a glance

```text
Retail Website / App
   |
   v
Load Balancer + API Gateway
   |                     
   +--> Ad Placement Service --> Redis (impression metadata)
   |
   +--> Click Tracking Service --> Event Ingestion API
   |                                  |
   +--> Event Ingestion API ----------+--> Kafka
                                           |-- ad-events ----------> Flink Ad Analytics
                                           |                         (unique views/clicks)
                                           |-- customer-events --+-> Flink Customer Journey
                                           |                     |   (click-to-basket)
                                           +-- ad-events clicks -+
                                                                    |
                                                                    v
                                                        PostgreSQL Aggregates
                                                                    |
                                                                    v
                                                             Insights API
                                                                    |
                                                                    v
                                                        Marketers / Dashboards

Kafka -- archival sink --> S3-compatible Object Storage
Flink -- checkpoints --> S3-compatible Object Storage
S3 raw events --> Reconciliation Worker --> PostgreSQL corrections
```

**Note:** The two Flink boxes represent **logical processing responsibilities**; they need not be two separate physical clusters. The diagram shows the main data paths, not every control-plane connection.

## 2. Entry point: Retail website, load balancer, and API gateway

**What happens:** The website or mobile app requests an ad, reports an impression when the ad is actually displayed, and sends subsequent click and shopping events. The load balancer routes requests to healthy services. The gateway can enforce TLS, request routing, throttling, and identity checks appropriate to each endpoint.

**Why:** The services should not each implement their own public-facing traffic controls. A shared gateway makes routing and policy enforcement consistent.

**Security distinction:** Public ad tracking can use signed, expiring tokens without forcing shoppers to log in. Advertiser reporting endpoints should require authenticated access (for example OAuth2/OIDC JWTs). A gateway may validate a token, but the Insights API must still enforce tenant authorization.

## 3. Ad Placement Service — deciding what to show

1. The website requests an advertisement.
2. The **Ad Placement Service (Spring Boot)** selects an eligible ad using ad metadata from the proposed Ad DB.
3. It generates a unique `impressionId` and stores short-lived impression metadata in **Redis** (tenant, campaign, ad, destination, expiry).
4. It returns the advertisement and a **signed tracking URL** to the website.

**Example:** Target requests an ad for a Nike placement. The service returns `CAMP101` and a tracking token associated with impression `IMP1001`.

**Important accuracy rule:** Generating an impression ID does **not** mean the ad was viewed. An `IMPRESSION` event should be emitted when the defined viewability/display rule is satisfied. The precise viewability rule is a business decision.

**Why Redis:** Tracking metadata is short-lived and needs fast lookups during click redirects. TTL expires old entries automatically.

## 4. Click Tracking Service — recording engagement safely

1. The customer clicks the tracked advertisement URL, such as `GET /click/{trackingToken}`.
2. The **Click Tracking Service** verifies the token signature and expiry.
3. It fetches impression metadata from Redis and checks the destination against an allowed destination policy to avoid unsafe redirects.
4. It performs an idempotency check for repeated **delivery of the same click event**.
5. It sends a `CLICK` event to the Event Ingestion API and waits for the agreed durable-acceptance acknowledgment before redirecting.
6. It returns an HTTP **302 redirect** to the permitted product destination.

**Important:** Two intentional clicks by the same customer may be two legitimate click events. Event deduplication removes repeated delivery of the *same* event ID; distinct-customer reporting later counts that customer once per reporting window.

**Trade-off:** Waiting for durable acknowledgment improves tracking reliability but adds redirect latency. Production design should define a timeout and failure policy.

## 5. Unified Event Ingestion API — validating and routing events

**Endpoint (proposed):** `POST /events/ingest`

The Spring Boot ingestion service accepts supported event types:

- `IMPRESSION` and `CLICK` from advertising interactions.
- `PRODUCT_VIEW`, `ADD_TO_CART`, and `ORDER_PURCHASE` from shopping activity.

It validates the event schema and timestamp, authenticates trusted producers where applicable, checks that the producer is allowed to submit for the tenant, enriches fields as needed, and publishes to Kafka. Redis can provide a short-lived idempotency check; Flink also needs downstream deduplication because retries and replay can still occur.

**Illustrative event:**

```json
{
  "eventId": "EVT101",
  "tenantId": "TARGET",
  "customerId": "U1",
  "campaignId": "CAMP101",
  "productId": "NIKE-SHOE-01",
  "eventType": "ADD_TO_CART",
  "eventTimestamp": "2026-10-23T10:10:00Z"
}
```

The fields shown are an **illustrative proposed event schema**, not the current `insights-api` entity. Cart events may need campaign attribution to be determined by Flink rather than supplied by the website.

## 6. Apache Kafka — buffering and separating event streams

Kafka decouples event producers from stream processors, provides partitioned high-throughput streams, and retains events for recovery and replay.

| Topic | Events | Proposed producer partition key | Why |
|---|---|---|---|
| `ad-events` | `IMPRESSION`, `CLICK` | `tenantId + adId` | Distributes ad engagement traffic by tenant and ad |
| `customer-events` | `PRODUCT_VIEW`, `ADD_TO_CART`, `ORDER_PURCHASE` | `tenantId + userId` | Preserves ordering per customer key within a partition |

**Important:** Kafka partitioning and Flink `keyBy(...)` serve different purposes. Flink can repartition ad events by `(tenantId, campaignId)` for campaign aggregation, or both streams by `(tenantId, customerId)` for customer-journey correlation. The same Kafka topic can be consumed by more than one Flink job/consumer group.

**Proposed resilience:** Three brokers, replication factor three, and configurable seven-day retention. These are target production settings, not properties of the current Java demo. Replication protects against broker failures; retention enables replay within the configured window. Raw archival in object storage provides a longer recovery horizon.

## 7. Flink Box 1 — Ad Analytics Processor

**Business question:** *How many unique customers saw or clicked each advertisement campaign?*

**Input:** Kafka `ad-events`.

1. **Deduplicate by `eventId`:** If Kafka delivers the same event twice, process it once within the deduplication retention horizon.
2. **Group by `(tenantId, campaignId)`:** Keep different retailers and campaigns separate.
3. **Use event-time windows:** For example, calculate a five-minute window based on when the action happened, not merely when Flink received it.
4. **Count distinct `customerId` per event type and window:** A customer who clicks three times counts as **one unique clicking customer** for that window.
5. **Use keyed state and TTL:** Retain the identifiers needed for deduplication and counting; configure TTL carefully so it does not expire valid state prematurely.
6. **Use watermarks and an explicit lateness policy:** Handle bounded out-of-order events; events beyond the accepted lateness threshold need a correction/reconciliation path.
7. **Publish deterministic aggregates:** Write unique impression and click counts to PostgreSQL.

### Five-row example — two tenants

All rows fall within the same five-minute reporting window.

| eventId | tenantId | campaignId | customerId | Action |
|---|---|---|---|---|
| E1 | TARGET | CAMP101 | U1 | Impression |
| E2 | TARGET | CAMP101 | U1 | Click |
| E2 | TARGET | CAMP101 | U1 | Click |
| E3 | TARGET | CAMP101 | U2 | Click |
| E4 | WALMART | CAMP101 | U3 | Impression |

- The second `E2` is a **duplicate delivery**, so Flink ignores it.
- `TARGET + CAMP101` produces **1 unique impression** (`U1`) and **2 unique clicks** (`U1`, `U2`).
- `WALMART + CAMP101` produces **1 unique impression** (`U3`) and **0 unique clicks**.
- The identical campaign ID does not mix tenants because tenant ID is part of the grouping key.

### Important historical distinct-count limitation

If U1 clicks on Monday and Tuesday, U1 counts once on **each** day, but only once across the **two-day range**. Therefore, `SUM(daily_unique_clicks)` does **not** necessarily equal unique customers for the full date range. The production design must either limit exact distinct reporting to supported precomputed windows, maintain mergeable distinct-count state/sketches (with documented accuracy), or calculate exact distinct counts from appropriately retained customer-level data. The current Java demo does not solve arbitrary-range distinct counting.

## 8. Flink Box 2 — Customer Journey Processor

**Business question:** *What did a customer do after clicking an advertisement?*

**Inputs:** Clicks from Kafka `ad-events` **and** shopping events from `customer-events`.

1. **Deduplicate by `eventId`** so retry/replay does not create false conversions.
2. **Re-key both streams by `(tenantId, customerId)`** so a customer's ad clicks and cart actions can be correlated in keyed state.
3. **Retain recent eligible click details**, including campaign, product/ad mapping, and event time.
4. **Match `ADD_TO_CART` to an eligible earlier click**, requiring the same tenant and customer, a qualifying product, and an allowed attribution window.
5. **Apply a proposed last-click rule:** If several eligible clicks exist, attribute the cart action to the most recent qualifying click.
6. **Use watermarks, keyed state, and TTL** to handle bounded out-of-order events and expire old attribution state.
7. **Count distinct attributed customers per campaign and reporting window** and write the aggregate to PostgreSQL.

### Five-row example — attribution

Assume the cart product is eligible for the clicked campaign, the events occur in timestamp order, and the attribution window is **24 hours**.

| eventId | tenantId | customerId | campaignId | Action |
|---|---|---|---|---|
| E1 | TARGET | U1 | CAMP101 | Ad Click |
| E2 | TARGET | U1 | CAMP101 | Add to Cart |
| E2 | TARGET | U1 | CAMP101 | Add to Cart |
| E3 | TARGET | U2 | CAMP101 | Add to Cart |
| E4 | WALMART | U3 | CAMP101 | Ad Click |

- U1 clicked, then added the eligible product to the cart: **count 1**.
- The repeated E2 is a duplicate delivery: **do not count again**.
- U2 added to cart without a qualifying prior click: **count 0**.
- U3 clicked but did not add to cart: **count 0**.
- **Result:** TARGET/CAMP101 = **1** unique click-to-basket customer; WALMART/CAMP101 = **0**.

**Late events:** A click at 10:00 might reach Flink after a 10:05 cart event. Event timestamps and retained state allow matching within the allowed lateness policy. Very late events are handled through correction/reconciliation, not assumed to be magically fixed by watermarks.

**Business decisions to confirm:** Attribution window length, eligible product matching, last-click versus other attribution models, and how multiple cart actions count.

## 9. PostgreSQL — low-latency metrics serving

Flink does the heavy streaming computation **before** a reporting request arrives. PostgreSQL stores smaller, pre-aggregated rows rather than forcing each API call to scan raw event history.

**Proposed logical tables:**

| Table | Example dimensions | Purpose |
|---|---|---|
| `campaign_metrics` | tenant, campaign, window | Impressions, clicks, click-to-basket aggregates |
| `ad_metrics` | tenant, ad, window | Ad-level impressions and clicks |
| `campaign_metadata` | tenant, campaign | Campaign details and status |

**Low-latency query strategy:** Composite indexes on `(tenantId, campaignId, windowStart)`, date-based partitioning where table size justifies it, and pre-aggregated five-minute/daily metrics. Queries should select only the authorized tenant and relevant campaign/time range.

**Idempotent writes:** Flink or reconciliation should use deterministic keys and UPSERTs that **replace a complete aggregate snapshot**, rather than blindly adding replayed increments. Checkpointing alone does not guarantee exactly-once writes to PostgreSQL.

**Database choice:** PostgreSQL is a good serving store for indexed lookups on prepared aggregates. ClickHouse is a **future OLAP option** if requirements expand to complex, high-cardinality historical analysis over very large datasets. OLAP means *Online Analytical Processing*.

## 10. Insights API — exposing campaign metrics

**Proposed API paths:**

```http
GET /ad/{campaignId}/clicks
GET /ad/{campaignId}/impressions
GET /ad/{campaignId}/clickToBasket
```

For example:

```http
GET /ad/CAMP101/clicks
X-Tenant-Id: T001
```

Illustrative response from the **implemented local demo**:

```json
{
  "campaignId": "CAMP101",
  "tenantId": "T001",
  "clicks": 12000
}
```

In the working `insights-api/` module, the value comes from **sample data loaded into an in-memory H2 database**. The API follows Controller → Service → Repository → Database and returns JSON; it does **not** currently query live Flink/PostgreSQL output.

### Proposed historical API extension

```http
GET /ad/CAMP101/clicks?from=2026-10-01&to=2026-10-23
```

Historical requests would read indexed pre-aggregates from PostgreSQL, avoiding raw S3 scans for common reports. However, **exact unique customers across arbitrary date ranges** require the distinct-count strategy described in Section 7; summing per-window unique values is not sufficient. Optional `from`/`to` filtering is **not implemented** in the current Java demo.

## 11. S3-compatible object storage and reconciliation

**Object storage** (for example S3 or MinIO) has two separate roles:

- **Raw event archive:** A Kafka sink/archival consumer writes event data for long-term retention, audit, backfill, and replay.
- **Flink checkpoints:** Flink stores recoverable processing state independently of the raw event archive.

**Reconciliation Worker (proposed batch service):** Periodically reads archived events, recomputes expected aggregates, compares them with PostgreSQL, and applies controlled, idempotent corrections for late, missed, or incorrect records.

**Example:** A delayed click arrives outside the normal Flink lateness threshold. Reconciliation can recompute the affected reporting window and replace its stored aggregate with a corrected value.

## 12. Security and multi-tenancy

The platform shares infrastructure among retailers but must prevent one tenant from reading or altering another tenant's data.

| Layer | Proposed controls |
|---|---|
| API Gateway | TLS, token validation for protected endpoints, per-tenant rate limits |
| Ad Placement | Signed, expiring tracking URLs |
| Click Tracking | Token signature/expiry checks, safe redirect allowlist |
| Event Ingestion | Trusted producer authentication, tenant validation, schema validation |
| Kafka / Flink | Tenant-aware partitioning, keys, and tenant-scoped state |
| Insights API | Spring Security authorization; derive or verify tenant identity from authenticated claims |
| PostgreSQL / S3 | Tenant-scoped access policies, encryption at rest, least-privilege credentials |
| Platform | Secrets management, audit logs, TLS for service communication |

**Example:** A Target marketer's JWT authorizes tenant `TARGET`. Changing a request header to `WALMART` must not grant Walmart data access. The API checks the authenticated tenant context before querying the database.

**Tenant configuration and quotas:** Each tenant can have an attribution window, retention policy, campaign settings, and API quota. Kafka producer/consumer quotas, Flink capacity planning, and per-tenant usage monitoring help manage noisy-neighbor risk. These are **proposed controls**, not features already implemented in the H2 demo.

## 13. Reliability, failure recovery, and scaling

**Kafka:** Partitioning enables parallel consumers; replication protects event availability; configured retention enables replay.

**Flink:** Keyed state tracks customer/session and aggregation information. Checkpoints to S3-compatible storage capture state and source offsets. After a crash, Flink restores a successful checkpoint and reprocesses the necessary Kafka records.

**PostgreSQL:** Deterministic aggregate keys and idempotent UPSERTs reduce double counting during retries/replay. Primary/standby and failover are proposed deployment measures.

**Spring Boot services:** Kubernetes replicas, readiness/liveness probes, rolling deployments, and autoscaling handle load spikes and failed instances. API rate limiting protects against overload.

**Example failure:** Flink processes events and crashes. Kafka still retains events. Flink restores the last successful checkpoint, replays the remaining events, and writes updated aggregate snapshots without adding the same counts twice.

**Important caveat:** Exactly-once outcomes depend on the complete source → state → sink design, not simply on enabling checkpoints.

## 14. Monitoring, diagnostics, and alerting

| Tool | Purpose |
|---|---|
| Prometheus + Grafana | Metrics collection and dashboards |
| ELK / OpenSearch | Centralized application and infrastructure logs |
| OpenTelemetry | Distributed traces across services |
| Alertmanager | Operational alerts and notifications |

**Key metrics:** Kafka consumer lag/throughput; Flink processing latency and failed checkpoints; API p95/p99 latency and 5xx error rate; PostgreSQL query latency and connection usage; end-to-end event freshness; and per-tenant traffic/quotas.

**Example alert:** If Kafka consumer lag grows while Flink throughput falls, operators inspect processing bottlenecks, checkpoint health, and scaling limits before data freshness breaches its target.

## 15. Technology decisions and trade-offs

| Choice | Why it fits | Trade-off |
|---|---|---|
| Spring Boot services | Familiar REST patterns and independent deployments | More services to operate |
| Redis | Fast ephemeral impression/idempotency metadata | TTL and cache availability must be managed |
| Kafka | Durable event streams and consumer decoupling | Broker and partition operations |
| Flink | Stateful event-time joins and streaming aggregates | Stateful jobs/checkpoint tuning add complexity |
| PostgreSQL | Predictable low-latency queries on prepared aggregates | Not ideal for arbitrary scans over billions of raw events |
| S3-compatible storage | Economical durable archive and backfill | Historical recomputation is asynchronous |
| ClickHouse (future) | Columnar OLAP for complex large-scale analysis | Additional data pipeline and operating cost |

**Cost controls:** Tiered retention (Kafka short, S3 long), compression and object lifecycle policies, autoscaling where useful, monitoring per-tenant consumption, and avoiding premature deployment of extra databases. Exact retention durations and capacity targets must be validated against business needs.

## 16. Implementation boundary — what is actually coded?

**Implemented in `insights-api/`:** Spring Boot REST endpoints for clicks, impressions, and click-to-basket; controller/service/repository/entity layering; sample metrics initialized in H2; tenant-scoped queries; missing-campaign handling; and tests.

**Proposed, not end-to-end implemented:** Ad Placement, Click Tracking, Event Ingestion, Redis integration, Kafka, Flink jobs, PostgreSQL serving, S3 archive, reconciliation, production Kubernetes/Jenkins deployment, JWT/RBAC, and historical date-range filters.

**Current accuracy limitation:** The API sums stored per-window metrics. For arbitrary ranges, those sums do not guarantee exact distinct-customer counts across windows.


