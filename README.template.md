# 🏗️ IQ Key Value Backend Microservices

> Enterprise microservices foundation for scalable SaaS products. Essential business infrastructure—Identity, Payments, and CRM—on secure, multi-tenant architecture.

## Table of Contents

- [Business Pillars](#business-pillars)
- [Service Ecosystem](#service-ecosystem)
- [Infrastructure](#infrastructure)
- [Quick Start](#quick-start)
- [Domain Adaptability](#domain-adaptability)

## Business Pillars

Critical domains to accelerate product development:

- **Identity & Access**: Multi-tenant authentication and role-based access control
- **Financial Operations**: Payment processing, merchant onboarding, subscriptions, and revenue sharing

## Service Ecosystem

Domain-aligned services with event-driven communication:

- **🔐 [IAM Service](foundation-iam-service/README.md)**: Authentication, user lifecycle, organization management
- **🌐 [Gateway Service](foundation-gateway-service/README.md)**: Routing, rate limiting, JWT validation, circuit breaker
- **💰 [Billing Service](foundation-billing-service/README.md)**: Payments, subscriptions, multi-gateway support, Stripe Connect

## Infrastructure

Production-ready capabilities across all services:

- **Multi-Tenancy**: Schema-per-tenant data isolation
- **Security**: JWT-based authentication with user context propagation
- **Resilience**: Circuit breakers, rate limiting, distributed caching
- **Observability**: Distributed tracing, metrics, structured logging
- **Messaging**: Event-driven architecture with RabbitMQ

## Quick Start

Deploy with Docker Compose:

```bash
# Core services
cd foundation-iam-service && docker-compose up -d
cd ../foundation-gateway-service && docker-compose up -d

# Business services
cd ../foundation-billing-service && docker-compose up -d

```

Access APIs via Gateway: http://localhost:8080

## Domain Adaptability

Rapid deployment across industries:

- **B2B SaaS**: Multi-tenancy and organization management
- **Marketplaces**: Merchant onboarding and revenue orchestration
- **Regulated Sectors**: Data isolation and audit trails

---

**Build faster** on infrastructure that handles complexity, focus on your business value.
