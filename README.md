# 🚀 IQ Key Value Microservices Platform

> Spring Boot microservices platform demonstrating modern architecture patterns, security best practices, and operational excellence for building scalable distributed systems.

[![Project Site](https://img.shields.io/badge/Project-iqkv.dev-blue?style=for-the-badge&logo=appveyor)](https://iqkv.dev)
[![Live Demo](https://img.shields.io/badge/Live%20Demo-iqkv.site-success?style=for-the-badge&logo=rocket)](https://iqkv.site)
[![Tenant App](https://img.shields.io/badge/Tenant%20App-app.iqkv.site-informational?style=for-the-badge)](https://app.iqkv.site)
[![Platform Admin](https://img.shields.io/badge/Platform%20Admin-admin.iqkv.site-blueviolet?style=for-the-badge)](https://admin.iqkv.site)
[![Swagger UI](https://img.shields.io/badge/API%20Docs-swagger-orange?style=for-the-badge&logo=swagger)](https://api.iqkv.site/swagger-ui.html)

<div align="center">
  <img src="https://github.com/dimdnk/dimdnk/blob/dev/screenshots/novustools-mockup-1781349539211.png?raw=true" width="600" alt="IQKV Platform — Platform Admin">
  <p><strong>Hybrid Tenancy SaaS Boilerplate. Microservice-first architecture, a collection of small, loosely coupled, and independently deployable services from day one.</strong></p>
</div>


## Table of Contents

- [Live Demo](#live-demo)
- [Business Purpose](#business-purpose)
- [Platform Services](#platform-services)
- [Architecture Overview](#architecture-overview)
- [Key Features](#key-features)
- [Architecture Patterns](#architecture-patterns)
- [Getting Started](#getting-started)
- [CI/CD & Deployment](#cicd--deployment)
- [Adding a New Microservice](#adding-a-new-microservice)
- [Learning Objectives](#learning-objectives)
- [Adapting for Your Domain](#adapting-for-your-domain)

## Live Demo

The platform runs live at **[iqkv.site](https://iqkv.site)** — a fully deployed instance of this repository with all services, both SPAs, and the complete observability stack.

### Public entry points

| URL | What you see |
|---|---|
| [app.iqkv.site](https://app.iqkv.site) | Tenant App — sign up, sign in, team management, invitations, account profile |
| [admin.iqkv.site](https://admin.iqkv.site) | Platform Admin — global user/org management, subscription monitoring, plan catalog |
| [api.iqkv.site/swagger-ui.html](https://api.iqkv.site/swagger-ui.html) | Aggregated Swagger UI — all backend APIs in one place |

> Observability tools (Grafana, Prometheus, RabbitMQ management, MailHog) are available in the local Docker demo only — see [Getting Started](#getting-started).

### Demo credentials

The demo instance runs in `MULTI_TENANT` mode. You can sign up freely — each registration creates an isolated tenant workspace. Platform Admin access and billing test credentials are available on request.

### What the demo illustrates

- End-to-end **multi-tenant signup flow**: registration → email verification → tenant provisioning (async via RabbitMQ) → login
- **Invitation flow**: tenant owner invites a team member, invitee receives email via MailHog, accepts, joins workspace
- **In-app notifications and announcements**: real-time WebSocket push visible in the Tenant App notification bell
- **JWT lifecycle**: obtain token at IAM, validated at Gateway via JWKS, user context propagated downstream as headers
- **Subscription management**: create plan, subscribe via Stripe test mode, observe `subscription.created` event flow to IAM (tenant status) and Audit
- **Audit trail**: every event (signup, login, invitation, subscription) visible in the Platform Admin audit log
- **Observability**: Grafana, Prometheus, RabbitMQ management, and MailHog are available locally via the Docker demo stack — run `./demo.sh` to access them at `http://api.iqkv.local/services/*`
- **Platform mode guard**: Gateway polls IAM `/actuator/info` every 60 s — mismatched `rolloutMode` across services triggers 503

## Business Purpose

A microservices ecosystem that provides:

- **Identity & Access Management** - Centralized authentication with RS256 JWT tokens, multi-tenant user lifecycle, email verification, invitation flows, in-app notifications, site-wide announcements, and role-based access control
- **API Gateway** - Reactive entry point with JWT validation, header sanitization, tenant context injection, and platform mode consistency enforcement
- **Billing & Payments** - Stripe-backed subscription management, plan catalog, webhook processing, and event-driven billing notifications
- **Activity Auditing** - System-wide audit trails capturing security events, data changes, and administrative actions with structured storage and querying
- **Extensible Platform** - Foundation for adding new microservices with standardized security, observability, and integration patterns
- **Included UI Applications** - Production-ready React frontends for both customers (Tenant App) and operators (Platform Admin)

This platform serves as a reference implementation for organizations building microservices architectures, showcasing production-ready patterns for authentication, API management, business domain services, and modern frontend development.


<div align="center">
  <img src="https://github.com/dimdnk/dimdnk/blob/dev/screenshots/chrome_IS8q0S3OwG.gif?raw=true" width="800" alt="IQKV Platform — Platform Admin">
  <p><strong>Separated Global Backoffice UI for entire Platform Management.</strong></p>
</div>

## Platform Services

### 🔐 [IAM Service](https://github.com/IQKV/foundation-iam-service/tree/dev/README.md)

Centralized authentication and identity management hub.

**Core Capabilities:**

- JWT-based authentication with RS256 (`JwtTokenGenerator` with RSA PEM keys)
- User registration via `SignupStrategy` — `MULTI_TENANT` creates a new tenant + grants `TENANT_OWNER`; `SINGLE_TENANT` joins the default tenant with `MEMBER`
- Email verification with 64-char hex one-time tokens, 24h expiry, rate-limited resend (3/hour)
- Password reset with 32-byte hex tokens, configurable TTL, enumeration-safe responses
- Tenant invitation flow — send, preview, accept (existing or new user), revoke
- In-app notifications — transactional events (signup, invitation, etc.) persisted as user notifications and pushed via WebSocket (STOMP/SockJS)
- Site-wide announcements — multi-lingual announcements with async fan-out to all users and real-time broadcast
- Two-layer token revocation: JTI denylist (per-signout) + `last_global_signout_at` (signout-all)
- Account lockout after configurable failed attempts with sliding window
- Schema-per-tenant PostgreSQL isolation (`t_{tenantKey}`) provisioned via Liquibase on `tenant.created` events
- JWKS endpoint (`/.well-known/jwks.json`) for public key distribution
- Tenant lifecycle management: `PROVISIONING → ACTIVE → SUSPENDED/DELETED` with event publishing
- Admin user management with paginated listing and partial updates
- Tenant owner member management: ban/unban, edit authority (TENANT_OWNER ↔ MEMBER), transfer ownership
- Platform admin actions: ban/unban users globally, unlock temporarily locked users

**Key Patterns:**

- `JwtAuthenticationFilter` runs before Spring Security — checks JTI denylist and `last_global_signout_at` on every request
- `MyBatisSchemaInterceptor` sets PostgreSQL `search_path` to `t_{tenantKey}, public` per request
- `TenantLiquibaseRunner` provisions tenant schemas asynchronously via RabbitMQ (`tenant.created` → `tenant.provisioned`)
- `StuckTenantReaperJob` marks tenants stuck in `PROVISIONING` as `PROVISIONING_FAILED` every 5 minutes
- `SubscriptionEventConsumer` suspends tenants on `subscription.cancelled` events from Billing
- `NotificationService` pushes real-time updates via WebSocket to `/user/{userId}/queue/notifications`
- `AnnouncementService` triggers async fan-out for site-wide announcements in batches of 1000
- `PlatformModeInfoContributor` exposes `platform.rollout-mode` via `/actuator/info` for Gateway validation
- Hourly ShedLock-protected cleanup jobs for expired tokens, verification tokens, and invitations
- Micrometer counters for `auth.success`, `auth.failure` (with tenant and reason tags), and `tenant.created`

### 🌐 [Gateway Service](https://github.com/IQKV/foundation-gateway-service/tree/dev/README.md)

Reactive API gateway providing the single external entry point for all services.

**Core Capabilities:**

- JWT validation via JWKS endpoint (Spring Security OAuth2 Resource Server, reactive)
- Routes: `/.well-known/**` and `/api/v1/iam/**` → IAM; `/api/v1/billing/**` → Billing
- Global CORS configuration with configurable allowed origins
- Platform mode consistency enforcement — polls IAM `/actuator/info` every 60s; returns 503 on mismatch
- Correlation ID generation and propagation (`X-Correlation-ID`)
- Security response headers on all responses

**Key Patterns:**

- `HeaderSanitizationFilter` (order `-190`) strips all `X-User-*`, `X-Tenant-ID`, `X-Organization-ID`, and `X-Audit-*` headers from incoming requests before JWT processing — prevents identity spoofing
- `AuditContextFilter` (order `-180`) extracts client IP and User-Agent from the original request and forwards them as `X-Audit-IP`, `X-Audit-UA`, and `X-Audit-Source` for downstream audit enrichment
- `JwtContextPropagationFilter` (order `-100`) reads validated JWT from `ReactiveSecurityContextHolder` and injects `X-User-ID`, `X-Username`, `X-User-Email`, `X-Tenant-ID`, `X-User-Authorities` for downstream services
- `TenantContextFilter` (order `-50`) — `MULTI_TENANT`: passes `X-Tenant-ID` from JWT; `SINGLE_TENANT`: injects `defaultTenantKey` when header is absent
- `ResponseTransformationFilter` (order `MIN_VALUE+1`) adds `X-Content-Type-Options`, `X-Frame-Options`, `X-XSS-Protection`, `Referrer-Policy` to all responses
- `PlatformModeGuardFilter` validates rollout mode consistency with IAM at startup and on a 60s schedule; sets readiness to `REFUSING_TRAFFIC` on mismatch
- Type-safe configuration with `@ConfigurationProperties` records (`GatewayProperties`, `GatewayConfigurationProperties`, `PlatformConfigurationProperties`)

### 💰 [Billing Service](https://github.com/IQKV/foundation-billing-service/tree/dev/README.md)

Stripe-backed subscription and billing management service.

**Core Capabilities:**

- Plan catalog with `MONTHLY`/`ANNUAL` billing periods, minor-unit pricing, feature sets (JSON), and `TENANT`/`USER` scope
- Subscription management with Stripe-synced status (`active`, `trialing`, `past_due`, `canceled`, `unpaid`)
- Stripe webhook processing with idempotency via `webhook_log` table — handles `customer.subscription.*` and `invoice.*` events
- Billing settings per tenant: Stripe customer ID, billing email, company name, address, tax ID
- User billing settings for `SINGLE_TENANT` mode: per-user Stripe customer creation
- Entitlement evaluation: resolves active subscription + plan feature set for a subject (tenant or user)
- Event-driven bootstrap: `TenantEventConsumer` creates Stripe customer on `tenant.created`; `UserEventConsumer` cleans up `profileOwnerId` on user removal
- Email notifications: subscription activated, subscription cancelled, invoice paid, payment failed
- Scheduled trial and overdue notifications (daily at 9AM and 10AM UTC via ShedLock)

**Key Patterns:**

- `SubscriptionSubjectResolver` strategy — `MULTI_TENANT`: subject is `TENANT/tenantKey`; `SINGLE_TENANT`: subject is `USER/userId`
- `PlanEligibilityPolicyImpl` validates plan scope matches subject type before subscription creation
- `WebhookProcessingService` idempotency: checks `webhook_log` by `externalEventId` before processing; status lifecycle `RECEIVED → PROCESSED/FAILED`
- `BillingContactResolver` fallback chain: `ownerEmail` from event → `DEFAULT_BILLING_EMAIL` env var → null (Stripe allows no email)
- `TrialNotificationService` finds trials ending in 2–3 days and `past_due` subscriptions daily
- `TenantExtractionFilter` resolves tenant from `X-Tenant-ID` header (injected by Gateway) with context validation
- `PaymentGatewayClient` wraps Stripe SDK; initialized with `secretKey` at construction

### 📋 [Audit Service](https://github.com/IQKV/foundation-audit-service/tree/dev/README.md)

Centralized, event-driven activity logging service. Passive observer of the platform event bus.

**Core Capabilities:**

- Passive consumption of all platform events via RabbitMQ topic wildcards (`user.#`, `tenant.#`, `subscription.#`, `invoice.#`, `audit.#`) — zero code changes in domain services for basic auditing
- Normalizes disparate domain events into a unified `AuditRecord` format with enriched technical context
- Captures client IP and User-Agent propagated from the Gateway as `X-Audit-IP` / `X-Audit-UA`
- Dedicated PostgreSQL database — high-volume logging isolated from business-critical transactions
- JSONB storage for dynamic event metadata — full domain event payload preserved for forensic queries
- Secured, paginated, filterable admin search API restricted to `PLATFORM_ADMIN`
- SPI pattern (`foundation-audit-spi`) — plug in Elasticsearch or custom SIEM backends without touching core

**Key Patterns:**

- `AuditEventConsumer` binds to `iqkv.events` with wildcard routing keys; normalizes events into `AuditRecord` via `AuditRecordMapper`
- `AuditContextEnricher` reads `X-Audit-IP` and `X-Audit-UA` headers propagated from the Gateway and attaches them to every record
- `AuditProvider` SPI interface decouples storage from consumption — `PostgresAuditProvider` is the default implementation
- `AuditSearchRestResource` exposes paginated search at `/api/v1/audits` — filterable by user, tenant, action, and time range
- JSONB `TypeHandler` in MyBatis preserves the full dynamic event payload alongside normalized fields
- Micrometer counters for `audit.event.consumption` (by type and source), `audit.persistence.duration`, and `audit.search.latency`

### 💻 UI Applications

Production-ready frontends for different user roles.

#### [Tenant App](https://github.com/IQKV/foundation-ui-app/tree/dev/README.md)

React 19 SPA for workspace members — sign-in with tenant discovery, team management, invitations, and account profile. Scoped to a single tenant via `X-Tenant-ID` and tenant-scoped JWTs.

#### [Platform Admin](https://github.com/IQKV/foundation-ui-platform-admin/tree/dev/README.md)

React 19 SPA for operators — global user/organization management, subscription monitoring, plan catalog CRUD, and platform-wide metrics dashboard.

## Architecture Overview

### Microservices Architecture

```text
┌─────────────┐      ┌─────────────────────────┐
│   Clients   │◄────►│  Tenant App (React)     │
│ (Web/Mobile)│      │  (app.iqkv.local)       │
└──────┬──────┘      └─────────────────────────┘
       │             ┌─────────────────────────┐
       │             │  Platform Admin (React) │
       │             │  (admin.iqkv.local)     │
       │             └─────────────────────────┘
       │ :80
       ▼
┌──────────────────────────────────────────┐
│          Nginx Proxy (:80)               │
│  • Host-based routing (UIs / API)        │
│  • Path-based routing (Infrastructure)   │
└──────┬──────────────────┬────────────────┘
       │                  │
       ▼                  ▼
┌──────────────────────────────────────────┐
│         Gateway Service (:8080)          │
│  • JWT Validation (JWKS from IAM)        │
│  • Header Sanitization + Propagation     │
│  • Tenant Context Injection              │
│  • Platform Mode Guard                   │
│  • Security Response Headers             │
└──────┬──────────────────┬────────────────┘
       │                  │
       ▼                  ▼
┌─────────────────┐  ┌──────────────────────┐  ┌──────────────────────┐
│   IAM Service   │  │   Billing Service    │  │    Audit Service     │
│  • Auth / JWT   │  │  • Subscriptions     │  │  • Audit Trails      │
│  • Users        │  │  • Plan Catalog      │  │  • Event Logging     │
│  • Tenants      │  │  • Stripe Webhooks   │  │  • Compliance        │
│  • Invitations  │  │  • Billing Settings  │  │  • Query API         │
│  • JWKS         │  │  • Entitlements      │  │                      │
└────────┬────────┘  └──────────┬───────────┘  └──────────┬───────────┘
         │                      │                         │
         ▼                      ▼                         ▼
┌─────────────────┐  ┌──────────────────────┐  ┌──────────────────────┐
│  PostgreSQL IAM │  │  PostgreSQL Billing  │  │  PostgreSQL Audit    │
│  (schema/tenant)│  │                      │  │                      │
└─────────────────┘  └──────────────────────┘  └──────────────────────┘

Shared Infrastructure
┌──────────────────┐  ┌──────────────────────┐  ┌──────────────────────┐
│     RabbitMQ     │  │       MailHog        │  │    Observability     │
│  iqkv.events     │  │  (local SMTP)        │  │  Prometheus/Grafana  │
│  topic exchange  │  │                      │  │  Loki / Promtail     │
└──────────────────┘  └──────────────────────┘  └──────────────────────┘
┌──────────────────┐  ┌──────────────────────┐  ┌──────────────────────┐
│      MinIO       │  │       DbGate         │  │       Redis          │
│  (S3 Storage)    │  │  (DB Admin UI)       │  │    (Cache/State)     │
│  :9000 / :9001   │  │      :3000           │  │       :6379          │
└──────────────────┘  └──────────────────────┘  └──────────────────────┘
```

### Technology Stack

- **Runtime:** Java 25 with modern features (records, var, text blocks, pattern matching, switch expressions)
- **Framework:** Spring Boot 4.x, Spring Cloud Gateway (WebFlux/reactive)
- **Database:** PostgreSQL 17 with Liquibase migrations, MyBatis, schema-per-tenant isolation
- **Messaging:** RabbitMQ topic exchange with dead-letter queues and 24h message TTL
- **Object Storage:** MinIO S3-compatible storage for file uploads, avatars, and documents
- **Database Admin:** DbGate web-based administration for PostgreSQL, Redis, RabbitMQ, and MinIO
- **Security:** JWT with RS256 (JJWT), Spring Security OAuth2 Resource Server, BCrypt strength 12
- **Frontend:** React 19, TypeScript, Mantine UI 8, TanStack Router & Query, Feature-Sliced Design (FSD)
- **Payments:** Stripe Java SDK for customer and subscription management
- **Observability:** Prometheus (Micrometer), Grafana, Loki, Promtail, structured JSON logging (Logstash Logback Encoder)
- **Distributed Locking:** ShedLock with JDBC provider for scheduled jobs
- **API Documentation:** SpringDoc OpenAPI with Swagger UI (webmvc + webflux variants)
- **Testing:** JUnit 5, Testcontainers, ArchUnit, H2
- **Containerization:** Docker with multi-stage builds, Docker Compose for local development

## Key Features

### Security & Authentication

- RS256 JWT authentication issued by IAM, validated at Gateway via JWKS
- Two-layer token revocation: JTI denylist (per-signout) + `last_global_signout_at` (signout-all)
- `JwtAuthenticationFilter` checks both revocation layers before Spring Security processes the request
- `HeaderSanitizationFilter` strips all user/tenant identity headers from client requests before JWT propagation — prevents spoofing
- JWT claims propagated downstream as `X-User-ID`, `X-Username`, `X-User-Email`, `X-Tenant-ID`, `X-User-Authorities`
- Role-based access control with `@PreAuthorize` and `@EnableMethodSecurity`
- Account lockout after configurable failed login attempts with sliding window; platform admins can unlock users manually
- Email verification with one-time tokens and rate-limited resend
- Password reset with enumeration-safe responses and session invalidation on completion
- Tenant invitation flow with authority assignment and support for both existing and new users
- Tenant owner member management: ban/unban, edit authority (TENANT_OWNER ↔ MEMBER), transfer ownership with last-owner safeguards
- Platform admin actions: ban/unban users globally
- **In-App Notifications**: Transactional events (signup, invitation, password reset) persisted and pushed in real time via WebSocket (STOMP/SockJS)
- **Site-Wide Announcements**: Multi-lingual announcements with async fan-out to all users in batches of 1000 and real-time broadcast via WebSocket

### Multi-Tenancy

- `MULTI_TENANT` mode: each signup creates a new tenant; subscriptions scoped per tenant
- `SINGLE_TENANT` mode: all users join a default tenant; subscriptions scoped per user
- Strategy pattern (`SignupStrategy`, `SubscriptionSubjectResolver`) selects behavior via `@ConditionalOnProperty`
- All three services validate rollout mode at startup; Gateway additionally polls IAM every 60s and blocks traffic on mismatch
- Schema-per-tenant PostgreSQL isolation in IAM — `MyBatisSchemaInterceptor` sets `search_path` per request
- Tenant provisioning via async RabbitMQ flow: `tenant.created` → Liquibase migrations → `tenant.provisioned`
- **Frontend Support**: Both apps support runtime `public/config.js` overrides and silent token refresh for seamless multi-tenant switching.

### Included UI Applications

- **Tenant App**: React 19 SPA with Mantine UI, TanStack Router, and Lingui i18n. Supports tenant discovery, self-service signup, and team management.
- **Platform Admin**: Operator console with `mantine-datatable`, real-time metrics dashboard, and global user/organization CRUD.
- **Landing Kit**: Performance-optimized Astro landing kit with Tailwind CSS and DaisyUI.

### Event-Driven Integration

- RabbitMQ topic exchange `iqkv.events` with dead-letter exchange and 24h TTL on all queues
- IAM publishes: `tenant.created/provisioned/provisioning_failed/suspended/deleted`, `user.created/deleted/removed/invited`, `notification.iam.email`
- Billing consumes `tenant.created` to bootstrap Stripe customer and billing settings
- Billing publishes: `subscription.created/cancelled`, `invoice.paid`, `payment.failed`, `notification.billing.email`
- IAM consumes `subscription.cancelled` to suspend the corresponding tenant
- ShedLock prevents duplicate scheduled job execution across clustered deployments

### Operational Excellence

- Structured JSON logging (Logstash Logback Encoder) for all services
- Correlation ID generated at Gateway, propagated through all downstream services and logs
- Prometheus metrics with Micrometer — auth counters with tenant/reason tags, auth duration timer
- Grafana dashboards provisioned from `docker/grafana/provisioning`
- Health checks on management port 8081 with readiness/liveness probes
- Graceful shutdown configured on all services
- Environment-specific profiles: `local`, `sit`, `uat`, `prd`

### Developer Experience

- OpenAPI documentation with Swagger UI — Gateway aggregates specs from IAM and Billing
- Docker Compose for local development with MailHog for email testing
- Consistent RFC 7807 error responses across all services
- Architecture validation with ArchUnit
- Integration tests with Testcontainers (PostgreSQL, RabbitMQ)
- Demo data Liquibase context for local development

## Architecture Patterns

### Cross-Cutting Patterns

- **Database Per Service** — IAM, Billing, and Audit each own a dedicated PostgreSQL instance
- **API Gateway** — single external entry point; downstream services not directly exposed
- **Event-Driven Choreography** — services react to domain events via RabbitMQ without direct coupling
- **Distributed Locking** — ShedLock with JDBC provider ensures scheduled jobs run once across replicas
- **Correlation ID Tracking** — generated at Gateway, propagated through all services and logs
- **Platform Mode Guard** — all core services enforce rollout mode consistency at startup

### Communication Patterns

- Synchronous REST APIs with proper HTTP semantics and RFC 7807 error responses
- JWT-based user context propagation via headers (`X-User-ID`, `X-Tenant-ID`, `X-User-Authorities`)
- Asynchronous event publishing via RabbitMQ topic exchange with dead-letter queues
- Gateway aggregates downstream OpenAPI specs for unified Swagger UI
- Webhook ingestion from Stripe with signature verification and idempotency

## Getting Started

### Prerequisites

- Java 25+
- Docker and Docker Compose

### Full Demo Stack (all-in-one)

The fastest way to see the entire platform running is the demo stack — one command starts all services, both SPAs, Nginx, and the full observability stack.

**1. Add local domains to your `hosts` file (one-time setup)**

```
# Linux / macOS: /etc/hosts
# Windows: C:\Windows\System32\drivers\etc\hosts

127.0.0.1  api.iqkv.local
127.0.0.1  admin.iqkv.local
127.0.0.1  app.iqkv.local
```

**2. Copy environment variables and start**

```bash
cp .env.example .env
# Defaults work out of the box.
# Set STRIPE_SECRET_KEY and STRIPE_WEBHOOK_SECRET for real billing.

# Linux / macOS
./demo.sh

# Windows (PowerShell)
.\demo.ps1
```

Both scripts run `docker compose -f compose.demo.yaml up -d --remove-orphans`.

**Expected startup time:** 2–4 minutes with images already pulled. Services start in dependency order — databases and RabbitMQ first, then IAM, then Billing and Audit, then Gateway, then UIs.

### Platform Entry Points

| URL                                          | Description                                    |
| -------------------------------------------- | ---------------------------------------------- |
| `http://app.iqkv.local`                      | Tenant app — sign up, sign in, team management |
| `http://admin.iqkv.local`                    | Platform admin UI                              |
| `http://api.iqkv.local/swagger-ui.html`      | Aggregated Swagger UI                          |
| `http://api.iqkv.local/services/grafana/`    | Grafana dashboards                             |
| `http://api.iqkv.local/services/prometheus/` | Prometheus                                     |
| `http://api.iqkv.local/services/rabbitmq/`   | RabbitMQ management UI                         |
| `http://api.iqkv.local/services/mailhog/`    | MailHog (captured emails)                      |

### Infrastructure Administration

The demo stack includes administrative tools for all infrastructure services:

- **DbGate** - Web-based database administration tool providing unified access to:
  - PostgreSQL databases (IAM, Billing, Audit)
  - Redis cache
  - RabbitMQ message queue
  - MinIO S3 object storage

See [docker/dbgate.md](docker/dbgate.md) for detailed DbGate documentation.

- **MinIO** - S3-compatible object storage for file uploads, avatars, and documents
  - S3 API: `foundation-minio:9000` (internal)
  - Console UI: `foundation-minio:9001` (internal)
  - Used by IAM and Billing services for asset storage

**Note:** DbGate and MinIO Console are available internally on the `foundation-network`. To access them, you can:

1. Configure Nginx to expose them via subdomain or path routing
2. Use `docker exec` to access their web interfaces
3. Port forward using Docker commands

### Individual Service Development

Each service ships three compose files covering every local workflow:

| File                     | Purpose                                                                                    |
| ------------------------ | ------------------------------------------------------------------------------------------ |
| `compose.yaml`           | Infrastructure only (PostgreSQL, RabbitMQ, MailHog, MinIO). Run the service from your IDE. |
| `compose.base.yaml`      | Shared service definitions — extended by the other two. Not used directly.                 |
| `compose.container.yaml` | Full stack — infrastructure + service built from source. No IDE required.                  |

**Infrastructure Components:**

- **PostgreSQL** - Dedicated database instance per service
- **RabbitMQ** - Message queue for event-driven communication
- **MailHog** - Local SMTP server for email testing
- **MinIO** - S3-compatible object storage for file uploads (IAM and Billing services)
- **Redis** - Cache and session storage (available in full demo stack)

**IDE workflow (recommended for contributors):**

```bash
# IAM Service — PostgreSQL, RabbitMQ, MailHog, MinIO
cd foundation-iam-service
cp .env.example .env.local
docker compose up -d
./mvnw spring-boot:run -Pdev
# → API: http://localhost:8080  Swagger: http://localhost:8080/swagger-ui.html

# Billing Service — PostgreSQL, RabbitMQ, MailHog
cd foundation-billing-service
docker compose up -d
./mvnw spring-boot:run -Pdev

# Audit Service — PostgreSQL, RabbitMQ
cd foundation-audit-service
docker compose up -d
./mvnw spring-boot:run -Pdev

# Gateway Service — depends on IAM + Billing + Audit
cd foundation-gateway-service
docker compose up -d
./mvnw spring-boot:run -Pdev
```

**Fully containerised (no IDE):**

```bash
# Build and run service + infrastructure from source
docker compose -f compose.container.yaml up -d --build
```

Each service uses isolated named volumes and a dedicated Docker network — running multiple services simultaneously requires no port remapping.

## CI/CD & Deployment

The platform ships reference CI/CD pipelines and Helm charts in the [`cicd/`](cicd/README.md) folder.

| Resource | Description |
|---|---|
| [`cicd/pipeline/`](cicd/pipeline/README.md) | Drone CI pipeline definitions for every service — Java microservices (10-pipeline flow), frontend apps (4-pipeline flow), and infrastructure |
| [`cicd/chart/`](cicd/chart/README.md) | Helm charts for Kubernetes deployment across SIT, UAT, and production environments |

**Pipeline flow for Java services:** `VerifyCode` (tests + SonarQube + PMD + SpotBugs) → `PublishArtifacts` (Nexus) → `PublishDockerImage` → `DeployWorkInProgress` (SIT auto) → `PromoteFeatureDeployment` / `PromoteDeployment` (manual promote) → `ReleasePackage` (semver automation).

**Helm charts** cover all platform components — `foundation-infra` (PostgreSQL, Redis, RabbitMQ, MinIO, MailHog), backend services, and frontend SPAs — with `values-sit.yaml`, `values-uat.yaml`, and `values-prd.yaml` variants.

> These files are **template references**. They run against a self-hosted Drone CI instance and a separate Helm charts repository. See [`cicd/README.md`](cicd/README.md) for required secrets and adaptation instructions.

## Adding a New Microservice

The [`cicd/pipeline/foundation-microservice-project-layout.yml`](cicd/pipeline/foundation-microservice-project-layout.yml) is the **canonical template** for adding a service to the platform. It encodes every convention the ecosystem enforces — quality gates, release automation, deployment pipeline, Helm integration — so new services slot in without improvising.

### What the template gives you

- **10-pipeline Drone CI flow** — the full lifecycle from push to production: verify → publish artifacts → publish Docker image → deploy to SIT → promote to UAT/PRD → automated release
- **Quality gates wired in** — `mvn clean verify` (JaCoCo + Checkstyle), SonarQube with quality gate wait, PMD High-priority rules, SpotBugs
- **Conventional release automation** — `ReleasePackage` strips `-SNAPSHOT`, creates git tag, bumps pom + `package.json` to next SNAPSHOT, regenerates `CHANGELOG.md` via `release-it`, all in one promote trigger
- **Consistent image tagging** — `wip` → `:wip`, `feature/my-thing` → `:my-thing`, release tag → `:1.2.3`
- **Helm-based atomic deployment** — deploy steps run `helm upgrade --install --atomic --wait` with automatic rollback on failure
- **Rollback pipelines** — matching `Rollback*` pipelines for every target via `helm uninstall`
- **Slack notifications** — success/failure webhooks on every significant step with consistent emoji convention

### Steps to add a service

**1. Bootstrap the Maven module**

Add your service to the root `pom.xml`:

```xml
<modules>
    ...
    <module>foundation-your-service</module>
</modules>
```

Inherit from `com.iqkv:boot-parent-pom`. This pulls in Checkstyle config, JaCoCo thresholds, Surefire/Failsafe split, dependency management, and plugin versions.

**2. Follow the package structure**

```
foundation-your-service/src/main/java/com/iqkv/your/
├── config/          — @Configuration classes, @ConfigurationProperties records
├── domain/          — entities, business objects
├── repository/      — JPA repositories or MyBatis mappers
├── service/         — business logic, @Transactional
├── presentation/
│   ├── web/         — public REST controllers
│   └── admin/       — PLATFORM_ADMIN-only endpoints
├── security/        — service-specific security config and filters
├── exception/       — domain exceptions + @ControllerAdvice (RFC 7807)
└── YourServiceApplication.java
```

**3. Wire platform cross-cutting concerns**

These are non-negotiable for every platform service:

| Concern | How |
|---|---|
| Correlation ID | Read `X-Correlation-ID` from request, attach to MDC, include in all log lines |
| Structured logging | Use Logstash Logback Encoder — JSON output, no plaintext in production |
| Health checks | Expose `/actuator/health/liveness` and `/actuator/health/readiness` on port 8081 |
| Tenant context | Read `X-Tenant-ID` header (injected by Gateway), validate, store in `TenantContext` |
| User context | Read `X-User-ID`, `X-Username`, `X-User-Authorities` headers (injected by Gateway) |
| JWT | Configure as OAuth2 Resource Server — validate against IAM JWKS endpoint |
| Metrics | Expose `/actuator/prometheus`; add Micrometer counters for critical business operations |
| OpenAPI | Annotate controllers and DTOs with SpringDoc; Gateway aggregates specs automatically |
| Error responses | Return RFC 7807 `ProblemDetail` — no custom error envelope schemas |
| Rollout mode | Read `platform.rolloutMode` from config; fail fast at startup if inconsistent with other services |

**4. Publish domain events**

Route keys follow the convention `your-domain.{verb}` and `notification.your-service.email`. The Audit Service binds to `#` wildcards — your events are automatically captured without any Audit Service changes.

**5. Add Helm chart**

Copy an existing backend chart (e.g. `cicd/chart/foundation-billing-service`). Required sections:

- `infraServices.postgresql.*` and optionally `infraServices.rabbitmq.*`
- `platform.rolloutMode` — must match all other services in the release
- Dual management services on port 8081 (`managementService` and `managementHttpService`)
- Liveness/readiness probes on `/actuator/health/*` port 8081
- Non-root security context (`runAsUser: 1001`, `capabilities.drop: [ALL]`)
- `monitoring.serviceMonitor` + `monitoring.prometheusRule` blocks (disabled by default)

Store the chart in the Helm charts repository under `{org}/{repo-name}/`.

**6. Copy and configure the pipeline**

Copy `cicd/pipeline/foundation-microservice-project-layout.yml` to your service repo. Adjust `--set` flags to match your `values.yaml` keys and add service-specific secrets. The `sonar.projectKey` derives automatically from `${DRONE_REPO_OWNER}:${DRONE_REPO_NAME}`.

**7. Add architecture tests**

Every service enforces layer rules with ArchUnit. Copy the test class from an existing service, update the package prefix, and verify on first build.

**8. Register in the monorepo**

- Add to `compose.demo.yaml` in the correct startup order (after `foundation-infra`, before `foundation-gateway-service`)
- Add a route in Gateway routing config pointing to the new service
- Update the Platform Services section in this README

## Learning Objectives

This platform demonstrates:

### Microservices Architecture

- Service decomposition and bounded contexts
- Database per service pattern
- API gateway as single entry point
- Event-driven choreography with RabbitMQ
- Async tenant provisioning with status lifecycle and failure recovery

### Security Implementation

- RS256 JWT issuance and validation across services
- Two-layer token revocation (JTI denylist + global signout timestamp)
- Header sanitization to prevent identity spoofing at the gateway
- JWT claim propagation to downstream services
- Schema-per-tenant data isolation with MyBatis interceptor
- Enumeration-safe password reset and email verification flows

### Multi-Tenancy

- Strategy pattern for `MULTI_TENANT` vs `SINGLE_TENANT` behavior
- Conditional Spring beans with `@ConditionalOnProperty`
- Cross-service rollout mode consistency enforcement
- Tenant lifecycle with async provisioning and failure recovery

### Operational Patterns

- Structured JSON logging with correlation ID propagation
- Prometheus metrics with business-relevant tags
- ShedLock for distributed scheduled job coordination
- Dead-letter queues for messaging fault tolerance
- Readiness probe integration with platform mode guard

### Modern Java Development

- Java 25 features (records, var, text blocks, pattern matching, switch expressions)
- Immutable DTOs with records and `@ConfigurationProperties` records
- Reactive programming with WebFlux and Project Reactor (Gateway)
- Spring Boot 4.x best practices with type-safe configuration
- MyBatis with XML mappers and custom type handlers
- Clean package-by-feature structure with clear layer separation

### Modern Frontend Development

- React 19 with TypeScript and Mantine UI 8
- Feature-Sliced Design (FSD) architecture for scalable SPAs
- File-based routing with TanStack Router
- Server state management with TanStack Query
- Internationalization with Lingui i18n
- Vite-based builds with runtime environment configuration

## Adapting for Your Domain

This platform provides reusable patterns for:

### Authentication & Authorization

- Multi-tenant SaaS applications with per-tenant schema isolation
- Single-tenant applications with per-user billing
- Partner and employee portals with invitation-based onboarding
- Any system requiring centralized JWT issuance with downstream validation

### API Gateway Patterns

- Reactive entry point with JWT validation and context propagation
- Header sanitization before forwarding to internal services
- Platform-wide configuration consistency enforcement
- Aggregated API documentation across multiple services

### Billing & Subscriptions

- SaaS subscription management with Stripe
- Plan catalogs with scope-based eligibility (tenant vs user)
- Webhook-driven subscription lifecycle with idempotent processing
- Event-driven billing notifications and trial management

The patterns demonstrated here apply to any organization building microservices architectures requiring centralized authentication, API management, and scalable domain services.

---

**Use this as a foundation** for building production-ready microservices with modern Spring Boot, demonstrating security, observability, and operational excellence patterns that scale.
