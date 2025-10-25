import { test, expect } from '@playwright/test';
import { createApiClient } from '../../utils/api-client.js';
import { TypedApiClient } from '../../utils/typed-api-client.js';
import { getEnvironmentConfig } from '../../config/environments.js';
import { AuthTestFramework } from '../../utils/auth-test-framework.js';

// Authenticated CRUD coverage for Spring Boot REST endpoints in Bookstore
// Endpoints:
// - POST /api/v1/bookstore/books
// - PUT /api/v1/bookstore/books/{id}
// - GET /api/v1/bookstore/books/{id}
// - DELETE /api/v1/bookstore/books/{id}
// Note: Depending on environment RBAC, creation may require elevated roles. Test is conditional.

test.describe('Bookstore Service - CRUD (Authenticated, Conditional)', () => {
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

  test('Create/Update/Get/Delete book (conditional on RBAC)', async () => {
    // Register and login a test user
    const user = await auth.registerUser();
    await auth.loginTestUser(user);

    // Attempt to create a book
    let createdId: number | null = null;
    try {
      const created = await client.createBook({
        title: `E2E Test Book ${Date.now()}`,
        author: 'E2E Tester',
        isbn: `978-1-${Math.floor(Math.random() * 9_000_000)}-0`,
        price: 12.34,
        category: 'Fiction',
        description: 'E2E CRUD test book',
        stock: 10
      });
      createdId = created.id;
    } catch (e: any) {
      // If forbidden due to RBAC, skip rest of CRUD assertions
      if (e.status === 403 || e.status === 401) {
        test.skip(true, 'Insufficient privileges to create book in current environment');
      } else {
        throw e;
      }
    }

    expect(createdId).toBeTruthy();

    // Update book
    const updated = await client.updateBook(createdId!, { price: 15.99 });
    expect(updated.price).toBe(15.99);

    // Get by id
    const fetched = await client.getBookById(createdId!);
    expect(fetched.id).toBe(createdId);

    // Delete book
    await client.deleteBook(createdId!);

    // Verify delete (optional best-effort)
    try {
      await client.getBookById(createdId!);
      test.fail(true, 'Fetching deleted book should fail');
    } catch {
      // Expected to fail with 404 or similar
    }
  });
});
