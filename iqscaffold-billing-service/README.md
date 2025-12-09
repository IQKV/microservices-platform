# 💳 IQ Scaffold Billing Service

> Subscription billing and usage metering microservice handling subscription lifecycle management, payment processing, quota enforcement, invoice generation, and multi-provider payment integration.

## Business Purpose

A billing and monetization service that handles:

- **Subscription Management** - Full lifecycle from trial to active, including upgrades, downgrades, cancellations, and renewals
- **Payment Processing** - Multi-provider payment integration (Stripe, PayPal) with retry logic and idempotency guarantees
- **Invoice Generation** - Automated invoice creation with proration, usage charges, and line item calculations
- **Quota Enforcement** - Real-time usage tracking and quota limits with 5% grace period for soft enforcement
- **Usage Metering** - Track API calls, storage, email sends, and custom metrics for usage-based billing
- **Plan Management** - Flexible subscription plans with tiered pricing, billing cycles, and feature flags
- **Billing Analytics** - Revenue reporting, churn analysis, and subscription metrics
- **Customer Portal** - Self-service billing dashboard for customers to manage subscriptions and payments
- **GDPR Compliance** - Data export and deletion capabilities for regulatory compliance
- **Multi-Tenancy** - Tenant isolation with schema-per-tenant strategy for data segregation

## Overview

This service centralizes billing operations for the IQ Scaffold microservices platform, enabling other services to check quotas, record usage, and enforce subscription limits while maintaining consistent billing policies.

## What It Demonstrates

### 💳 Subscription Lifecycle Management

- State machine with 7 states (INCOMPLETE, TRIAL, ACTIVE, PAST_DUE, CANCELED, SUSPENDED, EXPIRED)
- Trial period management with eligibility checking (one trial per tenant)
- Automated trial expiration and conversion to paid
- Plan upgrades/downgrades with proration calculations
- Cancellation at period end vs immediate cancellation
- Subscription reactivation and renewal automation
- Grace period handling for failed payments

### 💰 Payment Processing Patterns

- Multi-provider architecture (Stripe, PayPal, Manual)
- Idempotency with Redis-backed caching (24-hour TTL)
- Automatic payment retry with exponential backoff
- Refund processing (full and partial)
- Payment method management with default selection
- Provider-agnostic payment result abstraction
- Webhook handling for payment provider events
- Payment status tracking (PENDING, SUCCEEDED, FAILED, REFUNDED)

### 📄 Invoice Generation

- Automated invoice generation for billing periods
- Proration calculations for mid-cycle plan changes
- Usage-based billing with line item details
- One-time charge invoicing
- Invoice status workflow (DRAFT, OPEN, PAID, VOID, UNCOLLECTIBLE)
- Due date management (7-day default)
- Financial precision with 2 decimal places and HALF_UP rounding
- Invoice number generation (INV-YYYYMM-XXXXX format)

### 📊 Usage Metering & Quota Enforcement

- Real-time usage tracking for multiple metric types (API_CALLS, STORAGE_GB, EMAIL_SENDS, ACTIVE_USERS)
- Quota checking with 5% grace period (soft limit before hard enforcement)
- Usage aggregation and reporting
- Approaching limit warnings at 90% threshold
- Unlimited quotas for enterprise plans
- Usage reset on billing cycle renewal
- Billing event audit trail

### 🎯 Plan Management

- Tiered pricing (FREE, PRO, ENTERPRISE)
- Multiple billing cycles (MONTHLY, YEARLY)
- Feature flags for plan capabilities
- Quota definitions per plan
- Trial period configuration
- Plan transition validation
- Active/inactive plan management

### 🏢 Multi-Tenancy Patterns

- Schema-per-tenant isolation with Hibernate MultiTenantConnectionProvider
- TenantContext for thread-local tenant management
- Tenant extraction from JWT claims via TenantExtractionFilter
- Per-tenant Liquibase migrations
- Tenant-scoped repositories and queries
- Cross-tenant operations via TenantContext.executeInTenantContext()

### 🔐 Security & Compliance

- JWT-based authentication with OAuth2 resource server
- Role-based access control (USER, ADMIN, SUPER_ADMIN)
- Rate limiting with Redis-backed counters
- Audit logging for billing operations
- GDPR data export and deletion
- Data retention policies with automated cleanup
- Secure webhook signature verification

