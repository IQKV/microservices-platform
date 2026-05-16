# 🚀 IQ Key Value Microservices Platform

> Spring Boot microservices platform demonstrating modern architecture patterns, security best practices, and operational excellence for building scalable distributed systems.

## Table of Contents

- [Business Purpose](#business-purpose)
- [Platform Services](#platform-services)
- [Architecture Overview](#architecture-overview)
- [Key Features](#key-features)
- [Architecture Patterns](#architecture-patterns)
- [Getting Started](#getting-started)
- [Monitoring](#monitoring)
- [Learning Objectives](#learning-objectives)
- [Adapting for Your Domain](#adapting-for-your-domain)

## Business Purpose

A microservices ecosystem that provides:

- **Identity & Access Management** - Centralized authentication with RS256 JWT tokens, multi-tenant user lifecycle, email verification, invitation flows, and role-based access control
- **API Gateway** - Reactive entry point with JWT validation, header sanitization, tenant context injection, and platform mode consistency enforcement
- **Billing & Payments** - Stripe-backed subscription management, plan catalog, webhook processing, and event-driven billing notifications
- **Extensible Platform** - Foundation for adding new microservices with standardized security, observability, and integration patterns
- **Included UI Applications** - Production-ready React frontends for both customers (Tenant App) and operators (Platform Admin)

This platform serves as a reference implementation for organizations building microservices architectures, showcasing production-ready patterns for authentication, API management, business domain services, and modern frontend development.

## Platform Services

### 🔐 [IAM Service](foundation-iam-service/README.md)

Centralized authentication and identity management hub.

**Core Capabilities:**

- JWT-based authentication with RS256 (`JwtTokenGenerator` with RSA PEM keys)
- User registration via `SignupStrategy` — `MULTI_TENANT` creates a new tenant + grants `TENANT_OWNER`; `SINGLE_TENANT` joins the default tenant with `MEMBER`
- Email verification with 64-char hex one-time tokens, 24h expiry, rate-limited resend (3/hour)
- Password reset with 32-byte hex tokens, configurable TTL, enumeration-safe responses
- Tenant invitation flow — send, preview, accept (existing or new user), revoke
- Two-layer token revocation: JTI denylist (per-signout) + `last_global_signout_at` (signout-all)
- Account lockout after configurable failed attempts with sliding window
- Schema-per-tenant PostgreSQL isolation (`t_{tenantKey}`) provisioned via Liquibase on `tenant.created` events
- JWKS endpoint (`/.well-known/jwks.json`) for public key distribution
- Tenant lifecycle management: `PROVISIONING → ACTIVE → SUSPENDED/DELETED` with event publishing
- Admin user management with paginated listing and partial updates

**Key Patterns:**

- `JwtAuthenticationFilter` runs before Spring Security — checks JTI denylist and `last_global_signout_at` on every request
- `MyBatisSchemaInterceptor` sets PostgreSQL `search_path` to `t_{tenantKey}, public` per request
- `TenantLiquibaseRunner` provisions tenant schemas asynchronously via RabbitMQ (`tenant.created` → `tenant.provisioned`)
- `StuckTenantReaperJob` marks tenants stuck in `PROVISIONING` as `PROVISIONING_FAILED` every 5 minutes
- `SubscriptionEventConsumer` suspends tenants on `subscription.cancelled` events from Billing
- `PlatformModeInfoContributor` exposes `platform.rollout-mode` via `/actuator/info` for Gateway validation
- Hourly ShedLock-protected cleanup jobs for expired tokens, verification tokens, and invitations
- Micrometer counters for `auth.success`, `auth.failure` (with tenant and reason tags), and `tenant.created`

### 🌐 [Gateway Service](foundation-gateway-service/README.md)

Reactive API gateway providing the single external entry point for all services.

**Core Capabilities:**

- JWT validation via JWKS endpoint (Spring Security OAuth2 Resource Server, reactive)
- Routes: `/.well-known/**` and `/api/v1/iam/**` → IAM; `/api/v1/billing/**` → Billing
- Global CORS configuration with configurable allowed origins
- Platform mode consistency enforcement — polls IAM `/actuator/info` every 60s; returns 503 on mismatch
- Correlation ID generation and propagation (`X-Correlation-ID`)
- Security response headers on all responses

**Key Patterns:**

- `HeaderSanitizationFilter` (order `-190`) strips all `X-User-*`, `X-Tenant-ID`, `X-Organization-ID` headers from incoming requests before JWT processing — prevents identity spoofing
- `JwtContextPropagationFilter` (order `-100`) reads validated JWT from `ReactiveSecurityContextHolder` and injects `X-User-ID`, `X-Username`, `X-User-Email`, `X-Tenant-ID`, `X-User-Authorities` for downstream services
- `TenantContextFilter` (order `-50`) — `MULTI_TENANT`: passes `X-Tenant-ID` from JWT; `SINGLE_TENANT`: injects `defaultTenantKey` when header is absent
- `ResponseTransformationFilter` (order `MIN_VALUE+1`) adds `X-Content-Type-Options`, `X-Frame-Options`, `X-XSS-Protection`, `Referrer-Policy` to all responses
- `PlatformModeGuardFilter` validates rollout mode consistency with IAM at startup and on a 60s schedule; sets readiness to `REFUSING_TRAFFIC` on mismatch
- Type-safe configuration with `@ConfigurationProperties` records (`GatewayProperties`, `GatewayConfigurationProperties`, `PlatformConfigurationProperties`)

### 💰 [Billing Service](foundation-billing-service/README.md)

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

### 💻 UI Applications

Production-ready frontends for different user roles.

#### [Tenant App](foundation-ui-app/README.md)
React 19 SPA for workspace members — sign-in with tenant discovery, team management, invitations, and account profile. Scoped to a single tenant via `X-Tenant-ID` and tenant-scoped JWTs.

#### [Platform Admin](foundation-ui-platform-admin/README.md)
React 19 SPA for operators — global user/organization management, subscription monitoring, plan catalog CRUD, and platform-wide metrics dashboard.

## Architecture Overview

### Microservices Architecture

```text
┌─────────────┐      ┌─────────────────────────┐
│   Clients   │◄────►│  Tenant App (React)     │
│ (Web/Mobile)│      └─────────────────────────┘
└──────┬──────┘      ┌─────────────────────────┐
       │             │  Platform Admin (React) │
       │             └─────────────────────────┘
       │ :80
       ▼
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
┌─────────────────┐  ┌──────────────────────┐
│   IAM Service   │  │   Billing Service    │
│  • Auth / JWT   │  │  • Subscriptions     │
│  • Users        │  │  • Plan Catalog      │
│  • Tenants      │  │  • Stripe Webhooks   │
│  • Invitations  │  │  • Billing Settings  │
│  • JWKS         │  │  • Entitlements      │
└────────┬────────┘  └──────────┬───────────┘
         │                      │
         ▼                      ▼
┌─────────────────┐  ┌──────────────────────┐
│  PostgreSQL IAM │  │  PostgreSQL Billing  │
│  (schema/tenant)│  │                      │
└─────────────────┘  └──────────────────────┘

Shared Infrastructure
┌──────────────────┐  ┌──────────────────────┐  ┌──────────────────────┐
│     RabbitMQ     │  │       MailHog        │  │    Observability     │
│  iqkv.events     │  │  (local SMTP)        │  │  Prometheus/Grafana  │
│  topic exchange  │  │                      │  │  Loki / Promtail     │
└──────────────────┘  └──────────────────────┘  └──────────────────────┘
```

### Technology Stack

- **Runtime:** Java 25 with modern features (records, var, text blocks, pattern matching, switch expressions)
- **Framework:** Spring Boot 4.x, Spring Cloud Gateway (WebFlux/reactive)
- **Database:** PostgreSQL 17 with Liquibase migrations, MyBatis, schema-per-tenant isolation
- **Messaging:** RabbitMQ topic exchange with dead-letter queues and 24h message TTL
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
- Account lockout after configurable failed login attempts with sliding window
- Email verification with one-time tokens and rate-limited resend
- Password reset with enumeration-safe responses and session invalidation on completion
- Tenant invitation flow with authority assignment and support for both existing and new users

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

- **Database Per Service** — IAM and Billing each own a dedicated PostgreSQL instance
- **API Gateway** — single external entry point; downstream services not directly exposed
- **Event-Driven Choreography** — services react to domain events via RabbitMQ without direct coupling
- **Distributed Locking** — ShedLock with JDBC provider ensures scheduled jobs run once across replicas
- **Correlation ID Tracking** — generated at Gateway, propagated through all services and logs
- **Platform Mode Guard** — all three services enforce rollout mode consistency at startup

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

### Full Platform (all services)

```bash
# Start the entire platform from the repository root
docker compose up

# Platform entry point
# http://localhost:80  →  Gateway Service
# http://localhost:8888/dashboard/  →  Traefik Dashboard
```

### Individual Service Development

Each service can be run independently with its own Docker Compose:

```bash
# IAM Service with PostgreSQL, RabbitMQ, MailHog
cd foundation-iam-service
docker compose up

# Billing Service with PostgreSQL, RabbitMQ, MailHog
cd foundation-billing-service
docker compose up

# Gateway Service (expects IAM running separately)
cd foundation-gateway-service
docker compose up
```

### API Documentation

Once services are running, access Swagger UI:

- Gateway (aggregated): `http://localhost:80/swagger-ui.html`
- IAM Service (direct): `http://localhost:8080/swagger-ui.html`
- Billing Service (direct): `http://localhost:8080/swagger-ui.html`

### Monitoring

Access observability tools via Traefik virtual hosts (add to `/etc/hosts` or use a local DNS):

- Grafana: `http://grafana.localhost`
- Prometheus: `http://prometheus.localhost`
- RabbitMQ Management: `http://rabbitmq.localhost`
- MailHog: `http://mailhog.localhost`
- Health checks: `http://{service}:8081/actuator/health`

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
