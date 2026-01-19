# 🏗️ IQ Scaffold Backend Microservices

> Enterprise microservices foundation for scalable SaaS products. Essential business infrastructure—Identity, Payments, and CRM—on secure, multi-tenant architecture.

## Table of Contents

- [Business Pillars](#business-pillars)
- [Service Ecosystem](#service-ecosystem)
- [Infrastructure](#infrastructure)
- [Quick Start](#quick-start)
- [Domain Adaptability](#domain-adaptability)

## Business Pillars

Three critical domains to accelerate product development:

- **Identity & Access**: Multi-tenant authentication and role-based access control
- **Financial Operations**: Payment processing, merchant onboarding, subscriptions, and revenue sharing
- **CRM & Sales**: Complete lead-to-customer pipeline with scoring, conversion tracking, and analytics

## Service Ecosystem

Domain-aligned services with event-driven communication:

- **🔐 [User Service](iqscaffold-user-service/README.md)**: Authentication, user lifecycle, organization management
- **🌐 [Gateway Service](iqscaffold-gateway-service/README.md)**: Routing, rate limiting, JWT validation, circuit breaker
- **💰 [Billing Service](iqscaffold-billing-service/README.md)**: Payments, subscriptions, multi-gateway support, Stripe Connect
- **👥 [Contact Service](iqscaffold-contact-service/README.md)**: Contact management, bulk operations, lead conversion tracking
- **🎯 [Lead Service](iqscaffold-lead-service/README.md)**: Lead capture, scoring, qualification, assignment, conversion
- **📊 [Pipeline Service](iqscaffold-pipeline-service/README.md)**: Sales stages, follow-ups, analytics, conversion metrics

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
cd iqscaffold-user-service && docker-compose up -d
cd ../iqscaffold-gateway-service && docker-compose up -d

# Business services
cd ../iqscaffold-billing-service && docker-compose up -d
cd ../iqscaffold-lead-service && docker-compose up -d
cd ../iqscaffold-contact-service && docker-compose up -d
cd ../iqscaffold-pipeline-service && docker-compose up -d
```

Access APIs via Gateway: http://localhost:8081

## Domain Adaptability

Rapid deployment across industries:

- **B2B SaaS**: Multi-tenancy and organization management
- **Marketplaces**: Merchant onboarding and revenue orchestration
- **CRM/ERP**: Relationship tracking and sales pipeline
- **Regulated Sectors**: Data isolation and audit trails

---

**Build faster** on infrastructure that handles complexity, focus on your business value.
