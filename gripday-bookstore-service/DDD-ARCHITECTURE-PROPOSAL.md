# DDD Architecture Proposal for Gripday Bookstore Service

## Current Architecture Analysis

### Current Structure

```
org.gripday.bookstore/
├── catalog/          (24 classes - mixed concerns)
├── inventory/        (9 classes - mixed concerns)
└── shared/           (26 classes - infrastructure & cross-cutting)
```

### Issues Identified

1. **Anemic Domain Model**: Entities (Book, Category, Inventory) are just data holders with getters/setters
2. **Service Layer Doing Too Much**: Business logic scattered in service classes
3. **Mixed Concerns**: DTOs, entities, filters, exceptions all in same package
4. **No Clear Bounded Contexts**: Catalog and Inventory are separate but tightly coupled
5. **Infrastructure Mixed with Domain**: Repositories, REST resources alongside domain logic
6. **Shared Package Anti-pattern**: 26 infrastructure classes in "shared" - unclear responsibilities

---

## Proposed DDD Architecture

### Package Structure Overview

```
org.gripday.bookstore/
├── catalog/                          (Catalog Bounded Context)
│   ├── domain/
│   │   ├── model/                    (Entities & Value Objects)
│   │   ├── repository/               (Repository Interfaces)
│   │   └── service/                  (Domain Services)
│   ├── application/                  (Application Services & Use Cases)
│   └── infrastructure/
│       ├── persistence/              (JPA Repositories)
│       └── web/                      (REST Controllers)
│
├── inventory/                        (Inventory Bounded Context)
│   ├── domain/
│   │   ├── model/
│   │   ├── repository/
│   │   └── service/
│   ├── application/
│   └── infrastructure/
│       ├── persistence/
│       └── web/
│
└── common/                           (Shared Kernel)
    ├── domain/                       (Shared domain concepts)
    ├── application/                  (Shared application utilities)
    └── infrastructure/               (Technical infrastructure)
        ├── config/                   (Configuration classes)
        ├── security/                 (Security infrastructure)
        ├── monitoring/               (Metrics, logging)
        └── web/                      (Web infrastructure)
```

---

## Detailed Refactoring Plan

### 1. Catalog Bounded Context

#### catalog/domain/model/

**Purpose**: Core business entities with behavior

**Classes to create/refactor**:

- `Book` (Entity) - Add business methods
  - `updateDetails(title, author, description, price)`
  - `changeCategory(Category)`
  - `markAsUnavailable()`
  - `isInPriceRange(minPrice, maxPrice)`
- `Category` (Entity) - Add business methods
  - `addBook(Book)` ✓ (already exists)
  - `removeBook(Book)` ✓ (already exists)
- `BookId` (Value Object) - NEW
  - Wrap Long id for type safety
- `ISBN` (Value Object) - NEW
  - Validate ISBN format
  - Ensure uniqueness constraint
- `Money` (Value Object) - NEW
  - Replace BigDecimal for price
  - Add currency support
  - Business rules for pricing

