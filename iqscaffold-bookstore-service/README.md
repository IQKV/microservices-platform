# 📚 IQ Scaffold Bookstore Service

> Reference implementation demonstrating Domain-Driven Design, tactical patterns, and production-ready microservice architecture with Spring Boot 3.x and Java 21.

## Business Purpose

A bookstore management system that handles:

- **Book Catalog** - Maintain a searchable collection of books with details (title, author, ISBN, price, category)
- **Inventory Tracking** - Monitor stock levels, reserved quantities, and availability in real-time
- **Search & Discovery** - Enable customers to find books by multiple criteria (title, author, category, price range)
- **Access Control** - Clear separation between public browsing endpoints and admin-only management operations
- **Inventory Operations** - Reserve, release, and adjust inventory for order fulfillment workflows

## Overview

This is an exemplary Spring Boot microservice showcasing production-ready patterns for building domain-rich applications. It demonstrates tactical Domain-Driven Design patterns, value objects, rich domain models, and modern Java 21 features in a real-world catalog and inventory management context.

## What It Demonstrates

### 📖 Domain-Driven Design (Tactical Patterns)

**Value Objects**

- `ISBN` - Self-validating ISBN-10/ISBN-13 with format validation and normalization
- `Money` - Immutable monetary amounts with currency validation and arithmetic operations
- `BookId` - Type-safe identifier wrapper preventing primitive obsession

**Aggregate Roots**

- `Book` - Rich domain model with business methods (markAsAvailable, updateDetails, changeCategory)
- `Category` - Category aggregate with book relationship management
- `Inventory` - Inventory aggregate with reservation and availability logic

**Domain Services**

- `DuplicateIsbnChecker` - Ensures ISBN uniqueness across catalog
- `BookAvailabilityChecker` - Complex availability calculation logic

**Factory Methods**

- `Book.create()` - Encapsulates creation logic with invariant validation
- `Inventory.create()` - Ensures proper inventory initialization

**Business Logic in Domain**

- `Book.canBeSold()` - Combines availability and stock checks
- `Book.isInPriceRange()` - Price range validation
- `Inventory.reserve()` - Reservation with insufficient stock protection
- `Inventory.getAvailableQuantity()` - Calculated property (quantity - reserved)

### 🔍 Advanced Query & Search Patterns

**Multi-Criteria Search**

- Dynamic filtering with `BookSearchQuery` record
- Composite queries (title + author + category + price range + availability)
- Pagination with Spring Data
- Query optimization with strategic caching

**Filter Pattern Implementation**

- `AuthorFilter`, `CategoryFilter`, `PriceRangeFilter`, `AvailabilityFilter`
- Composable filters for flexible search
- `FilterMetadata` for search result context

**Response Builders**

- `BookCatalogResponseBuilder` - Enriched responses with metadata
- `SearchMetadata`, `PaginationMetadata` - Structured search context

### 📦 Inventory Management Patterns

**Reservation System**

- Reserve inventory for pending orders
- Release reserved inventory on cancellation
- Prevent overselling with optimistic locking
- Bulk operations for efficiency

**Stock Monitoring**

- Low stock threshold alerts
- Out-of-stock detection
- Available quantity calculation (quantity - reserved)
- Real-time inventory statistics

### 🎯 Observability & Monitoring

**Structured Logging**

- Correlation ID propagation via `CorrelationIdFilter`
- User context in MDC via `UserContextMdcFilter`
- JSON logging with Logback
- Audit logging for admin operations via `AuditLogger`

**Metrics & Monitoring**

- Custom business metrics via `BookstoreMetrics`
- Prometheus integration
- Operation timing (book creation, search, inventory updates)
- Cache hit/miss tracking

**Health & Actuator**

- Database health checks
- Redis health checks
- Custom health indicators
- Graceful shutdown support

### 🔐 Security Implementation

**JWT Authentication**

- JWT validation via `JwtAuthenticationFilter`
- User context extraction via `UserContextExtractor`
- RSA256 signature verification
- Token-based stateless authentication

**Authorization**

- Method-level security with `@PreAuthorize`
- Role-based access control (ADMIN, SUPERADMIN)
- Public vs admin endpoint separation
- Audit trail for administrative actions

