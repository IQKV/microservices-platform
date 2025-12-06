# Design Document: Billing & Subscription Management Service

## Overview

The Billing & Subscription Management Service (iqscaffold-billing-service) is a **microservice with orchestration capabilities** that manages subscription lifecycles, payment processing, invoicing, and usage-based billing for the IQ Scaffold platform. Beyond traditional billing operations, this service provides lightweight APIs that other microservices can call to check subscription status, verify feature access, and enforce quotas based on subscription tiers.

**Service Naming:** `iqscaffold-billing-service`

**Rationale:** The name clearly communicates the primary responsibility (billing and subscriptions) while the architecture supports lightweight orchestration through simple API calls that other services make to check subscription status and quotas.

### Architectural Role

The billing service manages subscriptions and billing while providing **lightweight integration APIs** for other microservices:

- **Subscription Lifecycle Management**: Create, upgrade, downgrade, cancel, and reactivate subscriptions with trial periods and proration
- **Payment Processing**: Multi-provider payment integration (Stripe, PayPal, manual) with automated retry logic
- **Invoice Management**: Automated invoice generation, payment processing, PDF generation, and delivery
- **Usage-Based Billing**: Metered billing for overages and consumption-based pricing
- **Multi-Tenancy**: Strict tenant isolation using schema-per-tenant strategy

**Lightweight Orchestration APIs** (for easy integration with business microservices):
- **Subscription Status Check**: Simple API for other services (CRM, Campaign, Email, Scoring, etc.) to verify if a tenant has an active subscription
- **Feature Access Check**: Lightweight API to check if a tenant's plan includes specific features
- **Quota Enforcement**: Simple quota check and usage recording APIs for services to enforce limits (email sends, API calls, storage, etc.)
- **Usage Recording**: Easy-to-use API for services to report usage metrics for billing and quota tracking

### Key Capabilities

**Core Billing Operations:**
- **Subscription Lifecycle**: Create, upgrade, downgrade, cancel, reactivate subscriptions with trial periods and proration
- **Payment Processing**: Multi-provider payment integration (Stripe, PayPal, manual) with automated retry logic
- **Invoice Management**: Automated invoice generation, payment processing, PDF generation, and delivery
- **Usage-Based Billing**: Metered billing for overages and consumption-based pricing
- **Multi-Tenancy**: Strict tenant isolation using schema-per-tenant strategy

**Easy Integration for Business Microservices:**

The billing service provides **simple REST APIs** that business microservices (CRM, Campaign Service, Email Sender, Scoring Service, etc.) can easily call to enforce billing restrictions:

1. **Subscription Status Check**
   - `GET /api/v1/billing/subscriptions/{tenantId}/status` → Check if tenant has active subscription
   - Use case: Block access to CRM/Campaign/Email services if subscription expired

2. **Feature Access Check**
   - `POST /api/v1/billing/subscriptions/{tenantId}/check-feature` → Check if plan includes a feature
   - Use case: Enable/disable advanced features (AI scoring, bulk email, custom workflows) based on plan tier

3. **Quota Check & Enforcement**
   - `POST /api/v1/billing/usage/{tenantId}/check-quota` → Check if quota available before operation
   - `POST /api/v1/billing/usage/{tenantId}/record` → Record usage after operation
   - Use case: Enforce limits on email sends, API calls, storage, campaign executions, scoring requests

4. **Plan Information**
   - `GET /api/v1/billing/subscriptions/{tenantId}/plan` → Get current plan details with features and quotas
   - Use case: Display plan limits in service UIs, show upgrade prompts

**Example Integration Pattern:**
```java
// In CRM Service - before creating a contact
@Service
public class ContactService {
    private final BillingClient billingClient;
    
    public Contact createContact(String tenantId, ContactRequest request) {
        // Check subscription status
        SubscriptionStatus status = billingClient.getSubscriptionStatus(tenantId);
        if (!status.isActive()) {
            throw new SubscriptionInactiveException("Please renew your subscription");
        }
        
        // Check quota
        QuotaCheckResult quota = billingClient.checkQuota(tenantId, "CONTACTS", 1);
        if (!quota.isAvailable()) {
            throw new QuotaExceededException("Contact limit reached. Upgrade your plan.");
        }
        
        // Create contact
        Contact contact = contactRepository.save(new Contact(request));
        
        // Record usage
        billingClient.recordUsage(tenantId, "CONTACTS", 1);
        
        return contact;
    }
}
```

**Customer & Admin Experience:**
- **Customer Portal**: Self-service dashboard for subscription management, usage monitoring, invoices, and payment methods
- **Administrative Functions**: Comprehensive admin tools for subscription management, analytics, and manual billing operations
- **Usage Analytics**: Real-time usage dashboards showing consumption across all platform services

### Design Principles

1. **Payment Provider Abstraction**: Use adapter pattern to support multiple payment providers without coupling to specific implementations
2. **Event-Driven Architecture**: Leverage RabbitMQ for asynchronous operations (usage metering, invoice generation, webhooks)
3. **Idempotency**: Ensure all financial operations are idempotent to prevent duplicate charges
4. **Audit Trail**: Maintain comprehensive, immutable audit logs for all billing operations
5. **Graceful Degradation**: Continue operating with reduced functionality when external dependencies are unavailable
6. **Tenant Isolation**: Enforce strict data separation using database schemas
7. **Proration Accuracy**: Calculate prorated charges and credits precisely for mid-period changes
8. **Modern Java Practices**: Leverage Java 21 features (records, text blocks, pattern matching, switch expressions) for clean, maintainable code
9. **Internationalization**: Full i18n support for multi-language customer communication
10. **Domain-Driven Design**: Organize code by domain concepts with clear package structure following platform conventions

---

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Gateway Service                           │
│                  (Routing, Auth, Rate Limiting)                  │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Billing Service (Port 8082)                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │   REST API   │  │   Admin API  │  │  Internal    │          │
│  │  Controllers │  │  Controllers │  │  API         │          │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘          │
│         │                  │                  │                   │
│         └──────────────────┼──────────────────┘                   │
│                            ▼                                      │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │                    Service Layer                            │ │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │ │
│  │  │ Subscription │  │   Payment    │  │   Invoice    │     │ │
│  │  │   Service    │  │   Service    │  │   Service    │     │ │
│  │  └──────────────┘  └──────────────┘  └──────────────┘     │ │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │ │
│  │  │    Usage     │  │     Plan     │  │   Webhook    │     │ │
│  │  │   Service    │  │   Service    │  │   Service    │     │ │
│  │  └──────────────┘  └──────────────┘  └──────────────┘     │ │
│  └────────────────────────────────────────────────────────────┘ │
│                            ▼                                      │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │                  Repository Layer                           │ │
│  │  (JPA Repositories with Multi-Tenant Context)              │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
         │                    │                    │
         ▼                    ▼                    ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│  PostgreSQL  │    │    Redis     │    │   RabbitMQ   │
│  (Multi-     │    │  (Caching,   │    │  (Async      │
│   Tenant)    │    │   Sessions)  │    │   Events)    │
└──────────────┘    └──────────────┘    └──────────────┘

External Integrations:
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│    Stripe    │    │    PayPal    │    │    Email     │
│   Provider   │    │   Provider   │    │   Service    │
└──────────────┘    └──────────────┘    └──────────────┘
```

### Domain-Driven Design Architecture

The billing service follows tactical Domain-Driven Design patterns to ensure a maintainable, testable, and business-focused architecture.

#### Layered Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                        │
│  REST Controllers, Request/Response DTOs, Exception Handlers │
│                           ↓                                   │
├─────────────────────────────────────────────────────────────┤
│                    Application Layer                         │
│  Application Services, Use Case Orchestration, DTOs         │
│                           ↓                                   │
├─────────────────────────────────────────────────────────────┤
│                      Domain Layer                            │
│  Entities, Value Objects, Domain Services, Repositories     │
│  (interfaces), Domain Events, Factories, Specifications     │
│                           ↓                                   │
├─────────────────────────────────────────────────────────────┤
│                  Infrastructure Layer                        │
│  Repository Implementations, External Service Adapters,     │
│  Database Configurations, Message Queue Implementations     │
└─────────────────────────────────────────────────────────────┘
```

**Dependency Rule**: Domain depends on nothing. Application depends on Domain. Infrastructure depends on Domain and Application. Presentation depends on Application.

**Design Rationale**: This layered architecture ensures that business logic remains isolated in the domain layer, making it testable and independent of infrastructure concerns. The dependency inversion principle allows us to swap implementations without affecting business logic.

#### Aggregates and Aggregate Roots

**Subscription Aggregate**
- **Aggregate Root**: Subscription
- **Entities**: None (subscription is self-contained)
- **Invariants**: 
  - One active subscription per tenant
  - Status transitions must follow valid state machine
  - Trial period cannot be negative
  - Current period end must be after start
- **Boundary**: All subscription modifications go through the Subscription aggregate root

**Invoice Aggregate**
- **Aggregate Root**: Invoice
- **Entities**: InvoiceLineItem (within aggregate)
- **Invariants**:
  - Invoice total must equal sum of line items plus tax
  - Invoice number must be unique
  - Paid invoices cannot be modified
  - Line items cannot be empty
- **Boundary**: Line items are only accessible through Invoice

**Payment Aggregate**
- **Aggregate Root**: Payment
- **Entities**: None
- **Invariants**:
  - Payment amount must be positive
  - Refund amount cannot exceed original payment
  - Payment must reference valid invoice
- **Boundary**: Payment state changes go through Payment aggregate root

**SubscriptionPlan Aggregate** (Public Schema)
- **Aggregate Root**: SubscriptionPlan
- **Entities**: None
- **Invariants**:
  - Plan code must be unique
  - Base price must be non-negative
  - Trial days must be non-negative
- **Boundary**: Plan modifications go through SubscriptionPlan aggregate root

**PaymentMethod Aggregate**
- **Aggregate Root**: PaymentMethod
- **Entities**: None
- **Invariants**:
  - Only one default payment method per tenant
  - Expiry date must be in the future for active methods
- **Boundary**: Payment method state changes go through PaymentMethod aggregate root

**Design Rationale**: Aggregates enforce consistency boundaries and business invariants. By defining clear aggregate boundaries, we ensure that all business rules are enforced and data remains consistent within transaction boundaries.

#### Domain Services

Domain services encapsulate business logic that doesn't naturally belong to a single aggregate:

**ProrationCalculator**
```java
@Service
public class ProrationCalculator {
    public ProrationResult calculate(
        Subscription subscription,
        SubscriptionPlan newPlan,
        LocalDateTime effectiveDate
    ) {
        // Complex proration logic spanning subscription and plan
    }
}
```

**QuotaEnforcer**
```java
@Service
public class QuotaEnforcer {
    public QuotaCheckResult checkQuota(
        Subscription subscription,
        MetricType metricType,
        long requestedQuantity
    ) {
        // Quota validation logic spanning subscription, plan, and usage
    }
}
```