### 🎯 Observability & Monitoring

- Structured JSON logging with correlation IDs
- OpenTelemetry distributed tracing
- Prometheus metrics integration
- Health checks and actuator endpoints
- Billing event tracking and audit trail

## Architecture Patterns

### Key Design Patterns

- **Aggregate Root Pattern**: Subscription, Invoice, Payment as aggregates
- **Domain Service Pattern**: InvoiceGenerator, ProrationCalculator, QuotaEnforcer
- **Repository Pattern**: Data access abstraction
- **Factory Pattern**: SubscriptionFactory, InvoiceFactory, PaymentProviderFactory
- **Specification Pattern**: QuotaExceededSpecification, TrialEligibilitySpecification
- **Strategy Pattern**: Multiple payment providers with common interface
- **State Machine Pattern**: Subscription status transitions with validation
- **Anti-Corruption Layer**: PaymentResult for provider-agnostic interface

### API Design

- RESTful endpoints with proper HTTP methods
- Versioning support (URL-based /api/v1)
- OpenAPI/Swagger documentation
- Problem Details (RFC 7807) for errors
- Consistent DTO pattern with Java records
- Pagination support for list endpoints

## Technical Highlights

### Subscription State Machine

```
INCOMPLETE → TRIAL, ACTIVE, EXPIRED
TRIAL → ACTIVE, EXPIRED, CANCELED
ACTIVE → PAST_DUE, CANCELED, SUSPENDED, EXPIRED
PAST_DUE → ACTIVE, EXPIRED, SUSPENDED
CANCELED → ACTIVE, EXPIRED
SUSPENDED → ACTIVE, EXPIRED
EXPIRED → (terminal state)
```

### Quota Grace Period Logic

- Base limit with 5% overage allowance
- Formula: `graceLimit = baseLimit * 1.05`
- Example: 1000 API calls → 1050 allowed before hard rejection
- Applies to all metric types except unlimited quotas

### Payment Idempotency

- Redis-backed cache with 24-hour TTL
- Idempotency key per payment request
- Prevents duplicate charges
- Returns cached result for duplicate requests

### Proration Calculations

- Time-based proration for plan changes
- Credit for unused time on old plan
- Charge for new plan prorated to period end
- Maintains 2 decimal precision
- Handles both upgrades and downgrades

### Performance Optimization

- Redis caching for quota checks and payment idempotency
- Connection pooling for database and payment providers
- Async payment processing with CompletableFuture
- Efficient usage aggregation queries
- Scheduled jobs for batch operations

### Data Management

- Liquibase for database migrations
- PostgreSQL with JSONB support for metadata
- Transaction management with @Transactional
- Soft deletes for audit trail
- Automated data retention cleanup

### Testing Approach

- Unit tests with JUnit 5
- Property-based testing with jqwik
- Integration tests with Testcontainers
- Architecture tests with ArchUnit
- Spring Modulith validation

### Operational Features

- Docker containerization
- Environment-specific profiles (local, staging, production)
- Graceful shutdown
- Circuit breakers with Resilience4j
- Health checks and actuator endpoints
- Scheduled jobs for renewals, retries, and cleanup

## Use Cases Implemented

### Subscription Lifecycle

- Start free trial (14 days default)
- Convert trial to paid subscription
- Upgrade plan with proration
- Downgrade plan with credit
- Cancel at period end (access until end)
- Cancel immediately (instant revocation)
- Reactivate canceled subscription
- Suspend subscription (admin action)
- Automatic renewal on billing cycle

### Payment Operations

- Process payment with idempotency
- Retry failed payments automatically
- Refund payment (full or partial)
- Add payment method
- Remove payment method
- Set default payment method
- List payment history
- Handle payment provider webhooks

### Invoice Management

- Generate invoice for billing period
- Generate invoice with usage charges
- Generate proration invoice for plan change
- Generate one-time charge invoice
- Mark invoice as paid
- Void invoice
- Mark invoice as uncollectible
- List invoices by tenant
- Download invoice PDF

### Usage Tracking

- Record usage for metric type
- Check quota availability
- Enforce quota limits with grace period
- Get usage summary for billing period
- Track approaching limit warnings
- Reset usage on billing cycle
- Aggregate usage across metrics

### Customer Portal

