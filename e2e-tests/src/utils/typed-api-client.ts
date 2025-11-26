/**
 * Type-safe API client methods for IQ Scaffold microservices
 * Provides strongly typed methods for all API endpoints
 */

import { ApiClient } from './api-client.js';
import { ValidationUtils, schemas } from './validation.js';
import {
  LoginCredentials,
  AuthResponse,
  UserRegistrationData,
  UserResponse,
  VerificationResponse,
  BookResponse,
  TenantData,
  PaginatedResponse,
  HealthCheckResponse,
  TokenRefreshResponse,
  LogoutResponse,
  CreateBookRequest,
  UpdateBookRequest,
  CreateUserRequest,
  UpdateUserRequest,
  CreateTenantRequest,
  UpdateTenantRequest,
  EmailVerificationRequest,
  ResendVerificationRequest,
  VerificationStatusResponse,
  UpdateInventoryRequest,
  BulkInventoryRequest,
  CategoryResponse
} from '../types/api-responses.js';

export interface PaginationParams {
  page?: number;
  size?: number;
  sort?: string;
  direction?: 'asc' | 'desc';
}

export interface BookSearchParams extends PaginationParams {
  title?: string;
  author?: string;
  category?: string;
  tags?: string[];
  minPrice?: number;
  maxPrice?: number;
  inStock?: boolean;
}

export interface UserSearchParams extends PaginationParams {
  username?: string;
  email?: string;
  role?: string;
  enabled?: boolean;
  emailVerified?: boolean;
}

/**
 * Type-safe API client with validation and strongly typed methods
 */
export class TypedApiClient {
  constructor(private readonly apiClient: ApiClient) {}

  // ============================================================================
  // Authentication Service Methods
  // ============================================================================

  /**
   * Authenticate user with credentials
   */
  async login(credentials: LoginCredentials): Promise<AuthResponse> {
    // Validate input
    ValidationUtils.validate(credentials, schemas.loginCredentials);
    
    const response = await this.apiClient.authenticate(credentials);
    
    // Validate response
    return ValidationUtils.validateAuthResponse(response);
  }

  /**
   * Register a new user
   */
  async registerUser(userData: UserRegistrationData): Promise<UserResponse> {
    // Validate input
    ValidationUtils.validate(userData, schemas.userRegistration);
    
    const response = await this.apiClient.request<UserResponse>({
      method: 'POST',
      url: '/api/v1/auth/register',
      data: userData
    });

    // Validate response
    return ValidationUtils.validateUserData(response.data);
  }

  /**
   * Verify user email with token
   */
  async verifyEmail(request: EmailVerificationRequest): Promise<VerificationResponse> {
    // Validate input
    ValidationUtils.validate(request, schemas.emailVerification);
    
    const response = await this.apiClient.request<VerificationResponse>({
      method: 'POST',
      url: '/api/v1/auth/verify-email',
      data: request
    });

    return response.data;
  }

  /**
   * Resend email verification
   */
  async resendEmailVerification(request: ResendVerificationRequest): Promise<VerificationStatusResponse> {
    // Validate input
    ValidationUtils.validate(request, schemas.resendVerification);
    
    const response = await this.apiClient.request<VerificationStatusResponse>({
      method: 'POST',
      url: '/api/v1/auth/resend-verification',
      data: request
    });

    return ValidationUtils.validateVerificationStatusResponse(response.data);
  }

  /**
   * Get email verification status
   */
  async getVerificationStatus(email: string): Promise<VerificationStatusResponse> {
    const response = await this.apiClient.request<VerificationStatusResponse>({
      method: 'GET',
      url: '/api/v1/auth/verification-status',
      params: { email }
    });

    return ValidationUtils.validateVerificationStatusResponse(response.data);
  }

  /**
   * Refresh authentication tokens
   */
  async refreshTokens(): Promise<TokenRefreshResponse> {
    const tokens = this.apiClient.getAuthTokens();
    if (!tokens?.refreshToken) {
      throw new Error('No refresh token available');
    }

    const response = await this.apiClient.request<TokenRefreshResponse>({
      method: 'POST',
      url: '/api/v1/auth/refresh',
      data: { refreshToken: tokens.refreshToken }
    });

    // Validate response
    ValidationUtils.validate(response.data, schemas.authTokens);
    return response.data;
  }