**SubscriptionLifecycleManager**
```java
@Service
public class SubscriptionLifecycleManager {
    public void transitionStatus(
        Subscription subscription,
        SubscriptionStatus newStatus,
        String reason
    ) {
        // Complex state transition logic with validation
    }
}
```

**InvoiceGenerator**
```java
@Service
public class InvoiceGenerator {
    public Invoice generate(
        Subscription subscription,
        LocalDateTime periodStart,
        LocalDateTime periodEnd
    ) {
        // Invoice creation logic spanning subscription, plan, and usage
    }
}
```

**Design Rationale**: Domain services keep business logic in the domain layer while avoiding artificial assignment to aggregates. They remain stateless and focused on domain operations.

#### Value Objects

Value objects are immutable and defined by their attributes rather than identity:

**PlanQuotas** - Encapsulates quota limits
**ProrationResult** - Encapsulates proration calculation results
**UsageSummary** - Encapsulates usage aggregation
**UsageMetric** - Encapsulates individual metric usage
**QuotaCheckResult** - Encapsulates quota validation results
**InvoiceLineItem** - Encapsulates invoice line details

**Design Rationale**: Value objects provide type safety, encapsulate business logic, and ensure immutability. They can be freely shared and compared by value rather than identity.

#### Specifications

Specifications encapsulate business rules that can be reused and combined:

**ActiveSubscriptionSpecification**
```java
public class ActiveSubscriptionSpecification implements Specification<Subscription> {
    @Override
    public boolean isSatisfiedBy(Subscription subscription) {
        return subscription.getStatus() == SubscriptionStatus.ACTIVE
            && subscription.getCurrentPeriodEnd().isAfter(LocalDateTime.now());
    }
}
```

**QuotaExceededSpecification**
```java
public class QuotaExceededSpecification implements Specification<UsageContext> {
    @Override
    public boolean isSatisfiedBy(UsageContext context) {
        return context.getCurrentUsage() >= context.getQuotaLimit();
    }
}
```

**ValidPlanTransitionSpecification**
```java
public class ValidPlanTransitionSpecification implements Specification<PlanTransition> {
    @Override
    public boolean isSatisfiedBy(PlanTransition transition) {
        // Validate upgrade/downgrade rules
    }
}
```

**TrialEligibilitySpecification**
```java
public class TrialEligibilitySpecification implements Specification<Tenant> {
    @Override
    public boolean isSatisfiedBy(Tenant tenant) {
        // Check if tenant has never had a trial
    }
}
```

**Design Rationale**: Specifications make business rules explicit, testable, and reusable. They can be combined using AND, OR, NOT operators to create complex validation logic.

#### Factories

Factories encapsulate complex object construction:

**SubscriptionFactory**
```java
@Component
public class SubscriptionFactory {
    public Subscription createTrialSubscription(
        UUID tenantId,
        UUID userId,
        SubscriptionPlan plan
    ) {
        // Complex subscription creation with trial logic
        // Validates business rules before creation
    }
    
    public Subscription createPaidSubscription(
        UUID tenantId,
        UUID userId,
        SubscriptionPlan plan,
        PaymentMethod paymentMethod
    ) {
        // Complex subscription creation with payment validation
    }
}
```

**InvoiceFactory**
```java
@Component
public class InvoiceFactory {
    public Invoice createSubscriptionInvoice(
        Subscription subscription,
        List<InvoiceLineItem> lineItems
    ) {
        // Complex invoice creation with line items
        // Calculates totals and validates invariants
    }
    
    public Invoice createProrationInvoice(
        Subscription subscription,
        ProrationResult proration
    ) {
        // Creates invoice with proration line items
    }
}
```

**Design Rationale**: Factories ensure that complex objects are created in a valid state with all invariants satisfied. They centralize creation logic and make it testable.

#### Domain Events

Domain events represent significant business occurrences:

**Subscription Events**
- `SubscriptionCreated`
- `SubscriptionUpgraded`
- `SubscriptionDowngraded`
- `SubscriptionCanceled`
- `SubscriptionReactivated`
- `TrialStarted`
- `TrialEnding`
- `TrialEnded`
- `TrialConverted`

**Payment Events**
- `PaymentSucceeded`
- `PaymentFailed`
- `PaymentRefunded`
- `PaymentMethodAdded`
- `PaymentMethodRemoved`

**Invoice Events**
- `InvoiceGenerated`
- `InvoicePaid`
- `InvoiceVoided`

**Usage Events**
- `UsageRecorded`
- `QuotaExceeded`
- `QuotaWarning`

**Design Rationale**: Domain events enable loose coupling between aggregates and support eventual consistency. They trigger side effects (notifications, integrations) without coupling aggregates directly.

#### Anti-Corruption Layer

The billing service uses anti-corruption layers to isolate the domain model from external systems:

**PaymentProviderAdapter** - Translates between domain payment models and provider-specific APIs (Stripe, PayPal)
**UserServiceAdapter** - Translates between domain tenant/user models and User Service APIs
**EmailServiceAdapter** - Translates between domain notification models and Email Service APIs

**Design Rationale**: Anti-corruption layers protect the domain model from external API changes and allow us to swap implementations without affecting business logic.

### Service Boundaries

**Billing Service Responsibilities:**
- Subscription lifecycle management (create, update, cancel, reactivate)
- Payment method management and tokenization
- Payment processing and retry logic
- Invoice generation and management
- Usage tracking and quota enforcement
- Proration calculations
- Webhook processing from payment providers
- Billing analytics and reporting

**External Service Dependencies:**
- **User Service**: Tenant and user information, subscription status updates
- **Gateway Service**: Request routing, feature access control, quota enforcement
- **Email Service**: Billing notifications and invoice delivery
- **Payment Providers**: Payment processing, tokenization, webhook events

### Multi-Tenancy Strategy

The service implements schema-per-tenant multi-tenancy:

1. **Public Schema**: Stores subscription plans (shared across all tenants)
2. **Tenant Schemas**: Each tenant has a dedicated schema containing:
   - Subscriptions
   - Usage records
   - Invoices
   - Payments
   - Payment methods
   - Billing events

3. **Tenant Context Resolution**: 
   - Extract tenant ID from JWT token in request
   - Set Hibernate tenant identifier for current request
   - All database operations automatically scoped to tenant schema

---

## Security Architecture

### Authentication and Authorization

The billing service implements strict security following the same patterns as the user service to ensure consistency across the platform.

#### JWT Token Validation

**JWT Configuration**:
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    @Value("${iqscaffold.billing.security.jwt.jwk-set-uri}")
    private String jwkSetUri;
    
    @Bean
    public SecurityFilterChain filterChain(
        HttpSecurity http,
        TenantExtractionFilter tenantExtractionFilter,
        RateLimitingFilter rateLimitingFilter
    ) throws Exception {
        return http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/actuator/**"))
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/v1/billing/plans/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                
                // Integration APIs (require authentication)
                .requestMatchers("/api/v1/billing/subscriptions/*/status").authenticated()
                .requestMatchers("/api/v1/billing/subscriptions/*/check-feature").authenticated()
                .requestMatchers("/api/v1/billing/usage/**").authenticated()
                
                // Customer portal (require authentication)
                .requestMatchers("/api/v1/billing/portal/**").authenticated()
                
                // Admin endpoints (require ADMIN or SUPER_ADMIN authority)
                .requestMatchers("/api/v1/admin/billing/**")
                    .hasAnyAuthority("ADMIN", "SUPER_ADMIN")
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> 
                oauth2.jwt(jwt -> jwt.decoder(jwtDecoder())))
            .addFilterBefore(tenantExtractionFilter, 
                UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(rateLimitingFilter, 
                UsernamePasswordAuthenticationFilter.class)
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.deny())
                .httpStrictTransportSecurity(hsts -> 
                    hsts.maxAgeInSeconds(31536000).includeSubDomains(true))
            )
            .build();
    }
    
    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }
}
```

**Design Rationale**: Using OAuth2 Resource Server with JWT validation ensures that all requests are authenticated using tokens issued by the User Service. The JWK endpoint provides public keys for token verification without sharing secrets.

#### JWT Claims Extraction

**JWT Claim Names**:
```java
public final class JwtClaimNames {
    public static final String SUBJECT = "sub";
    public static final String USERNAME = "username";
    public static final String EMAIL = "email";
    public static final String ROLES = "roles"; // Contains authorities
    public static final String TENANT_ID = "tenant_id";
    public static final String FIRST_NAME = "firstName";
    public static final String LAST_NAME = "lastName";
    
    private JwtClaimNames() {
        throw new UnsupportedOperationException("Utility class");
    }
}
```

**User Context**:
```java
public record UserContext(
    Long userId,
    String username,
    String email,
    Set<String> authorities,
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
    
    public boolean isTenantOwner() {
        return hasAuthority("TENANT_OWNER");
    }
    
    public boolean canManageBilling() {
        return isAdmin() || isTenantOwner() || hasAuthority("BILLING_ADMIN");
    }
}
```

**JWT Utilities**:
```java
@Component
public class JwtUtils {
    
    public static UserContext extractUserContext(Jwt jwt) {
        Long userId = jwt.getClaim(JwtClaimNames.SUBJECT);
        String username = jwt.getClaim(JwtClaimNames.USERNAME);
        String email = jwt.getClaim(JwtClaimNames.EMAIL);
        List<String> roles = jwt.getClaim(JwtClaimNames.ROLES);
        String firstName = jwt.getClaim(JwtClaimNames.FIRST_NAME);
        String lastName = jwt.getClaim(JwtClaimNames.LAST_NAME);
        String tenantId = jwt.getClaim(JwtClaimNames.TENANT_ID);
        
        if (tenantId == null) {
            throw new AuthenticationException("Missing tenant_id claim in JWT");
        }
        
        Set<String> authorities = roles != null 
            ? new HashSet<>(roles) 
            : Collections.emptySet();
        
        return new UserContext(
            userId, username, email, authorities, 
            firstName, lastName, tenantId
        );
    }
}
```

**Design Rationale**: Extracting user context from JWT claims provides all necessary information for authorization decisions and audit logging without additional database queries.

#### Tenant Context Management

**Tenant Context**:
```java
public final class TenantContext {
    
    private static final ThreadLocal<String> currentTenantId = new ThreadLocal<>();
    
    private TenantContext() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    public static void setCurrentTenantId(String tenantId) {
        currentTenantId.set(tenantId);
    }
    
    public static String getCurrentTenantId() {
        String tenantId = currentTenantId.get();
        if (tenantId == null) {
            throw new IllegalStateException("No tenant context set");
        }
        return tenantId;
    }
    
    public static String getCurrentTenantIdOrDefault() {
        return currentTenantId.get();
    }
    
    public static boolean hasTenantContext() {
        return currentTenantId.get() != null;
    }
    
