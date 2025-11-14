import { test, expect } from '@playwright/test';
import { createApiClient } from '../../utils/api-client.js';
import { TypedApiClient } from '../../utils/typed-api-client.js';
import { AuthTestFramework } from '../../utils/auth-test-framework.js';
import { getEnvironmentConfig } from '../../config/environments.js';

// Admin Users CRUD (conditional on RBAC)
// Endpoints:
// - GET /api/v1/users
// - POST /api/v1/users
// - GET /api/v1/users/{id}
// - PUT /api/v1/users/{id}
// - DELETE /api/v1/users/{id}

test.describe('User Service - Admin Users CRUD (Conditional)', () => {
  const env = getEnvironmentConfig();
  const apiClient = createApiClient(env);
  const client = new TypedApiClient(apiClient);
  const auth = new AuthTestFramework(apiClient);

  test.beforeAll(async () => {
    await apiClient.initialize();
  });

  test.afterAll(async () => {
    await auth.dispose();
  });

  test('List, create, update, get and delete users (skip on 401/403)', async () => {
    // Authenticate as a normal user first; environments may require ADMIN role.
    const user = await auth.registerUser({ emailVerified: true });
    await auth.loginTestUser(user);

    // List users
    try {
      const list = await client.getUsers({ page: 0, size: 5 });
      expect(list.page.number).toBeGreaterThanOrEqual(0);
    } catch (e: any) {
      if (e.status === 401 || e.status === 403) test.skip(true, 'Admin privilege required to list users');
      throw e;
    }

    // Create user
    let createdId: number | null = null;
    try {
      const created = await client.createUser({
        username: `e2e_admin_user_${Date.now()}`,
        email: `e2e_admin_${Date.now()}@example.com`,
        password: 'TestPassword123!',
        roles: ['USER'],
        enabled: true
      });
      createdId = created.id;
      expect(created.username).toBeDefined();
    } catch (e: any) {
      if (e.status === 401 || e.status === 403) test.skip(true, 'Admin privilege required to create users');
      throw e;
    }

    // Get by id
    const byId = await client.getUserById(createdId!);
    expect(byId.id).toBe(createdId);

    // Update user
    const updated = await client.updateUser(createdId!, { firstName: 'E2E', lastName: 'Updated' });
    expect(updated.firstName).toBe('E2E');

    // Delete user
    await client.deleteUser(createdId!);
  });
});
