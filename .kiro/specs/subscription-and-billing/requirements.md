# Requirements Document

## Introduction

This document specifies the requirements for the Billing & Subscription Management Service within the IQ Scaffold Microservices Platform. The Billing Service (iqscaffold-billing-service) provides comprehensive subscription lifecycle management, usage-based billing, payment processing, and invoice management capabilities. It integrates with payment providers (Stripe, PayPal), and provides **simple REST APIs** that other business microservices (CRM, Campaign Service, Email Sender, Scoring Service, etc.) can easily call to check subscription status, verify feature access, and enforce quotas. This enables easy integration of billing restrictions across all platform services.

The service is designed to support flexible subscription plans (FREE, PRO, ENTERPRISE), multiple billing cycles (MONTHLY, YEARLY, LIFETIME), trial periods, proration for upgrades/downgrades, usage metering, and automated payment retry logic. It maintains strict tenant isolation, ensures PCI DSS compliance through payment provider abstraction, and provides comprehensive audit trails for all billing operations.

**Note:** This document uses a hybrid format combining user stories with detailed technical requirements. The initial requirements (1-6) follow the standard user story format with EARS-compliant acceptance criteria. The remaining sections (7-18) maintain the original detailed technical specification format with EARS notation for comprehensive coverage of all functional and non-functional requirements. This approach balances user-centric requirements with the technical depth needed for a complex billing system.

## Glossary

- **Billing Service**: The iqscaffold-billing-service microservice responsible for subscription and payment management
- **Subscription Plan**: A predefined tier of service (FREE, PRO, ENTERPRISE) with associated features, quotas, and pricing
- **Subscription**: An active or historical association between a tenant and a subscription plan
- **Tenant**: A customer organization using the IQ Scaffold platform
- **Feature**: A specific capability or function that can be enabled/disabled based on subscription plan (e.g., advanced workflows, AI features, bulk operations)
- **Quota**: A limit on resource usage defined by a subscription plan (API calls, storage, email sends, campaign executions, scoring requests, active users)
- **Usage Record**: A measurement of resource consumption (API calls, storage, users, email sends, etc.) for billing purposes and quota enforcement
- **Business Microservices**: Platform services that integrate with billing (CRM, Campaign Service, Email Sender, Scoring Service, Analytics, Automation, etc.)
- **User Service**: The iqscaffold-user-service microservice managing user and tenant data
- **Gateway Service**: The iqscaffold-gateway-service microservice handling request routing and access control
- **Payment Provider**: External payment processing service (Stripe, PayPal, or manual processing)
- **Invoice**: A billing document detailing charges for a subscription period
- **Proration**: Proportional billing adjustment when subscription changes mid-period
- **Trial Period**: A time-limited free access period for paid subscription plans
- **Billing Cycle**: The recurring period for subscription charges (MONTHLY, YEARLY, LIFETIME)
- **Payment Method**: A stored payment instrument (credit card, bank account, PayPal)
- **Webhook**: An HTTP callback from payment provider for event notifications
- **MRR**: Monthly Recurring Revenue
- **ARR**: Annual Recurring Revenue
- **Churn Rate**: The percentage of subscriptions canceled over a period

---

## Requirements

### Requirement 1: Service Infrastructure

**User Story:** As a platform architect, I want the billing service to be implemented as a properly configured microservice with appropriate infrastructure, so that it can scale independently and integrate seamlessly with the platform.

#### Acceptance Criteria

1. THE Billing Service SHALL be implemented as a Spring Boot microservice named iqscaffold-billing-service
2. THE Billing Service SHALL expose endpoints on port 8082
3. THE Billing Service SHALL use PostgreSQL 15 or higher as the primary database
4. THE Billing Service SHALL use Redis for caching subscription data and rate limiting
5. THE Billing Service SHALL use RabbitMQ for asynchronous operations including usage metering and invoice generation

### Requirement 1.1: Easy Integration APIs for Business Microservices

**User Story:** As a developer of business microservices (CRM, Campaign Service, Email Sender, Scoring Service, etc.), I want simple REST APIs to check subscription status, verify feature access, and enforce quotas, so that I can easily integrate billing restrictions into my service.

#### Acceptance Criteria

1. THE Billing Service SHALL provide the following integration APIs for business microservices:
   - `GET /api/v1/billing/subscriptions/{tenantId}/status` → Get subscription status (ACTIVE, TRIAL, EXPIRED, etc.)
   - `POST /api/v1/billing/subscriptions/{tenantId}/check-feature` → Check if tenant's plan includes a specific feature
   - `POST /api/v1/billing/usage/{tenantId}/check-quota` → Check if tenant has quota available for an operation
   - `POST /api/v1/billing/usage/{tenantId}/record` → Record usage for quota tracking and billing
   - `GET /api/v1/billing/subscriptions/{tenantId}/plan` → Get current plan details with features and quotas

2. THE Billing Service SHALL support checking features for different service contexts (e.g., "CRM.BULK_IMPORT", "EMAIL.CUSTOM_TEMPLATES", "SCORING.AI_MODELS")

3. THE Billing Service SHALL support quota checking for common metrics:
   - API_CALLS (across all services)
   - STORAGE_GB (file storage)
   - EMAIL_SENDS (email sender service)
   - CAMPAIGN_EXECUTIONS (campaign service)
   - SCORING_REQUESTS (scoring service)
   - ACTIVE_USERS (user seats)
   - CUSTOM_DOMAINS
   - DATA_EXPORTS

4. THE Billing Service SHALL return clear, actionable error messages when quotas are exceeded or features are not available, including upgrade URLs

5. THE Billing Service SHALL cache subscription status and plan details in Redis with 5-minute TTL to minimize database queries

6. THE Billing Service SHALL provide a Java client library (BillingClient) that business microservices can include as a dependency for easy integration

7. THE Billing Service SHALL document integration patterns with code examples for common use cases (quota enforcement, feature gating, usage recording)

### Requirement 2: Multi-Tenancy & Data Isolation

**User Story:** As a platform architect, I want strict tenant isolation for all billing data, so that customer billing information remains secure and separated.

#### Acceptance Criteria

1. THE Billing Service SHALL store subscription plans in the public schema shared across all tenants
2. THE Billing Service SHALL store subscriptions in tenant-scoped schemas
3. THE Billing Service SHALL store usage records in tenant-scoped schemas
4. THE Billing Service SHALL store invoices in tenant-scoped schemas
5. THE Billing Service SHALL store payment methods in tenant-scoped schemas
6. THE Billing Service SHALL store billing events in tenant-scoped schemas
7. THE Billing Service SHALL enforce tenant isolation using Hibernate multi-tenancy with schema-per-tenant strategy

### Requirement 3: Strict Security Integration

**User Story:** As a platform architect, I want the billing service to implement the same strict security patterns as the user service, so that authentication, authorization, and tenant isolation are consistent across the platform.

#### Acceptance Criteria

1. THE Billing Service SHALL use Spring Security with OAuth2 Resource Server for JWT-based authentication
2. THE Billing Service SHALL validate JWT tokens using the JWK endpoint from User Service (/.well-known/jwks.json)
3. THE Billing Service SHALL extract and validate the following JWT claims from access tokens:
   - `sub` (subject) - User ID
   - `username` - Username
   - `email` - User email
   - `roles` - User authorities (ADMIN, SUPER_ADMIN, USER, etc.)
   - `tenant_id` - Tenant ID for multi-tenancy
   - `firstName` - User first name
   - `lastName` - User last name
4. THE Billing Service SHALL implement TenantContext using ThreadLocal for tenant isolation (following user-service pattern)
5. THE Billing Service SHALL automatically set TenantContext from JWT `tenant_id` claim on each request
6. THE Billing Service SHALL add tenant ID to MDC (Mapped Diagnostic Context) for structured logging
7. THE Billing Service SHALL use @PreAuthorize annotations for authority-based access control on admin endpoints
8. THE Billing Service SHALL enforce the following authority-based access rules:
   - Public endpoints: Subscription plan listing (no authentication)
   - Customer endpoints: Authenticated users can manage their own tenant's subscription
   - Admin endpoints: Only users with ADMIN or SUPER_ADMIN authority can access admin billing operations
9. THE Billing Service SHALL implement RateLimitingFilter for API rate limiting (following user-service pattern)
10. THE Billing Service SHALL use stateless session management (SessionCreationPolicy.STATELESS)
11. THE Billing Service SHALL configure security headers (HSTS, frame options, content type options)
12. THE Billing Service SHALL disable CSRF for API endpoints (using JWT tokens)

### Requirement 3.1: Tenant Isolation & Context Management

**User Story:** As a platform architect, I want strict tenant isolation in the billing service, so that tenants cannot access or modify other tenants' billing data.

#### Acceptance Criteria

1. THE Billing Service SHALL implement TenantContext class with the following methods:
   - `setCurrentTenantId(String tenantId)` - Set tenant for current thread
   - `getCurrentTenantId()` - Get current tenant ID
   - `getCurrentTenantIdOrDefault()` - Get tenant ID or default
   - `hasTenantContext()` - Check if tenant context is set
   - `clear()` - Clear tenant context
   - `executeInTenantContext(String tenantId, Runnable)` - Execute code in tenant context
2. THE Billing Service SHALL create a TenantExtractionFilter that:
   - Extracts tenant_id from JWT token
   - Sets TenantContext for the request
   - Clears TenantContext after request completion
3. THE Billing Service SHALL validate that the tenant_id in the JWT matches the tenant_id in request paths (e.g., /api/v1/billing/subscriptions/{tenantId})
4. THE Billing Service SHALL throw AccessDeniedException if a user attempts to access another tenant's billing data
5. THE Billing Service SHALL use Hibernate multi-tenancy with CurrentTenantIdentifierResolver that reads from TenantContext
6. THE Billing Service SHALL ensure all database queries are automatically scoped to the current tenant schema

### Requirement 3.2: Service Integration

**User Story:** As a platform architect, I want the billing service to integrate securely with other platform services and external payment providers, so that billing operations can access necessary data and process payments securely.

#### Acceptance Criteria

1. THE Billing Service SHALL integrate with the User Service to retrieve user and tenant information using authenticated REST calls
2. THE Billing Service SHALL integrate with the Gateway Service for request routing and feature access control
3. THE Billing Service SHALL provide simple REST APIs that business microservices (CRM, Campaign Service, Email Sender, Scoring Service, etc.) can call to check subscription status and enforce quotas
4. THE Billing Service SHALL validate JWT tokens on all integration API endpoints to ensure only authenticated services can call them
5. THE Billing Service SHALL integrate with payment providers through an abstract PaymentProviderAdapter interface
6. THE Billing Service SHALL integrate with the Email Service for sending billing notifications and invoices
7. THE Billing Service SHALL publish subscription change events to RabbitMQ when subscriptions are created, upgraded, downgraded, or canceled
8. THE Billing Service SHALL accept usage data from all business microservices for billing and quota enforcement
9. THE Billing Service SHALL use service-to-service authentication for internal API calls (JWT tokens passed in Authorization header)

### Requirement 3.1: Service Structure

**User Story:** As a developer, I want a clear and organized service structure following domain-driven design principles, so that the codebase is maintainable and follows established patterns.

#### Acceptance Criteria

1. THE Billing Service SHALL organize code into domain-specific packages following the existing platform patterns
2. THE Billing Service SHALL implement the following package structure:

```
iqscaffold-billing-service/
├── src/main/java/com/iqscaffold/billingservice/
│   ├── integration/               # Integration APIs for business microservices
│   │   ├── SubscriptionStatusRestResource.java
│   │   ├── FeatureCheckRestResource.java
│   │   ├── QuotaCheckRestResource.java
│   │   ├── UsageRecordingRestResource.java
│   │   ├── SubscriptionStatusDto.java
│   │   ├── FeatureCheckRequest.java
│   │   ├── FeatureCheckResponse.java
│   │   ├── QuotaCheckRequest.java
│   │   ├── QuotaCheckResponse.java
│   │   ├── RecordUsageRequest.java
│   │   └── PlanDetailsDto.java
│   │
│   ├── subscription/              # Subscription lifecycle management
│   │   ├── Subscription.java      # Entity
│   │   ├── SubscriptionRepository.java
│   │   ├── SubscriptionService.java
│   │   ├── SubscriptionRestResource.java
│   │   ├── SubscriptionDto.java
│   │   ├── CreateSubscriptionRequest.java
│   │   ├── UpdateSubscriptionRequest.java
│   │   └── SubscriptionStatus.java  # Enum
│   │
│   ├── plan/                      # Subscription plan management
│   │   ├── SubscriptionPlan.java  # Entity
│   │   ├── SubscriptionPlanRepository.java
│   │   ├── SubscriptionPlanService.java
│   │   ├── SubscriptionPlanRestResource.java
│   │   ├── SubscriptionPlanDto.java
│   │   ├── CreatePlanRequest.java
│   │   ├── UpdatePlanRequest.java
│   │   ├── PlanTier.java          # Enum: FREE, PRO, ENTERPRISE
│   │   └── BillingCycle.java      # Enum: MONTHLY, YEARLY, LIFETIME
│   │
│   ├── usage/                     # Usage tracking & metering
│   │   ├── UsageRecord.java       # Entity
│   │   ├── UsageRecordRepository.java
│   │   ├── UsageMeteringService.java
│   │   ├── UsageRestResource.java
│   │   ├── UsageDto.java
│   │   ├── UsageSummaryDto.java
│   │   ├── QuotaStatus.java
│   │   ├── QuotaUsageDto.java
│   │   └── MetricType.java        # Enum: API_CALLS, STORAGE, USERS, CUSTOM
│   │
│   ├── payment/                   # Payment processing
│   │   ├── Payment.java           # Entity
│   │   ├── PaymentRepository.java
│   │   ├── PaymentService.java
│   │   ├── PaymentRestResource.java
│   │   ├── PaymentDto.java
│   │   ├── PaymentResult.java
│   │   ├── PaymentStatus.java     # Enum
│   │   ├── PaymentProviderAdapter.java  # Interface
│   │   ├── StripePaymentProvider.java
│   │   ├── PayPalPaymentProvider.java
│   │   └── ManualPaymentProvider.java
│   │
│   ├── paymentmethod/             # Payment method management
│   │   ├── PaymentMethod.java     # Entity
│   │   ├── PaymentMethodRepository.java
│   │   ├── PaymentMethodService.java
│   │   ├── PaymentMethodRestResource.java
│   │   ├── PaymentMethodDto.java
│   │   ├── AddPaymentMethodRequest.java
│   │   └── PaymentMethodType.java # Enum: CARD, BANK_ACCOUNT, PAYPAL
│   │
│   ├── invoice/                   # Invoice generation & management
│   │   ├── Invoice.java           # Entity
│   │   ├── InvoiceRepository.java
│   │   ├── InvoiceService.java
│   │   ├── InvoiceRestResource.java
│   │   ├── InvoiceDto.java
│   │   ├── InvoiceLineItem.java   # Value object
│   │   ├── InvoiceStatus.java     # Enum
│   │   └── InvoicePdfGenerator.java
│   │
│   ├── billing/                   # Billing cycles & charges
│   │   ├── BillingService.java
│   │   ├── ProrationService.java
│   │   ├── DunningService.java
│   │   └── BillingEventHandler.java
│   │
│   ├── portal/                    # Customer billing portal
│   │   ├── BillingPortalService.java
│   │   ├── BillingPortalRestResource.java
│   │   ├── BillingDashboardDto.java
│   │   ├── SubscriptionDetailsDto.java
│   │   └── UpgradeDowngradeRequest.java
│   │
│   ├── webhook/                   # Payment provider webhooks
│   │   ├── WebhookRestResource.java
│   │   ├── WebhookService.java
│   │   ├── StripeWebhookHandler.java
│   │   ├── PayPalWebhookHandler.java
│   │   └── WebhookEvent.java
│   │
│   ├── analytics/                 # Billing analytics (admin)
│   │   ├── BillingAnalyticsService.java
│   │   ├── BillingAnalyticsRestResource.java
│   │   ├── RevenueReportDto.java
│   │   ├── ChurnAnalysisDto.java
│   │   └── MRRDto.java
│   │
│   ├── config/                    # Configuration
│   │   ├── BillingServiceConfig.java
│   │   ├── DatabaseConfig.java
│   │   ├── RedisConfig.java
│   │   ├── RabbitMQConfig.java
│   │   ├── SecurityConfig.java
│   │   ├── OpenApiConfig.java
│   │   ├── ObservabilityConfig.java
│   │   ├── PaymentProviderConfig.java
│   │   └── BillingProperties.java  # @ConfigurationProperties
│   │
│   ├── security/                  # Security & validation
│   │   ├── BillingSecurityService.java
│   │   ├── TenantAccessValidator.java
│   │   ├── PaymentDataEncryption.java
│   │   └── WebhookSignatureValidator.java
│   │
│   ├── shared/                    # Shared utilities
│   │   ├── exception/
│   │   │   ├── SubscriptionNotFoundException.java
│   │   │   ├── QuotaExceededException.java
│   │   │   ├── PaymentFailedException.java
│   │   │   ├── InvalidPlanTransitionException.java
│   │   │   └── BillingExceptionHandler.java
│   │   ├── BillingConstants.java
│   │   ├── CurrencyUtils.java
│   │   ├── DateUtils.java
│   │   └── BillingAuditLog.java   # Entity
│   │
│   └── BillingServiceApplication.java
│
├── src/main/resources/
│   ├── db/changelog/
│   │   ├── system/
│   │   │   ├── master.xml
│   │   │   └── 20251205000000-subscription-plans-schema.xml
│   │   └── tenant/
│   │       ├── master.xml
│   │       ├── 20251205000100-subscriptions-schema.xml
│   │       ├── 20251205000200-usage-records-schema.xml
│   │       ├── 20251205000300-payment-methods-schema.xml
│   │       ├── 20251205000400-invoices-schema.xml
│   │       ├── 20251205000500-payments-schema.xml
│   │       └── 20251205000600-billing-events-schema.xml
│   │
│   ├── templates/
│   │   ├── email/
│   │   │   ├── subscription_created.html
│   │   │   ├── trial_ending.html
│   │   │   ├── invoice_generated.html
│   │   │   ├── payment_succeeded.html
│   │   │   ├── payment_failed.html
│   │   │   └── subscription_canceled.html
│   │   └── invoice/
│   │       └── invoice_template.html
│   │
│   ├── application.yml
│   ├── application-local.yml
│   ├── application-staging.yml
│   ├── application-production.yml
│   └── logback-spring.xml
│
└── src/test/java/com/iqscaffold/billingservice/
    ├── integration/
    │   ├── FeatureCheckServiceTest.java
    │   └── QuotaCheckIntegrationTest.java
    ├── subscription/
    │   ├── SubscriptionServiceTest.java
    │   └── SubscriptionIntegrationTest.java
    ├── plan/
    │   └── SubscriptionPlanServiceTest.java
    ├── usage/
    │   └── UsageMeteringServiceTest.java
    ├── payment/
    │   ├── PaymentServiceTest.java
    │   └── StripePaymentProviderTest.java
    ├── invoice/
    │   └── InvoiceServiceTest.java
    ├── billing/
    │   └── ProrationServiceTest.java
    └── architecture/
        └── BillingArchitectureTest.java
```

