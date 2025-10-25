/**
 * Type-safe API client methods for Gripday microservices
 * Provides strongly typed methods for all API endpoints
 */

import { ApiClient } from './api-client.js';
import { ValidationUtils, schemas } from './validation.js';
import {
  ApiResponse,
  LoginCredentials,
  AuthResponse,
  UserRegistrationData,
  UserResponse,
  VerificationResponse,
  BookData,
  BookResponse,
  TenantData,
  PaginatedResponse,
  HealthCheckResponse,
  TokenRefreshResponse,
  LogoutResponse
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
  constructor(private apiClient: ApiClient) {}

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
  async verifyEmail(token: string): Promise<VerificationResponse> {
    const response = await this.apiClient.request<VerificationResponse>({
      method: 'POST',
      url: '/api/v1/auth/verify-email',
      data: { token }
    });

    return response.data;
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
      url: '/api/v1/auth/me'
    });

    return ValidationUtils.validateUserData(response.data);
  }

  /**
   * Update current user profile
   */
  async updateCurrentUser(userData: Partial<UserRegistrationData>): Promise<UserResponse> {
    const response = await this.apiClient.request<UserResponse>({
      method: 'PUT',
      url: '/api/v1/auth/me',
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
      url: '/api/v1/users',
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
      url: `/api/v1/users/${userId}`
    });

    return ValidationUtils.validateUserData(response.data);
  }

  /**
   * Create new user (admin only)
   */
  async createUser(userData: UserRegistrationData): Promise<UserResponse> {
    ValidationUtils.validate(userData, schemas.userRegistration);
    
    const response = await this.apiClient.request<UserResponse>({
      method: 'POST',
      url: '/api/v1/users',
      data: userData
    });

    return ValidationUtils.validateUserData(response.data);
  }

  /**
   * Update user (admin only)
   */
  async updateUser(userId: number, userData: Partial<UserRegistrationData>): Promise<UserResponse> {
    const response = await this.apiClient.request<UserResponse>({
      method: 'PUT',
      url: `/api/v1/users/${userId}`,
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
      url: `/api/v1/users/${userId}`
    });
  }

  /**
   * Enable/disable user (admin only)
   */
  async setUserEnabled(userId: number, enabled: boolean): Promise<UserResponse> {
    const response = await this.apiClient.request<UserResponse>({
      method: 'PATCH',
      url: `/api/v1/users/${userId}/enabled`,
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
  async createTenant(tenantData: Omit<TenantData, 'id' | 'createdAt' | 'updatedAt'>): Promise<TenantData> {
    const response = await this.apiClient.request<TenantData>({
      method: 'POST',
      url: '/api/v1/tenants',
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
      url: '/api/v1/books',
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
      url: `/api/v1/books/${bookId}`
    });

    return ValidationUtils.validateBookData(response.data);
  }

  /**
   * Create new book
   */
  async createBook(bookData: Omit<BookData, 'id' | 'tenantId' | 'createdAt' | 'updatedAt'>): Promise<BookResponse> {
    ValidationUtils.validate(bookData, schemas.createBook);
    
    const response = await this.apiClient.request<BookResponse>({
      method: 'POST',
      url: '/api/v1/books',
      data: bookData
    });

    return ValidationUtils.validateBookData(response.data);
  }

  /**
   * Update book
   */
  async updateBook(bookId: number, bookData: Partial<Omit<BookData, 'id' | 'tenantId' | 'createdAt' | 'updatedAt'>>): Promise<BookResponse> {
    const response = await this.apiClient.request<BookResponse>({
      method: 'PUT',
      url: `/api/v1/books/${bookId}`,
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
      url: `/api/v1/books/${bookId}`
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
      url: '/api/v1/books/search',
      params: searchParams
    });

    return ValidationUtils.validatePaginatedResponse(response.data, schemas.bookData);
  }

  /**
   * Update book stock
   */
  async updateBookStock(bookId: number, stock: number): Promise<BookResponse> {
    const response = await this.apiClient.request<BookResponse>({
      method: 'PATCH',
      url: `/api/v1/books/${bookId}/stock`,
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
      url: `/api/v1/books/category/${encodeURIComponent(category)}`,
      params: params as Record<string, string | number | boolean>
    });

    return ValidationUtils.validatePaginatedResponse(response.data, schemas.bookData);
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
   * Check Auth service health
   */
  async checkAuthServiceHealth(): Promise<HealthCheckResponse> {
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