    public static void clear() {
        currentTenantId.remove();
    }
    
    public static <T> T executeInTenantContext(String tenantId, Supplier<T> operation) {
        String previousTenantId = getCurrentTenantIdOrDefault();
        try {
            setCurrentTenantId(tenantId);
            return operation.get();
        } finally {
            if (previousTenantId != null) {
                setCurrentTenantId(previousTenantId);
            } else {
                clear();
            }
        }
    }
    
    public static void executeInTenantContext(String tenantId, Runnable operation) {
        executeInTenantContext(tenantId, () -> {
            operation.run();
            return null;
        });
    }
}
```

**Tenant Extraction Filter**:
```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TenantExtractionFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(TenantExtractionFilter.class);
    
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();
            
            if (authentication instanceof JwtAuthenticationToken token) {
                Jwt jwt = token.getToken();
                String tenantId = jwt.getClaimAsString(JwtClaimNames.TENANT_ID);
                
                if (tenantId != null) {
                    TenantContext.setCurrentTenantId(tenantId);
                    MDC.put(BillingConstants.MDC.TENANT_ID, tenantId);
                    
                    // Validate tenant ID in path matches JWT tenant ID
                    validateTenantIdInPath(request, tenantId);
                }
            }
            
            filterChain.doFilter(request, response);
            
        } finally {
            TenantContext.clear();
            MDC.remove(BillingConstants.MDC.TENANT_ID);
        }
    }
    
    private void validateTenantIdInPath(HttpServletRequest request, String jwtTenantId) {
        String path = request.getRequestURI();
        
        // Extract tenant ID from path patterns like /api/v1/billing/subscriptions/{tenantId}/...
        Pattern pattern = Pattern.compile("/subscriptions/([^/]+)/");
        Matcher matcher = pattern.matcher(path);
        
        if (matcher.find()) {
            String pathTenantId = matcher.group(1);
            if (!jwtTenantId.equals(pathTenantId)) {
                throw new AccessDeniedException(
                    "Tenant ID in path does not match authenticated tenant"
                );
            }
        }
    }
}
```

**Hibernate Multi-Tenancy Integration**:
```java
@Component
public class CurrentTenantIdentifierResolverImpl 
    implements CurrentTenantIdentifierResolver {
    
    @Override
    public String resolveCurrentTenantIdentifier() {
        String tenantId = TenantContext.getCurrentTenantIdOrDefault();
        return tenantId != null ? tenantId : "public";
    }
    
    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
```

**Design Rationale**: ThreadLocal-based tenant context ensures that all database operations within a request are automatically scoped to the correct tenant schema. The filter extracts tenant ID from JWT and validates it against path parameters to prevent cross-tenant access.

#### Rate Limiting

**Rate Limiting Filter**:
```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RateLimitingFilter extends OncePerRequestFilter {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final BillingProperties billingProperties;
    
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        
        if (!billingProperties.security().rateLimiting().enabled()) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String tenantId = TenantContext.getCurrentTenantIdOrDefault();
        if (tenantId == null) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String key = "rate-limit:billing:" + tenantId;
        Long requests = redisTemplate.opsForValue().increment(key);
        
        if (requests == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(1));
        }
        
        int limit = billingProperties.security().rateLimiting().requestsPerMinute();
        
        if (requests > limit) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("""
                {
                  "error": "RATE_LIMIT_EXCEEDED",
                  "message": "Too many requests. Please try again later.",
                  "limit": %d,
                  "window": "1 minute"
                }
                """.formatted(limit));
            return;
        }
        
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(limit - requests));
        
        filterChain.doFilter(request, response);
    }
}
```

**Design Rationale**: Redis-based rate limiting provides distributed rate limiting across multiple service instances. Per-tenant rate limiting prevents abuse while allowing legitimate usage.

#### Authorization Patterns

**Method-Level Security**:
```java
@RestController
@RequestMapping("/api/v1/billing/portal")
public class BillingPortalRestResource {
    
    @PostMapping("/subscription/upgrade")
    @PreAuthorize("hasAnyAuthority('TENANT_OWNER', 'BILLING_ADMIN', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<SubscriptionDto> upgradeSubscription(
        @Valid @RequestBody UpgradeRequest request
    ) {
        // Only users with billing management authority can upgrade
    }
    
    @GetMapping("/dashboard")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BillingDashboardDto> getDashboard() {
        // Any authenticated user can view dashboard
    }
}
```

**Service-Level Authorization**:
```java
@Service
public class SubscriptionService {
    
    public void validateBillingAccess(String tenantId) {
        UserContext userContext = getCurrentUserContext();
        
        if (!userContext.tenantId().equals(tenantId) && !userContext.isAdmin()) {
            throw new AccessDeniedException(
                "User does not have access to this tenant's billing information"
            );
        }
    }
    
    public void validateBillingManagement(String tenantId) {
        UserContext userContext = getCurrentUserContext();
        
        if (!userContext.tenantId().equals(tenantId)) {
            throw new AccessDeniedException(
                "User does not belong to this tenant"
            );
        }
        
        if (!userContext.canManageBilling()) {
            throw new AccessDeniedException(
                "User does not have billing management authority"
            );
        }
    }
}
```

**Design Rationale**: Combining method-level and service-level authorization provides defense in depth. Method-level security prevents unauthorized endpoint access, while service-level checks ensure business logic enforces authorization rules.

### Payment Security

#### PCI DSS Compliance

The billing service achieves PCI DSS compliance through payment provider abstraction:

1. **Never Store Card Numbers**: Full credit card numbers are never stored in the database
2. **Tokenization**: Payment providers (Stripe, PayPal) tokenize payment methods
3. **Provider Tokens Only**: Only provider-generated tokens are stored
4. **Display Last 4 Digits**: Only last 4 digits and brand are displayed to users
5. **Secure Transmission**: All payment data transmitted over TLS 1.3

**Payment Method Storage**:
```java
@Entity
@Table(name = "payment_methods")
public class PaymentMethod {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private UUID tenantId;
    private UUID userId;
    
    @Enumerated(EnumType.STRING)
    private PaymentMethodType type;
    
    // Provider token - NOT the actual card number
    private String providerPaymentMethodId;
    
    // Only last 4 digits for display
    private String last4;
    private String brand;
    
    private Integer expiryMonth;
    private Integer expiryYear;
    
    // No full card number stored!
}
```

**Design Rationale**: By delegating payment processing to PCI-compliant providers and never storing sensitive payment data, the billing service reduces PCI DSS scope and security risk.

#### Webhook Security

**Webhook Signature Verification**:
```java
@RestController
@RequestMapping("/api/v1/billing/webhooks")
public class WebhookRestResource {
    
    private final WebhookService webhookService;
    
