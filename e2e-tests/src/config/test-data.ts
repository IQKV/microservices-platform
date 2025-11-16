/**
 * Test data configuration and constants
 */

export interface TestUserData {
  username: string;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  roles?: string[];
}

export interface TestTenantData {
  name: string;
  subdomain: string;
  description: string;
  settings?: Record<string, any>;
}

export interface TestBookData {
  title: string;
  author: string;
  isbn: string;
  price: number;
  category: string;
  description: string;
  stock: number;
}

/**
 * Default test data templates
 */
export const DEFAULT_TEST_DATA = {
  users: {
    admin: {
      username: 'test-admin',
      email: 'test-admin@example.com',
      password: 'TestPassword123!',
      firstName: 'Test',
      lastName: 'Admin',
      roles: ['ADMIN', 'USER']
    } as TestUserData,
    
    user: {
      username: 'test-user',
      email: 'test-user@example.com',
      password: 'TestPassword123!',
      firstName: 'Test',
      lastName: 'User',
      roles: ['USER']
    } as TestUserData,
    
    manager: {
      username: 'test-manager',
      email: 'test-manager@example.com',
      password: 'TestPassword123!',
      firstName: 'Test',
      lastName: 'Manager',
      roles: ['MANAGER', 'USER']
    } as TestUserData
  },

  tenants: {
    default: {
      name: 'Test Tenant',
      subdomain: 'test-tenant',
      description: 'Default test tenant for API testing',
      settings: {
        allowRegistration: true,
        requireEmailVerification: false
      }
    } as TestTenantData,
    
    enterprise: {
      name: 'Enterprise Test Tenant',
      subdomain: 'enterprise-test',
      description: 'Enterprise test tenant with advanced features',
      settings: {
        allowRegistration: false,
        requireEmailVerification: true,
        enableSso: true
      }
    } as TestTenantData
  },

  books: {
    fiction: {
      title: 'Test Fiction Book',
      author: 'Test Author',
      isbn: '978-0-123456-78-9',
      price: 19.99,
      category: 'Fiction',
      description: 'A test fiction book for API testing',
      stock: 100
    } as TestBookData,
    
    nonFiction: {
      title: 'Test Non-Fiction Book',
      author: 'Test Expert',
      isbn: '978-0-987654-32-1',
      price: 29.99,
      category: 'Non-Fiction',
      description: 'A test non-fiction book for API testing',
      stock: 50
    } as TestBookData,
    
    technical: {
      title: 'Test Technical Book',
      author: 'Tech Author',
      isbn: '978-0-555666-77-8',
      price: 49.99,
      category: 'Technology',
      description: 'A test technical book for API testing',
      stock: 25
    } as TestBookData
  }
};

/**
 * Test data generation settings
 */
export const TEST_DATA_CONFIG = {
  // User generation settings
  user: {
    passwordMinLength: 8,
    passwordMaxLength: 20,
    usernameMinLength: 3,
    usernameMaxLength: 30,
    emailDomains: ['example.com', 'test.com', 'demo.org'],
    defaultRoles: ['USER']
  },

  // Tenant generation settings
  tenant: {
    subdomainMinLength: 3,
    subdomainMaxLength: 20,
    nameMinLength: 5,
    nameMaxLength: 50,
    descriptionMaxLength: 200
  },

  // Book generation settings
  book: {
    titleMinLength: 5,
    titleMaxLength: 100,
    authorMinLength: 3,
    authorMaxLength: 50,
    descriptionMaxLength: 500,
    priceMin: 5.99,
    priceMax: 99.99,
    stockMin: 0,
    stockMax: 1000,
    categories: [
      'Fiction',
      'Non-Fiction',
      'Technology',
      'Science',
      'History',
      'Biography',
      'Self-Help',
      'Business',
      'Health',
      'Travel'
    ]
  },

  // Test execution settings
  execution: {
    maxRetries: 3,
    retryDelay: 1000,
    cleanupTimeout: 30000,
    dataIsolationEnabled: true,
    parallelTestDataCreation: true
  }
};

/**
 * API endpoint paths
 */
export const API_ENDPOINTS = {
  user: {
    register: '/api/v1/auth/register',
    login: '/api/v1/auth/login',
    refresh: '/api/v1/auth/refresh',
    logout: '/api/v1/auth/logout',
    verify: '/api/v1/auth/verify',
    profile: '/api/v1/auth/profile',
    changePassword: '/api/v1/users/me/password'
  },
  
  users: {
    base: '/api/v1/admin/users',
    byId: (id: string) => `/api/v1/admin/users/${id}`,
    search: '/api/v1/admin/users/search',
    roles: (id: string) => `/api/v1/admin/users/${id}/roles`
  },
  
  tenants: {
    base: '/api/v1/tenants',
    byId: (id: string) => `/api/v1/tenants/${id}`,
    users: (id: string) => `/api/v1/tenants/${id}/users`,
    settings: (id: string) => `/api/v1/tenants/${id}/settings`
  },
  
  books: {
    base: '/api/v1/books',
    byId: (id: string) => `/api/v1/books/${id}`,
    search: '/api/v1/books/search',
    categories: '/api/v1/books/categories',
    inventory: (id: string) => `/api/v1/books/${id}/inventory`
  },
  
  health: {
    gateway: '/actuator/health',
    user: '/actuator/health',
    bookstore: '/actuator/health'
  }
};

/**
 * HTTP status codes for testing
 */
export const HTTP_STATUS = {
  OK: 200,
  CREATED: 201,
  NO_CONTENT: 204,
  BAD_REQUEST: 400,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  CONFLICT: 409,
  UNPROCESSABLE_ENTITY: 422,
  TOO_MANY_REQUESTS: 429,
  INTERNAL_SERVER_ERROR: 500,
  SERVICE_UNAVAILABLE: 503
} as const;

/**
 * Common test headers
 */
export const TEST_HEADERS = {
  CONTENT_TYPE_JSON: 'application/json',
  ACCEPT_JSON: 'application/json',
  TENANT_ID_HEADER: 'X-Tenant-ID',
  CORRELATION_ID_HEADER: 'X-Correlation-ID',
  USER_AGENT: 'Gripday-E2E-Tests/1.0.0'
} as const;