  /**
   * Logout user and invalidate tokens
   */
  async logout(): Promise<LogoutResponse> {
    const response = await this.apiClient.request<LogoutResponse>({
      method: 'POST',
      url: '/api/v1/auth/logout'
    });

    // Clear local tokens
    this.apiClient.clearAuth();
    
    return response.data;
  }

  /**
   * Get current user profile
   */
  async getCurrentUser(): Promise<UserResponse> {
    const response = await this.apiClient.request<UserResponse>({
      method: 'GET',
      url: '/api/v1/users/me'
    });

    return ValidationUtils.validateUserData(response.data);
  }

  /**
   * Update current user profile
   */
  async updateCurrentUser(userData: UpdateUserRequest): Promise<UserResponse> {
    ValidationUtils.validate(userData, schemas.updateUser);
    
    const response = await this.apiClient.request<UserResponse>({
      method: 'PUT',
      url: '/api/v1/users/me',
      data: userData
    });

    return ValidationUtils.validateUserData(response.data);
  }

  // ============================================================================
  // User Management Methods (Admin only)
  // ============================================================================

  /**
   * Get all users (admin only)
   */
  async getUsers(params?: UserSearchParams): Promise<PaginatedResponse<UserResponse>> {
    const response = await this.apiClient.request<PaginatedResponse<UserResponse>>({
      method: 'GET',
      url: '/api/v1/admin/users',
      params: params as Record<string, string | number | boolean>
    });

    return ValidationUtils.validatePaginatedResponse(response.data, schemas.userData);
  }

  /**
   * Get user by ID (admin only)
   */
  async getUserById(userId: number): Promise<UserResponse> {
    const response = await this.apiClient.request<UserResponse>({
      method: 'GET',
      url: `/api/v1/admin/users/${userId}`
    });

    return ValidationUtils.validateUserData(response.data);
  }

  /**
   * Create new user (admin only)
   */
  async createUser(userData: CreateUserRequest): Promise<UserResponse> {
    ValidationUtils.validate(userData, schemas.createUser);
    
    const response = await this.apiClient.request<UserResponse>({
      method: 'POST',
      url: '/api/v1/admin/users',
      data: userData
    });

    return ValidationUtils.validateUserData(response.data);
  }

  /**
   * Update user (admin only)
   */
  async updateUser(userId: number, userData: UpdateUserRequest): Promise<UserResponse> {
    ValidationUtils.validate(userData, schemas.updateUser);
    
    const response = await this.apiClient.request<UserResponse>({
      method: 'PUT',
      url: `/api/v1/admin/users/${userId}`,
      data: userData
    });

    return ValidationUtils.validateUserData(response.data);
  }

  /**
   * Delete user (admin only)
   */
  async deleteUser(userId: number): Promise<void> {
    await this.apiClient.request({
      method: 'DELETE',
      url: `/api/v1/admin/users/${userId}`
    });
  }

  /**
   * Enable/disable user (admin only)
   */
  async setUserEnabled(userId: number, enabled: boolean): Promise<UserResponse> {
    const response = await this.apiClient.request<UserResponse>({
      method: 'PATCH',
      url: `/api/v1/admin/users/${userId}/enabled`,
      data: { enabled }
    });

    return ValidationUtils.validateUserData(response.data);
  }

  // ============================================================================
  // Tenant Management Methods
  // ============================================================================

  /**
   * Get current tenant information
   */
  async getCurrentTenant(): Promise<TenantData> {
    const response = await this.apiClient.request<TenantData>({
      method: 'GET',
      url: '/api/v1/tenants/current'
    });

    return ValidationUtils.validateTenantData(response.data);
  }

  /**
   * Get all tenants (admin only)
   */
  async getTenants(params?: PaginationParams): Promise<PaginatedResponse<TenantData>> {
    const response = await this.apiClient.request<PaginatedResponse<TenantData>>({
      method: 'GET',
      url: '/api/v1/tenants',
      params: params as Record<string, string | number | boolean>
    });

    return ValidationUtils.validatePaginatedResponse(response.data, schemas.tenantData);
  }

