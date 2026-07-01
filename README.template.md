# 🏗️ IQ Key Value Backend Microservices

> Enterprise microservices foundation for scalable SaaS products. Essential business infrastructure—Identity, Payments, and API Gateway—on secure, multi-tenant architecture.

[![Project Site](https://img.shields.io/badge/Project-iqkv.dev-blue?style=for-the-badge&logo=appveyor)](https://iqkv.dev)
[![Live Demo](https://img.shields.io/badge/Live%20Demo-iqkv.site-success?style=for-the-badge&logo=rocket)](https://iqkv.site)
[![Tenant App](https://img.shields.io/badge/Tenant%20App-app.iqkv.site-informational?style=for-the-badge)](https://app.iqkv.site)
[![Platform Admin](https://img.shields.io/badge/Platform%20Admin-admin.iqkv.site-blueviolet?style=for-the-badge)](https://admin.iqkv.site)
[![Swagger UI](https://img.shields.io/badge/API%20Docs-swagger-orange?style=for-the-badge&logo=swagger)](https://api.iqkv.site/swagger-ui.html)

## Table of Contents

- [Live Demo](#live-demo)
- [Business Pillars](#business-pillars)
- [Service Ecosystem](#service-ecosystem)
- [Infrastructure](#infrastructure)
- [Quick Start](#quick-start)
- [CI/CD & Deployment](#cicd--deployment)
- [Adding a New Microservice](#adding-a-new-microservice)
- [Domain Adaptability](#domain-adaptability)

## Live Demo

The platform runs live at **[iqkv.site](https://iqkv.site)** with all services and both SPAs deployed.

| URL                                                                    | Description                                                     |
| ---------------------------------------------------------------------- | --------------------------------------------------------------- |
| [app.iqkv.site](https://app.iqkv.site)                                 | Tenant App — sign up, sign in, team management, invitations     |
| [admin.iqkv.site](https://admin.iqkv.site)                             | Platform Admin — users, organizations, subscriptions, audit log |
| [api.iqkv.site/swagger-ui.html](https://api.iqkv.site/swagger-ui.html) | Aggregated Swagger UI                                           |

> Observability tools (Grafana, Prometheus, RabbitMQ management, MailHog) are available in the local Docker demo only — see [Quick Start](#quick-start).

**Demo credentials** — the instance runs in `MULTI_TENANT` mode. Sign up freely at [app.iqkv.site](https://app.iqkv.site) to create an isolated tenant workspace. Platform Admin access and billing test credentials are available on request.

## Business Pillars

Critical domains to accelerate product development:

- **Identity & Access**: Multi-tenant authentication, magic link and OAuth2/OIDC social sign-in, invitation flows, in-app notifications, site-wide announcements, and role-based access control
- **Financial Operations**: Multi-gateway subscriptions (Stripe + Lemon Squeezy), FLAT and PER_SEAT pricing, plan catalog with trial support, entitlement evaluation, and webhook-driven billing
- **Frontend Experience**: Production-ready React 19 SPAs for tenants and platform administrators, plus a SaaS landing kit and documentation website to complete your product's public presence

<div align="center">
  <img src="https://github.com/dimdnk/dimdnk/blob/dev/screenshots/chrome_IS8q0S3OwG.gif?raw=true" width="800" alt="IQKV Platform — Platform Admin">
  <p><strong>Hybrid Tenancy SaaS Boilerplate. Microservice-first architecture, a collection of small, loosely coupled, and independently deployable services from day one.</strong></p>
</div>

## Service Ecosystem

Domain-aligned services with event-driven communication:

- **🔐 [IAM Service](foundation-iam-service/README.md)**: RS256 JWT authentication, magic link auth, OAuth2/OIDC social sign-in, account linking, tenant SSO, avatar uploads, tenant lifecycle, email verification, password reset, invitation flows, in-app notifications, site-wide announcements, tenant owner member management (ban/unban, edit authority, transfer ownership), platform admin user actions (ban/unban, unlock)
- **🌐 [Gateway Service](foundation-gateway-service/README.md)**: Reactive entry point with JWT validation, header sanitization, tenant context injection, platform mode guard
- **💰 [Billing Service](foundation-billing-service/README.md)**: Multi-gateway subscriptions (Stripe + Lemon Squeezy), FLAT/PER_SEAT pricing models, plan catalog with trial support, webhook processing, entitlement evaluation
- **📋 [Audit Service](foundation-audit-service/README.md)**: System-wide audit trails, activity tracking, compliance logging, event-driven log ingestion
- **📝 [CMS Service](foundation-cms-service/README.md)**: Content management for static pages, multi-language support, hierarchical content, and SEO-friendly metadata
- **💻 [Tenant App](foundation-ui-app/README.md)**: React 19 SPA for workspace members — OAuth2/OIDC sign-in, magic link auth, team management, invitations, billing self-service with entitlement-based feature gating, account profile
- **🛡️ [Platform Admin](foundation-ui-platform-admin/README.md)**: React 19 SPA for operators — global user/org management, subscription lifecycle, plan catalog CRUD, announcement management with translations, OIDC identity remediation
- **🚀 [SaaS Landing Kit](foundation-ui-saas-landing-kit/README.md)**: Modern, performant landing page built with Astro, React, Tailwind CSS, and shadcn/ui — includes authentication integration
- **📚 [Documentation Website](foundation-docs-website/README.md)**: VitePress-based documentation site with user guides, platform overview, and quick start instructions

## Infrastructure

Production-ready capabilities across all services:

- **Multi-Tenancy**: Schema-per-tenant PostgreSQL isolation with `MULTI_TENANT` and `SINGLE_TENANT` modes
- **Security**: RS256 JWT with two-layer revocation (JTI denylist + global signout timestamp), header sanitization to prevent spoofing, account lockout with manual unlock, OAuth2/OIDC social sign-in with server-side PKCE
- **Object Storage**: MinIO S3-compatible storage for file uploads, avatars, and documents
- **Database Administration**: DbGate web-based tool for PostgreSQL, Redis, RabbitMQ, and MinIO management
- **Frontend**: React 19, Mantine UI 9, TanStack Router & Query, Feature-Sliced Design (FSD), Astro (landing kit), VitePress (documentation)
- **Event-Driven**: RabbitMQ topic exchange with dead-letter queues — tenant provisioning, subscription lifecycle, billing notifications
- **Observability**: Correlation ID propagation, Prometheus metrics, Grafana dashboards, structured JSON logging
- **Distributed Locking**: ShedLock for scheduled jobs (token cleanup, trial notifications, stuck tenant recovery)
- **Member Management**: Tenant owner actions (ban/unban, edit authority, transfer ownership), platform admin actions (ban/unban users, unlock users)
- **Payments**: Multi-gateway strategy — Stripe and Lemon Squeezy with FLAT and PER_SEAT pricing models, entitlement evaluation, and plan-based feature gating

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

## Adding a New Microservice

[`cicd/pipeline/foundation-microservice-project-layout.yml`](cicd/pipeline/foundation-microservice-project-layout.yml) is the starting point. Copy it to your new service repository — it encodes the full 10-pipeline lifecycle, quality gates (JaCoCo, Checkstyle, SonarQube, PMD, SpotBugs), Helm deployment, and automated release flow.

### Checklist

1. **Maven module** — add to root `pom.xml`, inherit `com.iqkv:boot-parent-pom`
2. **Package structure** — `config/`, `domain/`, `repository/`, `service/`, `presentation/web/`, `presentation/admin/`, `security/`, `exception/`
3. **Cross-cutting concerns** (mandatory for every service):
   - Correlation ID propagation via MDC
   - Structured JSON logging (Logstash Logback Encoder)
   - Spring Actuator health probes on port 8081
   - Tenant + user context from Gateway-injected headers (`X-Tenant-ID`, `X-User-ID`, `X-User-Authorities`)
   - OAuth2 Resource Server config pointing to IAM JWKS endpoint
   - Micrometer metrics + `/actuator/prometheus`
   - SpringDoc OpenAPI annotations (Gateway aggregates specs)
   - RFC 7807 `ProblemDetail` error responses
   - `platform.rolloutMode` consistency check at startup
4. **Domain events** — publish to `iqkv.events` topic exchange using `your-domain.{verb}` routing keys; Audit Service captures them automatically
5. **Helm chart** — copy from `cicd/chart/foundation-billing-service`, update infra connection values, keep `platform.rolloutMode` and management service / probe config
6. **Pipeline** — copy `foundation-microservice-project-layout.yml`, adjust `helm --set` flags, configure Drone secrets
7. **ArchUnit tests** — copy architecture test class, update package prefix
8. **Monorepo registration** — add to `compose.demo.yaml`, add Gateway route, update this README

See the full [platform README](README.md#adding-a-new-microservice) for detailed guidance on each step.

## Domain Adaptability

Rapid deployment across industries:

- **B2B SaaS**: Multi-tenant schema isolation, invitation-based onboarding, per-tenant subscriptions
- **Single-Tenant Apps**: Per-user billing, default tenant auto-join, user-scoped subscriptions
- **Regulated Sectors**: Schema-per-tenant data isolation, audit trails, enumeration-safe auth flows

---

**Build faster** on infrastructure that handles complexity, focus on your business value.
