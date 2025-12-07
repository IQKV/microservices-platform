# Implementation Plan: Billing & Subscription Management Service

## Technical Implementation Standards

This implementation follows the detailed technical standards defined in the design document and requirements:

**Event Handling & Async Processing Guidelines:**

**Use Synchronous Processing When:**
- User needs immediate feedback (payment processing, quota checks)
- Operation is fast (<100ms) and reliable
- Strong consistency is required (subscription creation, plan changes)
- Operation is part of a user-facing transaction

**Use Asynchronous Processing When:**
- Operation is long-running (>1 second) - PDF generation, batch processing
- High volume operations that can be buffered - usage recording (10,000+ records/sec)
- External service calls that may be slow/unreliable - email sending, webhooks
- Eventual consistency is acceptable - notifications, analytics updates
- Retry logic is needed - payment retries, webhook processing
- Operations can be batched for efficiency - usage aggregation, invoice generation
- Decoupling is beneficial - prevent cascading failures

**Async Processing Patterns Used:**
1. **Event Publishing:** Domain events published to RabbitMQ after aggregate persistence
2. **Message Consumers:** Dedicated consumers process events asynchronously with retry logic
3. **Scheduled Jobs:** Jobs identify work and publish to queues for async processing
4. **Idempotency:** All async operations use idempotency keys to handle duplicates
5. **Dead Letter Queues:** Failed messages routed to DLQ for manual review
6. **Circuit Breakers:** Protect against cascading failures in external service calls

**Java 21 Modern Features:**
- Use Java records for all DTOs, value objects, and immutable data carriers
- Use text blocks (""") for multi-line strings (JPQL queries, OpenAPI descriptions, JSON examples)
- Use pattern matching for instanceof instead of traditional casting
- Use switch expressions with arrow syntax instead of traditional switch statements
- Use var for local variables when type is obvious
- Use sealed interfaces for restricted type hierarchies (e.g., PaymentProviderAdapter)
- Use Stream API enhancements (toList() instead of collect(Collectors.toList()))

**Naming Conventions:**
- REST controllers: Use `-RestResource` suffix (e.g., SubscriptionRestResource, not SubscriptionController)
- Configuration: Use `@ConfigurationProperties` with Java records, prefix `iqscaffold.billing.`
- Constants: Centralize in `BillingConstants.java` with nested static classes
- Exceptions: Domain-specific custom exceptions in `shared/exception` package

**OpenAPI Documentation:**
- Use `@Tag` on controllers for grouping
- Use `@Operation` with detailed descriptions using text blocks
- Use `@ApiResponses` documenting all response codes
- Use `@Schema` with examples using text blocks
- Use `@SecurityRequirement` on protected endpoints
- Use `@Timed` for metrics collection

**Internationalization:**
- Use `MessageService` for all user-facing messages
- Message keys follow pattern: `{category}.{entity}.{action}`
- Support English, Spanish, French
- Use parameterized messages with `{0}`, `{1}` placeholders

**Database Migrations:**
- System changesets: `db/changelog/system/00000000000000-descriptive-name.xml`
- Tenant changesets: `db/changelog/tenant/YYYYMMDDHHMMSS-descriptive-name.xml`
- Separate changesets for tables, indexes, constraints, and seed data

**Package Structure:**
- Domain-driven organization: subscription/, plan/, usage/, payment/, paymentmethod/, invoice/, billing/, portal/, webhook/, analytics/, config/, security/, shared/
- Each domain package contains: entity, repository, service, REST resource, DTOs

---

## Phase 1: Project Setup and Infrastructure

- [x] 1. Create billing service module structure and configure Java 21


  - Create iqscaffold-billing-service directory with Maven module structure
  - Configure pom.xml with Java 21 target version
  - Add Spring Boot, PostgreSQL, Redis, RabbitMQ dependencies
  - Add jqwik dependency for property-based testing
  - Add SpringDoc OpenAPI dependency for API documentation
  - Add module to parent pom.xml
  - Configure Maven compiler plugin with Java 21 source/target and enable preview features
  - Set up application.yml with multi-environment configuration (local, staging, production)
  - Configure port 8082 for the service
  - _Requirements: 1.1, 1.2, 3.8.1, 3.8.12_

- [x] 1.1 Set up configuration properties with type-safe records


  - Create BillingProperties record class with @ConfigurationProperties(prefix = "iqscaffold.billing")
  - Define nested record classes for payment, subscription, usage, invoice, portal, and features configuration
  - Add Jakarta validation annotations to all configuration properties
  - Configure spring-configuration-metadata.json generation for IDE autocomplete
  - Create application-local.yml, application-staging.yml, application-production.yml
  - Use environment variables with IQSCAFFOLD_BILLING_ prefix for sensitive configuration
  - _Requirements: 3.2.1, 3.2.2, 3.2.3, 3.2.4, 3.2.5, 3.2.6, 3.2.7, 3.2.8_



- [x] 1.2 Create shared utilities and constants

  - Create BillingConstants.java with nested static classes for Headers, MDC, SubscriptionStatus, BillingEvents, CacheNames, PaymentProviders, InvoiceFormat, Defaults, MetricTypes, and ErrorCodes
  - Add Javadoc comments to each nested class
  - Make all constant fields public static final with UPPER_SNAKE_CASE naming
  - Ensure private constructors throw UnsupportedOperationException

  - _Requirements: 3.5.1, 3.5.2, 3.5.3, 3.5.4, 3.5.5, 3.5.6, 3.5.7, 3.5.8, 3.5.9_

- [x] 1.3 Set up internationalization (i18n) support

  - Configure Spring messages in application.yml (basename: i18n/messages, encoding: UTF-8, cache-duration: PT1H)
  - Create messages.properties (English), messages_es.properties (Spanish), messages_fr.properties (French)
  - Organize message keys by category (subscription, plan, payment, invoice, usage, feature, validation, error, email)
  - Create MessageService class in shared package for convenient message retrieval
  - Implement locale resolution from user preferences, Accept-Language header, or default
  - _Requirements: 3.7.1, 3.7.2, 3.7.3, 3.7.4, 3.7.6, 3.7.7, 3.7.8, 3.7.9, 3.7.10, 3.7.11, 3.7.12, 3.7.13, 3.7.14, 3.7.15_


- [x] 1.4 Create custom exception hierarchy

  - Create BillingException base class in shared/exception package
  - Create SubscriptionException with nested SubscriptionNotFoundException, SubscriptionAlreadyExistsException, InvalidSubscriptionStateException
  - Create PaymentException with nested PaymentFailedException (with paymentId, reason fields), PaymentMethodNotFoundException, InvalidPaymentMethodException
  - Create UsageException with nested QuotaExceededException (with metricType, limit, used fields), UsageLimitExceededException
  - Create PlanException with nested PlanNotFoundException, InvalidPlanTransitionException (with fromPlan, toPlan fields)
  - Create InvoiceException with nested InvoiceNotFoundException, InvoiceAlreadyPaidException
  - Provide both message-only and message-with-cause constructors for base exceptions
  - Add getter methods for context fields
  - _Requirements: 3.6.1, 3.6.2, 3.6.3, 3.6.4, 3.6.5, 3.6.6, 3.6.7, 3.6.8_

- [x] 1.5 Configure OpenAPI documentation with SpringDoc

  - Configure SpringDoc in application.yml with API docs path and Swagger UI
  - Define API groups: subscription-plans, subscriptions, payments, invoices, usage, webhooks, admin
  - Configure group paths-to-match for logical organization
  - Set operations-sorter and tags-sorter to alpha
  - Define common error responses for reuse across endpoints
  - _Requirements: 3.3.7, 3.3.10_

- [x] 2. Set up Liquibase database migration infrastructure







  - Create db/changelog/system/ directory for public schema migrations
  - Create db/changelog/tenant/ directory for tenant schema migrations
  - Create system/master.xml and tenant/master.xml files with proper XML headers
  - Configure Liquibase properties in application.yml (systemChangeLog, tenantChangeLog paths)
  - Set up naming conventions: system changesets use 00000000000000-descriptive-name.xml, tenant changesets use YYYYMMDDHHMMSS-descriptive-name.xml
  - Create initial system changeset for subscription_plans table with indexes and constraints
  - Create initial tenant changesets for subscriptions, usage_records, payment_methods, invoices, payments, billing_events tables
  - Use descriptive comments in each changeSet
  - Add rollback blocks where applicable
  - Separate table creation, index creation, and constraint addition into distinct changesets
  - _Requirements: 3.4.1, 3.4.2, 3.4.3, 3.4.4, 3.4.5, 3.4.6, 3.4.7, 3.4.8, 3.4.9, 3.4.10, 3.4.11, 3.4.12, 3.4.13, 3.4.14, 3.4.15, 3.4.16, 3.4.17, 3.4.18, 3.4.19, 3.4.20_



- [x] 3. Set up multi-tenancy infrastructure








  - Implement Hibernate multi-tenancy configuration with schema-per-tenant strategy
  - Create TenantContext holder for thread-local tenant ID storage
  - Implement TenantIdentifierResolver to extract tenant from request context
  - Configure Liquibase to run programmatically per schema (system and tenant)
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7_

- [x] 4. Configure external integrations





  - Set up PostgreSQL datasource with connection pooling (20 connections per instance)
  - Configure Redis for caching with appropriate TTL settings (subscriptions: 5min, plans: 1hr, usage: 1min, quotas: 1min, payment-methods: 10min)
  - Configure RabbitMQ exchanges, queues, and dead letter queues
  - Set up Spring Cloud Sleuth for distributed tracing
  - _Requirements: 1.3, 1.4, 1.5_

## Phase 2: Domain Layer - Aggregates, Entities, and Value Objects

- [x] 5. Set up domain package structure following platform conventions





  - Create package structure: subscription/, plan/, usage/, payment/, paymentmethod/, invoice/, billing/, portal/, webhook/, analytics/, config/, security/, shared/
  - Within each domain package, create entity, repository, service, REST resource (-RestResource suffix), and DTO files
  - Create shared/exception/ package for custom exceptions
  - Follow naming conventions: entities (no suffix), repositories (Repository suffix), services (Service suffix), REST resources (RestResource suffix)
  - _Requirements: 3.1.1, 3.1.2, 3.1.3, REQ-DDD-045, REQ-DDD-046, REQ-DDD-047, REQ-DDD-048, REQ-DDD-049, REQ-DDD-061, REQ-DDD-062_

- [x] 6. Implement subscription aggregate with Java 21 features





  - Create Subscription aggregate root entity with identity and lifecycle
  - Create SubscriptionPlan aggregate root entity (public schema)
  - Create PlanTier and BillingCycle enums
  - Create PlanQuotas as Java record (immutable value object)
  - Use switch expressions for status transitions
  - Use pattern matching for instanceof where applicable
  - Implement aggregate invariants and business rules
  - Ensure modifications go through aggregate root
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 3.8.2, 3.8.5, 3.8.4, REQ-DDD-005, REQ-DDD-006, REQ-DDD-010, REQ-DDD-011, REQ-DDD-012, REQ-DDD-013, REQ-DDD-015, REQ-DDD-016, REQ-DDD-017_


- [x] 7. Implement invoice aggregate with Java records




  - Create Invoice aggregate root entity with identity and lifecycle
  - Create InvoiceLineItem as Java record (immutable value object)
  - Create InvoiceStatus enum
  - Implement aggregate invariants (total calculations, line item consistency)
  - Ensure all modifications to line items go through Invoice aggregate root
  - _Requirements: REQ-INV-001, REQ-INV-002, REQ-INV-007, 3.8.2, REQ-DDD-007, REQ-DDD-010, REQ-DDD-011, REQ-DDD-012, REQ-DDD-013, REQ-DDD-014_

- [x] 8. Implement payment aggregates





  - Create Payment aggregate root entity
  - Create PaymentMethod aggregate root entity
  - Create PaymentStatus and PaymentMethodType enums
  - Implement aggregate invariants and business rules
  - Use switch expressions for payment status handling
  - _Requirements: REQ-PAY-007, REQ-PAY-014, 3.8.5, REQ-DDD-008, REQ-DDD-009, REQ-DDD-010, REQ-DDD-011, REQ-DDD-012, REQ-DDD-013_

- [x] 9. Implement usage entities and value objects as Java records





  - Create UsageRecord entity
  - Create BillingEvent entity
  - Create MetricType enum
  - Create UsageSummary, UsageMetric, QuotaCheckResult as Java records (immutable value objects)
  - Implement value object equality based on attributes (automatic with records)
  - _Requirements: REQ-USAGE-001, REQ-USAGE-002, 3.8.2, REQ-DDD-013, REQ-DDD-015, REQ-DDD-016, REQ-DDD-017_

- [x] 10. Implement value objects for business logic as Java records





  - Create ProrationResult as Java record with calculation logic
  - Ensure all value objects are immutable (automatic with records)
  - Use records to encapsulate complex business logic with minimal boilerplate
  - _Requirements: 3.8.2, 3.8.15, REQ-DDD-015, REQ-DDD-016, REQ-DDD-017, REQ-DDD-018_

## Phase 3: Domain Layer - Repositories, Domain Services, and Specifications

- [x] 11. Implement repository interfaces in domain layer with text blocks for queries





  - Create SubscriptionRepository interface in domain layer
  - Create SubscriptionPlanRepository interface in domain layer
  - Create InvoiceRepository interface in domain layer
  - Create PaymentRepository interface in domain layer
  - Create PaymentMethodRepository interface in domain layer
  - Create UsageRecordRepository interface in domain layer
  - Define repository methods that return fully reconstituted aggregates
  - Use text blocks (""") for multi-line JPQL queries
  - Use specification pattern for complex queries
  - _Requirements: 3.8.3, REQ-DDD-024, REQ-DDD-026, REQ-DDD-027, REQ-DDD-028, REQ-DDD-029_