- View billing dashboard
- View current subscription
- View payment methods
- View invoice history
- Upgrade/downgrade plan
- Cancel subscription
- Update payment method
- View usage metrics

### Administrative Functions

- Create subscription plan
- Update plan pricing and quotas
- Deactivate plan
- View subscription analytics
- Generate revenue reports
- Analyze churn metrics
- Extend trial period
- Force subscription cancellation
- Void invoices

### GDPR Compliance

- Export billing data for user
- Delete billing data for user
- Apply data retention policies
- Audit data access

## API Examples

### Public Endpoints

- `GET /api/v1/plans` - List available subscription plans
- `GET /api/v1/plans/{id}` - Get plan details

### Protected Endpoints (Requires Authentication)

#### Subscription Management

- `POST /api/v1/subscriptions` - Create subscription
- `GET /api/v1/subscriptions/me` - Get my subscription
- `PUT /api/v1/subscriptions/{id}` - Update subscription
- `POST /api/v1/subscriptions/{id}/cancel` - Cancel subscription
- `POST /api/v1/subscriptions/{id}/reactivate` - Reactivate subscription
- `POST /api/v1/subscriptions/{id}/upgrade` - Upgrade plan
- `POST /api/v1/subscriptions/{id}/downgrade` - Downgrade plan

#### Payment Management

- `POST /api/v1/payments` - Process payment
- `GET /api/v1/payments/{id}` - Get payment details
- `POST /api/v1/payments/{id}/retry` - Retry failed payment
- `POST /api/v1/payments/{id}/refund` - Refund payment
- `GET /api/v1/payments/invoice/{invoiceId}` - List payments for invoice

#### Payment Method Management

- `POST /api/v1/payment-methods` - Add payment method
- `GET /api/v1/payment-methods` - List payment methods
- `DELETE /api/v1/payment-methods/{id}` - Remove payment method
- `POST /api/v1/payment-methods/{id}/default` - Set default payment method

#### Invoice Management

- `GET /api/v1/invoices` - List my invoices
- `GET /api/v1/invoices/{id}` - Get invoice details
- `GET /api/v1/invoices/{id}/pdf` - Download invoice PDF

#### Usage Tracking

- `GET /api/v1/usage/summary` - Get usage summary
- `GET /api/v1/usage/metrics` - Get usage metrics
- `GET /api/v1/usage/quota` - Check quota status

#### Customer Portal

- `GET /api/v1/portal/dashboard` - Get billing dashboard
- `POST /api/v1/portal/upgrade` - Upgrade plan
- `POST /api/v1/portal/downgrade` - Downgrade plan

### Admin Endpoints (Requires ADMIN/SUPER_ADMIN Role)

#### Plan Management

- `POST /api/v1/admin/plans` - Create plan
- `PUT /api/v1/admin/plans/{id}` - Update plan
- `DELETE /api/v1/admin/plans/{id}` - Delete plan

#### Subscription Administration

- `GET /api/v1/admin/subscriptions` - List all subscriptions
- `POST /api/v1/admin/subscriptions/{id}/suspend` - Suspend subscription
- `POST /api/v1/admin/subscriptions/{id}/extend-trial` - Extend trial
- `POST /api/v1/admin/subscriptions/{id}/cancel` - Force cancel

#### Invoice Administration

- `POST /api/v1/admin/invoices/{id}/void` - Void invoice
- `POST /api/v1/admin/invoices/{id}/uncollectible` - Mark uncollectible

#### Analytics

- `GET /api/v1/admin/analytics/revenue` - Revenue report
- `GET /api/v1/admin/analytics/churn` - Churn analysis
- `GET /api/v1/admin/analytics/subscriptions` - Subscription metrics

### Internal Endpoints (Service-to-Service)

- `POST /api/v1/internal/quota/check` - Check quota availability
- `POST /api/v1/internal/usage/record` - Record usage
- `POST /api/v1/internal/features/check` - Check feature access
- `GET /api/v1/internal/subscriptions/{tenantId}` - Get subscription status
- `GET /api/v1/internal/plans/{id}` - Get plan details

### Webhook Endpoints

- `POST /api/v1/webhooks/stripe` - Stripe webhook handler
- `POST /api/v1/webhooks/paypal` - PayPal webhook handler

### Monitoring Endpoints