    @PostMapping("/stripe")
    public ResponseEntity<Void> handleStripeWebhook(
        @RequestBody String payload,
        @RequestHeader("Stripe-Signature") String signature
    ) {
        if (!webhookService.verifyStripeSignature(payload, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        webhookService.processStripeWebhook(payload);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/paypal")
    public ResponseEntity<Void> handlePayPalWebhook(
        @RequestBody String payload,
        @RequestHeader("PAYPAL-TRANSMISSION-ID") String transmissionId,
        @RequestHeader("PAYPAL-TRANSMISSION-TIME") String transmissionTime,
        @RequestHeader("PAYPAL-TRANSMISSION-SIG") String signature
    ) {
        if (!webhookService.verifyPayPalSignature(
            payload, transmissionId, transmissionTime, signature
        )) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        webhookService.processPayPalWebhook(payload);
        return ResponseEntity.ok().build();
    }
}
```

**Design Rationale**: Webhook signature verification ensures that webhook events are genuinely from the payment provider and haven't been tampered with, preventing malicious webhook injection attacks.

#### Idempotency

**Idempotency Key Handling**:
```java
@Service
public class PaymentService {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    public Payment processPayment(
        Invoice invoice,
        PaymentMethod paymentMethod,
        String idempotencyKey
    ) {
        String key = "payment:idempotency:" + idempotencyKey;
        
        // Check if payment already processed
        String existingPaymentId = redisTemplate.opsForValue().get(key);
        if (existingPaymentId != null) {
            return paymentRepository.findById(Long.parseLong(existingPaymentId))
                .orElseThrow(() -> new PaymentNotFoundException(
                    "Payment not found: " + existingPaymentId
                ));
        }
        
        // Process payment
        Payment payment = doProcessPayment(invoice, paymentMethod);
        
        // Store idempotency key
        redisTemplate.opsForValue().set(
            key,
            payment.getId().toString(),
            Duration.ofHours(24)
        );
        
        return payment;
    }
}
```

**Design Rationale**: Idempotency keys prevent duplicate charges when requests are retried due to network issues or client errors. Redis provides fast, distributed idempotency checking across service instances.

### Data Protection

#### Encryption

**At Rest**: PostgreSQL transparent data encryption for sensitive fields
**In Transit**: TLS 1.3 for all API communication
**Secrets Management**: Environment variables for API keys and secrets

#### Audit Logging

**Audit Log Entity**:
```java
@Entity
@Table(name = "billing_audit_log")
public class BillingAuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private UUID tenantId;
    private UUID userId;
    
    private String operation; // e.g., "SUBSCRIPTION_UPGRADED"
    private String entityType; // e.g., "SUBSCRIPTION"
    private Long entityId;
    
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> changes;
    
    private String ipAddress;
    private String userAgent;
    
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    // Immutable - no update or delete methods
}
```

**Audit Logging Service**:
```java
@Service
public class AuditLogService {
    
    public void logSubscriptionChange(
        Subscription subscription,
        String operation,
        Map<String, Object> changes
    ) {
        BillingAuditLog log = new BillingAuditLog();
        log.setTenantId(subscription.getTenantId());
        log.setUserId(getCurrentUserId());
        log.setOperation(operation);
        log.setEntityType("SUBSCRIPTION");
        log.setEntityId(subscription.getId());
        log.setChanges(changes);
        log.setIpAddress(getCurrentIpAddress());
        log.setUserAgent(getCurrentUserAgent());
        log.setCreatedAt(LocalDateTime.now());
        
        auditLogRepository.save(log);
    }
}
```

**Design Rationale**: Immutable audit logs provide a complete history of all billing operations for compliance, debugging, and security investigations. JSONB storage allows flexible change tracking without schema modifications.

---

## Technical Implementation Standards

### Service Structure and Organization

The billing service follows domain-driven design principles with a clear package structure organized by business domain:

```
iqscaffold-billing-service/
├── subscription/              # Subscription lifecycle management
├── plan/                      # Subscription plan management
├── usage/                     # Usage tracking & metering
├── payment/                   # Payment processing
├── paymentmethod/             # Payment method management
├── invoice/                   # Invoice generation & management
├── billing/                   # Billing cycles & charges
├── portal/                    # Customer billing portal
├── webhook/                   # Payment provider webhooks
├── analytics/                 # Billing analytics (admin)
├── config/                    # Configuration
├── security/                  # Security & validation
└── shared/                    # Shared utilities
    ├── exception/             # Custom exceptions
    ├── BillingConstants.java  # Centralized constants
    ├── MessageService.java    # i18n message service
    └── BillingAuditLog.java   # Audit logging
```

**Design Rationale**: This structure provides clear separation of concerns, making the codebase maintainable and allowing teams to work on different domains independently. Each package contains its entity, repository, service, REST resource, and DTOs, following the established platform patterns.

### Configuration Management

**Configuration Properties Pattern**:
- Use `@ConfigurationProperties` with Java records for type-safe configuration
- Prefix all custom properties with `iqscaffold.billing.`
- Apply Jakarta validation annotations for configuration validation
- Generate `spring-configuration-metadata.json` for IDE support

**Configuration Structure**:
```yaml
iqscaffold:
  billing:
    payment:
      provider: stripe
      stripe:
        api-key: ${STRIPE_API_KEY}
        webhook-secret: ${STRIPE_WEBHOOK_SECRET}
    subscription:
      default-currency: USD
      trial-days: 14
      grace-period-days: 3
    usage:
      metering-enabled: true
      batch-size: 100
      flush-interval: PT30S
    invoice:
      number-format: "INV-{YEAR}{MONTH}-{SEQUENCE}"
      due-days: 7
      auto-finalize: true
    portal:
      enabled: true
      allow-plan-changes: true
    features:
      proration: true
      dunning: true
      analytics: true
```

**Design Rationale**: Type-safe configuration with validation prevents runtime errors from misconfiguration. The hierarchical structure makes configuration intuitive and environment variables provide secure handling of sensitive data.

### REST API Standards

**Controller Naming**: All REST controllers use the `-RestResource` suffix (e.g., `SubscriptionRestResource`, `InvoiceRestResource`)

**OpenAPI Documentation**: Comprehensive API documentation using SpringDoc annotations:
- `@Tag` for controller-level grouping
- `@Operation` with detailed descriptions using text blocks
- `@ApiResponses` documenting all response codes
- `@Schema` with examples for request/response models
- Organized into logical groups (subscriptions, payments, invoices, usage, webhooks, admin)

**Design Rationale**: Consistent naming and comprehensive documentation improve developer experience and API discoverability. Text blocks (Java 21) make multi-line descriptions readable in code.

### Database Migration Strategy

**Liquibase Organization**:
- **System changesets**: `db/changelog/system/` for public schema (subscription plans)
- **Tenant changesets**: `db/changelog/tenant/` for tenant-scoped tables
- **Naming convention**: 
  - System: `00000000000000-descriptive-name.xml`
  - Tenant: `YYYYMMDDHHMMSS-descriptive-name.xml`

**Migration Patterns**:
- Separate changesets for table creation, indexes, constraints, and seed data
- Descriptive comments in each changeset
- Rollback blocks where applicable
- Foreign key constraints with explicit `onDelete` behavior

**Design Rationale**: Separate system and tenant migrations support multi-tenancy. Timestamp-based naming for tenant migrations ensures proper ordering across environments. Granular changesets enable targeted rollbacks.

### Constants and String Literals

**Centralized Constants**: All repeatable strings centralized in `BillingConstants.java`:
- HTTP headers (`Headers.X_TENANT_ID`, `Headers.X_IDEMPOTENCY_KEY`)
- MDC keys for logging (`MDC.TENANT_ID`, `MDC.SUBSCRIPTION_ID`)
- Event types (`BillingEvents.SUBSCRIPTION_CREATED`, `BillingEvents.PAYMENT_FAILED`)
- Status values (`SubscriptionStatus.ACTIVE`, `SubscriptionStatus.TRIAL`)
- Error codes (`ErrorCodes.QUOTA_EXCEEDED`, `ErrorCodes.PAYMENT_FAILED`)
- Cache names (`CacheNames.SUBSCRIPTIONS`, `CacheNames.USAGE_QUOTAS`)
- Default values (`Defaults.CURRENCY`, `Defaults.TRIAL_DAYS`)

**Design Rationale**: Centralized constants eliminate magic strings, reduce typos, enable IDE refactoring, and provide a single source of truth for shared values.

### Exception Handling

**Custom Exception Hierarchy**:
```
BillingException (base)
├── SubscriptionException
│   ├── SubscriptionNotFoundException
│   ├── SubscriptionAlreadyExistsException
│   └── InvalidSubscriptionStateException
├── PaymentException
│   ├── PaymentFailedException (with paymentId, reason)
│   ├── PaymentMethodNotFoundException
│   └── InvalidPaymentMethodException
├── UsageException
│   ├── QuotaExceededException (with metricType, limit, used)
│   └── UsageLimitExceededException
├── PlanException
│   ├── PlanNotFoundException
│   └── InvalidPlanTransitionException (with fromPlan, toPlan)
└── InvoiceException
    ├── InvoiceNotFoundException
    └── InvoiceAlreadyPaidException
```

**HTTP Status Mapping**:
- `NotFoundException` → 404
- `AlreadyExistsException` → 409
- `QuotaExceededException` → 429
- `PaymentFailedException` → 402
- `InvalidStateException` → 400

**Design Rationale**: Domain-specific exceptions provide precise error handling and meaningful error messages. Context fields (paymentId, limits) enable detailed error responses. Nested static classes group related exceptions logically.

### Internationalization (i18n)

**Message Management**:
- Message properties in `src/main/resources/i18n/messages.properties`
- Support for English (default), Spanish, French
- `MessageService` for convenient message retrieval
- Locale resolution from user preferences, Accept-Language header, or default

**Message Key Organization**:
```properties
# Pattern: {category}.{entity}.{action}
subscription.created=Subscription created successfully
subscription.upgraded=Subscription upgraded to {0}
payment.failed=Payment failed: {0}
usage.quota.exceeded={0} quota exceeded. Limit: {1}, Used: {2}
email.subscription.created.subject=Welcome to {0} Plan
```

**Design Rationale**: Full i18n support enables global customer base. Parameterized messages support dynamic content. Organized message keys improve maintainability. Caching (1 hour) optimizes performance.

### Java 21 Modern Features

**Records for DTOs**:
```java
public record SubscriptionDto(
    Long id,
    String tenantId,
    String planCode,
    String status,
    LocalDateTime currentPeriodEnd
) {}
```

**Text Blocks for Multi-line Strings**:
```java
@Query("""
    SELECT s FROM Subscription s
    WHERE s.tenantId = :tenantId
      AND s.status = :status
    ORDER BY s.createdAt DESC
    """)
```

**Pattern Matching**:
```java
if (authentication instanceof JwtAuthenticationToken token) {
    String userId = token.getToken().getSubject();
}
```

**Switch Expressions**:
```java
String statusMessage = switch (subscription.getStatus()) {
    case TRIAL -> "Trial period active";
    case ACTIVE -> "Subscription active";
    case PAST_DUE -> "Payment overdue";
    default -> "Unknown status";
};
```

**Sealed Classes for Type Hierarchies**:
```java
public sealed interface PaymentProvider 
    permits StripePaymentProvider, PayPalPaymentProvider, ManualPaymentProvider {
    PaymentResult processPayment(PaymentRequest request);
}
```

**Design Rationale**: Modern Java features reduce boilerplate, improve readability, and provide compile-time safety. Records eliminate DTO boilerplate. Text blocks make multi-line strings maintainable. Pattern matching reduces casting. Switch expressions eliminate break statements. Sealed classes restrict type hierarchies for better domain modeling.

---

## Components and Interfaces

### Core Domain Entities

#### SubscriptionPlan (Public Schema)
```java
@Entity
@Table(name = "subscription_plans", schema = "public")
public class SubscriptionPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String planCode;
    
    private String name;
    private String description;
    
    @Enumerated(EnumType.STRING)
    private PlanTier tier; // FREE, PRO, ENTERPRISE
    
    @Enumerated(EnumType.STRING)
    private BillingCycle billingCycle; // MONTHLY, YEARLY, LIFETIME
    
    private BigDecimal basePrice;
    private String currency;
    
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> features;
    
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private PlanQuotas quotas;
    
    private Integer trialDays;
    private Boolean active;
    private Boolean publicPlan;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

#### Subscription (Tenant Schema)
```java
@Entity
@Table(name = "subscriptions")
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private UUID tenantId;
    
    @Column(nullable = false)
    private UUID userId;
    
    @ManyToOne
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;
    
    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status; // TRIAL, ACTIVE, PAST_DUE, CANCELED, EXPIRED, SUSPENDED, INCOMPLETE
    
    private LocalDateTime currentPeriodStart;
    private LocalDateTime currentPeriodEnd;
    private LocalDateTime trialStart;
    private LocalDateTime trialEnd;
    private LocalDateTime canceledAt;
    private Boolean cancelAtPeriodEnd;
    
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

#### Invoice (Tenant Schema)
```java
@Entity
@Table(name = "invoices")
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;
    
    @Column(nullable = false)
    private UUID tenantId;
    
    @Column(unique = true, nullable = false)
    private String invoiceNumber; // Format: INV-{YEAR}{MONTH}-{SEQUENCE}
    
    @Enumerated(EnumType.STRING)
    private InvoiceStatus status; // DRAFT, OPEN, PAID, VOID, UNCOLLECTIBLE
    
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal total;
    private String currency;
    
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private LocalDateTime dueDate;
    private LocalDateTime paidAt;
    
    @ManyToOne
    @JoinColumn(name = "payment_method_id")
    private PaymentMethod paymentMethod;
    
    private String providerInvoiceId;
    
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private List<InvoiceLineItem> lineItems;
    
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

#### Payment (Tenant Schema)
```java
@Entity
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;
    
    @Column(nullable = false)
    private UUID tenantId;
    
    private BigDecimal amount;
    private String currency;
    
    @Enumerated(EnumType.STRING)
    private PaymentStatus status; // PENDING, SUCCEEDED, FAILED, REFUNDED
    
    @ManyToOne
    @JoinColumn(name = "payment_method_id")
    private PaymentMethod paymentMethod;
    
    private String providerPaymentId;
    private String failureReason;
    
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

#### UsageRecord (Tenant Schema)
```java
@Entity
@Table(name = "usage_records")
public class UsageRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;
    
    @Column(nullable = false)
    private UUID tenantId;
    
    @Enumerated(EnumType.STRING)
    private MetricType metricType; // API_CALLS, STORAGE_GB, ACTIVE_USERS, CUSTOM
    
    private Long quantity;
    private String unit;
    
    private LocalDateTime recordedAt;
    private LocalDateTime billingPeriodStart;
    private LocalDateTime billingPeriodEnd;
    
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;
    
    private LocalDateTime createdAt;
}
```

#### PaymentMethod (Tenant Schema)
```java
@Entity
@Table(name = "payment_methods")
public class PaymentMethod {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private UUID tenantId;
    
