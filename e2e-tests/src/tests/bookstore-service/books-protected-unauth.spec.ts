import { test, expect } from '@playwright/test';
import { createApiClient } from '../../utils/api-client.js';
import { getEnvironmentConfig } from '../../config/environments.js';

// Ensure protected REST endpoints of Bookstore are secured
// Tries unauthenticated requests and expects 401/403

test.describe('Bookstore Service - Protected Endpoints (Unauthenticated)', () => {
  const env = getEnvironmentConfig();
  const apiClient = createApiClient(env);

  test.beforeAll(async () => {
    await apiClient.initialize();
  });

  test.afterAll(async () => {
    await apiClient.dispose();
  });

  test('POST /api/v1/bookstore/books should be unauthorized without token', async () => {
    try {
      await apiClient.request({
        method: 'POST',
        url: '/api/v1/bookstore/books',
        data: {
          title: 'Sec Test',
          author: 'Sec Author',
          isbn: '978-1-23456-789-7',
          price: 9.99,
          category: 'Fiction'
        }
      });
      test.fail(true, 'Request should not succeed without authentication');
    } catch (e: any) {
      expect([401, 403]).toContain(e.status);
    }
  });

  test('PUT /api/v1/bookstore/books/{id} should be unauthorized without token', async () => {
    try {
      await apiClient.request({
        method: 'PUT',
        url: '/api/v1/bookstore/books/1',
        data: { price: 10.99 }
      });
      test.fail(true, 'Request should not succeed without authentication');
    } catch (e: any) {
      expect([401, 403]).toContain(e.status);
    }
  });

  test('DELETE /api/v1/bookstore/books/{id} should be unauthorized without token', async () => {
    try {
      await apiClient.request({
        method: 'DELETE',
        url: '/api/v1/bookstore/books/1'
      });
      test.fail(true, 'Request should not succeed without authentication');
    } catch (e: any) {
      expect([401, 403]).toContain(e.status);
    }
  });

  test('PUT /api/v1/bookstore/inventory/{id} should be unauthorized without token', async () => {
    try {
      await apiClient.request({
        method: 'PUT',
        url: '/api/v1/bookstore/inventory/1',
        data: { stock: 5 }
      });
      test.fail(true, 'Request should not succeed without authentication');
    } catch (e: any) {
      expect([401, 403]).toContain(e.status);
    }
  });
});
