/**
 * Role-Based Access Control (RBAC) Test Helpers
 * Provides utilities for testing role and permission-based access control
 */

import { ApiClient } from './api-client.js';
import { AuthTestUser } from './auth-test-framework.js';
import { JwtTestUtils } from './jwt-test-utils.js';
import { ApiResponse } from '../types/api-responses.js';

export interface RoleDefinition {
  name: string;
  permissions: string[];
  description?: string;
}

export interface EndpointAccessRule {
  endpoint: string;
  method: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  requiredRoles?: string[];
  requiredPermissions?: string[];
  description?: string;
}

export interface AccessTestResult {
  endpoint: string;
  method: string;
  user: string;
  roles: string[];
  expectedAccess: boolean;
  actualAccess: boolean;
  statusCode: number;
  passed: boolean;
  error?: string;
  responseTime: number;
}

export interface RoleTestSuite {
  name: string;
  description: string;
  users: AuthTestUser[];
  endpoints: EndpointAccessRule[];
  results: AccessTestResult[];
}

/**
 * RBAC Test Helpers for access control testing
 */
export class RbacTestHelpers {
  private readonly apiClient: ApiClient;

  // Standard role definitions for iqscaffold platform
  static readonly STANDARD_ROLES: Record<string, RoleDefinition> = {
    USER: {
      name: 'USER',
      permissions: ['READ_OWN', 'WRITE_OWN'],
      description: 'Standard user with basic access'
    },
    MODERATOR: {
      name: 'MODERATOR',
      permissions: ['READ', 'WRITE', 'MODERATE'],
      description: 'Moderator with content management access'
    },
    ADMIN: {
      name: 'ADMIN',
      permissions: ['READ', 'WRITE', 'DELETE', 'ADMIN', 'USER_MANAGEMENT'],
      description: 'Administrator with full system access'
    },
    SUPER_ADMIN: {
      name: 'SUPER_ADMIN',
      permissions: ['READ', 'WRITE', 'DELETE', 'ADMIN', 'USER_MANAGEMENT', 'TENANT_MANAGEMENT'],
      description: 'Super administrator with tenant management access'
    }
  };

  // Standard endpoint access rules
  static readonly STANDARD_ENDPOINTS: EndpointAccessRule[] = [
    // Public endpoints (no authentication required)
    {
      endpoint: '/actuator/health',
      method: 'GET',
      description: 'Health check endpoint'
    },
    {
      endpoint: '/api/v1/auth/register',
      method: 'POST',
      description: 'User registration'
    },
    {
      endpoint: '/api/v1/auth/login',
      method: 'POST',
      description: 'User login'
    },

    // User endpoints (authenticated users)
    {
      endpoint: '/api/v1/users/me',
      method: 'GET',
      requiredRoles: ['USER'],
      description: 'Get current user profile'
    },
    {
      endpoint: '/api/v1/users/me',
      method: 'PUT',
      requiredRoles: ['USER'],
      description: 'Update current user profile'
    },

    // Admin endpoints
    {
      endpoint: '/api/v1/admin/users',
      method: 'GET',
      requiredRoles: ['ADMIN'],
      description: 'List all users'
    },
    {
      endpoint: '/api/v1/admin/users',
      method: 'POST',
      requiredRoles: ['ADMIN'],
      description: 'Create new user'
    },
    {
      endpoint: '/api/v1/admin/users/{id}',
      method: 'GET',
      requiredRoles: ['ADMIN'],
      description: 'Get user by ID'
    },
    {
      endpoint: '/api/v1/admin/users/{id}',
      method: 'PUT',
      requiredRoles: ['ADMIN'],
      description: 'Update user'
    },
    {
      endpoint: '/api/v1/admin/users/{id}',
      method: 'DELETE',
      requiredRoles: ['ADMIN'],
      description: 'Delete user'
    },
    // Super Admin endpoints
    {
      endpoint: '/api/v1/tenants',
      method: 'GET',
      requiredRoles: ['SUPER_ADMIN'],
      description: 'List all tenants'
    },
    {
      endpoint: '/api/v1/tenants',
      method: 'POST',
      requiredRoles: ['SUPER_ADMIN'],
      description: 'Create new tenant'
    },
    {
      endpoint: '/api/v1/tenants/{id}',
      method: 'PUT',
      requiredRoles: ['SUPER_ADMIN'],
      description: 'Update tenant'
    }
  ];

