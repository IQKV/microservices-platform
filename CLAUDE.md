# Spring Boot 3 Best Practices Guide

## Overview

This document outlines modern Spring Boot 3 best practices for enterprise-grade applications using Java 21+, incorporating cutting-edge JVM features and cloud-native patterns.

## 🏗️ Project Structure & Architecture

### Multi-Module Maven Structure

```
multimoduleservice/
├── pom.xml (parent aggregator)
├── shared/                    # Common utilities, DTOs, interfaces
├── domain/                    # Domain models, business logic
├── infrastructure/            # External integrations, repositories
├── api/                      # REST controllers, web layer
└── application/              # Application services, orchestration
```

### Spring Modulith Organization

- Use `@ApplicationModule` for clear module boundaries
- Define module APIs through package-private visibility
- Leverage `@ModuleTest` for isolated module testing
- Document module interactions with Spring Modulith documentation

## ☕ Modern Java 21+ Features

### Virtual Threads (Project Loom)

```java
// Enable virtual threads globally
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    
    @Override
    public Executor getAsyncExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}

// Use virtual threads for blocking I/O operations
@RestController
public class ProductController {
    
    @Async
    @GetMapping("/products/{id}")
    public CompletableFuture<Product> getProduct(@PathVariable String id) {
        // Blocking database call - virtual thread handles efficiently
        return CompletableFuture.completedFuture(productService.findById(id));
    }
}
```

### Pattern Matching & Switch Expressions

```java
// Modern switch expressions with pattern matching
public ResponseEntity<String> handleException(Exception ex) {
    return switch (ex) {
        case ValidationException ve -> 
            ResponseEntity.badRequest().body("Validation failed: " + ve.getMessage());
        case EntityNotFoundException enfe -> 
            ResponseEntity.notFound().build();
        case SecurityException se -> 
            ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        default -> 
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error");
    };
}
```

### Records for DTOs

```java
// Immutable DTOs with validation
public record CreateProductRequest(
    @NotBlank String name,
    @NotNull @Positive BigDecimal price,
    @Valid CategoryDto category
) {}

// Response DTOs with computed fields
public record ProductResponse(
    String id,
    String name,
    BigDecimal price,
    String displayPrice
) {
    public ProductResponse(Product product) {
        this(
            product.getId(),
            product.getName(),
            product.getPrice(),
            formatPrice(product.getPrice())
        );
    }
}
```

## 🌐 Web Layer Best Practices

### RESTful API Design

```java
@RestController
@RequestMapping("/api/v1/products")
@Validated
@Slf4j
public class ProductController {
    
    private final ProductService productService;
    
    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getProducts(
        @PageableDefault(size = 20, sort = "name") Pageable pageable,
        @RequestParam(required = false) String search
    ) {
        var products = productService.findProducts(search, pageable);
        var response = products.map(ProductResponse::new);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
        @Valid @RequestBody CreateProductRequest request,
        UriComponentsBuilder uriBuilder
    ) {
        var product = productService.createProduct(request);
        var location = uriBuilder.path("/api/v1/products/{id}")
            .buildAndExpand(product.getId())
            .toUri();
        return ResponseEntity.created(location).body(new ProductResponse(product));
    }
}
```

### Exception Handling

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
        MethodArgumentNotValidException ex
    ) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                FieldError::getDefaultMessage
            ));
        
        var errorResponse = new ErrorResponse(
            "VALIDATION_FAILED",
            "Request validation failed",
            errors
        );
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Unexpected error occurred", ex);
        var errorResponse = new ErrorResponse(
            "INTERNAL_ERROR",
            "An unexpected error occurred",
            Map.of()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
```

## 🗄️ Data Access Layer

### JPA Best Practices

```java
@Entity
@Table(name = "products")
@EntityListeners(AuditingEntityListener.class)
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    
    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;
    
    @Version
    private Long version;
    
    // Constructor, getters, setters
}

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    @Query("SELECT p FROM Product p WHERE p.name ILIKE %:search% OR p.description ILIKE %:search%")
    Page<Product> findBySearchTerm(@Param("search") String search, Pageable pageable);
    
    @Modifying
    @Query("UPDATE Product p SET p.active = false WHERE p.lastModifiedDate < :cutoff")
    int deactivateOldProducts(@Param("cutoff") Instant cutoff);
}
```

### Transaction Management

```java
@Service
@Transactional(readOnly = true)
@Slf4j
public class ProductService {
    
    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;
    