3. THE Billing Service SHALL follow the existing platform patterns for:
   - Entity naming and structure (extending TenantAware where applicable)
   - Repository interfaces (extending JpaRepository)
   - Service layer business logic
   - REST resource controllers (using @RestController)
   - DTO pattern with Java records
   - Exception handling with GlobalExceptionHandler
   - Configuration with @ConfigurationProperties

4. THE Billing Service SHALL use Liquibase for database migrations with separate system and tenant changesets
5. THE Billing Service SHALL implement comprehensive unit and integration tests following the platform testing patterns
6. THE Billing Service SHALL use ArchUnit for architecture validation tests

### Requirement 3.3: Security Configuration Implementation

**User Story:** As a developer, I want detailed security configuration following the user-service patterns, so that the billing service has consistent and robust security.

#### Acceptance Criteria

1. THE Billing Service SHALL create a SecurityConfig class annotated with @Configuration, @EnableWebSecurity, and @EnableMethodSecurity(prePostEnabled = true)
2. THE Billing Service SHALL configure a SecurityFilterChain bean with the following settings:
   ```java
   @Bean
   public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
     return http
       .csrf((csrf) -> csrf.ignoringRequestMatchers("/api/**", "/actuator/**"))
       .sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
       .authorizeHttpRequests((auth) ->
         auth
           // Public endpoints
           .requestMatchers("/api/v1/billing/plans/**")
           .permitAll()
           .requestMatchers("/actuator/health", "/actuator/info")
           .permitAll()
           .requestMatchers("/swagger-ui/**", "/v3/api-docs/**")
           .permitAll()
           // Integration APIs (require authentication)
           .requestMatchers("/api/v1/billing/subscriptions/*/status")
           .authenticated()
           .requestMatchers("/api/v1/billing/subscriptions/*/check-feature")
           .authenticated()
           .requestMatchers("/api/v1/billing/usage/**")
           .authenticated()
           // Customer portal (require authentication)
           .requestMatchers("/api/v1/billing/portal/**")
           .authenticated()
           // Admin endpoints (require ADMIN or SUPER_ADMIN authority)
           .requestMatchers("/api/v1/admin/billing/**")
           .hasAnyAuthority("ADMIN", "SUPER_ADMIN")
           // All other requests require authentication
           .anyRequest()
           .authenticated()
       )
       .oauth2ResourceServer((oauth2) -> oauth2.jwt((jwt) -> jwt.decoder(jwtDecoder)))
       .addFilterBefore(tenantExtractionFilter, UsernamePasswordAuthenticationFilter.class)
       .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
       .headers((headers) -> headers.frameOptions((frameOptions) -> frameOptions.deny()).httpStrictTransportSecurity((hsts) -> hsts.maxAgeInSeconds(31536000).includeSubDomains(true)))
       .build();
   }
   ```
3. THE Billing Service SHALL configure JwtDecoder to validate tokens using the User Service JWK endpoint:
   ```java
   @Bean
   public JwtDecoder jwtDecoder() {
     String jwkSetUri = userServiceUrl + "/.well-known/jwks.json";
     return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
   }
   ```
4. THE Billing Service SHALL create a TenantExtractionFilter that:
   - Runs before authentication filter
   - Extracts tenant_id from JWT claims
   - Sets TenantContext for the request
   - Validates tenant_id matches request path parameters
   - Clears TenantContext in finally block
5. THE Billing Service SHALL create a RateLimitingFilter using Redis for distributed rate limiting
6. THE Billing Service SHALL use BCryptPasswordEncoder with strength 12 for any password hashing needs
7. THE Billing Service SHALL configure CORS if needed for customer portal access

### Requirement 3.4: JWT Claims Extraction & Validation

**User Story:** As a developer, I want to extract and validate JWT claims consistently, so that user context is available throughout the billing service.

#### Acceptance Criteria

1. THE Billing Service SHALL create a JwtClaimNames class with constants matching the user-service:
   ```java
   public static final String SUBJECT = "sub";

   public static final String USERNAME = "username";

   public static final String EMAIL = "email";

   public static final String ROLES = "roles"; // Contains authorities (ADMIN, SUPER_ADMIN, USER)

   public static final String TENANT_ID = "tenant_id";

   public static final String FIRST_NAME = "firstName";

   public static final String LAST_NAME = "lastName";
   ```
2. THE Billing Service SHALL create a UserContext record to hold extracted JWT claims:
   ```java
   public record UserContext(
     Long userId,
     String username,
     String email,
     Set<String> authorities, // Authorities from JWT roles claim
     String firstName,
     String lastName,
     String tenantId
   ) {
     public boolean hasAuthority(String authority) {
       return authorities.contains(authority);
     }

     public boolean isAdmin() {
       return hasAuthority("ADMIN") || hasAuthority("SUPER_ADMIN");
     }
   }
   ```
3. THE Billing Service SHALL create a JwtUtils class to extract UserContext from JWT:
   ```java
   public static UserContext extractUserContext(Jwt jwt) {
     // Extract claims with type safety and null handling
   }
   ```
4. THE Billing Service SHALL validate that tenant_id claim is present in all authenticated requests
5. THE Billing Service SHALL throw AuthenticationException if required claims are missing from JWT

### Requirement 3.5: Configuration Patterns

**User Story:** As a developer, I want consistent configuration patterns following the platform conventions, so that configuration is type-safe, validated, and generates proper metadata.

#### Acceptance Criteria

1. THE Billing Service SHALL use the `iqscaffold.` prefix for all custom configuration properties
2. THE Billing Service SHALL implement configuration using Java records with `@ConfigurationProperties`
3. THE Billing Service SHALL create a `BillingProperties.java` record class with the following structure:

```java
@ConfigurationProperties(prefix = "iqscaffold.billing")
@Validated
public record BillingProperties(
  @Valid @NotNull Payment payment,
  @Valid @NotNull Subscription subscription,
  @Valid @NotNull Usage usage,
  @Valid @NotNull Invoice invoice,
  @Valid @NotNull Portal portal,
  @Valid @NotNull Features features
) {
  // Nested record classes for each configuration section
}
```

4. THE Billing Service SHALL use Jakarta validation annotations on all configuration properties
5. THE Billing Service SHALL generate `spring-configuration-metadata.json` for IDE autocomplete support
6. THE Billing Service SHALL organize configuration in `application.yml` with the following structure:

```yaml
iqscaffold:
  billing:
    security:
      jwt:
        jwk-set-uri: ${USER_SERVICE_URL:http://localhost:8081}/.well-known/jwks.json
        issuer: iqscaffold-user-service
      rate-limiting:
        enabled: true
        requests-per-minute: 100
        burst-capacity: 200
    integration:
      user-service:
        url: ${USER_SERVICE_URL:http://localhost:8081}
        timeout: 5s
      email-service:
        url: ${EMAIL_SERVICE_URL:http://localhost:8084}
        timeout: 10s
    payment:
      provider: stripe
      stripe:
        api-key: ${STRIPE_API_KEY}
        webhook-secret: ${STRIPE_WEBHOOK_SECRET}
      paypal:
        client-id: ${PAYPAL_CLIENT_ID}
        client-secret: ${PAYPAL_CLIENT_SECRET}
    subscription:
      default-currency: USD
      trial-days: 14
      grace-period-days: 3
      allow-multiple-subscriptions: false
    usage:
      metering-enabled: true
      batch-size: 100
      flush-interval: PT30S
      retention-days: 365
    invoice:
      number-format: "INV-{YEAR}{MONTH}-{SEQUENCE}"
      due-days: 7
      auto-finalize: true
      pdf-generation-enabled: true
    portal:
      enabled: true
      allow-plan-changes: true
      allow-cancellation: true
    features:
      proration: true
      dunning: true
      analytics: true
      webhooks: true
```

7. THE Billing Service SHALL use environment variables with `IQSCAFFOLD_BILLING_` prefix for sensitive configuration
8. THE Billing Service SHALL provide separate configuration files for each environment: `application-local.yml`, `application-staging.yml`, `application-production.yml`

### Requirement 3.3: Controller Naming and OpenAPI Annotations

**User Story:** As a developer, I want consistent REST controller naming and comprehensive OpenAPI documentation, so that APIs are well-documented and follow platform conventions.

#### Acceptance Criteria

1. THE Billing Service SHALL use the `-RestResource` suffix for all REST controller classes
2. THE Billing Service SHALL annotate all controllers with `@RestController` and `@RequestMapping`
3. THE Billing Service SHALL use `@Tag` annotation on each controller with name and description
4. THE Billing Service SHALL use `@Operation` annotation on each endpoint with:
   - `summary`: Brief description
   - `description`: Detailed description with features, requirements, and examples (using text blocks)
   - `tags`: Array of relevant tags

5. THE Billing Service SHALL use `@ApiResponses` annotation on each endpoint documenting:
   - Success responses (200, 201, 204) with `@Content` and `@Schema`
   - Error responses (400, 401, 403, 404, 409, 429, 500) with references to common responses
   - Example responses using `@ExampleObject` with realistic JSON

6. THE Billing Service SHALL use `@SecurityRequirement` annotation on protected endpoints
7. THE Billing Service SHALL configure SpringDoc OpenAPI with groups for logical API organization:

```yaml
springdoc:
  api-docs:
    enabled: true
    path: /api-docs
    groups:
      enabled: true
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
    groups-order: ASC
    operations-sorter: alpha
    tags-sorter: alpha
  group-configs:
    - group: subscription-plans
      display-name: Subscription Plan APIs
      paths-to-match: /api/*/billing/plans/**
    - group: subscriptions
      display-name: Subscription Management APIs
      paths-to-match: /api/*/billing/subscriptions/**, /api/*/billing/portal/subscription/**
    - group: payments
      display-name: Payment APIs
      paths-to-match: /api/*/billing/payments/**, /api/*/billing/portal/payment-methods/**
    - group: invoices
      display-name: Invoice APIs
      paths-to-match: /api/*/billing/invoices/**, /api/*/billing/portal/invoices/**
    - group: usage
      display-name: Usage & Metering APIs
      paths-to-match: /api/*/billing/usage/**, /api/*/billing/portal/usage/**
    - group: webhooks
      display-name: Webhook APIs
      paths-to-match: /api/*/billing/webhooks/**
    - group: admin
      display-name: Admin Billing APIs
      paths-to-match: /api/*/admin/billing/**
```

8. THE Billing Service SHALL follow this controller example pattern:

```java
@RestController
@RequestMapping("/api/v1/billing/subscriptions")
@Tag(name = "Subscriptions", description = "Subscription lifecycle management and operations")
public class SubscriptionRestResource {

  @Operation(
    summary = "Create subscription",
    description = """
    Create a new subscription for a tenant.

    ## Features
    - Automatic trial period activation if configured
    - Payment method validation for paid plans
    - Tenant isolation enforcement
    - Subscription status tracking

    ## Business Rules
    - Only one active subscription per tenant
    - Free plans activate immediately
    - Paid plans require payment method (unless trial available)
    """,
    tags = { "Subscriptions" }
  )
  @ApiResponses(
    value = {
      @ApiResponse(
        responseCode = "201",
        description = "Subscription created successfully",
        content = @Content(
          mediaType = "application/json",
          schema = @Schema(implementation = SubscriptionDto.class),
          examples = @ExampleObject(
            name = "Trial Subscription",
            value = """
            {
              "id": 123,
              "tenantId": "tenant-abc",
              "planCode": "pro-monthly",
              "status": "TRIAL",
              "trialEnd": "2024-12-19T23:59:59Z"
            }
            """
          )
        )
      ),
      @ApiResponse(responseCode = "400", description = "Invalid request", ref = "#/components/responses/BadRequest"),
      @ApiResponse(responseCode = "409", description = "Subscription already exists", ref = "#/components/responses/Conflict"),
    }
  )
  @PostMapping
  public ResponseEntity<SubscriptionDto> createSubscription(@Valid @RequestBody CreateSubscriptionRequest request) {
    // Implementation
  }
}
```

9. THE Billing Service SHALL use `@Timed` annotation from Micrometer on all endpoints for metrics collection
10. THE Billing Service SHALL define common error responses in OpenAPI configuration for reuse across endpoints

### Requirement 3.4: Liquibase Database Migration Patterns

**User Story:** As a developer, I want consistent Liquibase migration patterns following the platform conventions, so that database changes are versioned, repeatable, and support multi-tenancy.

#### Acceptance Criteria

1. THE Billing Service SHALL organize Liquibase changesets in the following directory structure:

```
src/main/resources/db/changelog/
├── system/
│   ├── master.xml
│   └── 00000000000000-create-subscription-plans-table.xml
└── tenant/
    ├── master.xml
    ├── 20251205000100-subscriptions-schema.xml
    ├── 20251205000200-usage-records-schema.xml
    ├── 20251205000300-payment-methods-schema.xml
    ├── 20251205000400-invoices-schema.xml
    ├── 20251205000500-payments-schema.xml
    └── 20251205000600-billing-events-schema.xml
```

2. THE Billing Service SHALL use XML format for all Liquibase changesets
3. THE Billing Service SHALL use the following naming conventions:
   - **System changesets**: `00000000000000-descriptive-name.xml` (14-digit zero-padded sequence)
   - **Tenant changesets**: `YYYYMMDDHHMMSS-descriptive-name.xml` (timestamp-based)

4. THE Billing Service SHALL include the standard XML header in all changeset files:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.29.xsd">
    <!-- changesets here -->
</databaseChangeLog>
```

5. THE Billing Service SHALL use the following changeSet ID format:
   - **System changesets**: `{file-prefix}-{sequence}` (e.g., `00000000000001`, `00000000000002`)
   - **Tenant changesets**: `{timestamp}-{sequence}-{description}` (e.g., `20251205000100-000001-create-subscriptions-table`)

6. THE Billing Service SHALL set author to `iqscaffold` for all changesets
7. THE Billing Service SHALL include descriptive comments in each changeSet using `<comment>` tag
8. THE Billing Service SHALL create master.xml files that include all changesets in order:

```xml
<!-- system/master.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.29.xsd">

    <include file="db/changelog/system/00000000000000-create-subscription-plans-table.xml"/>

</databaseChangeLog>
```

```xml
<!-- tenant/master.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.29.xsd">

    <include file="db/changelog/20251205000100-subscriptions-schema.xml"/>
    <include file="db/changelog/20251205000200-usage-records-schema.xml"/>
    <include file="db/changelog/20251205000300-payment-methods-schema.xml"/>
    <include file="db/changelog/20251205000400-invoices-schema.xml"/>
    <include file="db/changelog/20251205000500-payments-schema.xml"/>
    <include file="db/changelog/20251205000600-billing-events-schema.xml"/>