**Security Configuration**

- CORS configuration per environment
- Secure headers (HSTS, CSP, X-Frame-Options)
- CSRF protection
- OAuth2 Resource Server integration

## Architecture Patterns

### Modular Package Structure

The service is organized into bounded contexts following DDD principles:

```
com.iqscaffold.bookstore/
├── catalog/              # Book catalog bounded context
│   ├── Book.java         # Aggregate root with rich domain logic
│   ├── Category.java     # Category aggregate
│   ├── CatalogApplicationService.java  # Use case orchestration
│   ├── BookRepository.java             # Data access
│   ├── BookResource.java               # Public REST API
│   ├── BookManagementResource.java     # Admin REST API
│   └── *Filter.java      # Search filter implementations
├── inventory/            # Inventory bounded context
│   ├── Inventory.java    # Aggregate root
│   ├── InventoryApplicationService.java
│   ├── InventoryRepository.java
│   ├── InventoryResource.java
│   └── InventoryManagementResource.java
└── shared/               # Shared kernel
    ├── ISBN.java         # Value object
    ├── Money.java        # Value object
    ├── BookId.java       # Value object
    ├── AggregateRoot.java # Base class
    ├── UserContext.java   # Security context
    └── *Config.java       # Infrastructure configuration
```

### Key Design Patterns

**Domain Layer**

- Aggregate pattern with clear boundaries
- Value objects for type safety (ISBN, Money)
- Factory methods for object creation
- Domain services for cross-aggregate logic
- Rich domain models (not anemic)

**Application Layer**

- Application services orchestrate use cases
- DTO pattern for API boundaries (Java records)
- Command pattern for write operations (CreateBookCommand, UpdateBookCommand)
- Query pattern for read operations (BookSearchQuery)

**Infrastructure Layer**

- Repository pattern for data access
- Cache-aside pattern with Redis
- Optimistic locking for concurrency
- Correlation ID propagation
- Structured logging with MDC

### API Design

**RESTful Principles**

- Proper HTTP methods (GET, POST, PUT, DELETE)
- Resource-oriented URLs
- HTTP status codes (200, 201, 204, 400, 404, 409)
- Content negotiation
- HATEOAS-ready structure

**Versioning Strategy**

- URL versioning (`/api/v1/bookstore/...`)
- Header-based versioning support via `ApiVersionInterceptor`
- Content negotiation versioning
- Deprecation notices via `@ApiDeprecationNotice`

**Documentation**

- OpenAPI 3.0 with SpringDoc
- Swagger UI for interactive testing
- Security schemes documented
- Request/response examples
- Error response documentation (RFC 7807 Problem Details)

### Endpoint Organization Pattern

**Public Resources** (No Authentication)

- `BookResource` - Catalog browsing and search
- `InventoryResource` - Inventory information and availability
- `ApiInfoResource` - API version and capabilities

**Admin Resources** (ADMIN/SUPERADMIN Required)

- `BookManagementResource` - Book CRUD operations
- `InventoryManagementResource` - Inventory management

**Benefits:**

- Clear security boundaries at package level
- Easy gateway routing configuration
- Simplified access control policies
- Better code organization and maintainability
- Testability (separate test suites for public vs admin)

## Technical Highlights

### Performance Optimization

- Multi-level caching strategy (Redis)
- Database query optimization
- Lazy loading with JPA
- Connection pooling
- Async processing support

### Data Management

- Liquibase for database migrations
- PostgreSQL with proper indexing
- Transaction management
- Soft deletes for data retention

### Testing Approach

- Unit tests with JUnit 5
- Integration tests with Testcontainers
- Security testing
- Architecture tests with ArchUnit

### Operational Features

- Docker containerization
- Kubernetes-ready configuration
- Environment-specific profiles (local, staging, production)
- Graceful shutdown
- Structured JSON logging

## Use Cases Implemented

### Catalog Management

- Create, read, update, delete books
- Category organization
- ISBN uniqueness validation
- Availability status management

### Search & Discovery

- Search by title, author, category
- Price range filtering
- Availability filtering
- Recent books and popular items

### Inventory Operations

