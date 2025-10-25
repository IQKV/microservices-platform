import { test, expect } from '@playwright/test';
import { createApiClient } from '../../utils/api-client.js';
import { TypedApiClient } from '../../utils/typed-api-client.js';
import { getEnvironmentConfig } from '../../config/environments.js';

// Public categories endpoints via Gateway -> Bookstore
// Endpoints under: /api/v1/books/categories/**

test.describe('Bookstore Service - Categories (Public)', () => {
  const env = getEnvironmentConfig();
  const apiClient = createApiClient(env);
  const client = new TypedApiClient(apiClient);

  test.beforeAll(async () => {
    await apiClient.initialize();
  });

  test.afterAll(async () => {
    await client.dispose();
  });

  test('GET /api/v1/books/categories should return a list (smoke)', async () => {
    const res = await client.getCategories();
    expect(Array.isArray(res)).toBe(true);
  });

  test('GET /api/v1/books/categories/{id} should return a category when an id exists (conditional)', async () => {
    const list = await client.getCategories();
    if (!list.length) test.skip();

    const first = list[0];
    const byId = await client.getCategoryById(first.id);
    expect(byId.id).toBe(first.id);
  });
});
