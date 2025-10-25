/**
 * Test data factory utilities for generating realistic test data using Faker.js
 * Provides factory methods for users, books, tenants, and other entities with relationship management
 */

import { faker } from '@faker-js/faker';
import { 
  UserRegistrationData, 
  UserData, 
  BookData, 
  CreateBookRequest,
  TenantData, 
  CreateTenantRequest,
  CreateUserRequest 
} from '../types/api-responses.js';
import { 
  TestUserData, 
  TestTenantData, 
  TestBookData, 
  TEST_DATA_CONFIG 
} from '../config/test-data.js';
import { 
  generateTestEmail, 
  generateTestUsername, 
  generateTestSubdomain, 
  generateTestId 
} from '../config/database.js';

/**
 * Configuration options for test data generation
 */
export interface TestDataFactoryConfig {
  tenantId?: string;
  locale?: string;
  seed?: number;
  useRealisticData?: boolean;
  isolationPrefix?: string;
}

/**
 * Options for user creation
 */
export interface CreateUserOptions {
  tenantId?: string;
  roles?: string[];
  enabled?: boolean;
  emailVerified?: boolean;
  department?: string;
  organizationId?: string;
}

/**
 * Options for book creation
 */
export interface CreateBookOptions {
  tenantId?: string;
  category?: string;
  tags?: string[];
  inStock?: boolean;
  available?: boolean;
}

/**
 * Options for tenant creation
 */
export interface CreateTenantOptions {
  enabled?: boolean;
  settings?: Record<string, unknown>;
  domain?: string;
}

/**
 * Relationship tracking for test data cleanup
 */
export interface TestDataRelationships {
  users: Set<number>;
  books: Set<number>;
  tenants: Set<string>;
  userTenantMappings: Map<number, string>;
  bookTenantMappings: Map<number, string>;
}

/**
 * Test data factory for generating realistic test data with relationship management
 */
export class TestDataFactory {
  private config: TestDataFactoryConfig;
  private relationships: TestDataRelationships;
  private currentTenantId?: string;

  constructor(config: TestDataFactoryConfig = {}) {
    this.config = {
      locale: 'en',
      useRealisticData: true,
      isolationPrefix: 'test_',
      ...config
    };

    // Set faker locale and seed if provided
    faker.locale = this.config.locale!;
    if (this.config.seed) {
      faker.seed(this.config.seed);
    }

    // Initialize relationship tracking
    this.relationships = {
      users: new Set(),
      books: new Set(),
      tenants: new Set(),
      userTenantMappings: new Map(),
      bookTenantMappings: new Map()
    };

    this.currentTenantId = this.config.tenantId;
  }

  /**
   * Sets the current tenant context for data creation
   */
  setTenantContext(tenantId: string): void {
    this.currentTenantId = tenantId;
  }

  /**
   * Gets the current tenant context
   */
  getTenantContext(): string | undefined {
    return this.currentTenantId;
  }

  /**
   * Creates a realistic user registration data object
   */
  createUserRegistrationData(overrides: Partial<UserRegistrationData> = {}): UserRegistrationData {
    const firstName = faker.name.firstName();
    const lastName = faker.name.lastName();
    const username = overrides.username || generateTestUsername(firstName.toLowerCase());
    const email = overrides.email || generateTestEmail(username);

    const userData: UserRegistrationData = {
      username,
      email,
      password: this.generateSecurePassword(),
      firstName,
      lastName,
      ...(overrides.tenantId || this.currentTenantId ? { tenantId: overrides.tenantId || this.currentTenantId } : {}),
      ...overrides
    };

    return userData;
  }

  /**
   * Creates a realistic user data object for API responses
   */
  createUserData(overrides: Partial<UserData> = {}, options: CreateUserOptions = {}): UserData {
    const id = overrides.id || faker.datatype.number({ min: 1, max: 999999 });
    const firstName = overrides.firstName || faker.name.firstName();
    const lastName = overrides.lastName || faker.name.lastName();
    const username = overrides.username || generateTestUsername(firstName.toLowerCase());
    const email = overrides.email || generateTestEmail(username);
    const tenantId = options.tenantId || this.currentTenantId;

    const userData: UserData = {
      id,
      username,
      email,
      roles: options.roles || TEST_DATA_CONFIG.user.defaultRoles,
      permissions: this.generatePermissionsFromRoles(options.roles || TEST_DATA_CONFIG.user.defaultRoles),
      enabled: options.enabled !== undefined ? options.enabled : true,
      emailVerified: options.emailVerified !== undefined ? options.emailVerified : true,
      createdAt: faker.date.recent(30).toISOString(),
      updatedAt: faker.date.recent(7).toISOString(),
      ...(firstName ? { firstName } : {}),
      ...(lastName ? { lastName } : {}),
      ...(options.department ? { department: options.department } : { department: faker.commerce.department() }),
      ...(options.organizationId ? { organizationId: options.organizationId } : { organizationId: faker.datatype.uuid() }),
      ...(tenantId ? { tenantId } : {}),
      ...overrides
    };

    // Track relationships
    this.relationships.users.add(userData.id);
    if (tenantId) {
      this.relationships.userTenantMappings.set(userData.id, tenantId);
    }

    return userData;
  }