- `/actuator/health` - Health status
- `/actuator/metrics` - Application metrics
- `/actuator/prometheus` - Prometheus metrics
- `/swagger-ui.html` - API documentation

## Learning Points

This implementation serves as a reference for:

- Building subscription billing systems with complex state machines
- Implementing multi-provider payment processing
- Designing usage-based billing with quota enforcement
- Calculating proration for mid-cycle plan changes
- Ensuring financial precision in monetary calculations
- Implementing idempotency for payment operations
- Building customer self-service billing portals
- GDPR compliance in billing systems
- Multi-tenant billing with data isolation
- Automated billing workflows with scheduled jobs

## Adapting for Your Domain

The patterns demonstrated here apply to various scenarios:

### SaaS Platforms

- Subscription management for software products
- Usage-based pricing models
- Tiered feature access
- Free trial conversions

### API Monetization

- API call metering and billing
- Rate limiting based on plan tier
- Overage charges for excess usage
- Pay-as-you-go pricing

### Marketplace Platforms

- Seller subscription management
- Transaction fee billing
- Commission calculations
- Multi-party payment splits

### Content Platforms

- Subscription tiers for content access
- Storage quota enforcement
- Bandwidth metering
- Creator monetization

## Integration with Other Services

### Consuming Billing Service

Other microservices check quotas and record usage:

```java
// Check quota before operation
@Autowired
private BillingClient billingClient;

public void performOperation() {
  QuotaCheckResponse quota = billingClient.checkQuota(tenantId, MetricType.API_CALLS, 1L);

  if (!quota.isAllowed()) {
    throw new QuotaExceededException(quota.getMessage());
  }

  // Perform operation

  // Record usage
  billingClient.recordUsage(tenantId, MetricType.API_CALLS, 1L);
}
```

### Subscription Status Checking

```java
// Check subscription status
SubscriptionStatusDto status = billingClient.getSubscriptionStatus(tenantId);

if (!status.isActive()) {
  throw new SubscriptionInactiveException("Subscription is not active");
}

// Check feature access
FeatureCheckResponse feature = billingClient.checkFeature(
  tenantId,
  "advanced_workflows"
);

if (!feature.isEnabled()) {
  throw new FeatureNotAvailableException("Feature not available in current plan");
}
```

### Quota Check Response Structure

```json
{
  "allowed": true,
  "currentUsage": 850,
  "limit": 1000,
  "remaining": 150,
  "percentageUsed": 85.0,
  "isApproachingLimit": false,
  "message": "Quota available"
}
```

### Subscription Status Response Structure

```json
{
  "subscriptionId": 123,
  "tenantId": "tenant-uuid",
  "status": "ACTIVE",
  "planTier": "PRO",
  "billingCycle": "MONTHLY",
  "currentPeriodStart": "2024-01-01T00:00:00Z",
  "currentPeriodEnd": "2024-02-01T00:00:00Z",
  "cancelAtPeriodEnd": false,
  "trialEnd": null,
  "features": {
    "advanced_workflows": true,
    "api_access": true,
    "priority_support": true
  }
}
```

## Scheduled Jobs

### Subscription Renewal Job

- Runs daily at 1:00 AM
- Processes subscriptions ending within 24 hours
- Generates invoices for next billing period
- Processes payments automatically
- Handles renewal failures with grace period

### Trial Expiration Job

- Runs daily at 2:00 AM
- Expires trials that have ended
- Sends trial expiration notifications
- Transitions to EXPIRED status

### Trial Reminder Job

- Runs daily at 10:00 AM
- Sends reminders 3 days before trial ends
- Encourages conversion to paid plan

### Payment Retry Job

- Runs every 6 hours
- Retries failed payments with exponential backoff
- Maximum 3 retry attempts
- Suspends subscription after final failure

### Usage Reset Job

- Runs on first day of month at 3:00 AM
- Resets monthly usage counters
- Archives previous period usage

### Data Retention Job

- Runs weekly on Sunday at 4:00 AM
- Deletes old billing events per retention policy
- Archives old invoices and payments
- Maintains audit trail

---

**Use this as a blueprint** for building subscription billing systems and implementing monetization in your microservices architecture. The code demonstrates production-ready patterns for payment processing, usage metering, quota enforcement, and subscription lifecycle management.
