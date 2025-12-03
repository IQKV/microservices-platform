/**
 * Database helper utilities for test data setup, cleanup, and isolation
 * Provides connection management, transaction-based isolation, and seeding capabilities
 */

import { Pool, PoolClient, QueryResult, PoolConfig } from 'pg';
import { 
  DatabaseConfig, 
  TestEnvironmentConfig 
} from '../types/environment.js';
import { 
  buildConnectionUrl, 
  DEFAULT_POOL_CONFIG, 
  DEFAULT_TRANSACTION_CONFIG,
  TransactionConfig,
  CLEANUP_QUERIES,
  HEALTH_CHECK_QUERIES,
  DATABASE_SCHEMAS,
  generateTestId
} from '../config/database.js';
import { getEnvironmentConfig } from '../config/environments.js';
import { TestDataRelationships } from './test-data-factory.js';

/**
 * Database connection manager for test environments
 */
export class DatabaseConnectionManager {
  private pools: Map<string, Pool> = new Map();
  private config: TestEnvironmentConfig;

  constructor(config?: TestEnvironmentConfig) {
    this.config = config || getEnvironmentConfig();
  }

  /**
   * Gets or creates a connection pool for a specific database
   */
  async getPool(service: 'auth'): Promise<Pool> {
    if (this.pools.has(service)) {
      return this.pools.get(service)!;
    }

    const dbConfig = this.config.databases[service];
    const poolConfig: PoolConfig = {
      connectionString: buildConnectionUrl(dbConfig),
      min: DEFAULT_POOL_CONFIG.min,
      max: DEFAULT_POOL_CONFIG.max,
      idleTimeoutMillis: DEFAULT_POOL_CONFIG.idleTimeoutMillis,
      application_name: 'iqscaffold-e2e-tests'
    };

    const pool = new Pool(poolConfig);
    this.pools.set(service, pool);

    // Test the connection
    try {
      const client = await pool.connect();
      await client.query(HEALTH_CHECK_QUERIES[service]);
      client.release();
    } catch (error) {
      await pool.end();
      this.pools.delete(service);
      throw new Error(`Failed to connect to ${service} database: ${error}`);
    }

    return pool;
  }

  /**
   * Gets a client from the connection pool
   */
  async getClient(service: 'auth'): Promise<PoolClient> {
    const pool = await this.getPool(service);
    return pool.connect();
  }

  /**
   * Closes all connection pools
   */
  async closeAll(): Promise<void> {
    const closePromises = Array.from(this.pools.values()).map(pool => pool.end());
    await Promise.all(closePromises);
    this.pools.clear();
  }

  /**
   * Checks health of all database connections
   */
  async checkHealth(): Promise<Record<string, boolean>> {
    const results: Record<string, boolean> = {};
    
    const service: 'auth' = 'auth';
    try {
      const client = await this.getClient(service);
      await client.query(HEALTH_CHECK_QUERIES[service]);
      client.release();
      results[service] = true;
    } catch (error) {
      results[service] = false;
    }

    return results;
  }
}

/**
 * Transaction manager for test isolation
 */
export class TestTransactionManager {
  private connectionManager: DatabaseConnectionManager;
  private activeTransactions: Map<string, PoolClient> = new Map();

  constructor(connectionManager: DatabaseConnectionManager) {
    this.connectionManager = connectionManager;
  }

  /**
   * Begins a transaction for test isolation
   */
  async beginTransaction(
    service: 'auth', 
    testId: string = generateTestId(),
    config: TransactionConfig = DEFAULT_TRANSACTION_CONFIG
  ): Promise<string> {
    const client = await this.connectionManager.getClient(service);
    
    try {
      await client.query('BEGIN');
      
      // Set transaction isolation level
      if (config.isolationLevel !== 'READ_COMMITTED') {
        await client.query(`SET TRANSACTION ISOLATION LEVEL ${config.isolationLevel}`);
      }
      
      // Set read-only if specified
      if (config.readOnly) {
        await client.query('SET TRANSACTION READ ONLY');
      }
      
      // Set statement timeout
      if (config.timeout > 0) {
        await client.query(`SET statement_timeout = ${config.timeout}`);
      }

      const transactionId = `${service}_${testId}`;
      this.activeTransactions.set(transactionId, client);
      
      return transactionId;
    } catch (error) {
      client.release();
      throw new Error(`Failed to begin transaction for ${service}: ${error}`);
    }
  }

  /**
   * Commits a transaction
   */
  async commitTransaction(transactionId: string): Promise<void> {
    const client = this.activeTransactions.get(transactionId);
    if (!client) {
      throw new Error(`Transaction ${transactionId} not found`);
    }

    try {
      await client.query('COMMIT');
    } finally {
      client.release();
      this.activeTransactions.delete(transactionId);
    }
  }