- [x] 12. Implement domain services




  - Create ProrationCalculator domain service for proration logic
  - Create QuotaEnforcer domain service for quota validation
  - Create SubscriptionLifecycleManager domain service for state transitions
  - Create InvoiceGenerator domain service for invoice creation logic
  - Keep domain services stateless and focused on domain logic only
  - _Requirements: REQ-DDD-019, REQ-DDD-020, REQ-DDD-021, REQ-DDD-022, REQ-DDD-023_

- [x] 13. Implement specifications





  - Create ActiveSubscriptionSpecification
  - Create QuotaExceededSpecification
  - Create ValidPlanTransitionSpecification
  - Create TrialEligibilitySpecification
  - Implement composite specifications with AND, OR, NOT operators
  - Use specifications to encapsulate reusable business rules
  - _Requirements: REQ-DDD-039, REQ-DDD-040, REQ-DDD-041, REQ-DDD-042, REQ-DDD-043, REQ-DDD-044_

- [x] 14. Implement factories










  - Create SubscriptionFactory for complex subscription creation
  - Create InvoiceFactory for invoice creation with line items
  - Encapsulate complex object construction and ensure invariants
  - Validate business rules in factories before object creation
  - _Requirements: REQ-DDD-035, REQ-DDD-036, REQ-DDD-037, REQ-DDD-038_

- [x] 15. Implement domain events as Java records






  - Create domain event base class as immutable Java record
  - Create domain events as records: SubscriptionCreated, SubscriptionUpgraded, SubscriptionDowngraded, SubscriptionCanceled, SubscriptionReactivated, TrialStarted, TrialEnding, TrialEnded, InvoiceGenerated, InvoicePaid, InvoiceVoided, PaymentSucceeded, PaymentFailed, PaymentRefunded, UsageRecorded, QuotaExceeded
  - Include timestamp and aggregate identity in all events
  - Implement event publishing mechanism after aggregate persistence
  - _Requirements: 3.8.2, REQ-DDD-030, REQ-DDD-031, REQ-DDD-032, REQ-DDD-033, REQ-DDD-034_

## Phase 4: Infrastructure Layer - Persistence and External Integrations

- [x] 16. Implement repository implementations in infrastructure layer




  - Create JPA repository implementations for all domain repositories
  - Implement multi-tenant support in repository implementations
  - Ensure repositories return fully reconstituted aggregates
  - Use var for obvious types in repository implementations
  - _Requirements: 3.8.6, REQ-DDD-025, REQ-DDD-028, REQ-DATA-001 through REQ-DATA-022_