    @Transactional
    public Product createProduct(CreateProductRequest request) {
        var product = new Product(request.name(), request.price());
        var savedProduct = productRepository.save(product);
        
        // Publish domain event
        eventPublisher.publishEvent(new ProductCreatedEvent(savedProduct.getId()));
        
        return savedProduct;
    }
    
    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }
}
```

## 📊 Observability & Monitoring

### Metrics with Micrometer

```java
@Component
@Slf4j
public class ProductMetrics {
    
    private final Counter productCreatedCounter;
    private final Timer productSearchTimer;
    
    public ProductMetrics(MeterRegistry meterRegistry) {
        this.productCreatedCounter = Counter.builder("products.created")
            .description("Number of products created")
            .register(meterRegistry);
            
        this.productSearchTimer = Timer.builder("products.search.duration")
            .description("Product search duration")
            .register(meterRegistry);
    }
    
    public void recordProductCreated() {
        productCreatedCounter.increment();
    }
    
    public <T> T recordSearchTime(Supplier<T> searchOperation) {
        return productSearchTimer.recordCallable(searchOperation::get);
    }
}
```

### Structured Logging

```java
@RestController
@Slf4j
public class ProductController {
    
    @GetMapping("/products/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
        log.info("Fetching product with id: {}", id);
        
        try {
            var product = productService.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));
                
            log.info("Successfully retrieved product: productId={}, name={}", 
                product.getId(), product.getName());
                
            return ResponseEntity.ok(new ProductResponse(product));
        } catch (final Exception ex) {
            log.error("Error fetching product: productId={}, error={}", 
                id, ex.getMessage(), ex);
            throw ex;
        }
    }
}
```

## 🔒 Security Best Practices

### Method-Level Security

```java
@Service
@PreAuthorize("hasRole('USER')")
public class ProductService {
    
    @PreAuthorize("hasRole('ADMIN') or @productSecurity.isOwner(#productId, authentication.name)")
    public void deleteProduct(Long productId) {
        productRepository.deleteById(productId);
    }
    
    @PostFilter("hasRole('ADMIN') or filterObject.userId == authentication.name")
    public List<Product> findAllProducts() {
        return productRepository.findAll();
    }
}

@Component
public class ProductSecurity {
    
    private final ProductRepository productRepository;
    
    public boolean isOwner(Long productId, String username) {
        return productRepository.findById(productId)
            .map(product -> product.getCreatedBy().equals(username))
            .orElse(false);
    }
}
```

### Input Validation

```java
public record UpdateProductRequest(
    @NotBlank @Size(min = 1, max = 100) String name,
    @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal price,
    @Pattern(regexp = "^[A-Z_]+$") String status
) {}
```

## 🧪 Testing Strategies

### Integration Testing with Testcontainers

```java
@SpringBootTest
@Testcontainers
class ProductServiceIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    @Autowired
    private ProductService productService;
    
    @Test
    void shouldCreateProduct() {
        // Given
        var request = new CreateProductRequest("Test Product", new BigDecimal("99.99"));
        
        // When
        var product = productService.createProduct(request);
        
        // Then
        assertThat(product.getId()).isNotNull();
        assertThat(product.getName()).isEqualTo("Test Product");
        assertThat(product.getPrice()).isEqualByComparingTo("99.99");
    }
}
```

### Web Layer Testing

```java
@WebMvcTest(ProductController.class)
class ProductControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private ProductService productService;
    
    @Test
    void shouldReturnProduct() throws Exception {
        // Given
        var product = new Product(1L, "Test Product", new BigDecimal("99.99"));
        when(productService.findById(1L)).thenReturn(Optional.of(product));
        
        // When & Then
        mockMvc.perform(get("/api/v1/products/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Test Product"))
            .andExpect(jsonPath("$.price").value(99.99));
    }
}
```

## 🚀 Performance Optimization

### Caching Strategy

```java
@Service
@CacheConfig(cacheNames = "products")
public class ProductService {
    
    @Cacheable(key = "#id", unless = "#result == null")
    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }
    
    @CacheEvict(key = "#result.id")
    @Transactional
    public Product updateProduct(Long id, UpdateProductRequest request) {
        var product = findById(id).orElseThrow();
        product.updateFrom(request);
        return productRepository.save(product);
    }
}
```

### Database Query Optimization

```java
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    // Use JOIN FETCH to avoid N+1 queries
    @Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.active = true")
    List<Product> findActiveProductsWithCategory();
    
    // Use projections for read-only data
    @Query("SELECT new com.example.dto.ProductSummary(p.id, p.name, p.price) FROM Product p")
    List<ProductSummary> findProductSummaries();
    
    // Use native queries for complex operations
    @Query(value = "SELECT * FROM products WHERE tsv @@ plainto_tsquery(:query)", nativeQuery = true)
    List<Product> fullTextSearch(@Param("query") String query);
}
```

## ☁️ Cloud-Native Patterns

### Configuration Management

```yaml
# application.yml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}
  
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/mydb}
    username: ${DATABASE_USERNAME:admin}
    password: ${DATABASE_PASSWORD:password}
    hikari:
      maximum-pool-size: ${DATABASE_POOL_SIZE:10}
      minimum-idle: 2
      idle-timeout: 300000
      max-lifetime: 600000

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true

