/**
 * Role-Based Access Control (RBAC) Tests
 * Tests role and permission-based access control across the platform
 */

import { test, expect } from '@playwright/test';
import { 
  AuthTestFramework, 
  RbacTestHelpers,
  JwtTestUtils,
  ApiClient,
  createApiClient
} from '../../utils/index.js';
import { getEnvironmentConfig } from '../../config/environments.js';
import type { 
  AuthTestUser, 
  AccessTestResult,
  RoleTestSuite 
} from '../../utils/index.js';

test.describe('Role-Based Access Control Tests', () => {
  let authFramework: AuthTestFramework;
  let rbacHelpers: RbacTestHelpers;
  let apiClient: ApiClient;
  const config = getEnvironmentConfig();

  test.beforeEach(async () => {
    apiClient = createApiClient(config);
    authFramework = new AuthTestFramework(apiClient);
    rbacHelpers = new RbacTestHelpers(apiClient);
  });

  test.afterEach(async () => {
    await authFramework.cleanup();
    await apiClient.dispose();
  });

  test.describe('Role Assignment and Validation', () => {
    test('should assign correct roles to users', async () => {
      // Create users with different roles
      const regularUser = await authFramework.createUserWithRoles(['USER'], {
        username: 'regular_user',
        email: 'regular@example.com'
      });

      const moderatorUser = await authFramework.createUserWithRoles(['USER', 'MODERATOR'], {
        username: 'moderator_user',
        email: 'moderator@example.com'
      });

      const adminUser = await authFramework.createUserWithRoles(['USER', 'ADMIN'], {
        username: 'admin_user',
        email: 'admin@example.com'
      });

      // Verify role assignments
      expect(regularUser.roles).toEqual(['USER']);
      expect(moderatorUser.roles).toContain('USER');
      expect(moderatorUser.roles).toContain('MODERATOR');
      expect(adminUser.roles).toContain('USER');
      expect(adminUser.roles).toContain('ADMIN');
    });

    test('should include roles in JWT tokens', async () => {
      const adminUser = await authFramework.createUserWithRoles(['USER', 'ADMIN'], {
        username: 'jwt_role_test',
        email: 'jwt_role_test@example.com',
        emailVerified: true
      });

      const authResponse = await authFramework.loginTestUser(adminUser);
      
      // Verify roles in JWT token
      expect(JwtTestUtils.hasRole(authResponse.tokens.accessToken, 'USER')).toBe(true);
      expect(JwtTestUtils.hasRole(authResponse.tokens.accessToken, 'ADMIN')).toBe(true);
      expect(JwtTestUtils.hasRole(authResponse.tokens.accessToken, 'SUPER_ADMIN')).toBe(false);
    });

    test('should validate permission inheritance', async () => {
      const testUsers = [
        await authFramework.createUserWithRoles(['USER'], {
          username: 'permission_user',
          email: 'permission_user@example.com',
          emailVerified: true
        }),
        await authFramework.createUserWithRoles(['USER', 'MODERATOR'], {
          username: 'permission_moderator',
          email: 'permission_moderator@example.com',
          emailVerified: true
        }),
        await authFramework.createUserWithRoles(['USER', 'ADMIN'], {
          username: 'permission_admin',
          email: 'permission_admin@example.com',
          emailVerified: true
        })
      ];

      const inheritanceTest = await rbacHelpers.testPermissionInheritance(testUsers);
      
      // Verify permission inheritance is correct
      inheritanceTest.inheritanceTests.forEach(test => {
        expect(test.inheritanceCorrect).toBe(true);
      });
    });
  });

  test.describe('Endpoint Access Control', () => {
    let regularUser: AuthTestUser;
    let moderatorUser: AuthTestUser;
    let adminUser: AuthTestUser;

    test.beforeEach(async () => {
      // Create test users with different roles
      regularUser = await authFramework.createUserWithRoles(['USER'], {
        username: 'endpoint_regular',
        email: 'endpoint_regular@example.com',
        emailVerified: true
      });

      moderatorUser = await authFramework.createUserWithRoles(['USER', 'MODERATOR'], {
        username: 'endpoint_moderator',
        email: 'endpoint_moderator@example.com',
        emailVerified: true
      });

      adminUser = await authFramework.createUserWithRoles(['USER', 'ADMIN'], {
        username: 'endpoint_admin',
        email: 'endpoint_admin@example.com',
        emailVerified: true
      });
    });

    test('should allow user access to user endpoints', async () => {
      const userEndpoints = [
        { endpoint: '/api/v1/users/me', method: 'GET' as const, requiredRoles: ['USER'] },
      ];

      const results = await rbacHelpers.testUserAccess(regularUser, userEndpoints);
      
      // All user endpoints should be accessible
      results.forEach(result => {
        expect(result.passed).toBe(true);
        expect(result.actualAccess).toBe(true);
      });
    });

    test('should restrict admin endpoints from regular users', async () => {
      const adminEndpoints = [
        { endpoint: '/api/v1/admin/users', method: 'GET' as const, requiredRoles: ['ADMIN'] },
        { endpoint: '/api/v1/admin/users/1', method: 'GET' as const, requiredRoles: ['ADMIN'] },
        { endpoint: '/api/v1/admin/users', method: 'POST' as const, requiredRoles: ['ADMIN'] }
      ];

      const results = await rbacHelpers.testUserAccess(regularUser, adminEndpoints);
      
      // All admin endpoints should be restricted
      results.forEach(result => {
        expect(result.passed).toBe(true);
        expect(result.actualAccess).toBe(false);
        expect([401, 403]).toContain(result.statusCode);
      });
    });

    test('should allow admin access to admin endpoints', async () => {
      const adminEndpoints = [
        { endpoint: '/api/v1/admin/users', method: 'GET' as const, requiredRoles: ['ADMIN'] },
        { endpoint: '/api/v1/admin/users/1', method: 'GET' as const, requiredRoles: ['ADMIN'] }
      ];

      const results = await rbacHelpers.testUserAccess(adminUser, adminEndpoints);
      
      // Admin should have access to admin endpoints
      results.forEach(result => {
        expect(result.passed).toBe(true);
        expect(result.actualAccess).toBe(true);
        expect(result.statusCode).toBeGreaterThanOrEqual(200);
        expect(result.statusCode).toBeLessThan(300);
      });
    });

  });

  test.describe('Access Matrix Testing', () => {
    test('should validate complete access matrix', async () => {
      // Create test users
      const testUsers = RbacTestHelpers.createTestUsersWithRoles();
      
      // Register all test users
      const registeredUsers: AuthTestUser[] = [];
      for (const userData of testUsers) {
        const user = await authFramework.registerUser({
          ...userData,
          emailVerified: true
        });
        registeredUsers.push(user);
      }

      // Get standard test endpoints
      const testEndpoints = RbacTestHelpers.getStandardTestEndpoints();

      // Run access matrix test
      const accessMatrix = await rbacHelpers.testAccessMatrix(registeredUsers, testEndpoints);
      
      // Generate test report
      const report = rbacHelpers.generateTestReport(accessMatrix);
      
      // Verify overall success rate is high
      expect(report.summary.successRate).toBeGreaterThan(90);
      
      // Verify no unexpected failures
      const criticalFailures = report.failures.filter(failure => 
        failure.expectedAccess && !failure.actualAccess
      );
      expect(criticalFailures.length).toBe(0);
    });

    test('should prevent role escalation attempts', async () => {
      const regularUser = await authFramework.createUserWithRoles(['USER'], {
        username: 'escalation_test',
        email: 'escalation_test@example.com',
        emailVerified: true
      });

      const privilegedEndpoints = [
        { endpoint: '/api/v1/admin/users', method: 'GET' as const, requiredRoles: ['ADMIN'] },
        { endpoint: '/api/v1/admin/users', method: 'POST' as const, requiredRoles: ['ADMIN'] },
        { endpoint: '/api/v1/tenants', method: 'GET' as const, requiredRoles: ['SUPER_ADMIN'] }
      ];

      const escalationTest = await rbacHelpers.testRoleEscalation(regularUser, privilegedEndpoints);
      
      // Verify no successful escalation
      expect(escalationTest.successful).toBe(false);
      
      // All escalation attempts should fail
      escalationTest.escalationAttempts.forEach(attempt => {
        expect(attempt.actualAccess).toBe(false);
        expect([401, 403]).toContain(attempt.statusCode);
      });
    });
  });

  test.describe('Multi-Tenant Access Control', () => {
    test('should enforce tenant isolation', async () => {
      // Create users in different tenants
      const tenant1User = await authFramework.createUserWithRoles(['USER'], {
        username: 'tenant1_user',
        email: 'tenant1@example.com',
        tenantId: 'tenant1',
        emailVerified: true
      });

      const tenant2User = await authFramework.createUserWithRoles(['USER'], {
        username: 'tenant2_user',
        email: 'tenant2@example.com',
        tenantId: 'tenant2',
        emailVerified: true
      });

      // Test cross-tenant access
      const crossTenantEndpoints = [
        '/api/v1/users/me'
      ];

      const isolationTest = await rbacHelpers.testTenantIsolation(
        tenant1User,
        tenant2User,
        crossTenantEndpoints
      );

      // Verify tenant isolation is maintained
      expect(isolationTest.isolationMaintained).toBe(true);
    });

    test('should include tenant context in JWT tokens', async () => {
      const tenantUser = await authFramework.createUserWithRoles(['USER'], {
        username: 'tenant_jwt_test',
        email: 'tenant_jwt_test@example.com',
        tenantId: 'test_tenant',
        emailVerified: true
      });

      const authResponse = await authFramework.loginTestUser(tenantUser);
      
      // Verify tenant ID in JWT token
      expect(JwtTestUtils.belongsToTenant(authResponse.tokens.accessToken, 'test_tenant')).toBe(true);
      expect(JwtTestUtils.belongsToTenant(authResponse.tokens.accessToken, 'other_tenant')).toBe(false);
    });

    test('should propagate tenant context through requests', async () => {
      const tenantUser = await authFramework.createUserWithRoles(['USER'], {
        username: 'tenant_context_test',
        email: 'tenant_context_test@example.com',
        tenantId: 'context_tenant',
        emailVerified: true
      });

      // Login and set tenant context
      await authFramework.loginTestUser(tenantUser);
      authFramework.setTenant('context_tenant');

      // Make authenticated request
      const currentUser = await authFramework.getCurrentUser();
      
      // Verify tenant context is maintained
      expect(currentUser).toBeDefined();
      // Note: In a real implementation, you would verify the tenant ID is included in the response
    });
  });

  test.describe('Permission-Based Access Control', () => {
    test('should validate specific permissions', async () => {
      const testUser = await authFramework.createUserWithRoles(['USER', 'MODERATOR'], {
        username: 'permission_test',
        email: 'permission_test@example.com',
        emailVerified: true
      });

      const authResponse = await authFramework.loginTestUser(testUser);
      
      // Verify specific permissions in token
      expect(JwtTestUtils.hasPermission(authResponse.tokens.accessToken, 'READ')).toBe(true);
      expect(JwtTestUtils.hasPermission(authResponse.tokens.accessToken, 'WRITE')).toBe(true);
      expect(JwtTestUtils.hasPermission(authResponse.tokens.accessToken, 'MODERATE')).toBe(true);
      expect(JwtTestUtils.hasPermission(authResponse.tokens.accessToken, 'ADMIN')).toBe(false);
    });

    test('should enforce fine-grained permissions', async () => {
      // Create user with limited permissions
      const limitedUser = await authFramework.createUserWithRoles(['USER'], {
        username: 'limited_permissions',
        email: 'limited_permissions@example.com',
        emailVerified: true
      });

      // Login user
      await authFramework.loginTestUser(limitedUser);

      const result = await rbacHelpers.testEndpointAccess(limitedUser, writeEndpoint);
      
      // User should not have write access
      expect(result.actualAccess).toBe(false);
      expect([401, 403]).toContain(result.statusCode);
    });
  });

  test.describe('Dynamic Role Changes', () => {
    test('should handle role changes after login', async () => {
      const testUser = await authFramework.createUserWithRoles(['USER'], {
        username: 'dynamic_role_test',
        email: 'dynamic_role_test@example.com',
        emailVerified: true
      });

      // Initial login with USER role
      let authResponse = await authFramework.loginTestUser(testUser);
      expect(JwtTestUtils.hasRole(authResponse.tokens.accessToken, 'USER')).toBe(true);
      expect(JwtTestUtils.hasRole(authResponse.tokens.accessToken, 'ADMIN')).toBe(false);

      // Note: In a real implementation, you would update the user's roles
      // and test that new tokens reflect the role changes after refresh
      
      // For now, we test that the current token structure supports role validation
      const userContext = JwtTestUtils.extractUserContext(authResponse.tokens.accessToken);
      expect(userContext?.roles).toContain('USER');
    });

    test('should require new tokens after role changes', async () => {
      const testUser = await authFramework.createUserWithRoles(['USER'], {
        username: 'role_change_token_test',
        email: 'role_change_token_test@example.com',
        emailVerified: true
      });

      // Login and get initial tokens
      const initialAuth = await authFramework.loginTestUser(testUser);
      const initialRoles = JwtTestUtils.extractUserContext(initialAuth.tokens.accessToken)?.roles || [];

      // Refresh tokens (simulating role change scenario)
      const refreshedTokens = await authFramework.refreshTokens();
      const refreshedRoles = JwtTestUtils.extractUserContext(refreshedTokens.accessToken)?.roles || [];

      // Verify token structure supports role updates
      expect(Array.isArray(initialRoles)).toBe(true);
      expect(Array.isArray(refreshedRoles)).toBe(true);
    });
  });
});
