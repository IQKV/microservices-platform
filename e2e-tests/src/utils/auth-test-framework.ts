/**
 * Authentication Test Framework
 * Provides utilities for testing JWT-based authentication flows
 */

import { faker } from '@faker-js/faker';
import { TypedApiClient } from './typed-api-client.js';
import { ApiClient } from './api-client.js';
import { getEnvironmentConfig } from '../config/environments.js';
import {
  LoginCredentials,
  AuthResponse,
  UserRegistrationData,
  UserResponse,
  VerificationResponse,
  TokenRefreshResponse,
  LogoutResponse,
  AuthTokens,
  UserContext,
  EmailVerificationRequest,
  ResendVerificationRequest,
  VerificationStatusResponse
} from '../types/api-responses.js';

export interface AuthTestUser {
  id?: number;
  username: string;
  email: string;
  password: string;
  firstName?: string;
  lastName?: string;
  roles?: string[];
  tenantId?: string;
  emailVerified?: boolean;
  enabled?: boolean;
}

export interface TokenTestResult {
  isValid: boolean;
  isExpired: boolean;
  claims?: UserContext;
  error?: string;
}

export interface AuthFlowTestResult {
  success: boolean;
  user?: UserResponse;
  tokens?: AuthTokens;
  error?: string;
  duration: number;
}

/**
 * Authentication Test Framework for JWT testing
 */
export class AuthTestFramework {
  private readonly apiClient: ApiClient;
  private readonly typedClient: TypedApiClient;
  private readonly config = getEnvironmentConfig();
  private readonly createdUsers: AuthTestUser[] = [];

  constructor(apiClient?: ApiClient) {
    this.apiClient = apiClient || new ApiClient(this.config);
    this.typedClient = new TypedApiClient(this.apiClient);
  }

  // ============================================================================
  // User Registration and Management
  // ============================================================================

  /**
   * Generate realistic test user data
   */
  generateTestUser(overrides: Partial<AuthTestUser> = {}): AuthTestUser {
    const firstName = faker.name.firstName();
    const lastName = faker.name.lastName();
    const username = faker.internet.userName(firstName, lastName).toLowerCase();
    
    return {
      username,
      email: faker.internet.email(firstName, lastName).toLowerCase(),
      password: this.generateSecurePassword(),
      firstName,
      lastName,
      roles: ['USER'],
      tenantId: overrides.tenantId || 'default',
      emailVerified: false,
      enabled: true,
      ...overrides
    };
  }

  /**
   * Generate secure password for testing
   */
  private generateSecurePassword(): string {
    // Generate password that meets typical security requirements
    const lowercase = faker.random.alphaNumeric(4).toLowerCase();
    const uppercase = faker.random.alphaNumeric(4).toUpperCase();
    const numbers = faker.random.numeric(2);
    const symbols = '!@#$%';
    const symbol = symbols[Math.floor(Math.random() * symbols.length)];
    
    return `${uppercase}${lowercase}${numbers}${symbol}`;
  }

  /**
   * Register a new test user
   */
  async registerUser(userData?: Partial<AuthTestUser>): Promise<AuthTestUser> {
    const testUser = this.generateTestUser(userData);
    
    const registrationData: UserRegistrationData = {
      username: testUser.username,
      email: testUser.email,
      password: testUser.password,
      firstName: testUser.firstName,
      lastName: testUser.lastName,
      tenantId: testUser.tenantId
    };

    try {
      const response = await this.typedClient.registerUser(registrationData);
      testUser.id = response.id;
      testUser.emailVerified = response.emailVerified;
      testUser.enabled = response.enabled;
      
      // Track created user for cleanup
      this.createdUsers.push(testUser);
      
      return testUser;
    } catch (error) {
      throw new Error(`Failed to register test user: ${error instanceof Error ? error.message : 'Unknown error'}`);
    }
  }