  /**
   * Rolls back a transaction
   */
  async rollbackTransaction(transactionId: string): Promise<void> {
    const client = this.activeTransactions.get(transactionId);
    if (!client) {
      throw new Error(`Transaction ${transactionId} not found`);
    }

    try {
      await client.query('ROLLBACK');
    } finally {
      client.release();
      this.activeTransactions.delete(transactionId);
    }
  }

  /**
   * Executes a query within a transaction
   */
  async query(transactionId: string, text: string, params?: any[]): Promise<QueryResult> {
    const client = this.activeTransactions.get(transactionId);
    if (!client) {
      throw new Error(`Transaction ${transactionId} not found`);
    }

    return client.query(text, params);
  }

  /**
   * Rolls back all active transactions
   */
  async rollbackAll(): Promise<void> {
    const rollbackPromises = Array.from(this.activeTransactions.keys()).map(
      transactionId => this.rollbackTransaction(transactionId)
    );
    await Promise.all(rollbackPromises);
  }

  /**
   * Gets the client for a transaction (for direct access)
   */
  getTransactionClient(transactionId: string): PoolClient | undefined {
    return this.activeTransactions.get(transactionId);
  }
}

/**
 * Test data seeder for known datasets
 */
export class TestDataSeeder {
  private connectionManager: DatabaseConnectionManager;

  constructor(connectionManager: DatabaseConnectionManager) {
    this.connectionManager = connectionManager;
  }

  /**
   * Seeds basic test data for user service
   */
  async seedAuthData(tenantId?: string): Promise<void> {
    const client = await this.connectionManager.getClient('auth');
    
    try {
      await client.query('BEGIN');

      // Create test tenant if provided
      if (tenantId) {
        await client.query(`
          INSERT INTO tenants (id, name, subdomain, enabled, settings, created_at, updated_at)
          VALUES ($1, $2, $3, $4, $5, NOW(), NOW())
          ON CONFLICT (id) DO NOTHING
        `, [tenantId, `Test Tenant ${tenantId}`, `test-${tenantId}`, true, JSON.stringify({ allowRegistration: true })]);
      }

      // Create test authorities/roles
      const authorities = ['USER', 'ADMIN', 'MANAGER'];
      for (const authority of authorities) {
        await client.query(`
          INSERT INTO authorities (name, description, created_at, updated_at)
          VALUES ($1, $2, NOW(), NOW())
          ON CONFLICT (name) DO NOTHING
        `, [authority, `${authority} role for testing`]);
      }

      await client.query('COMMIT');
    } catch (error) {
      await client.query('ROLLBACK');
      throw new Error(`Failed to seed auth data: ${error}`);
    } finally {
      client.release();
    }
  }

  /**
   * Seeds all basic test data
   */
  async seedAllData(tenantId?: string): Promise<void> {
    await this.seedAuthData(tenantId);
  }
}

/**
 * Test data cleanup manager
 */
export class TestDataCleanup {
  private connectionManager: DatabaseConnectionManager;

  constructor(connectionManager: DatabaseConnectionManager) {
    this.connectionManager = connectionManager;
  }

  /**
   * Cleans up test data based on relationships
   */
  async cleanupTestData(relationships: TestDataRelationships): Promise<void> {
    await this.cleanupAuthData(relationships);
  }

  /**
   * Cleans up user service test data
   */
  async cleanupAuthData(relationships: TestDataRelationships): Promise<void> {
    const client = await this.connectionManager.getClient('auth');
    
    try {
      await client.query('BEGIN');

      // Clean up users and related data
      if (relationships.users.size > 0) {
        const userIds = Array.from(relationships.users);
        
        // Clean up user authorities
        await client.query(`
          DELETE FROM user_authorities WHERE user_id = ANY($1)
        `, [userIds]);

        // Clean up tenant users
        await client.query(`
          DELETE FROM tenant_users WHERE user_id = ANY($1)
        `, [userIds]);

        // Clean up email verifications
        await client.query(`
          DELETE FROM email_verifications WHERE user_id = ANY($1)
        `, [userIds]);

        // Clean up password reset tokens
        await client.query(`
          DELETE FROM password_reset_tokens WHERE user_id = ANY($1)
        `, [userIds]);

        // Clean up users
        await client.query(`
          DELETE FROM users WHERE id = ANY($1)
        `, [userIds]);
      }

      // Clean up tenants
      if (relationships.tenants.size > 0) {
        const tenantIds = Array.from(relationships.tenants);
        await client.query(`
          DELETE FROM tenants WHERE id = ANY($1)
        `, [tenantIds]);
      }

      await client.query('COMMIT');
    } catch (error) {
      await client.query('ROLLBACK');
      throw new Error(`Failed to cleanup auth data: ${error}`);
    } finally {
      client.release();
    }
  }

