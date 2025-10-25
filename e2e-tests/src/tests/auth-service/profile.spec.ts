import { test, expect } from '@playwright/test';
import { createApiClient } from '../../utils/api-client.js';
import { TypedApiClient } from '../../utils/typed-api-client.js';
import { AuthTestFramework } from '../../utils/auth-test-framework.js';
import { getEnvironmentConfig } from '../../config/environments.js';

// Coverage for profile endpoints via Auth service through the Gateway
// - GET /api/v1/auth/me
// - PUT /api/v1/auth/me

test.describe('Auth Service - Profile Endpoints', () => {
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

  test('GET /api/v1/auth/me should return current user after login', async () => {
    const user = await auth.registerUser({ emailVerified: true });
    await auth.loginTestUser(user);

    const me = await client.getCurrentUser();
    expect(me.username).toBe(user.username);
    expect(me.email).toBe(user.email);
  });

  test('PUT /api/v1/auth/me should update current user fields', async () => {
    const user = await auth.registerUser({ emailVerified: true });
    await auth.loginTestUser(user);

    const first = 'UpdatedFirst';
    const last = 'UpdatedLast';

    const updated = await client.updateCurrentUser({ firstName: first, lastName: last });
    expect(updated.firstName).toBe(first);
    expect(updated.lastName).toBe(last);

    const me = await client.getCurrentUser();
    expect(me.firstName).toBe(first);
    expect(me.lastName).toBe(last);
  });
});
