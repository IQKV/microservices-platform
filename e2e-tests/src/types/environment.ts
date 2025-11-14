/**
 * Environment configuration types for Playwright API testing
 */

export type EnvironmentName = 'local' | 'staging' | 'production';

export interface DatabaseConfig {
  host: string;
  port: number;
  database: string;
  username: string;
  password: string;
  url: string;
}

export interface RedisConfig {
  host: string;
  port: number;
  url: string;
}

export interface ServiceEndpoints {
  gateway: string;
  auth: string;
  bookstore: string;
}

export interface TimeoutConfig {
  request: number;
  test: number;
  suite: number;
  healthCheck: number;
}

export interface RetryConfig {
  flaky: number;
  failed: number;
  networkError: number;
}

export interface ParallelismConfig {
  workers: number;
  shards: number;
}

export interface TestEnvironmentConfig {
  name: EnvironmentName;
  baseUrl: string;
  services: ServiceEndpoints;
  databases: {
    auth: DatabaseConfig;
    bookstore: DatabaseConfig;
  };
  redis: RedisConfig;
  timeouts: TimeoutConfig;
  retries: RetryConfig;
  parallelism: ParallelismConfig;
  debug: boolean;
  headless: boolean;
  screenshot: 'off' | 'only-on-failure' | 'on';
  video: 'off' | 'on-first-retry' | 'retain-on-failure' | 'on';
  trace: 'off' | 'on-first-retry' | 'retain-on-failure' | 'on';
}

export interface EnvironmentVariables {
  TEST_ENV?: string;
  BASE_URL?: string;
  GATEWAY_URL?: string;
  USER_SERVICE_URL?: string;
  BOOKSTORE_SERVICE_URL?: string;
  DATABASE_USER_URL?: string;
  DATABASE_BOOKSTORE_URL?: string;
  REDIS_URL?: string;
  TEST_TIMEOUT?: string;
  TEST_WORKERS?: string;
  DEBUG?: string;
  HEADLESS?: string;
  SCREENSHOT?: string;
  VIDEO?: string;
  TRACE?: string;
}