  /**
   * Creates a user creation request object
   */
  createUserRequest(overrides: Partial<CreateUserRequest> = {}, options: CreateUserOptions = {}): CreateUserRequest {
    const firstName = faker.name.firstName();
    const lastName = faker.name.lastName();
    const username = overrides.username || generateTestUsername(firstName.toLowerCase());
    const email = overrides.email || generateTestEmail(username);

    return {
      username,
      email,
      password: this.generateSecurePassword(),
      firstName,
      lastName,
      roles: options.roles || TEST_DATA_CONFIG.user.defaultRoles,
      enabled: options.enabled !== undefined ? options.enabled : true,
      ...(options.tenantId || this.currentTenantId ? { tenantId: options.tenantId || this.currentTenantId } : {}),
      ...overrides
    };
  }

  /**
   * Creates a realistic book data object
   */
  createBookData(overrides: Partial<BookData> = {}, options: CreateBookOptions = {}): BookData {
    const id = overrides.id || faker.datatype.number({ min: 1, max: 999999 });
    const title = overrides.title || this.generateBookTitle();
    const author = overrides.author || `${faker.name.firstName()} ${faker.name.lastName()}`;
    const isbn = overrides.isbn || this.generateISBN();
    const category = options.category || faker.helpers.arrayElement(TEST_DATA_CONFIG.book.categories);
    const price = overrides.price || faker.datatype.float({ 
      min: TEST_DATA_CONFIG.book.priceMin, 
      max: TEST_DATA_CONFIG.book.priceMax, 
      precision: 0.01 
    });
    const stock = overrides.stock || faker.datatype.number({ 
      min: TEST_DATA_CONFIG.book.stockMin, 
      max: TEST_DATA_CONFIG.book.stockMax 
    });
    const tenantId = options.tenantId || this.currentTenantId;

    const bookData: BookData = {
      id,
      title,
      author,
      isbn,
      price,
      stock,
      category,
      available: options.available !== undefined ? options.available : stock > 0,
      availableQuantity: options.inStock !== false ? stock : 0,
      createdAt: faker.date.recent(90).toISOString(),
      updatedAt: faker.date.recent(30).toISOString(),
      ...(overrides.description ? { description: overrides.description } : { description: faker.lorem.paragraphs(2) }),
      ...(overrides.currency ? { currency: overrides.currency } : { currency: 'USD' }),
      ...(category ? { categoryName: category } : {}),
      ...(options.tags ? { tags: options.tags } : { tags: this.generateBookTags(category) }),
      ...(tenantId ? { tenantId } : {}),
      ...overrides
    };

    // Track relationships
    this.relationships.books.add(bookData.id);
    if (tenantId) {
      this.relationships.bookTenantMappings.set(bookData.id, tenantId);
    }

    return bookData;
  }

  /**
   * Creates a book creation request object
   */
  createBookRequest(overrides: Partial<CreateBookRequest> = {}, options: CreateBookOptions = {}): CreateBookRequest {
    const title = overrides.title || this.generateBookTitle();
    const author = overrides.author || `${faker.name.firstName()} ${faker.name.lastName()}`;
    const isbn = overrides.isbn || this.generateISBN();
    const category = options.category || faker.helpers.arrayElement(TEST_DATA_CONFIG.book.categories);
    const price = overrides.price || faker.datatype.float({ 
      min: TEST_DATA_CONFIG.book.priceMin, 
      max: TEST_DATA_CONFIG.book.priceMax, 
      precision: 0.01 
    });
    const stock = overrides.stock || faker.datatype.number({ 
      min: TEST_DATA_CONFIG.book.stockMin, 
      max: TEST_DATA_CONFIG.book.stockMax 
    });

    return {
      title,
      author,
      isbn,
      description: overrides.description || faker.lorem.paragraphs(2),
      price,
      currency: overrides.currency || 'USD',
      stock: options.inStock !== false ? stock : 0,
      category,
      tags: options.tags || this.generateBookTags(category),
      ...overrides
    };
  }