</databaseChangeLog>
```

9. THE Billing Service SHALL follow these table creation patterns:
   - Use `BIGINT` with `autoIncrement="true"` for primary key ID columns
   - Use `VARCHAR` with appropriate length constraints for string columns
   - Use `TIMESTAMP` with `defaultValueComputed="CURRENT_TIMESTAMP"` for audit timestamps
   - Use `BOOLEAN` with `defaultValueBoolean` for boolean columns
   - Use `DECIMAL(10,2)` for monetary amounts
   - Use `JSONB` for flexible JSON data (PostgreSQL)

10. THE Billing Service SHALL create indexes immediately after table creation in separate changesets
11. THE Billing Service SHALL use descriptive index names: `idx_{table}_{column(s)}` (e.g., `idx_subscriptions_tenant_id`)
12. THE Billing Service SHALL add foreign key constraints with descriptive names: `fk_{table}_{column}` (e.g., `fk_subscription_plan`)
13. THE Billing Service SHALL specify `onDelete` behavior for foreign keys (CASCADE, SET NULL, RESTRICT)
14. THE Billing Service SHALL add check constraints for data validation where appropriate
15. THE Billing Service SHALL use `<preConditions>` for database-specific SQL (e.g., PostgreSQL-only constraints)
16. THE Billing Service SHALL provide `<rollback>` blocks for all changesets where applicable
17. THE Billing Service SHALL insert seed data (e.g., default subscription plans) in separate changesets
18. THE Billing Service SHALL group related operations in single changesets (e.g., table creation with its columns)
19. THE Billing Service SHALL separate index creation, constraint addition, and data insertion into distinct changesets

Example changeset structure:

```xml
<changeSet id="20251205000100-000001-create-subscriptions-table" author="iqscaffold">
    <comment>Create subscriptions table for subscription lifecycle management</comment>

    <createTable tableName="subscriptions">
        <column name="id" type="BIGINT" autoIncrement="true">
            <constraints primaryKey="true" nullable="false"/>
        </column>
        <column name="tenant_id" type="VARCHAR(100)">
            <constraints nullable="false"/>
        </column>
        <column name="user_id" type="BIGINT">
            <constraints nullable="false"/>
        </column>
        <column name="plan_id" type="BIGINT">
            <constraints nullable="false"/>
        </column>
        <column name="status" type="VARCHAR(20)">
            <constraints nullable="false"/>
        </column>
        <column name="current_period_start" type="TIMESTAMP">
            <constraints nullable="true"/>
        </column>
        <column name="current_period_end" type="TIMESTAMP">
            <constraints nullable="true"/>
        </column>
        <column name="trial_start" type="TIMESTAMP">
            <constraints nullable="true"/>
        </column>
        <column name="trial_end" type="TIMESTAMP">
            <constraints nullable="true"/>
        </column>
        <column name="canceled_at" type="TIMESTAMP">
            <constraints nullable="true"/>
        </column>
        <column name="cancel_at_period_end" type="BOOLEAN" defaultValueBoolean="false">
            <constraints nullable="false"/>
        </column>
        <column name="metadata" type="JSONB">
            <constraints nullable="true"/>
        </column>
        <column name="created_at" type="TIMESTAMP" defaultValueComputed="CURRENT_TIMESTAMP">
            <constraints nullable="false"/>
        </column>
        <column name="updated_at" type="TIMESTAMP" defaultValueComputed="CURRENT_TIMESTAMP">
            <constraints nullable="false"/>
        </column>
    </createTable>
</changeSet>

<changeSet id="20251205000100-000002-add-subscriptions-indexes" author="iqscaffold">
    <comment>Create indexes on subscriptions table for query performance</comment>

    <createIndex indexName="idx_subscriptions_tenant_id" tableName="subscriptions">
        <column name="tenant_id"/>
    </createIndex>

    <createIndex indexName="idx_subscriptions_user_id" tableName="subscriptions">
        <column name="user_id"/>
    </createIndex>

    <createIndex indexName="idx_subscriptions_plan_id" tableName="subscriptions">
        <column name="plan_id"/>
    </createIndex>

    <createIndex indexName="idx_subscriptions_status" tableName="subscriptions">
        <column name="status"/>
    </createIndex>

    <createIndex indexName="idx_subscriptions_period_end" tableName="subscriptions">
        <column name="current_period_end"/>
    </createIndex>
</changeSet>

<changeSet id="20251205000100-000003-add-subscriptions-constraints" author="iqscaffold">
    <comment>Add foreign key and check constraints to subscriptions table</comment>

    <addForeignKeyConstraint
            baseTableName="subscriptions"
            baseColumnNames="plan_id"
            constraintName="fk_subscription_plan"
            referencedTableName="subscription_plans"
            referencedColumnNames="id"
            onDelete="RESTRICT"/>

    <sql>
        ALTER TABLE subscriptions
            ADD CONSTRAINT chk_subscriptions_status
                CHECK (status IN ('TRIAL', 'ACTIVE', 'PAST_DUE', 'CANCELED', 'EXPIRED', 'SUSPENDED', 'INCOMPLETE'));
    </sql>

    <rollback>
        <dropForeignKeyConstraint baseTableName="subscriptions" constraintName="fk_subscription_plan"/>
        <sql>ALTER TABLE subscriptions DROP CONSTRAINT IF EXISTS chk_subscriptions_status;</sql>
    </rollback>
</changeSet>
```

20. THE Billing Service SHALL configure Liquibase properties in `application.yml`:

```yaml
spring:
  liquibase:
    enabled: false # Disabled globally - run programmatically per schema

iqscaffold:
  liquibase:
    systemChangeLog: classpath:db/changelog/system/master.xml
    tenantChangeLog: classpath:db/changelog/tenant/master.xml
```

### Requirement 3.5: Constants and String Literals

**User Story:** As a developer, I want all repeatable strings centralized in constants classes, so that the codebase is maintainable and changes can be made in one place.

#### Acceptance Criteria

1. THE Billing Service SHALL create a `BillingConstants.java` class in the `shared` package
2. THE Billing Service SHALL declare the constants class as `public final` with a private constructor that throws `UnsupportedOperationException`
3. THE Billing Service SHALL organize constants into nested static final classes by category
4. THE Billing Service SHALL use the following constant categories:

```java
package com.iqscaffold.billingservice.shared;

/**
 * Common constants used across the Billing Service.
 * Centralizes repeatable strings for HTTP headers, MDC keys, billing events,
 * subscription statuses, and other shared values to ensure consistency and maintainability.
 */
public final class BillingConstants {

  private BillingConstants() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  /**
   * HTTP Header names used for request/response tracking and billing context.
   */
  public static final class Headers {

    private Headers() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String X_CORRELATION_ID = "X-Correlation-ID";
    public static final String X_REQUEST_ID = "X-Request-ID";
    public static final String X_TENANT_ID = "X-Tenant-ID";
    public static final String X_SUBSCRIPTION_ID = "X-Subscription-ID";
    public static final String X_IDEMPOTENCY_KEY = "X-Idempotency-Key";
  }

  /**
   * MDC (Mapped Diagnostic Context) keys for structured logging.
   */
  public static final class MDC {

    private MDC() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String CORRELATION_ID = "correlationId";
    public static final String REQUEST_ID = "requestId";
    public static final String TENANT_ID = "tenant.id";
    public static final String SUBSCRIPTION_ID = "subscriptionId";
    public static final String INVOICE_ID = "invoiceId";
    public static final String PAYMENT_ID = "paymentId";
  }

  /**
   * Subscription status values.
   */
  public static final class SubscriptionStatus {

    private SubscriptionStatus() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String TRIAL = "TRIAL";
    public static final String ACTIVE = "ACTIVE";
    public static final String PAST_DUE = "PAST_DUE";
    public static final String CANCELED = "CANCELED";
    public static final String EXPIRED = "EXPIRED";
    public static final String SUSPENDED = "SUSPENDED";
    public static final String INCOMPLETE = "INCOMPLETE";
  }

  /**
   * Billing event types for audit and tracking.
   */
  public static final class BillingEvents {

    private BillingEvents() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // Subscription Events
    public static final String SUBSCRIPTION_CREATED = "SUBSCRIPTION_CREATED";
    public static final String SUBSCRIPTION_UPDATED = "SUBSCRIPTION_UPDATED";
    public static final String SUBSCRIPTION_UPGRADED = "SUBSCRIPTION_UPGRADED";
    public static final String SUBSCRIPTION_DOWNGRADED = "SUBSCRIPTION_DOWNGRADED";
    public static final String SUBSCRIPTION_CANCELED = "SUBSCRIPTION_CANCELED";
    public static final String SUBSCRIPTION_EXPIRED = "SUBSCRIPTION_EXPIRED";
    public static final String SUBSCRIPTION_REACTIVATED = "SUBSCRIPTION_REACTIVATED";

    // Trial Events
    public static final String TRIAL_STARTED = "TRIAL_STARTED";
    public static final String TRIAL_ENDING_SOON = "TRIAL_ENDING_SOON";
    public static final String TRIAL_ENDED = "TRIAL_ENDED";
    public static final String TRIAL_CONVERTED = "TRIAL_CONVERTED";

    // Payment Events
    public static final String PAYMENT_SUCCEEDED = "PAYMENT_SUCCEEDED";
    public static final String PAYMENT_FAILED = "PAYMENT_FAILED";
    public static final String PAYMENT_REFUNDED = "PAYMENT_REFUNDED";
    public static final String PAYMENT_METHOD_ADDED = "PAYMENT_METHOD_ADDED";
    public static final String PAYMENT_METHOD_REMOVED = "PAYMENT_METHOD_REMOVED";

    // Invoice Events
    public static final String INVOICE_GENERATED = "INVOICE_GENERATED";
    public static final String INVOICE_PAID = "INVOICE_PAID";
    public static final String INVOICE_VOIDED = "INVOICE_VOIDED";

    // Usage Events
    public static final String QUOTA_EXCEEDED = "QUOTA_EXCEEDED";
    public static final String QUOTA_WARNING = "QUOTA_WARNING";
    public static final String USAGE_RECORDED = "USAGE_RECORDED";
  }

  /**
   * Cache names used throughout the billing service.
   */
  public static final class CacheNames {

    private CacheNames() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String SUBSCRIPTION_PLANS = "subscription-plans";
    public static final String SUBSCRIPTIONS = "subscriptions";
    public static final String USAGE_QUOTAS = "usage-quotas";
    public static final String PAYMENT_METHODS = "payment-methods";
  }

  /**
   * Payment provider names.
   */
  public static final class PaymentProviders {

    private PaymentProviders() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String STRIPE = "stripe";
    public static final String PAYPAL = "paypal";
    public static final String MANUAL = "manual";
  }

  /**
   * Invoice number format and patterns.
   */
  public static final class InvoiceFormat {

    private InvoiceFormat() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String NUMBER_PREFIX = "INV-";
    public static final String NUMBER_PATTERN = "INV-{YEAR}{MONTH}-{SEQUENCE}";
    public static final String DATE_FORMAT = "yyyyMM";
  }

  /**
   * Default values for billing operations.
   */
  public static final class Defaults {

    private Defaults() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String CURRENCY = "USD";
    public static final int TRIAL_DAYS = 14;
    public static final int GRACE_PERIOD_DAYS = 3;
    public static final int INVOICE_DUE_DAYS = 7;
    public static final int PAYMENT_RETRY_ATTEMPTS = 3;
  }

  /**
   * Metric types for usage tracking.
   */
  public static final class MetricTypes {

    private MetricTypes() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String API_CALLS = "API_CALLS";
    public static final String STORAGE_GB = "STORAGE_GB";
    public static final String ACTIVE_USERS = "ACTIVE_USERS";
    public static final String CUSTOM = "CUSTOM";
  }

  /**
   * Error codes for billing operations.
   */
  public static final class ErrorCodes {

    private ErrorCodes() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String SUBSCRIPTION_NOT_FOUND = "SUBSCRIPTION_NOT_FOUND";
    public static final String SUBSCRIPTION_ALREADY_EXISTS = "SUBSCRIPTION_ALREADY_EXISTS";
    public static final String PLAN_NOT_FOUND = "PLAN_NOT_FOUND";
    public static final String INVALID_PLAN_TRANSITION = "INVALID_PLAN_TRANSITION";
    public static final String QUOTA_EXCEEDED = "QUOTA_EXCEEDED";
    public static final String FEATURE_NOT_AVAILABLE = "FEATURE_NOT_AVAILABLE";
    public static final String PAYMENT_REQUIRED = "PAYMENT_REQUIRED";
    public static final String PAYMENT_FAILED = "PAYMENT_FAILED";
    public static final String INVOICE_NOT_FOUND = "INVOICE_NOT_FOUND";
    public static final String INVALID_PAYMENT_METHOD = "INVALID_PAYMENT_METHOD";
    public static final String USAGE_LIMIT_EXCEEDED = "USAGE_LIMIT_EXCEEDED";
  }
}
```

5. THE Billing Service SHALL NOT use string literals directly in code for:
   - HTTP header names
   - MDC keys
   - Event types
   - Status values
   - Error codes
   - Cache names
   - Provider names
   - Default values

6. THE Billing Service SHALL reference constants using the full path (e.g., `BillingConstants.Headers.X_TENANT_ID`)
7. THE Billing Service SHALL add Javadoc comments to each nested class explaining its purpose
8. THE Billing Service SHALL make all constant fields `public static final`
9. THE Billing Service SHALL use UPPER_SNAKE_CASE naming for all constant fields

### Requirement 3.6: Custom Exception Hierarchy

**User Story:** As a developer, I want domain-specific custom exceptions instead of generic exceptions, so that error handling is precise and meaningful.

#### Acceptance Criteria

1. THE Billing Service SHALL create custom exception classes in the `shared/exception` package
2. THE Billing Service SHALL NOT throw generic exceptions like `RuntimeException`, `IllegalArgumentException`, or `IllegalStateException` for business logic errors
3. THE Billing Service SHALL create the following exception hierarchy:

```java
// Base billing exception
public class BillingException extends RuntimeException {

  public BillingException(final String message) {
    super(message);
  }

  public BillingException(final String message, final Throwable cause) {
    super(message, cause);
  }
}

// Subscription exceptions
public class SubscriptionException extends BillingException {

  public SubscriptionException(final String message) {
    super(message);
  }

  public SubscriptionException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public static class SubscriptionNotFoundException extends SubscriptionException {

    public SubscriptionNotFoundException(final String message) {
      super(message);
    }
  }

  public static class SubscriptionAlreadyExistsException extends SubscriptionException {

    public SubscriptionAlreadyExistsException(final String message) {
      super(message);
    }
  }

  public static class InvalidSubscriptionStateException extends SubscriptionException {

    public InvalidSubscriptionStateException(final String message) {
      super(message);
    }
  }
}

// Payment exceptions
public class PaymentException extends BillingException {

  public PaymentException(final String message) {
    super(message);
  }

  public PaymentException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public static class PaymentFailedException extends PaymentException {

    private final String paymentId;
    private final String reason;

    public PaymentFailedException(final String message, final String paymentId, final String reason) {
      super(message);
      this.paymentId = paymentId;
      this.reason = reason;
    }

    public String getPaymentId() {
      return paymentId;
    }

    public String getReason() {
      return reason;
    }
  }

  public static class PaymentMethodNotFoundException extends PaymentException {

    public PaymentMethodNotFoundException(final String message) {
      super(message);
    }
  }

  public static class InvalidPaymentMethodException extends PaymentException {

    public InvalidPaymentMethodException(final String message) {
      super(message);
    }
  }
}

// Usage and quota exceptions
public class UsageException extends BillingException {

  public UsageException(final String message) {
    super(message);
  }

  public static class QuotaExceededException extends UsageException {

    private final String metricType;
    private final long limit;
    private final long used;

    public QuotaExceededException(final String message, final String metricType, final long limit, final long used) {
      super(message);
      this.metricType = metricType;
      this.limit = limit;
      this.used = used;
    }

    public String getMetricType() {
      return metricType;
    }

    public long getLimit() {
      return limit;
    }

    public long getUsed() {
      return used;
    }
  }

  public static class UsageLimitExceededException extends UsageException {

    public UsageLimitExceededException(final String message) {
      super(message);
    }
  }
}

// Plan exceptions
public class PlanException extends BillingException {

  public PlanException(final String message) {
    super(message);
  }

  public static class PlanNotFoundException extends PlanException {

    public PlanNotFoundException(final String message) {
      super(message);
    }
  }

  public static class InvalidPlanTransitionException extends PlanException {

    private final String fromPlan;
    private final String toPlan;

    public InvalidPlanTransitionException(final String message, final String fromPlan, final String toPlan) {
      super(message);
      this.fromPlan = fromPlan;
      this.toPlan = toPlan;
    }

    public String getFromPlan() {
      return fromPlan;
    }

    public String getToPlan() {
      return toPlan;
    }
  }
}

// Invoice exceptions
public class InvoiceException extends BillingException {

  public InvoiceException(final String message) {
    super(message);
  }

  public static class InvoiceNotFoundException extends InvoiceException {

