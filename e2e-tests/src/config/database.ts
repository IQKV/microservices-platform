/**
 * Database configuration and connection utilities for testing
 */

import { DatabaseConfig } from '../types/environment.js';
import { getEnvironmentConfig } from './environments.js';

/**
 * Database connection pool configuration
 */
export interface DatabasePoolConfig {
  min: number;
  max: number;
  acquireTimeoutMillis: number;
  createTimeoutMillis: number;
  destroyTimeoutMillis: number;
  idleTimeoutMillis: number;
  reapIntervalMillis: number;
  createRetryIntervalMillis: number;
}

/**
 * Database transaction configuration
 */
export interface TransactionConfig {
  isolationLevel: 'READ_UNCOMMITTED' | 'READ_COMMITTED' | 'REPEATABLE_READ' | 'SERIALIZABLE';
  readOnly: boolean;
  timeout: number;
}

/**
 * Default database pool configuration for testing
 */
export const DEFAULT_POOL_CONFIG: DatabasePoolConfig = {
  min: 2,
  max: 10,
  acquireTimeoutMillis: 30000,
  createTimeoutMillis: 30000,
  destroyTimeoutMillis: 5000,
  idleTimeoutMillis: 30000,
  reapIntervalMillis: 1000,
  createRetryIntervalMillis: 200
};

/**
 * Default transaction configuration for testing
 */
export const DEFAULT_TRANSACTION_CONFIG: TransactionConfig = {
  isolationLevel: 'READ_COMMITTED',
  readOnly: false,
  timeout: 30000
};

/**
 * Gets database configuration for a specific service
 */
export function getDatabaseConfig(service: 'auth' | 'bookstore'): DatabaseConfig {
  const envConfig = getEnvironmentConfig();
  return envConfig.databases[service];
}

/**
 * Gets all database configurations
 */
export function getAllDatabaseConfigs(): Record<string, DatabaseConfig> {
  const envConfig = getEnvironmentConfig();
  return envConfig.databases;
}

/**
 * Builds a PostgreSQL connection URL with additional parameters
 */
export function buildConnectionUrl(
  config: DatabaseConfig,
  additionalParams?: Record<string, string>
): string {
  const url = new URL(config.url);
  
  // Add default parameters for testing
  const defaultParams = {
    'application_name': 'gripday-e2e-tests',
    'connect_timeout': '30',
    'statement_timeout': '60000',
    'idle_in_transaction_session_timeout': '30000'
  };
  
  // Merge with additional parameters
  const allParams = { ...defaultParams, ...additionalParams };
  
  // Add parameters to URL
  Object.entries(allParams).forEach(([key, value]) => {
    url.searchParams.set(key, value);
  });
  
  return url.toString();
}

/**
 * Database schema management utilities
 */
export const DATABASE_SCHEMAS = {
  user: {
    tables: [
      'users',
      'user_authorities',
      'authorities',
      'tenants',
      'tenant_users',
      'email_verifications',
      'password_reset_tokens'
    ],
    sequences: [
      'users_id_seq',
      'authorities_id_seq',
      'tenants_id_seq'
    ]
  },
  
  bookstore: {
    tables: [
      'books',
      'categories',
      'book_categories',
      'inventory',
      'orders',
      'order_items'
    ],
    sequences: [
      'books_id_seq',
      'categories_id_seq',
      'orders_id_seq'
    ]
  }
} as const;

/**
 * Test data cleanup queries
 */
export const CLEANUP_QUERIES = {
  user: [
    'DELETE FROM email_verifications WHERE email LIKE \'%test%\' OR email LIKE \'%example.com\'',
    'DELETE FROM password_reset_tokens WHERE user_id IN (SELECT id FROM users WHERE email LIKE \'%test%\' OR email LIKE \'%example.com\')',
    'DELETE FROM tenant_users WHERE user_id IN (SELECT id FROM users WHERE email LIKE \'%test%\' OR email LIKE \'%example.com\')',
    'DELETE FROM user_authorities WHERE user_id IN (SELECT id FROM users WHERE email LIKE \'%test%\' OR email LIKE \'%example.com\')',
    'DELETE FROM users WHERE email LIKE \'%test%\' OR email LIKE \'%example.com\'',
    'DELETE FROM tenants WHERE name LIKE \'%Test%\' OR subdomain LIKE \'%test%\''
  ],
  
  bookstore: [
    'DELETE FROM order_items WHERE order_id IN (SELECT id FROM orders WHERE created_at > NOW() - INTERVAL \'1 hour\')',
    'DELETE FROM orders WHERE created_at > NOW() - INTERVAL \'1 hour\'',
    'DELETE FROM inventory WHERE book_id IN (SELECT id FROM books WHERE title LIKE \'%Test%\')',
    'DELETE FROM book_categories WHERE book_id IN (SELECT id FROM books WHERE title LIKE \'%Test%\')',
    'DELETE FROM books WHERE title LIKE \'%Test%\'',
    'DELETE FROM categories WHERE name LIKE \'%Test%\''
  ]
} as const;

/**
 * Database health check queries
 */
export const HEALTH_CHECK_QUERIES = {
  user: 'SELECT 1 as health_check',
  bookstore: 'SELECT 1 as health_check'
} as const;

/**
 * Test isolation utilities
 */
export interface TestIsolationConfig {
  useTransactions: boolean;
  cleanupAfterEach: boolean;
  cleanupAfterAll: boolean;
  isolationPrefix: string;
}

export const DEFAULT_ISOLATION_CONFIG: TestIsolationConfig = {
  useTransactions: true,
  cleanupAfterEach: true,
  cleanupAfterAll: true,
  isolationPrefix: 'test_'
};

/**
 * Generates a unique test identifier for data isolation
 */
export function generateTestId(): string {
  const timestamp = Date.now();
  const random = Math.random().toString(36).substring(2, 8);
  return `test_${timestamp}_${random}`;
}

/**
 * Generates a unique test email for user creation
 */
export function generateTestEmail(prefix: string = 'test'): string {
  const testId = generateTestId();
  return `${prefix}_${testId}@example.com`;
}

/**
 * Generates a unique test username
 */
export function generateTestUsername(prefix: string = 'test'): string {
  const testId = generateTestId();
  return `${prefix}_${testId}`;
}

/**
 * Generates a unique test tenant subdomain
 */
export function generateTestSubdomain(prefix: string = 'test'): string {
  const testId = generateTestId();
  return `${prefix}-${testId}`;
}