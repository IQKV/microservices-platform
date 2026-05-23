# 🏗️ IQ Key Value Backend Microservices

> Enterprise microservices foundation for scalable SaaS products. Essential business infrastructure—Identity, Payments, and API Gateway—on secure, multi-tenant architecture.

[![Project Site](https://img.shields.io/badge/Project-iqkv.dev-blue?style=for-the-badge&logo=appveyor)](https://iqkv.dev)
[![Live Demo](https://img.shields.io/badge/Demo-iqkv.site-success?style=for-the-badge&logo=playstation)](https://iqkv.site)

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
- **📋 [Audit Service](foundation-audit-service/README.md)**: System-wide audit trails, activity tracking, compliance logging, event-driven log ingestion
- **💻 [Tenant App](foundation-ui-app/README.md)**: React 19 SPA for workspace members — sign-in, team management, invitations, account profile
- **🛡️ [Platform Admin](foundation-ui-platform-admin/README.md)**: React 19 SPA for operators — global user/org management, subscription monitoring, plan catalog CRUD

## Infrastructure

Production-ready capabilities across all services:

- **Multi-Tenancy**: Schema-per-tenant PostgreSQL isolation with `MULTI_TENANT` and `SINGLE_TENANT` modes
- **Security**: RS256 JWT with two-layer revocation (JTI denylist + global signout timestamp), header sanitization to prevent spoofing
- **Frontend**: React 19, Mantine UI 8, TanStack Router & Query, Feature-Sliced Design (FSD)
- **Event-Driven**: RabbitMQ topic exchange with dead-letter queues — tenant provisioning, subscription lifecycle, billing notifications
- **Observability**: Correlation ID propagation, Prometheus metrics, Grafana dashboards, structured JSON logging
- **Distributed Locking**: ShedLock for scheduled jobs (token cleanup, trial notifications, stuck tenant recovery)

## Quick Start

The easiest way to see the platform in action is using the provided demo scripts:

```bash
# On Linux or macOS
./demo.sh

# On Windows (PowerShell)
./demo.ps1
```

These scripts launch the entire stack using `compose.demo.yaml`.

### Platform Entry Points

Once the stack is running, access the platform via these local domains:

- **API Gateway**: [http://api.iqkv.local](http://api.iqkv.local)
- **Tenant App**: [http://app.iqkv.local](http://app.iqkv.local)
- **Platform Admin**: [http://admin.iqkv.local](http://admin.iqkv.local)

_Note: Ensure you have mapped these domains to `127.0.0.1` in your hosts file._

### Individual Service Development

```bash
# IAM Service with PostgreSQL, RabbitMQ, MailHog
cd foundation-iam-service && docker compose up

# Billing Service with PostgreSQL, RabbitMQ, MailHog
cd foundation-billing-service && docker compose up

# Audit Service with PostgreSQL, RabbitMQ
cd foundation-audit-service && docker compose up

# Gateway Service (expects IAM running separately)
cd foundation-gateway-service && docker compose up
```

### Monitoring & Infrastructure

Access observability tools via the unified API domain:

- **Grafana**: [http://api.iqkv.local/services/grafana/](http://api.iqkv.local/services/grafana/)
- **Prometheus**: [http://api.iqkv.local/services/prometheus/](http://api.iqkv.local/services/prometheus/)
- **RabbitMQ**: [http://api.iqkv.local/services/rabbitmq/](http://api.iqkv.local/services/rabbitmq/)
- **MailHog**: [http://api.iqkv.local/services/mailhog/](http://api.iqkv.local/services/mailhog/)

## Domain Adaptability

Rapid deployment across industries:

- **B2B SaaS**: Multi-tenant schema isolation, invitation-based onboarding, per-tenant subscriptions
- **Single-Tenant Apps**: Per-user billing, default tenant auto-join, user-scoped subscriptions
- **Regulated Sectors**: Schema-per-tenant data isolation, audit trails, enumeration-safe auth flows

---

**Build faster** on infrastructure that handles complexity, focus on your business value.
