/**
 * Test context type definitions for Playwright API testing
 */

import { TestDataFactory, TestDataRelationships } from '../utils/test-data-factory.js';
import { DatabaseHelper } from '../utils/database-helper.js';
import { ApiClient } from '../utils/api-client.js';
import { AuthTestFramework } from '../utils/auth-test-framework.js';
import { UserData, TenantData, AuthTokens } from './api-responses.js';
import { TestEnvironmentConfig } from './environment.js';

/**
 * Main test context interface containing all test utilities and state
 */
export interface TestContext {
  // Environment and configuration
  environment: TestEnvironmentConfig;
  
  // Test utilities
  apiClient: ApiClient;
  authFramework: AuthTestFramework;
  testDataFactory: TestDataFactory;
  databaseHelper: DatabaseHelper;
  
  // Current test state
  currentUser?: UserData;
  currentTenant?: TenantData;
  authTokens?: AuthTokens;
  
  // Test data tracking
  relationships: TestDataRelationships;
  
  // Test metadata
  testId: string;
  testName: string;
  startTime: Date;
}

/**
 * Authentication context for tests
 */
export interface AuthContext {
  user: UserData;
  tokens: AuthTokens;
  tenant?: TenantData;
  isAuthenticated: boolean;
  roles: string[];
  permissions: string[];
}

/**
 * Tenant context for multi-tenant tests
 */
export interface TenantContext {
  tenant: TenantData;
  users: UserData[];
  isActive: boolean;
  settings: Record<string, unknown>;
}

/**
 * Test isolation context for parallel execution
 */
export interface TestIsolationContext {
  testId: string;
  transactionIds: Map<string, string>;
  isolatedData: TestDataRelationships;
  cleanupRequired: boolean;
}

/**
 * API test context for request/response tracking
 */
export interface ApiTestContext {
  baseUrl: string;
  headers: Record<string, string>;
  timeout: number;
  retries: number;
  lastRequest?: {
    method: string;
    url: string;
    headers: Record<string, string>;
    data?: unknown;
    timestamp: Date;
  };
  lastResponse?: {
    status: number;
    headers: Record<string, string>;
    data: unknown;
    duration: number;
    timestamp: Date;
  };
}

/**
 * Security test context for authorization testing
 */
export interface SecurityTestContext {
  currentRole: string;
  allowedEndpoints: string[];
  forbiddenEndpoints: string[];
  testTokens: {
    valid: string;
    expired: string;
    invalid: string;
    malformed: string;
  };
}

/**
 * Performance test context for timing and metrics
 */
export interface PerformanceTestContext {
  startTime: Date;
  endTime?: Date;
  duration?: number;
  requestCount: number;
  responseTimeThresholds: {
    fast: number;
    acceptable: number;
    slow: number;
  };
  metrics: {
    averageResponseTime: number;
    minResponseTime: number;
    maxResponseTime: number;
    successRate: number;
  };
}

/**
 * Test fixture context for Playwright fixtures
 */
export interface TestFixtureContext {
  testContext: TestContext;
  authContext?: AuthContext;
  tenantContext?: TenantContext;
  isolationContext: TestIsolationContext;
  apiContext: ApiTestContext;
  securityContext?: SecurityTestContext;
  performanceContext?: PerformanceTestContext;
}

/**
 * Test setup options
 */
export interface TestSetupOptions {
  requireAuth?: boolean;
  requireTenant?: boolean;
  useTransactions?: boolean;
  seedData?: boolean;
  isolateData?: boolean;
  roles?: string[];
  permissions?: string[];
  tenantId?: string;
  customHeaders?: Record<string, string>;
}

/**
 * Test cleanup options
 */
export interface TestCleanupOptions {
  cleanupData?: boolean;
  rollbackTransactions?: boolean;
  closeConnections?: boolean;
  clearCache?: boolean;
  resetState?: boolean;
}

/**
 * Test data creation options
 */
export interface TestDataCreationOptions {
  count?: number;
  tenantId?: string;
  relationships?: boolean;
  realistic?: boolean;
  seed?: number;
  overrides?: Record<string, unknown>;
}

/**
 * Test assertion context
 */
export interface TestAssertionContext {
  expectedStatus?: number;
  expectedHeaders?: Record<string, string | RegExp>;
  expectedBodySchema?: unknown;
  customValidators?: Array<(response: unknown) => boolean>;
  errorMessages?: string[];
}

/**
 * Test retry context
 */
export interface TestRetryContext {
  maxRetries: number;
  currentRetry: number;
  retryDelay: number;
  retryCondition: (error: Error) => boolean;
  lastError?: Error;
}

/**
 * Test reporting context
 */
export interface TestReportingContext {
  testSuite: string;
  testCase: string;
  tags: string[];
  severity: 'low' | 'medium' | 'high' | 'critical';
  category: 'functional' | 'integration' | 'security' | 'performance';
  screenshots: string[];
  logs: string[];
  metrics: Record<string, number>;
  customData: Record<string, unknown>;
}