# Design Document

## Overview

This design outlines a Playwright-based API testing infrastructure for the Gripday microservices platform. The solution provides end-to-end automated testing capabilities for all REST APIs across User Service, Gateway Service, and Bookstore Service, with support for authentication flows, multi-tenant scenarios, and reporting.

The testing infrastructure follows a modular, scalable architecture that integrates seamlessly with the existing Docker Compose development environment and CI/CD pipelines.

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Playwright Test Suite                        │
├─────────────────────────────────────────────────────────────────┤
│  Test Runners  │  Test Utilities  │  Data Management  │ Reports │
├─────────────────────────────────────────────────────────────────┤
│                    Test Environment                             │
├─────────────────────────────────────────────────────────────────┤
│  Gateway (8080) │ User Service (8080) │ Bookstore Service (8080) │
├─────────────────────────────────────────────────────────────────┤
│     PostgreSQL (Auth)  │  PostgreSQL (Bookstore)  │    Redis    │
└─────────────────────────────────────────────────────────────────┘
```

### Directory Structure

```
e2e-tests/
├── playwright.config.ts              # Main Playwright configuration
├── package.json                      # Dependencies and scripts
├── docker-compose.test.yml           # Test environment setup
├── src/
│   ├── config/
│   │   ├── environments.ts           # Environment-specific configurations
│   │   ├── test-data.ts              # Test data constants and factories
│   │   └── database.ts               # Database connection utilities
│   ├── fixtures/
│   │   ├── auth.fixture.ts           # Authentication test fixtures
│   │   ├── tenant.fixture.ts         # Multi-tenant test fixtures
│   │   └── api.fixture.ts            # API client fixtures
│   ├── utils/
│   │   ├── api-client.ts             # HTTP client with authentication
│   │   ├── test-data-factory.ts      # Test data generation utilities
│   │   ├── database-helper.ts        # Database operations for tests
│   │   ├── assertions.ts             # Custom assertion helpers
│   │   └── cleanup.ts                # Test cleanup utilities
│   ├── types/
│   │   ├── api-responses.ts          # API response type definitions
│   │   ├── test-context.ts           # Test context interfaces
│   │   └── environment.ts            # Environment configuration types
│   └── tests/
│       ├── user-service/
│       │   ├── authentication.spec.ts
│       │   ├── user-management.spec.ts
│       │   ├── email-verification.spec.ts
│       │   └── tenant-management.spec.ts
│       ├── gateway-service/
│       │   ├── routing.spec.ts
│       │   ├── rate-limiting.spec.ts
│       │   ├── circuit-breaker.spec.ts
│       │   └── cors.spec.ts
│       ├── bookstore-service/
│       │   ├── book-management.spec.ts
│       │   ├── inventory.spec.ts
│       │   └── search.spec.ts
│       ├── integration/
│       │   ├── end-to-end-flows.spec.ts
│       │   ├── multi-tenant.spec.ts
│       │   └── service-communication.spec.ts
│       └── security/
│           ├── authentication-security.spec.ts
│           ├── authorization.spec.ts
│           ├── input-validation.spec.ts
│           └── rate-limiting-security.spec.ts
├── reports/                          # Generated test reports
├── screenshots/                      # Test failure screenshots
└── test-data/                        # Static test data files
```

## Components and Interfaces

### 1. Test Environment Management

**Docker Compose Test Configuration**

- Isolated test environment with dedicated databases
- Automatic service startup/shutdown
- Health checks and dependency management
- Port isolation to avoid conflicts with development environment

```typescript
interface TestEnvironment {
  baseUrl: string;
  services: {
    gateway: string;
    auth: string;
    bookstore: string;
  };
  databases: {
    auth: DatabaseConfig;
    bookstore: DatabaseConfig;
  };
  redis: RedisConfig;
}
```

### 2. API Client Framework

**Authenticated HTTP Client**

- Automatic JWT token management
- Request/response logging and debugging
- Retry logic with exponential backoff
- Multi-tenant header injection
- Request/response validation

```typescript
interface ApiClient {
  authenticate(credentials: LoginCredentials): Promise<AuthTokens>;
  request<T>(config: RequestConfig): Promise<ApiResponse<T>>;
  setTenant(tenantId: string): void;
  setAuthToken(token: string): void;
}
```

### 3. Test Data Management

**Data Factory System**

- Realistic test data generation using Faker.js
- Database seeding and cleanup
- Tenant-aware data creation
- Relationship management between entities
- Parallel test isolation

```typescript
interface TestDataFactory {
  createUser(overrides?: Partial<UserData>): Promise<UserData>;
  createBook(overrides?: Partial<BookData>): Promise<BookData>;
  createTenant(overrides?: Partial<TenantData>): Promise<TenantData>;
  cleanup(): Promise<void>;
}
```

### 4. Authentication Test Framework

**JWT Token Management**

- Token lifecycle testing (creation, refresh, expiration)
- Role-based access control validation
- Multi-tenant token context
- Token security validation

```typescript
interface AuthTestFramework {
  registerUser(userData: UserRegistrationData): Promise<UserResponse>;
  loginUser(credentials: LoginCredentials): Promise<AuthResponse>;
  refreshToken(refreshToken: string): Promise<AuthResponse>;
  verifyEmail(token: string): Promise<VerificationResponse>;
}
```

### 5. Multi-Tenant Test Support

**Tenant Context Management**

- Header-based tenant identification testing
- Subdomain routing validation
- Data isolation verification
- Cross-tenant access prevention

```typescript
interface TenantTestFramework {
  createTenantContext(tenantId: string): TenantContext;
  validateTenantIsolation(tenant1: string, tenant2: string): Promise<boolean>;
  testCrossTenantAccess(unauthorizedTenant: string): Promise<void>;
}
```

## Data Models

### Test Configuration Model

```typescript
interface TestConfig {
  environment: "local" | "staging" | "production";
  baseUrls: {
    gateway: string;
    auth: string;
    bookstore: string;
  };
  timeouts: {
    request: number;
    test: number;
    suite: number;
  };
  retries: {
    flaky: number;
    failed: number;
  };
  parallelism: {
    workers: number;
    shards: number;
  };
}
```

### Test Context Model

```typescript
interface TestContext {
  user: UserData;
  tenant: TenantData;
  authTokens: AuthTokens;
  apiClient: ApiClient;
  testData: TestDataFactory;
}
```

### API Response Models

```typescript
interface ApiResponse<T> {
  status: number;
  headers: Record<string, string>;
  data: T;
  duration: number;
  correlationId?: string;
}

