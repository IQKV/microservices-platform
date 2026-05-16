# 🏗️ IQ Key Value Backend Microservices

> Enterprise microservices foundation for scalable SaaS products. Essential business infrastructure—Identity, Payments, and API Gateway—on secure, multi-tenant architecture.

## Table of Contents

- [Business Pillars](#business-pillars)
- [Service Ecosystem](#service-ecosystem)
- [Infrastructure](#infrastructure)
- [Quick Start](#quick-start)
- [Domain Adaptability](#domain-adaptability)

## Business Pillars

Critical domains to accelerate product development:

- **Identity & Access**: Multi-tenant authentication, invitation flows, and role-based access control
- **Financial Operations**: Stripe-backed subscriptions, plan catalog, and webhook-driven billing
- **Frontend Experience**: Production-ready React 19 SPAs for tenants and platform administrators

## Service Ecosystem

Domain-aligned services with event-driven communication:

- **🔐 [IAM Service](foundation-iam-service/README.md)**: RS256 JWT authentication, tenant lifecycle, email verification, password reset, invitation flows
- **🌐 [Gateway Service](foundation-gateway-service/README.md)**: Reactive entry point with JWT validation, header sanitization, tenant context injection, platform mode guard
- **💰 [Billing Service](foundation-billing-service/README.md)**: Stripe subscriptions, plan catalog, webhook processing, entitlement evaluation
- **💻 [Tenant App](../foundation-ui-app/README.md)**: React 19 SPA for workspace members — sign-in, team management, invitations, account profile
- **🛡️ [Platform Admin](../foundation-ui-platform-admin/README.md)**: React 19 SPA for operators — global user/org management, subscription monitoring, plan catalog CRUD

## Infrastructure

Production-ready capabilities across all services:

- **Multi-Tenancy**: Schema-per-tenant PostgreSQL isolation with `MULTI_TENANT` and `SINGLE_TENANT` modes
- **Security**: RS256 JWT with two-layer revocation (JTI denylist + global signout timestamp), header sanitization to prevent spoofing
- **Frontend**: React 19, Mantine UI 8, TanStack Router & Query, Feature-Sliced Design (FSD)
- **Event-Driven**: RabbitMQ topic exchange with dead-letter queues — tenant provisioning, subscription lifecycle, billing notifications
- **Observability**: Correlation ID propagation, Prometheus metrics, Grafana dashboards, structured JSON logging
- **Distributed Locking**: ShedLock for scheduled jobs (token cleanup, trial notifications, stuck tenant recovery)

## Quick Start

Deploy the full platform with Docker Compose:

```bash
# Start all services from repository root
docker compose up

# Platform entry point
# http://localhost:80  →  Gateway Service
# http://localhost:8888/dashboard/  →  Traefik Dashboard
```

Individual service development:

```bash
# IAM Service with PostgreSQL, RabbitMQ, MailHog
cd foundation-iam-service && docker compose up

# Billing Service with PostgreSQL, RabbitMQ, MailHog
cd foundation-billing-service && docker compose up

# Gateway Service (expects IAM running separately)
cd foundation-gateway-service && docker compose up
```

Access APIs:

- Gateway (aggregated): `http://localhost:80/swagger-ui.html`
- Grafana: `http://grafana.localhost`
- RabbitMQ: `http://rabbitmq.localhost`
- MailHog: `http://mailhog.localhost`

## Domain Adaptability

Rapid deployment across industries:

- **B2B SaaS**: Multi-tenant schema isolation, invitation-based onboarding, per-tenant subscriptions
- **Single-Tenant Apps**: Per-user billing, default tenant auto-join, user-scoped subscriptions
- **Regulated Sectors**: Schema-per-tenant data isolation, audit trails, enumeration-safe auth flows

---

**Build faster** on infrastructure that handles complexity, focus on your business value.
