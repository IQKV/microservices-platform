# 🏗️ IQ Key Value Backend Microservices

> Enterprise microservices foundation for scalable SaaS products. Essential business infrastructure—Identity, Payments, and API Gateway—on secure, multi-tenant architecture.

[![Project Site](https://img.shields.io/badge/Project-iqkv.dev-blue?style=for-the-badge&logo=appveyor)](https://iqkv.dev)
[![Live Demo](https://img.shields.io/badge/Demo-iqkv.site-success?style=for-the-badge&logo=playstation)](https://iqkv.site)

## Table of Contents

- [Business Pillars](#business-pillars)
- [Service Ecosystem](#service-ecosystem)
- [Infrastructure](#infrastructure)
- [Quick Start](#quick-start)
- [CI/CD & Deployment](#cicd--deployment)
- [Domain Adaptability](#domain-adaptability)

## Business Pillars

Critical domains to accelerate product development:

- **Identity & Access**: Multi-tenant authentication, invitation flows, in-app notifications, site-wide announcements, and role-based access control
- **Financial Operations**: Stripe-backed subscriptions, plan catalog, and webhook-driven billing
- **Frontend Experience**: Production-ready React 19 SPAs for tenants and platform administrators

## Service Ecosystem

Domain-aligned services with event-driven communication:

- **🔐 [IAM Service](foundation-iam-service/README.md)**: RS256 JWT authentication, tenant lifecycle, email verification, password reset, invitation flows, in-app notifications, site-wide announcements, tenant owner member management (ban/unban, edit authority, transfer ownership), platform admin user actions (ban/unban, unlock)
- **🌐 [Gateway Service](foundation-gateway-service/README.md)**: Reactive entry point with JWT validation, header sanitization, tenant context injection, platform mode guard
- **💰 [Billing Service](foundation-billing-service/README.md)**: Stripe subscriptions, plan catalog, webhook processing, entitlement evaluation
- **📋 [Audit Service](foundation-audit-service/README.md)**: System-wide audit trails, activity tracking, compliance logging, event-driven log ingestion
- **💻 [Tenant App](foundation-ui-app/README.md)**: React 19 SPA for workspace members — sign-in, team management, invitations, account profile
- **🛡️ [Platform Admin](foundation-ui-platform-admin/README.md)**: React 19 SPA for operators — global user/org management, subscription monitoring, plan catalog CRUD

## Infrastructure

Production-ready capabilities across all services:

- **Multi-Tenancy**: Schema-per-tenant PostgreSQL isolation with `MULTI_TENANT` and `SINGLE_TENANT` modes
- **Security**: RS256 JWT with two-layer revocation (JTI denylist + global signout timestamp), header sanitization to prevent spoofing, account lockout with manual unlock
- **Object Storage**: MinIO S3-compatible storage for file uploads, avatars, and documents
- **Database Administration**: DbGate web-based tool for PostgreSQL, Redis, RabbitMQ, and MinIO management
- **Frontend**: React 19, Mantine UI 8, TanStack Router & Query, Feature-Sliced Design (FSD)
- **Event-Driven**: RabbitMQ topic exchange with dead-letter queues — tenant provisioning, subscription lifecycle, billing notifications
- **Observability**: Correlation ID propagation, Prometheus metrics, Grafana dashboards, structured JSON logging
- **Distributed Locking**: ShedLock for scheduled jobs (token cleanup, trial notifications, stuck tenant recovery)
- **Member Management**: Tenant owner actions (ban/unban, edit authority, transfer ownership), platform admin actions (ban/unban users, unlock users)

## Quick Start

### Full Demo Stack (all-in-one)

One command starts the entire platform — all services, both SPAs, Nginx reverse proxy, and the full observability stack (Prometheus, Grafana, Loki).

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

- **DbGate**: Web-based database administration for PostgreSQL, Redis, RabbitMQ, and MinIO. See [docker/dbgate.md](docker/dbgate.md).
- **MinIO**: S3-compatible object storage (`:9000` API, `:9001` Console) for file uploads and assets.

### Individual Service Development

Each service ships three compose files covering every local workflow:

| File                     | Purpose                                                                                    |
| ------------------------ | ------------------------------------------------------------------------------------------ |
| `compose.yaml`           | Infrastructure only (PostgreSQL, RabbitMQ, MailHog, MinIO). Run the service from your IDE. |
| `compose.base.yaml`      | Shared service definitions — extended by the other two. Not used directly.                 |
| `compose.container.yaml` | Full stack — infrastructure + service built from source. No IDE required.                  |

**IDE workflow:**

```bash
# Start infrastructure only, run service from your IDE
cd foundation-iam-service
cp .env.example .env.local
docker compose up -d
./mvnw spring-boot:run -Pdev
# → API: http://localhost:8080  Swagger: http://localhost:8080/swagger-ui.html
```

**Fully containerised:**

```bash
# Build and run service + infrastructure from source
docker compose -f compose.container.yaml up -d --build
```

Each service uses isolated named volumes and a dedicated Docker network — running multiple services simultaneously requires no port remapping.

### Monitoring & Infrastructure

Access observability tools via the unified API domain:

- **Grafana**: [http://api.iqkv.local/services/grafana/](http://api.iqkv.local/services/grafana/)
- **Prometheus**: [http://api.iqkv.local/services/prometheus/](http://api.iqkv.local/services/prometheus/)
- **RabbitMQ**: [http://api.iqkv.local/services/rabbitmq/](http://api.iqkv.local/services/rabbitmq/)
- **MailHog**: [http://api.iqkv.local/services/mailhog/](http://api.iqkv.local/services/mailhog/)

**Database Administration:**

- **DbGate**: Web-based unified admin interface for all data stores (PostgreSQL, Redis, RabbitMQ, MinIO). See [docker/dbgate.md](docker/dbgate.md) for setup.

**Object Storage:**

- **MinIO**: S3-compatible storage for file uploads, avatars, and documents. Services use MinIO for asset storage with dedicated buckets per service.

## CI/CD & Deployment

Reference Drone CI pipelines and Helm charts are in the [`cicd/`](cicd/README.md) folder.

| Resource                                    | Description                                                                                |
| ------------------------------------------- | ------------------------------------------------------------------------------------------ |
| [`cicd/pipeline/`](cicd/pipeline/README.md) | Drone CI pipelines — Java microservices (10-stage flow), frontend apps, and infrastructure |
| [`cicd/chart/`](cicd/chart/README.md)       | Helm charts for Kubernetes — SIT, UAT, and production value files per chart                |

> Template reference only. Adapt to your own Drone CI instance and Helm charts repository.

## Domain Adaptability

Rapid deployment across industries:

- **B2B SaaS**: Multi-tenant schema isolation, invitation-based onboarding, per-tenant subscriptions
- **Single-Tenant Apps**: Per-user billing, default tenant auto-join, user-scoped subscriptions
- **Regulated Sectors**: Schema-per-tenant data isolation, audit trails, enumeration-safe auth flows

---

**Build faster** on infrastructure that handles complexity, focus on your business value.
