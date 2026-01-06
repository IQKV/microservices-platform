# 💰 IQ Scaffold Billing Service

> Multi-tenant billing and payment orchestration service providing Stripe integration, merchant onboarding, automated payment lifecycle management, and comprehensive financial operations.

## Business Purpose

A core domain service for the IQ Scaffold platform that handles comprehensive financial operations:

- **Payment Orchestration** - End-to-end management of payment intents, from creation to final settlement with external providers.
- **Merchant Onboarding** - Automated onboarding flow for platform merchants using Stripe Connect (Standard/Express).
- **Automated Payouts** - Tracking and reconciliation of payouts from the platform/gateway to merchant bank accounts.
- **Revenue Sharing** - Implementation of platform fees (application fees) on top of merchant transactions.
- **Compliance & Auditing** - Detailed audit trails for every payment state transition and external webhook event.
- **Multi-Tenant Finance** - Strict data isolation for financial records using schema-per-tenant architecture.
- **Event-Driven Notifications** - Comprehensive email notification system with RabbitMQ messaging integration.
- **Refund Management** - Full and partial refund processing with automated state synchronization.

## Overview

The Billing Service acts as the financial engine of the IQ Scaffold ecosystem. It abstracts the complexities of payment gateways (primarily Stripe) while providing a multi-tenant-aware API for creating payments, managing refunds, and onboarding new merchants. It ensures that every transaction is tracked, audited, and correctly attributed to the appropriate tenant.

## What It Demonstrates

### 💳 Payment Lifecycle Patterns

- State machine-driven payment transitions (PENDING → PROCESSING → SUCCEEDED/FAILED → REFUNDED).
- Idempotent webhook processing for external event synchronization via `PaymentIntent` ID.
- Asynchronous payment status updates via Stripe webhooks with cryptographic validation.
- Support for platform fees (SaaS commission logic) automatically calculated per transaction.
- Detailed audit logging for every step of the payment journey using a dedicated audit entity.

### 🏦 Stripe Connect Integration

- **Standard/Express Onboarding**: Automated generation of onboarding links for merchants.
- **Direct & Destination Charges**: Support for complex payment flows with platform fees.
- **Account Capability Sync**: Automatic tracking of merchant "charges_enabled" and "payouts_enabled" status.
- **Connect Webhooks**: Handling events from connected accounts to sync local merchant state.

### 🏢 Multi-Tenancy & Data Isolation

- **Schema-per-Tenant**: Physical data separation for payments, payouts, and customer records.
- **Dynamic Connection Routing**: Hibernate-based multi-tenant connection provider.
- **Tenant Context Propagation**: Secure extraction of tenant ID from JWT claims or headers.
- **Liquibase Multi-Tenancy**: Automated schema migrations across all tenant schemas.

### 🔐 Security & Compliance

- **JWT Validation**: OAuth2 Resource Server integration for secure API access.
- **User Context Propagation**: Enrichment of transactions with user identity (userId, email).
- **Webhook Signature Verification**: Cryptographic validation of incoming Stripe events.
- **MDC Logging**: Correlation of logs with tenant, user, and payment identifiers.

### 📧 Business Communication & Messaging

- **Notification Engine**: Automated emails for onboarding and payment events via Thymeleaf templates.
- **Multi-lingual Support**: Internationalized message resolution for error codes and notifications.
- **Asynchronous Emailing**: Non-blocking email delivery using Spring's `@Async` support.
- **RabbitMQ Integration**: Event-driven messaging for billing events, notifications, and cross-service communication.
- **Event Publishing**: Publishes payment lifecycle events (created, succeeded, failed, refunded) to message queues.
- **Email Templates**: Rich HTML email templates for merchant onboarding, payment confirmations, and refund notifications.

## Architecture Patterns

### Payment Processing Flow

