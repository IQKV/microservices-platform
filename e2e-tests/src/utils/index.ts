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

// Test Data Management exports
export { 
  TestDataFactory, 
  testDataFactory, 
  createTenantAwareFactory, 
  createSeededFactory 
} from './test-data-factory.js';

// Database Helper exports
export { 
  DatabaseHelper, 
  DatabaseConnectionManager,
  TestTransactionManager,
  TestDataSeeder,
  TestDataCleanup,
  databaseHelper,
  createDatabaseHelper
} from './database-helper.js';

// Test Data Manager exports
export { 
  TestDataManager,
  createTestDataManager,
  createTenantTestDataManager
} from './test-data-manager.js';

// Re-export types for convenience
export type {
  PaginationParams,
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

// Test Data Factory types
export type {
  TestDataFactoryConfig,
  CreateUserOptions,
  CreateTenantOptions,
  TestDataRelationships
} from './test-data-factory.js';

// Database Helper types
export type {
  DatabasePoolConfig,
  TransactionConfig,
  TestIsolationConfig
} from '../config/database.js';

// Test Data Manager types
export type {
  TestDataManagerConfig,
  TestDataCreationResult,
  BatchTestDataResult
} from './test-data-manager.js';

export type {
  ApiResponse,
  ApiError,
  RequestConfig,
  AuthTokens,
  LoginCredentials,
  AuthResponse,
  UserData,
  UserRegistrationData,
  TenantData,
  PaginatedResponse,
  HealthCheckResponse,
  ErrorResponse,
  ValidationError
} from '../types/api-responses.js';