  /**
   * Performs general cleanup using predefined queries
   */
  async performGeneralCleanup(): Promise<void> {
    await this.executeCleanupQueries('auth', CLEANUP_QUERIES.auth);
  }

  /**
   * Executes cleanup queries for a specific service
   */
  private async executeCleanupQueries(service: 'auth', queries: readonly string[]): Promise<void> {
    const client = await this.connectionManager.getClient(service);
    
    try {
      await client.query('BEGIN');
      
      for (const query of queries) {
        try {
          await client.query(query);
        } catch (error) {
          console.warn(`Cleanup query failed for ${service}: ${query}`, error);
          // Continue with other queries even if one fails
        }
      }
      
      await client.query('COMMIT');
    } catch (error) {
      await client.query('ROLLBACK');
      throw new Error(`Failed to execute cleanup queries for ${service}: ${error}`);
    } finally {
      client.release();
    }
  }

  /**
   * Truncates all test tables (use with caution)
   */
  async truncateAllTestTables(): Promise<void> {
    await this.truncateServiceTables('auth');
  }

  /**
   * Truncates tables for a specific service
   */
  private async truncateServiceTables(service: 'auth'): Promise<void> {
    const client = await this.connectionManager.getClient(service);
    const tables = DATABASE_SCHEMAS[service].tables;
    
    try {
      await client.query('BEGIN');
      
      // Disable foreign key checks temporarily
      await client.query('SET session_replication_role = replica');
      
      for (const table of tables) {
        await client.query(`TRUNCATE TABLE ${table} RESTART IDENTITY CASCADE`);
      }
      
      // Re-enable foreign key checks
      await client.query('SET session_replication_role = DEFAULT');
      
      await client.query('COMMIT');
    } catch (error) {
      await client.query('ROLLBACK');
      throw new Error(`Failed to truncate tables for ${service}: ${error}`);
    } finally {
      client.release();
    }
  }
}

/**
 * Main database helper class that combines all functionality
 */
export class DatabaseHelper {
  public readonly connectionManager: DatabaseConnectionManager;
  public readonly transactionManager: TestTransactionManager;
  public readonly seeder: TestDataSeeder;
  public readonly cleanup: TestDataCleanup;

  constructor(config?: TestEnvironmentConfig) {
    this.connectionManager = new DatabaseConnectionManager(config);
    this.transactionManager = new TestTransactionManager(this.connectionManager);
    this.seeder = new TestDataSeeder(this.connectionManager);
    this.cleanup = new TestDataCleanup(this.connectionManager);
  }

  /**
   * Initializes the database helper and checks connections
   */
  async initialize(): Promise<void> {
    const health = await this.connectionManager.checkHealth();
    const unhealthyServices = Object.entries(health)
      .filter(([, healthy]) => !healthy)
      .map(([service]) => service);

    if (unhealthyServices.length > 0) {
      throw new Error(`Database health check failed for services: ${unhealthyServices.join(', ')}`);
    }
  }

  /**
   * Executes a query against a specific service database
   */
  async query(service: 'auth', text: string, params?: any[]): Promise<QueryResult> {
    const client = await this.connectionManager.getClient(service);
    try {
      return await client.query(text, params);
    } finally {
      client.release();
    }
  }

  /**
   * Executes multiple queries in a transaction
   */
  async executeInTransaction(
    service: 'auth',
    queries: Array<{ text: string; params?: any[] }>,
    config?: TransactionConfig
  ): Promise<QueryResult[]> {
    const testId = generateTestId();
    const transactionId = await this.transactionManager.beginTransaction(service, testId, config);
    
    try {
      const results: QueryResult[] = [];
      for (const query of queries) {
        const result = await this.transactionManager.query(transactionId, query.text, query.params);
        results.push(result);
      }
      
      await this.transactionManager.commitTransaction(transactionId);
      return results;
    } catch (error) {
      await this.transactionManager.rollbackTransaction(transactionId);
      throw error;
    }
  }

  /**
   * Sets up test environment with seeded data
   */
  async setupTestEnvironment(tenantId?: string): Promise<void> {
    await this.initialize();
    await this.seeder.seedAllData(tenantId);
  }

  /**
   * Tears down test environment and cleans up data
   */
  async teardownTestEnvironment(relationships?: TestDataRelationships): Promise<void> {
    try {
      // Rollback any active transactions
      await this.transactionManager.rollbackAll();
      
      // Clean up test data
      if (relationships) {
        await this.cleanup.cleanupTestData(relationships);
      } else {
        await this.cleanup.performGeneralCleanup();
      }
    } finally {
      // Always close connections
      await this.connectionManager.closeAll();
    }
  }
}

/**
 * Default database helper instance
 */
export const databaseHelper = new DatabaseHelper();

/**
 * Creates a database helper with custom configuration
 */
export function createDatabaseHelper(config: TestEnvironmentConfig): DatabaseHelper {
  return new DatabaseHelper(config);
}