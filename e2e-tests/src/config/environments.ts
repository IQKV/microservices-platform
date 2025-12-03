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
      auth: 'http://localhost:8091'
    },
    databases: {
      auth: {
        host: 'localhost',
        port: 5434,
        database: 'iqscaffold_user_test',
        username: 'iqscaffold_test_user',
        password: 'iqscaffold_test_password',
        url: 'postgresql://iqscaffold_test_user:iqscaffold_test_password@localhost:5434/iqscaffold_user_test'
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
    baseUrl: 'https://api.iqscaffold.website',
    services: {
      gateway: 'https://api.iqscaffold.website',
      auth: 'https://user.iqscaffold.website'
    },
    databases: {
      auth: {
        host: 'staging-postgres-user.iqscaffold.com',
        port: 5432,
        database: 'iqscaffold_user_staging',
        username: 'iqscaffold_staging_user',
        password: process.env.STAGING_DB_PASSWORD || 'staging_password',
        url: `postgresql://iqscaffold_staging_user:${process.env.STAGING_DB_PASSWORD || 'staging_password'}@staging-postgres-user.iqscaffold.com:5432/iqscaffold_user_staging`
      }
    },
    redis: {
      host: 'staging-redis.iqscaffold.com',
      port: 6379,
      url: `redis://${process.env.STAGING_REDIS_PASSWORD ? `:${process.env.STAGING_REDIS_PASSWORD}@` : ''}staging-redis.iqscaffold.com:6379`
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
    baseUrl: 'https://api.iqscaffold.com',
    services: {
      gateway: 'https://api.iqscaffold.com',
      auth: 'https://user.iqscaffold.com'
    },
    databases: {
      auth: {
        host: 'prod-postgres-user.iqscaffold.com',
        port: 5432,
        database: 'iqscaffold_user_prod',
        username: 'iqscaffold_prod_user',
        password: process.env.PROD_DB_PASSWORD || 'prod_password',
        url: `postgresql://iqscaffold_prod_user:${process.env.PROD_DB_PASSWORD || 'prod_password'}@prod-postgres-user.iqscaffold.com:5432/iqscaffold_user_prod`
      }
    },
    redis: {
      host: 'prod-redis.iqscaffold.com',
      port: 6379,
      url: `redis://${process.env.PROD_REDIS_PASSWORD ? `:${process.env.PROD_REDIS_PASSWORD}@` : ''}prod-redis.iqscaffold.com:6379`
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
  // Validate database configurations
  if (!config.databases.auth?.url) {
    errors.push('databases.auth.url is required');
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

  // Apply database URL overrides
  if (envVars.DATABASE_USER_URL) {
    overriddenConfig.databases.auth.url = envVars.DATABASE_USER_URL;
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