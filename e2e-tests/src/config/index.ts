/**
 * Configuration module exports
 */

// Environment configuration
export {
  getEnvironmentConfig,
  getEnvironmentConfigByName,
  getAvailableEnvironments,
  isValidEnvironment,
  environments
} from './environments.js';

// Environment types
export type {
  EnvironmentName,
  DatabaseConfig,
  RedisConfig,
  ServiceEndpoints,
  TimeoutConfig,
  RetryConfig,
  ParallelismConfig,
  TestEnvironmentConfig,
  EnvironmentVariables
} from '../types/environment.js';

// Test data configuration
export {
  DEFAULT_TEST_DATA,
  TEST_DATA_CONFIG,
  API_ENDPOINTS,
  HTTP_STATUS,
  TEST_HEADERS
} from './test-data.js';

export type {
  TestUserData,
  TestTenantData,
} from './test-data.js';

// Database configuration
export {
  getDatabaseConfig,
  getAllDatabaseConfigs,
  buildConnectionUrl,
  DATABASE_SCHEMAS,
  CLEANUP_QUERIES,
  HEALTH_CHECK_QUERIES,
  DEFAULT_POOL_CONFIG,
  DEFAULT_TRANSACTION_CONFIG,
  DEFAULT_ISOLATION_CONFIG,
  generateTestId,
  generateTestEmail,
  generateTestUsername,
  generateTestSubdomain
} from './database.js';

export type {
  DatabasePoolConfig,
  TransactionConfig,
  TestIsolationConfig
} from './database.js';

// Validation utilities
export {
  validateEnvironmentConfig,
  checkServiceHealth,
  validateEnvironment,
  waitForServicesHealthy,
  printValidationResults
} from './validation.js';

export type {
  ValidationResult,
  ServiceHealthResult,
  EnvironmentValidationResult
} from './validation.js';

// Default export - current environment configuration
export { default as config } from './environments.js';