- [x] 17. Create payment provider anti-corruption layer with sealed interface





  - Define PaymentProviderAdapter as sealed interface permitting StripePaymentProvider, PayPalPaymentProvider, ManualPaymentProvider
  - Create PaymentResult, PaymentMethodDetails, and CustomerUpdateRequest as Java records
  - Document interface contract and expected behaviors
  - _Requirements: 3.8.2, 3.8.8, REQ-PAY-001, REQ-DDD-003, REQ-DDD-057, REQ-DDD-058, REQ-DDD-059, REQ-DDD-060_

- [x] 18. Implement Stripe payment adapter with anti-corruption layer







  - Create StripePaymentAdapter as final class implementing PaymentProviderAdapter
  - Integrate Stripe Java SDK
  - Translate Stripe models to domain models in the adapter
  - Implement payment method management (create, delete, get details)
  - Implement payment processing (charge, refund)
  - Implement customer management (create, update)
  - Implement webhook signature verification
  - Isolate domain model from Stripe API changes
  - Use pattern matching for instanceof when handling Stripe responses
  - _Requirements: 3.8.4, 3.8.8, REQ-PAY-002, REQ-PAY-007, REQ-PAY-008, REQ-PAY-009, REQ-PAY-020, REQ-PAY-030, REQ-PAY-038, REQ-DDD-057, REQ-DDD-059, REQ-DDD-060_

- [x] 19. Implement PayPal payment adapter with anti-corruption layer





  - Create PayPalPaymentAdapter as final class implementing PaymentProviderAdapter
  - Integrate PayPal Java SDK
  - Translate PayPal models to domain models in the adapter
  - Implement payment method management
  - Implement payment processing
  - Implement customer management
  - Implement webhook signature verification
  - Isolate domain model from PayPal API changes
  - _Requirements: 3.8.8, REQ-PAY-003, REQ-PAY-009, REQ-DDD-058, REQ-DDD-059, REQ-DDD-060_

- [x] 20. Implement manual payment adapter





  - Create ManualPaymentAdapter as final class implementing PaymentProviderAdapter
  - Implement manual payment recording for enterprise contracts
  - Implement offline payment tracking
  - _Requirements: 3.8.8, REQ-PAY-004_

- [x] 21. Create payment provider factory






  - Implement PaymentProviderFactory to select provider based on configuration
  - Use switch expression for provider selection
  - Add fallback logic to manual processing when provider unavailable
  - Configure provider selection via BillingProperties
  - _Requirements: 3.8.5, REQ-PAY-005, REQ-PAY-006_

## Phase 5: Application Layer - Application Services and DTOs

- [x] 22. Create DTOs as Java records





  - Create SubscriptionDto, SubscriptionPlanDto, CreateSubscriptionRequest, UpdateSubscriptionRequest as Java records
  - Create InvoiceDto, InvoiceLineItemDto, CreateInvoiceRequest as Java records
  - Create PaymentDto, PaymentMethodDto, AddPaymentMethodRequest as Java records
  - Create UsageDto, UsageSummaryDto, QuotaUsageDto as Java records
  - Create BillingDashboardDto, UpgradeDowngradeRequest as Java records
  - Add Jakarta validation annotations to request records
  - Use @Schema annotations with examples (using text blocks) for OpenAPI documentation
  - _Requirements: 3.8.2, 3.8.3, 3.8.15_

- [ ] 23. Implement plan application service with i18n
  - Create PlanApplicationService as thin orchestration layer
  - Inject MessageService for internationalized messages
  - Delegate business logic to domain services and aggregates
  - Implement createPlan, updatePlan, archivePlan use cases
  - Implement getPlan, getPlanByCode, getPublicPlans, getAllPlans queries
  - Use ValidPlanTransitionSpecification for validation
  - Manage transactions at application service level
  - Translate between domain objects and DTOs (records)
  - Use i18n messages for success/error messages (e.g., messageService.getMessage("plan.created"))
  - Add caching for plan lookups with 1-hour TTL using BillingConstants.CacheNames
  - _Requirements: 3.7.5, 3.7.6, 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 5.1, 5.2, 5.3, 5.4, REQ-DDD-052, REQ-DDD-053, REQ-DDD-054, REQ-DDD-056_

- [ ] 24. Implement subscription application service - creation and trial management with i18n
  - Create SubscriptionApplicationService as thin orchestration layer
  - Inject MessageService for internationalized messages
  - Use SubscriptionFactory for subscription creation
  - Use SubscriptionLifecycleManager domain service for state transitions
  - Use TrialEligibilitySpecification for validation
  - Implement createSubscription use case with trial period logic
  - Implement trial management use cases (extendTrial, convertTrialToActive)
  - Publish domain events (SubscriptionCreated, TrialStarted) using BillingConstants.BillingEvents
  - Manage transactions at application service level
  - Translate between domain objects and DTOs (records)
  - Use i18n messages for success/error messages
  - Add subscription caching with 5-minute TTL using BillingConstants.CacheNames
  - _Requirements: 3.7.5, 3.7.6, REQ-SUB-001 through REQ-SUB-015, REQ-DDD-052, REQ-DDD-053, REQ-DDD-054, REQ-DDD-055, REQ-DDD-056_

- [ ] 25. Implement subscription application service - upgrade and downgrade with i18n
  - Use ProrationCalculator domain service for proration calculations
  - Use ValidPlanTransitionSpecification for validation
  - Use QuotaExceededSpecification for downgrade validation
  - Use i18n messages for upgrade/downgrade confirmations
  - Implement upgradeSubscription use case with immediate proration
  - Implement downgradeSubscription use case with scheduled and immediate options
  - Publish domain events (SubscriptionUpgraded, SubscriptionDowngraded)
  - Manage transactions at application service level
  - _Requirements: REQ-SUB-023 through REQ-SUB-038, REQ-DDD-052, REQ-DDD-053, REQ-DDD-054, REQ-DDD-055_

- [ ] 24. Implement subscription application service - cancellation and reactivation
  - Use SubscriptionLifecycleManager domain service for state transitions
  - Use ActiveSubscriptionSpecification for validation
  - Implement cancelSubscription use case with immediate and period-end options
  - Implement reactivateSubscription use case
  - Publish domain events (SubscriptionCanceled, SubscriptionReactivated)
  - Manage transactions at application service level
  - _Requirements: REQ-SUB-039 through REQ-SUB-050, REQ-DDD-052, REQ-DDD-053, REQ-DDD-054, REQ-DDD-055_

- [ ] 25. Implement payment application service
  - Create PaymentApplicationService as thin orchestration layer
  - Delegate to Payment and PaymentMethod aggregates for business logic
  - Use PaymentProviderAdapter (anti-corruption layer) for external calls
  - Implement payment method management use cases (add, remove, setDefault, list)
  - Implement payment processing use cases (processPayment, retryPayment, refundPayment)
    - **Sync Processing:** Payment processing is synchronous (user needs immediate feedback)
    - **Timeout Handling:** Set timeout for payment provider calls (10 seconds)
    - **Circuit Breaker:** Implement circuit breaker for payment provider failures
  - Publish domain events (PaymentSucceeded, PaymentFailed, PaymentRefunded)
    - **Event-Driven:** PaymentFailed triggers async retry scheduling
    - **Event-Driven:** PaymentSucceeded triggers async receipt email and invoice update
    - **Event-Driven:** PaymentRefunded triggers async notification
  - Implement idempotency using idempotency keys
    - Store idempotency keys with payment results (24-hour TTL)
    - Return cached result for duplicate payment requests
  - Implement retryPayment use case
    - **Async Processing:** Payment retries scheduled via PaymentRetryConsumer
    - **Retry Schedule:** Day 1, Day 3, Day 7, Day 14 after initial failure
    - **Exponential Backoff:** Increasing delays between retry attempts
  - Manage transactions at application service level
  - Translate between domain objects and DTOs
  - Add payment method caching with 10-minute TTL
  - _Requirements: REQ-PAY-007 through REQ-PAY-036, REQ-DDD-052, REQ-DDD-053, REQ-DDD-054, REQ-DDD-055, REQ-DDD-056_


