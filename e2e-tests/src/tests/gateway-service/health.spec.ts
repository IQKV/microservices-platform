import { test, expect } from '@playwright/test';
import { createApiClient } from '../../utils/api-client.js';
import { TypedApiClient } from '../../utils/typed-api-client.js';
import { getEnvironmentConfig } from '../../config/environments.js';

// Spring Boot Actuator and service health coverage via Gateway
// Endpoints:
// - GET /actuator/health (gateway)
// - GET /api/v1/auth/health (auth)
// - GET /api/v1/books/health (bookstore)

test.describe('Gateway and Services - Health Checks', () => {
  const env = getEnvironmentConfig();
  const apiClient = createApiClient(env);
  const client = new TypedApiClient(apiClient);

  test.beforeAll(async () => {
    await apiClient.initialize();
  });

  test.afterAll(async () => {
    await client.dispose();
  });

  test('GET /actuator/health should report UP', async () => {
    const res = await client.checkGatewayHealth();
    expect(res.status).toBe('UP');
  });

  test('GET /api/v1/auth/health should report UP', async () => {
    const res = await client.checkAuthServiceHealth();
    expect(res.status).toBe('UP');
  });

  test('GET /api/v1/books/health should report UP', async () => {
    const res = await client.checkBookstoreServiceHealth();
    expect(res.status).toBe('UP');
  });
});