    public InvoiceNotFoundException(final String message) {
      super(message);
    }
  }

  public static class InvoiceAlreadyPaidException extends InvoiceException {

    public InvoiceAlreadyPaidException(final String message) {
      super(message);
    }
  }
}
```

4. THE Billing Service SHALL use nested static classes for related exception types
5. THE Billing Service SHALL provide both message-only and message-with-cause constructors for base exceptions
6. THE Billing Service SHALL include relevant context fields in exceptions (e.g., paymentId, reason, limits)
7. THE Billing Service SHALL provide getter methods for context fields
8. THE Billing Service SHALL use descriptive exception names ending with "Exception"
9. THE Billing Service SHALL handle custom exceptions in a global exception handler with appropriate HTTP status codes:
   - `NotFoundException` → 404
   - `AlreadyExistsException` → 409
   - `QuotaExceededException` → 429
   - `PaymentFailedException` → 402
   - `InvalidStateException` → 400

10. THE Billing Service SHALL log exceptions with appropriate severity levels:
    - Business exceptions (quota exceeded, not found) → WARN
    - System exceptions (payment provider failure) → ERROR
    - Security exceptions (unauthorized access) → ERROR

### Requirement 3.7: Internationalization (i18n)

**User Story:** As a developer, I want full internationalization support for all user-facing messages, so that the billing service can serve customers in multiple languages.

#### Acceptance Criteria

1. THE Billing Service SHALL enable Spring internationalization support in `application.yml`:

```yaml
spring:
  messages:
    basename: i18n/messages
    encoding: UTF-8
    cache-duration: PT1H
    fallback-to-system-locale: false
    use-code-as-default-message: true
```

2. THE Billing Service SHALL create message property files in `src/main/resources/i18n/`:
   - `messages.properties` (default English)
   - `messages_es.properties` (Spanish)
   - `messages_fr.properties` (French)
   - Additional languages as needed

3. THE Billing Service SHALL create a `MessageService` class in the `shared` package:

```java
package com.iqscaffold.billingservice.shared;

import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

/**
 * Service for retrieving internationalized messages.
 * Provides convenient methods to access i18n messages with locale resolution.
 */
@Service
public class MessageService {

  private final MessageSource messageSource;

  public MessageService(final MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  public String getMessage(final String code) {
    return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
  }

  public String getMessage(final String code, final Object[] args) {
    return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
  }

  public String getMessage(final String code, final Locale locale) {
    return messageSource.getMessage(code, null, locale);
  }

  public String getMessage(final String code, final Object[] args, final Locale locale) {
    return messageSource.getMessage(code, args, locale);
  }
}
```

4. THE Billing Service SHALL organize message keys by category in `messages.properties`:

```properties
# Subscription Messages
subscription.created=Subscription created successfully
subscription.updated=Subscription updated successfully
subscription.upgraded=Subscription upgraded to {0}
subscription.downgraded=Subscription downgraded to {0}
subscription.canceled=Subscription canceled successfully
subscription.reactivated=Subscription reactivated successfully
subscription.not.found=Subscription not found
subscription.already.exists=Active subscription already exists
subscription.trial.started=Trial period started
subscription.trial.ending=Your trial ends in {0} days
subscription.trial.ended=Trial period has ended
subscription.trial.converted=Trial converted to paid subscription

# Plan Messages
plan.created=Subscription plan created successfully
plan.updated=Subscription plan updated successfully
plan.not.found=Subscription plan not found
plan.transition.invalid=Cannot change from {0} to {1}

# Payment Messages
payment.succeeded=Payment of {0} {1} processed successfully
payment.failed=Payment failed: {0}
payment.refunded=Payment refunded successfully
payment.method.added=Payment method added successfully
payment.method.removed=Payment method removed successfully
payment.method.not.found=Payment method not found
payment.method.invalid=Invalid payment method
payment.required=Payment method required

# Invoice Messages
invoice.generated=Invoice {0} generated successfully
invoice.paid=Invoice paid successfully
invoice.voided=Invoice voided
invoice.not.found=Invoice not found
invoice.already.paid=Invoice already paid
invoice.sent=Invoice sent to {0}

# Usage Messages
usage.quota.exceeded={0} quota exceeded. Limit: {1}, Used: {2}
usage.quota.warning={0} usage at {1}% of quota
usage.recorded=Usage recorded successfully
usage.limit.exceeded=Current usage exceeds new plan limits

# Feature Messages
feature.not.available=Feature not available in current plan
feature.upgrade.required=Upgrade to {0} plan to access this feature

# Validation Messages
validation.required={0} is required
validation.invalid={0} is invalid
validation.min.value={0} must be at least {1}
validation.max.value={0} must not exceed {1}
validation.positive={0} must be positive
validation.currency.invalid=Invalid currency code

# Error Messages
error.internal=An internal error occurred
error.bad.request=Bad request
error.not.found=Resource not found
error.conflict=Resource conflict
error.validation=Validation error
error.unauthorized=Unauthorized access
error.forbidden=Forbidden access
error.payment.required=Payment required
error.quota.exceeded=Quota exceeded

# Email Messages - Subscription Created
email.subscription.created.subject=Welcome to {0} Plan
email.subscription.created.greeting=Hello {0}
email.subscription.created.body=Your subscription to {1} plan has been activated successfully.
email.subscription.created.details=Subscription Details
email.subscription.created.plan=Plan: {0}
email.subscription.created.price=Price: {0} {1}/{2}
email.subscription.created.next.billing=Next billing date: {0}
email.subscription.created.button=View Subscription
email.subscription.created.footer=Thank you for choosing our service.

# Email Messages - Trial Ending
email.trial.ending.subject=Your trial ends in {0} days
email.trial.ending.greeting=Hello {0}
email.trial.ending.body=Your {1} plan trial period will end in {2} days.
email.trial.ending.action=Add a payment method to continue enjoying premium features.
email.trial.ending.button=Add Payment Method
email.trial.ending.footer=Questions? Contact our support team.

# Email Messages - Invoice Generated
email.invoice.generated.subject=Invoice {0} - {1} {2}
email.invoice.generated.greeting=Hello {0}
email.invoice.generated.body=Your invoice for {1} is ready.
email.invoice.generated.amount=Amount due: {0} {1}
email.invoice.generated.due.date=Due date: {0}
email.invoice.generated.button=View Invoice
email.invoice.generated.footer=Payment will be automatically processed on the due date.

# Email Messages - Payment Succeeded
email.payment.succeeded.subject=Payment Confirmation - {0} {1}
email.payment.succeeded.greeting=Hello {0}
email.payment.succeeded.body=We have successfully processed your payment.
email.payment.succeeded.amount=Amount paid: {0} {1}
email.payment.succeeded.date=Payment date: {0}
email.payment.succeeded.invoice=Invoice: {0}
email.payment.succeeded.button=View Receipt
email.payment.succeeded.footer=Thank you for your payment.

# Email Messages - Payment Failed
email.payment.failed.subject=Payment Failed - Action Required
email.payment.failed.greeting=Hello {0}
email.payment.failed.body=We were unable to process your payment.
email.payment.failed.reason=Reason: {0}
email.payment.failed.amount=Amount: {0} {1}
email.payment.failed.action=Please update your payment method to avoid service interruption.
email.payment.failed.button=Update Payment Method
email.payment.failed.footer=We will retry the payment in {0} days.

# Email Messages - Subscription Canceled
email.subscription.canceled.subject=Subscription Canceled
email.subscription.canceled.greeting=Hello {0}
email.subscription.canceled.body=Your {1} subscription has been canceled.
email.subscription.canceled.access=You will have access until {0}
email.subscription.canceled.reactivate=Changed your mind? You can reactivate anytime.
email.subscription.canceled.button=Reactivate Subscription
email.subscription.canceled.footer=We're sorry to see you go. Feedback welcome!

# Success Messages
operation.success=Operation completed successfully
```

5. THE Billing Service SHALL use message keys instead of hardcoded strings in:
   - Exception messages
   - Email templates
   - API response messages
   - Validation messages
   - Log messages (where user-facing)

6. THE Billing Service SHALL inject `MessageService` into services that need i18n:

```java
@Service
public class SubscriptionService {

  private final MessageService messageService;

  public SubscriptionService(MessageService messageService) {
    this.messageService = messageService;
  }

  public SubscriptionDto createSubscription(CreateSubscriptionRequest request) {
    // Business logic...

    // Use i18n message
    String message = messageService.getMessage("subscription.created");

    // With parameters
    String upgradeMessage = messageService.getMessage("subscription.upgraded", new Object[] { newPlan.getName() });

    return subscriptionDto;
  }
}
```

7. THE Billing Service SHALL use user's preferred locale from:
   - User preferences (if available)
   - Accept-Language HTTP header
   - Default locale (English)

8. THE Billing Service SHALL pass locale to email templates:

```java
Locale userLocale = user.getPreferredLocale() != null
    ? Locale.forLanguageTag(user.getPreferredLocale())
    : Locale.ENGLISH;

context.setVariable("greeting", messageService.getMessage(
    "email.subscription.created.greeting",
    new Object[]{user.getFirstName()},
    userLocale
));
```

9. THE Billing Service SHALL support parameterized messages using `{0}`, `{1}`, etc. placeholders
10. THE Billing Service SHALL provide translations for at least English (default), Spanish, and French
11. THE Billing Service SHALL use descriptive message keys following the pattern: `{category}.{entity}.{action}` (e.g., `subscription.created`, `payment.failed`)
12. THE Billing Service SHALL NOT concatenate translated strings (use complete message keys instead)
13. THE Billing Service SHALL cache messages for 1 hour to improve performance
14. THE Billing Service SHALL fall back to English if translation is missing for requested locale
15. THE Billing Service SHALL use UTF-8 encoding for all message property files

### Requirement 3.8: Java 21 Modern Features

**User Story:** As a developer, I want to leverage Java 21 modern features for cleaner, more maintainable code, so that the codebase uses current best practices.

#### Acceptance Criteria

1. THE Billing Service SHALL use Java 21 as the target version in `pom.xml`:

```xml
<properties>
    <java.version>21</java.version>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
</properties>
```

2. THE Billing Service SHALL use **Records** for all DTOs, request/response objects, and immutable data carriers:

```java
// ✅ CORRECT - Use records for DTOs
public record SubscriptionDto(Long id, String tenantId, String planCode, String status, LocalDateTime currentPeriodEnd) {}

public record CreateSubscriptionRequest(@NotBlank String planCode, @NotNull Long userId, String paymentMethodId) {}

// ❌ INCORRECT - Don't use classes for simple DTOs
public class SubscriptionDto {

  private Long id;
  private String tenantId;
  // getters, setters, equals, hashCode, toString...
}
```

3. THE Billing Service SHALL use **Text Blocks** (triple quotes `"""`) for multi-line strings:

```java
// ✅ CORRECT - Use text blocks for OpenAPI descriptions
@Operation(
    summary = "Create subscription",
    description = """
        Create a new subscription for a tenant.

        ## Features
        - Automatic trial period activation if configured
        - Payment method validation for paid plans
        - Tenant isolation enforcement