- [ ] 26. Implement invoice application service
  - Create InvoiceApplicationService as thin orchestration layer
  - Use InvoiceFactory for invoice creation
  - Use InvoiceGenerator domain service for invoice generation logic
  - Implement generateInvoice use case for regular billing periods
    - **Async Processing:** Invoice generation triggered by scheduled job, processed asynchronously
    - **Long-Running:** PDF generation can take seconds, shouldn't block HTTP requests
  - Implement generateProrationInvoice use case for mid-period changes
  - Implement invoice number generation (INV-{YEAR}{MONTH}-{SEQUENCE})
  - Implement finalizeInvoice and voidInvoice use cases
  - Implement generateInvoicePdf using PDF generation library
    - **Async Processing:** PDF generation published to queue, processed by InvoiceGenerationConsumer
    - **Streaming:** Use streaming for large invoices to prevent memory issues
  - Publish domain events (InvoiceGenerated, InvoicePaid, InvoiceVoided)
    - **Event-Driven:** InvoiceGenerated triggers async email notification with PDF attachment
    - **Event-Driven:** InvoicePaid triggers async receipt email and analytics update
  - Manage transactions at application service level
  - Translate between domain objects and DTOs
  - _Requirements: REQ-INV-001 through REQ-INV-021, REQ-INV-030 through REQ-INV-034, REQ-DDD-052, REQ-DDD-053, REQ-DDD-054, REQ-DDD-055, REQ-DDD-056_

- [ ] 27. Implement usage application service
  - Create UsageApplicationService as thin orchestration layer
  - Use QuotaEnforcer domain service for quota validation
  - Use QuotaExceededSpecification for quota checks
  - Implement recordUsage and recordUsageBatch use cases with idempotency
    - **Async Processing:** recordUsage publishes to RabbitMQ queue for async processing (high volume)
    - **Sync Response:** Returns immediately with 202 Accepted status
    - **Idempotency:** Use usage record ID to prevent duplicate processing
  - Implement usage aggregation by billing period
  - Implement getCurrentUsage and getUsageForPeriod query use cases
  - Implement checkQuota and enforceQuota use cases
    - **Caching:** Cache quota checks for 1 minute to reduce database load
    - **Real-time:** Quota enforcement must be synchronous for immediate feedback
  - Implement resetUsageForNewPeriod use case
  - Publish domain events (UsageRecorded, QuotaExceeded)
    - **Event-Driven:** QuotaExceeded event triggers async notification and feature throttling
  - Manage transactions at application service level
  - Translate between domain objects and DTOs
  - Add usage caching with 1-minute TTL
  - _Requirements: REQ-USAGE-001 through REQ-USAGE-023, REQ-DDD-052, REQ-DDD-053, REQ-DDD-054, REQ-DDD-055, REQ-DDD-056_

- [ ] 28. Implement webhook application service
  - Create WebhookApplicationService as thin orchestration layer
  - Use PaymentProviderAdapter for webhook signature verification
  - Implement processStripeWebhook and processPayPalWebhook use cases
    - **Async Processing:** Webhook endpoint returns 200 OK immediately, actual processing happens asynchronously
    - **Why Async:** Payment providers expect fast response (<5 seconds), processing may take longer
    - **Idempotency:** Critical for webhooks - providers may send duplicate events
    - **Retry Logic:** Failed webhook processing retried with exponential backoff
  - Implement event handlers for payment and subscription events
    - **Event Types:** payment_intent.succeeded, payment_intent.failed, charge.refunded, customer.subscription.updated
    - **Async Handlers:** Each event type processed by dedicated async handler
  - Implement idempotent webhook processing
    - Use webhook event ID as idempotency key
    - Store processed webhook IDs in database with TTL (30 days)
    - Return success for duplicate webhook events without reprocessing
  - Implement webhook event logging
    - Log all webhook events (raw payload, signature, processing status)
    - Enable webhook replay for debugging
  - Manage transactions at application service level
  - **Sync Response Pattern:** Webhook endpoint validates signature → publishes to queue → returns 200 OK
  - **Async Processing Pattern:** Consumer fetches from queue → processes event → updates domain → publishes domain events
  - _Requirements: REQ-PAY-037 through REQ-PAY-048, REQ-DDD-052, REQ-DDD-053, REQ-DDD-054_

## Phase 6: Infrastructure Layer - Asynchronous Processing

**Event Handling & Async Processing Strategy:**

