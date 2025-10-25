/**
 * Example usage of the API client framework
 * This file demonstrates how to use the TypedApiClient for testing
 */

import { createApiClient, TypedApiClient } from './index.js';
import { getEnvironmentConfig } from '../config/environments.js';

/**
 * Example: Basic API client usage
 */
export async function exampleBasicUsage(): Promise<void> {
  // Get environment configuration
  const envConfig = getEnvironmentConfig();
  
  // Create API client
  const apiClient = createApiClient(envConfig, 'tenant-123');
  await apiClient.initialize();
  
  // Create typed client wrapper
  const typedClient = new TypedApiClient(apiClient);
  
  try {
    // Example: Login
    const authResponse = await typedClient.login({
      username: 'testuser',
      password: 'testpassword',
      tenantId: 'tenant-123'
    });
    
    console.log('Login successful:', authResponse.user.username);
    
    // Example: Get current user
    const currentUser = await typedClient.getCurrentUser();
    console.log('Current user:', currentUser.email);
    
    // Example: Get books with pagination
    const books = await typedClient.getBooks({
      page: 0,
      size: 10,
      sort: 'title',
      direction: 'asc'
    });
    
    console.log(`Found ${books.page.totalElements} books`);
    
    // Example: Search books
    const searchResults = await typedClient.searchBooks('javascript', {
      category: 'programming',
      inStock: true
    });
    
    console.log(`Search found ${searchResults.content.length} books`);
    
    // Example: Health checks
    const gatewayHealth = await typedClient.checkGatewayHealth();
    console.log('Gateway health:', gatewayHealth.status);
    
  } catch (error) {
    console.error('API call failed:', error);
  } finally {
    // Clean up
    await typedClient.dispose();
  }
}

/**
 * Example: Multi-tenant usage
 */
export async function exampleMultiTenantUsage(): Promise<void> {
  const envConfig = getEnvironmentConfig();
  const apiClient = createApiClient(envConfig);
  await apiClient.initialize();
  
  const typedClient = new TypedApiClient(apiClient);
  
  try {
    // Login as admin user
    await typedClient.login({
      username: 'admin',
      password: 'adminpassword'
    });
    
    // Switch to tenant A
    typedClient.setTenant('tenant-a');
    const tenantABooks = await typedClient.getBooks();
    console.log(`Tenant A has ${tenantABooks.page.totalElements} books`);
    
    // Switch to tenant B
    typedClient.setTenant('tenant-b');
    const tenantBBooks = await typedClient.getBooks();
    console.log(`Tenant B has ${tenantBBooks.page.totalElements} books`);
    
    // Verify tenant isolation
    if (tenantABooks.content.length > 0 && tenantBBooks.content.length > 0) {
      const tenantABookIds = tenantABooks.content.map(book => book.id);
      const tenantBBookIds = tenantBBooks.content.map(book => book.id);
      const overlap = tenantABookIds.filter(id => tenantBBookIds.includes(id));
      
      if (overlap.length === 0) {
        console.log('✓ Tenant isolation verified - no shared books');
      } else {
        console.log('✗ Tenant isolation failed - found shared books:', overlap);
      }
    }
    
  } catch (error) {
    console.error('Multi-tenant test failed:', error);
  } finally {
    await typedClient.dispose();
  }
}

/**
 * Example: Error handling and retry logic
 */
export async function exampleErrorHandling(): Promise<void> {
  const envConfig = getEnvironmentConfig();
  const apiClient = createApiClient(envConfig);
  await apiClient.initialize();
  
  const typedClient = new TypedApiClient(apiClient);
  
  try {
    // This should fail with authentication error
    await typedClient.getCurrentUser();
  } catch (error) {
    console.log('Expected authentication error:', error);
  }
  
  try {
    // Login with invalid credentials
    await typedClient.login({
      username: 'invalid',
      password: 'invalid'
    });
  } catch (error) {
    console.log('Expected login failure:', error);
  }
  
  try {
    // Login successfully
    await typedClient.login({
      username: 'testuser',
      password: 'testpassword'
    });
    
    // Try to access non-existent resource
    await typedClient.getBookById(999999);
  } catch (error) {
    console.log('Expected not found error:', error);
  }
  
  await typedClient.dispose();
}

/**
 * Example: Request/response validation
 */
export async function exampleValidation(): Promise<void> {
  const envConfig = getEnvironmentConfig();
  const apiClient = createApiClient(envConfig);
  await apiClient.initialize();
  
  const typedClient = new TypedApiClient(apiClient);
  
  try {
    // Login
    await typedClient.login({
      username: 'testuser',
      password: 'testpassword'
    });
    
    // Create book with validation
    const newBook = await typedClient.createBook({
      title: 'Test Book',
      author: 'Test Author',
      isbn: '978-0-123456-78-9',
      description: 'A test book for API validation',
      price: 29.99,
      currency: 'USD',
      stock: 100,
      category: 'testing',
      tags: ['api', 'testing', 'validation']
    });
    
    console.log('Book created successfully:', newBook.title);
    
    // Update book
    const updatedBook = await typedClient.updateBook(newBook.id, {
      price: 24.99,
      stock: 95
    });
    
    console.log('Book updated successfully:', updatedBook.price);
    
    // Clean up
    await typedClient.deleteBook(newBook.id);
    console.log('Book deleted successfully');
    
  } catch (error) {
    console.error('Validation example failed:', error);
  } finally {
    await typedClient.dispose();
  }
}