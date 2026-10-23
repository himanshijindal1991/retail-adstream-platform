# Retail AdStream — Insights API

## Project Overview

A Spring Boot REST API for retrieving advertising campaign insights in a real-time retail analytics platform.

The API serves pre-aggregated campaign metrics, including impressions, clicks, and click-to-basket conversions.

This service is part of a proposed streaming architecture using Apache Kafka, Apache Flink, PostgreSQL, and object storage.

## Technology Stack

- Java 17
- Spring Boot 4.1.2
- Spring Data JPA / Hibernate
- H2 Database (local demonstration)
- PostgreSQL (proposed production database)
- Maven
- JUnit 5 and Mockito
- Swagger / OpenAPI

## Architecture

Retail Website → Event Ingestion → Kafka → Flink → PostgreSQL → Insights API

The local demonstration uses H2 and preloaded sample metrics instead of running Kafka, Flink, or PostgreSQL.

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| GET | `/ad/{campaignId}/clicks` | Unique customers who clicked |
| GET | `/ad/{campaignId}/impressions` | Unique customers who viewed ads |
| GET | `/ad/{campaignId}/clickToBasket` | Unique customers who added products to their basket after an ad click |

## Running Locally

Prerequisites: Java 17 and a working Maven Wrapper.

```bash
./mvnw spring-boot:run
```

The application runs at `http://localhost:8080`.

## Example Request

```bash
curl -H "X-Tenant-Id: T001" \
  http://localhost:8080/ad/CAMP101/clicks
```

Example response:

```json
{
  "tenantId": "T001",
  "campaignId": "CAMP101",
  "clicks": 12000
}
```

## API Documentation

Swagger UI: `http://localhost:8080/swagger-ui.html`

## Running Tests

```bash
./mvnw clean test
```

Tests cover campaign metrics retrieval, zero counts, and missing-campaign handling.

## Multi-Tenancy

Campaign queries are scoped by tenant ID to prevent accidental mixing of retailer data.

For demonstration purposes, tenant ID is supplied using the `X-Tenant-Id` request header.

In production, tenant identity must be derived from authenticated credentials.

## Design Considerations

- Kafka provides durable event streaming.
- Flink performs deduplication, session tracking, and campaign aggregation.
- PostgreSQL serves precomputed metrics.
- Object storage retains raw events for reconciliation and backfills.
- Idempotency prevents duplicate event processing.
- Horizontal scaling supports traffic spikes.

## Limitations

This implementation demonstrates the Insights API only. Kafka, Flink, Redis, reconciliation, and the production deployment are proposed architecture components rather than implemented services.

The sample metrics represent predefined cumulative reporting windows. Arbitrary historical date-range queries and exact distinct-customer aggregation across windows are not implemented....