  /**
   * Creates a realistic tenant data object
   */
  createTenantData(overrides: Partial<TenantData> = {}, options: CreateTenantOptions = {}): TenantData {
    const id = overrides.id || generateTestId();
    const name = overrides.name || `${faker.company.name()} Test Tenant`;
    const subdomain = overrides.subdomain || generateTestSubdomain(name.toLowerCase().replace(/\s+/g, '-'));

    const tenantData: TenantData = {
      id,
      name,
      domain: options.domain || `${subdomain}.example.com`,
      subdomain,
      enabled: options.enabled !== undefined ? options.enabled : true,
      settings: {
        allowRegistration: true,
        requireEmailVerification: false,
        maxUsers: faker.datatype.number({ min: 10, max: 1000 }),
        features: ['books', 'users', 'analytics'],
        ...options.settings
      },
      createdAt: faker.date.recent(60).toISOString(),
      updatedAt: faker.date.recent(14).toISOString(),
      ...overrides
    };

    // Track relationships
    this.relationships.tenants.add(tenantData.id);

    return tenantData;
  }

  /**
   * Creates a tenant creation request object
   */
  createTenantRequest(overrides: Partial<CreateTenantRequest> = {}, options: CreateTenantOptions = {}): CreateTenantRequest {
    const name = overrides.name || `${faker.company.name()} Test Tenant`;
    const subdomain = overrides.subdomain || generateTestSubdomain(name.toLowerCase().replace(/\s+/g, '-'));

    return {
      name,
      domain: options.domain || `${subdomain}.example.com`,
      subdomain,
      enabled: options.enabled !== undefined ? options.enabled : true,
      settings: {
        allowRegistration: true,
        requireEmailVerification: false,
        maxUsers: faker.datatype.number({ min: 10, max: 1000 }),
        features: ['books', 'users', 'analytics'],
        ...options.settings
      },
      ...overrides
    };
  }

  /**
   * Creates multiple users with relationships to a tenant
   */
  createUsersForTenant(tenantId: string, count: number = 5, options: CreateUserOptions = {}): UserData[] {
    const users: UserData[] = [];
    
    for (let i = 0; i < count; i++) {
      const user = this.createUserData({}, { ...options, tenantId });
      users.push(user);
    }

    return users;
  }

  /**
   * Creates multiple books for a tenant with varied categories
   */
  createBooksForTenant(tenantId: string, count: number = 10, options: CreateBookOptions = {}): BookData[] {
    const books: BookData[] = [];
    const categories = TEST_DATA_CONFIG.book.categories;
    
    for (let i = 0; i < count; i++) {
      const category = categories[i % categories.length];
      const book = this.createBookData({}, { ...options, tenantId, ...(category ? { category } : {}) });
      books.push(book);
    }

    return books;
  }

  /**
   * Creates a complete tenant with associated users and books
   */
  createTenantWithData(
    tenantOverrides: Partial<TenantData> = {},
    userCount: number = 3,
    bookCount: number = 5
  ): { tenant: TenantData; users: UserData[]; books: BookData[] } {
    const tenant = this.createTenantData(tenantOverrides);
    const users = this.createUsersForTenant(tenant.id, userCount);
    const books = this.createBooksForTenant(tenant.id, bookCount);

    return { tenant, users, books };
  }

  /**
   * Gets all tracked relationships for cleanup
   */
  getRelationships(): TestDataRelationships {
    return { ...this.relationships };
  }

  /**
   * Clears all tracked relationships
   */
  clearRelationships(): void {
    this.relationships.users.clear();
    this.relationships.books.clear();
    this.relationships.tenants.clear();
    this.relationships.userTenantMappings.clear();
    this.relationships.bookTenantMappings.clear();
  }

  /**
   * Generates a secure password meeting requirements
   */
  private generateSecurePassword(): string {
    const length = faker.datatype.number({ 
      min: TEST_DATA_CONFIG.user.passwordMinLength, 
      max: TEST_DATA_CONFIG.user.passwordMaxLength 
    });
    
    // Ensure password has required complexity
    const lowercase = faker.random.alpha({ count: 3, casing: 'lower' });
    const uppercase = faker.random.alpha({ count: 2, casing: 'upper' });
    const numbers = faker.datatype.number({ min: 100, max: 999 }).toString();
    const special = faker.helpers.arrayElement(['!', '@', '#', '$', '%', '^', '&', '*']);
    
    const remaining = length - lowercase.length - uppercase.length - numbers.length - special.length;
    const extra = remaining > 0 ? faker.random.alphaNumeric(remaining) : '';
    
    // Shuffle the password components
    const password = faker.helpers.shuffle([...lowercase, ...uppercase, ...numbers, special, ...extra]).join('');
    
    return password;
  }

