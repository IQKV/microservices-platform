# 💰 IQ Scaffold Billing Service

> Multi-tenant billing and payment orchestration service providing Stripe integration, merchant onboarding, and automated payment lifecycle management.

## Business Purpose

A core domain service for the IQ Scaffold platform that handles financial operations:

- **Payment Orchestration** - End-to-end management of payment intents, from creation to final settlement with external providers.
- **Merchant Onboarding** - Automated onboarding flow for platform merchants using Stripe Connect (Standard/Express).
- **Automated Payouts** - Tracking and reconciliation of payouts from the platform/gateway to merchant bank accounts.
- **Revenue Sharing** - Implementation of platform fees (application fees) on top of merchant transactions.
- **Compliance & Auditing** - Detailed audit trails for every payment state transition and external webhook event.
- **Multi-Tenant Finance** - Strict data isolation for financial records using schema-per-tenant architecture.

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

### 📧 Business Communication

- **Notification Engine**: Automated emails for onboarding and payment events via Thymeleaf templates.
- **Multi-lingual Support**: Internationalized message resolution for error codes and notifications.
- **Asynchronous Emailing**: Non-blocking email delivery using Spring's `@Async` support.

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

| Initial State | Event        | Target State | Notes                      |
| :------------ | :----------- | :----------- | :------------------------- |
| `null`        | Create       | `PENDING`    | Initial record creation    |
| `PENDING`     | API Call     | `PROCESSING` | Intent sent to Stripe      |
| `PROCESSING`  | Webhook      | `SUCCEEDED`  | Success confirmation       |
| `PROCESSING`  | Webhook      | `FAILED`     | Payment failed/declined    |
| `SUCCEEDED`   | Admin Action | `REFUNDED`   | Money returned to customer |
| `*`           | Webhook      | `CANCELED`   | Intent expired or canceled |

## Database & Multi-Tenancy

The service uses **Liquibase** for evolutionary database design with a schema-per-tenant isolation strategy:

1. **Public Schema**: Stores shared platform registry (e.g., `merchant_stripe_config` mapping tenants to Stripe Account IDs).
2. **Tenant Schemas**: Isolated storage for `payment`, `payment_audit_trail`, and `payout` records.

### Implementation Detail

On every request, the `TenantIdentifierResolver` extracts the tenant ID from the `X-Tenant-ID` header (propagated by the Gateway) and routes Hibernate sessions to the appropriate schema.

## Technical Highlights

- **Spring Boot 3.x**: Latest framework features for microservices.
- **Stripe Java SDK**: Deep integration with Stripe's advanced features.
- **Hibernate Multi-Tenancy**: Enterprise-grade data isolation.
- **Resilience**: Transactional integrity across local DB and external state updates.
- **Observability**: Structured logging with OpenTelemetry-ready context.

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
```