  /**
   * Register multiple test users
   */
  async registerMultipleUsers(count: number, userData?: Partial<AuthTestUser>): Promise<AuthTestUser[]> {
    const users: AuthTestUser[] = [];
    
    for (let i = 0; i < count; i++) {
      const user = await this.registerUser({
        ...userData,
        username: `${userData?.username || 'testuser'}_${i + 1}`,
        email: `${userData?.username || 'testuser'}_${i + 1}@example.com`
      });
      users.push(user);
    }
    
    return users;
  }

  /**
   * Login with user credentials
   */
  async loginUser(credentials: LoginCredentials): Promise<AuthResponse> {
    try {
      const response = await this.typedClient.login(credentials);
      
      // Store tokens in API client for subsequent requests
      this.apiClient.setAuthTokens(response.tokens);
      
      return response;
    } catch (error) {
      throw new Error(`Login failed: ${error instanceof Error ? error.message : 'Unknown error'}`);
    }
  }

  /**
   * Login with test user
   */
  async loginTestUser(testUser: AuthTestUser): Promise<AuthResponse> {
    const credentials: LoginCredentials = {
      username: testUser.username,
      password: testUser.password,
      tenantId: testUser.tenantId
    };
    
    return this.loginUser(credentials);
  }

  // ============================================================================
  // Email Verification Testing
  // ============================================================================

  /**
   * Get email verification status
   */
  async getEmailVerificationStatus(email: string): Promise<VerificationStatusResponse> {
    return this.typedClient.getVerificationStatus(email);
  }

  /**
   * Resend email verification
   */
  async resendEmailVerification(email: string): Promise<VerificationStatusResponse> {
    const request: ResendVerificationRequest = { email };
    return this.typedClient.resendEmailVerification(request);
  }

  /**
   * Verify email with token (simulated)
   * Note: In real tests, you would extract the token from email or database
   */
  async verifyEmailWithToken(token: string): Promise<VerificationResponse> {
    const request: EmailVerificationRequest = { token };
    return this.typedClient.verifyEmail(request);
  }

  /**
   * Test complete email verification flow
   */
  async testEmailVerificationFlow(testUser: AuthTestUser): Promise<{
    registrationSuccess: boolean;
    verificationSent: boolean;
    verificationStatus: VerificationStatusResponse;
    error?: string;
  }> {
    try {
      // Check initial verification status
      const initialStatus = await this.getEmailVerificationStatus(testUser.email);
      
      // Resend verification if needed
      const resendResult = await this.resendEmailVerification(testUser.email);
      
      // Check status after resend
      const finalStatus = await this.getEmailVerificationStatus(testUser.email);
      
      return {
        registrationSuccess: true,
        verificationSent: resendResult.verified === false, // Should be false initially
        verificationStatus: finalStatus
      };
    } catch (error) {
      return {
        registrationSuccess: false,
        verificationSent: false,
        verificationStatus: { verified: false, message: 'Error occurred' },
        error: error instanceof Error ? error.message : 'Unknown error'
      };
    }
  }

  // ============================================================================
  // JWT Token Lifecycle Management
  // ============================================================================

  /**
   * Test JWT token refresh
   */
  async refreshTokens(): Promise<TokenRefreshResponse> {
    try {
      const response = await this.typedClient.refreshTokens();
      
      // Update stored tokens
      this.apiClient.setAuthTokens({
        accessToken: response.accessToken,
        refreshToken: response.refreshToken,
        tokenType: response.tokenType,
        expiresIn: response.expiresIn,
        refreshExpiresIn: response.refreshExpiresIn
      });
      
      return response;
    } catch (error) {
      throw new Error(`Token refresh failed: ${error instanceof Error ? error.message : 'Unknown error'}`);
    }
  }