  /**
   * Create new tenant (admin only)
   */
  async createTenant(tenantData: CreateTenantRequest): Promise<TenantData> {
    ValidationUtils.validate(tenantData, schemas.createTenant);
    
    const response = await this.apiClient.request<TenantData>({
      method: 'POST',
      url: '/api/v1/tenants',
      data: tenantData
    });

    return ValidationUtils.validateTenantData(response.data);
  }

  /**
   * Update tenant (admin only)
   */
  async updateTenant(tenantId: string, tenantData: UpdateTenantRequest): Promise<TenantData> {
    ValidationUtils.validate(tenantData, schemas.updateTenant);
    
    const response = await this.apiClient.request<TenantData>({
      method: 'PUT',
      url: `/api/v1/tenants/${tenantId}`,
      data: tenantData
    });

    return ValidationUtils.validateTenantData(response.data);
  }

  // ============================================================================
  // Bookstore Service Methods
  // ============================================================================

  /**
   * Get all books
   */
  async getBooks(params?: BookSearchParams): Promise<PaginatedResponse<BookResponse>> {
    const response = await this.apiClient.request<PaginatedResponse<BookResponse>>({
      method: 'GET',
      url: '/api/v1/bookstore/books',
      params: params as Record<string, string | number | boolean>
    });

    return ValidationUtils.validatePaginatedResponse(response.data, schemas.bookData);
  }

  /**
   * Get book by ID
   */
  async getBookById(bookId: number): Promise<BookResponse> {
    const response = await this.apiClient.request<BookResponse>({
      method: 'GET',
      url: `/api/v1/bookstore/books/${bookId}`
    });

    return ValidationUtils.validateBookData(response.data);
  }

  /**
   * Create new book
   */
  async createBook(bookData: CreateBookRequest): Promise<BookResponse> {
    ValidationUtils.validate(bookData, schemas.createBook);
    
    const response = await this.apiClient.request<BookResponse>({
      method: 'POST',
      url: '/api/v1/bookstore/books',
      data: bookData
    });

    return ValidationUtils.validateBookData(response.data);
  }

  /**
   * Update book
   */
  async updateBook(bookId: number, bookData: UpdateBookRequest): Promise<BookResponse> {
    ValidationUtils.validate(bookData, schemas.updateBook);
    
    const response = await this.apiClient.request<BookResponse>({
      method: 'PUT',
      url: `/api/v1/bookstore/books/${bookId}`,
      data: bookData
    });

    return ValidationUtils.validateBookData(response.data);
  }

  /**
   * Delete book
   */
  async deleteBook(bookId: number): Promise<void> {
    await this.apiClient.request({
      method: 'DELETE',
      url: `/api/v1/bookstore/books/${bookId}`
    });
  }

  /**
   * Search books
   */
  async searchBooks(query: string, params?: BookSearchParams): Promise<PaginatedResponse<BookResponse>> {
    const searchParams: Record<string, string | number | boolean> = {
      q: query,
      ...(params?.page !== undefined && { page: params.page }),
      ...(params?.size !== undefined && { size: params.size }),
      ...(params?.sort && { sort: params.sort }),
      ...(params?.direction && { direction: params.direction }),
      ...(params?.title && { title: params.title }),
      ...(params?.author && { author: params.author }),
      ...(params?.category && { category: params.category }),
      ...(params?.tags && { tags: params.tags.join(',') }),
      ...(params?.minPrice !== undefined && { minPrice: params.minPrice }),
      ...(params?.maxPrice !== undefined && { maxPrice: params.maxPrice }),
      ...(params?.inStock !== undefined && { inStock: params.inStock })
    };

    const response = await this.apiClient.request<PaginatedResponse<BookResponse>>({
      method: 'GET',
      url: '/api/v1/bookstore/books/search',
      params: searchParams
    });

    return ValidationUtils.validatePaginatedResponse(response.data, schemas.bookData);
  }

  /**
   * Update book stock
   */
  async updateBookStock(bookId: number, stock: number): Promise<BookResponse> {
    const response = await this.apiClient.request<BookResponse>({
      method: 'PUT',
      url: `/api/v1/bookstore/inventory/${bookId}`,
      data: { stock }
    });

    return ValidationUtils.validateBookData(response.data);
  }