        ## Business Rules
        - Only one active subscription per tenant
        - Free plans activate immediately
        """,
    tags = {"Subscriptions"}
)

// ✅ CORRECT - Use text blocks for JPQL queries
@Query("""
    SELECT s FROM Subscription s
    WHERE s.tenantId = :tenantId
      AND s.status = :status
    ORDER BY s.createdAt DESC
    """)
List<Subscription> findByTenantAndStatus(
    @Param("tenantId") String tenantId,
    @Param("status") String status
);

// ✅ CORRECT - Use text blocks for JSON examples
@Schema(
    example = """
        {
          "planCode": "pro-monthly",
          "userId": 123,
          "paymentMethodId": "pm_1234567890"
        }
        """
)

// ❌ INCORRECT - Don't use string concatenation
@Operation(
    summary = "Create subscription",
    description = "Create a new subscription for a tenant.\n" +
                  "\n" +
                  "## Features\n" +
                  "- Automatic trial period activation\n"
)
```

4. THE Billing Service SHALL use **Pattern Matching for instanceof** instead of traditional casting:

```java
// ✅ CORRECT - Pattern matching with instanceof
if (authentication instanceof JwtAuthenticationToken token) {
    String userId = token.getToken().getSubject();
    String tenantId = token.getToken().getClaimAsString("tenantId");
    // Use token directly
}

// ✅ CORRECT - Pattern matching in methods
private String extractString(Object value) {
    return value instanceof String s ? s : null;
}

private Long extractLong(Object value) {
    return value instanceof Number n ? n.longValue() : null;
}

// ❌ INCORRECT - Traditional casting
if (authentication instanceof JwtAuthenticationToken) {
    JwtAuthenticationToken token = (JwtAuthenticationToken) authentication;
    String userId = token.getToken().getSubject();
}
```

5. THE Billing Service SHALL use **Switch Expressions** with arrow syntax for cleaner switch statements:

```java
// ✅ CORRECT - Switch expression with arrow syntax
String statusMessage = switch (subscription.getStatus()) {
    case TRIAL -> "Trial period active";
    case ACTIVE -> "Subscription active";
    case PAST_DUE -> "Payment overdue";
    case CANCELED -> "Subscription canceled";
    case EXPIRED -> "Subscription expired";
    default -> "Unknown status";
};

// ✅ CORRECT - Switch expression with yield for complex logic
int retryDelay = switch (attemptNumber) {
    case 1 -> 3;
    case 2 -> 5;
    case 3 -> 7;
    default -> {
        logger.warn("Unexpected attempt number: {}", attemptNumber);
        yield 10;
    }
};

// ✅ CORRECT - Switch expression for HTTP status mapping
HttpStatus status = switch (exception) {
    case SubscriptionNotFoundException e -> HttpStatus.NOT_FOUND;
    case SubscriptionAlreadyExistsException e -> HttpStatus.CONFLICT;
    case QuotaExceededException e -> HttpStatus.TOO_MANY_REQUESTS;
    case PaymentFailedException e -> HttpStatus.PAYMENT_REQUIRED;
    default -> HttpStatus.INTERNAL_SERVER_ERROR;
};

// ❌ INCORRECT - Traditional switch with breaks
String statusMessage;
switch (subscription.getStatus()) {
    case TRIAL:
        statusMessage = "Trial period active";
        break;
    case ACTIVE:
        statusMessage = "Subscription active";
        break;
    default:
        statusMessage = "Unknown status";
}
```

6. THE Billing Service SHALL use **Local Variable Type Inference (var)** for improved readability when type is obvious:

```java
// ✅ CORRECT - Use var when type is obvious from right side
var subscription = subscriptionRepository.findById(id);
var plans = planRepository.findAll();
var message = messageService.getMessage("subscription.created");
var invoice = new Invoice(subscription, period);
var context = new Context();
var userLocale = Locale.forLanguageTag(user.getPreferredLocale());

// ✅ CORRECT - Use var in loops
for (var subscription : subscriptions) {
    processSubscription(subscription);
}

// ✅ CORRECT - Use var with stream operations
var activeSubscriptions = subscriptions.stream()
    .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
    .toList();

// ❌ INCORRECT - Don't use var when type is not obvious
var result = process(); // What type is result?
var data = getData(); // What type is data?

// ✅ CORRECT - Use explicit type when not obvious
SubscriptionDto result = process();
List<InvoiceDto> data = getData();

// ❌ INCORRECT - Don't use var for primitives or simple assignments
var count = 0; // Use int count = 0;
var flag = true; // Use boolean flag = true;
```

7. THE Billing Service SHALL use **Enhanced NullPointerException messages** (automatic in Java 21) for better debugging
8. THE Billing Service SHALL use **Sealed Classes** for restricted type hierarchies where appropriate:

```java
// ✅ CORRECT - Sealed class for payment provider types
public sealed interface PaymentProvider permits StripePaymentProvider, PayPalPaymentProvider, ManualPaymentProvider {
  PaymentResult processPayment(PaymentRequest request);
}

public final class StripePaymentProvider implements PaymentProvider {

  @Override
  public PaymentResult processPayment(PaymentRequest request) {
    // Stripe implementation
  }
}

public final class PayPalPaymentProvider implements PaymentProvider {

  @Override
  public PaymentResult processPayment(PaymentRequest request) {
    // PayPal implementation
  }
}

public final class ManualPaymentProvider implements PaymentProvider {

  @Override
  public PaymentResult processPayment(PaymentRequest request) {
    // Manual implementation
  }
}
```

9. THE Billing Service SHALL use **Stream API enhancements** including `toList()` instead of `collect(Collectors.toList())`:

```java
// ✅ CORRECT - Use toList() (Java 16+)
var activePlans = plans.stream().filter(Plan::isActive).toList();

// ❌ INCORRECT - Old style
var activePlans = plans.stream().filter(Plan::isActive).collect(Collectors.toList());
```

10. THE Billing Service SHALL use **Record Patterns** (Java 21) for destructuring:

```java
// ✅ CORRECT - Record pattern matching
if (result instanceof SuccessResult(var data, var message)) {
    logger.info("Success: {} - {}", message, data);
}

// ✅ CORRECT - Switch with record patterns
String description = switch (event) {
    case SubscriptionCreated(var id, var plan) ->
        "Subscription " + id + " created for plan " + plan;
    case PaymentProcessed(var amount, var currency) ->
        "Payment of " + amount + " " + currency + " processed";
    default -> "Unknown event";
};
```

11. THE Billing Service SHALL NOT use deprecated features or old-style Java patterns
12. THE Billing Service SHALL configure compiler to show warnings for outdated patterns:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <source>21</source>
        <target>21</target>
        <compilerArgs>
            <arg>-Xlint:all</arg>
            <arg>-Xlint:-processing</arg>
            <arg>--enable-preview</arg>
        </compilerArgs>
    </configuration>
</plugin>
```

13. THE Billing Service SHALL use modern Java features consistently across the entire codebase
14. THE Billing Service SHALL document any preview features used with clear comments
15. THE Billing Service SHALL prefer immutability using records over mutable classes where possible

---

### Requirement 4

**User Story:** As a platform administrator, I want to create and configure subscription plans with flexible features and quotas, so that I can offer different service tiers to customers.

#### Acceptance Criteria

1. THE Billing Service SHALL allow administrators to create subscription plans with name, code, tier, billing cycle, price, and currency
2. THE Billing Service SHALL enforce unique plan codes across all subscription plans
3. THE Billing Service SHALL support plan tiers FREE, PRO, and ENTERPRISE
4. THE Billing Service SHALL support billing cycles MONTHLY, YEARLY, and LIFETIME
5. THE Billing Service SHALL store plan features as a JSON structure
6. THE Billing Service SHALL store plan quotas including maxUsers, storageGb, apiCallsPerMonth, and custom metrics as JSON
7. THE Billing Service SHALL allow administrators to configure trial period duration in days for each plan
8. THE Billing Service SHALL allow administrators to set plan visibility as public or private

### Requirement 5

**User Story:** As a platform administrator, I want to update existing subscription plans while protecting current subscribers, so that I can evolve offerings without disrupting existing customers.

#### Acceptance Criteria

1. THE Billing Service SHALL allow administrators to update plan name, description, features, and quotas
2. THE Billing Service SHALL apply price changes only to new subscriptions while maintaining original pricing for existing subscriptions
3. IF a plan has active subscriptions THEN THE Billing Service SHALL prevent plan deletion
4. THE Billing Service SHALL allow administrators to archive or deactivate plans to prevent new subscriptions

### Requirement 6

**User Story:** As a potential customer, I want to view available subscription plans with their features and pricing, so that I can choose the right plan for my needs.

#### Acceptance Criteria

1. THE Billing Service SHALL provide a public endpoint that lists all active public subscription plans
2. THE Billing Service SHALL include features, quotas, and pricing in plan listing responses
3. THE Billing Service SHALL sort plans by tier and price in listing responses
4. WHERE billing cycle filtering is requested, THE Billing Service SHALL filter plans by the specified billing cycle
5. THE Billing Service SHALL provide an admin endpoint that lists all plans including private and archived plans

---

## Detailed Technical Requirements

The following sections provide comprehensive technical requirements using EARS notation. These requirements complement the user stories above and provide the detailed specifications needed for implementation.

### EARS Pattern Reference

- **[U] Ubiquitous**: THE system SHALL [requirement] - Always active requirements
- **[E] Event-driven**: WHEN [trigger] THE system SHALL [requirement] - Requirements triggered by events
- **[S] State-driven**: WHILE [state] THE system SHALL [requirement] - Requirements active in specific states
- **[O] Optional**: WHERE [feature] THE system SHALL [requirement] - Requirements for optional features
- **[UW] Unwanted**: IF [condition] THEN THE system SHALL [requirement] - Requirements handling error conditions

---

## 3. Subscription Lifecycle Management

### 3.1 Subscription Creation

**REQ-SUB-001** [E]  
WHEN a user subscribes to a plan, the system shall create a subscription record linked to the tenant.

**REQ-SUB-002** [E]  
WHEN creating a subscription for a plan with trial period, the system shall set subscription status to TRIAL.

**REQ-SUB-003** [E]  
WHEN creating a subscription for a plan without trial period, the system shall set subscription status to ACTIVE and require payment.

**REQ-SUB-004** [E]  
WHEN creating a subscription for a FREE plan, the system shall activate immediately without requiring payment.

**REQ-SUB-005** [UW]  
IF a tenant already has an active subscription THEN the system shall reject new subscription creation with error code SUBSCRIPTION_ALREADY_EXISTS.

**REQ-SUB-006** [E]  
WHEN a subscription is created, the system shall send a confirmation email to the user.

**REQ-SUB-007** [E]  
WHEN a subscription is created, the system shall update the tenant record in the User Service with subscription details.

### 3.2 Trial Period Management

**REQ-SUB-008** [E]  
WHEN a trial period starts, the system shall grant full access to plan features.

**REQ-SUB-009** [E]  
WHEN 7 days remain in trial period, the system shall send a trial ending reminder email.

**REQ-SUB-010** [E]  
WHEN 3 days remain in trial period, the system shall send a trial ending reminder email.

**REQ-SUB-011** [E]  
WHEN 1 day remains in trial period, the system shall send a trial ending reminder email.

**REQ-SUB-012** [E]  
WHEN trial period ends and payment method is on file, the system shall convert subscription to ACTIVE status and charge the first invoice.

**REQ-SUB-013** [E]  
WHEN trial period ends and no payment method is on file, the system shall convert subscription to EXPIRED status and restrict access.

**REQ-SUB-014** [E]  
WHEN a user cancels during trial period, the system shall cancel subscription without charge.

**REQ-SUB-015** [U]  
The system shall allow administrators to manually extend trial periods.

### 3.3 Subscription Status Management

**REQ-SUB-016** [U]  
The system shall support subscription statuses: TRIAL, ACTIVE, PAST_DUE, CANCELED, EXPIRED, SUSPENDED, and INCOMPLETE.

**REQ-SUB-017** [S]  
WHILE subscription status is TRIAL, the system shall provide full feature access without charges.

**REQ-SUB-018** [S]  
WHILE subscription status is ACTIVE, the system shall provide full feature access and process regular billing.

**REQ-SUB-019** [S]  
WHILE subscription status is PAST_DUE, the system shall provide limited access and attempt payment retries.

**REQ-SUB-020** [S]  
WHILE subscription status is CANCELED, the system shall provide access until period end.

**REQ-SUB-021** [S]  
WHILE subscription status is EXPIRED, the system shall deny access to paid features.

**REQ-SUB-022** [S]  
WHILE subscription status is SUSPENDED, the system shall deny all access.

### 3.4 Subscription Upgrade

**REQ-SUB-023** [E]  
WHEN a user upgrades to a higher-tier plan, the system shall calculate prorated credit for unused time on current plan.

**REQ-SUB-024** [E]  
WHEN a user upgrades to a higher-tier plan, the system shall calculate prorated charge for new plan.

**REQ-SUB-025** [E]  
WHEN a user upgrades, the system shall generate a proration invoice immediately.

**REQ-SUB-026** [E]  
WHEN a user upgrades, the system shall update subscription to new plan immediately.

**REQ-SUB-027** [E]  
WHEN a user upgrades, the system shall start new billing cycle immediately.

**REQ-SUB-028** [E]  
WHEN a user upgrades, the system shall update features and quotas immediately.

**REQ-SUB-029** [E]  
WHEN a user upgrades, the system shall send confirmation email with proration details.

**REQ-SUB-030** [U]  
The system shall calculate proration using formula: `(Plan Price / Days in Period) × Days Remaining`.

### 3.5 Subscription Downgrade

**REQ-SUB-031** [E]  
WHEN a user downgrades to a lower-tier plan, the system shall schedule downgrade for end of current billing period by default.

**REQ-SUB-032** [O]  
WHERE immediate downgrade is requested, the system shall apply downgrade immediately with prorated refund.

**REQ-SUB-033** [E]  
WHEN a user downgrades, the system shall validate current usage against new plan quotas.

**REQ-SUB-034** [UW]  
IF current usage exceeds new plan quotas THEN the system shall reject downgrade with error code USAGE_LIMIT_EXCEEDED.

**REQ-SUB-035** [E]  
WHEN downgrade is scheduled, the system shall send confirmation email with effective date.

**REQ-SUB-036** [E]  
WHEN scheduled downgrade effective date arrives, the system shall update subscription to new plan.

**REQ-SUB-037** [E]  
WHEN scheduled downgrade effective date arrives, the system shall start new billing cycle with new pricing.

**REQ-SUB-038** [E]  
WHEN scheduled downgrade effective date arrives, the system shall restrict features according to new plan.

### 3.6 Subscription Cancellation

**REQ-SUB-039** [E]  
WHEN a user cancels subscription, the system shall present cancellation options: cancel at period end or cancel immediately.

**REQ-SUB-040** [E]  
WHEN user selects cancel at period end, the system shall set cancelAtPeriodEnd flag to true and maintain access until period end.

**REQ-SUB-041** [E]  
WHEN user selects cancel immediately, the system shall revoke access immediately and issue prorated refund.

**REQ-SUB-042** [E]  
WHEN a user cancels subscription, the system shall update subscription status to CANCELED.

**REQ-SUB-043** [E]  
WHEN a user cancels subscription, the system shall send cancellation confirmation email.

**REQ-SUB-044** [E]  
WHEN cancellation period end arrives, the system shall change subscription status to EXPIRED.

**REQ-SUB-045** [O]  
WHERE cancellation reason is provided, the system shall capture and store the reason for analytics.

### 3.7 Subscription Reactivation

**REQ-SUB-046** [S]  
WHILE subscription status is CANCELED and period has not ended, the system shall allow reactivation.

**REQ-SUB-047** [E]  
WHEN a user reactivates canceled subscription, the system shall remove cancelAtPeriodEnd flag.

**REQ-SUB-048** [E]  
WHEN a user reactivates canceled subscription, the system shall maintain ACTIVE status.

**REQ-SUB-049** [E]  
WHEN a user reactivates canceled subscription, the system shall continue billing as scheduled.

**REQ-SUB-050** [E]  
WHEN a user reactivates canceled subscription, the system shall send reactivation confirmation email.

---

## 4. Usage-Based Billing & Metering

### 4.1 Usage Tracking

**REQ-USAGE-001** [U]  
The system shall track usage metrics including API calls, storage, active users, and custom metrics.

**REQ-USAGE-002** [E]  
WHEN usage is recorded, the system shall include tenant ID, metric type, quantity, and timestamp.

**REQ-USAGE-003** [U]  
The system shall aggregate usage by billing period.

**REQ-USAGE-004** [U]  
The system shall retain usage data for historical analysis and compliance.

**REQ-USAGE-005** [U]  
The system shall implement idempotent usage recording to prevent double-counting.

**REQ-USAGE-006** [U]  
The system shall complete usage recording within 50ms to minimize latency impact.

### 4.2 Usage Metering Integration

**REQ-USAGE-007** [E]  
WHEN Gateway Service processes an API request, the system shall record one API call usage asynchronously.

**REQ-USAGE-008** [E]  
WHEN User Service creates or deletes a user, the system shall update active user count.

**REQ-USAGE-009** [U]  
The system shall take daily snapshots of active user counts.

**REQ-USAGE-010** [O]  
WHERE storage service exists, WHEN files are uploaded or deleted, the system shall update storage usage.

### 4.3 Quota Enforcement

**REQ-USAGE-011** [E]  
WHEN an operation is requested, the system shall check current usage against subscription quota before allowing operation.

**REQ-USAGE-012** [UW]  
IF quota is exceeded THEN the system shall reject operation with error code QUOTA_EXCEEDED and HTTP status 429.

**REQ-USAGE-013** [U]  
The system shall include quota details in error response: limit, used, percentage, and reset date.

**REQ-USAGE-014** [U]  
The system shall enforce quotas at Gateway Service for API calls, User Service for user creation, and Storage Service for file uploads.

**REQ-USAGE-015** [U]  
The system shall allow 5% grace period for soft quota limits (105% of quota).

**REQ-USAGE-016** [U]  
The system shall allow administrators to temporarily override quotas.

### 4.4 Usage Reporting

**REQ-USAGE-017** [U]  
The system shall display current usage for all tracked metrics in the billing portal.

**REQ-USAGE-018** [U]  
The system shall display usage as current value, quota limit, and percentage used.

**REQ-USAGE-019** [U]  
The system shall provide historical usage charts for the last 6 months.

**REQ-USAGE-020** [U]  
The system shall allow users to export usage data as CSV.

**REQ-USAGE-021** [E]  
WHEN usage reaches 80% of quota, the system shall send usage alert email.

**REQ-USAGE-022** [E]  
WHEN usage reaches 90% of quota, the system shall send usage alert email.

**REQ-USAGE-023** [E]  
WHEN usage reaches 100% of quota, the system shall send usage alert email.

---

## 5. Payment Processing

### 5.1 Payment Provider Abstraction

**REQ-PAY-001** [U]  
The system shall implement a PaymentProviderAdapter interface for payment provider abstraction.

**REQ-PAY-002** [U]  
The system shall support Stripe as the primary payment provider.

**REQ-PAY-003** [U]  
The system shall support PayPal as a secondary payment provider.

**REQ-PAY-004** [U]  
The system shall support manual/custom payment processing for enterprise contracts.

**REQ-PAY-005** [U]  
The system shall select payment provider implementation via configuration.

**REQ-PAY-006** [UW]  
IF payment provider is unavailable THEN the system shall fallback to manual processing mode.

### 5.2 Payment Method Management

**REQ-PAY-007** [U]  
The system shall allow users to add credit/debit cards via secure tokenization.

**REQ-PAY-008** [O]  
WHERE user is in US, the system shall allow users to add bank accounts (ACH).

**REQ-PAY-009** [U]  
The system shall allow users to add PayPal accounts.

**REQ-PAY-010** [U]  
The system shall support multiple payment methods per tenant.

**REQ-PAY-011** [U]  
The system shall require one payment method to be marked as default.

**REQ-PAY-012** [U]  
The system shall allow users to change default payment method.

**REQ-PAY-013** [UW]  
IF payment method is default and subscription is active THEN the system shall prevent payment method removal.

**REQ-PAY-014** [U]  
The system shall never store full credit card numbers.

**REQ-PAY-015** [U]  
The system shall use payment provider tokenization for secure card storage.

**REQ-PAY-016** [U]  
The system shall display only last 4 digits and brand to users.

**REQ-PAY-017** [U]  
The system shall track payment method expiration dates.

**REQ-PAY-018** [E]  
WHEN payment method is expiring within 30 days, the system shall send renewal reminder email.

**REQ-PAY-019** [U]  
The system shall maintain audit log for all payment method changes.

### 5.3 Payment Processing

**REQ-PAY-020** [E]  
WHEN an invoice is generated, the system shall attempt automatic payment using default payment method.

**REQ-PAY-021** [U]  
The system shall allow users to make manual payments for invoices.

**REQ-PAY-022** [E]  
WHEN payment succeeds, the system shall send payment confirmation email.

**REQ-PAY-023** [E]  
WHEN payment fails, the system shall send payment failure email with retry information.

**REQ-PAY-024** [U]  
The system shall track payment status: pending, succeeded, failed, refunded.

**REQ-PAY-025** [U]  
The system shall implement idempotent payment processing to prevent duplicate charges.

**REQ-PAY-026** [E]  
WHEN payment fails immediately, the system shall retry after 3 days.

**REQ-PAY-027** [E]  
WHEN second payment attempt fails, the system shall retry after 5 days.

**REQ-PAY-028** [E]  
WHEN third payment attempt fails, the system shall retry after 7 days.

**REQ-PAY-029** [E]  
WHEN all 3 payment attempts fail, the system shall mark subscription as PAST_DUE.

### 5.4 Refund Processing

**REQ-PAY-030** [U]  
The system shall allow administrators to issue full or partial refunds.

**REQ-PAY-031** [U]  
The system shall require refund reason for all refunds.

**REQ-PAY-032** [E]  
WHEN refund is issued, the system shall process refund through payment provider.

**REQ-PAY-033** [E]  
WHEN refund is issued, the system shall update invoice to reflect refund.

**REQ-PAY-034** [E]  
WHEN refund is issued, the system shall notify customer via email.

**REQ-PAY-035** [E]  
WHEN refund is issued, the system shall display refund in billing portal.

**REQ-PAY-036** [U]  
The system shall maintain audit log for all refunds.

### 5.5 Webhook Handling

**REQ-PAY-037** [U]  
The system shall provide webhook endpoints for each payment provider.

**REQ-PAY-038** [E]  
WHEN webhook is received, the system shall verify signature for security.

**REQ-PAY-039** [U]  
The system shall implement idempotent webhook processing.

**REQ-PAY-040** [E]  
WHEN payment_intent.succeeded event is received, the system shall update payment and invoice status.

**REQ-PAY-041** [E]  
WHEN payment_intent.failed event is received, the system shall update payment status and trigger retry logic.

**REQ-PAY-042** [E]  
WHEN customer.subscription.updated event is received, the system shall synchronize subscription data.

**REQ-PAY-043** [E]  
WHEN customer.subscription.deleted event is received, the system shall update subscription status.

**REQ-PAY-044** [E]  
WHEN invoice.payment_succeeded event is received, the system shall mark invoice as paid.

**REQ-PAY-045** [E]  
WHEN invoice.payment_failed event is received, the system shall trigger payment retry logic.

**REQ-PAY-046** [U]  
The system shall process webhook events asynchronously.

**REQ-PAY-047** [UW]  
IF webhook processing fails THEN the system shall retry processing with exponential backoff.

**REQ-PAY-048** [U]  
The system shall maintain webhook event log for debugging and audit.

---

## 6. Invoice Management

### 6.1 Invoice Generation

**REQ-INV-001** [E]  
WHEN billing period starts, the system shall automatically generate invoice for subscription.

**REQ-INV-002** [U]  
The system shall assign unique invoice number using format `INV-{YEAR}{MONTH}-{SEQUENCE}`.

**REQ-INV-003** [U]  
The system shall include billing period start and end dates in invoice.

**REQ-INV-004** [U]  
The system shall include line items for subscription fee, usage charges, credits, and taxes in invoice.

**REQ-INV-005** [U]  
The system shall calculate subtotal, tax, and total for invoice.

**REQ-INV-006** [U]  
The system shall include payment due date in invoice (typically 7 days from generation).

**REQ-INV-007** [U]  
The system shall support invoice statuses: draft, open, paid, void, uncollectible.

**REQ-INV-008** [E]  
WHEN invoice is finalized, the system shall generate PDF for download.

**REQ-INV-009** [E]  
WHEN invoice is finalized, the system shall send invoice via email.

**REQ-INV-010** [U]  
The system shall complete invoice generation within 2 seconds.

### 6.2 Invoice Line Items

**REQ-INV-011** [U]  
The system shall support line item types: subscription fee, usage charges, proration credit, proration charge, discount, and tax.

**REQ-INV-012** [U]  
The system shall include description, quantity, unit price, and amount for each line item.

**REQ-INV-013** [E]  
WHEN subscription is upgraded, the system shall add proration credit and proration charge line items.

**REQ-INV-014** [E]  
WHEN usage exceeds quota, the system shall add usage overage line items.

**REQ-INV-015** [O]  
WHERE promotional discount applies, the system shall add discount line items.

### 6.3 Invoice Payment

**REQ-INV-016** [E]  
WHEN invoice is finalized, the system shall attempt automatic payment.

**REQ-INV-017** [U]  
The system shall allow manual payment through billing portal.

**REQ-INV-018** [E]  
WHEN payment succeeds, the system shall update invoice status to paid.

**REQ-INV-019** [E]  
WHEN payment succeeds, the system shall record payment date.

**REQ-INV-020** [E]  
WHEN payment succeeds, the system shall send receipt email.

**REQ-INV-021** [E]  
WHEN payment fails, the system shall update invoice status to open with failure reason.

### 6.4 Invoice History

**REQ-INV-022** [U]  
The system shall display all invoices in billing portal with pagination.

**REQ-INV-023** [U]  
The system shall sort invoices by date with newest first.

**REQ-INV-024** [U]  
The system shall allow filtering invoices by status and date range.

**REQ-INV-025** [U]  
The system shall display invoice number, date, amount, and status for each invoice.

**REQ-INV-026** [U]  
The system shall allow users to view invoice details.

**REQ-INV-027** [U]  
The system shall allow users to download invoice as PDF.

**REQ-INV-028** [U]  
The system shall allow users to email invoice to specified address.

**REQ-INV-029** [U]  
The system shall retain invoices indefinitely for compliance.

### 6.5 Invoice Voiding

**REQ-INV-030** [U]  
The system shall allow administrators to void unpaid invoices.

**REQ-INV-031** [U]  
The system shall require void reason for all voided invoices.

**REQ-INV-032** [E]  
WHEN invoice is voided, the system shall prevent payment of voided invoice.

**REQ-INV-033** [E]  
WHEN invoice is voided, the system shall mark invoice as void in history.

**REQ-INV-034** [E]  
WHEN invoice is voided, the system shall notify customer via email.

---

## 7. Customer Billing Portal

### 7.1 Billing Dashboard

**REQ-PORTAL-001** [U]  
The system shall provide billing dashboard displaying current subscription details.

**REQ-PORTAL-002** [U]  
The system shall display subscription plan, status, and renewal date on dashboard.

**REQ-PORTAL-003** [U]  
The system shall display current usage metrics with quota indicators on dashboard.

**REQ-PORTAL-004** [U]  
The system shall display upcoming invoice preview on dashboard.

**REQ-PORTAL-005** [U]  
The system shall display payment method on file on dashboard.

**REQ-PORTAL-006** [U]  
The system shall provide quick actions for upgrade, add payment method, and view invoices on dashboard.

### 7.2 Subscription Management

**REQ-PORTAL-007** [U]  
The system shall allow users to view current plan details in portal.

**REQ-PORTAL-008** [U]  
The system shall allow users to compare available plans in portal.

**REQ-PORTAL-009** [U]  
The system shall allow users to upgrade to higher tier immediately in portal.

**REQ-PORTAL-010** [U]  
The system shall allow users to downgrade to lower tier (scheduled) in portal.

**REQ-PORTAL-011** [U]  
The system shall allow users to cancel subscription in portal.

**REQ-PORTAL-012** [U]  
The system shall allow users to reactivate canceled subscription in portal.

**REQ-PORTAL-013** [U]  
The system shall allow users to view subscription history in portal.

**REQ-PORTAL-014** [U]  
The system shall restrict subscription modifications to tenant owner or billing admin roles.

**REQ-PORTAL-015** [U]  
The system shall allow regular users to view subscription details in read-only mode.

### 7.3 Payment Method Management

**REQ-PORTAL-016** [U]  
The system shall allow users to view all payment methods in portal.

**REQ-PORTAL-017** [U]  
The system shall allow users to add new payment methods (card, bank account, PayPal) in portal.

**REQ-PORTAL-018** [U]  
The system shall allow users to set default payment method in portal.

**REQ-PORTAL-019** [U]  
The system shall allow users to remove payment methods in portal.

**REQ-PORTAL-020** [U]  
The system shall allow users to update billing address in portal.

**REQ-PORTAL-021** [U]  
The system shall use payment provider iframe or modal for secure card entry.

**REQ-PORTAL-022** [U]  
The system shall require authentication for sensitive payment operations.

### 7.4 Invoice Access

**REQ-PORTAL-023** [U]  
The system shall allow users to view invoice list in portal.

**REQ-PORTAL-024** [U]  
The system shall allow users to filter and search invoices in portal.

**REQ-PORTAL-025** [U]  
The system shall allow users to view invoice details in portal.

**REQ-PORTAL-026** [U]  
The system shall allow users to download invoice PDF in portal.

**REQ-PORTAL-027** [U]  
The system shall allow users to email invoice to accountant or finance team in portal.

**REQ-PORTAL-028** [U]  
The system shall allow users to print invoice in portal.

### 7.5 Usage Monitoring

**REQ-PORTAL-029** [U]  
The system shall display current usage for each metric in portal.

**REQ-PORTAL-030** [U]  
The system shall display quota limits in portal.

**REQ-PORTAL-031** [U]  
The system shall display percentage used with visual indicators in portal.

**REQ-PORTAL-032** [U]  
The system shall display usage trends with charts in portal.

**REQ-PORTAL-033** [U]  
The system shall display projected usage for current period in portal.

**REQ-PORTAL-034** [U]  
The system shall display alerts for approaching quota limits in portal.

---

## 8. Administrative Functions

### 8.1 Subscription Administration

**REQ-ADMIN-001** [U]  
The system shall allow administrators to view all subscriptions across tenants.

**REQ-ADMIN-002** [U]  
The system shall allow administrators to filter subscriptions by status, plan, and tenant.

**REQ-ADMIN-003** [U]  
The system shall allow administrators to view subscription details.

**REQ-ADMIN-004** [U]  
The system shall allow administrators to manually create subscription for tenant.

**REQ-ADMIN-005** [U]  
The system shall allow administrators to force cancel subscription.

**REQ-ADMIN-006** [U]  
The system shall allow administrators to extend trial period.

**REQ-ADMIN-007** [U]  
The system shall allow administrators to apply credits to account.

**REQ-ADMIN-008** [U]  
The system shall allow administrators to temporarily override quotas.

**REQ-ADMIN-009** [U]  
The system shall allow administrators to view subscription audit log.

### 8.2 Billing Analytics

**REQ-ADMIN-010** [U]  
The system shall calculate and display Monthly Recurring Revenue (MRR).

**REQ-ADMIN-011** [U]  
The system shall calculate and display Annual Recurring Revenue (ARR).

**REQ-ADMIN-012** [U]  
The system shall display revenue breakdown by plan.

**REQ-ADMIN-013** [U]  
The system shall display subscription count by status.

**REQ-ADMIN-014** [U]  
The system shall calculate and display churn rate.

**REQ-ADMIN-015** [U]  
The system shall calculate and display Customer Lifetime Value (CLV).

**REQ-ADMIN-016** [U]  
The system shall calculate and display failed payment rate.

**REQ-ADMIN-017** [U]  
The system shall calculate and display trial conversion rate.

**REQ-ADMIN-018** [U]  
The system shall allow administrators to export reports as CSV or Excel.

**REQ-ADMIN-019** [U]  
The system shall allow administrators to schedule automated reports.

**REQ-ADMIN-020** [U]  
The system shall provide dashboard with key billing metrics.

### 8.3 Manual Billing Operations

**REQ-ADMIN-021** [U]  
The system shall allow administrators to generate invoice manually.

**REQ-ADMIN-022** [U]  
The system shall allow administrators to apply discount or credit to invoice.

**REQ-ADMIN-023** [U]  
The system shall allow administrators to issue refund.

**REQ-ADMIN-024** [U]  
The system shall allow administrators to void invoice.

**REQ-ADMIN-025** [U]  
The system shall allow administrators to adjust subscription price (custom pricing).

**REQ-ADMIN-026** [U]  
The system shall allow administrators to grant free subscription.

**REQ-ADMIN-027** [U]  
The system shall allow administrators to extend subscription period.

---

## 9. Non-Functional Requirements

### 9.1 Performance

**REQ-PERF-001** [U]  
The system shall respond to API requests within 200ms at 95th percentile.

**REQ-PERF-002** [U]  
The system shall complete usage recording within 50ms.

**REQ-PERF-003** [U]  
The system shall complete invoice generation within 2 seconds.

**REQ-PERF-004** [U]  
The system shall complete payment processing within 5 seconds excluding payment provider latency.

**REQ-PERF-005** [U]  
The system shall support 1000 concurrent users.

**REQ-PERF-006** [U]  
The system shall handle 10,000 usage records per second.

**REQ-PERF-007** [U]  
The system shall process 100 payments per minute.

**REQ-PERF-008** [U]  
The system shall generate 1000 invoices per hour.

**REQ-PERF-009** [U]  
The system shall support horizontal scaling for service instances.

**REQ-PERF-010** [U]  
The system shall implement database connection pooling.

**REQ-PERF-011** [U]  
The system shall cache frequently accessed data in Redis.

**REQ-PERF-012** [U]  
The system shall process non-critical operations asynchronously.

### 9.2 Reliability

**REQ-REL-001** [U]  
The system shall maintain 99.9% uptime.

**REQ-REL-002** [UW]  
IF payment provider is unavailable THEN the system shall degrade gracefully.

**REQ-REL-003** [U]  
The system shall implement retry mechanisms for transient failures.

**REQ-REL-004** [U]  
The system shall implement circuit breakers for external dependencies.

**REQ-REL-005** [U]  
The system shall use ACID transactions for financial operations.

**REQ-REL-006** [U]  
The system shall implement idempotent operations to prevent duplicate charges.

**REQ-REL-007** [U]  
The system shall maintain audit trail for all billing operations.

**REQ-REL-008** [U]  
The system shall validate data at all layers (controller, service, repository).

**REQ-REL-009** [U]  
The system shall implement reconciliation processes for payment provider synchronization.

**REQ-REL-010** [U]  
The system shall automatically retry failed payments according to retry schedule.

**REQ-REL-011** [UW]  
IF automatic payment fails THEN the system shall fallback to manual processing.

**REQ-REL-012** [U]  
The system shall use dead letter queue for failed webhook processing.

**REQ-REL-013** [U]  
The system shall implement health checks for monitoring.

### 9.3 Security

**REQ-SEC-001** [U]  
The system shall comply with PCI DSS through payment provider.

**REQ-SEC-002** [U]  
The system shall never store full credit card numbers.

**REQ-SEC-003** [U]  
The system shall encrypt sensitive data at rest.

**REQ-SEC-004** [U]  
The system shall encrypt all data in transit using TLS 1.3.

**REQ-SEC-005** [U]  
The system shall store secure tokens in Redis with encryption.

**REQ-SEC-006** [U]  
The system shall implement role-based access control (RBAC).

**REQ-SEC-007** [U]  
The system shall enforce tenant isolation for multi-tenancy.

**REQ-SEC-008** [U]  
The system shall authenticate API requests via JWT.

**REQ-SEC-009** [E]  
WHEN webhook is received, the system shall verify signature before processing.

**REQ-SEC-010** [U]  
The system shall implement rate limiting on public endpoints.

**REQ-SEC-011** [U]  
The system shall maintain comprehensive audit logging.

**REQ-SEC-012** [U]  
The system shall create immutable audit records.

**REQ-SEC-013** [U]  
The system shall comply with GDPR and CCPA regulations.

**REQ-SEC-014** [U]  
The system shall implement data retention policies.

**REQ-SEC-015** [U]  
The system shall support right to data export.

**REQ-SEC-016** [U]  
The system shall support right to data deletion.

### 9.4 Maintainability

**REQ-MAINT-001** [U]  
The system shall follow clean architecture principles.

**REQ-MAINT-002** [U]  
The system shall implement domain-driven design patterns.

**REQ-MAINT-003** [U]  
The system shall follow SOLID principles.

**REQ-MAINT-004** [U]  
The system shall maintain 80% minimum unit test coverage.

**REQ-MAINT-005** [U]  
The system shall include integration tests for critical flows.

**REQ-MAINT-006** [U]  
The system shall include architecture tests using ArchUnit.

**REQ-MAINT-007** [U]  
The system shall provide OpenAPI/Swagger documentation.

**REQ-MAINT-008** [U]  
The system shall include code comments for complex logic.

**REQ-MAINT-009** [U]  
The system shall include README for service setup.

**REQ-MAINT-010** [U]  
The system shall include runbooks for operations.

**REQ-MAINT-011** [U]  
The system shall document architecture decisions in ADRs.

**REQ-MAINT-012** [U]  
The system shall implement structured JSON logging.

**REQ-MAINT-013** [U]  
The system shall implement distributed tracing with correlation IDs.

**REQ-MAINT-014** [U]  
The system shall expose Prometheus metrics.

**REQ-MAINT-015** [U]  
The system shall provide Grafana dashboards.

**REQ-MAINT-016** [U]  
The system shall implement alerting for critical issues.

### 9.5 Usability

**REQ-USE-001** [U]  
The system shall provide intuitive billing portal UI.

**REQ-USE-002** [U]  
The system shall provide clear error messages.

**REQ-USE-003** [U]  
The system shall provide helpful tooltips and documentation.

**REQ-USE-004** [U]  
The system shall implement mobile-responsive design.

**REQ-USE-005** [U]  
The system shall comply with WCAG 2.1 AA accessibility standards.

**REQ-USE-006** [U]  
The system shall support multiple currencies.

**REQ-USE-007** [U]  
The system shall support multiple languages.

**REQ-USE-008** [U]  
The system shall format dates and times according to locale.

**REQ-USE-009** [U]  
The system shall calculate taxes by region.

---

## 10. Integration Requirements

### 10.1 User Service Integration

**REQ-INT-001** [U]  
The system shall add subscriptionId, subscriptionStatus, and subscriptionPlanCode fields to Tenant entity in User Service.

**REQ-INT-002** [U]  
The system shall add subscription context to JWT tokens including subscriptionStatus, subscriptionPlan, and features.

**REQ-INT-003** [U]  
The system shall provide internal endpoint in User Service for billing service to update tenant subscription status.

**REQ-INT-004** [E]  
WHEN subscription status changes, the system shall update tenant record in User Service.

### 10.2 Gateway Service Integration

**REQ-INT-005** [U]  
The system shall implement FeatureAccessFilter in Gateway Service to check feature access based on subscription.

**REQ-INT-006** [E]  
WHEN request requires feature not in subscription THEN the system shall reject request with error code FEATURE_NOT_AVAILABLE and HTTP status 403.

**REQ-INT-007** [U]  
The system shall implement QuotaEnforcementFilter in Gateway Service to enforce API call quotas.

**REQ-INT-008** [E]  
WHEN API request is processed, the system shall record API call usage asynchronously.

**REQ-INT-009** [E]  
WHEN API quota is exceeded, the system shall reject request with error code QUOTA_EXCEEDED and HTTP status 429.

### 10.3 Email Service Integration

**REQ-INT-010** [U]  
The system shall create email templates for subscription_created, trial_ending, trial_ended, subscription_upgraded, subscription_downgraded, subscription_canceled, invoice_generated, payment_succeeded, payment_failed, payment_retry, subscription_past_due, and subscription_expired events.

**REQ-INT-011** [E]  
WHEN subscription is created, the system shall send subscription_created email.

**REQ-INT-012** [E]  
WHEN trial is ending, the system shall send trial_ending email.

**REQ-INT-013** [E]  
WHEN invoice is generated, the system shall send invoice_generated email.

**REQ-INT-014** [E]  
WHEN payment succeeds, the system shall send payment_succeeded email.

**REQ-INT-015** [E]  
WHEN payment fails, the system shall send payment_failed email.

---

## 11. Data Model Requirements

### 11.1 Subscription Plans

**REQ-DATA-001** [U]  
The system shall store subscription plans with fields: id, planCode (unique), name, description, tier, billingCycle, basePrice, currency, features (JSONB), quotas (JSONB), trialDays, active, public, createdAt, updatedAt.

**REQ-DATA-002** [U]  
The system shall create unique index on planCode field.

**REQ-DATA-003** [U]  
The system shall store subscription plans in public schema.

### 11.2 Subscriptions

**REQ-DATA-004** [U]  
The system shall store subscriptions with fields: id, tenantId, userId, planId (FK), status, currentPeriodStart, currentPeriodEnd, trialStart, trialEnd, canceledAt, cancelAtPeriodEnd, metadata (JSONB), createdAt, updatedAt.

**REQ-DATA-005** [U]  
The system shall create indexes on tenantId, status, and currentPeriodEnd fields.

**REQ-DATA-006** [U]  
The system shall store subscriptions in tenant-scoped schemas.

### 11.3 Usage Records

**REQ-DATA-007** [U]  
The system shall store usage records with fields: id, subscriptionId (FK), tenantId, metricType, quantity, unit, recordedAt, billingPeriodStart, billingPeriodEnd, metadata (JSONB), createdAt.

**REQ-DATA-008** [U]  
The system shall create indexes on tenantId, subscriptionId, billingPeriod, and metricType fields.

**REQ-DATA-009** [U]  
The system shall store usage records in tenant-scoped schemas.

### 11.4 Payment Methods

**REQ-DATA-010** [U]  
The system shall store payment methods with fields: id, tenantId, userId, type, providerPaymentMethodId, last4, brand, expiryMonth, expiryYear, isDefault, active, createdAt, updatedAt.

**REQ-DATA-011** [U]  
The system shall create indexes on tenantId and isDefault fields.

**REQ-DATA-012** [U]  
The system shall store payment methods in tenant-scoped schemas.

### 11.5 Invoices

**REQ-DATA-013** [U]  
The system shall store invoices with fields: id, subscriptionId (FK), tenantId, invoiceNumber (unique), status, subtotal, tax, total, currency, periodStart, periodEnd, dueDate, paidAt, paymentMethodId (FK), providerInvoiceId, lineItems (JSONB), metadata (JSONB), createdAt, updatedAt.

**REQ-DATA-014** [U]  
The system shall create indexes on tenantId, subscriptionId, status, and dueDate fields.

**REQ-DATA-015** [U]  
The system shall create unique index on invoiceNumber field.

**REQ-DATA-016** [U]  
The system shall store invoices in tenant-scoped schemas.

### 11.6 Payments

**REQ-DATA-017** [U]  
The system shall store payments with fields: id, invoiceId (FK), tenantId, amount, currency, status, paymentMethodId (FK), providerPaymentId, failureReason, metadata (JSONB), createdAt, updatedAt.

**REQ-DATA-018** [U]  
The system shall create indexes on invoiceId, tenantId, and status fields.

**REQ-DATA-019** [U]  
The system shall store payments in tenant-scoped schemas.

### 11.7 Billing Events

**REQ-DATA-020** [U]  
The system shall store billing events with fields: id, tenantId, subscriptionId (FK), eventType, eventData (JSONB), processedAt, createdAt.

**REQ-DATA-021** [U]  
The system shall create indexes on tenantId, subscriptionId, eventType, and createdAt fields.

**REQ-DATA-022** [U]  
The system shall store billing events in tenant-scoped schemas.

---

## 12. API Requirements

### 12.1 Public Endpoints

**REQ-API-001** [U]  
The system shall provide GET /api/v1/billing/plans endpoint to list available subscription plans.

**REQ-API-002** [U]  
The system shall provide GET /api/v1/billing/plans/{code} endpoint to get specific plan details.

### 12.2 Customer Portal Endpoints

**REQ-API-003** [U]  
The system shall provide GET /api/v1/billing/portal/dashboard endpoint to get billing dashboard data (authenticated).

**REQ-API-004** [U]  
The system shall provide POST /api/v1/billing/portal/subscription/upgrade endpoint to upgrade subscription (authenticated).

**REQ-API-005** [U]  
The system shall provide POST /api/v1/billing/portal/subscription/downgrade endpoint to downgrade subscription (authenticated).

**REQ-API-006** [U]  
The system shall provide POST /api/v1/billing/portal/subscription/cancel endpoint to cancel subscription (authenticated).

**REQ-API-007** [U]  
The system shall provide GET /api/v1/billing/portal/invoices endpoint to list invoices with pagination (authenticated).

**REQ-API-008** [U]  
The system shall provide GET /api/v1/billing/portal/invoices/{id}/pdf endpoint to download invoice PDF (authenticated).

**REQ-API-009** [U]  
The system shall provide GET /api/v1/billing/portal/usage endpoint to get current usage metrics (authenticated).

**REQ-API-010** [U]  
The system shall provide GET /api/v1/billing/portal/payment-methods endpoint to list payment methods (authenticated).

**REQ-API-011** [U]  
The system shall provide POST /api/v1/billing/portal/payment-methods endpoint to add payment method (authenticated).

**REQ-API-012** [U]  
The system shall provide DELETE /api/v1/billing/portal/payment-methods/{id} endpoint to remove payment method (authenticated).

### 12.3 Admin Endpoints

**REQ-API-013** [U]  
The system shall provide POST /api/v1/admin/billing/plans endpoint to create subscription plan (ADMIN role).

**REQ-API-014** [U]  
The system shall provide PUT /api/v1/admin/billing/plans/{id} endpoint to update subscription plan (ADMIN role).

**REQ-API-015** [U]  
The system shall provide GET /api/v1/admin/billing/subscriptions endpoint to list all subscriptions with filtering (ADMIN role).

**REQ-API-016** [U]  
The system shall provide GET /api/v1/admin/billing/subscriptions/{id} endpoint to get subscription details (ADMIN role).

**REQ-API-017** [U]  
The system shall provide POST /api/v1/admin/billing/subscriptions/{id}/cancel endpoint to force cancel subscription (ADMIN role).

**REQ-API-018** [U]  
The system shall provide POST /api/v1/admin/billing/subscriptions/{id}/extend-trial endpoint to extend trial period (ADMIN role).

**REQ-API-019** [U]  
The system shall provide GET /api/v1/admin/billing/invoices endpoint to list all invoices with filtering (ADMIN role).

**REQ-API-020** [U]  
The system shall provide POST /api/v1/admin/billing/invoices/{id}/void endpoint to void invoice (ADMIN role).

**REQ-API-021** [U]  
The system shall provide GET /api/v1/admin/billing/analytics/mrr endpoint to get Monthly Recurring Revenue (ADMIN role).

**REQ-API-022** [U]  
The system shall provide GET /api/v1/admin/billing/analytics/churn endpoint to get churn analysis (ADMIN role).

### 12.4 Webhook Endpoints

**REQ-API-023** [U]  
The system shall provide POST /api/v1/billing/webhooks/stripe endpoint for Stripe webhook handler.

**REQ-API-024** [U]  
The system shall provide POST /api/v1/billing/webhooks/paypal endpoint for PayPal webhook handler.

### 12.5 Internal Endpoints

**REQ-API-025** [U]  
The system shall provide internal endpoint for checking feature access by tenant.

**REQ-API-026** [U]  
The system shall provide internal endpoint for checking quota status by tenant.

**REQ-API-027** [U]  
The system shall provide internal endpoint for recording usage by tenant.

---

## 13. Business Rules

### 13.1 Subscription Rules

**REQ-BIZ-001** [U]  
The system shall enforce one active subscription per tenant rule.

**REQ-BIZ-002** [U]  
The system shall allow trial only for first subscription per tenant.

**REQ-BIZ-003** [U]  
The system shall grant full feature access during trial period.

**REQ-BIZ-004** [E]  
WHEN trial ends with payment method on file, the system shall auto-convert to ACTIVE and charge first invoice.

**REQ-BIZ-005** [E]  
WHEN trial ends without payment method, the system shall convert to EXPIRED and restrict access.

**REQ-BIZ-006** [E]  
WHEN user upgrades, the system shall apply changes immediately with proration.

**REQ-BIZ-007** [E]  
WHEN user downgrades, the system shall schedule changes for period end by default.

**REQ-BIZ-008** [E]  
WHEN user cancels, the system shall maintain access until period end by default.

### 13.2 Payment Rules

**REQ-BIZ-009** [U]  
The system shall require no payment method for FREE plan.

**REQ-BIZ-010** [U]  
The system shall make payment method optional during trial for paid plans.

**REQ-BIZ-011** [U]  
The system shall require payment method before trial end for paid plans.

**REQ-BIZ-012** [U]  
The system shall require payment method before activation for paid plans without trial.

**REQ-BIZ-013** [E]  
WHEN invoice is generated, the system shall attempt payment immediately.

**REQ-BIZ-014** [E]  
WHEN payment fails, the system shall retry after 3, 5, and 7 days.

**REQ-BIZ-015** [E]  
WHEN payment fails first time, the system shall set subscription to PAST_DUE.

**REQ-BIZ-016** [E]  
WHEN all payment retries fail, the system shall set subscription to EXPIRED after grace period.

**REQ-BIZ-017** [U]  
The system shall issue prorated refunds for immediate downgrades and cancellations.

**REQ-BIZ-018** [U]  
The system shall issue full refund within 7 days of initial payment (satisfaction guarantee).

### 13.3 Usage Rules

**REQ-BIZ-019** [U]  
The system shall enforce hard limits by blocking operations when quota exceeded.

**REQ-BIZ-020** [U]  
The system shall allow 5% overage for soft limits with warning.

**REQ-BIZ-021** [E]  
WHEN billing period starts, the system shall reset quota counters.

**REQ-BIZ-022** [U]  
The system shall record usage in real-time or near-real-time.

**REQ-BIZ-023** [U]  
The system shall aggregate usage by billing period for invoicing.

---

## 14. Error Handling Requirements

### 14.1 Error Codes

**REQ-ERR-001** [E]  
WHEN subscription does not exist, the system shall return error code SUBSCRIPTION_NOT_FOUND with HTTP status 404.

**REQ-ERR-002** [E]  
WHEN tenant already has active subscription, the system shall return error code SUBSCRIPTION_ALREADY_EXISTS with HTTP status 409.

**REQ-ERR-003** [E]  
WHEN subscription plan does not exist, the system shall return error code PLAN_NOT_FOUND with HTTP status 404.

**REQ-ERR-004** [E]  
WHEN plan transition is invalid, the system shall return error code INVALID_PLAN_TRANSITION with HTTP status 400.

**REQ-ERR-005** [E]  
WHEN usage quota is exceeded, the system shall return error code QUOTA_EXCEEDED with HTTP status 429.

**REQ-ERR-006** [E]  
WHEN feature is not available in plan, the system shall return error code FEATURE_NOT_AVAILABLE with HTTP status 403.

**REQ-ERR-007** [E]  
WHEN payment method is required, the system shall return error code PAYMENT_REQUIRED with HTTP status 402.

**REQ-ERR-008** [E]  
WHEN payment processing fails, the system shall return error code PAYMENT_FAILED with HTTP status 402.

**REQ-ERR-009** [E]  
WHEN invoice does not exist, the system shall return error code INVOICE_NOT_FOUND with HTTP status 404.

**REQ-ERR-010** [E]  
WHEN payment method is invalid or expired, the system shall return error code INVALID_PAYMENT_METHOD with HTTP status 400.

**REQ-ERR-011** [E]  
WHEN usage exceeds new plan limits, the system shall return error code USAGE_LIMIT_EXCEEDED with HTTP status 400.

### 14.2 Error Response Format

**REQ-ERR-012** [U]  
The system shall return error responses with fields: error (code), message (description), timestamp, path, and details (optional context).

**REQ-ERR-013** [E]  
WHEN quota is exceeded, the system shall include quota details in error response: limit, used, percentage, resetDate, and upgradeUrl.

**REQ-ERR-014** [E]  
WHEN feature is not available, the system shall include feature name and required plan in error response.

**REQ-ERR-015** [E]  
WHEN payment fails, the system shall include failure reason and retry information in error response.

---

## 15. Testing Requirements

### 15.1 Unit Testing

**REQ-TEST-001** [U]  
The system shall maintain minimum 80% unit test coverage.

**REQ-TEST-002** [U]  
The system shall include unit tests for service layer business logic.

**REQ-TEST-003** [U]  
The system shall include unit tests for proration calculations.

**REQ-TEST-004** [U]  
The system shall include unit tests for subscription state transitions.

**REQ-TEST-005** [U]  
The system shall include unit tests for quota enforcement logic.

**REQ-TEST-006** [U]  
The system shall include unit tests for payment processing logic.

**REQ-TEST-007** [U]  
The system shall include unit tests for invoice generation logic.

### 15.2 Integration Testing

**REQ-TEST-008** [U]  
The system shall include integration tests using Testcontainers for database operations.

**REQ-TEST-009** [U]  
The system shall include integration tests with mocked payment provider.

**REQ-TEST-010** [U]  
The system shall include integration tests for webhook handling.

**REQ-TEST-011** [U]  
The system shall include end-to-end tests for subscription flows.

**REQ-TEST-012** [U]  
The system shall include integration tests for multi-tenant isolation.

### 15.3 Architecture Testing

**REQ-TEST-013** [U]  
The system shall include ArchUnit tests to verify services reside in correct package.

**REQ-TEST-014** [U]  
The system shall include ArchUnit tests to verify repositories are only accessed by services.

**REQ-TEST-015** [U]  
The system shall include ArchUnit tests to verify no circular dependencies exist.

### 15.4 Performance Testing

**REQ-TEST-016** [U]  
The system shall include performance tests for 1000 concurrent subscription creations.

**REQ-TEST-017** [U]  
The system shall include performance tests for 10,000 usage records per second.

**REQ-TEST-018** [U]  
The system shall include performance tests for 100 payments per minute.

**REQ-TEST-019** [U]  
The system shall include performance tests for invoice generation for 1000 subscriptions.

**REQ-TEST-020** [U]  
The system shall verify no memory leaks in performance tests.

**REQ-TEST-021** [U]  
The system shall verify database connection pool stability in performance tests.

**REQ-TEST-022** [U]  
The system shall verify no deadlocks occur in performance tests.

### 15.5 Security Testing

**REQ-TEST-023** [U]  
The system shall include security tests for authentication and authorization.

**REQ-TEST-024** [U]  
The system shall include security tests for tenant isolation.

**REQ-TEST-025** [U]  
The system shall include security tests for payment data security.

**REQ-TEST-026** [U]  
The system shall include security tests for webhook signature verification.

**REQ-TEST-027** [U]  
The system shall include security tests for SQL injection prevention.

**REQ-TEST-028** [U]  
The system shall include security tests for XSS prevention.

---

## 16. Deployment Requirements

### 16.1 Environment Configuration

**REQ-DEPLOY-001** [U]  
The system shall support local, staging, and production environments.

**REQ-DEPLOY-002** [U]  
The system shall configure payment provider via environment-specific configuration files.

**REQ-DEPLOY-003** [U]  
The system shall store sensitive configuration (API keys, secrets) in environment variables.

**REQ-DEPLOY-004** [U]  
The system shall enable usage metering, proration, and dunning features via configuration.

### 16.2 Infrastructure

**REQ-DEPLOY-005** [U]  
The system shall deploy minimum 2 service instances for high availability.

**REQ-DEPLOY-006** [U]  
The system shall implement auto-scaling based on CPU/memory thresholds (50-80%).

**REQ-DEPLOY-007** [U]  
The system shall allocate 2 CPU cores and 4GB RAM per service instance.

**REQ-DEPLOY-008** [U]  
The system shall use PostgreSQL 15+ with connection pool of 20 connections per instance.

**REQ-DEPLOY-009** [U]  
The system shall perform daily database backups with 30-day retention.

**REQ-DEPLOY-010** [U]  
The system shall use Redis with minimum 2GB memory and persistence enabled.

**REQ-DEPLOY-011** [U]  
The system shall use RabbitMQ with durable queues and dead letter exchange.

### 16.3 Monitoring & Alerting

**REQ-DEPLOY-012** [U]  
The system shall monitor request rate and latency metrics.

**REQ-DEPLOY-013** [U]  
The system shall monitor error rate by endpoint.

**REQ-DEPLOY-014** [U]  
The system shall monitor payment success and failure rates.

**REQ-DEPLOY-015** [U]  
The system shall monitor subscription churn rate.

**REQ-DEPLOY-016** [U]  
The system shall monitor database connection pool usage.

**REQ-DEPLOY-017** [U]  
The system shall monitor cache hit rate.

**REQ-DEPLOY-018** [U]  
The system shall monitor message queue depth.

**REQ-DEPLOY-019** [E]  
WHEN payment failure rate exceeds 5%, the system shall trigger alert.

**REQ-DEPLOY-020** [E]  
WHEN API error rate exceeds 1%, the system shall trigger alert.

**REQ-DEPLOY-021** [E]  
WHEN response time p95 exceeds 500ms, the system shall trigger alert.

**REQ-DEPLOY-022** [E]  
WHEN database connection pool exceeds 80%, the system shall trigger alert.

**REQ-DEPLOY-023** [E]  
WHEN failed webhook processing occurs, the system shall trigger alert.

**REQ-DEPLOY-024** [U]  
The system shall provide service health dashboard.

**REQ-DEPLOY-025** [U]  
The system shall provide billing metrics dashboard.

**REQ-DEPLOY-026** [U]  
The system shall provide revenue dashboard.

**REQ-DEPLOY-027** [U]  
The system shall provide usage analytics dashboard.

### 16.4 Backup & Recovery

**REQ-DEPLOY-028** [U]  
The system shall perform daily full database backups.

**REQ-DEPLOY-029** [U]  
The system shall perform hourly incremental database backups.

**REQ-DEPLOY-030** [U]  
The system shall retain backups for 30 days.

**REQ-DEPLOY-031** [U]  
The system shall perform weekly backup verification via restore test.

**REQ-DEPLOY-032** [U]  
The system shall achieve Recovery Time Objective (RTO) of 4 hours.

**REQ-DEPLOY-033** [U]  
The system shall achieve Recovery Point Objective (RPO) of 1 hour.

**REQ-DEPLOY-034** [U]  
The system shall maintain documented disaster recovery procedures.

**REQ-DEPLOY-035** [U]  
The system shall conduct quarterly disaster recovery drills.

---

## 17. Migration Requirements

### 17.1 Existing Tenant Migration

**REQ-MIG-001** [E]  
WHEN migrating tenants without subscriptions, the system shall create default FREE plan subscription.

**REQ-MIG-002** [E]  
WHEN migrating tenants without subscriptions, the system shall set subscription status to ACTIVE.

**REQ-MIG-003** [E]  
WHEN migrating tenants without subscriptions, the system shall notify tenant of new billing system.

**REQ-MIG-004** [E]  
WHEN migrating tenants with custom agreements, the system shall create custom ENTERPRISE plan.

**REQ-MIG-005** [E]  
WHEN migrating tenants with custom agreements, the system shall set custom pricing.

**REQ-MIG-006** [E]  
WHEN migrating tenants with custom agreements, the system shall import existing payment methods.

**REQ-MIG-007** [E]  
WHEN migrating tenants with custom agreements, the system shall migrate billing history.

### 17.2 Data Migration Process

**REQ-MIG-008** [U]  
The system shall create subscription plans in new system before tenant migration.

**REQ-MIG-009** [U]  
The system shall map existing tenants to appropriate plans during migration.

**REQ-MIG-010** [U]  
The system shall create subscription records during migration.

**REQ-MIG-011** [O]  
WHERE payment methods exist, the system shall import payment methods during migration.

**REQ-MIG-012** [O]  
WHERE invoice history exists, the system shall migrate invoice history during migration.

**REQ-MIG-013** [U]  
The system shall validate data integrity after migration.

**REQ-MIG-014** [U]  
The system shall enable billing service routing after successful migration.

**REQ-MIG-015** [U]  
The system shall monitor for issues after migration.

### 17.3 Rollback Plan

**REQ-MIG-016** [UW]  
IF migration issues are detected THEN the system shall disable billing service routing in gateway.

**REQ-MIG-017** [UW]  
IF migration issues are detected THEN the system shall revert tenant subscription references.

**REQ-MIG-018** [UW]  
IF migration issues are detected THEN the system shall restore previous billing system if applicable.

**REQ-MIG-019** [UW]  
IF migration issues are detected THEN the system shall investigate and fix issues before re-attempting migration.

---

## 18. Compliance & Legal Requirements

### 18.1 Data Privacy

**REQ-COMP-001** [U]  
The system shall comply with GDPR right to access by providing billing data export.

**REQ-COMP-002** [U]  
The system shall comply with GDPR right to deletion by deleting billing data on request.

**REQ-COMP-003** [U]  
The system shall implement data minimization by collecting only necessary billing data.

**REQ-COMP-004** [U]  
The system shall obtain explicit consent for payment processing.

**REQ-COMP-005** [U]  
The system shall comply with CCPA data privacy requirements.

**REQ-COMP-006** [U]  
The system shall implement data retention policies for billing records.

**REQ-COMP-007** [U]  
The system shall retain invoices indefinitely for tax and audit compliance.

**REQ-COMP-008** [U]  
The system shall anonymize or delete personal data after retention period expires.

### 18.2 Financial Compliance

**REQ-COMP-009** [U]  
The system shall comply with PCI DSS Level 1 requirements through payment provider.

**REQ-COMP-010** [U]  
The system shall maintain audit trail for all financial transactions.

**REQ-COMP-011** [U]  
The system shall generate tax-compliant invoices with required information.

**REQ-COMP-012** [U]  
The system shall calculate and collect applicable taxes by jurisdiction.

**REQ-COMP-013** [U]  
The system shall support tax reporting requirements.

**REQ-COMP-014** [U]  
The system shall maintain financial records for minimum 7 years.

---

## 18. Tactical Domain-Driven Design Requirements

### 18.1 Bounded Contexts

**REQ-DDD-001** [U]  
The system shall implement the Billing bounded context as a separate microservice with clear boundaries.

**REQ-DDD-002** [U]  
The system shall define explicit context mapping with User Service (Customer-Supplier), Gateway Service (Conformist), and Email Service (Published Language).

**REQ-DDD-003** [U]  
The system shall use anti-corruption layer when integrating with external payment providers.

**REQ-DDD-004** [U]  
The system shall maintain ubiquitous language within the Billing context using terms: Subscription, Plan, Invoice, Payment, Usage, Quota, Proration, Trial.

### 18.2 Aggregates and Aggregate Roots

**REQ-DDD-005** [U]  
The system shall implement Subscription as an aggregate root with identity, lifecycle, and invariants.

**REQ-DDD-006** [U]  
The system shall implement SubscriptionPlan as an aggregate root in the public schema.

**REQ-DDD-007** [U]  
The system shall implement Invoice as an aggregate root with InvoiceLineItem as entities within the aggregate.

**REQ-DDD-008** [U]  
The system shall implement Payment as an aggregate root.

**REQ-DDD-009** [U]  
The system shall implement PaymentMethod as an aggregate root.

**REQ-DDD-010** [U]  
The system shall enforce aggregate boundaries by only allowing external references via aggregate root IDs.

**REQ-DDD-011** [U]  
The system shall ensure all modifications to aggregate entities go through the aggregate root.

**REQ-DDD-012** [U]  
The system shall maintain aggregate consistency within a single transaction boundary.

### 18.3 Entities and Value Objects

**REQ-DDD-013** [U]  
The system shall implement Subscription, SubscriptionPlan, Invoice, Payment, PaymentMethod, UsageRecord as entities with unique identity.

**REQ-DDD-014** [U]  
The system shall implement InvoiceLineItem as an entity within Invoice aggregate.

**REQ-DDD-015** [U]  
The system shall implement PlanQuotas, ProrationResult, UsageSummary, UsageMetric, QuotaCheckResult as value objects without identity.

**REQ-DDD-016** [U]  
The system shall ensure value objects are immutable.

**REQ-DDD-017** [U]  
The system shall implement value object equality based on attributes, not identity.

**REQ-DDD-018** [U]  
The system shall use value objects to encapsulate complex business logic (e.g., proration calculations).

### 18.4 Domain Services

**REQ-DDD-019** [U]  
The system shall implement ProrationCalculator as a domain service for proration logic that doesn't belong to any single aggregate.

**REQ-DDD-020** [U]  
The system shall implement QuotaEnforcer as a domain service for quota validation across subscription and usage.

**REQ-DDD-021** [U]  
The system shall implement SubscriptionLifecycleManager as a domain service for complex state transitions.

**REQ-DDD-022** [U]  
The system shall implement InvoiceGenerator as a domain service for invoice creation logic.

**REQ-DDD-023** [U]  
The system shall keep domain services stateless and focused on domain logic only.

### 18.5 Repositories

**REQ-DDD-024** [U]  
The system shall implement repository interfaces in the domain layer.

**REQ-DDD-025** [U]  
The system shall implement repository implementations in the infrastructure layer.

**REQ-DDD-026** [U]  
The system shall provide one repository per aggregate root.

**REQ-DDD-027** [U]  
The system shall use repositories to abstract persistence mechanisms from domain logic.

**REQ-DDD-028** [U]  
The system shall implement repository methods that return fully reconstituted aggregates.

**REQ-DDD-029** [U]  
The system shall use specification pattern in repositories for complex queries.

### 18.6 Domain Events

**REQ-DDD-030** [U]  
The system shall publish domain events for significant business occurrences: SubscriptionCreated, SubscriptionUpgraded, SubscriptionDowngraded, SubscriptionCanceled, SubscriptionReactivated, TrialStarted, TrialEnding, TrialEnded, InvoiceGenerated, InvoicePaid, InvoiceVoided, PaymentSucceeded, PaymentFailed, PaymentRefunded, UsageRecorded, QuotaExceeded.

**REQ-DDD-031** [U]  
The system shall implement domain events as immutable value objects with timestamp and aggregate identity.

**REQ-DDD-032** [U]  
The system shall publish domain events after successful aggregate persistence.

**REQ-DDD-033** [U]  
The system shall use domain events for eventual consistency between aggregates.

**REQ-DDD-034** [U]  
The system shall use domain events to trigger side effects (notifications, integrations) without coupling aggregates.

### 18.7 Factories

**REQ-DDD-035** [U]  
The system shall implement SubscriptionFactory for complex subscription creation logic.

**REQ-DDD-036** [U]  
The system shall implement InvoiceFactory for invoice creation with line items.

**REQ-DDD-037** [U]  
The system shall use factories to encapsulate complex object construction and ensure invariants.

**REQ-DDD-038** [U]  
The system shall validate business rules in factories before object creation.

### 18.8 Specifications

**REQ-DDD-039** [U]  
The system shall implement ActiveSubscriptionSpecification to check if subscription is active.

**REQ-DDD-040** [U]  
The system shall implement QuotaExceededSpecification to check if usage exceeds quota.

**REQ-DDD-041** [U]  
The system shall implement ValidPlanTransitionSpecification to validate plan upgrades/downgrades.

**REQ-DDD-042** [U]  
The system shall implement TrialEligibilitySpecification to check if tenant is eligible for trial.

**REQ-DDD-043** [U]  
The system shall use specifications to encapsulate business rules that can be reused and combined.

**REQ-DDD-044** [U]  
The system shall implement composite specifications using AND, OR, NOT operators.

### 18.9 Layered Architecture

**REQ-DDD-045** [U]  
The system shall organize code into layers: Domain, Application, Infrastructure, Presentation.

**REQ-DDD-046** [U]  
The system shall place entities, value objects, domain services, repositories (interfaces), domain events, factories, and specifications in the Domain layer.

**REQ-DDD-047** [U]  
The system shall place application services, DTOs, use case orchestration in the Application layer.

**REQ-DDD-048** [U]  
The system shall place repository implementations, external service adapters, database configurations in the Infrastructure layer.

**REQ-DDD-049** [U]  
The system shall place REST controllers, request/response models, exception handlers in the Presentation layer.

**REQ-DDD-050** [U]  
The system shall enforce dependency rule: Domain depends on nothing, Application depends on Domain, Infrastructure depends on Domain and Application, Presentation depends on Application.

**REQ-DDD-051** [U]  
The system shall use dependency injection to invert infrastructure dependencies.

### 18.10 Application Services

**REQ-DDD-052** [U]  
The system shall implement application services as thin orchestration layer coordinating domain objects.

**REQ-DDD-053** [U]  
The system shall keep business logic in domain layer, not in application services.

**REQ-DDD-054** [U]  
The system shall use application services to manage transactions.

**REQ-DDD-055** [U]  
The system shall use application services to publish domain events.

**REQ-DDD-056** [U]  
The system shall use application services to translate between domain objects and DTOs.

### 18.11 Anti-Corruption Layer

**REQ-DDD-057** [U]  
The system shall implement PaymentProviderAdapter as anti-corruption layer for Stripe integration.

**REQ-DDD-058** [U]  
The system shall implement PaymentProviderAdapter as anti-corruption layer for PayPal integration.

**REQ-DDD-059** [U]  
The system shall translate external payment provider models to domain models in the adapter.

**REQ-DDD-060** [U]  
The system shall isolate domain model from external API changes using the anti-corruption layer.

### 18.12 Package Structure

**REQ-DDD-061** [U]  
The system shall organize packages by layer and feature: com.iqscaffold.billing.domain.subscription, com.iqscaffold.billing.domain.invoice, com.iqscaffold.billing.domain.payment, com.iqscaffold.billing.domain.usage, com.iqscaffold.billing.application, com.iqscaffold.billing.infrastructure, com.iqscaffold.billing.presentation.

**REQ-DDD-062** [U]  
The system shall place shared kernel (common value objects, base classes) in com.iqscaffold.billing.domain.shared.

**REQ-DDD-063** [U]  
The system shall enforce package dependencies using ArchUnit tests.

---

## 19. Event Handling & ern Summary

| Pattern          | Template                                           | Usage                                  |
| ---------------- | -------------------------------------------------- | -------------------------------------- |
| Ubiquitous (U)   | The system shall [requirement]                     | Always active requirements             |
| Event-driven (E) | WHEN [trigger] the system shall [requirement]      | Requirements triggered by events       |
| State-driven (S) | WHILE [state] the system shall [requirement]       | Requirements active in specific states |
| Optional (O)     | WHERE [feature] the system shall [requirement]     | Requirements for optional features     |
| Unwanted (UW)    | IF [condition] THEN the system shall [requirement] | Requirements handling error conditions |

---

## Appendix B: Requirement Traceability

### Critical Path Requirements (Must Have for MVP)

**Subscription Management:**

- REQ-SUB-001 to REQ-SUB-007 (Subscription Creation)
- REQ-SUB-016 to REQ-SUB-022 (Status Management)
- REQ-SUB-023 to REQ-SUB-029 (Upgrade)
- REQ-SUB-039 to REQ-SUB-044 (Cancellation)

**Payment Processing:**

- REQ-PAY-001 to REQ-PAY-006 (Provider Abstraction)
- REQ-PAY-007 to REQ-PAY-019 (Payment Methods)
- REQ-PAY-020 to REQ-PAY-029 (Payment Processing)

**Invoice Management:**

- REQ-INV-001 to REQ-INV-010 (Invoice Generation)
- REQ-INV-016 to REQ-INV-021 (Invoice Payment)

**Usage & Quotas:**

- REQ-USAGE-001 to REQ-USAGE-006 (Usage Tracking)
- REQ-USAGE-011 to REQ-USAGE-016 (Quota Enforcement)

### High Priority (Should Have)

**Trial Management:**

- REQ-SUB-008 to REQ-SUB-015 (Trial Period)

**Downgrade:**

- REQ-SUB-031 to REQ-SUB-038 (Downgrade)

**Usage Reporting:**

- REQ-USAGE-017 to REQ-USAGE-023 (Usage Reporting)

**Billing Portal:**

- REQ-PORTAL-001 to REQ-PORTAL-034 (All Portal Features)

### Medium Priority (Nice to Have)

**Reactivation:**

- REQ-SUB-046 to REQ-SUB-050 (Reactivation)

**Refunds:**

- REQ-PAY-030 to REQ-PAY-036 (Refund Processing)

**Admin Functions:**

- REQ-ADMIN-001 to REQ-ADMIN-027 (All Admin Features)

---

## Document Revision History

| Version | Date       | Author  | Changes                            |
| ------- | ---------- | ------- | ---------------------------------- |
| 1.0.0   | 2024-12-05 | Kiro AI | Initial EARS requirements document |

---

**End of Requirements Document**