This phase implements event-driven architecture for operations that benefit from asynchronous processing. Use async processing when:
- **Operations can be eventually consistent** (don't need immediate synchronous response)
- **Long-running operations** that would block HTTP requests (invoice generation, PDF creation, email sending)
- **High-volume operations** that need to be buffered and processed in batches (usage recording)
- **External integrations** that may be slow or unreliable (payment provider webhooks, email delivery)
- **Retry logic is needed** for failed operations (payment retries, webhook processing)
- **Decoupling services** to prevent cascading failures (notification sending shouldn't block payment processing)

**Key Async Use Cases in Billing Service:**
1. **Usage Recording** - High volume, can be batched, eventual consistency acceptable
2. **Invoice Generation** - Long-running (PDF creation), can be scheduled
3. **Webhook Processing** - External events, need retry logic, idempotency required
4. **Email Notifications** - External service, shouldn't block business operations
5. **Payment Retries** - Scheduled retries with exponential backoff
6. **Trial Expiration** - Scheduled batch processing

**Event Publishing Pattern:**
- Domain events published AFTER successful aggregate persistence (transactional outbox pattern)
- Events contain aggregate ID and minimal data (consumers fetch full data if needed)
- Events enable eventual consistency between aggregates
- Events trigger async workflows without tight coupling

- [ ] 29. Set up RabbitMQ infrastructure
  - Create billing.events exchange (topic, durable)
  - Create queues: usage, invoice, webhook, notification, payment-retry
  - Configure dead letter queues for all queues
  - Set up routing keys for each queue
  - Configure message TTL and retry policies (exponential backoff: 1min, 5min, 15min, 1hr, 6hr)
  - Configure queue priorities for critical operations (payment processing > notifications)
  - _Requirements: 1.5_


- [ ] 30. Implement domain event publisher
  - Create DomainEventPublisher in infrastructure layer
  - Implement publishing to RabbitMQ for domain events
  - Publish events after successful aggregate persistence (transactional outbox pattern recommended)
  - Use domain events for eventual consistency between aggregates
  - **Async Benefits:** Decouples event producers from consumers, enables parallel processing of event handlers, prevents cascading failures
  - **Event Types for Async Processing:**
    - SubscriptionCreated → triggers welcome email, analytics update
    - InvoiceGenerated → triggers PDF generation, email notification
    - PaymentFailed → triggers retry scheduling, notification
    - UsageRecorded → triggers quota check, analytics update
    - QuotaExceeded → triggers notification, feature throttling
  - Implement event metadata (eventId, timestamp, aggregateId, aggregateType, eventType, tenantId)
  - Implement event versioning for schema evolution
  - _Requirements: REQ-DDD-032, REQ-DDD-033, REQ-DDD-034, REQ-DDD-055, REQ-USAGE-006, REQ-INV-009, REQ-PAY-046_

- [ ] 31. Implement message consumers
  - **UsageRecordConsumer** for asynchronous usage recording
    - **Why Async:** High volume (10,000+ records/sec), can be batched, eventual consistency acceptable
    - Implement batch processing (process 100 records at a time)
    - Implement idempotency using usage record ID
    - Implement quota check after batch processing
    - Handle failures with retry and DLQ
  - **InvoiceGenerationConsumer** for asynchronous invoice generation
    - **Why Async:** Long-running operation (PDF generation can take seconds), shouldn't block subscription operations
    - Implement PDF generation using streaming to handle large invoices
    - Implement retry logic for PDF generation failures
    - Publish InvoiceGenerated event after successful generation
  - **WebhookProcessingConsumer** for asynchronous webhook processing
    - **Why Async:** External events from payment providers, need retry logic, idempotency critical
    - Implement idempotency using webhook event ID
    - Implement exponential backoff retry (1min, 5min, 15min, 1hr, 6hr)
    - Implement webhook signature verification
    - Handle duplicate webhook events gracefully
  - **NotificationConsumer** for sending billing emails
    - **Why Async:** External service (email provider), shouldn't block business operations, can tolerate delays
    - Implement email template rendering
    - Implement retry logic for email delivery failures
    - Implement email delivery tracking
    - Handle email provider rate limits
  - **PaymentRetryConsumer** for automated payment retries
    - **Why Async:** Scheduled retries with delays, external payment provider calls
    - Implement retry schedule (Day 1, Day 3, Day 7, Day 14)
    - Implement exponential backoff between attempts
    - Update subscription status based on retry results
    - Publish PaymentSucceeded or PaymentFailed events
  - **General Consumer Patterns:**
    - Implement error handling with categorization (transient vs permanent failures)
    - Implement dead letter queue processing with manual review workflow
    - Implement consumer metrics (processing time, success rate, retry count)
    - Implement circuit breaker for external service calls
    - Implement message acknowledgment only after successful processing
  - _Requirements: REQ-USAGE-006, REQ-INV-010, REQ-PAY-026, REQ-PAY-027, REQ-PAY-028, REQ-PAY-046_

## Phase 7: Presentation Layer - REST API Controllers

- [ ] 32. Implement public plan endpoints with comprehensive OpenAPI documentation
  - Create SubscriptionPlanRestResource (use -RestResource suffix per platform conventions)
  - Add @RestController and @RequestMapping("/api/v1/billing/plans")
  - Add @Tag(name = "Subscription Plans", description = "Public subscription plan APIs")
  - Delegate to PlanApplicationService
  - Implement GET /api/v1/billing/plans endpoint with billing cycle filtering
    - Add @Operation with summary and detailed description using text blocks (""")
    - Add @ApiResponses documenting 200, 400, 500 responses with @Content and @Schema
    - Add @Parameter annotations for query parameters
    - Include @ExampleObject with realistic JSON examples
  - Implement GET /api/v1/billing/plans/{code} endpoint
    - Add @Operation with detailed description
    - Add @ApiResponses for 200, 404, 500
  - Use request/response DTOs as Java records (not domain objects)
  - Add @Timed annotation for metrics collection
  - _Requirements: 3.3.1, 3.3.2, 3.3.3, 3.3.4, 3.3.5, 3.3.6, 3.3.8, 3.3.9, 6.1, 6.2, 6.3, 6.4, REQ-API-001, REQ-API-002, REQ-DDD-049_

- [ ] 33. Implement customer portal endpoints - subscription management with OpenAPI
  - Create BillingPortalRestResource (use -RestResource suffix)
  - Add @RestController and @RequestMapping("/api/v1/billing/portal")
  - Add @Tag(name = "Customer Portal", description = "Self-service billing portal APIs")
  - Delegate to SubscriptionApplicationService
  - Implement GET /api/v1/billing/portal/dashboard endpoint
    - Add @Operation with comprehensive description using text blocks
    - Add @ApiResponses with example responses using @ExampleObject
    - Add @SecurityRequirement annotation for JWT authentication
  - Implement POST /api/v1/billing/portal/subscription/upgrade endpoint
  - Implement POST /api/v1/billing/portal/subscription/downgrade endpoint
  - Implement POST /api/v1/billing/portal/subscription/cancel endpoint
  - Implement POST /api/v1/billing/portal/subscription/reactivate endpoint
  - Use request/response DTOs as Java records (not domain objects)
  - Add role-based access control (TENANT_OWNER, BILLING_ADMIN)
  - Add @Timed annotation for metrics collection
  - _Requirements: 3.3.1, 3.3.2, 3.3.3, 3.3.4, 3.3.5, 3.3.6, 3.3.9, REQ-PORTAL-001 through REQ-PORTAL-015, REQ-API-003 through REQ-API-006, REQ-DDD-049_


- [ ] 34. Implement customer portal endpoints - invoices and payments
  - Delegate to InvoiceApplicationService, UsageApplicationService, and PaymentApplicationService
  - Implement GET /api/v1/billing/portal/invoices endpoint with pagination and filtering
  - Implement GET /api/v1/billing/portal/invoices/{id}/pdf endpoint
  - Implement GET /api/v1/billing/portal/usage endpoint
  - Implement GET /api/v1/billing/portal/payment-methods endpoint
  - Implement POST /api/v1/billing/portal/payment-methods endpoint
  - Implement DELETE /api/v1/billing/portal/payment-methods/{id} endpoint
  - Implement PUT /api/v1/billing/portal/payment-methods/{id}/default endpoint
  - Use request/response DTOs (not domain objects)
  - Add OpenAPI/Swagger documentation
  - _Requirements: REQ-PORTAL-016 through REQ-PORTAL-034, REQ-API-007 through REQ-API-012, REQ-DDD-049_

- [ ] 35. Implement admin endpoints - plan and subscription management
  - Create AdminBillingController in presentation layer
  - Delegate to PlanApplicationService and SubscriptionApplicationService
  - Implement POST /api/v1/admin/billing/plans endpoint
  - Implement PUT /api/v1/admin/billing/plans/{id} endpoint
  - Implement GET /api/v1/admin/billing/subscriptions endpoint with filtering
  - Implement GET /api/v1/admin/billing/subscriptions/{id} endpoint
  - Implement POST /api/v1/admin/billing/subscriptions/{id}/cancel endpoint
  - Implement POST /api/v1/admin/billing/subscriptions/{id}/extend-trial endpoint
  - Use request/response DTOs (not domain objects)
  - Add role-based access control (ADMIN role)
  - Add OpenAPI/Swagger documentation
  - _Requirements: REQ-ADMIN-001 through REQ-ADMIN-009, REQ-API-013 through REQ-API-018, REQ-DDD-049_

- [ ] 36. Implement admin endpoints - invoices and analytics
  - Delegate to InvoiceApplicationService
  - Implement GET /api/v1/admin/billing/invoices endpoint with filtering
  - Implement POST /api/v1/admin/billing/invoices/{id}/void endpoint
  - Implement GET /api/v1/admin/billing/analytics/mrr endpoint
  - Implement GET /api/v1/admin/billing/analytics/churn endpoint
  - Use request/response DTOs (not domain objects)
  - Add OpenAPI/Swagger documentation
  - _Requirements: REQ-ADMIN-010 through REQ-ADMIN-027, REQ-API-019 through REQ-API-022, REQ-DDD-049_


- [ ] 37. Implement webhook endpoints
  - Create WebhookController in presentation layer
  - Delegate to WebhookApplicationService
  - Implement POST /api/v1/billing/webhooks/stripe endpoint
  - Implement POST /api/v1/billing/webhooks/paypal endpoint
  - Use request/response DTOs (not domain objects)
  - Add webhook signature verification
  - Add OpenAPI/Swagger documentation
  - _Requirements: REQ-PAY-037, REQ-PAY-038, REQ-PAY-039, REQ-API-023, REQ-API-024, REQ-DDD-049_

- [ ] 38. Implement internal endpoints
  - Create InternalBillingController in presentation layer
  - Delegate to SubscriptionApplicationService and UsageApplicationService
  - Implement GET /internal/billing/feature-access/{tenantId} endpoint
  - Implement GET /internal/billing/quota/{tenantId} endpoint
  - Implement POST /internal/billing/usage endpoint
  - Use request/response DTOs (not domain objects)
  - Add internal service authentication
  - Add OpenAPI/Swagger documentation
  - _Requirements: REQ-INT-005 through REQ-INT-009, REQ-API-025 through REQ-API-027, REQ-DDD-049_

## Phase 8: Error Handling and Validation

- [ ] 39. Implement exception hierarchy
  - Create BillingException base class
  - Create specific exceptions: SubscriptionNotFoundException, SubscriptionAlreadyExistsException, PlanNotFoundException, InvalidPlanTransitionException, QuotaExceededException, FeatureNotAvailableException, PaymentRequiredException, PaymentFailedException, InvoiceNotFoundException, InvalidPaymentMethodException, UsageLimitExceededException
  - Add error codes and details to exceptions
  - _Requirements: REQ-ERR-001, REQ-ERR-002, REQ-ERR-003, REQ-ERR-004, REQ-ERR-005, REQ-ERR-006, REQ-ERR-007, REQ-ERR-008, REQ-ERR-009, REQ-ERR-010, REQ-ERR-011_

- [ ] 40. Implement global exception handler
  - Create BillingExceptionHandler with @RestControllerAdvice in presentation layer
  - Implement handlers for all billing exceptions
  - Implement error response format with error code, message, timestamp, path, and details
  - Add specific error details for quota exceeded, feature not available, and payment failed
  - _Requirements: REQ-ERR-012, REQ-ERR-013, REQ-ERR-014, REQ-ERR-015, REQ-DDD-049_


- [ ] 41. Implement request validation
  - Add Bean Validation annotations to DTOs and request objects in presentation layer
  - Use specifications in domain layer for business rule validation
  - Implement validation at controller (input), application service (use case), and domain (invariants) layers
  - _Requirements: REQ-REL-008, REQ-DDD-043_

## Phase 9: Integration with Other Services

- [ ] 42. Update User Service for billing integration
  - Add subscriptionId, subscriptionStatus, and subscriptionPlanCode fields to Tenant entity
  - Add subscription context to JWT token generation (subscriptionStatus, subscriptionPlan, features)
  - Create internal endpoint for billing service to update tenant subscription status
  - Implement subscription status update logic
  - _Requirements: REQ-INT-001, REQ-INT-002, REQ-INT-003, REQ-INT-004_

- [ ] 43. Update Gateway Service for feature access control
  - Implement FeatureAccessFilter to check feature access based on subscription
  - Add feature access rejection with FEATURE_NOT_AVAILABLE error (HTTP 403)
  - Integrate with billing service internal endpoint for feature checks
  - _Requirements: REQ-INT-005, REQ-INT-006_

- [ ] 44. Update Gateway Service for quota enforcement
  - Implement QuotaEnforcementFilter to enforce API call quotas
  - Add asynchronous API call usage recording
  - Add quota exceeded rejection with QUOTA_EXCEEDED error (HTTP 429)
  - Integrate with billing service internal endpoint for quota checks
  - _Requirements: REQ-INT-007, REQ-INT-008, REQ-INT-009, REQ-USAGE-007_

- [ ] 45. Create email templates for billing notifications
  - Create email templates for: subscription_created, trial_ending, trial_ended, subscription_upgraded, subscription_downgraded, subscription_canceled, invoice_generated, payment_succeeded, payment_failed, payment_retry, subscription_past_due, subscription_expired
  - Integrate with Email Service for sending notifications
  - Implement email sending logic in NotificationConsumer
  - _Requirements: REQ-INT-010, REQ-INT-011, REQ-INT-012, REQ-INT-013, REQ-INT-014, REQ-INT-015_


## Phase 10: Scheduled Jobs and Background Tasks

**Scheduled Jobs & Async Processing:**

Scheduled jobs identify work to be done and publish events to queues for async processing. This pattern provides:
- **Scalability:** Jobs can run on one instance while consumers scale independently
- **Resilience:** Failed processing doesn't affect job scheduling
- **Monitoring:** Separate metrics for job execution vs work processing
- **Flexibility:** Easy to adjust consumer concurrency without changing job logic

**Job Pattern:** Job queries database → publishes events to queue → consumers process events asynchronously

- [ ] 46. Implement scheduled jobs
  - **TrialExpirationJob** - Check and convert expired trials (runs daily at 2 AM UTC)
    - **Async Pattern:** Query expired trials → publish TrialExpired events to queue → consumer processes conversions
    - **Why Async:** May affect thousands of subscriptions, shouldn't block job execution
    - Implement batch processing (100 subscriptions per batch)
    - Publish TrialExpired event for each expired trial
    - Consumer handles subscription status update and notification
  - **PaymentRetryJob** - Retry failed payments according to schedule (runs daily at 3 AM UTC)
    - **Async Pattern:** Query payments due for retry → publish PaymentRetry events to queue → consumer processes retries
    - **Why Async:** External payment provider calls, need retry logic, may take time
    - Implement retry schedule logic (Day 1, Day 3, Day 7, Day 14)
    - Publish PaymentRetry event for each payment to retry
    - Consumer handles payment provider call and result processing
  - **SubscriptionRenewalJob** - Generate invoices for upcoming renewals (runs daily at 1 AM UTC)
    - **Async Pattern:** Query subscriptions due for renewal → publish InvoiceGeneration events to queue → consumer generates invoices
    - **Why Async:** Invoice generation includes PDF creation, long-running operation
    - Query subscriptions renewing in next 3 days
    - Publish InvoiceGeneration event for each subscription
    - Consumer handles invoice creation, PDF generation, and email notification
  - **UsageResetJob** - Reset usage counters at period start (runs daily at 12 AM UTC)
    - **Async Pattern:** Query subscriptions starting new period → publish UsageReset events to queue → consumer resets counters
    - **Why Async:** May affect thousands of subscriptions, database-intensive
    - Implement batch processing (500 subscriptions per batch)
    - Publish UsageReset event for each subscription
    - Consumer handles usage counter reset and archival
  - **TrialReminderJob** - Send trial ending reminders (runs daily at 10 AM UTC)
    - **Async Pattern:** Query trials ending soon → publish TrialReminder events to queue → consumer sends emails
    - **Why Async:** Email sending is external service, shouldn't block job
    - Query trials ending in 3 days and 1 day
    - Publish TrialReminder event for each trial
    - Consumer handles email template rendering and sending
  - **General Job Patterns:**
    - Implement distributed locking to prevent duplicate job execution (use Redis)
    - Implement job execution logging and metrics
    - Implement job failure alerting
    - Implement job execution timeout (max 5 minutes per job)
    - Use pagination for large result sets
    - Publish events in batches to RabbitMQ (100 events per batch)
  - _Requirements: REQ-SUB-009, REQ-SUB-010, REQ-SUB-011, REQ-SUB-012, REQ-SUB-013, REQ-PAY-026, REQ-PAY-027, REQ-PAY-028, REQ-BIZ-021_

## Phase 11: Monitoring and Observability

- [ ] 47. Implement metrics and monitoring
  - Configure Prometheus metrics for business metrics (MRR, ARR, churn, trial conversion, ARPU, CLV)
  - Configure Prometheus metrics for operational metrics (API latency, payment success/failure rate, invoice generation time, usage recording latency, webhook processing time, database connection pool, cache hit rate, message queue depth)
  - Configure Prometheus metrics for error metrics (error rate by endpoint, payment failure rate, webhook failures, database errors)
  - Create Grafana dashboards for service health, billing metrics, revenue, and usage analytics
  - _Requirements: REQ-MAINT-014, REQ-MAINT-015, REQ-DEPLOY-012, REQ-DEPLOY-013, REQ-DEPLOY-014, REQ-DEPLOY-015, REQ-DEPLOY-016, REQ-DEPLOY-017, REQ-DEPLOY-018, REQ-DEPLOY-024, REQ-DEPLOY-025, REQ-DEPLOY-026, REQ-DEPLOY-027_

- [ ] 48. Implement structured logging
  - Configure structured JSON logging with timestamp, level, service, traceId, spanId, tenantId, userId, operation, message, and details
  - Implement appropriate log levels (ERROR, WARN, INFO, DEBUG)
  - Add correlation IDs for distributed tracing
  - _Requirements: REQ-MAINT-012, REQ-MAINT-013_

- [ ] 49. Implement alerting
  - Configure critical alerts: payment failure rate > 5%, API error rate > 1%, database connection pool > 80%, webhook processing failures, service health check failures
  - Configure warning alerts: response time p95 > 500ms, cache hit rate < 70%, message queue depth > 1000, trial conversion rate drops > 20%
  - _Requirements: REQ-MAINT-016, REQ-DEPLOY-019, REQ-DEPLOY-020, REQ-DEPLOY-021, REQ-DEPLOY-022, REQ-DEPLOY-023_


- [ ] 50. Implement health checks
  - Create BillingHealthIndicator to check database, Redis, RabbitMQ, and payment provider connectivity
  - Configure Spring Boot Actuator health endpoints
  - _Requirements: REQ-REL-013_

## Phase 12: Security Implementation

- [ ] 51. Implement security measures
  - Configure JWT token validation for authenticated endpoints
  - Implement role-based access control (TENANT_OWNER, BILLING_ADMIN, ADMIN, USER)
  - Implement tenant context extraction from JWT claims
  - Configure TLS 1.3 for all API communication
  - Implement rate limiting on public endpoints
  - Configure encryption at rest for sensitive data in PostgreSQL
  - Configure encryption for Redis tokens
  - _Requirements: REQ-SEC-001, REQ-SEC-002, REQ-SEC-003, REQ-SEC-004, REQ-SEC-005, REQ-SEC-006, REQ-SEC-007, REQ-SEC-008, REQ-SEC-009, REQ-SEC-010_

- [ ] 52. Implement audit logging
  - Create audit logging for all billing operations
  - Implement immutable audit records
  - Store audit logs in billing_events table
  - _Requirements: REQ-SEC-011, REQ-SEC-012, REQ-REL-007_

- [ ] 53. Implement GDPR compliance features
  - Implement data export functionality for billing data
  - Implement data deletion functionality for billing data
  - Implement data retention policies
  - _Requirements: REQ-SEC-013, REQ-SEC-014, REQ-SEC-015, REQ-SEC-016, REQ-COMP-001, REQ-COMP-002, REQ-COMP-003, REQ-COMP-004, REQ-COMP-005, REQ-COMP-006, REQ-COMP-007, REQ-COMP-008_

## Phase 13: Testing

- [ ] 54. Write unit tests for domain layer
  - Write unit tests for aggregates (Subscription, SubscriptionPlan, Invoice, Payment, PaymentMethod)
  - Write unit tests for domain services (ProrationCalculator, QuotaEnforcer, SubscriptionLifecycleManager, InvoiceGenerator)
  - Write unit tests for specifications (ActiveSubscriptionSpecification, QuotaExceededSpecification, ValidPlanTransitionSpecification, TrialEligibilitySpecification)
  - Write unit tests for factories (SubscriptionFactory, InvoiceFactory)
  - Write unit tests for value objects (PlanQuotas, ProrationResult, UsageSummary, UsageMetric, QuotaCheckResult)
  - Achieve minimum 80% code coverage
  - _Requirements: REQ-TEST-001, REQ-TEST-002, REQ-MAINT-004_

- [ ] 55. Write unit tests for application layer
  - Write unit tests for PlanApplicationService orchestration logic
  - Write unit tests for SubscriptionApplicationService orchestration logic
  - Write unit tests for PaymentApplicationService orchestration logic
  - Write unit tests for InvoiceApplicationService orchestration logic
  - Write unit tests for UsageApplicationService orchestration logic
  - Write unit tests for WebhookApplicationService orchestration logic
  - Mock domain services and repositories
  - Verify transaction management and DTO translation
  - _Requirements: REQ-TEST-002, REQ-MAINT-004_


- [ ]* 47.1 Write unit tests for proration calculations
  - Test proration calculation correctness for various scenarios
  - Test edge cases (same day changes, end of period, leap years)
  - Ensure 100% coverage for financial calculations
  - _Requirements: REQ-TEST-003_

- [ ]* 47.2 Write unit tests for subscription state transitions
  - Test all valid state transitions
  - Test invalid state transition rejection
  - Test state transition side effects
  - _Requirements: REQ-TEST-004_

- [ ]* 47.3 Write unit tests for quota enforcement logic
  - Test quota checking for various usage levels
  - Test quota exceeded scenarios
  - Test grace period logic (5% overage)
  - _Requirements: REQ-TEST-005_

- [ ]* 47.4 Write unit tests for payment processing logic
  - Test payment processing success and failure scenarios
  - Test payment retry logic
  - Test refund processing
  - Test idempotency
  - _Requirements: REQ-TEST-006_

- [ ]* 47.5 Write unit tests for invoice generation logic
  - Test invoice generation for regular billing periods
  - Test proration invoice generation
  - Test invoice line item calculations
  - Test invoice total calculations
  - Ensure 100% coverage for financial calculations
  - _Requirements: REQ-TEST-007_

- [ ] 48. Write integration tests
  - Set up Testcontainers for PostgreSQL, Redis, and RabbitMQ
  - Write integration tests for complete subscription lifecycle (create → upgrade → cancel)
  - Write integration tests for payment processing with retry logic
  - Write integration tests for invoice generation and payment
  - Write integration tests for usage recording and quota enforcement
  - Write integration tests for webhook processing with WireMock
  - Write integration tests for multi-tenant data isolation
  - _Requirements: REQ-TEST-008, REQ-TEST-009, REQ-TEST-010, REQ-TEST-011, REQ-TEST-012, REQ-MAINT-005_


- [ ]* 49. Write property-based tests
  - Set up jqwik property-based testing framework
  - Configure property tests to run minimum 100 iterations
  - **Property 1: Tenant Isolation for Subscriptions** - For any two distinct tenants, querying subscriptions for one tenant should never return subscriptions belonging to the other tenant - **Validates: Requirements 2.2**
  - **Property 2: Tenant Isolation for Usage Records** - For any two distinct tenants, querying usage records for one tenant should never return usage records belonging to the other tenant - **Validates: Requirements 2.3**
  - **Property 3: Tenant Isolation for Invoices** - For any two distinct tenants, querying invoices for one tenant should never return invoices belonging to the other tenant - **Validates: Requirements 2.4**
  - **Property 4: Tenant Isolation for Payment Methods** - For any two distinct tenants, querying payment methods for one tenant should never return payment methods belonging to the other tenant - **Validates: Requirements 2.5**
  - **Property 5: Tenant Isolation for Billing Events** - For any two distinct tenants, querying billing events for one tenant should never return billing events belonging to the other tenant - **Validates: Requirements 2.6**
  - **Property 6: Plan Creation Persistence** - For any valid subscription plan, creating the plan and then retrieving it should return a plan with all the same field values - **Validates: Requirements 4.1**
  - **Property 7: Plan Features Round-Trip** - For any valid JSON feature structure, storing it in a plan's features field and then retrieving the plan should return the same feature structure - **Validates: Requirements 4.5**
  - **Property 8: Plan Quotas Round-Trip** - For any valid quota structure, storing it in a plan and then retrieving the plan should return the same quota structure - **Validates: Requirements 4.6**
  - **Property 9: Trial Period Persistence** - For any valid trial period duration in days, setting it on a plan and then retrieving the plan should return the same trial period value - **Validates: Requirements 4.7**
  - **Property 10: Plan Visibility Filtering** - For any plan marked as private, it should not appear in the public plan listing endpoint results - **Validates: Requirements 4.8**
  - **Property 11: Plan Update Persistence** - For any existing plan and valid updates, updating the plan and then retrieving it should return the updated values - **Validates: Requirements 5.1**
  - **Property 12: Price Grandfathering** - For any subscription created before a plan price change, the subscription should maintain the original price even after the plan price is updated - **Validates: Requirements 5.2**
  - **Property 13: Archived Plan Subscription Prevention** - For any archived or deactivated plan, attempting to create a new subscription with that plan should fail - **Validates: Requirements 5.4**
  - **Property 14: Public Plan Listing Filtering** - For any set of plans, the public listing endpoint should return only plans that are both active and public - **Validates: Requirements 6.1**
  - **Property 15: Plan Listing Completeness** - For any plan in the listing response, the response should include features, quotas, and pricing information - **Validates: Requirements 6.2**


  - **Property 16: Plan Listing Sort Order** - For any set of plans with different tiers and prices, the listing response should be sorted first by tier then by price - **Validates: Requirements 6.3**
  - **Property 17: Billing Cycle Filtering** - For any billing cycle filter value, the filtered plan listing should return only plans with that billing cycle - **Validates: Requirements 6.4**
  - **Property 18: Admin Plan Listing Completeness** - For any set of plans including private and archived plans, the admin listing endpoint should return all plans - **Validates: Requirements 6.5**
  - **Property 19: Proration Calculation Correctness** - For any subscription upgrade or downgrade, the proration calculation should satisfy the formula and all amounts should be non-negative - **Validates: Subscription upgrade/downgrade requirements**
  - **Property 20: Invoice Total Consistency** - For any invoice, the total should equal the sum of all line item amounts plus tax - **Validates: Invoice generation requirements**
  - **Property 21: Usage Aggregation Commutativity** - For any set of usage records, the aggregated total usage should be the same regardless of processing order - **Validates: Usage tracking requirements**
  - **Property 22: Quota Enforcement Consistency** - For any tenant and metric type, if current usage equals or exceeds quota limit, operations that increase usage should be rejected - **Validates: Quota enforcement requirements**
  - **Property 23: Subscription Status Transition Validity** - For any subscription, all status transitions should follow valid state machine paths - **Validates: Subscription lifecycle requirements**
  - **Property 24: Payment Idempotency** - For any payment request with the same idempotency key, processing multiple times should result in only one payment - **Validates: Payment processing requirements**
  - **Property 25: Webhook Idempotency** - For any webhook event with the same event ID, processing multiple times should result in the same system state - **Validates: Webhook handling requirements**

- [ ]* 50. Write architecture tests for DDD compliance
  - Set up ArchUnit for architecture testing
  - Write tests to verify layered architecture (Domain → Application → Infrastructure → Presentation)
  - Write tests to verify domain layer has no dependencies on other layers
  - Write tests to verify application layer only depends on domain layer
  - Write tests to verify infrastructure layer depends on domain and application
  - Write tests to verify presentation layer depends on application
  - Write tests to verify aggregates are only accessed through aggregate roots
  - Write tests to verify repositories are only accessed by application services
  - Write tests to verify domain services are stateless
  - Write tests to verify value objects are immutable
  - Write tests to verify no circular dependencies exist
  - Write tests to verify package structure follows DDD conventions
  - _Requirements: REQ-TEST-013, REQ-TEST-014, REQ-TEST-015, REQ-MAINT-006, REQ-DDD-050, REQ-DDD-051, REQ-DDD-063_

- [ ]* 51. Write security tests
  - Write tests for authentication and authorization
  - Write tests for tenant isolation
  - Write tests for payment data security
  - Write tests for webhook signature verification
  - Write tests for SQL injection prevention
  - Write tests for XSS prevention
  - _Requirements: REQ-TEST-023, REQ-TEST-024, REQ-TEST-025, REQ-TEST-026, REQ-TEST-027, REQ-TEST-028_


## Phase 14: Documentation and Deployment

- [ ] 56. Create service documentation
  - Write comprehensive README with service setup instructions
  - Document API endpoints with examples
  - Create runbooks for common operations
  - Document architecture decisions in ADRs
  - Document deployment procedures
  - Document disaster recovery procedures
  - _Requirements: REQ-MAINT-007, REQ-MAINT-008, REQ-MAINT-009, REQ-MAINT-010, REQ-MAINT-011, REQ-DEPLOY-034_

- [ ] 57. Create Docker and Kubernetes configurations
  - Create Dockerfile for billing service
  - Create docker-compose.yaml for local development
  - Create Kubernetes deployment manifests
  - Create Helm chart for billing service
  - Configure environment-specific values (local, staging, production)
  - _Requirements: REQ-DEPLOY-001, REQ-DEPLOY-002, REQ-DEPLOY-003, REQ-DEPLOY-004, REQ-DEPLOY-005, REQ-DEPLOY-006, REQ-DEPLOY-007_

- [ ] 58. Configure database backup and recovery
  - Configure daily full database backups
  - Configure hourly incremental database backups
  - Set up 30-day backup retention
  - Implement weekly backup verification via restore test
  - Document disaster recovery procedures
  - _Requirements: REQ-DEPLOY-009, REQ-DEPLOY-028, REQ-DEPLOY-029, REQ-DEPLOY-030, REQ-DEPLOY-031, REQ-DEPLOY-032, REQ-DEPLOY-033, REQ-DEPLOY-035_

## Phase 15: Data Migration

- [ ] 59. Create migration scripts
  - Create script to create subscription plans in billing service
  - Create script to map existing tenants to appropriate plans
  - Create script to create subscription records for all tenants
  - Create script to import payment methods (if applicable)
  - Create script to migrate invoice history (if applicable)
  - Create script to validate data integrity after migration
  - _Requirements: REQ-MIG-001, REQ-MIG-002, REQ-MIG-003, REQ-MIG-004, REQ-MIG-005, REQ-MIG-006, REQ-MIG-007, REQ-MIG-008, REQ-MIG-009, REQ-MIG-010, REQ-MIG-011, REQ-MIG-012, REQ-MIG-013_


- [ ] 60. Execute migration
  - Execute pre-migration validation
  - Run migration scripts in staging environment
  - Validate migration results
  - Update tenant records in User Service with subscription references
  - Enable billing service routing in Gateway Service
  - Monitor for issues post-migration
  - _Requirements: REQ-MIG-014, REQ-MIG-015_

- [ ] 61. Prepare rollback plan
  - Document rollback procedures
  - Create rollback scripts to disable billing service routing
  - Create rollback scripts to revert tenant subscription references
  - Test rollback procedures in staging
  - _Requirements: REQ-MIG-016, REQ-MIG-017, REQ-MIG-018, REQ-MIG-019_

## Phase 16: Final Integration and Testing

- [ ] 62. Checkpoint - Ensure all tests pass
  - Run all unit tests and verify 80%+ coverage
  - Run all integration tests
  - Run all property-based tests
  - Run all architecture tests
  - Run all security tests
  - Fix any failing tests
  - Ask the user if questions arise

- [ ] 63. End-to-end testing
  - Test complete subscription lifecycle from creation to cancellation
  - Test payment processing with Stripe test mode
  - Test invoice generation and PDF download
  - Test usage tracking and quota enforcement
  - Test webhook processing
  - Test admin operations
  - Test multi-tenant isolation
  - _Requirements: REQ-TEST-011_

- [ ] 64. Performance testing
  - Test 1000 concurrent subscription creations
  - Test 10,000 usage records per second
  - Test 100 payments per minute
  - Test invoice generation for 1000 subscriptions
  - Verify no memory leaks
  - Verify database connection pool stability
  - Verify no deadlocks occur
  - _Requirements: REQ-TEST-016, REQ-TEST-017, REQ-TEST-018, REQ-TEST-019, REQ-TEST-020, REQ-TEST-021, REQ-TEST-022, REQ-PERF-001, REQ-PERF-002, REQ-PERF-003, REQ-PERF-004, REQ-PERF-005, REQ-PERF-006, REQ-PERF-007, REQ-PERF-008, REQ-PERF-009, REQ-PERF-010, REQ-PERF-011, REQ-PERF-012_


- [ ] 65. Final checkpoint - Production readiness
  - Verify all monitoring and alerting is configured
  - Verify all documentation is complete
  - Verify all security measures are in place
  - Verify backup and recovery procedures are tested
  - Verify disaster recovery procedures are documented
  - Conduct final code review
  - Obtain stakeholder approval for production deployment
  - Ask the user if questions arise

---

## Notes

- Tasks marked with "*" are optional and can be skipped for faster MVP delivery
- Each task includes references to specific requirements from the requirements document
- **The implementation follows tactical Domain-Driven Design (DDD) principles:**
  - Layered architecture: Domain → Application → Infrastructure → Presentation
  - Aggregates with clear boundaries and invariants
  - Domain services for cross-aggregate business logic
  - Specifications for reusable business rules
  - Factories for complex object construction
  - Domain events for eventual consistency
  - Anti-corruption layer for external integrations
  - Repository pattern for persistence abstraction
- The implementation follows an incremental approach, building from domain layer outward
- Testing is integrated throughout the implementation, not left until the end
- Property-based tests validate universal correctness properties across all inputs
- Architecture tests enforce DDD layering and dependency rules
- The plan assumes familiarity with Spring Boot, PostgreSQL, Redis, RabbitMQ, and DDD patterns
- External payment provider integration (Stripe, PayPal) requires test accounts and API keys
- Multi-tenant schema management requires careful database migration planning
- Performance testing should be conducted in an environment similar to production
