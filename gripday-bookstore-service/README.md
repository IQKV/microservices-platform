# 📚 Gripday Bookstore

> Reference implementation for building microservices with Spring Boot, demonstrating best practices for catalog and inventory management.

## Business Purpose

A bookstore management system that handles:

- **Book Catalog** - Maintain a searchable collection of books with details (title, author, ISBN, price, category)
- **Inventory Tracking** - Monitor stock levels, reserved quantities, and availability in real-time
- **Search & Discovery** - Enable customers to find books by multiple criteria (title, author, category, price range)
- **Access Control** - Separate public browsing from administrative operations (adding books, updating inventory)

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
- Method-level security with @PreAuthorize
- User context extraction and propagation
- Audit trail for administrative actions

## Architecture Patterns

### Key Design Patterns

- Repository pattern for data access
- Service layer for business logic
- DTO pattern for API boundaries
- Cache-aside pattern with Redis
- Optimistic locking for concurrency

### API Design

- RESTful endpoints with proper HTTP methods
- Versioning support (URL, header, content negotiation)
- OpenAPI/Swagger documentation
- Problem Details (RFC 7807) for errors
- Pagination with Spring Data

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

- Role-based operations
- Audit logging
- Metrics collection
- Health monitoring

## API Examples

### Public Endpoints

- `GET /api/v1/bookstore/books` - Browse catalog with filters
- `GET /api/v1/bookstore/books/{id}` - Get book details
- `GET /api/v1/bookstore/books/search` - Advanced search
- `GET /api/v1/bookstore/books/available` - Available books only

### Admin Endpoints (Requires Authentication)

- `POST /api/v1/bookstore/books` - Create new book
- `PUT /api/v1/bookstore/books/{id}` - Update book
- `DELETE /api/v1/bookstore/books/{id}` - Remove book
- `PUT /api/v1/bookstore/inventory/{bookId}` - Update inventory

### Monitoring Endpoints

- `/actuator/health` - Health status
- `/actuator/metrics` - Application metrics
- `/actuator/prometheus` - Prometheus metrics
- `/swagger-ui.html` - API documentation

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