  /**
   * Generates realistic book title
   */
  private generateBookTitle(): string {
    const templates = [
      () => `The ${faker.random.word()} of ${faker.random.word()}`,
      () => `${faker.random.word()} and ${faker.random.word()}`,
      () => `A Guide to ${faker.random.word()}`,
      () => `${faker.random.word()}: ${faker.random.words(3)}`,
      () => `The Complete ${faker.random.word()}`,
      () => faker.random.words(faker.datatype.number({ min: 2, max: 5 }))
    ];
    
    const template = faker.helpers.arrayElement(templates);
    return faker.helpers.fake(template().replace(/\b\w/g, (l: string) => l.toUpperCase()));
  }

  /**
   * Generates a valid ISBN-13
   */
  private generateISBN(): string {
    const prefix = '978';
    const group = faker.datatype.number({ min: 0, max: 9 });
    const publisher = faker.datatype.number({ min: 100000, max: 999999 });
    const title = faker.datatype.number({ min: 10, max: 99 });
    
    // Calculate check digit (simplified)
    const digits = `${prefix}${group}${publisher}${title}`;
    let sum = 0;
    for (let i = 0; i < digits.length; i++) {
      sum += parseInt(digits[i]!) * (i % 2 === 0 ? 1 : 3);
    }
    const checkDigit = (10 - (sum % 10)) % 10;
    
    return `${prefix}-${group}-${publisher}-${title}-${checkDigit}`;
  }

  /**
   * Generates relevant tags for a book category
   */
  private generateBookTags(category: string): string[] {
    const categoryTags: Record<string, string[]> = {
      'Fiction': ['novel', 'story', 'drama', 'adventure', 'mystery'],
      'Non-Fiction': ['facts', 'real', 'educational', 'informative', 'reference'],
      'Technology': ['programming', 'software', 'computer', 'digital', 'innovation'],
      'Science': ['research', 'discovery', 'experiment', 'theory', 'analysis'],
      'History': ['historical', 'past', 'events', 'timeline', 'culture'],
      'Biography': ['life', 'person', 'memoir', 'autobiography', 'story'],
      'Self-Help': ['improvement', 'motivation', 'success', 'personal', 'growth'],
      'Business': ['management', 'strategy', 'leadership', 'finance', 'entrepreneurship'],
      'Health': ['wellness', 'fitness', 'medical', 'nutrition', 'lifestyle'],
      'Travel': ['journey', 'destination', 'culture', 'adventure', 'guide']
    };

    const baseTags = categoryTags[category] || ['general', 'book', 'reading'];
    const selectedTags = faker.helpers.arrayElements(baseTags, faker.datatype.number({ min: 2, max: 4 }));
    
    // Add some random generic tags
    const genericTags = ['popular', 'bestseller', 'recommended', 'new', 'classic'];
    const additionalTags = faker.helpers.arrayElements(genericTags, faker.datatype.number({ min: 0, max: 2 }));
    
    return [...selectedTags, ...additionalTags];
  }

  /**
   * Generates permissions based on roles
   */
  private generatePermissionsFromRoles(roles: string[]): string[] {
    const rolePermissions: Record<string, string[]> = {
      'ADMIN': ['user:read', 'user:write', 'user:delete', 'book:read', 'book:write', 'book:delete', 'tenant:read', 'tenant:write'],
      'MANAGER': ['user:read', 'user:write', 'book:read', 'book:write', 'tenant:read'],
      'USER': ['user:read', 'book:read'],
      'GUEST': ['book:read']
    };

    const permissions = new Set<string>();
    roles.forEach(role => {
      const rolePerms = rolePermissions[role] || [];
      rolePerms.forEach(perm => permissions.add(perm));
    });

    return Array.from(permissions);
  }
}

/**
 * Default test data factory instance
 */
export const testDataFactory = new TestDataFactory();

/**
 * Creates a tenant-aware test data factory
 */
export function createTenantAwareFactory(tenantId: string, config: TestDataFactoryConfig = {}): TestDataFactory {
  return new TestDataFactory({ ...config, tenantId });
}

/**
 * Creates a seeded test data factory for reproducible tests
 */
export function createSeededFactory(seed: number, config: TestDataFactoryConfig = {}): TestDataFactory {
  return new TestDataFactory({ ...config, seed });
}