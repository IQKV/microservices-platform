/**
 * Authentication Flow Tests
 * Tests user registration, email verification, login, token refresh, and logout workflows
 */

import { test, expect } from '@playwright/test';
import { 
  AuthTestFramework, 
  JwtTestUtils, 
  RbacTestHelpers,
  ApiClient,
  createApiClient
} from '../../utils/index.js';
import { getEnvironmentConfig } from '../../config/environments.js';
import type { 
  LoginCredentials,
  AuthResponse,
  TokenRefreshResponse 
} from '../../types/api-responses.js';
import type { AuthTestUser } from '../../utils/index.js';

test.describe('Authentication Flow Tests', () => {
  let authFramework: AuthTestFramework;
  let apiClient: ApiClient;
  const config = getEnvironmentConfig();

  test.beforeEach(async () => {
    apiClient = createApiClient(config);
    authFramework = new AuthTestFramework(apiClient);
  });

  test.afterEach(async () => {
    await authFramework.cleanup();
    await apiClient.dispose();
  });

  test.describe('User Registration Flow', () => {
    test('should register new user successfully', async () => {
      // Generate test user data
      const testUser = authFramework.generateTestUser({
        username: 'newuser_registration',
        email: 'newuser_registration@example.com'
      });

      // Register the user
      const registeredUser = await authFramework.registerUser(testUser);

      // Verify registration response
      expect(registeredUser.id).toBeDefined();
      expect(registeredUser.username).toBe(testUser.username);
      expect(registeredUser.email).toBe(testUser.email);
      expect(registeredUser.emailVerified).toBe(false); // Should require verification
      expect(registeredUser.enabled).toBe(true);
    });

    test('should reject registration with duplicate username', async () => {
      // Register first user
      const firstUser = await authFramework.registerUser({
        username: 'duplicate_test',
        email: 'first@example.com'
      });

      // Attempt to register second user with same username
      await expect(async () => {
        await authFramework.registerUser({
          username: 'duplicate_test', // Same username
          email: 'second@example.com'  // Different email
        });
      }).rejects.toThrow();
    });

    test('should reject registration with duplicate email', async () => {
      // Register first user
      const firstUser = await authFramework.registerUser({
        username: 'first_user',
        email: 'duplicate@example.com'
      });

      // Attempt to register second user with same email
      await expect(async () => {
        await authFramework.registerUser({
          username: 'second_user',     // Different username
          email: 'duplicate@example.com' // Same email
        });
      }).rejects.toThrow();
    });

    test('should reject registration with invalid email format', async () => {
      await expect(async () => {
        await authFramework.registerUser({
          username: 'testuser',
          email: 'invalid-email-format' // Invalid email
        });
      }).rejects.toThrow();
    });

    test('should reject registration with weak password', async () => {
      await expect(async () => {
        await authFramework.registerUser({
          username: 'testuser',
          email: 'test@example.com',
          password: '123' // Weak password
        });
      }).rejects.toThrow();
    });
  });

  test.describe('Email Verification Flow', () => {
    test('should require email verification for new users', async () => {
      // Register new user
      const testUser = await authFramework.registerUser({
        username: 'verification_test',
        email: 'verification_test@example.com'
      });

      // Check initial verification status
      const status = await authFramework.getEmailVerificationStatus(testUser.email);
      expect(status.verified).toBe(false);
    });

    test('should allow resending verification email', async () => {
      // Register new user
      const testUser = await authFramework.registerUser({
        username: 'resend_test',
        email: 'resend_test@example.com'
      });

      // Resend verification email
      const resendResult = await authFramework.resendEmailVerification(testUser.email);
      expect(resendResult.message).toContain('verification');
    });

    test('should handle verification email rate limiting', async () => {
      // Register new user
      const testUser = await authFramework.registerUser({
        username: 'rate_limit_test',
        email: 'rate_limit_test@example.com'
      });

      // Send multiple verification requests rapidly
      await authFramework.resendEmailVerification(testUser.email);
      await authFramework.resendEmailVerification(testUser.email);
      
      // Third request should be rate limited
      await expect(async () => {
        await authFramework.resendEmailVerification(testUser.email);
      }).rejects.toThrow(/rate limit|too many requests/i);
    });

    test('should complete email verification flow', async () => {
      // Register new user
      const testUser = await authFramework.registerUser({
        username: 'complete_verification',
        email: 'complete_verification@example.com'
      });

      // Test complete verification flow
      const verificationFlow = await authFramework.testEmailVerificationFlow(testUser);
      
      expect(verificationFlow.registrationSuccess).toBe(true);
      expect(verificationFlow.verificationSent).toBe(true);
      expect(verificationFlow.verificationStatus).toBeDefined();
    });
  });

  test.describe('Login Flow', () => {
    let testUser: AuthTestUser;

    test.beforeEach(async () => {
      // Create a verified user for login tests
      testUser = await authFramework.registerUser({
        username: 'login_test_user',
        email: 'login_test@example.com',
        emailVerified: true
      });
    });

    test('should login with valid credentials', async () => {
      const credentials: LoginCredentials = {
        username: testUser.username,
        password: testUser.password,
        ...(testUser.tenantId && { tenantId: testUser.tenantId })
      };

      const authResponse = await authFramework.loginUser(credentials);

      // Verify login response
      expect(authResponse.user).toBeDefined();
      expect(authResponse.user.username).toBe(testUser.username);
      expect(authResponse.user.email).toBe(testUser.email);
      expect(authResponse.tokens).toBeDefined();
      expect(authResponse.tokens.accessToken).toBeDefined();
      expect(authResponse.tokens.refreshToken).toBeDefined();
      expect(authResponse.tokens.tokenType).toBe('Bearer');
      expect(authResponse.tokens.expiresIn).toBeGreaterThan(0);
    });

    test('should reject login with invalid username', async () => {
      const credentials: LoginCredentials = {
        username: 'nonexistent_user',
        password: testUser.password
      };

      await expect(async () => {
        await authFramework.loginUser(credentials);
      }).rejects.toThrow(/invalid credentials|unauthorized/i);
    });

    test('should reject login with invalid password', async () => {
      const credentials: LoginCredentials = {
        username: testUser.username,
        password: 'wrong_password'
      };

      await expect(async () => {
        await authFramework.loginUser(credentials);
      }).rejects.toThrow(/invalid credentials|unauthorized/i);
    });

    test('should enforce email verification requirement', async () => {
      // Create unverified user
      const unverifiedUser = await authFramework.registerUser({
        username: 'unverified_login',
        email: 'unverified_login@example.com',
        emailVerified: false
      });

      const credentials: LoginCredentials = {
        username: unverifiedUser.username,
        password: unverifiedUser.password
      };

      // Login should fail for unverified user
      await expect(async () => {
        await authFramework.loginUser(credentials);
      }).rejects.toThrow(/email.*verification|verify.*email/i);
    });

    test('should include user context in JWT tokens', async () => {
      const authResponse = await authFramework.loginTestUser(testUser);
      
      // Validate JWT token structure
      const tokenValidation = JwtTestUtils.validateToken(authResponse.tokens.accessToken);
      expect(tokenValidation.isValid).toBe(true);
      expect(tokenValidation.isExpired).toBe(false);
      expect(tokenValidation.payload).toBeDefined();

      // Verify user context in token
      const userContext = JwtTestUtils.extractUserContext(authResponse.tokens.accessToken);
      expect(userContext).toBeDefined();
      expect(userContext!.username).toBe(testUser.username);
      expect(userContext!.email).toBe(testUser.email);
      expect(userContext!.roles).toContain('USER');
    });
  });

  test.describe('JWT Token Lifecycle', () => {
    let testUser: AuthTestUser;
    let authResponse: AuthResponse;

    test.beforeEach(async () => {
      testUser = await authFramework.registerUser({
        username: 'token_test_user',
        email: 'token_test@example.com',
        emailVerified: true
      });
      authResponse = await authFramework.loginTestUser(testUser);
    });

    test('should generate valid JWT tokens on login', async () => {
      const { accessToken, refreshToken } = authResponse.tokens;

      // Validate access token
      const accessValidation = JwtTestUtils.validateToken(accessToken);
      expect(accessValidation.isValid).toBe(true);
      expect(accessValidation.isExpired).toBe(false);
      expect(accessValidation.payload).toBeDefined();

      // Validate refresh token
      const refreshValidation = JwtTestUtils.validateToken(refreshToken);
      expect(refreshValidation.isValid).toBe(true);
      expect(refreshValidation.isExpired).toBe(false);
      expect(refreshValidation.expiresIn).toBeGreaterThan(accessValidation.expiresIn!);
    });

    test('should refresh tokens successfully', async () => {
      const originalTokens = authResponse.tokens;
      
      // Wait a moment to ensure new tokens have different timestamps
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      // Refresh tokens
      const refreshResponse = await authFramework.refreshTokens();

      // Verify new tokens are different
      expect(refreshResponse.accessToken).not.toBe(originalTokens.accessToken);
      expect(refreshResponse.refreshToken).not.toBe(originalTokens.refreshToken);

      // Verify new tokens are valid
      const newAccessValidation = JwtTestUtils.validateToken(refreshResponse.accessToken);
      expect(newAccessValidation.isValid).toBe(true);
      expect(newAccessValidation.isExpired).toBe(false);

      // Compare token changes
      const comparison = JwtTestUtils.compareTokens(originalTokens, {
        accessToken: refreshResponse.accessToken,
        refreshToken: refreshResponse.refreshToken,
        tokenType: refreshResponse.tokenType,
        expiresIn: refreshResponse.expiresIn,
        refreshExpiresIn: refreshResponse.refreshExpiresIn
      });

      expect(comparison.accessTokenChanged).toBe(true);
      expect(comparison.refreshTokenChanged).toBe(true);
      expect(comparison.expirationExtended).toBe(true);
    });

    test('should handle token expiration gracefully', async () => {
      // Test token expiration detection
      const tokenTest = await authFramework.testTokenExpiration();
      
      // With fresh tokens, they should be valid
      expect(tokenTest.isValid).toBe(true);
      expect(tokenTest.isExpired).toBe(false);
    });

    test('should complete full token lifecycle', async () => {
      const lifecycle = await authFramework.testTokenLifecycle(testUser);

      // Verify all lifecycle stages
      expect(lifecycle.login.success).toBe(true);
      expect(lifecycle.login.tokens).toBeDefined();
      expect(lifecycle.tokenUse.isValid).toBe(true);
      expect(lifecycle.tokenRefresh.isValid).toBe(true);
    });

    test('should reject requests with invalid tokens', async () => {
      // Set invalid token
      apiClient.setAuthTokens({
        accessToken: 'invalid.token.here',
        refreshToken: 'invalid.refresh.token',
        tokenType: 'Bearer',
        expiresIn: 3600,
        refreshExpiresIn: 86400
      });

      // Request should fail
      await expect(async () => {
        await authFramework.getCurrentUser();
      }).rejects.toThrow(/unauthorized|invalid.*token/i);
    });

    test('should reject requests with expired tokens', async () => {
      // Generate expired token
      const expiredToken = JwtTestUtils.generateExpiredMockToken({
        sub: testUser.id?.toString() || '1',
        username: testUser.username,
        email: testUser.email
      });

      apiClient.setAuthTokens({
        accessToken: expiredToken,
        refreshToken: expiredToken,
        tokenType: 'Bearer',
        expiresIn: 0,
        refreshExpiresIn: 0
      });

      // Request should fail due to expired token
      await expect(async () => {
        await authFramework.getCurrentUser();
      }).rejects.toThrow(/unauthorized|expired|invalid.*token/i);
    });
  });

  test.describe('Logout Flow', () => {
    let testUser: AuthTestUser;

    test.beforeEach(async () => {
      testUser = await authFramework.registerUser({
        username: 'logout_test_user',
        email: 'logout_test@example.com',
        emailVerified: true
      });
      await authFramework.loginTestUser(testUser);
    });

    test('should logout successfully', async () => {
      // Verify user is authenticated
      expect(authFramework.isAuthenticated()).toBe(true);
      
      // Logout
      const logoutResponse = await authFramework.logout();
      expect(logoutResponse.success).toBe(true);
      
      // Verify user is no longer authenticated
      expect(authFramework.isAuthenticated()).toBe(false);
    });

    test('should invalidate tokens after logout', async () => {
      // Get tokens before logout
      const tokensBeforeLogout = apiClient.getAuthTokens();
      expect(tokensBeforeLogout).toBeDefined();
      
      // Logout
      await authFramework.logout();
      
      // Verify tokens are cleared
      const tokensAfterLogout = apiClient.getAuthTokens();
      expect(tokensAfterLogout).toBeNull();
    });

    test('should reject authenticated requests after logout', async () => {
      // Verify authenticated request works before logout
      const userBefore = await authFramework.getCurrentUser();
      expect(userBefore).toBeDefined();
      
      // Logout
      await authFramework.logout();
      
      // Authenticated request should fail after logout
      const userAfter = await authFramework.getCurrentUser();
      expect(userAfter).toBeNull();
    });

    test('should handle logout when not authenticated', async () => {
      // Clear authentication first
      authFramework.clearAuth();
      
      // Logout should still work (or fail gracefully)
      await expect(async () => {
        await authFramework.logout();
      }).rejects.toThrow(/unauthorized|not.*authenticated/i);
    });
  });

  test.describe('Complete Authentication Flow Integration', () => {
    test('should complete full authentication workflow', async () => {
      const completeFlow = await authFramework.testCompleteAuthFlow({
        username: 'complete_flow_test',
        email: 'complete_flow_test@example.com'
      });

      // Verify all flow stages
      expect(completeFlow.registration.success).toBe(true);
      expect(completeFlow.registration.user).toBeDefined();
      
      expect(completeFlow.emailVerification.success).toBe(true);
      
      expect(completeFlow.login.success).toBe(true);
      expect(completeFlow.login.user).toBeDefined();
      expect(completeFlow.login.tokens).toBeDefined();
      
      expect(completeFlow.tokenRefresh.success).toBe(true);
      
      expect(completeFlow.logout.success).toBe(true);
    });

    test('should maintain security throughout authentication flow', async () => {
      // Register and login user
      const testUser = await authFramework.registerUser({
        username: 'security_test',
        email: 'security_test@example.com',
        emailVerified: true
      });
      
      const authResponse = await authFramework.loginTestUser(testUser);
      
      // Verify token security properties
      const tokenClaims = JwtTestUtils.getTokenClaims(authResponse.tokens.accessToken);
      expect(tokenClaims).toBeDefined();
      expect(tokenClaims!.issuer).toBe('gripday-user-service');
      expect(tokenClaims!.subject).toBeDefined();
      expect(tokenClaims!.expiresAt).toBeDefined();
      
      // Verify user context propagation
      const userContext = JwtTestUtils.extractUserContext(authResponse.tokens.accessToken);
      expect(userContext).toBeDefined();
      expect(userContext!.userId).toBe(testUser.id);
      expect(userContext!.username).toBe(testUser.username);
      expect(userContext!.email).toBe(testUser.email);
    });
  });
});