```text
Request Flow:
1. Client           → POST /api/v1/payments (amount, currency)
2. Billing Service  → Validate state transition (null -> PENDING)
3. Billing Service  → Resolve tenant-specific Stripe Connect Account
4. Billing Service  → Persist local Payment record (PENDING)
5. Billing Service  → Call Stripe API (Create PaymentIntent)
6. Stripe Gateway   → Return client_secret
7. Billing Service  → Return client_secret to Client
8. Stripe Webhook   → Receive payment_intent.succeeded
9. Billing Service  → Validate & update local state (PENDING -> SUCCEEDED)
```

### Key Components

- **PaymentStateMachine**: Encapsulates legal state transitions for financial integrity.
- **StripePaymentProvider**: Adapter for Stripe API, handling Connect accounts and application fees.
- **StripeWebhookService**: Secured entry point for external events with signature validation.
- **MerchantOnboardingService**: Orchestrates the Stripe Connect onboarding journey.
- **RefundService**: Manages the business logic and external calls for payment reversals.
- **PaymentAuditTrailService**: Logs all payment state changes for compliance and debugging.
- **MessagingService**: Publishes billing events and notification events to RabbitMQ.
- **EmailService**: Handles transactional email sending with SMTP integration.
- **NotificationService**: High-level notification orchestration combining email and event publishing.
- **PaymentNotificationService**: Business logic integration for payment-related notifications.

## API Endpoints

### Payment Operations

- `POST /api/v1/billing/payments/intent` - Create a payment intent (amount, currency, description)
- `GET /api/v1/billing/payments/{id}` - Retrieve detailed payment status and history
- `GET /api/v1/billing/payments` - Paginated list of payments for the current tenant
- `POST /api/v1/billing/payments/{id}/refund` - Process a full refund (Requires `ADMIN` role)

### Merchant Administration

- `POST /api/v1/admin/billing/merchants/onboard` - Initiate Stripe Connect onboarding (Requires `ADMIN` role)
- `GET /api/v1/admin/billing/merchants/status` - Check current merchant configuration and capability status

### Internal/Webhook

- `POST /api/v1/billing/webhooks/stripe` - Public endpoint for Stripe event consumption (Crypto-secured)

## Payment State Machine

The service enforces strict transitions to ensure financial consistency:

| Initial State | Event        | Target State         | Notes                      |
| :------------ | :----------- | :------------------- | :------------------------- |
| `null`        | Create       | `PENDING`            | Initial record creation    |
| `PENDING`     | API Call     | `PROCESSING`         | Intent sent to Stripe      |
| `PROCESSING`  | Webhook      | `SUCCEEDED`          | Success confirmation       |
| `PROCESSING`  | Webhook      | `FAILED`             | Payment failed/declined    |
| `SUCCEEDED`   | Admin Action | `REFUNDED`           | Money returned to customer |
| `SUCCEEDED`   | Webhook      | `PARTIALLY_REFUNDED` | Partial refund processed   |
| `*`           | Webhook      | `CANCELED`           | Intent expired or canceled |

**State Validation**: The `PaymentStateMachine` component validates all transitions to prevent invalid state changes (e.g., preventing a 'COMPLETED' payment from moving back to 'PENDING').

## Database & Multi-Tenancy

The service uses **Liquibase** for evolutionary database design with a schema-per-tenant isolation strategy:

1. **Public Schema**: Stores shared platform registry (e.g., `merchant_stripe_config` mapping tenants to Stripe Account IDs).
2. **Tenant Schemas**: Isolated storage for `payment`, `payment_audit_trail`, and `payout` records.

### Schema Structure

**Public Schema Tables:**

- `merchant_stripe_config`: Maps tenants to Stripe Connect accounts, stores capabilities and fee percentages

**Tenant Schema Tables:**

- `payment`: Payment records with status, amounts, Stripe intent IDs, application fees
- `payment_audit_trail`: Audit log of all payment state transitions
- `payout`: Payout records from Stripe
- `stripe_customer`: Customer records for Stripe integration

### Implementation Detail

