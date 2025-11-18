---
name: generate-tests
description: Generate comprehensive test suite for Spring Boot applications including unit, integration, and contract tests
---

# Generate Tests Command

Generate comprehensive test coverage for Spring Boot applications using modern testing practices and frameworks.

## Test Generation Scope

### 1. Unit Tests

- **Service Layer Tests**: Business logic validation with Mockito
- **Controller Tests**: Web layer testing with @WebMvcTest
- **Repository Tests**: Data access testing with @DataJpaTest
- **Component Tests**: Individual component behavior validation

### 2. Integration Tests

- **Application Tests**: Full application context with @SpringBootTest
- **Database Tests**: Real database integration with Testcontainers
- **External Service Tests**: API integration testing with WireMock
- **Security Tests**: Authentication and authorization testing

### 3. Contract Tests

- **API Contract Tests**: Consumer-driven contracts with Spring Cloud Contract
- **Database Contract Tests**: Schema and data contract validation
- **Message Contract Tests**: Event and message format validation

## Test Templates

### Unit Test Template

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private UserService userService;

  @Test
  @DisplayName("Should create user with valid data")
  void shouldCreateUserWithValidData() {
    // Given
    var createRequest = new CreateUserRequest("john.doe", "john@example.com");
    var savedUser = new User(1L, "john.doe", "john@example.com");
    when(userRepository.save(any(User.class))).thenReturn(savedUser);

    // When
    var result = userService.createUser(createRequest);

    // Then
    assertThat(result.getId()).isEqualTo(1L);
    assertThat(result.getUsername()).isEqualTo("john.doe");
    verify(userRepository).save(any(User.class));
  }
}
```

### Integration Test Template

```java
@SpringBootTest
@Testcontainers
class UserServiceIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15").withDatabaseName("testdb").withUsername("test").withPassword("test");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired
  private UserService userService;

  @Test
  void shouldCreateAndRetrieveUser() {
    // Test implementation
  }
}
```

### Web Layer Test Template

```java
@WebMvcTest(UserController.class)
class UserControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private UserService userService;

  @Test
  void shouldReturnUserById() throws Exception {
    // Given
    var user = new User(1L, "john.doe", "john@example.com");
    when(userService.findById(1L)).thenReturn(Optional.of(user));

    // When & Then
    mockMvc.perform(get("/api/users/1")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1)).andExpect(jsonPath("$.username").value("john.doe"));
  }
}
```

## Test Configuration

### Test Application Properties

```yaml
# application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true

logging:
  level:
    org.springframework.web: DEBUG
    com.example: DEBUG
```

### Test Profiles

```java
@TestConfiguration
@Profile("test")
public class TestConfig {

  @Bean
  @Primary
  public Clock testClock() {
    return Clock.fixed(Instant.parse("2023-01-01T00:00:00Z"), ZoneOffset.UTC);
  }

  @Bean
  @Primary
  public EmailService mockEmailService() {
    return Mockito.mock(EmailService.class);
  }
}
```

## Test Execution Instructions

1. **Analyze Codebase**: Identify classes and methods requiring test coverage
2. **Generate Test Structure**: Create test classes following naming conventions
3. **Implement Test Cases**: Write comprehensive test scenarios
4. **Configure Test Data**: Set up test fixtures and data builders
5. **Validate Coverage**: Ensure adequate test coverage metrics
6. **Document Tests**: Add clear test documentation and comments

## Coverage Requirements

- **Line Coverage**: Minimum 90%
- **Branch Coverage**: Minimum 85%
- **Method Coverage**: Minimum 95%
- **Class Coverage**: Minimum 90%

## Test Categories

### Fast Tests (Unit Tests)

- No external dependencies
- In-memory databases only
- Mocked external services
- Execution time < 100ms per test

### Slow Tests (Integration Tests)

- Real database connections
- External service calls
- Full application context
- Execution time < 5s per test

### Contract Tests

- API contract validation
- Schema compatibility tests
- Message format verification
- Cross-service integration validation

## Output Structure

```
src/test/java/
├── unit/
│   ├── service/
│   ├── controller/
│   └── repository/
├── integration/
│   ├── api/
│   ├── database/
│   └── security/
└── contract/
    ├── consumer/
    └── provider/
```

Execute this command to generate a comprehensive test suite that follows Spring Boot testing best practices and ensures high-quality, maintainable test code.
