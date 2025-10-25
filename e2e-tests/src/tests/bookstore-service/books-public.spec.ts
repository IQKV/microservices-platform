import { test, expect } from '@playwright/test';
import { createApiClient } from '../../utils/api-client.js';
import { TypedApiClient } from '../../utils/typed-api-client.js';
import { getEnvironmentConfig } from '../../config/environments.js';

// Public endpoints coverage for Bookstore service via Gateway
// Endpoints under: /api/v1/bookstore/books/**

test.describe('Bookstore Service - Public Catalog', () => {
  const env = getEnvironmentConfig();
  const apiClient = createApiClient(env);
  const client = new TypedApiClient(apiClient);

  test.beforeAll(async () => {
    await apiClient.initialize();
  });

  test.afterAll(async () => {
    await client.dispose();
  });

  test('GET /api/v1/bookstore/books should return paginated list', async () => {
    const res = await client.getBooks({ page: 0, size: 5 });
    expect(res).toBeDefined();
    expect(Array.isArray(res.content)).toBe(true);
    expect(res.page.number).toBeGreaterThanOrEqual(0);
  });

  test('GET /api/v1/bookstore/books/search should support basic search params', async () => {
    const res = await client.searchBooks('', { page: 0, size: 5 });
    expect(res).toBeDefined();
    expect(res.page.size).toBeGreaterThan(0);
  });

  test('GET /api/v1/bookstore/books/{id} should work when an id exists (conditional)', async () => {
    const list = await client.getBooks({ page: 0, size: 1 });
    if (list.content.length === 0) test.skip();

    const first = list.content[0];
    const byId = await client.getBookById(first.id);
    expect(byId.id).toBe(first.id);
  });

  test('GET /api/v1/bookstore/books/search/category should filter by category (smoke)', async () => {
    const res = await client.getBooksByCategory('Fiction', { page: 0, size: 5 });
    expect(res).toBeDefined();
    expect(res.page.number).toBeGreaterThanOrEqual(0);
  });
});