  constructor(apiClient: ApiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Test access to a single endpoint with specific user
   */
  async testEndpointAccess(
    user: AuthTestUser,
    endpoint: EndpointAccessRule,
    actualEndpoint?: string
  ): Promise<AccessTestResult> {
    const startTime = Date.now();
    const testEndpoint = actualEndpoint || endpoint.endpoint.replace('{id}', '1');
    
    try {
      const response = await this.apiClient.request({
        method: endpoint.method,
        url: testEndpoint
      });

      const responseTime = Date.now() - startTime;
      const actualAccess = response.status >= 200 && response.status < 300;
      const expectedAccess = this.shouldHaveAccess(user, endpoint);

      return {
        endpoint: testEndpoint,
        method: endpoint.method,
        user: user.username,
        roles: user.roles || [],
        expectedAccess,
        actualAccess,
        statusCode: response.status,
        passed: expectedAccess === actualAccess,
        responseTime
      };
    } catch (error: any) {
      const responseTime = Date.now() - startTime;
      const statusCode = error.status || 0;
      const actualAccess = false;
      const expectedAccess = this.shouldHaveAccess(user, endpoint);

      return {
        endpoint: testEndpoint,
        method: endpoint.method,
        user: user.username,
        roles: user.roles || [],
        expectedAccess,
        actualAccess,
        statusCode,
        passed: expectedAccess === actualAccess,
        error: error.message || 'Unknown error',
        responseTime
      };
    }
  }

  /**
   * Test access to multiple endpoints with a single user
   */
  async testUserAccess(
    user: AuthTestUser,
    endpoints: EndpointAccessRule[]
  ): Promise<AccessTestResult[]> {
    const results: AccessTestResult[] = [];

    // Login as the user first
    await this.loginUser(user);

    for (const endpoint of endpoints) {
      const result = await this.testEndpointAccess(user, endpoint);
      results.push(result);
    }

    return results;
  }

  /**
   * Test access matrix: multiple users against multiple endpoints
   */
  async testAccessMatrix(
    users: AuthTestUser[],
    endpoints: EndpointAccessRule[]
  ): Promise<RoleTestSuite> {
    const results: AccessTestResult[] = [];

    for (const user of users) {
      const userResults = await this.testUserAccess(user, endpoints);
      results.push(...userResults);
    }

    return {
      name: 'Access Matrix Test',
      description: 'Comprehensive RBAC testing across users and endpoints',
      users,
      endpoints,
      results
    };
  }

  /**
   * Test role escalation scenarios
   */
  async testRoleEscalation(
    lowPrivilegeUser: AuthTestUser,
    highPrivilegeEndpoints: EndpointAccessRule[]
  ): Promise<{
    user: AuthTestUser;
    escalationAttempts: AccessTestResult[];
    successful: boolean;
  }> {
    const escalationAttempts = await this.testUserAccess(lowPrivilegeUser, highPrivilegeEndpoints);
    const successful = escalationAttempts.some(attempt => attempt.actualAccess && !attempt.expectedAccess);

    return {
      user: lowPrivilegeUser,
      escalationAttempts,
      successful
    };
  }

  /**
   * Test permission inheritance
   */
  async testPermissionInheritance(
    users: AuthTestUser[]
  ): Promise<{
    inheritanceTests: Array<{
      user: AuthTestUser;
      expectedPermissions: string[];
      actualPermissions: string[];
      inheritanceCorrect: boolean;
    }>;
  }> {
    const inheritanceTests = [];

    for (const user of users) {
      await this.loginUser(user);
      
      // Get current user to check actual permissions
      try {
        const response = await this.apiClient.request({
          method: 'GET',
          url: '/api/v1/users/me'
        });

        const actualPermissions = response.data.permissions || [];
        const expectedPermissions = this.getExpectedPermissions(user.roles || []);
        const inheritanceCorrect = this.comparePermissions(expectedPermissions, actualPermissions);

        inheritanceTests.push({
          user,
          expectedPermissions,
          actualPermissions,
          inheritanceCorrect
        });
      } catch (error) {
        inheritanceTests.push({
          user,
          expectedPermissions: [],
          actualPermissions: [],
          inheritanceCorrect: false
        });
      }
    }

    return { inheritanceTests };
  }

  /**
   * Test tenant isolation
   */
  async testTenantIsolation(
    tenant1User: AuthTestUser,
    tenant2User: AuthTestUser,
    crossTenantEndpoints: string[]
  ): Promise<{
    tenant1Results: AccessTestResult[];
    tenant2Results: AccessTestResult[];
    isolationMaintained: boolean;
  }> {
    // Test tenant 1 user accessing tenant 2 resources
    await this.loginUser(tenant1User);
    this.apiClient.setTenant(tenant2User.tenantId || 'tenant2');
    
    const tenant1Results: AccessTestResult[] = [];
    for (const endpoint of crossTenantEndpoints) {
      const result = await this.testEndpointAccess(tenant1User, {
        endpoint,
        method: 'GET'
      });
      tenant1Results.push(result);
    }

    // Test tenant 2 user accessing tenant 1 resources
    await this.loginUser(tenant2User);
    this.apiClient.setTenant(tenant1User.tenantId || 'tenant1');
    
    const tenant2Results: AccessTestResult[] = [];
    for (const endpoint of crossTenantEndpoints) {
      const result = await this.testEndpointAccess(tenant2User, {
        endpoint,
        method: 'GET'
      });
      tenant2Results.push(result);
    }

    // Check if isolation is maintained (all cross-tenant access should fail)
    const isolationMaintained = [...tenant1Results, ...tenant2Results]
      .every(result => !result.actualAccess || result.statusCode === 403 || result.statusCode === 404);

    return {
      tenant1Results,
      tenant2Results,
      isolationMaintained
    };
  }

  /**
   * Generate RBAC test report
   */
  generateTestReport(testSuite: RoleTestSuite): {
    summary: {
      totalTests: number;
      passed: number;
      failed: number;
      successRate: number;
    };
    roleBreakdown: Record<string, {
      tests: number;
      passed: number;
      failed: number;
    }>;
    endpointBreakdown: Record<string, {
      tests: number;
      passed: number;
      failed: number;
    }>;
    failures: AccessTestResult[];
  } {
    const totalTests = testSuite.results.length;
    const passed = testSuite.results.filter(r => r.passed).length;
    const failed = totalTests - passed;
    const successRate = totalTests > 0 ? (passed / totalTests) * 100 : 0;

    // Role breakdown
    const roleBreakdown: Record<string, { tests: number; passed: number; failed: number }> = {};
    testSuite.results.forEach(result => {
      result.roles.forEach(role => {
        if (!roleBreakdown[role]) {
          roleBreakdown[role] = { tests: 0, passed: 0, failed: 0 };
        }
        roleBreakdown[role].tests++;
        if (result.passed) {
          roleBreakdown[role].passed++;
        } else {
          roleBreakdown[role].failed++;
        }
      });
    });

    // Endpoint breakdown
    const endpointBreakdown: Record<string, { tests: number; passed: number; failed: number }> = {};
    testSuite.results.forEach(result => {
      const key = `${result.method} ${result.endpoint}`;
      if (!endpointBreakdown[key]) {
        endpointBreakdown[key] = { tests: 0, passed: 0, failed: 0 };
      }
      endpointBreakdown[key].tests++;
      if (result.passed) {
        endpointBreakdown[key].passed++;
      } else {
        endpointBreakdown[key].failed++;
      }
    });

    const failures = testSuite.results.filter(r => !r.passed);

    return {
      summary: {
        totalTests,
        passed,
        failed,
        successRate
      },
      roleBreakdown,
      endpointBreakdown,
      failures
    };
  }

  // ============================================================================
  // Private Helper Methods
  // ============================================================================

  /**
   * Determine if user should have access to endpoint
   */
  private shouldHaveAccess(user: AuthTestUser, endpoint: EndpointAccessRule): boolean {
    // If no roles required, endpoint is public
    if (!endpoint.requiredRoles || endpoint.requiredRoles.length === 0) {
      return true;
    }

    // Check if user has any of the required roles
    const userRoles = user.roles || [];
    return endpoint.requiredRoles.some(requiredRole => userRoles.includes(requiredRole));
  }

  /**
   * Login user and set authentication
   */
  private async loginUser(user: AuthTestUser): Promise<void> {
    try {
      const response = await this.apiClient.request({
        method: 'POST',
        url: '/api/v1/auth/login',
        data: {
          username: user.username,
          password: user.password,
          tenantId: user.tenantId
        }
      });

      if (response.data.tokens) {
        this.apiClient.setAuthTokens(response.data.tokens);
      }

      if (user.tenantId) {
        this.apiClient.setTenant(user.tenantId);
      }
    } catch (error) {
      throw new Error(`Failed to login user ${user.username}: ${error instanceof Error ? error.message : 'Unknown error'}`);
    }
  }

  /**
   * Get expected permissions for roles
   */
  private getExpectedPermissions(roles: string[]): string[] {
    const permissions = new Set<string>();
    
    roles.forEach(role => {
      const roleDefinition = RbacTestHelpers.STANDARD_ROLES[role];
      if (roleDefinition) {
        roleDefinition.permissions.forEach(permission => permissions.add(permission));
      }
    });

    return Array.from(permissions).sort();
  }

  /**
   * Compare expected vs actual permissions
   */
  private comparePermissions(expected: string[], actual: string[]): boolean {
    if (expected.length !== actual.length) {
      return false;
    }

    const sortedExpected = [...expected].sort();
    const sortedActual = [...actual].sort();

    return sortedExpected.every((permission, index) => permission === sortedActual[index]);
  }

  /**
   * Create test users with specific roles
   */
  static createTestUsersWithRoles(): AuthTestUser[] {
    return [
      {
        username: 'testuser',
        email: 'testuser@example.com',
        password: 'TestPass123!',
        roles: ['USER'],
        tenantId: 'default'
      },
      {
        username: 'testmoderator',
        email: 'testmoderator@example.com',
        password: 'TestPass123!',
        roles: ['USER', 'MODERATOR'],
        tenantId: 'default'
      },
      {
        username: 'testadmin',
        email: 'testadmin@example.com',
        password: 'TestPass123!',
        roles: ['USER', 'ADMIN'],
        tenantId: 'default'
      },
      {
        username: 'testsuperadmin',
        email: 'testsuperadmin@example.com',
        password: 'TestPass123!',
        roles: ['USER', 'ADMIN', 'SUPER_ADMIN'],
        tenantId: 'default'
      }
    ];
  }

  /**
   * Get standard test endpoints
   */
  static getStandardTestEndpoints(): EndpointAccessRule[] {
    return [...RbacTestHelpers.STANDARD_ENDPOINTS];
  }
}
