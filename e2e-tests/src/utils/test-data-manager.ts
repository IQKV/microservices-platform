/**
 * Comprehensive test data management system
 * Combines TestDataFactory and DatabaseHelper for complete test data lifecycle management
 */

import { TestDataFactory, TestDataRelationships, CreateUserOptions, CreateBookOptions, CreateTenantOptions } from './test-data-factory.js';
import { DatabaseHelper, TestTransactionManager } from './database-helper.js';
import { UserData, BookData, TenantData, CreateUserRequest, CreateBookRequest, CreateTenantRequest } from '../types/api-responses.js';
import { TestEnvironmentConfig } from '../types/environment.js';
import { generateTestId } from '../config/database.js';
import { getEnvironmentConfig } from '../config/environments.js';

/**
 * Test data management configuration
 */
export interface TestDataManagerConfig {
  environment?: TestEnvironmentConfig;
  useTransactions?: boolean;
  autoCleanup?: boolean;
  tenantId?: string;
  seed?: number;
  isolationPrefix?: string;
}

/**
 * Test data creation result
 */
export interface TestDataCreationResult<T> {
  data: T;
  id: string | number;
  relationships: TestDataRelationships;
  transactionId?: string;
}

/**
 * Batch test data creation result
 */
export interface BatchTestDataResult<T> {
  items: T[];
  relationships: TestDataRelationships;
  transactionId?: string;
  count: number;
}

/**
 * Test data manager that provides high-level test data operations
 */
export class TestDataManager {
  private factory: TestDataFactory;
  private databaseHelper: DatabaseHelper;
  private config: TestDataManagerConfig;
  private activeTransactions: Map<string, string> = new Map();
  private testId: string;

  constructor(config: TestDataManagerConfig = {}) {
    this.config = {
      useTransactions: true,
      autoCleanup: true,
      ...config
    };

    this.testId = generateTestId();
    
    // Initialize factory
    this.factory = new TestDataFactory({
      ...(this.config.tenantId ? { tenantId: this.config.tenantId } : {}),
      ...(this.config.seed ? { seed: this.config.seed } : {}),
      ...(this.config.isolationPrefix ? { isolationPrefix: this.config.isolationPrefix } : {})
    });

    // Initialize database helper
    this.databaseHelper = new DatabaseHelper(this.config.environment);
  }

  /**
   * Initializes the test data manager
   */
  async initialize(): Promise<void> {
    await this.databaseHelper.initialize();
    
    // Seed basic data if needed
    if (this.config.tenantId) {
      await this.databaseHelper.seeder.seedAllData(this.config.tenantId);
    }
  }

  /**
   * Creates a user with database persistence
   */
  async createUser(
    overrides: Partial<CreateUserRequest> = {},
    options: CreateUserOptions = {}
  ): Promise<TestDataCreationResult<UserData>> {
    const userRequest = this.factory.createUserRequest(overrides, options);
    
    let transactionId: string | undefined;
    if (this.config.useTransactions) {
      transactionId = await this.databaseHelper.transactionManager.beginTransaction('auth', this.testId);
      this.activeTransactions.set(`user_${this.testId}`, transactionId);
    }

    try {
      // Insert user into database
      const result = await this.insertUserToDatabase(userRequest, transactionId);
      
      // Create UserData from the result
      const userData = this.factory.createUserData({
        id: result.id,
        username: userRequest.username,
        email: userRequest.email,
        ...(userRequest.firstName ? { firstName: userRequest.firstName } : {}),
        ...(userRequest.lastName ? { lastName: userRequest.lastName } : {}),
        roles: userRequest.roles || ['USER'],
        enabled: userRequest.enabled !== false,
        emailVerified: true
      }, options);

      return {
        data: userData,
        id: userData.id,
        relationships: this.factory.getRelationships(),
        ...(transactionId ? { transactionId } : {})
      };
    } catch (error) {
      if (transactionId) {
        await this.databaseHelper.transactionManager.rollbackTransaction(transactionId);
        this.activeTransactions.delete(`user_${this.testId}`);
      }
      throw error;
    }
  }