logging:
  level:
    com.example: ${LOG_LEVEL:INFO}
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level [%X{traceId:-},%X{spanId:-}] %logger{36} - %msg%n"
```

### Health Checks

```java
@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    
    private final DataSource dataSource;
    
    @Override
    public Health health() {
        try (var connection = dataSource.getConnection()) {
            if (connection.isValid(1)) {
                return Health.up()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("connection", "Available")
                    .build();
            }
        } catch (SQLException ex) {
            return Health.down()
                .withDetail("database", "PostgreSQL")
                .withDetail("error", ex.getMessage())
                .build();
        }
        
        return Health.down().build();
    }
}
```

## 📦 Build & Deployment

### Maven Configuration Best Practices

```xml
<properties>
    <!-- Use Java 21 LTS -->
    <java.version>25</java.version>
    <maven.compiler.source>${java.version}</maven.compiler.source>
    <maven.compiler.target>${java.version}</maven.compiler.target>
    
    <!-- Enable preview features if needed -->
    <maven.compiler.parameters>true</maven.compiler.parameters>
    
    <!-- Spring Boot 3.2+ -->
    <spring-boot.version>3.2.0</spring-boot.version>
    
    <!-- Testing -->
    <testcontainers.version>1.19.0</testcontainers.version>
    <junit-jupiter.version>5.10.0</junit-jupiter.version>
</properties>

<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <configuration>
                <layers>
                    <enabled>true</enabled>
                </layers>
                <!-- Enable virtual threads -->
                <jvmArguments>--enable-preview</jvmArguments>
            </configuration>
        </plugin>
        
        <plugin>
            <groupId>org.graalvm.buildtools</groupId>
            <artifactId>native-maven-plugin</artifactId>
            <configuration>
                <buildArgs>
                    <buildArg>--enable-url-protocols=https</buildArg>
                    <buildArg>--initialize-at-build-time=org.slf4j</buildArg>
                </buildArgs>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### Docker Best Practices

```dockerfile
FROM eclipse-temurin:21-jre-alpine as runtime

# Create non-root user
RUN addgroup -g 1000 appuser && adduser -D -s /bin/sh -u 1000 -G appuser appuser

# Copy application layers
COPY --chown=appuser:appuser target/dependency/ /app/lib/
COPY --chown=appuser:appuser target/spring-boot-loader/ /app/
COPY --chown=appuser:appuser target/snapshot-dependencies/ /app/
COPY --chown=appuser:appuser target/application/ /app/

USER appuser

# JVM optimizations for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+UseStringDeduplication"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.JarLauncher"]
```

## 🔧 Development Workflow

### Local Development Setup

```yaml
# docker-compose.yml for local development
version: '3.8'
services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: mydb
      POSTGRES_USER: admin
      POSTGRES_PASSWORD: password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
      
volumes:
  postgres_data:
```

### Profile-Based Configuration

```java
@Configuration
@Profile("!test")
public class ProductionConfig {
    
    @Bean
    @ConditionalOnProperty(name = "app.cache.enabled", havingValue = "true", matchIfMissing = true)
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("products", "categories");
    }
}

@Configuration
@Profile("test")
public class TestConfig {
    
    @Bean
    @Primary
    public Clock testClock() {
        return Clock.fixed(Instant.parse("2023-01-01T00:00:00Z"), ZoneOffset.UTC);
    }
}
```

## 🎯 Key Takeaways

1. **Embrace Virtual Threads**: Use Project Loom for better resource utilization
2. **Modern Java Features**: Leverage records, pattern matching, and enhanced switch expressions
3. **Comprehensive Testing**: Use Testcontainers for reliable integration testing
4. **Observability First**: Implement structured logging, metrics, and distributed tracing
5. **Security by Design**: Apply method-level security and comprehensive input validation
6. **Cloud-Native Patterns**: Design for containers with health checks and externalized configuration
7. **Performance Focus**: Optimize database queries, implement caching, and monitor metrics
8. **Modular Architecture**: Use Spring Modulith for clear module boundaries and better maintainability

This guide provides a foundation for building robust, scalable Spring Boot 3 applications with modern Java 21+ features and enterprise-grade practices.
