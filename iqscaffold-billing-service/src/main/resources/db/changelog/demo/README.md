# Demo Billing Records

This directory contains demo billing data migrations for development and testing purposes. The migrations create a complete billing scenario for the `demo-tenant` organization with subscription, payment, and feature usage data.

## Demo Billing Scenario

### Organization Context
- **Tenant**: `demo-tenant` (Demo Tech Solutions)
- **Billing Email**: billing@demo.iqscaffold.com
- **Stripe Account**: acct_demo_tech_solutions
- **Application Fee**: 2.5% platform fee

## Demo Records Created

### 1. Enterprise Subscription Plan
- **Plan ID**: `550e8400-e29b-41d4-a716-446655440001`
- **Name**: Enterprise
- **Price**: $299.00/month USD
- **Trial Period**: 14 days
- **Features**: Advanced analytics, SSO integration, audit logging, third-party integrations
- **Limits**: 100 users, 500GB storage, 1M API calls/month
- **Support**: Enterprise level with 2-hour response time

**Plan Features Configuration:**
- **Advanced Analytics**: Unlimited dashboards, custom reports, 24-month data retention
- **API Calls**: 1M monthly quota with $0.001 overage rate
- **Storage**: 500GB quota with $0.10/GB overage rate
- **SSO Integration**: SAML, OAuth2, OIDC support with 10 max connections
- **Support**: Enterprise tier with dedicated manager and phone support

### 2. Active Tenant Subscription
- **Subscription ID**: `550e8400-e29b-41d4-a716-446655440010`
- **Status**: ACTIVE (converted from trial)
- **Current Period**: December 1, 2024 - January 1, 2025
- **Trial Period**: November 1-15, 2024 (completed successfully)
- **Stripe IDs**: 
  - Subscription: `sub_demo_tech_enterprise`
  - Customer: `cus_demo_tech_solutions`

**Subscription Lifecycle:**
- Started trial on November 1, 2024
- Trial ended November 15, 2024
- Converted to active subscription with first payment
- Auto-renewal enabled for continuous billing

### 3. Paid Invoice (December 2024)
- **Invoice ID**: `550e8400-e29b-41d4-a716-446655440020`
- **Invoice Number**: INV-DEMO-2024-12-001
- **Amount**: $299.00 (subtotal) + $26.18 (tax) = $325.18 total
- **Status**: PAID
- **Payment Date**: December 1, 2024 at 2:32 PM UTC
- **Payment Method**: Credit card ending in 4242

**Invoice Details:**
- Billing period: December 1, 2024 - January 1, 2025
- Tax rate: 8.75% (state/local taxes)
- Auto-collection enabled
- PDF and hosted invoice URLs available

### 4. Successful Payment
- **Payment ID**: `550e8400-e29b-41d4-a716-446655440030`
- **Amount**: $325.18 USD (including tax)
- **Status**: succeeded
- **Payment Intent**: `pi_demo_december_payment`
- **Platform Fee**: $8.13 (2.5% of $325.18)
- **Merchant Account**: acct_demo_tech_solutions

### 5. Subscription Audit Trail
- **Event**: Trial to Active conversion
- **Date**: December 1, 2024
- **Reason**: Trial period ended successfully, first payment processed
- **Details**: Payment method verified, billing address confirmed

### 6. Feature Usage Samples
**API Usage Tracking:**
- Endpoint: `/api/v1/analytics/reports`
- User: owner (Alice Johnson)
- Timestamp: December 15, 2024 10:30 AM UTC
- Metadata: Request/response sizes, processing time, cache status

**Advanced Analytics Usage:**
- Endpoint: `/api/v1/analytics/dashboard/custom`
- User: admin (Bob Smith)
- Timestamp: December 15, 2024 2:45 PM UTC
- Metadata: Custom dashboard with 8 widgets, 50K data points, PDF export

