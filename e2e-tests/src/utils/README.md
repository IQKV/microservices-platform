# API Client Framework

This directory contains the core API client framework for Playwright API testing of the Gripday microservices platform.

## Overview

The API client framework provides:

- **Authenticated HTTP Client**: Automatic JWT token management and refresh
- **Type-Safe API Methods**: Strongly typed methods for all API endpoints
- **Request/Response Validation**: Schema validation using Joi
- **Retry Logic**: Exponential backoff for network failures
- **Multi-Tenant Support**: Header-based tenant identification
- **Comprehensive Error Handling**: Structured error types and logging

## Core Components

### ApiClient (`api-client.ts`)

The base HTTP client with authentication and retry capabilities:

```typescript
import { createApiClient } from './api-client.js';
import { getEnvironmentConfig } from '../config/environments.js';

const envConfig = getEnvironmentConfig();
const apiClient = createApiClient(envConfig, 'tenant-123');
await apiClient.initialize();

// Authenticate
await apiClient.authenticate({
  username: 'testuser',
  password: 'testpassword',
  tenantId: 'tenant-123'
});

// Make requests
const response = await apiClient.request({
  method: 'GET',
  url: '/api/v1/books',
  params: { page: 0, size: 10 }
});
```

### TypedApiClient (`typed-api-client.ts`)

Type-safe wrapper with validation and strongly typed methods:

```typescript
import { TypedApiClient } from './typed-api-client.js';

const typedClient = new TypedApiClient(apiClient);

// Login (automatically validates request/response)
const authResponse = await typedClient.login({
  username: 'testuser',
  password: 'testpassword'
});

// Get books with type safety
const books = await typedClient.getBooks({
  page: 0,
  size: 10,
  category: 'programming'
});

// Create book with validation
const newBook = await typedClient.createBook({
  title: 'Test Book',
  author: 'Test Author',
  isbn: '978-0-123456-78-9',
  price: 29.99,
  currency: 'USD',
  stock: 100,
  category: 'testing'
});
```

### Validation (`validation.ts`)

Schema validation utilities using Joi:

```typescript
import { ValidationUtils, schemas } from './validation.js';

// Validate data against schema
const validatedUser = ValidationUtils.validateUserData(userData);

// Check if data is valid without throwing
const isValid = ValidationUtils.isValid(bookData, schemas.bookData);

// Get validation errors
const errors = ValidationUtils.getValidationErrors(data, schema);
```

## Available API Methods

### Authentication Service

- `login(credentials)` - Authenticate user
- `registerUser(userData)` - Register new user
- `verifyEmail(token)` - Verify email address
- `refreshTokens()` - Refresh JWT tokens
- `logout()` - Logout and invalidate tokens
- `getCurrentUser()` - Get current user profile
- `updateCurrentUser(userData)` - Update user profile

### User Management (Admin only)

- `getUsers(params?)` - Get all users with pagination
- `getUserById(userId)` - Get user by ID
- `createUser(userData)` - Create new user
- `updateUser(userId, userData)` - Update user
- `deleteUser(userId)` - Delete user
- `setUserEnabled(userId, enabled)` - Enable/disable user

### Tenant Management

- `getCurrentTenant()` - Get current tenant info
- `getTenants(params?)` - Get all tenants (admin only)
- `createTenant(tenantData)` - Create new tenant (admin only)

### Bookstore Service

- `getBooks(params?)` - Get books with search/pagination
- `getBookById(bookId)` - Get book by ID
- `createBook(bookData)` - Create new book
- `updateBook(bookId, bookData)` - Update book
- `deleteBook(bookId)` - Delete book
- `searchBooks(query, params?)` - Search books
- `updateBookStock(bookId, stock)` - Update book stock
- `getBooksByCategory(category, params?)` - Get books by category

### Health Checks

- `checkGatewayHealth()` - Check Gateway service health
- `checkUserServiceHealth()` - Check User service health
- `checkBookstoreServiceHealth()` - Check Bookstore service health

## Error Handling

The framework provides structured error handling with specific error types:

```typescript
import { ApiError, ApiErrorType } from '../types/api-responses.js';

try {
  await typedClient.getBookById(999);
} catch (error) {
  if (error instanceof ApiError) {
    switch (error.type) {
      case ApiErrorType.NOT_FOUND_ERROR:
        console.log('Book not found');
        break;
      case ApiErrorType.AUTHENTICATION_ERROR:
        console.log('Authentication required');
        break;
      case ApiErrorType.NETWORK_ERROR:
        console.log('Network error, retrying...');
        break;
      default:
        console.log('Unknown error:', error.message);
    }
  }
}
```

## Multi-Tenant Support

The client supports multi-tenant scenarios:

```typescript
// Set tenant for all subsequent requests
typedClient.setTenant('tenant-123');

// Login with tenant context
await typedClient.login({
  username: 'user',
  password: 'password',
  tenantId: 'tenant-123'
});

// All API calls will include X-Tenant-ID header
const books = await typedClient.getBooks(); // Only tenant-123 books
```

## Configuration

The client uses environment-specific configuration:

```typescript
import { getEnvironmentConfig } from '../config/environments.js';

// Get config for current environment (TEST_ENV)
const config = getEnvironmentConfig();

// Get config for specific environment
const stagingConfig = getEnvironmentConfigByName('staging');

// Create client with custom config
const apiClient = createApiClient(config, 'tenant-id');
```

## Request/Response Logging

The client provides logging:

```typescript
// Enable debug logging
const apiClient = createApiClient(envConfig, tenantId);
// Logs include:
// - Request method, URL, headers
// - Response status, duration
// - Correlation IDs for tracing
// - Error details and retry attempts
```

## Best Practices

1. **Always initialize the client**: Call `apiClient.initialize()` before making requests
2. **Use TypedApiClient**: Prefer the typed wrapper for better type safety and validation
3. **Handle errors appropriately**: Use structured error handling with ApiError types
4. **Clean up resources**: Call `dispose()` when done with the client
5. **Use correlation IDs**: Leverage built-in correlation ID generation for tracing
6. **Validate tenant isolation**: Test multi-tenant scenarios thoroughly
7. **Monitor token expiration**: The client handles token refresh automatically

## Examples

See `api-client-example.ts` for usage examples including:

- Basic API client usage
- Multi-tenant scenarios
- Error handling patterns
- Request/response validation
- Health check monitoring

## Testing Integration

Use the API client in Playwright tests:

```typescript
import { test, expect } from '@playwright/test';
import { createApiClient, TypedApiClient } from '../utils/index.js';

test('should authenticate and get user profile', async () => {
  const envConfig = getEnvironmentConfig();
  const apiClient = createApiClient(envConfig);
  await apiClient.initialize();
  
  const typedClient = new TypedApiClient(apiClient);
  
  // Login
  const authResponse = await typedClient.login({
    username: 'testuser',
    password: 'testpassword'
  });
  
  expect(authResponse.user.username).toBe('testuser');
  
  // Get profile
  const profile = await typedClient.getCurrentUser();
  expect(profile.email).toContain('@');
  
  await typedClient.dispose();
});
```