    @Column(nullable = false)
    private UUID userId;
    
    @Enumerated(EnumType.STRING)
    private PaymentMethodType type; // CARD, BANK_ACCOUNT, PAYPAL
    
    private String providerPaymentMethodId;
    private String last4;
    private String brand;
    private Integer expiryMonth;
    private Integer expiryYear;
    
    private Boolean isDefault;
    private Boolean active;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### Service Layer Interfaces

#### SubscriptionService
```java
public interface SubscriptionService {
    // Subscription Creation
    Subscription createSubscription(UUID tenantId, UUID userId, String planCode);
    
    // Subscription Lifecycle
    Subscription upgradeSubscription(Long subscriptionId, String newPlanCode);
    Subscription downgradeSubscription(Long subscriptionId, String newPlanCode, boolean immediate);
    Subscription cancelSubscription(Long subscriptionId, boolean immediate, String reason);
    Subscription reactivateSubscription(Long subscriptionId);
    
    // Trial Management
    void extendTrial(Long subscriptionId, int additionalDays);
    void convertTrialToActive(Long subscriptionId);
    
    // Status Management
    void updateSubscriptionStatus(Long subscriptionId, SubscriptionStatus newStatus);
    
    // Queries
    Subscription getSubscription(Long subscriptionId);
    Subscription getActiveSubscriptionForTenant(UUID tenantId);
    List<Subscription> getSubscriptionsByStatus(SubscriptionStatus status);
    
    // Proration
    ProrationResult calculateProration(Subscription current, SubscriptionPlan newPlan);
}
```

#### PaymentService
```java
public interface PaymentService {
    // Payment Method Management
    PaymentMethod addPaymentMethod(UUID tenantId, UUID userId, PaymentMethodRequest request);
    void removePaymentMethod(Long paymentMethodId);
    PaymentMethod setDefaultPaymentMethod(Long paymentMethodId);
    List<PaymentMethod> getPaymentMethods(UUID tenantId);
    
    // Payment Processing
    Payment processPayment(Invoice invoice, PaymentMethod paymentMethod);
    Payment retryPayment(Long paymentId);
    Payment refundPayment(Long paymentId, BigDecimal amount, String reason);
    
    // Payment Status
    void updatePaymentStatus(Long paymentId, PaymentStatus status, String failureReason);
    
    // Queries
    Payment getPayment(Long paymentId);
    List<Payment> getPaymentsByInvoice(Long invoiceId);
}
```

#### InvoiceService
```java
public interface InvoiceService {
    // Invoice Generation
    Invoice generateInvoice(Subscription subscription);
    Invoice generateProrationInvoice(Subscription subscription, ProrationResult proration);
    
    // Invoice Management
    Invoice finalizeInvoice(Long invoiceId);
    void voidInvoice(Long invoiceId, String reason);
    byte[] generateInvoicePdf(Long invoiceId);
    
    // Invoice Payment
    void markInvoiceAsPaid(Long invoiceId, Payment payment);
    void markInvoiceAsFailed(Long invoiceId, String reason);
    
    // Queries
    Invoice getInvoice(Long invoiceId);
    List<Invoice> getInvoicesByTenant(UUID tenantId, InvoiceStatus status, Pageable pageable);
    Invoice getUpcomingInvoice(UUID tenantId);
}
```

#### UsageService
```java
public interface UsageService {
    // Usage Recording
    void recordUsage(UUID tenantId, MetricType metricType, long quantity);
    void recordUsageBatch(List<UsageRecordRequest> records);
    
    // Usage Queries
    UsageSummary getCurrentUsage(UUID tenantId);
    UsageSummary getUsageForPeriod(UUID tenantId, LocalDateTime start, LocalDateTime end);
    Map<MetricType, Long> getUsageByMetric(UUID tenantId, LocalDateTime start, LocalDateTime end);
    
    // Quota Enforcement
    QuotaCheckResult checkQuota(UUID tenantId, MetricType metricType, long requestedQuantity);
    void enforceQuota(UUID tenantId, MetricType metricType) throws QuotaExceededException;
    
    // Usage Reset
    void resetUsageForNewPeriod(UUID tenantId);
}
```

#### PlanService
```java
public interface PlanService {
    // Plan Management
    SubscriptionPlan createPlan(PlanRequest request);
    SubscriptionPlan updatePlan(Long planId, PlanUpdateRequest request);
    void archivePlan(Long planId);
    
    // Plan Queries
    SubscriptionPlan getPlan(Long planId);
    SubscriptionPlan getPlanByCode(String planCode);
    List<SubscriptionPlan> getPublicPlans(BillingCycle billingCycle);
    List<SubscriptionPlan> getAllPlans();
    
    // Plan Validation
    boolean canTransitionToP
lan(SubscriptionPlan from, SubscriptionPlan to);
}
```

#### WebhookService
```java
public interface WebhookService {
    // Webhook Processing
    void processStripeWebhook(String payload, String signature);
    void processPayPalWebhook(String payload, String signature);
    
    // Event Handlers
    void handlePaymentSucceeded(String providerPaymentId);
    void handlePaymentFailed(String providerPaymentId, String reason);
    void handleSubscriptionUpdated(String providerSubscriptionId);
    void handleSubscriptionDeleted(String providerSubscriptionId);
    void handleInvoicePaymentSucceeded(String providerInvoiceId);
    void handleInvoicePaymentFailed(String providerInvoiceId);
    
    // Webhook Logging
    void logWebhookEvent(String provider, String eventType, String payload);
}
```

### Payment Provider Abstraction

#### PaymentProviderAdapter Interface
```java
public interface PaymentProviderAdapter {
    // Provider Identification
    String getProviderName();
    
    // Payment Method Management
    String createPaymentMethod(PaymentMethodRequest request);
    void deletePaymentMethod(String providerPaymentMethodId);
    PaymentMethodDetails getPaymentMethodDetails(String providerPaymentMethodId);
    
    // Payment Processing
    PaymentResult processPayment(BigDecimal amount, String currency, String paymentMethodId);
    PaymentResult refundPayment(String providerPaymentId, BigDecimal amount);
    
    // Customer Management
    String createCustomer(UUID tenantId, String email);
    void updateCustomer(String providerCustomerId, CustomerUpdateRequest request);
    
    // Subscription Sync (Optional)
    void syncSubscription(Subscription subscription);
    
    // Webhook Verification
    boolean verifyWebhookSignature(String payload, String signature);
}
```

#### Implementations
- **StripePaymentAdapter**: Implements Stripe-specific payment processing
- **PayPalPaymentAdapter**: Implements PayPal-specific payment processing
- **ManualPaymentAdapter**: Implements manual/offline payment processing for enterprise contracts

---

## Data Models

### Database Schema Design

#### Public Schema Tables

**subscription_plans**
```sql
CREATE TABLE public.subscription_plans (
    id BIGSERIAL PRIMARY KEY,
    plan_code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    tier VARCHAR(20) NOT NULL, -- FREE, PRO, ENTERPRISE
    billing_cycle VARCHAR(20) NOT NULL, -- MONTHLY, YEARLY, LIFETIME
    base_price DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    features JSONB,
    quotas JSONB,
    trial_days INTEGER DEFAULT 0,
    active BOOLEAN DEFAULT true,
    public_plan BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_plans_tier ON public.subscription_plans(tier);
CREATE INDEX idx_plans_active ON public.subscription_plans(active);
```

#### Tenant Schema Tables

**subscriptions**
```sql
CREATE TABLE subscriptions (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL,
    user_id UUID NOT NULL,
    plan_id BIGINT NOT NULL REFERENCES public.subscription_plans(id),
    status VARCHAR(20) NOT NULL,
    current_period_start TIMESTAMP,
    current_period_end TIMESTAMP,
    trial_start TIMESTAMP,
    trial_end TIMESTAMP,
    canceled_at TIMESTAMP,
    cancel_at_period_end BOOLEAN DEFAULT false,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_subscriptions_tenant ON subscriptions(tenant_id);
CREATE INDEX idx_subscriptions_status ON subscriptions(status);
CREATE INDEX idx_subscriptions_period_end ON subscriptions(current_period_end);
```

**invoices**
```sql
CREATE TABLE invoices (
    id BIGSERIAL PRIMARY KEY,
    subscription_id BIGINT REFERENCES subscriptions(id),
    tenant_id UUID NOT NULL,
    invoice_number VARCHAR(50) UNIQUE NOT NULL,
    status VARCHAR(20) NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    tax DECIMAL(10,2) NOT NULL DEFAULT 0,
    total DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    period_start TIMESTAMP NOT NULL,
    period_end TIMESTAMP NOT NULL,
    due_date TIMESTAMP NOT NULL,
    paid_at TIMESTAMP,
    payment_method_id BIGINT REFERENCES payment_methods(id),
    provider_invoice_id VARCHAR(255),
    line_items JSONB NOT NULL,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_invoices_tenant ON invoices(tenant_id);
CREATE INDEX idx_invoices_subscription ON invoices(subscription_id);
CREATE INDEX idx_invoices_status ON invoices(status);
CREATE INDEX idx_invoices_due_date ON invoices(due_date);
```

**payments**
```sql
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT REFERENCES invoices(id),
    tenant_id UUID NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    payment_method_id BIGINT REFERENCES payment_methods(id),
    provider_payment_id VARCHAR(255),
    failure_reason TEXT,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payments_invoice ON payments(invoice_id);
CREATE INDEX idx_payments_tenant ON payments(tenant_id);
CREATE INDEX idx_payments_status ON payments(status);
```

**usage_records**
```sql
CREATE TABLE usage_records (
    id BIGSERIAL PRIMARY KEY,
    subscription_id BIGINT REFERENCES subscriptions(id),
    tenant_id UUID NOT NULL,
    metric_type VARCHAR(50) NOT NULL,
    quantity BIGINT NOT NULL,
    unit VARCHAR(20),
    recorded_at TIMESTAMP NOT NULL,
    billing_period_start TIMESTAMP NOT NULL,
    billing_period_end TIMESTAMP NOT NULL,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_usage_tenant ON usage_records(tenant_id);
CREATE INDEX idx_usage_subscription ON usage_records(subscription_id);
CREATE INDEX idx_usage_period ON usage_records(billing_period_start, billing_period_end);
CREATE INDEX idx_usage_metric ON usage_records(metric_type);
```

**payment_methods**
```sql
CREATE TABLE payment_methods (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL,
    user_id UUID NOT NULL,
    type VARCHAR(20) NOT NULL,
    provider_payment_method_id VARCHAR(255) NOT NULL,
    last4 VARCHAR(4),
    brand VARCHAR(50),
    expiry_month INTEGER,
    expiry_year INTEGER,
    is_default BOOLEAN DEFAULT false,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payment_methods_tenant ON payment_methods(tenant_id);
CREATE INDEX idx_payment_methods_default ON payment_methods(is_default);
```

**billing_events**
```sql
CREATE TABLE billing_events (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL,
    subscription_id BIGINT REFERENCES subscriptions(id),
    event_type VARCHAR(50) NOT NULL,
    event_data JSONB NOT NULL,
    processed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_billing_events_tenant ON billing_events(tenant_id);
CREATE INDEX idx_billing_events_subscription ON billing_events(subscription_id);
CREATE INDEX idx_billing_events_type ON billing_events(event_type);
CREATE INDEX idx_billing_events_created ON billing_events(created_at);
```

### Value Objects and DTOs

#### PlanQuotas
```java
public class PlanQuotas {
    private Integer maxUsers;
    private Integer storageGb;
    private Long apiCallsPerMonth;
    private Map<String, Long> customMetrics;
}
```

#### InvoiceLineItem
```java
public class InvoiceLineItem {
    private String description;
    private LineItemType type; // SUBSCRIPTION_FEE, USAGE_CHARGE, PRORATION_CREDIT, PRORATION_CHARGE, DISCOUNT, TAX
    private Long quantity;
    private BigDecimal unitPrice;
    private BigDecimal amount;
}
```

#### ProrationResult
```java
public class ProrationResult {
    private BigDecimal creditAmount;
    private BigDecimal chargeAmount;
    private BigDecimal netAmount;
    private int daysRemaining;
    private int daysInPeriod;
    private String description;
}
```

#### UsageSummary
```java
public class UsageSummary {
    private UUID tenantId;
    private Map<MetricType, UsageMetric> metrics;
    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
}

public class UsageMetric {
    private MetricType type;
    private Long used;
    private Long limit;
    private Double percentageUsed;
    private LocalDateTime resetDate;
}
```

#### QuotaCheckResult
```java
public class QuotaCheckResult {
    private boolean allowed;
    private Long currentUsage;
    private Long limit;
    private Long remaining;
    private String message;
}
```

---

## Error Handling

### Exception Hierarchy

```java
// Base Exception
public class BillingException extends RuntimeException {
    private final String errorCode;
    private final Map<String, Object> details;
}

// Specific Exceptions
public class SubscriptionNotFoundException extends BillingException {}
public class SubscriptionAlreadyExistsException extends BillingException {}
public class PlanNotFoundException extends BillingException {}
public class InvalidPlanTransitionException extends BillingException {}
public class QuotaExceededException extends BillingException {}
public class FeatureNotAvailableException extends BillingException {}
public class PaymentRequiredException extends BillingException {}
public class PaymentFailedException extends BillingException {}
public class InvoiceNotFoundException extends BillingException {}
public class InvalidPaymentMethodException extends BillingException {}
public class UsageLimitExceededException extends BillingException {}
```

### Error Response Format

```json
{
  "error": "QUOTA_EXCEEDED",
  "message": "API call quota exceeded for current billing period",
  "timestamp": "2024-12-05T10:30:00Z",
  "path": "/api/v1/some-endpoint",
  "details": {
    "limit": 10000,
    "used": 10000,
    "percentage": 100.0,
    "resetDate": "2024-12-31T23:59:59Z",
    "upgradeUrl": "/billing/plans"
  }
}
```

### Global Exception Handler

```java
@RestControllerAdvice
public class BillingExceptionHandler {
    
    @ExceptionHandler(SubscriptionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSubscriptionNotFound(SubscriptionNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("SUBSCRIPTION_NOT_FOUND", ex.getMessage(), ex.getDetails()));
    }
    
    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<ErrorResponse> handleQuotaExceeded(QuotaExceededException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .body(new ErrorResponse("QUOTA_EXCEEDED", ex.getMessage(), ex.getDetails()));
    }
    
    @ExceptionHandler(PaymentFailedException.class)
    public ResponseEntity<ErrorResponse> handlePaymentFailed(PaymentFailedException ex) {
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
            .body(new ErrorResponse("PAYMENT_FAILED", ex.getMessage(), ex.getDetails()));
    }
    
    // Additional handlers for other exceptions...
}
```

---

## Testing Strategy

### Unit Testing

Unit tests will verify individual components in isolation using mocks for dependencies.

**Coverage Requirements:**
- Minimum 80% code coverage
- 100% coverage for financial calculations (proration, invoice totals, tax calculations)
- 100% coverage for state transition logic

**Key Areas:**
- Service layer business logic
- Proration calculations
- Subscription state transitions
- Quota enforcement logic
- Payment processing logic
- Invoice generation logic
- Usage aggregation

**Testing Framework:**
- JUnit 5 for test execution
- Mockito for mocking dependencies
- AssertJ for fluent assertions

**Example Unit Test:**
```java
@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {
    
    @Mock
    private SubscriptionRepository subscriptionRepository;
    
    @Mock
    private PlanService planService;
    
    @Mock
    private InvoiceService invoiceService;
    
    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;
    
    @Test
    void shouldCalculateProrationCorrectly() {
        // Given
        SubscriptionPlan currentPlan = createPlan("PRO", new BigDecimal("50.00"));
        SubscriptionPlan newPlan = createPlan("ENTERPRISE", new BigDecimal("100.00"));
        Subscription subscription = createSubscription(currentPlan, 15); // 15 days into 30-day period
        
        // When
        ProrationResult result = subscriptionService.calculateProration(subscription, newPlan);
        
        // Then
        assertThat(result.getCreditAmount()).isEqualByComparingTo(new BigDecimal("25.00")); // 15/30 * 50
        assertThat(result.getChargeAmount()).isEqualByComparingTo(new BigDecimal("50.00")); // 15/30 * 100
        assertThat(result.getNetAmount()).isEqualByComparingTo(new BigDecimal("25.00")); // 50 - 25
    }
}
```

### Integration Testing

Integration tests will verify interactions between components using real database and message queue instances.

**Testing Framework:**
- Spring Boot Test
- Testcontainers for PostgreSQL, Redis, and RabbitMQ
- WireMock for mocking payment provider APIs

**Key Scenarios:**
- Complete subscription lifecycle (create → upgrade → cancel)
- Payment processing with retry logic
- Invoice generation and payment
- Usage recording and quota enforcement
- Webhook processing
- Multi-tenant data isolation

**Example Integration Test:**
```java
@SpringBootTest
@Testcontainers
class SubscriptionIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7");
    
    @Autowired
    private SubscriptionService subscriptionService;
    
    @Autowired
    private InvoiceService invoiceService;
    
    @Test
    @Transactional
    void shouldCreateSubscriptionWithTrialAndConvertToActive() {
        // Given
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String planCode = "PRO";
        
        // When - Create subscription with trial
        Subscription subscription = subscriptionService.createSubscription(tenantId, userId, planCode);
        
        // Then - Verify trial status
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.TRIAL);
        assertThat(subscription.getTrialEnd()).isAfter(LocalDateTime.now());
        
        // When - Convert to active
        subscriptionService.convertTrialToActive(subscription.getId());
        
        // Then - Verify active status and invoice generated
        Subscription activeSubscription = subscriptionService.getSubscription(subscription.getId());
        assertThat(activeSubscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        
        List<Invoice> invoices = invoiceService.getInvoicesByTenant(tenantId, null, Pageable.unpaged());
        assertThat(invoices).hasSize(1);
        assertThat(invoices.get(0).getStatus()).isEqualTo(InvoiceStatus.OPEN);
    }
}
```

### Property-Based Testing

Property-based tests will verify universal properties that should hold across all inputs using fast-check or similar library for Java.

**Property Testing Library:** jqwik (Java property-based testing framework)

**Key Properties to Test:**
- Proration calculations are always positive and sum correctly
- Invoice totals always equal sum of line items plus tax
- Usage aggregation is commutative (order doesn't matter)
- Subscription state transitions follow valid paths
- Quota checks are consistent with usage records

**Example Property Test:**
```java
@Property
void prorationShouldAlwaysBePositiveAndSumCorrectly(
    @ForAll @BigRange(min = "10.00", max = "1000.00") BigDecimal oldPrice,
    @ForAll @BigRange(min = "10.00", max = "1000.00") BigDecimal newPrice,
    @ForAll @IntRange(min = 1, max = 30) int daysRemaining,
    @ForAll @IntRange(min = 28, max = 31) int daysInPeriod
) {
    // Given
    SubscriptionPlan oldPlan = createPlan("OLD", oldPrice);
    SubscriptionPlan newPlan = createPlan("NEW", newPrice);
    Subscription subscription = createSubscriptionWithDaysRemaining(oldPlan, daysRemaining, daysInPeriod);
    
    // When
    ProrationResult result = subscriptionService.calculateProration(subscription, newPlan);
    
    // Then
    assertThat(result.getCreditAmount()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    assertThat(result.getChargeAmount()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    
    BigDecimal expectedCredit = oldPrice.multiply(BigDecimal.valueOf(daysRemaining))
        .divide(BigDecimal.valueOf(daysInPeriod), 2, RoundingMode.HALF_UP);
    BigDecimal expectedCharge = newPrice.multiply(BigDecimal.valueOf(daysRemaining))
        .divide(BigDecimal.valueOf(daysInPeriod), 2, RoundingMode.HALF_UP);
    
    assertThat(result.getCreditAmount()).isEqualByComparingTo(expectedCredit);
    assertThat(result.getChargeAmount()).isEqualByComparingTo(expectedCharge);
    assertThat(result.getNetAmount()).isEqualByComparingTo(expectedCharge.subtract(expectedCredit));
}
```

---

## Correctness Properties


*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

Based on the acceptance criteria analysis, the following correctness properties must be upheld by the billing service:

### Property 1: Tenant Isolation for Subscriptions

*For any* two distinct tenants, querying subscriptions for one tenant should never return subscriptions belonging to the other tenant.

**Validates: Requirements 2.2**

### Property 2: Tenant Isolation for Usage Records

*For any* two distinct tenants, querying usage records for one tenant should never return usage records belonging to the other tenant.

**Validates: Requirements 2.3**

### Property 3: Tenant Isolation for Invoices

*For any* two distinct tenants, querying invoices for one tenant should never return invoices belonging to the other tenant.

**Validates: Requirements 2.4**

### Property 4: Tenant Isolation for Payment Methods

*For any* two distinct tenants, querying payment methods for one tenant should never return payment methods belonging to the other tenant.

**Validates: Requirements 2.5**

### Property 5: Tenant Isolation for Billing Events

*For any* two distinct tenants, querying billing events for one tenant should never return billing events belonging to the other tenant.

**Validates: Requirements 2.6**

### Property 6: Plan Creation Persistence

*For any* valid subscription plan with name, code, tier, billing cycle, price, and currency, creating the plan and then retrieving it should return a plan with all the same field values.

**Validates: Requirements 4.1**

### Property 7: Plan Features Round-Trip

*For any* valid JSON feature structure, storing it in a plan's features field and then retrieving the plan should return the same feature structure.

**Validates: Requirements 4.5**

### Property 8: Plan Quotas Round-Trip

*For any* valid quota structure including maxUsers, storageGb, apiCallsPerMonth, and custom metrics, storing it in a plan and then retrieving the plan should return the same quota structure.

**Validates: Requirements 4.6**

### Property 9: Trial Period Persistence

*For any* valid trial period duration in days, setting it on a plan and then retrieving the plan should return the same trial period value.

**Validates: Requirements 4.7**

### Property 10: Plan Visibility Filtering

*For any* plan marked as private, it should not appear in the public plan listing endpoint results.

**Validates: Requirements 4.8**

### Property 11: Plan Update Persistence

*For any* existing plan and valid updates to name, description, features, or quotas, updating the plan and then retrieving it should return the updated values.

**Validates: Requirements 5.1**

### Property 12: Price Grandfathering

*For any* subscription created before a plan price change, the subscription should maintain the original price even after the plan price is updated.

**Validates: Requirements 5.2**

### Property 13: Archived Plan Subscription Prevention

*For any* archived or deactivated plan, attempting to create a new subscription with that plan should fail with an appropriate error.

**Validates: Requirements 5.4**

### Property 14: Public Plan Listing Filtering

*For any* set of plans with various active/inactive and public/private combinations, the public listing endpoint should return only plans that are both active and public.

**Validates: Requirements 6.1**

### Property 15: Plan Listing Completeness

*For any* plan in the listing response, the response should include features, quotas, and pricing information.

**Validates: Requirements 6.2**

### Property 16: Plan Listing Sort Order

*For any* set of plans with different tiers and prices, the listing response should be sorted first by tier (FREE < PRO < ENTERPRISE) and then by price within each tier.

**Validates: Requirements 6.3**

### Property 17: Billing Cycle Filtering

*For any* billing cycle filter value (MONTHLY, YEARLY, LIFETIME), the filtered plan listing should return only plans with that billing cycle.

**Validates: Requirements 6.4**

### Property 18: Admin Plan Listing Completeness

*For any* set of plans including private and archived plans, the admin listing endpoint should return all plans regardless of their active or public status.

**Validates: Requirements 6.5**

### Property 19: Proration Calculation Correctness

*For any* subscription upgrade or downgrade, the proration calculation should satisfy:
- Credit amount = (old plan price × days remaining) / days in period
- Charge amount = (new plan price × days remaining) / days in period
- Net amount = charge amount - credit amount
- All amounts should be non-negative

**Validates: Requirements from detailed technical specifications (subscription upgrades/downgrades)**

### Property 20: Invoice Total Consistency

*For any* invoice, the total should equal the sum of all line item amounts plus tax, and the subtotal should equal the sum of all non-tax line items.

**Validates: Requirements from detailed technical specifications (invoice generation)**

### Property 21: Usage Aggregation Commutativity

*For any* set of usage records for a tenant and metric type, the aggregated total usage should be the same regardless of the order in which the records are processed.

**Validates: Requirements from detailed technical specifications (usage tracking)**

### Property 22: Quota Enforcement Consistency

*For any* tenant and metric type, if the current usage equals or exceeds the quota limit, any operation that would increase usage should be rejected with a QUOTA_EXCEEDED error.

**Validates: Requirements from detailed technical specifications (quota enforcement)**

### Property 23: Subscription Status Transition Validity

*For any* subscription, all status transitions should follow valid state machine paths:
- TRIAL → ACTIVE (trial ends with payment)
- TRIAL → EXPIRED (trial ends without payment)
- ACTIVE → PAST_DUE (payment fails)
- ACTIVE → CANCELED (user cancels)
- PAST_DUE → ACTIVE (payment succeeds)
- PAST_DUE → EXPIRED (all retries fail)
- CANCELED → ACTIVE (reactivation before period end)
- CANCELED → EXPIRED (period ends)

Invalid transitions should be rejected.

**Validates: Requirements from detailed technical specifications (subscription lifecycle)**

### Property 24: Payment Idempotency

*For any* payment request with the same idempotency key, processing the request multiple times should result in only one payment being created and charged.

**Validates: Requirements from detailed technical specifications (payment processing)**

### Property 25: Webhook Idempotency

*For any* webhook event with the same event ID, processing the webhook multiple times should result in the same system state as processing it once.

**Validates: Requirements from detailed technical specifications (webhook handling)**

---

## API Design

### REST Endpoints

#### Public Endpoints

**GET /api/v1/billing/plans**
- Description: List all active, public subscription plans
- Query Parameters:
  - `billingCycle` (optional): Filter by billing cycle (MONTHLY, YEARLY, LIFETIME)
- Response: List of SubscriptionPlanDTO
- Authentication: Not required

**GET /api/v1/billing/plans/{code}**
- Description: Get specific plan details by plan code
- Path Parameters:
  - `code`: Plan code
- Response: SubscriptionPlanDTO
- Authentication: Not required

#### Customer Portal Endpoints

**GET /api/v1/billing/portal/dashboard**
- Description: Get billing dashboard data including subscription, usage, and upcoming invoice
- Response: BillingDashboardDTO
- Authentication: Required (JWT)

**POST /api/v1/billing/portal/subscription/upgrade**
- Description: Upgrade subscription to a higher tier
- Request Body: UpgradeRequest (newPlanCode)
- Response: SubscriptionDTO
- Authentication: Required (JWT, TENANT_OWNER or BILLING_ADMIN role)

**POST /api/v1/billing/portal/subscription/downgrade**
- Description: Downgrade subscription to a lower tier
- Request Body: DowngradeRequest (newPlanCode, immediate)
- Response: SubscriptionDTO
- Authentication: Required (JWT, TENANT_OWNER or BILLING_ADMIN role)

**POST /api/v1/billing/portal/subscription/cancel**
- Description: Cancel subscription
- Request Body: CancelRequest (immediate, reason)
- Response: SubscriptionDTO
- Authentication: Required (JWT, TENANT_OWNER or BILLING_ADMIN role)

**POST /api/v1/billing/portal/subscription/reactivate**
- Description: Reactivate a canceled subscription
- Response: SubscriptionDTO
- Authentication: Required (JWT, TENANT_OWNER or BILLING_ADMIN role)

**GET /api/v1/billing/portal/invoices**
- Description: List invoices with pagination
- Query Parameters:
  - `status` (optional): Filter by invoice status
  - `page` (default: 0): Page number
  - `size` (default: 20): Page size
- Response: Page<InvoiceDTO>
- Authentication: Required (JWT)

**GET /api/v1/billing/portal/invoices/{id}/pdf**
- Description: Download invoice as PDF
- Path Parameters:
  - `id`: Invoice ID
- Response: PDF file (application/pdf)
- Authentication: Required (JWT)

**GET /api/v1/billing/portal/usage**
- Description: Get current usage metrics
- Response: UsageSummaryDTO
- Authentication: Required (JWT)

**GET /api/v1/billing/portal/payment-methods**
- Description: List payment methods
- Response: List<PaymentMethodDTO>
- Authentication: Required (JWT)

**POST /api/v1/billing/portal/payment-methods**
- Description: Add new payment method
- Request Body: PaymentMethodRequest
- Response: PaymentMethodDTO
- Authentication: Required (JWT, TENANT_OWNER or BILLING_ADMIN role)

**DELETE /api/v1/billing/portal/payment-methods/{id}**
- Description: Remove payment method
- Path Parameters:
  - `id`: Payment method ID
- Response: 204 No Content
- Authentication: Required (JWT, TENANT_OWNER or BILLING_ADMIN role)

**PUT /api/v1/billing/portal/payment-methods/{id}/default**
- Description: Set payment method as default
- Path Parameters:
  - `id`: Payment method ID
- Response: PaymentMethodDTO
- Authentication: Required (JWT, TENANT_OWNER or BILLING_ADMIN role)

#### Admin Endpoints

**POST /api/v1/admin/billing/plans**
- Description: Create new subscription plan
- Request Body: PlanRequest
- Response: SubscriptionPlanDTO
- Authentication: Required (JWT, ADMIN role)

**PUT /api/v1/admin/billing/plans/{id}**
- Description: Update subscription plan
- Path Parameters:
  - `id`: Plan ID
- Request Body: PlanUpdateRequest
- Response: SubscriptionPlanDTO
- Authentication: Required (JWT, ADMIN role)

**GET /api/v1/admin/billing/subscriptions**
- Description: List all subscriptions with filtering
- Query Parameters:
  - `status` (optional): Filter by status
  - `planCode` (optional): Filter by plan
  - `tenantId` (optional): Filter by tenant
  - `page` (default: 0): Page number
  - `size` (default: 20): Page size
- Response: Page<SubscriptionDTO>
- Authentication: Required (JWT, ADMIN role)

**GET /api/v1/admin/billing/subscriptions/{id}**
- Description: Get subscription details
- Path Parameters:
  - `id`: Subscription ID
- Response: SubscriptionDTO
- Authentication: Required (JWT, ADMIN role)

**POST /api/v1/admin/billing/subscriptions/{id}/cancel**
- Description: Force cancel subscription
- Path Parameters:
  - `id`: Subscription ID
- Request Body: AdminCancelRequest (reason)
- Response: SubscriptionDTO
- Authentication: Required (JWT, ADMIN role)

**POST /api/v1/admin/billing/subscriptions/{id}/extend-trial**
- Description: Extend trial period
- Path Parameters:
  - `id`: Subscription ID
- Request Body: ExtendTrialRequest (additionalDays)
- Response: SubscriptionDTO
- Authentication: Required (JWT, ADMIN role)

**GET /api/v1/admin/billing/invoices**
- Description: List all invoices with filtering
- Query Parameters:
  - `status` (optional): Filter by status
  - `tenantId` (optional): Filter by tenant
  - `page` (default: 0): Page number
  - `size` (default: 20): Page size
- Response: Page<InvoiceDTO>
- Authentication: Required (JWT, ADMIN role)

**POST /api/v1/admin/billing/invoices/{id}/void**
- Description: Void an invoice
- Path Parameters:
  - `id`: Invoice ID
- Request Body: VoidInvoiceRequest (reason)
- Response: InvoiceDTO
- Authentication: Required (JWT, ADMIN role)

**GET /api/v1/admin/billing/analytics/mrr**
- Description: Get Monthly Recurring Revenue
- Response: MRRAnalyticsDTO
- Authentication: Required (JWT, ADMIN role)

**GET /api/v1/admin/billing/analytics/churn**
- Description: Get churn analysis
- Query Parameters:
  - `startDate` (optional): Start date for analysis
  - `endDate` (optional): End date for analysis
- Response: ChurnAnalyticsDTO
- Authentication: Required (JWT, ADMIN role)

#### Webhook Endpoints

**POST /api/v1/billing/webhooks/stripe**
- Description: Stripe webhook handler
- Headers:
  - `Stripe-Signature`: Webhook signature for verification
- Request Body: Stripe event payload
- Response: 200 OK
- Authentication: Signature verification

**POST /api/v1/billing/webhooks/paypal**
- Description: PayPal webhook handler
- Headers:
  - `PAYPAL-TRANSMISSION-ID`: Webhook ID
  - `PAYPAL-TRANSMISSION-TIME`: Timestamp
  - `PAYPAL-TRANSMISSION-SIG`: Signature
- Request Body: PayPal event payload
- Response: 200 OK
- Authentication: Signature verification

#### Internal Endpoints

**GET /internal/billing/feature-access/{tenantId}**
- Description: Check feature access for tenant
- Path Parameters:
  - `tenantId`: Tenant ID
- Query Parameters:
  - `feature`: Feature name
- Response: FeatureAccessDTO (allowed: boolean, reason: string)
- Authentication: Internal service authentication

**GET /internal/billing/quota/{tenantId}**
- Description: Check quota status for tenant
- Path Parameters:
  - `tenantId`: Tenant ID
- Query Parameters:
  - `metricType`: Metric type
- Response: QuotaStatusDTO
- Authentication: Internal service authentication

**POST /internal/billing/usage**
- Description: Record usage
- Request Body: UsageRecordRequest
- Response: 202 Accepted
- Authentication: Internal service authentication

---

## Asynchronous Processing

### RabbitMQ Queues and Exchanges

**Exchange: billing.events**
- Type: Topic
- Durable: true

**Queues:**

1. **billing.usage.queue**
   - Routing Key: `usage.recorded`
   - Purpose: Process usage records asynchronously
   - Consumer: UsageRecordConsumer
   - Dead Letter Queue: billing.usage.dlq

2. **billing.invoice.queue**
   - Routing Key: `invoice.generate`
   - Purpose: Generate invoices asynchronously
   - Consumer: InvoiceGenerationConsumer
   - Dead Letter Queue: billing.invoice.dlq

3. **billing.webhook.queue**
   - Routing Key: `webhook.received`
   - Purpose: Process webhook events asynchronously
   - Consumer: WebhookProcessingConsumer
   - Dead Letter Queue: billing.webhook.dlq

4. **billing.notification.queue**
   - Routing Key: `notification.send`
   - Purpose: Send billing notifications via email
   - Consumer: NotificationConsumer
   - Dead Letter Queue: billing.notification.dlq

5. **billing.payment-retry.queue**
   - Routing Key: `payment.retry`
   - Purpose: Retry failed payments
   - Consumer: PaymentRetryConsumer
   - Dead Letter Queue: billing.payment-retry.dlq

### Event Publishing

```java
@Service
public class BillingEventPublisher {
    
    private final RabbitTemplate rabbitTemplate;
    
    public void publishUsageRecorded(UsageRecordEvent event) {
        rabbitTemplate.convertAndSend("billing.events", "usage.recorded", event);
    }
    
    public void publishInvoiceGenerate(InvoiceGenerateEvent event) {
        rabbitTemplate.convertAndSend("billing.events", "invoice.generate", event);
    }
    
    public void publishWebhookReceived(WebhookEvent event) {
        rabbitTemplate.convertAndSend("billing.events", "webhook.received", event);
    }
    
    public void publishNotificationSend(NotificationEvent event) {
        rabbitTemplate.convertAndSend("billing.events", "notification.send", event);
    }
    
    public void publishPaymentRetry(PaymentRetryEvent event) {
        rabbitTemplate.convertAndSend("billing.events", "payment.retry", event);
    }
}
```

---

## Caching Strategy

### Redis Cache Configuration

**Cache Keys:**
- `subscription:{tenantId}` - Active subscription for tenant
- `plan:{planCode}` - Subscription plan details
- `usage:{tenantId}:{metricType}:{period}` - Usage aggregates
- `quota:{tenantId}:{metricType}` - Quota status
- `payment-method:{tenantId}:default` - Default payment method

**TTL Configuration:**
- Subscription: 5 minutes
- Plan: 1 hour
- Usage: 1 minute
- Quota: 1 minute
- Payment Method: 10 minutes

**Cache Invalidation:**
- Subscription changes: Invalidate `subscription:{tenantId}`
- Plan updates: Invalidate `plan:{planCode}`
- Usage recording: Invalidate `usage:{tenantId}:{metricType}:{period}` and `quota:{tenantId}:{metricType}`
- Payment method changes: Invalidate `payment-method:{tenantId}:default`

---

## Security Considerations

### Authentication and Authorization

1. **JWT Token Validation**: All authenticated endpoints validate JWT tokens issued by the User Service
2. **Role-Based Access Control**: 
   - TENANT_OWNER: Can manage subscription, payment methods, view invoices
   - BILLING_ADMIN: Same as TENANT_OWNER
   - ADMIN: Full access to all billing operations and analytics
   - USER: Read-only access to billing information

3. **Tenant Context Extraction**: Extract tenant ID from JWT claims to enforce tenant isolation

### Payment Security

1. **PCI DSS Compliance**: Never store full credit card numbers; use payment provider tokenization
2. **Webhook Signature Verification**: Verify all webhook signatures before processing
3. **Idempotency Keys**: Use idempotency keys for all payment operations to prevent duplicate charges
4. **Audit Logging**: Log all payment operations with immutable audit records

### Data Protection

1. **Encryption at Rest**: Sensitive data encrypted in PostgreSQL
2. **Encryption in Transit**: All API communication over TLS 1.3
3. **Tenant Isolation**: Schema-per-tenant ensures complete data separation
4. **GDPR Compliance**: Support data export and deletion requests

---

## Monitoring and Observability

### Metrics

**Business Metrics:**
- Monthly Recurring Revenue (MRR)
- Annual Recurring Revenue (ARR)
- Churn rate
- Trial conversion rate
- Average revenue per user (ARPU)
- Customer lifetime value (CLV)

**Operational Metrics:**
- API request rate and latency (p50, p95, p99)
- Payment success/failure rate
- Invoice generation time
- Usage recording latency
- Webhook processing time
- Database connection pool usage
- Cache hit rate
- Message queue depth

**Error Metrics:**
- Error rate by endpoint
- Payment failure rate
- Webhook processing failures
- Database errors
- External service errors (payment providers, email service)

### Logging

**Structured Logging Format:**
```json
{
  "timestamp": "2024-12-05T10:30:00Z",
  "level": "INFO",
  "service": "billing-service",
  "traceId": "abc123",
  "spanId": "def456",
  "tenantId": "tenant-uuid",
  "userId": "user-uuid",
  "operation": "subscription.upgrade",
  "message": "Subscription upgraded successfully",
  "details": {
    "subscriptionId": 123,
    "oldPlanCode": "PRO",
    "newPlanCode": "ENTERPRISE",
    "prorationAmount": 25.00
  }
}
```

**Log Levels:**
- ERROR: Payment failures, webhook processing errors, database errors
- WARN: Quota approaching limits, payment retry attempts, trial ending soon
- INFO: Subscription lifecycle events, invoice generation, payment success
- DEBUG: Detailed operation traces, cache hits/misses

### Distributed Tracing

- Use Spring Cloud Sleuth for trace and span ID generation
- Propagate trace context across service boundaries
- Include trace IDs in all log entries
- Export traces to distributed tracing system (Jaeger, Zipkin)

### Alerting

**Critical Alerts:**
- Payment failure rate > 5%
- API error rate > 1%
- Database connection pool > 80%
- Webhook processing failures
- Service health check failures

**Warning Alerts:**
- Response time p95 > 500ms
- Cache hit rate < 70%
- Message queue depth > 1000
- Trial conversion rate drops > 20%

---

## Deployment Considerations

### Environment Configuration

**Configuration Properties:**
```yaml
billing:
  payment:
    provider: stripe # stripe, paypal, manual
    stripe:
      apiKey: ${STRIPE_API_KEY}
      webhookSecret: ${STRIPE_WEBHOOK_SECRET}
    paypal:
      clientId: ${PAYPAL_CLIENT_ID}
      clientSecret: ${PAYPAL_CLIENT_SECRET}
  features:
    usageMetering: true
    proration: true
    dunning: true
  retry:
    maxAttempts: 3
    delays: [3, 5, 7] # days
  invoice:
    dueInDays: 7
    
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
  redis:
    host: ${REDIS_HOST}
    port: ${REDIS_PORT}
  rabbitmq:
    host: ${RABBITMQ_HOST}
    port: ${RABBITMQ_PORT}
    username: ${RABBITMQ_USERNAME}
    password: ${RABBITMQ_PASSWORD}
```

### Scaling Strategy

1. **Horizontal Scaling**: Deploy multiple service instances behind load balancer
2. **Database Connection Pooling**: Configure appropriate pool size per instance
3. **Redis Clustering**: Use Redis cluster for high availability
4. **RabbitMQ Clustering**: Use RabbitMQ cluster with mirrored queues

### Health Checks

```java
@Component
public class BillingHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        // Check database connectivity
        // Check Redis connectivity
        // Check RabbitMQ connectivity
        // Check payment provider connectivity
        
        return Health.up()
            .withDetail("database", "UP")
            .withDetail("redis", "UP")
            .withDetail("rabbitmq", "UP")
            .withDetail("paymentProvider", "UP")
            .build();
    }
}
```

---

## Migration Strategy

### Existing Tenant Migration

1. **Pre-Migration:**
   - Create subscription plans in new billing service
   - Map existing tenants to appropriate plans
   - Validate data integrity

2. **Migration:**
   - Create subscription records for all tenants
   - Import payment methods (if applicable)
   - Migrate invoice history (if applicable)
   - Update tenant records in User Service with subscription references

3. **Post-Migration:**
   - Enable billing service routing in Gateway
   - Monitor for issues
   - Provide rollback capability

4. **Rollback Plan:**
   - Disable billing service routing in Gateway
   - Revert tenant subscription references
   - Restore previous billing system (if applicable)

---

## Future Enhancements

1. **Multi-Currency Support**: Support multiple currencies with automatic conversion
2. **Tax Calculation Integration**: Integrate with tax calculation services (Avalara, TaxJar)
3. **Dunning Management**: Automated dunning campaigns for failed payments
4. **Usage-Based Pricing**: Support for metered billing beyond quotas
5. **Promotional Codes**: Support for discount codes and promotions
6. **Referral Program**: Track and reward customer referrals
7. **Self-Service Plan Changes**: Allow customers to change billing cycles
8. **Invoice Customization**: Allow tenants to customize invoice branding
9. **Payment Plans**: Support for installment payments
10. **Subscription Pausing**: Allow temporary subscription pauses

---

## Conclusion

This design provides a comprehensive, scalable, and secure billing and subscription management system for the IQ Scaffold platform. The architecture emphasizes payment provider abstraction, tenant isolation, idempotency, and comprehensive audit trails. The correctness properties defined ensure that critical billing behaviors are testable and verifiable through property-based testing, providing confidence in the system's reliability for financial operations.