  /**
   * Creates multiple users with database persistence
   */
  async createUsers(
    count: number,
    overrides: Partial<CreateUserRequest> = {},
    options: CreateUserOptions = {}
  ): Promise<BatchTestDataResult<UserData>> {
    const users: UserData[] = [];
    let transactionId: string | undefined;

    if (this.config.useTransactions) {
      transactionId = await this.databaseHelper.transactionManager.beginTransaction('auth', this.testId);
      this.activeTransactions.set(`users_${this.testId}`, transactionId);
    }

    try {
      for (let i = 0; i < count; i++) {
        const userRequest = this.factory.createUserRequest(overrides, options);
        const result = await this.insertUserToDatabase(userRequest, transactionId);
        
        const userData = this.factory.createUserData({
          id: result.id,
          username: userRequest.username,
          email: userRequest.email,
          ...(userRequest.firstName ? { firstName: userRequest.firstName } : {}),
          ...(userRequest.lastName ? { lastName: userRequest.lastName } : {}),
          roles: userRequest.roles || ['USER'],
          enabled: userRequest.enabled !== false,
          emailVerified: true
        }, options);

        users.push(userData);
      }

      return {
        items: users,
        relationships: this.factory.getRelationships(),
        count: users.length,
        ...(transactionId ? { transactionId } : {})
      };
    } catch (error) {
      if (transactionId) {
        await this.databaseHelper.transactionManager.rollbackTransaction(transactionId);
        this.activeTransactions.delete(`users_${this.testId}`);
      }
      throw error;
    }
  }

  /**
   * Creates a book with database persistence
   */
  async createBook(
    overrides: Partial<CreateBookRequest> = {},
    options: CreateBookOptions = {}
  ): Promise<TestDataCreationResult<BookData>> {
    const bookRequest = this.factory.createBookRequest(overrides, options);
    
    let transactionId: string | undefined;
    if (this.config.useTransactions) {
      transactionId = await this.databaseHelper.transactionManager.beginTransaction('bookstore', this.testId);
      this.activeTransactions.set(`book_${this.testId}`, transactionId);
    }

    try {
      // Insert book into database
      const result = await this.insertBookToDatabase(bookRequest, transactionId);
      
      // Create BookData from the result
      const bookData = this.factory.createBookData({
        id: result.id,
        title: bookRequest.title,
        author: bookRequest.author,
        isbn: bookRequest.isbn,
        ...(bookRequest.description ? { description: bookRequest.description } : {}),
        price: bookRequest.price,
        ...(bookRequest.currency ? { currency: bookRequest.currency } : {}),
        ...(bookRequest.stock !== undefined ? { stock: bookRequest.stock } : {}),
        ...(bookRequest.category ? { category: bookRequest.category } : {})
      }, options);

      return {
        data: bookData,
        id: bookData.id,
        relationships: this.factory.getRelationships(),
        ...(transactionId ? { transactionId } : {})
      };
    } catch (error) {
      if (transactionId) {
        await this.databaseHelper.transactionManager.rollbackTransaction(transactionId);
        this.activeTransactions.delete(`book_${this.testId}`);
      }
      throw error;
    }
  }

  /**
   * Creates multiple books with database persistence
   */
  async createBooks(
    count: number,
    overrides: Partial<CreateBookRequest> = {},
    options: CreateBookOptions = {}
  ): Promise<BatchTestDataResult<BookData>> {
    const books: BookData[] = [];
    let transactionId: string | undefined;

    if (this.config.useTransactions) {
      transactionId = await this.databaseHelper.transactionManager.beginTransaction('bookstore', this.testId);
      this.activeTransactions.set(`books_${this.testId}`, transactionId);
    }

    try {
      for (let i = 0; i < count; i++) {
        const bookRequest = this.factory.createBookRequest(overrides, options);
        const result = await this.insertBookToDatabase(bookRequest, transactionId);
        
        const bookData = this.factory.createBookData({
          id: result.id,
          title: bookRequest.title,
          author: bookRequest.author,
          isbn: bookRequest.isbn,
          ...(bookRequest.description ? { description: bookRequest.description } : {}),
          price: bookRequest.price,
          ...(bookRequest.currency ? { currency: bookRequest.currency } : {}),
          ...(bookRequest.stock !== undefined ? { stock: bookRequest.stock } : {}),
          ...(bookRequest.category ? { category: bookRequest.category } : {})
        }, options);

        books.push(bookData);
      }

      return {
        items: books,
        relationships: this.factory.getRelationships(),
        count: books.length,
        ...(transactionId ? { transactionId } : {})
      };
    } catch (error) {
      if (transactionId) {
        await this.databaseHelper.transactionManager.rollbackTransaction(transactionId);
        this.activeTransactions.delete(`books_${this.testId}`);
      }
      throw error;
    }
  }