- Stock level management
- Reservation system for orders
- Low stock alerts
- Bulk inventory updates
- Available quantity calculation

### Administrative Functions

- Role-based operations (ADMIN/SUPERADMIN)
- Separated admin endpoints under `/admin` path
- Comprehensive audit logging with user context
- Bulk operations for efficiency
- Metrics collection
- Health monitoring

## API Examples

### Public Endpoints (No Authentication Required)

**Book Catalog Browsing**

- `GET /api/v1/bookstore/books` - Browse catalog with filters (title, author, category, price range)
- `GET /api/v1/bookstore/books/{id}` - Get book details by ID
- `GET /api/v1/bookstore/books/isbn/{isbn}` - Get book by ISBN
- `GET /api/v1/bookstore/books/available` - List available books only
- `GET /api/v1/bookstore/books/in-stock` - List books currently in stock

**Search Operations**

- `GET /api/v1/bookstore/books/search` - Advanced search with multiple criteria
- `GET /api/v1/bookstore/books/search/title?title={query}` - Search by title
- `GET /api/v1/bookstore/books/search/author?author={query}` - Search by author
- `GET /api/v1/bookstore/books/search/category?category={name}` - Search by category
- `GET /api/v1/bookstore/books/search/price-range?minPrice={min}&maxPrice={max}` - Search by price range

**Inventory Information**

- `GET /api/v1/bookstore/inventory/{bookId}` - Get inventory details
- `GET /api/v1/bookstore/inventory/{bookId}/availability?quantity={qty}` - Check availability
- `GET /api/v1/bookstore/inventory/low-stock` - List low stock items
- `GET /api/v1/bookstore/inventory/out-of-stock` - List out of stock items
- `GET /api/v1/bookstore/inventory/stats/total-count` - Total inventory count
- `GET /api/v1/bookstore/inventory/stats/reserved-count` - Total reserved count
- `GET /api/v1/bookstore/inventory/stats/low-stock-count` - Low stock item count
- `GET /api/v1/bookstore/inventory/stats/out-of-stock-count` - Out of stock item count

### Admin Endpoints (Requires ADMIN or SUPERADMIN Role)

**Book Management** (`/api/v1/bookstore/admin/books`)

- `POST /api/v1/bookstore/admin/books` - Create new book
- `PUT /api/v1/bookstore/admin/books/{id}` - Update book details
- `DELETE /api/v1/bookstore/admin/books/{id}` - Remove book from catalog

**Inventory Management** (`/api/v1/bookstore/admin/inventory`)

- `PUT /api/v1/bookstore/admin/inventory/{bookId}` - Update inventory quantity and threshold
- `POST /api/v1/bookstore/admin/inventory/bulk-update` - Bulk update multiple inventories
- `POST /api/v1/bookstore/admin/inventory/{bookId}/reserve?quantity={qty}` - Reserve inventory for orders
- `POST /api/v1/bookstore/admin/inventory/{bookId}/release?quantity={qty}` - Release reserved inventory
- `POST /api/v1/bookstore/admin/inventory/{bookId}/adjust?adjustment={qty}` - Adjust inventory (positive or negative)

### API Information

- `GET /api/v1/bookstore/version` - API version and capabilities information

### Monitoring Endpoints

- `/actuator/health` - Health status
- `/actuator/metrics` - Application metrics
- `/actuator/prometheus` - Prometheus metrics
- `/swagger-ui.html` - Interactive API documentation

## Learning Points

This implementation serves as a reference for:

- Building microservices with Spring Boot 3.x
- Implementing clean architecture principles
- Designing RESTful APIs
- Managing database migrations
- Implementing caching strategies
- Adding observability and monitoring
- Securing REST APIs
- Writing maintainable, testable code

## Adapting for Your Domain

This bookstore example can be adapted for similar catalog and inventory scenarios:

- Product catalogs (e-commerce)
- Asset management systems
- Equipment rental services
- Library management systems
- Warehouse inventory tracking

The patterns demonstrated here apply to any domain requiring catalog management, inventory tracking, search capabilities, and role-based access control.

---

**Use this as a blueprint** for building your own microservices with similar requirements. The code demonstrates production-ready patterns that can be adapted to your specific business domain.