  /**
   * Test token expiration handling
   */
  async testTokenExpiration(): Promise<TokenTestResult> {
    const startTime = Date.now();
    
    try {
      // Get current tokens
      const tokens = this.apiClient.getAuthTokens();
      if (!tokens) {
        return {
          isValid: false,
          isExpired: true,
          error: 'No tokens available'
        };
      }

      // Try to make an authenticated request
      await this.typedClient.getCurrentUser();
      
      return {
        isValid: true,
        isExpired: false
      };
    } catch (error) {
      const duration = Date.now() - startTime;
      
      // Check if error is due to token expiration
      const isExpiredError = error instanceof Error && 
        (error.message.includes('401') || 
         error.message.includes('unauthorized') ||
         error.message.includes('expired'));
      
      return {
        isValid: false,
        isExpired: isExpiredError,
        error: error instanceof Error ? error.message : 'Unknown error'
      };
    }
  }

  /**
   * Test complete token lifecycle (create, use, refresh, expire)
   */
  async testTokenLifecycle(testUser: AuthTestUser): Promise<{
    login: AuthFlowTestResult;
    tokenUse: TokenTestResult;
    tokenRefresh: TokenTestResult;
    tokenExpiration: TokenTestResult;
  }> {
    const results = {
      login: { success: false, duration: 0 } as AuthFlowTestResult,
      tokenUse: { isValid: false, isExpired: false } as TokenTestResult,
      tokenRefresh: { isValid: false, isExpired: false } as TokenTestResult,
      tokenExpiration: { isValid: false, isExpired: false } as TokenTestResult
    };

    try {
      // 1. Login and get tokens
      const loginStart = Date.now();
      const authResponse = await this.loginTestUser(testUser);
      results.login = {
        success: true,
        user: authResponse.user,
        tokens: authResponse.tokens,
        duration: Date.now() - loginStart
      };

      // 2. Test token usage
      results.tokenUse = await this.testTokenExpiration();

      // 3. Test token refresh
      try {
        await this.refreshTokens();
        results.tokenRefresh = { isValid: true, isExpired: false };
      } catch (error) {
        results.tokenRefresh = {
          isValid: false,
          isExpired: false,
          error: error instanceof Error ? error.message : 'Unknown error'
        };
      }

      // 4. Test token expiration (would need to wait or manipulate time)
      results.tokenExpiration = await this.testTokenExpiration();

    } catch (error) {
      results.login.error = error instanceof Error ? error.message : 'Unknown error';
    }

    return results;
  }

  /**
   * Logout and invalidate tokens
   */
  async logout(): Promise<LogoutResponse> {
    try {
      const response = await this.typedClient.logout();
      return response;
    } catch (error) {
      throw new Error(`Logout failed: ${error instanceof Error ? error.message : 'Unknown error'}`);
    }
  }

  // ============================================================================
  // Role-Based Access Control (RBAC) Testing
  // ============================================================================

  /**
   * Create test user with specific roles
   */
  async createUserWithRoles(roles: string[], userData?: Partial<AuthTestUser>): Promise<AuthTestUser> {
    return this.registerUser({
      ...userData,
      roles
    });
  }

  /**
   * Test role-based access to protected endpoints
   */
  async testRoleBasedAccess(testUser: AuthTestUser, protectedEndpoints: string[]): Promise<{
    user: AuthTestUser;
    accessResults: Array<{
      endpoint: string;
      hasAccess: boolean;
      statusCode?: number;
      error?: string;
    }>;
  }> {
    // Login as the test user
    await this.loginTestUser(testUser);

    const accessResults = [];

    for (const endpoint of protectedEndpoints) {
      try {
        const response = await this.apiClient.request({
          method: 'GET',
          url: endpoint
        });

        accessResults.push({
          endpoint,
          hasAccess: response.status >= 200 && response.status < 300,
          statusCode: response.status
        });
      } catch (error: any) {
        accessResults.push({
          endpoint,
          hasAccess: false,
          statusCode: error.status || 0,
          error: error.message || 'Unknown error'
        });
      }
    }

    return {
      user: testUser,
      accessResults
    };
  }

