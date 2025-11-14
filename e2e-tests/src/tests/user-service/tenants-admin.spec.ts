import { test, expect } from '@playwright/test';
import { createApiClient } from '../../utils/api-client.js';
import { TypedApiClient } from '../../utils/typed-api-client.js';
import { AuthTestFramework } from '../../utils/auth-test-framework.js';
import { getEnvironmentConfig } from '../../config/environments.js';

// Admin Tenants CRUD/List (conditional on RBAC)
// Endpoints:
// - GET /api/v1/tenants
// - POST /api/v1/tenants
// - PUT /api/v1/tenants/{id}
// - GET /api/v1/tenants/current

test.describe('User Service - Tenants Admin (Conditional)', () => {
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

  test('List, create, update tenants (skip on 401/403)', async () => {
    const user = await auth.registerUser({ emailVerified: true, tenantId: 'default' });
    await auth.loginTestUser(user);

    // List tenants
    try {
      const list = await client.getTenants({ page: 0, size: 5 });
      expect(list.page.number).toBeGreaterThanOrEqual(0);
    } catch (e: any) {
      if (e.status === 401 || e.status === 403) test.skip(true, 'Admin privilege required to list tenants');
      throw e;
    }

    // Create tenant
    let createdId: string | null = null;
    try {
      const created = await client.createTenant({
        name: `E2E Tenant ${Date.now()}`,
        subdomain: `e2e-${Date.now()}`,
        enabled: true,
        settings: { plan: 'test' }
      });
      createdId = created.id;
      expect(created.name).toBeDefined();
    } catch (e: any) {
      if (e.status === 401 || e.status === 403) test.skip(true, 'Admin privilege required to create tenants');
      throw e;
    }

    // Update tenant
    const updated = await client.updateTenant(createdId!, { name: 'E2E Tenant Updated' });
    expect(updated.name).toContain('Updated');

    // Current tenant endpoint should work
    const current = await client.getCurrentTenant();
    expect(current).toBeDefined();
  });
});
