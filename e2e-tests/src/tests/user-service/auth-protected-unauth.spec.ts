import { test, expect } from '@playwright/test';
import { createApiClient } from '../../utils/api-client.js';
import { getEnvironmentConfig } from '../../config/environments.js';

// Ensure protected Auth REST endpoints require authentication
// - GET /api/v1/users/me
// - PUT /api/v1/users/me
// - POST /api/v1/auth/logout

test.describe('User Service - Protected Endpoints (Unauthenticated)', () => {
  const env = getEnvironmentConfig();
  const apiClient = createApiClient(env);

  test.beforeAll(async () => {
    await apiClient.initialize();
  });

  test.afterAll(async () => {
    await apiClient.dispose();
  });

  test('GET /api/v1/users/me should be unauthorized without token', async () => {
    try {
      await apiClient.request({ method: 'GET', url: '/api/v1/users/me' });
      test.fail(true, 'Request should not succeed without authentication');
    } catch (e: any) {
      expect([401, 403]).toContain(e.status);
    }
  });

  test('PUT /api/v1/users/me should be unauthorized without token', async () => {
    try {
      await apiClient.request({ method: 'PUT', url: '/api/v1/users/me', data: { firstName: 'X' } });
      test.fail(true, 'Request should not succeed without authentication');
    } catch (e: any) {
      expect([401, 403]).toContain(e.status);
    }
  });

  test('POST /api/v1/auth/logout should be unauthorized without token', async () => {
    try {
      await apiClient.request({ method: 'POST', url: '/api/v1/auth/logout' });
      test.fail(true, 'Request should not succeed without authentication');
    } catch (e: any) {
      expect([401, 403]).toContain(e.status);
    }
  });
});
