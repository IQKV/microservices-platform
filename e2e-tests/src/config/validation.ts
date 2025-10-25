/**
 * Environment and configuration validation utilities
 */

import { TestEnvironmentConfig } from '../types/environment.js';
import { getEnvironmentConfig } from './environments.js';

/**
 * Validation result interface
 */
export interface ValidationResult {
  isValid: boolean;
  errors: string[];
  warnings: string[];
}

/**
 * Service health check result
 */
export interface ServiceHealthResult {
  service: string;
  url: string;
  isHealthy: boolean;
  responseTime: number;
  error?: string;
}

/**
 * Environment validation result
 */
export interface EnvironmentValidationResult extends ValidationResult {
  environment: string;
  serviceHealth: ServiceHealthResult[];
  databaseConnections: Array<{
    service: string;
    isConnectable: boolean;
    error?: string;
  }>;
}

/**
 * Validates URL format
 */
function isValidUrl(url: string): boolean {
  try {
    new URL(url);
    return true;
  } catch {
    return false;
  }
}

/**
 * Validates port number
 */
function isValidPort(port: number): boolean {
  return Number.isInteger(port) && port > 0 && port <= 65535;
}

/**
 * Validates timeout value
 */
function isValidTimeout(timeout: number): boolean {
  return Number.isInteger(timeout) && timeout > 0;
}

/**
 * Validates environment configuration structure and values
 */
export function validateEnvironmentConfig(config: TestEnvironmentConfig): ValidationResult {
  const errors: string[] = [];
  const warnings: string[] = [];

  // Validate basic properties
  if (!config.name) {
    errors.push('Environment name is required');
  }

  // Validate URLs
  if (!config.baseUrl) {
    errors.push('Base URL is required');
  } else if (!isValidUrl(config.baseUrl)) {
    errors.push(`Invalid base URL format: ${config.baseUrl}`);
  }

  // Validate service URLs
  if (!config.services.gateway) {
    errors.push('Gateway service URL is required');
  } else if (!isValidUrl(config.services.gateway)) {
    errors.push(`Invalid gateway URL format: ${config.services.gateway}`);
  }

  if (!config.services.auth) {
    errors.push('Auth service URL is required');
  } else if (!isValidUrl(config.services.auth)) {
    errors.push(`Invalid auth service URL format: ${config.services.auth}`);
  }

  if (!config.services.bookstore) {
    errors.push('Bookstore service URL is required');
  } else if (!isValidUrl(config.services.bookstore)) {
    errors.push(`Invalid bookstore service URL format: ${config.services.bookstore}`);
  }

  // Validate database configurations
  ['auth', 'bookstore'].forEach(service => {
    const dbConfig = config.databases[service as keyof typeof config.databases];
    
    if (!dbConfig.host) {
      errors.push(`Database host is required for ${service} service`);
    }
    
    if (!isValidPort(dbConfig.port)) {
      errors.push(`Invalid database port for ${service} service: ${dbConfig.port}`);
    }
    
    if (!dbConfig.database) {
      errors.push(`Database name is required for ${service} service`);
    }
    
    if (!dbConfig.username) {
      errors.push(`Database username is required for ${service} service`);
    }
    
    if (!dbConfig.password) {
      warnings.push(`Database password is empty for ${service} service`);
    }
    
    if (!dbConfig.url) {
      errors.push(`Database URL is required for ${service} service`);
    }
  });

  // Validate Redis configuration
  if (!config.redis.host) {
    errors.push('Redis host is required');
  }
  
  if (!isValidPort(config.redis.port)) {
    errors.push(`Invalid Redis port: ${config.redis.port}`);
  }
  
  if (!config.redis.url) {
    errors.push('Redis URL is required');
  }

  // Validate timeout configurations
  if (!isValidTimeout(config.timeouts.request)) {
    errors.push(`Invalid request timeout: ${config.timeouts.request}`);
  }
  
  if (!isValidTimeout(config.timeouts.test)) {
    errors.push(`Invalid test timeout: ${config.timeouts.test}`);
  }
  
  if (!isValidTimeout(config.timeouts.suite)) {
    errors.push(`Invalid suite timeout: ${config.timeouts.suite}`);
  }
  
  if (!isValidTimeout(config.timeouts.healthCheck)) {
    errors.push(`Invalid health check timeout: ${config.timeouts.healthCheck}`);
  }

  // Validate retry configurations
  if (config.retries.flaky < 0) {
    errors.push(`Flaky retries must be non-negative: ${config.retries.flaky}`);
  }
  
  if (config.retries.failed < 0) {
    errors.push(`Failed retries must be non-negative: ${config.retries.failed}`);
  }
  
  if (config.retries.networkError < 0) {
    errors.push(`Network error retries must be non-negative: ${config.retries.networkError}`);
  }

  // Validate parallelism configurations
  if (config.parallelism.workers <= 0) {
    errors.push(`Workers must be positive: ${config.parallelism.workers}`);
  }
  
  if (config.parallelism.shards <= 0) {
    errors.push(`Shards must be positive: ${config.parallelism.shards}`);
  }

  // Validate screenshot, video, and trace settings
  const validScreenshotValues = ['off', 'only-on-failure', 'on'];
  if (!validScreenshotValues.includes(config.screenshot)) {
    errors.push(`Invalid screenshot setting: ${config.screenshot}. Must be one of: ${validScreenshotValues.join(', ')}`);
  }

  const validVideoValues = ['off', 'on-first-retry', 'retain-on-failure', 'on'];
  if (!validVideoValues.includes(config.video)) {
    errors.push(`Invalid video setting: ${config.video}. Must be one of: ${validVideoValues.join(', ')}`);
  }

  const validTraceValues = ['off', 'on-first-retry', 'retain-on-failure', 'on'];
  if (!validTraceValues.includes(config.trace)) {
    errors.push(`Invalid trace setting: ${config.trace}. Must be one of: ${validTraceValues.join(', ')}`);
  }

  // Performance warnings
  if (config.parallelism.workers > 16) {
    warnings.push(`High number of workers (${config.parallelism.workers}) may impact performance`);
  }
  
  if (config.timeouts.request > 120000) {
    warnings.push(`Very high request timeout (${config.timeouts.request}ms) may slow down tests`);
  }

  return {
    isValid: errors.length === 0,
    errors,
    warnings
  };
}

