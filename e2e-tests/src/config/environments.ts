import { config } from 'dotenv';
import { TestEnvironmentConfig, EnvironmentName, EnvironmentVariables } from '../types/environment.js';

// Load environment variables from .env file
config();

/**
 * Environment-specific configurations for Playwright API testing
 */
const environments: Record<EnvironmentName, TestEnvironmentConfig> = {
  local: {
    name: 'local',
    baseUrl: 'http://localhost:8090',
    services: {
      gateway: 'http://localhost:8090',
      user: 'http://localhost:8091',
      bookstore: 'http://localhost:8092'
    },
    databases: {
      user: {
        host: 'localhost',
        port: 5434,
        database: 'gripday_user_test',
        username: 'gripday_test_user',
        password: 'gripday_test_password',
        url: 'postgresql://gripday_test_user:gripday_test_password@localhost:5434/gripday_user_test'
      },
      bookstore: {
        host: 'localhost',
        port: 5435,
        database: 'gripday_bookstore_test',
        username: 'gripday_test_user',
        password: 'gripday_test_password',
        url: 'postgresql://gripday_test_user:gripday_test_password@localhost:5435/gripday_bookstore_test'
      }
    },
    redis: {
      host: 'localhost',
      port: 6380,
      url: 'redis://localhost:6380'
    },
    timeouts: {
      request: 30000,
      test: 60000,
      suite: 300000,
      healthCheck: 10000
    },
    retries: {
      flaky: 2,
      failed: 1,
      networkError: 3
    },
    parallelism: {
      workers: 4,
      shards: 1
    },
    debug: true,
    headless: true,
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    trace: 'retain-on-failure'
  },

  staging: {
    name: 'staging',
    baseUrl: 'https://api.gripday.website',
    services: {
      gateway: 'https://api.gripday.website',
      user: 'https://user.gripday.website',
      bookstore: 'https://bookstore.gripday.website'
    },
    databases: {
      user: {
        host: 'staging-postgres-user.gripday.com',
        port: 5432,
        database: 'gripday_user_staging',
        username: 'gripday_staging_user',
        password: process.env.STAGING_DB_PASSWORD || 'staging_password',
        url: `postgresql://gripday_staging_user:${process.env.STAGING_DB_PASSWORD || 'staging_password'}@staging-postgres-user.gripday.com:5432/gripday_user_staging`
      },
      bookstore: {
        host: 'staging-postgres-bookstore.gripday.com',
        port: 5432,
        database: 'gripday_bookstore_staging',
        username: 'gripday_staging_user',
        password: process.env.STAGING_DB_PASSWORD || 'staging_password',
        url: `postgresql://gripday_staging_user:${process.env.STAGING_DB_PASSWORD || 'staging_password'}@staging-postgres-bookstore.gripday.com:5432/gripday_bookstore_staging`
      }
    },
    redis: {
      host: 'staging-redis.gripday.com',
      port: 6379,
      url: `redis://${process.env.STAGING_REDIS_PASSWORD ? `:${process.env.STAGING_REDIS_PASSWORD}@` : ''}staging-redis.gripday.com:6379`
    },
    timeouts: {
      request: 45000,
      test: 90000,
      suite: 600000,
      healthCheck: 15000
    },
    retries: {
      flaky: 3,
      failed: 2,
      networkError: 5
    },
    parallelism: {
      workers: 6,
      shards: 2
    },
    debug: false,
    headless: true,
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    trace: 'retain-on-failure'
  },

  production: {
    name: 'production',
    baseUrl: 'https://api.gripday.com',
    services: {
      gateway: 'https://api.gripday.com',
      user: 'https://user.gripday.com',
      bookstore: 'https://bookstore.gripday.com'
    },
    databases: {
      user: {
        host: 'prod-postgres-user.gripday.com',
        port: 5432,
        database: 'gripday_user_prod',
        username: 'gripday_prod_user',
        password: process.env.PROD_DB_PASSWORD || 'prod_password',
        url: `postgresql://gripday_prod_user:${process.env.PROD_DB_PASSWORD || 'prod_password'}@prod-postgres-user.gripday.com:5432/gripday_user_prod`
      },
      bookstore: {
        host: 'prod-postgres-bookstore.gripday.com',
        port: 5432,
        database: 'gripday_bookstore_prod',
        username: 'gripday_prod_user',
        password: process.env.PROD_DB_PASSWORD || 'prod_password',
        url: `postgresql://gripday_prod_user:${process.env.PROD_DB_PASSWORD || 'prod_password'}@prod-postgres-bookstore.gripday.com:5432/gripday_bookstore_prod`
      }
    },
    redis: {
      host: 'prod-redis.gripday.com',
      port: 6379,
      url: `redis://${process.env.PROD_REDIS_PASSWORD ? `:${process.env.PROD_REDIS_PASSWORD}@` : ''}prod-redis.gripday.com:6379`
    },
    timeouts: {
      request: 60000,
      test: 120000,
      suite: 900000,
      healthCheck: 20000
    },
    retries: {
      flaky: 3,
      failed: 2,
      networkError: 5
    },
    parallelism: {
      workers: 8,
      shards: 4
    },
    debug: false,
    headless: true,
    screenshot: 'only-on-failure',
    video: 'off',
    trace: 'retain-on-failure'
  }
};

/**
 * Validates environment configuration
 */
