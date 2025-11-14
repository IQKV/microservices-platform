/**
 * JWT Token Lifecycle Tests
 * Tests JWT token creation, validation, refresh, and expiration handling
 */

import { test, expect } from '@playwright/test';
import { 
  AuthTestFramework, 
  JwtTestUtils,
  ApiClient,
  createApiClient
} from '../../utils/index.js';
import { getEnvironmentConfig } from '../../config/environments.js';
import type { 
  AuthTestUser, 
  AuthTokens,
  TokenValidationResult 
} from '../../utils/index.js';

test.describe('JWT Token Lifecycle Tests', () => {
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

  test.describe('Token Creation and Structure', () => {
    let testUser: AuthTestUser;
    let authTokens: AuthTokens;

    test.beforeEach(async () => {
      testUser = await authFramework.registerUser({
        username: 'token_structure_test',
        email: 'token_structure_test@example.com',
        emailVerified: true
      });
      
      const authResponse = await authFramework.loginTestUser(testUser);
      authTokens = authResponse.tokens;
    });

    test('should create properly formatted JWT tokens', async () => {
      // Validate token format
      expect(JwtTestUtils.isValidTokenFormat(authTokens.accessToken)).toBe(true);
      expect(JwtTestUtils.isValidTokenFormat(authTokens.refreshToken)).toBe(true);
      
      // Validate token structure
      expect(JwtTestUtils.validateTokensStructure(authTokens)).toBe(true);
    });

    test('should include required claims in access token', async () => {
      const tokenClaims = JwtTestUtils.getTokenClaims(authTokens.accessToken);
      
      expect(tokenClaims).toBeDefined();
      expect(tokenClaims!.subject).toBeDefined();
      expect(tokenClaims!.username).toBe(testUser.username);
      expect(tokenClaims!.email).toBe(testUser.email);
      expect(tokenClaims!.roles).toContain('USER');
      expect(tokenClaims!.issuedAt).toBeDefined();
      expect(tokenClaims!.expiresAt).toBeDefined();
      expect(tokenClaims!.issuer).toBe('gripday-user-service');
    });

    test('should have different expiration times for access and refresh tokens', async () => {
      const accessValidation = JwtTestUtils.validateToken(authTokens.accessToken);
      const refreshValidation = JwtTestUtils.validateToken(authTokens.refreshToken);
      
      expect(accessValidation.isValid).toBe(true);
      expect(refreshValidation.isValid).toBe(true);
      
      // Refresh token should expire later than access token
      expect(refreshValidation.expiresIn!).toBeGreaterThan(accessValidation.expiresIn!);
    });

    test('should include tenant information when applicable', async () => {
      const tenantUser = await authFramework.registerUser({
        username: 'tenant_token_test',
        email: 'tenant_token_test@example.com',
        tenantId: 'test_tenant',
        emailVerified: true
      });

      const tenantAuth = await authFramework.loginTestUser(tenantUser);
      
      // Verify tenant ID in token
      expect(JwtTestUtils.belongsToTenant(tenantAuth.tokens.accessToken, 'test_tenant')).toBe(true);
      
      const userContext = JwtTestUtils.extractUserContext(tenantAuth.tokens.accessToken);
      expect(userContext?.tenantId).toBe('test_tenant');
    });
  });

  test.describe('Token Validation', () => {
    let validTokens: AuthTokens;

    test.beforeEach(async () => {
      const testUser = await authFramework.registerUser({
        username: 'token_validation_test',
        email: 'token_validation_test@example.com',
        emailVerified: true
      });
      
      const authResponse = await authFramework.loginTestUser(testUser);
      validTokens = authResponse.tokens;
    });

    test('should validate fresh tokens as valid', async () => {
      const accessValidation = JwtTestUtils.validateToken(validTokens.accessToken);
      const refreshValidation = JwtTestUtils.validateToken(validTokens.refreshToken);
      
      expect(accessValidation.isValid).toBe(true);
      expect(accessValidation.isExpired).toBe(false);
      expect(accessValidation.expiresIn).toBeGreaterThan(0);
      
      expect(refreshValidation.isValid).toBe(true);
      expect(refreshValidation.isExpired).toBe(false);
      expect(refreshValidation.expiresIn).toBeGreaterThan(0);
    });

    test('should detect invalid token format', async () => {
      const invalidTokens = [
        'invalid.token',
        'not.a.jwt.token',
        'invalid',
        '',
        'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalid.signature'
      ];

      invalidTokens.forEach(token => {
        const validation = JwtTestUtils.validateToken(token);
        expect(validation.isValid).toBe(false);
        expect(validation.error).toBeDefined();
      });
    });

    test('should detect expired tokens', async () => {
      const expiredToken = JwtTestUtils.generateExpiredMockToken({
        sub: '1',
        username: 'testuser',
        email: 'test@example.com'
      });

      const validation = JwtTestUtils.validateToken(expiredToken);
      expect(validation.isValid).toBe(true); // Structure is valid
      expect(validation.isExpired).toBe(true); // But token is expired
      expect(validation.expiresIn).toBe(0);
    });

    test('should calculate time until expiration correctly', async () => {
      const timeUntilExpiration = JwtTestUtils.getTimeUntilExpiration(validTokens.accessToken);
      
      expect(timeUntilExpiration).toBeGreaterThan(0);
      expect(timeUntilExpiration).toBeLessThanOrEqual(validTokens.expiresIn);
    });

    test('should detect tokens expiring soon', async () => {
      // Create token that expires in 30 seconds
      const soonToExpireToken = JwtTestUtils.generateMockToken({
        sub: '1',
        username: 'testuser',
        email: 'test@example.com',
        exp: Math.floor(Date.now() / 1000) + 30 // 30 seconds from now
      });

      expect(JwtTestUtils.willExpireSoon(soonToExpireToken, 60)).toBe(true); // Within 60 seconds
      expect(JwtTestUtils.willExpireSoon(soonToExpireToken, 10)).toBe(false); // Not within 10 seconds
    });
  });

  test.describe('Token Refresh', () => {
    let testUser: AuthTestUser;
    let originalTokens: AuthTokens;

    test.beforeEach(async () => {
      testUser = await authFramework.registerUser({
        username: 'token_refresh_test',
        email: 'token_refresh_test@example.com',
        emailVerified: true
      });
      
      const authResponse = await authFramework.loginTestUser(testUser);
      originalTokens = authResponse.tokens;
    });

    test('should refresh tokens successfully', async () => {
      // Wait a moment to ensure different timestamps
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      const refreshResponse = await authFramework.refreshTokens();
      
      // Verify new tokens are different
      expect(refreshResponse.accessToken).not.toBe(originalTokens.accessToken);
      expect(refreshResponse.refreshToken).not.toBe(originalTokens.refreshToken);
      
      // Verify new tokens are valid
      const newAccessValidation = JwtTestUtils.validateToken(refreshResponse.accessToken);
      expect(newAccessValidation.isValid).toBe(true);
      expect(newAccessValidation.isExpired).toBe(false);
    });

    test('should maintain user context in refreshed tokens', async () => {
      const refreshResponse = await authFramework.refreshTokens();
      
      const originalContext = JwtTestUtils.extractUserContext(originalTokens.accessToken);
      const refreshedContext = JwtTestUtils.extractUserContext(refreshResponse.accessToken);
      
      expect(refreshedContext).toBeDefined();
      expect(refreshedContext!.userId).toBe(originalContext!.userId);
      expect(refreshedContext!.username).toBe(originalContext!.username);
      expect(refreshedContext!.email).toBe(originalContext!.email);
      expect(refreshedContext!.roles).toEqual(originalContext!.roles);
    });

    test('should extend token expiration on refresh', async () => {
      const refreshResponse = await authFramework.refreshTokens();
      
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

    test('should reject refresh with invalid refresh token', async () => {
      // Set invalid refresh token
      apiClient.setAuthTokens({
        ...originalTokens,
        refreshToken: 'invalid.refresh.token'
      });

      await expect(async () => {
        await authFramework.refreshTokens();
      }).rejects.toThrow(/invalid.*token|unauthorized/i);
    });

    test('should reject refresh with expired refresh token', async () => {
      const expiredRefreshToken = JwtTestUtils.generateExpiredMockToken({
        sub: testUser.id?.toString() || '1',
        username: testUser.username,
        email: testUser.email
      });

      apiClient.setAuthTokens({
        ...originalTokens,
        refreshToken: expiredRefreshToken
      });

      await expect(async () => {
        await authFramework.refreshTokens();
      }).rejects.toThrow(/expired|invalid.*token|unauthorized/i);
    });
  });

  test.describe('Token Expiration Handling', () => {
    let testUser: AuthTestUser;

    test.beforeEach(async () => {
      testUser = await authFramework.registerUser({
        username: 'token_expiration_test',
        email: 'token_expiration_test@example.com',
        emailVerified: true
      });
    });

    test('should detect token expiration in requests', async () => {
      // Login to get valid tokens
      await authFramework.loginTestUser(testUser);
      
      // Set expired token
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

      // Request should fail with expired token
      await expect(async () => {
        await authFramework.getCurrentUser();
      }).rejects.toThrow(/unauthorized|expired|invalid.*token/i);
    });

    test('should handle token expiration gracefully', async () => {
      await authFramework.loginTestUser(testUser);
      
      const tokenTest = await authFramework.testTokenExpiration();
      
      // With fresh tokens, should be valid
      expect(tokenTest.isValid).toBe(true);
      expect(tokenTest.isExpired).toBe(false);
    });

    test('should complete token lifecycle test', async () => {
      const lifecycle = await authFramework.testTokenLifecycle(testUser);
      
      expect(lifecycle.login.success).toBe(true);
      expect(lifecycle.login.tokens).toBeDefined();
      expect(lifecycle.tokenUse.isValid).toBe(true);
      expect(lifecycle.tokenRefresh.isValid).toBe(true);
    });
  });

  test.describe('Token Security', () => {
    let testUser: AuthTestUser;
    let authTokens: AuthTokens;

    test.beforeEach(async () => {
      testUser = await authFramework.registerUser({
        username: 'token_security_test',
        email: 'token_security_test@example.com',
        emailVerified: true
      });
      
      const authResponse = await authFramework.loginTestUser(testUser);
      authTokens = authResponse.tokens;
    });

    test('should include security claims in tokens', async () => {
      const tokenClaims = JwtTestUtils.getTokenClaims(authTokens.accessToken);
      
      expect(tokenClaims).toBeDefined();
      expect(tokenClaims!.issuer).toBe('gripday-user-service');
      expect(tokenClaims!.audience).toBeDefined();
      expect(tokenClaims!.jwtId).toBeDefined();
      expect(tokenClaims!.issuedAt).toBeDefined();
      expect(tokenClaims!.expiresAt).toBeDefined();
    });

    test('should reject tokens with invalid signatures', async () => {
      // Create token with invalid signature
      const invalidToken = authTokens.accessToken.substring(0, authTokens.accessToken.lastIndexOf('.')) + '.invalid_signature';
      
      apiClient.setAuthTokens({
        ...authTokens,
        accessToken: invalidToken
      });

      // Request should fail with invalid signature
      await expect(async () => {
        await authFramework.getCurrentUser();
      }).rejects.toThrow(/unauthorized|invalid.*token/i);
    });

    test('should validate token audience and issuer', async () => {
      const payload = JwtTestUtils.decodeToken(authTokens.accessToken);
      
      expect(payload).toBeDefined();
      expect(payload!.iss).toBe('gripday-user-service');
      expect(payload!.aud).toBe('gripday');
    });

    test('should include unique JWT ID for each token', async () => {
      // Get another set of tokens
      const secondAuth = await authFramework.loginTestUser(testUser);
      
      const firstJwtId = JwtTestUtils.getTokenClaims(authTokens.accessToken)?.jwtId;
      const secondJwtId = JwtTestUtils.getTokenClaims(secondAuth.tokens.accessToken)?.jwtId;
      
      expect(firstJwtId).toBeDefined();
      expect(secondJwtId).toBeDefined();
      expect(firstJwtId).not.toBe(secondJwtId);
    });
  });

  test.describe('Token Invalidation', () => {
    let testUser: AuthTestUser;

    test.beforeEach(async () => {
      testUser = await authFramework.registerUser({
        username: 'token_invalidation_test',
        email: 'token_invalidation_test@example.com',
        emailVerified: true
      });
    });

    test('should invalidate tokens on logout', async () => {
      // Login and verify authentication
      await authFramework.loginTestUser(testUser);
      expect(authFramework.isAuthenticated()).toBe(true);
      
      // Get tokens before logout
      const tokensBeforeLogout = apiClient.getAuthTokens();
      expect(tokensBeforeLogout).toBeDefined();
      
      // Logout
      await authFramework.logout();
      
      // Verify tokens are cleared locally
      const tokensAfterLogout = apiClient.getAuthTokens();
      expect(tokensAfterLogout).toBeNull();
      expect(authFramework.isAuthenticated()).toBe(false);
    });

    test('should reject invalidated tokens', async () => {
      // Login and get tokens
      const authResponse = await authFramework.loginTestUser(testUser);
      const originalTokens = authResponse.tokens;
      
      // Logout to invalidate tokens
      await authFramework.logout();
      
      // Try to use invalidated tokens
      apiClient.setAuthTokens(originalTokens);
      
      // Request should fail with invalidated tokens
      await expect(async () => {
        await authFramework.getCurrentUser();
      }).rejects.toThrow(/unauthorized|invalid.*token/i);
    });

    test('should handle multiple logout attempts gracefully', async () => {
      // Login
      await authFramework.loginTestUser(testUser);
      
      // First logout should succeed
      const firstLogout = await authFramework.logout();
      expect(firstLogout.success).toBe(true);
      
      // Second logout should fail gracefully
      await expect(async () => {
        await authFramework.logout();
      }).rejects.toThrow(/unauthorized|not.*authenticated/i);
    });
  });
});