**SSO Integration Usage:**
- Endpoint: `/auth/sso/saml/login`
- User: manager (Emma Davis)
- Timestamp: December 15, 2024 9:15 AM UTC
- Metadata: Azure AD SAML authentication, 8-hour session

## Data Relationships

### System Schema (Public)
```
subscription_plan (Enterprise)
├── plan_feature (advanced_analytics)
├── plan_feature (api_calls_monthly)
├── plan_feature (storage_quota)
├── plan_feature (sso_integration)
└── plan_feature (support_level)

merchant_stripe_config
├── tenant_id: demo-tenant
├── stripe_account_id: acct_demo_tech_solutions
└── application_fee_percent: 2.50%
```

### Tenant Schema (demo-tenant)
```
tenant_subscription (Active)
├── subscription_invoice (Paid)
├── subscription_item (Enterprise plan)
├── tenant_subscription_audit_trail (Trial → Active)
└── payment (Successful)

feature_usage_log
├── api_calls_monthly usage
├── advanced_analytics usage
└── sso_integration usage
```

## Business Scenarios Demonstrated

### 1. Successful Trial Conversion
- Organization signs up for Enterprise plan with 14-day trial
- Trial period completes successfully
- First payment processes automatically
- Subscription converts to active status
- Audit trail records the conversion

### 2. Monthly Billing Cycle
- Subscription generates monthly invoice
- Invoice includes base plan cost plus applicable taxes
- Payment processes successfully via stored payment method
- Platform collects 2.5% application fee
- Invoice marked as paid with payment confirmation

### 3. Feature Usage Tracking
- Users access various platform features
- Usage is logged for quota enforcement
- API calls tracked against monthly limit
- Advanced features usage monitored
- SSO authentication events recorded

### 4. Multi-Tenant Billing Architecture
- System-wide plans and features in public schema
- Tenant-specific billing data in isolated schemas
- Merchant configurations link tenants to payment processors
- Feature usage isolated per tenant for accurate billing

## Testing Scenarios

### Subscription Management Testing
- Test subscription status transitions (trial → active → canceled)
- Verify billing cycle calculations and invoice generation
- Test payment processing and failure handling
- Validate subscription upgrade/downgrade workflows

### Feature Enforcement Testing
- Test quota limits and overage calculations
- Verify feature enablement based on subscription plan
- Test usage tracking accuracy and performance
- Validate feature access control

### Payment Processing Testing
- Test successful payment flows
- Test payment failure and retry logic
- Verify platform fee calculations
- Test refund and chargeback handling

### Multi-Tenant Isolation Testing
- Verify tenant data isolation in billing records
- Test cross-tenant feature usage separation
- Validate merchant configuration security
- Test tenant-specific billing customizations

## Development Usage

### Running Demo Migrations
```bash
# Run with demo context to include demo data
mvn liquibase:update -Dliquibase.contexts=demo

# Run system migrations only (no demo data)
mvn liquibase:update -Dliquibase.contexts=system

# Run tenant migrations only (no demo data)
mvn liquibase:update -Dliquibase.contexts=tenant
```

### Accessing Demo Data
Use the demo billing records to test:
- Subscription management APIs
- Invoice generation and payment processing
- Feature usage tracking and quota enforcement
- Billing analytics and reporting
- Payment gateway integrations

### Integration with User Service
The demo billing data is designed to work with the demo users from the user service:
- **Alice Johnson (owner)**: Can access billing management features
- **Carol Williams (billing)**: Has full billing administration access
- **David Brown (finance)**: Has read-only access to financial data
- **Bob Smith (admin)**: Can view subscription status but not billing details

## Rollback Support

All demo migrations include comprehensive rollback scripts that remove data in dependency order:
1. Feature usage logs
2. Subscription audit trail
3. Subscription items
4. Payments
5. Invoices
6. Tenant subscriptions
7. Plan features
8. Merchant configurations
9. Subscription plans

```bash
# Rollback demo migrations
mvn liquibase:rollback -Dliquibase.rollbackCount=2
```