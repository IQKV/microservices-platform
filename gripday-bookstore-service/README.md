# 📚 Gripday Bookstore

> Reference implementation for building microservices with Spring Boot, demonstrating best practices for catalog and inventory management.

## Business Purpose

A bookstore management system that handles:

- **Book Catalog** - Maintain a searchable collection of books with details (title, author, ISBN, price, category)
- **Inventory Tracking** - Monitor stock levels, reserved quantities, and availability in real-time
- **Search & Discovery** - Enable customers to find books by multiple criteria (title, author, category, price range)
- **Access Control** - Clear separation between public browsing endpoints and admin-only management operations
- **Inventory Operations** - Reserve, release, and adjust inventory for order fulfillment workflows

## Overview

This is an exemplary Spring Boot microservice that showcases how to build a production-ready bookstore application. It demonstrates patterns and practices for developing similar microservices in your organization.

## What It Demonstrates

### 📖 Domain-Driven Design

- Clean separation of presentation, domain, and infrastructure layers
- Entity modeling with JPA (Book, Category, Inventory)
- Rich domain logic with business rules
- DTO pattern for API contracts

### 🔍 Advanced Query Patterns

- Multi-criteria search with dynamic filtering
- Fuzzy search implementation
- Full-text search capabilities
- Pagination and sorting
- Query optimization with caching

### 📦 Inventory Management Patterns

- Real-time stock tracking
- Reserved quantity handling for order workflows
- Low stock threshold monitoring
- Bulk update operations
- Availability calculation logic

### 🎯 Observability & Monitoring

- Structured logging with correlation IDs
- Prometheus metrics integration
- Custom business metrics
- Health checks and actuator endpoints
- Audit logging for critical operations

### 🔐 Security Implementation

- JWT-based authentication
- Role-based access control (RBAC)
- Clear separation of public and admin endpoints
- User context extraction and propagation
- Audit trail for administrative actions
- Admin operations require ADMIN or SUPERADMIN role

## Architecture Patterns

### Key Design Patterns

- Repository pattern for data access
- Service layer for business logic
- DTO pattern for API boundaries
- Cache-aside pattern with Redis
- Optimistic locking for concurrency

### API Design

- RESTful endpoints with proper HTTP methods
- Clear endpoint organization (public vs admin subpackages)
- Versioning support (URL, header, content negotiation)
- OpenAPI/Swagger documentation with security schemes
- Problem Details (RFC 7807) for errors
- Pagination with Spring Data

### Endpoint Organization Pattern

The service follows a clear separation between public and administrative endpoints:

**Public Resources**

- `BookResource` - Public catalog browsing and search operations
- `InventoryResource` - Public inventory information and availability checks
- `ApiInfoResource` - API version and capability information
- No authentication required, accessible to all users

**Admin Resources**

- `BookManagementResource` - Administrative book CRUD operations
- `InventoryManagementResource` - Administrative inventory management
- Requires JWT authentication with ADMIN or SUPERADMIN role
- All operations are audited with user context

This pattern provides:

- Clear security boundaries at the package level
- Easy-to-configure gateway routing rules
- Simplified access control policies
- Better code organization and maintainability

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