On every request, the `TenantIdentifierResolver` extracts the tenant ID from the `X-Tenant-ID` header (propagated by the Gateway) and routes Hibernate sessions to the appropriate schema. The `TenantContext` ThreadLocal maintains the current tenant context throughout the request lifecycle.

## Technical Highlights

- **Spring Boot 3.x**: Latest framework features for microservices.
- **Stripe Java SDK**: Deep integration with Stripe's advanced features including Connect.
- **Hibernate Multi-Tenancy**: Enterprise-grade data isolation with schema-per-tenant.
- **Resilience4j**: Circuit breaker and time limiter patterns for external service calls.
- **Transactional Integrity**: Ensures consistency across local DB and external state updates.
- **OpenTelemetry Observability**: Structured logging with distributed tracing support.
- **RabbitMQ Messaging**: Event-driven architecture with reliable message delivery.
- **JWT Security**: OAuth2 Resource Server with comprehensive authentication and authorization.
- **Email Integration**: SMTP-based transactional email system with Thymeleaf templates.

## Messaging & Event Architecture

### Event Types Published

The service publishes events to RabbitMQ for cross-service communication:

- **Payment Events**: `billing.payment.created`, `billing.payment.succeeded`, `billing.payment.failed`, `billing.payment.refunded`
- **Merchant Events**: `billing.merchant.onboarded`
- **Invoice Events**: `billing.invoice.generated`
- **Notification Events**: `notification.email`

### RabbitMQ Configuration

- **Exchanges**: `iqscaffold.events`, `iqscaffold.billing`, `iqscaffold.dlx`
- **Queues**: `iqscaffold.billing.events`, `iqscaffold.billing.payments`, `iqscaffold.notifications`
- **Dead Letter Queues**: Failed message handling with retry logic

### Email Notifications

Automated email notifications for:

- Merchant onboarding instructions
- Payment successful/failed confirmations
- Payment refund notifications
- Invoice generation notifications

## Webhook Processing

### Supported Stripe Events

- `payment_intent.succeeded` - Updates payment to SUCCEEDED status
- `payment_intent.payment_failed` - Updates payment to FAILED status
- `charge.refunded` - Syncs refund status (REFUNDED/PARTIALLY_REFUNDED)
- `payout.paid` - Records payout entities
- `account.updated` - Updates merchant capabilities (charges_enabled, payouts_enabled)

### Security Features

- **Signature Verification**: Cryptographic validation of incoming Stripe events using webhook secrets
- **Tenant Resolution**: Automatic tenant context resolution from webhook metadata
- **Idempotent Processing**: Safe to process the same webhook multiple times

## Configuration

The service uses type-safe properties via `BillingProperties` (record-based):

```yaml
iqscaffold:
  billing:
    payment:
      provider: stripe
      saas-mode: true
      stripe:
        api-key: ${STRIPE_API_KEY}
        webhook-secret: ${STRIPE_WEBHOOK_SECRET}
        client-id: ${STRIPE_CLIENT_ID}
    integration:
      email-service:
        url: ${EMAIL_SERVICE_URL}
        timeout-ms: 10000
    notifications:
      enable-email-notifications: true
      enable-webhook-notifications: true
      retry-delay: PT5S
      max-retries: 3
  email:
    smtp:
      host: ${SMTP_HOST}
      port: ${SMTP_PORT}
      username: ${SMTP_USERNAME}
      password: ${SMTP_PASSWORD}
      auth: true
      starttls: true
    sender:
      from-email: billing@iqscaffold.com
      from-name: IQ Scaffold Billing
    templates:
      merchant-onboarding-template: email/merchant-onboarding.html
      payment-successful-template: email/payment-successful.html
      payment-failed-template: email/payment-failed.html
      payment-refunded-template: email/payment-refunded.html
  messaging:
    rabbitmq:
      exchanges:
        events: iqscaffold.events
        billing: iqscaffold.billing
      routing-keys:
        payment-created: billing.payment.created
        payment-succeeded: billing.payment.succeeded
        payment-failed: billing.payment.failed
        payment-refunded: billing.payment.refunded
        merchant-onboarded: billing.merchant.onboarded
        notification-email: notification.email
```