function validateEnvironmentConfig(config: TestEnvironmentConfig): void {
  const errors: string[] = [];

  // Validate required URLs
  if (!config.baseUrl) {
    errors.push('baseUrl is required');
  }
  if (!config.services.gateway) {
    errors.push('services.gateway URL is required');
  }
  if (!config.services.auth) {
    errors.push('services.auth URL is required');
  }
  if (!config.services.bookstore) {
    errors.push('services.bookstore URL is required');
  }

  // Validate database configurations
  if (!config.databases.auth.url) {
    errors.push('databases.auth.url is required');
  }
  if (!config.databases.bookstore.url) {
    errors.push('databases.bookstore.url is required');
  }

  // Validate Redis configuration
  if (!config.redis.url) {
    errors.push('redis.url is required');
  }

  // Validate timeout values
  if (config.timeouts.request <= 0) {
    errors.push('timeouts.request must be positive');
  }
  if (config.timeouts.test <= 0) {
    errors.push('timeouts.test must be positive');
  }

  // Validate retry values
  if (config.retries.flaky < 0) {
    errors.push('retries.flaky must be non-negative');
  }
  if (config.retries.failed < 0) {
    errors.push('retries.failed must be non-negative');
  }

  // Validate parallelism values
  if (config.parallelism.workers <= 0) {
    errors.push('parallelism.workers must be positive');
  }
  if (config.parallelism.shards <= 0) {
    errors.push('parallelism.shards must be positive');
  }

  if (errors.length > 0) {
    throw new Error(`Environment configuration validation failed for ${config.name}:\n${errors.join('\n')}`);
  }
}

/**
 * Applies environment variable overrides to configuration
 */
function applyEnvironmentOverrides(config: TestEnvironmentConfig, envVars: EnvironmentVariables): TestEnvironmentConfig {
  const overriddenConfig = { ...config };

  // Apply URL overrides
  if (envVars.BASE_URL) {
    overriddenConfig.baseUrl = envVars.BASE_URL;
  }
  if (envVars.GATEWAY_URL) {
    overriddenConfig.services.gateway = envVars.GATEWAY_URL;
  }
  if (envVars.USER_SERVICE_URL) {
    overriddenConfig.services.auth = envVars.USER_SERVICE_URL;
  }
  if (envVars.BOOKSTORE_SERVICE_URL) {
    overriddenConfig.services.bookstore = envVars.BOOKSTORE_SERVICE_URL;
  }

  // Apply database URL overrides
  if (envVars.DATABASE_USER_URL) {
    overriddenConfig.databases.auth.url = envVars.DATABASE_USER_URL;
  }
  if (envVars.DATABASE_BOOKSTORE_URL) {
    overriddenConfig.databases.bookstore.url = envVars.DATABASE_BOOKSTORE_URL;
  }

  // Apply Redis URL override
  if (envVars.REDIS_URL) {
    overriddenConfig.redis.url = envVars.REDIS_URL;
  }

  // Apply timeout overrides
  if (envVars.TEST_TIMEOUT) {
    const timeout = parseInt(envVars.TEST_TIMEOUT, 10);
    if (!isNaN(timeout)) {
      overriddenConfig.timeouts.test = timeout;
    }
  }

  // Apply parallelism overrides
  if (envVars.TEST_WORKERS) {
    const workers = parseInt(envVars.TEST_WORKERS, 10);
    if (!isNaN(workers)) {
      overriddenConfig.parallelism.workers = workers;
    }
  }

  // Apply debug and UI overrides
  if (envVars.DEBUG) {
    overriddenConfig.debug = envVars.DEBUG.toLowerCase() === 'true';
  }
  if (envVars.HEADLESS) {
    overriddenConfig.headless = envVars.HEADLESS.toLowerCase() === 'true';
  }
  if (envVars.SCREENSHOT) {
    overriddenConfig.screenshot = envVars.SCREENSHOT as 'off' | 'only-on-failure' | 'on';
  }
  if (envVars.VIDEO) {
    overriddenConfig.video = envVars.VIDEO as 'off' | 'on-first-retry' | 'retain-on-failure' | 'on';
  }
  if (envVars.TRACE) {
    overriddenConfig.trace = envVars.TRACE as 'off' | 'on-first-retry' | 'retain-on-failure' | 'on';
  }

  return overriddenConfig;
}

/**
 * Gets the current environment configuration with validation and overrides
 */
export function getEnvironmentConfig(): TestEnvironmentConfig {
  const envName = (process.env.TEST_ENV || 'local') as EnvironmentName;
  
  if (!environments[envName]) {
    throw new Error(`Unknown environment: ${envName}. Available environments: ${Object.keys(environments).join(', ')}`);
  }

  let config = environments[envName];
  
  // Apply environment variable overrides
  config = applyEnvironmentOverrides(config, process.env as EnvironmentVariables);
  
  // Validate the final configuration
  validateEnvironmentConfig(config);
  
  return config;
}

/**
 * Gets configuration for a specific environment
 */
export function getEnvironmentConfigByName(envName: EnvironmentName): TestEnvironmentConfig {
  if (!environments[envName]) {
    throw new Error(`Unknown environment: ${envName}. Available environments: ${Object.keys(environments).join(', ')}`);
  }

  const config = environments[envName];
  validateEnvironmentConfig(config);
  
  return config;
}

/**
 * Lists all available environments
 */
export function getAvailableEnvironments(): EnvironmentName[] {
  return Object.keys(environments) as EnvironmentName[];
}

/**
 * Checks if an environment exists
 */
export function isValidEnvironment(envName: string): envName is EnvironmentName {
  return envName in environments;
}

// Export the environments object for direct access if needed
export { environments };

// Export default configuration
export default getEnvironmentConfig();