/**
 * Performs a health check on a service endpoint
 */
export async function checkServiceHealth(
  serviceName: string,
  url: string,
  timeout: number = 10000
): Promise<ServiceHealthResult> {
  const startTime = Date.now();
  
  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), timeout);
    
    const response = await fetch(`${url}/actuator/health`, {
      method: 'GET',
      headers: {
        'Accept': 'application/json',
        'User-Agent': 'Gripday-E2E-Health-Check/1.0.0'
      },
      signal: controller.signal
    });
    
    clearTimeout(timeoutId);
    const responseTime = Date.now() - startTime;
    
    const result: ServiceHealthResult = {
      service: serviceName,
      url,
      isHealthy: response.ok,
      responseTime
    };
    
    if (!response.ok) {
      result.error = `HTTP ${response.status}: ${response.statusText}`;
    }
    
    return result;
  } catch (error) {
    const responseTime = Date.now() - startTime;
    return {
      service: serviceName,
      url,
      isHealthy: false,
      responseTime,
      error: error instanceof Error ? error.message : 'Unknown error'
    };
  }
}

/**
 * Validates the current environment and performs health checks
 */
export async function validateEnvironment(): Promise<EnvironmentValidationResult> {
  const config = getEnvironmentConfig();
  const configValidation = validateEnvironmentConfig(config);
  
  // Perform service health checks
  const serviceHealthPromises = [
    checkServiceHealth('gateway', config.services.gateway, config.timeouts.healthCheck),
    checkServiceHealth('auth', config.services.auth, config.timeouts.healthCheck),
    checkServiceHealth('bookstore', config.services.bookstore, config.timeouts.healthCheck)
  ];
  
  const serviceHealth = await Promise.all(serviceHealthPromises);
  
  // Check database connectivity (simplified - just URL validation for now)
  const databaseConnections = [
    {
      service: 'auth',
      isConnectable: !!config.databases.auth.url && isValidUrl(config.databases.auth.url.replace('postgresql://', 'http://'))
    },
    {
      service: 'bookstore',
      isConnectable: !!config.databases.bookstore.url && isValidUrl(config.databases.bookstore.url.replace('postgresql://', 'http://'))
    }
  ];
  
  // Add service health errors to overall validation
  const serviceErrors = serviceHealth
    .filter(health => !health.isHealthy)
    .map(health => `Service ${health.service} is unhealthy: ${health.error}`);
  
  return {
    environment: config.name,
    isValid: configValidation.isValid && serviceHealth.every(h => h.isHealthy),
    errors: [...configValidation.errors, ...serviceErrors],
    warnings: configValidation.warnings,
    serviceHealth,
    databaseConnections
  };
}

/**
 * Waits for services to become healthy
 */
export async function waitForServicesHealthy(
  maxWaitTime: number = 120000,
  checkInterval: number = 5000
): Promise<boolean> {
  const config = getEnvironmentConfig();
  const startTime = Date.now();
  
  while (Date.now() - startTime < maxWaitTime) {
    const serviceHealth = await Promise.all([
      checkServiceHealth('gateway', config.services.gateway, config.timeouts.healthCheck),
      checkServiceHealth('auth', config.services.auth, config.timeouts.healthCheck),
      checkServiceHealth('bookstore', config.services.bookstore, config.timeouts.healthCheck)
    ]);
    
    if (serviceHealth.every(health => health.isHealthy)) {
      return true;
    }
    
    // Wait before next check
    await new Promise(resolve => setTimeout(resolve, checkInterval));
  }
  
  return false;
}

/**
 * Prints environment validation results in a readable format
 */
export function printValidationResults(result: EnvironmentValidationResult): void {
  console.log(`\n=== Environment Validation: ${result.environment.toUpperCase()} ===`);
  
  if (result.isValid) {
    console.log('✅ Environment configuration is valid');
  } else {
    console.log('❌ Environment configuration has errors');
  }
  
  if (result.errors.length > 0) {
    console.log('\n🚨 Errors:');
    result.errors.forEach(error => console.log(`  - ${error}`));
  }
  
  if (result.warnings.length > 0) {
    console.log('\n⚠️  Warnings:');
    result.warnings.forEach(warning => console.log(`  - ${warning}`));
  }
  
  console.log('\n🏥 Service Health:');
  result.serviceHealth.forEach(health => {
    const status = health.isHealthy ? '✅' : '❌';
    const time = `${health.responseTime}ms`;
    console.log(`  ${status} ${health.service}: ${time}${health.error ? ` (${health.error})` : ''}`);
  });
  
  console.log('\n💾 Database Connections:');
  result.databaseConnections.forEach(db => {
    const status = db.isConnectable ? '✅' : '❌';
    console.log(`  ${status} ${db.service}${db.error ? ` (${db.error})` : ''}`);
  });
  
  console.log('');
}