  /**
   * Test admin-only endpoints
   */
  async testAdminAccess(adminUser: AuthTestUser, regularUser: AuthTestUser): Promise<{
    adminResults: Array<{ endpoint: string; hasAccess: boolean; statusCode?: number }>;
    userResults: Array<{ endpoint: string; hasAccess: boolean; statusCode?: number }>;
  }> {
    const adminEndpoints = [
      '/api/v1/users',
      '/api/v1/users/1',
      '/api/v1/tenants'
    ];

    const adminResults = (await this.testRoleBasedAccess(adminUser, adminEndpoints)).accessResults;
    const userResults = (await this.testRoleBasedAccess(regularUser, adminEndpoints)).accessResults;

    return { adminResults, userResults };
  }

  // ============================================================================
  // Authentication Flow Testing
  // ============================================================================

  /**
   * Test complete authentication flow
   */
  async testCompleteAuthFlow(userData?: Partial<AuthTestUser>): Promise<{
    registration: { success: boolean; user?: AuthTestUser; error?: string };
    emailVerification: { success: boolean; verified: boolean; error?: string };
    login: AuthFlowTestResult;
    tokenRefresh: { success: boolean; error?: string };
    logout: { success: boolean; error?: string };
  }> {
    const results = {
      registration: { success: false },
      emailVerification: { success: false, verified: false },
      login: { success: false, duration: 0 } as AuthFlowTestResult,
      tokenRefresh: { success: false },
      logout: { success: false }
    };

    try {
      // 1. Register user
      const testUser = await this.registerUser(userData);
      results.registration = { success: true, user: testUser };

      // 2. Test email verification flow
      const emailFlow = await this.testEmailVerificationFlow(testUser);
      results.emailVerification = {
        success: !emailFlow.error,
        verified: emailFlow.verificationStatus.verified,
        error: emailFlow.error
      };

      // 3. Login
      const loginStart = Date.now();
      const authResponse = await this.loginTestUser(testUser);
      results.login = {
        success: true,
        user: authResponse.user,
        tokens: authResponse.tokens,
        duration: Date.now() - loginStart
      };

      // 4. Test token refresh
      try {
        await this.refreshTokens();
        results.tokenRefresh = { success: true };
      } catch (error) {
        results.tokenRefresh = {
          success: false,
          error: error instanceof Error ? error.message : 'Unknown error'
        };
      }

      // 5. Logout
      try {
        await this.logout();
        results.logout = { success: true };
      } catch (error) {
        results.logout = {
          success: false,
          error: error instanceof Error ? error.message : 'Unknown error'
        };
      }

    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Unknown error';
      
      if (!results.registration.success) {
        results.registration.error = errorMessage;
      } else if (!results.login.success) {
        results.login.error = errorMessage;
      }
    }

    return results;
  }

  // ============================================================================
  // Utility Methods
  // ============================================================================

  /**
   * Get current authentication status
   */
  isAuthenticated(): boolean {
    return this.typedClient.isAuthenticated();
  }

  /**
   * Get current user context
   */
  async getCurrentUser(): Promise<UserResponse | null> {
    try {
      return await this.typedClient.getCurrentUser();
    } catch {
      return null;
    }
  }

  /**
   * Clear authentication
   */
  clearAuth(): void {
    this.typedClient.clearAuth();
  }

  /**
   * Set tenant for multi-tenant testing
   */
  setTenant(tenantId: string): void {
    this.typedClient.setTenant(tenantId);
  }

  /**
   * Get API client for direct access
   */
  getApiClient(): ApiClient {
    return this.apiClient;
  }

  /**
   * Get typed API client
   */
  getTypedClient(): TypedApiClient {
    return this.typedClient;
  }

  /**
   * Clean up created test users
   */
  async cleanup(): Promise<void> {
    // Note: In a real implementation, you might want to delete created users
    // For now, we just clear the tracking array
    this.createdUsers.length = 0;
    this.clearAuth();
  }

  /**
   * Get all created test users
   */
  getCreatedUsers(): AuthTestUser[] {
    return [...this.createdUsers];
  }

  /**
   * Dispose of the framework
   */
  async dispose(): Promise<void> {
    await this.cleanup();
    await this.typedClient.dispose();
  }
}