/**
 * Utility exports for Playwright API testing
 */

// API Client exports
export { ApiClient, createApiClient } from './api-client.js';
export { TypedApiClient } from './typed-api-client.js';

// Validation exports
export { 
  ValidationUtils, 
  ApiValidationError,
  schemas 
} from './validation.js';

// Authentication Testing exports
export { AuthTestFramework } from './auth-test-framework.js';
export { JwtTestUtils } from './jwt-test-utils.js';
export { RbacTestHelpers } from './rbac-test-helpers.js';

// Re-export types for convenience
export type {
  PaginationParams,
  BookSearchParams,
  UserSearchParams
} from './typed-api-client.js';

export type { ApiClientConfig } from './api-client.js';

// Authentication testing types
export type {
  AuthTestUser,
  TokenTestResult,
  AuthFlowTestResult
} from './auth-test-framework.js';

export type {
  JwtPayload,
  TokenValidationResult,
  TokenComparisonResult
} from './jwt-test-utils.js';

export type {
  RoleDefinition,
  EndpointAccessRule,
  AccessTestResult,
  RoleTestSuite
} from './rbac-test-helpers.js';

export type {
  ApiResponse,
  ApiError,
  RequestConfig,
  AuthTokens,
  LoginCredentials,
  AuthResponse,
  UserData,
  UserRegistrationData,
  BookData,
  TenantData,
  PaginatedResponse,
  HealthCheckResponse,
  ErrorResponse,
  ValidationError
} from '../types/api-responses.js';