  /**
   * Creates a tenant with database persistence
   */
  async createTenant(
    overrides: Partial<CreateTenantRequest> = {},
    options: CreateTenantOptions = {}
  ): Promise<TestDataCreationResult<TenantData>> {
    const tenantRequest = this.factory.createTenantRequest(overrides, options);
    
    let transactionId: string | undefined;
    if (this.config.useTransactions) {
      transactionId = await this.databaseHelper.transactionManager.beginTransaction('auth', this.testId);
      this.activeTransactions.set(`tenant_${this.testId}`, transactionId);
    }

    try {
      // Insert tenant into database
      const result = await this.insertTenantToDatabase(tenantRequest, transactionId);
      
      // Create TenantData from the result
      const tenantData = this.factory.createTenantData({
        id: result.id,
        name: tenantRequest.name,
        ...(tenantRequest.domain ? { domain: tenantRequest.domain } : {}),
        ...(tenantRequest.subdomain ? { subdomain: tenantRequest.subdomain } : {}),
        enabled: tenantRequest.enabled !== false,
        settings: tenantRequest.settings || {}
      }, options);

      return {
        data: tenantData,
        id: tenantData.id,
        relationships: this.factory.getRelationships(),
        ...(transactionId ? { transactionId } : {})
      };
    } catch (error) {
      if (transactionId) {
        await this.databaseHelper.transactionManager.rollbackTransaction(transactionId);
        this.activeTransactions.delete(`tenant_${this.testId}`);
      }
      throw error;
    }
  }

  /**
   * Creates a complete test dataset with tenant, users, and books
   */
  async createCompleteDataset(
    tenantOverrides: Partial<CreateTenantRequest> = {},
    userCount: number = 3,
    bookCount: number = 5
  ): Promise<{
    tenant: TestDataCreationResult<TenantData>;
    users: BatchTestDataResult<UserData>;
    books: BatchTestDataResult<BookData>;
  }> {
    // Create tenant first
    const tenant = await this.createTenant(tenantOverrides);
    
    // Set tenant context for subsequent creations
    this.factory.setTenantContext(tenant.data.id);
    
    // Create users for the tenant
    const users = await this.createUsers(userCount, {}, { tenantId: tenant.data.id });
    
    // Create books for the tenant
    const books = await this.createBooks(bookCount, {}, { tenantId: tenant.data.id });

    return { tenant, users, books };
  }

  /**
   * Commits all active transactions
   */
  async commitTransactions(): Promise<void> {
    const commitPromises = Array.from(this.activeTransactions.values()).map(
      transactionId => this.databaseHelper.transactionManager.commitTransaction(transactionId)
    );
    
    await Promise.all(commitPromises);
    this.activeTransactions.clear();
  }

  /**
   * Rolls back all active transactions
   */
  async rollbackTransactions(): Promise<void> {
    const rollbackPromises = Array.from(this.activeTransactions.values()).map(
      transactionId => this.databaseHelper.transactionManager.rollbackTransaction(transactionId)
    );
    
    await Promise.all(rollbackPromises);
    this.activeTransactions.clear();
  }

  /**
   * Cleans up all test data
   */
  async cleanup(): Promise<void> {
    try {
      // Rollback any active transactions
      await this.rollbackTransactions();
      
      // Clean up test data
      const relationships = this.factory.getRelationships();
      await this.databaseHelper.cleanup.cleanupTestData(relationships);
      
      // Clear factory relationships
      this.factory.clearRelationships();
    } finally {
      // Always close database connections
      await this.databaseHelper.connectionManager.closeAll();
    }
  }

  /**
   * Gets the current test data relationships
   */
  getRelationships(): TestDataRelationships {
    return this.factory.getRelationships();
  }

  /**
   * Gets the test data factory instance
   */
  getFactory(): TestDataFactory {
    return this.factory;
  }

  /**
   * Gets the database helper instance
   */
  getDatabaseHelper(): DatabaseHelper {
    return this.databaseHelper;
  }