### Required Environment Variables

```bash
# Stripe Configuration
STRIPE_API_KEY=sk_live_...
STRIPE_PUBLIC_KEY=pk_live_...
STRIPE_WEBHOOK_SECRET=whsec_...
STRIPE_CLIENT_ID=ca_...

# Database Configuration
IQSCAFFOLD_DATABASE_URL=jdbc:postgresql://localhost:5432/iqscaffold_billing
IQSCAFFOLD_DATABASE_USERNAME=iqscaffold_billing
IQSCAFFOLD_DATABASE_PASSWORD=secure_password

# Redis Configuration
IQSCAFFOLD_CACHE_REDIS_HOST=localhost
IQSCAFFOLD_CACHE_REDIS_PORT=6379
IQSCAFFOLD_CACHE_REDIS_PASSWORD=redis_password

# RabbitMQ Configuration
IQSCAFFOLD_MESSAGING_RABBITMQ_HOST=localhost
IQSCAFFOLD_MESSAGING_RABBITMQ_USERNAME=iqscaffold
IQSCAFFOLD_MESSAGING_RABBITMQ_PASSWORD=rabbitmq_password

# Email Configuration
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=billing@iqscaffold.com
SMTP_PASSWORD=email_password

# Service Integration
IQSCAFFOLD_USER_SERVICE_URL=http://localhost:8080
EMAIL_SERVICE_URL=http://localhost:8084
```

## Observability & Monitoring

### Metrics

- Prometheus metrics at `/actuator/prometheus`
- HTTP request metrics (latency, success rates)
- Custom billing metrics (payment processing times, success rates)
- JVM metrics (memory, GC, threads)

### Logging

- Structured JSON logging via Logstash Logback Encoder
- Correlation ID tracking across requests
- Tenant and user context in all logs
- Payment event logging for audit trails

### Tracing

- OpenTelemetry integration for distributed tracing
- Trace ID and Span ID in logs
- Service-to-service tracing
- Performance metrics collection

### Health Checks

- Liveness probe: `/actuator/health/live`
- Readiness probe: `/actuator/health/ready`
- Database connectivity check
- Redis connectivity check
- RabbitMQ connectivity check

## Security Features

- **JWT Validation**: OAuth2 Resource Server with JWK Set validation from User Service
- **Role-Based Access Control**: Method-level security with @PreAuthorize (ADMIN, USER roles)
- **Webhook Signature Verification**: Cryptographic validation of Stripe events
- **Tenant Isolation**: Schema-per-tenant prevents cross-tenant data access
- **User Context Enrichment**: Audit logs include userId, email, tenant information
- **MDC Logging**: Correlation IDs for request tracing and debugging
- **CSRF Protection**: Configured appropriately for stateless API endpoints

## Development & Testing

### Key Dependencies

- Spring Boot 3.x (Web, Data JPA, Security, OAuth2, AMQP, Mail, Thymeleaf)
- PostgreSQL driver with Liquibase migrations
- Stripe Java SDK for payment processing
- JWT libraries (JJWT) for token validation
- Resilience4j for circuit breaker patterns
- Micrometer & OpenTelemetry for observability
- Testcontainers for integration testing

### Testing Strategy

- Unit tests with Mockito for service layer testing
- Integration tests with Testcontainers (PostgreSQL, RabbitMQ)
- Architecture tests with ArchUnit for structural validation
- Spring Modulith tests for modular architecture validation
- Webhook testing with signature verification

### API Documentation

- OpenAPI/Swagger documentation available at `/swagger-ui.html`
- Grouped APIs: Payments, Webhooks, Invoices, Subscriptions, Admin
- Interactive API testing interface