**Move from current catalog/**:

- `Book.java` → `catalog/domain/model/Book.java`
- `Category.java` → `catalog/domain/model/Category.java`

#### catalog/domain/repository/

**Purpose**: Repository interfaces (not implementations)

**Classes to move**:

- `BookRepository.java` → `catalog/domain/repository/BookRepository.java` (interface only)
- `CategoryRepository.java` → `catalog/domain/repository/CategoryRepository.java`

**Simplify**: Remove query methods, keep only:

- `findById(BookId)`
- `save(Book)`
- `delete(Book)`
- `findByIsbn(ISBN)`

#### catalog/domain/service/

**Purpose**: Domain services for complex business logic

**Classes to create**:

- `BookAvailabilityChecker` - NEW
  - Check if book can be sold
  - Coordinate with inventory context
- `DuplicateIsbnChecker` - NEW
  - Validate ISBN uniqueness
  - Extract from CatalogService

#### catalog/application/

**Purpose**: Application services, use cases, DTOs

**Classes to create/move**:

- `CatalogApplicationService` (rename from `CatalogService`)
  - Orchestrate use cases
  - Transaction boundaries
  - DTO conversions
- `SearchApplicationService` (rename from `SearchService`)
  - Handle search use cases
- `dto/` subfolder:
  - `BookDto.java`
  - `CategoryDto.java`
  - `CreateBookCommand.java` (rename from CreateBookRequest)
  - `UpdateBookCommand.java` (rename from UpdateBookRequest)
  - `BookSearchQuery.java` (rename from BookSearchCriteria)
  - `BookCatalogResponse.java`
- `usecase/` subfolder (optional, for clarity):
  - `CreateBookUseCase`
  - `UpdateBookUseCase`
  - `SearchBooksUseCase`

#### catalog/infrastructure/persistence/

**Purpose**: JPA implementations, query specifications

**Classes to create/move**:

- `JpaBookRepository` - NEW (implements domain BookRepository)
  - Extends Spring Data JpaRepository
  - All complex queries here
- `JpaCategoryRepository` - NEW
- `BookSpecifications` - NEW
  - Query specifications for filtering
  - Extract from current BookRepository queries

#### catalog/infrastructure/web/

**Purpose**: REST controllers, request/response models

**Classes to move**:

- `BookResource.java` → `BookController.java` (rename)
- `BookManagementResource.java` → `BookManagementController.java`

**Exceptions to move**:

- `BookNotFoundException.java`
- `CategoryNotFoundException.java`
- `DuplicateIsbnException.java`

---

### 2. Inventory Bounded Context

#### inventory/domain/model/

**Classes to refactor**:

- `Inventory` (Entity) - Already has good business methods ✓
  - `reserveQuantity()` ✓
  - `releaseReservedQuantity()` ✓
  - `adjustQuantity()` ✓
  - `canReserve()` ✓
  - `isLowStock()` ✓
- `StockLevel` (Value Object) - NEW
  - Wrap quantity, reservedQuantity
  - Business rules for stock levels
- `StockThreshold` (Value Object) - NEW
  - Wrap lowStockThreshold
  - Validation logic

**Move**:

- `Inventory.java` → `inventory/domain/model/Inventory.java`

#### inventory/domain/repository/

- `InventoryRepository.java` → `inventory/domain/repository/InventoryRepository.java`

#### inventory/domain/service/

**Classes to create**:

- `StockReservationService` - NEW
  - Handle reservation logic
  - Coordinate with order context (future)

#### inventory/application/

**Classes to move/rename**:

- `InventoryService` → `InventoryApplicationService`
- `dto/` subfolder:
  - `InventoryDto.java`
  - `UpdateInventoryCommand.java` (rename from UpdateInventoryRequest)
  - `BulkInventoryCommand.java` (rename from BulkInventoryRequest)

#### inventory/infrastructure/persistence/

- `JpaInventoryRepository` - NEW

#### inventory/infrastructure/web/

- `InventoryResource.java` → `InventoryController.java`
- `InventoryManagementResource.java` → `InventoryManagementController.java`

**Exceptions**:

- `InsufficientInventoryException.java`

---

### 3. Common (Shared Kernel)

#### common/domain/

**Purpose**: Shared domain concepts across contexts

**Classes to create**:

- `AggregateRoot` - Base class for aggregates
- `Entity` - Base class for entities
- `ValueObject` - Base class for value objects
- `DomainEvent` - Base for domain events (future)

#### common/application/

**Purpose**: Shared application utilities

**Classes to move from shared/**:

- `UserContext.java`
- `UserContextExtractor.java`

#### common/infrastructure/config/

**Classes to move from shared/**:

- `AsyncConfig.java`
- `CacheConfig.java`
- `CorsConfiguration.java`
- `OpenApiConfig.java`
- `QueryOptimizationConfig.java`
- `ApiVersioningConfig.java`

#### common/infrastructure/security/

**Classes to move from shared/**:

- `SecurityConfiguration.java`
- `JwtConfiguration.java`
- `JwtAuthenticationFilter.java`
- `CorrelationIdFilter.java`
- `UserContextMdcFilter.java`
- `UnauthorizedOperationException.java`

#### common/infrastructure/monitoring/

**Classes to move from shared/**:

- `BookstoreMetrics.java`
- `MetricsConfiguration.java`
- `MetricsScheduler.java`
- `AuditLogger.java`
- `LoggingConfiguration.java`
- `QueryPerformanceService.java`
- `CacheWarmupService.java`

#### common/infrastructure/web/

**Classes to move from shared/**:

- `BookstoreExceptionHandler.java` (rename to `GlobalExceptionHandler`)
- `ApiVersionInterceptor.java`
- `ApiInfoResource.java` → `ApiInfoController.java`
- `ApiVersion.java`
- `ApiDeprecationNotice.java`

---

## Class Naming Conventions

### Current → Proposed Changes

| Current Pattern                     | Proposed Pattern                                          | Reason                                |
| ----------------------------------- | --------------------------------------------------------- | ------------------------------------- |
| `*Resource`                         | `*Controller`                                             | More standard Spring naming           |
| `*Request`                          | `*Command`                                                | CQRS terminology for write operations |
| `*Criteria`                         | `*Query`                                                  | CQRS terminology for read operations  |
| `*Service`                          | `*ApplicationService`                                     | Distinguish from domain services      |
| `BookRepository` (interface + impl) | `BookRepository` (interface) + `JpaBookRepository` (impl) | Separate domain from infrastructure   |

### New Naming Patterns

| Type                 | Pattern                      | Example                                                   |
| -------------------- | ---------------------------- | --------------------------------------------------------- |
| Entities             | Noun                         | `Book`, `Category`, `Inventory`                           |
| Value Objects        | Noun                         | `ISBN`, `Money`, `StockLevel`                             |
| Domain Services      | Noun + Service               | `BookAvailabilityChecker`, `StockReservationService`      |
| Application Services | Context + ApplicationService | `CatalogApplicationService`                               |
| Controllers          | Resource + Controller        | `BookController`, `InventoryController`                   |
| Commands             | Verb + Noun + Command        | `CreateBookCommand`, `UpdateInventoryCommand`             |
| Queries              | Noun + Query                 | `BookSearchQuery`, `InventoryStatusQuery`                 |
| DTOs                 | Noun + Dto                   | `BookDto`, `InventoryDto`                                 |
| Exceptions           | Noun + Exception             | `BookNotFoundException`, `InsufficientInventoryException` |

---

## Benefits of This Architecture

### 1. Clear Separation of Concerns

- **Domain layer**: Pure business logic, no infrastructure dependencies
- **Application layer**: Use case orchestration, transaction management
- **Infrastructure layer**: Technical details (DB, REST, config)

### 2. Bounded Contexts

- **Catalog**: Manages book information and categories
- **Inventory**: Manages stock levels and reservations
- **Clear interfaces** between contexts

### 3. Testability

- Domain logic can be tested without Spring/JPA
- Application services can be tested with mocked repositories
- Infrastructure can be tested with integration tests

### 4. Maintainability

- Easy to find classes (clear package structure)
- Changes in one layer don't affect others
- New developers understand structure quickly

### 5. Scalability

- Bounded contexts can become separate microservices
- Domain model can evolve independently
- Infrastructure can be swapped (e.g., different DB)

---

## Migration Strategy

### Phase 1: Create New Package Structure (Low Risk)

1. Create new package folders
2. Don't move files yet
3. Review with team

### Phase 2: Move Infrastructure (Medium Risk)

1. Move `shared/` classes to `common/infrastructure/`
2. Update imports
3. Run tests

### Phase 3: Refactor Catalog Context (High Risk)

1. Create value objects (ISBN, Money)
2. Move domain classes to `catalog/domain/`
3. Split repositories (interface vs implementation)
4. Rename services
5. Move controllers to `infrastructure/web/`
6. Run tests after each step

### Phase 4: Refactor Inventory Context (High Risk)

1. Similar steps as Catalog
2. Create value objects
3. Reorganize packages
4. Run tests

### Phase 5: Add Domain Behavior (Medium Risk)

1. Move business logic from services to entities
2. Create domain services where needed
3. Refactor application services to orchestrate
4. Run tests

---

## Example: Before & After

### Before (Current)

```java
// org.gripday.bookstore.catalog.CatalogService
@Service
public class CatalogService {

  public BookDto createBook(CreateBookRequest request) {
    // Validation
    if (bookRepository.findByIsbn(request.isbn()).isPresent()) {
      throw new DuplicateIsbnException(request.isbn());
    }

    // Create entity
    var book = new Book();
    book.setTitle(request.title());
    book.setAuthor(request.author());
    // ... more setters

    return convertToDto(bookRepository.save(book));
  }
}
```

### After (Proposed)

```java
// org.gripday.bookstore.catalog.domain.model.Book
@Entity
public class Book extends AggregateRoot<BookId> {

  private ISBN isbn;
  private Money price;

  public static Book create(String title, String author, ISBN isbn, Money price) {
    // Validation in constructor
    return new Book(title, author, isbn, price);
  }

  public void updateDetails(String title, String author, Money price) {
    // Business rules here
    this.title = title;
    this.author = author;
    this.price = price;
  }
}

// org.gripday.bookstore.catalog.application.CatalogApplicationService
@Service
public class CatalogApplicationService {

  private final BookRepository bookRepository;
  private final DuplicateIsbnChecker isbnChecker;

  @Transactional
  public BookDto createBook(CreateBookCommand command) {
    var isbn = new ISBN(command.isbn());
    isbnChecker.ensureUnique(isbn);

    var book = Book.create(command.title(), command.author(), isbn, new Money(command.price()));

    bookRepository.save(book);
    return BookDto.from(book);
  }
}
```

---

## Recommendations

### Start Small

- Begin with Phase 1 & 2 (infrastructure reorganization)
- Get team feedback
- Then tackle domain refactoring

### Keep It Simple

- Don't over-engineer
- Value objects are optional (start with primitives if needed)
- Focus on clear package structure first

### Maintain Backward Compatibility

- Keep old classes during migration
- Use `@Deprecated` annotation
- Remove after full migration

### Document Decisions

- Update README with new structure
- Add package-info.java files
- Create architecture decision records (ADRs)

---

## Questions for Team Discussion

1. Should we introduce value objects immediately or gradually?
2. Do we want separate use case classes or keep logic in application services?
3. Should we split into separate modules (Maven modules) or keep single module?
4. Timeline for migration? (Suggest: 2-3 sprints)
5. Do we need domain events now or later?
