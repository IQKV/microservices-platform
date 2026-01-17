# 🚀 IQ Scaffold Microservices Platform

> Production-ready microservices platform demonstrating modern architecture patterns, security best practices, and operational excellence for building scalable distributed systems.

## Table of Contents

- [Business Pillars](#business-pillars)
- [Platform Ecosystem](#platform-ecosystem)
- [Common Business Infrastructure](#common-business-infrastructure)
- [Architecture & Tech Stack](#architecture--tech-stack)
- [Getting Started](#getting-started)
- [Adapting for Your Domain](#adapting-for-your-domain)

## Business Pillars

The platform centralizes three critical business domains to accelerate product development:

- **Identity & Access Management**: Centralized authentication with JWT tokens, role-based access control (RBAC), and multi-tenant user lifecycle management.
- **Financial Orchestration**: End-to-end payment orchestration with Stripe Connect, automated merchant onboarding, and complex revenue/subscription management.
- **CRM & Growth Engine**: Comprehensive sales lifecycle—from lead capture and chronological activity tracking to visual pipeline management and contact relationship history.

## Platform Ecosystem

Our services are domain-aligned and maintain independent databases while communicating via a high-performance event bus:

- **🔐 [User Service](iqscaffold-user-service/README.md)**: Identity hub for global user lifecycles, permissions, and organization provisioning.
- **🌐 [Gateway Service](iqscaffold-gateway-service/README.md)**: Protective edge layer for intelligent routing, reactive authentication, and tenant-aware protection.
- **💰 [Billing Service](iqscaffold-billing-service/README.md)**: Financial engine for merchant onboarding and production-ready payment gateway orchestration.
- **👤 [Contact Service](iqscaffold-contact-service/README.md)**: Unified relationship repository for profile metadata and bulk segment management.
- **🎯 [Lead Service](iqscaffold-lead-service/README.md)**: Growth engine driving conversions with automated capture, audit trails, and collaborative engagement.
- **📊 [Pipeline Service](iqscaffold-pipeline-service/README.md)**: Sales orchestrator for dynamic workflows, automated tasks, and real-time performance dashboards.

## Common Business Infrastructure

All services inherit these production-ready enterprise capabilities:

- **Enterprise Multi-Tenancy**: Strict data isolation using schema-per-tenant separation, ensuring compliance and security across all business domains.
- **Distributed Security**: Unified RSA256 JWT security model with token blacklisting (Redis) and seamless user context propagation.
- **Resilient Operations**: Built-in fault tolerance (Resilience4j), adaptive rate limiting (Redis ZSET), and circuit breaker patterns to ensure high availability.
- **Advanced Observability**: Full-chain distributed tracing (OpenTelemetry), Prometheus metrics, and structured JSON logging.
- **Event-Driven Agility**: Loose coupling via RabbitMQ for asynchronous processes like notifications and state synchronization.

## Architecture & Tech Stack

```text
┌─────────────┐     ┌──────────────────────────────────┐
│   Clients   │────▶│    Gateway Service (Port 8081)   │
└─────────────┘     └────────────────┬─────────────────┘
                                     │
            ┌────────────────────────┼────────────────────────┐
            ▼                        ▼                        ▼
    ┌──────────────┐         ┌──────────────┐         ┌──────────────┐
    │ User Service │         │ Billing Service│       │ CRM Services │
    │ (Port 8080)  │         │ (Port 8082)  │         │ (8083-8085)  │
    └──────┬───────┘         └──────┬───────┘         └──────┬───────┘
           ▼                        ▼                        ▼
    PostgreSQL (Users)      PostgreSQL (Billing)     PostgreSQL (CRM)
```

- **Runtime:** Java 21+ with Spring Boot 3.5.6 & Spring Cloud 2025.0.0
- **Storage:** PostgreSQL 15+ (Liquibase), Redis 7 (Caching & Rate Limiting)
- **Messaging:** RabbitMQ 3.13 (Event-Driven Synchronization)
- **Security:** Spring Security OAuth2, JWT RSA256, JTI-based blacklisting
- **Infrastructure:** Docker & Docker Compose for local/production environments

## Getting Started

### Prerequisites

- Java 21+
- Docker and Docker Compose
- PostgreSQL 15+ & Redis (or use Docker Compose)

### Local Development

Run services with their dependencies using the provided Docker Compose configurations:

```bash
# Core Services
cd iqscaffold-user-service && docker-compose up -d
cd ../iqscaffold-gateway-service && docker-compose up -d

# Domain Services
cd ../iqscaffold-billing-service && docker-compose up -d
cd ../iqscaffold-contact-service && docker-compose up -d
# ... and so on
```

## Adapting for Your Domain

Built for rapid adaptation across diverse industries:

- **B2B SaaS**: Leverage strong multi-tenancy and organization-level RBAC.
- **Marketplaces**: Utilize automated merchant onboarding and complex revenue sharing.
- **Regulated Sectors**: Benefit from strict data isolation and comprehensive chronological audit trails.

---

**Fast-track your roadmap** by building on a foundation that handles the complex infrastructure, so you can focus on your unique business value.
