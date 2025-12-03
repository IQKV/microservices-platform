/**
 * Test data factory utilities for generating realistic test data using Faker.js
 * Provides factory methods for users, tenants, and other entities with relationship management
 */

import { faker } from '@faker-js/faker';
import { 
  UserRegistrationData, 
  UserData, 
  TenantData, 
  CreateTenantRequest,
  CreateUserRequest 
} from '../types/api-responses.js';
import { 
  TestUserData, 
  TestTenantData, 
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
  tenants: Set<string>;
  userTenantMappings: Map<number, string>;
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
      tenants: new Set(),
      userTenantMappings: new Map()
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
        features: ['users', 'analytics'],
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
        features: ['users', 'analytics'],
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
   * Creates a complete tenant with associated users
   */
  createTenantWithData(
    tenantOverrides: Partial<TenantData> = {},
    userCount: number = 3
  ): { tenant: TenantData; users: UserData[] } {
    const tenant = this.createTenantData(tenantOverrides);
    const users = this.createUsersForTenant(tenant.id, userCount);

    return { tenant, users };
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
    this.relationships.tenants.clear();
    this.relationships.userTenantMappings.clear();
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
   * Generates permissions based on roles
   */
  private generatePermissionsFromRoles(roles: string[]): string[] {
    const rolePermissions: Record<string, string[]> = {
      'ADMIN': ['user:read', 'user:write', 'user:delete', 'tenant:read', 'tenant:write'],
      'MANAGER': ['user:read', 'user:write', 'tenant:read'],
      'USER': ['user:read'],
      'GUEST': []
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