  /**
   * Get books by category
   */
  async getBooksByCategory(category: string, params?: PaginationParams): Promise<PaginatedResponse<BookResponse>> {
    const response = await this.apiClient.request<PaginatedResponse<BookResponse>>({
      method: 'GET',
      url: `/api/v1/bookstore/books/search/category`,
      params: { category, ...(params as Record<string, string | number | boolean>) } as Record<string, string | number | boolean>
    });

    return ValidationUtils.validatePaginatedResponse(response.data, schemas.bookData);
  }

  /**
   * Update book inventory/stock
   */
  async updateBookInventory(bookId: number, inventoryData: UpdateInventoryRequest): Promise<BookResponse> {
    ValidationUtils.validate(inventoryData, schemas.updateInventory);
    
    const response = await this.apiClient.request<BookResponse>({
      method: 'PUT',
      url: `/api/v1/bookstore/inventory/${bookId}`,
      data: inventoryData
    });

    return ValidationUtils.validateBookData(response.data);
  }

  /**
   * Bulk update book inventory
   */
  async bulkUpdateInventory(inventoryData: BulkInventoryRequest): Promise<BookResponse[]> {
    ValidationUtils.validate(inventoryData, schemas.bulkInventory);
    
    const response = await this.apiClient.request<BookResponse[]>({
      method: 'POST',
      url: '/api/v1/bookstore/inventory/bulk-update',
      data: inventoryData.updates
    });

    // Validate each book in the response
    return response.data.map(book => ValidationUtils.validateBookData(book));
  }

  /**
   * Get all categories
   */
  async getCategories(): Promise<CategoryResponse[]> {
    const response = await this.apiClient.request<CategoryResponse[]>({
      method: 'GET',
      url: '/api/v1/books/categories'
    });

    return response.data.map(category => ValidationUtils.validateCategoryResponse(category));
  }

  /**
   * Get category by ID
   */
  async getCategoryById(categoryId: number): Promise<CategoryResponse> {
    const response = await this.apiClient.request<CategoryResponse>({
      method: 'GET',
      url: `/api/v1/books/categories/${categoryId}`
    });

    return ValidationUtils.validateCategoryResponse(response.data);
  }

  // ============================================================================
  // Health Check Methods
  // ============================================================================

  /**
   * Check Gateway service health
   */
  async checkGatewayHealth(): Promise<HealthCheckResponse> {
    const response = await this.apiClient.request<HealthCheckResponse>({
      method: 'GET',
      url: '/actuator/health'
    });

    return ValidationUtils.validateHealthCheckResponse(response.data);
  }

  /**
   * Check User service health
   */
  async checkUserServiceHealth(): Promise<HealthCheckResponse> {
    const response = await this.apiClient.request<HealthCheckResponse>({
      method: 'GET',
      url: '/api/v1/auth/health'
    });

    return ValidationUtils.validateHealthCheckResponse(response.data);
  }

  /**
   * Check Bookstore service health
   */
  async checkBookstoreServiceHealth(): Promise<HealthCheckResponse> {
    const response = await this.apiClient.request<HealthCheckResponse>({
      method: 'GET',
      url: '/api/v1/books/health'
    });

    return ValidationUtils.validateHealthCheckResponse(response.data);
  }

  // ============================================================================
  // Utility Methods
  // ============================================================================

  /**
   * Get the underlying API client
   */
  getApiClient(): ApiClient {
    return this.apiClient;
  }

  /**
   * Set tenant for multi-tenant requests
   */
  setTenant(tenantId: string): void {
    this.apiClient.setTenant(tenantId);
  }

  /**
   * Check if client is authenticated
   */
  isAuthenticated(): boolean {
    return this.apiClient.isAuthenticated();
  }

  /**
   * Get current tenant ID
   */
  getTenantId(): string | null {
    return this.apiClient.getTenantId();
  }

  /**
   * Clear authentication
   */
  clearAuth(): void {
    this.apiClient.clearAuth();
  }

  /**
   * Dispose of the client
   */
  async dispose(): Promise<void> {
    await this.apiClient.dispose();
  }
}