interface ErrorResponse {
  error: {
    code: string;
    message: string;
    details: string;
    timestamp: string;
    path: string;
    method: string;
    correlationId: string;
  };
}
```

## Error Handling

### Test Failure Management

**Comprehensive Error Capture**

- HTTP request/response logging
- Screenshot capture for visual debugging
- Database state snapshots
- Service log collection
- Correlation ID tracking

**Retry Strategies**

- Automatic retry for flaky network issues
- Exponential backoff for rate-limited requests
- Circuit breaker pattern for service failures
- Graceful degradation for non-critical failures

### Error Classification

```typescript
enum TestErrorType {
  NETWORK_ERROR = "NETWORK_ERROR",
  AUTHENTICATION_ERROR = "AUTHENTICATION_ERROR",
  VALIDATION_ERROR = "VALIDATION_ERROR",
  SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE",
  DATA_INTEGRITY_ERROR = "DATA_INTEGRITY_ERROR",
  TIMEOUT_ERROR = "TIMEOUT_ERROR",
}
```

## Testing Strategy

### Test Categories

**1. Unit API Tests**

- Individual endpoint validation
- Request/response schema validation
- Error handling verification
- Input validation testing

**2. Integration Tests**

- Service-to-service communication
- Gateway routing validation
- Authentication flow testing
- Multi-tenant functionality

**3. End-to-End Tests**

- Complete user workflows
- Cross-service transactions
- Real-world scenario simulation
- Performance validation

**4. Security Tests**

- Authentication bypass attempts
- Authorization boundary testing
- Input sanitization validation
- Rate limiting effectiveness

### Test Execution Strategy

**Parallel Execution**

- Worker-based parallelism for faster execution
- Test isolation through database transactions
- Tenant-based test partitioning
- Resource cleanup coordination

**Environment Management**

- Docker Compose for local testing
- Kubernetes for staging/production testing
- Environment-specific configuration
- Service health validation

### Test Data Strategy

**Data Isolation**

- Unique test data per test run
- Tenant-based data partitioning
- Automatic cleanup after test completion
- Database transaction rollback for unit tests

**Realistic Data Generation**

- Faker.js for realistic test data
- Relationship-aware data creation
- Configurable data volumes
- Edge case data scenarios

## Implementation Phases

### Phase 1: Foundation Setup

- Project structure and configuration
- Docker Compose test environment
- Basic API client framework
- Authentication utilities

### Phase 2: Core Test Implementation

- User Service API tests
- Gateway Service routing tests
- Basic integration tests
- Test data management

### Phase 3: Advanced Features

- Multi-tenant testing
- Security testing
- Performance validation
- Comprehensive reporting

### Phase 4: CI/CD Integration

- Pipeline integration
- Parallel execution optimization
- Report generation and publishing
- Monitoring and alerting

## Technology Stack

### Core Technologies

- **Playwright**: API testing framework with TypeScript support
- **TypeScript**: Type-safe test development
- **Docker Compose**: Test environment orchestration
- **PostgreSQL**: Database for test data
- **Redis**: Caching and session management

### Testing Libraries

- **@playwright/test**: Core testing framework
- **faker-js**: Realistic test data generation
- **joi**: Schema validation for API responses
- **dotenv**: Environment configuration management
- **winston**: Structured logging for tests

### Reporting and Monitoring

- **Playwright HTML Reporter**: Interactive test reports
- **JUnit Reporter**: CI/CD integration
- **Allure Reporter**: Advanced reporting with history
- **Custom Metrics**: Test execution analytics

## Configuration Management

### Environment Configuration

```typescript
const environments = {
  local: {
    baseUrl: "http://localhost:8080",
    services: {
      auth: "http://localhost:8080",
      bookstore: "http://localhost:8080",
    },
    databases: {
      auth: "postgresql://localhost:5432/gripday_user_test",
      bookstore: "postgresql://localhost:5433/gripday_bookstore_test",
    },
  },
  staging: {
    baseUrl: "https://api.gripday.website",
    // ... staging configuration
  },
};
```

### Test Configuration

```typescript
const testConfig = {
  timeout: 30000,
  retries: 2,
  workers: 4,
  reporter: [
    ["html", { outputFolder: "reports/html" }],
    ["junit", { outputFile: "reports/junit.xml" }],
    ["json", { outputFile: "reports/results.json" }],
  ],
};
```

## Security Considerations

### Test Data Security

- No production data in tests
- Encrypted sensitive test data
- Secure credential management
- Test environment isolation

### Authentication Security

- JWT token validation
- Role-based access testing
- Session management validation
- Multi-factor authentication testing

### Network Security

- HTTPS enforcement testing
- CORS policy validation
- Rate limiting verification
- Input sanitization testing

## Monitoring and Observability

### Test Metrics

- Test execution duration
- Success/failure rates
- API response times
- Error categorization

### Reporting Features

- Interactive HTML reports
- Test history tracking
- Failure analysis
- Performance trends

### Integration Points

- CI/CD pipeline integration
- Slack/Teams notifications
- Grafana dashboard integration
- Log aggregation with ELK stack