  /**
   * Inserts a user into the database
   */
  private async insertUserToDatabase(
    userRequest: CreateUserRequest, 
    transactionId?: string
  ): Promise<{ id: number }> {
    const query = `
      INSERT INTO users (username, email, password, first_name, last_name, enabled, email_verified, tenant_id, created_at, updated_at)
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8, NOW(), NOW())
      RETURNING id
    `;
    
    const params = [
      userRequest.username,
      userRequest.email,
      userRequest.password, // In real implementation, this should be hashed
      userRequest.firstName,
      userRequest.lastName,
      userRequest.enabled !== false,
      true, // email_verified
      userRequest.tenantId
    ];

    let result;
    if (transactionId) {
      result = await this.databaseHelper.transactionManager.query(transactionId, query, params);
    } else {
      result = await this.databaseHelper.query('auth', query, params);
    }

    const userId = result.rows[0].id;

    // Insert user roles if provided
    if (userRequest.roles && userRequest.roles.length > 0) {
      for (const role of userRequest.roles) {
        const roleQuery = `
          INSERT INTO user_authorities (user_id, authority_id)
          SELECT $1, id FROM authorities WHERE name = $2
        `;
        
        if (transactionId) {
          await this.databaseHelper.transactionManager.query(transactionId, roleQuery, [userId, role]);
        } else {
          await this.databaseHelper.query('auth', roleQuery, [userId, role]);
        }
      }
    }

    return { id: userId };
  }

  /**
   * Inserts a book into the database
   */
  private async insertBookToDatabase(
    bookRequest: CreateBookRequest, 
    transactionId?: string
  ): Promise<{ id: number }> {
    const query = `
      INSERT INTO books (title, author, isbn, description, price, currency, stock, category, tenant_id, created_at, updated_at)
      VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, NOW(), NOW())
      RETURNING id
    `;
    
    const params = [
      bookRequest.title,
      bookRequest.author,
      bookRequest.isbn,
      bookRequest.description,
      bookRequest.price,
      bookRequest.currency || 'USD',
      bookRequest.stock || 0,
      bookRequest.category,
      this.config.tenantId
    ];

    let result;
    if (transactionId) {
      result = await this.databaseHelper.transactionManager.query(transactionId, query, params);
    } else {
      result = await this.databaseHelper.query('bookstore', query, params);
    }

    const bookId = result.rows[0].id;

    // Create inventory record
    const inventoryQuery = `
      INSERT INTO inventory (book_id, quantity, available_quantity, created_at, updated_at)
      VALUES ($1, $2, $3, NOW(), NOW())
    `;
    
    const stock = bookRequest.stock || 0;
    if (transactionId) {
      await this.databaseHelper.transactionManager.query(transactionId, inventoryQuery, [bookId, stock, stock]);
    } else {
      await this.databaseHelper.query('bookstore', inventoryQuery, [bookId, stock, stock]);
    }

    return { id: bookId };
  }

  /**
   * Inserts a tenant into the database
   */
  private async insertTenantToDatabase(
    tenantRequest: CreateTenantRequest, 
    transactionId?: string
  ): Promise<{ id: string }> {
    const tenantId = generateTestId();
    const query = `
      INSERT INTO tenants (id, name, domain, subdomain, enabled, settings, created_at, updated_at)
      VALUES ($1, $2, $3, $4, $5, $6, NOW(), NOW())
      RETURNING id
    `;
    
    const params = [
      tenantId,
      tenantRequest.name,
      tenantRequest.domain,
      tenantRequest.subdomain,
      tenantRequest.enabled !== false,
      JSON.stringify(tenantRequest.settings || {})
    ];

    if (transactionId) {
      await this.databaseHelper.transactionManager.query(transactionId, query, params);
    } else {
      await this.databaseHelper.query('auth', query, params);
    }

    return { id: tenantId };
  }
}

/**
 * Creates a test data manager with default configuration
 */
export function createTestDataManager(config: TestDataManagerConfig = {}): TestDataManager {
  return new TestDataManager(config);
}

/**
 * Creates a test data manager for a specific tenant
 */
export function createTenantTestDataManager(tenantId: string, config: TestDataManagerConfig = {}): TestDataManager {
  return new TestDataManager({ ...config, tenantId });
}