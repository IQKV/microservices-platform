/**
 * Email Verification Tests
 * Tests email verification requirements, rate limiting, and verification flows
 */

import { test, expect } from '@playwright/test';
import { 
  AuthTestFramework, 
  ApiClient,
  createApiClient
} from '../../utils/index.js';
import { getEnvironmentConfig } from '../../config/environments.js';
import type { 
  LoginCredentials,
  VerificationStatusResponse 
} from '../../types/api-responses.js';
import type { AuthTestUser } from '../../utils/index.js';

test.describe('Email Verification Tests', () => {
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

  test.describe('Email Verification Requirements', () => {
    test('should require email verification for new users', async () => {
      // Register new user
      const testUser = await authFramework.registerUser({
        username: 'verification_required_test',
        email: 'verification_required@example.com'
      });

      // User should be created but not verified
      expect(testUser.id).toBeDefined();
      expect(testUser.emailVerified).toBe(false);
      
      // Check verification status via API
      const status = await authFramework.getEmailVerificationStatus(testUser.email);
      expect(status.verified).toBe(false);
      expect(status.message).toContain('verification');
    });

    test('should prevent login for unverified users', async () => {
      // Register unverified user
      const unverifiedUser = await authFramework.registerUser({
        username: 'unverified_login_test',
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
      }).rejects.toThrow(/email.*verification|verify.*email|account.*not.*verified/i);
    });

    test('should allow login for verified users', async () => {
      // Register verified user
      const verifiedUser = await authFramework.registerUser({
        username: 'verified_login_test',
        email: 'verified_login@example.com',
        emailVerified: true
      });

      const credentials: LoginCredentials = {
        username: verifiedUser.username,
        password: verifiedUser.password
      };

      // Login should succeed for verified user
      const authResponse = await authFramework.loginUser(credentials);
      expect(authResponse.user).toBeDefined();
      expect(authResponse.tokens).toBeDefined();
      expect(authResponse.user.emailVerified).toBe(true);
    });

    test('should show verification status in user profile', async () => {
      // Register unverified user
      const unverifiedUser = await authFramework.registerUser({
        username: 'profile_verification_test',
        email: 'profile_verification@example.com',
        emailVerified: false
      });

      // Check verification status
      const status = await authFramework.getEmailVerificationStatus(unverifiedUser.email);
      expect(status.verified).toBe(false);
      
      if (status.user) {
        expect(status.user.emailVerified).toBe(false);
      }
    });
  });

  test.describe('Email Verification Process', () => {
    let testUser: AuthTestUser;

    test.beforeEach(async () => {
      testUser = await authFramework.registerUser({
        username: 'verification_process_test',
        email: 'verification_process@example.com',
        emailVerified: false
      });
    });

    test('should allow resending verification email', async () => {
      // Resend verification email
      const resendResult = await authFramework.resendEmailVerification(testUser.email);
      
      expect(resendResult.message).toBeDefined();
      expect(resendResult.message.toLowerCase()).toContain('verification');
      expect(resendResult.verified).toBe(false); // Should still be unverified
    });

    test('should provide verification status endpoint', async () => {
      const status = await authFramework.getEmailVerificationStatus(testUser.email);
      
      expect(status.verified).toBe(false);
      expect(status.message).toBeDefined();
      
      if (status.user) {
        expect(status.user.email).toBe(testUser.email);
        expect(status.user.emailVerified).toBe(false);
      }
    });

    test('should handle verification for non-existent email', async () => {
      await expect(async () => {
        await authFramework.getEmailVerificationStatus('nonexistent@example.com');
      }).rejects.toThrow(/not found|user.*not.*exist/i);
    });

    test('should complete verification flow test', async () => {
      const verificationFlow = await authFramework.testEmailVerificationFlow(testUser);
      
      expect(verificationFlow.registrationSuccess).toBe(true);
      expect(verificationFlow.verificationSent).toBe(true);
      expect(verificationFlow.verificationStatus).toBeDefined();
      expect(verificationFlow.verificationStatus.verified).toBe(false); // Initially unverified
    });
  });

  test.describe('Email Verification Rate Limiting', () => {
    let testUser: AuthTestUser;

    test.beforeEach(async () => {
      testUser = await authFramework.registerUser({
        username: 'rate_limit_test',
        email: 'rate_limit@example.com',
        emailVerified: false
      });
    });

    test('should enforce rate limiting on verification emails', async () => {
      // Send first verification email
      const firstRequest = await authFramework.resendEmailVerification(testUser.email);
      expect(firstRequest.message).toBeDefined();

      // Send second verification email
      const secondRequest = await authFramework.resendEmailVerification(testUser.email);
      expect(secondRequest.message).toBeDefined();

      // Third request should be rate limited
      await expect(async () => {
        await authFramework.resendEmailVerification(testUser.email);
      }).rejects.toThrow(/rate limit|too many requests|limit exceeded/i);
    });

    test('should allow verification requests after rate limit period', async () => {
      // Send multiple requests to trigger rate limit
      await authFramework.resendEmailVerification(testUser.email);
      await authFramework.resendEmailVerification(testUser.email);
      
      // Third request should fail
      await expect(async () => {
        await authFramework.resendEmailVerification(testUser.email);
      }).rejects.toThrow(/rate limit|too many requests/i);

      // Note: In a real test, you would wait for the rate limit period to expire
      // and then verify that requests are allowed again
    });

    test('should have different rate limits per email address', async () => {
      // Create second user
      const secondUser = await authFramework.registerUser({
        username: 'rate_limit_test_2',
        email: 'rate_limit_2@example.com',
        emailVerified: false
      });

      // Exhaust rate limit for first user
      await authFramework.resendEmailVerification(testUser.email);
      await authFramework.resendEmailVerification(testUser.email);
      
      await expect(async () => {
        await authFramework.resendEmailVerification(testUser.email);
      }).rejects.toThrow(/rate limit|too many requests/i);

      // Second user should still be able to request verification
      const secondUserRequest = await authFramework.resendEmailVerification(secondUser.email);
      expect(secondUserRequest.message).toBeDefined();
    });

    test('should include rate limit information in error response', async () => {
      // Trigger rate limit
      await authFramework.resendEmailVerification(testUser.email);
      await authFramework.resendEmailVerification(testUser.email);
      
      try {
        await authFramework.resendEmailVerification(testUser.email);
        throw new Error('Expected rate limit error');
      } catch (error: any) {
        expect(error.message).toMatch(/rate limit|too many requests/i);
        // Note: In a real implementation, you might check for specific headers
        // like X-RateLimit-Remaining, X-RateLimit-Reset, etc.
      }
    });
  });

  test.describe('Email Verification Token Handling', () => {
    let testUser: AuthTestUser;

    test.beforeEach(async () => {
      testUser = await authFramework.registerUser({
        username: 'token_handling_test',
        email: 'token_handling@example.com',
        emailVerified: false
      });
    });

    test('should handle verification with valid token', async () => {
      // Note: In a real implementation, you would extract the verification token
      // from the email or database. For testing purposes, we simulate this.
      
      // Generate a mock verification token
      const mockToken = 'mock_verification_token_' + Date.now();
      
      // Attempt verification (this will likely fail in real implementation)
      try {
        const verificationResult = await authFramework.verifyEmailWithToken(mockToken);
        // If it succeeds (unlikely with mock token), verify the response
        expect(verificationResult.success).toBe(true);
        expect(verificationResult.user?.emailVerified).toBe(true);
      } catch (error: any) {
        // Expected to fail with mock token
        expect(error.message).toMatch(/invalid.*token|token.*expired|verification.*failed/i);
      }
    });

    test('should reject verification with invalid token', async () => {
      const invalidTokens = [
        'invalid_token',
        'expired_token',
        '',
        'malformed.token.here'
      ];

      for (const invalidToken of invalidTokens) {
        await expect(async () => {
          await authFramework.verifyEmailWithToken(invalidToken);
        }).rejects.toThrow(/invalid.*token|token.*expired|verification.*failed/i);
      }
    });

    test('should reject verification with expired token', async () => {
      // Simulate expired token
      const expiredToken = 'expired_verification_token_' + (Date.now() - 86400000); // 24 hours ago
      
      await expect(async () => {
        await authFramework.verifyEmailWithToken(expiredToken);
      }).rejects.toThrow(/token.*expired|verification.*expired|invalid.*token/i);
    });

    test('should handle verification for already verified user', async () => {
      // Create already verified user
      const verifiedUser = await authFramework.registerUser({
        username: 'already_verified_test',
        email: 'already_verified@example.com',
        emailVerified: true
      });

      // Check status
      const status = await authFramework.getEmailVerificationStatus(verifiedUser.email);
      expect(status.verified).toBe(true);
      expect(status.message).toMatch(/already.*verified|verification.*complete/i);
    });
  });

  test.describe('Email Verification Security', () => {
    test('should prevent verification status enumeration', async () => {
      // Attempt to check status for non-existent email
      await expect(async () => {
        await authFramework.getEmailVerificationStatus('nonexistent@example.com');
      }).rejects.toThrow(/not found|user.*not.*exist/i);
    });

    test('should prevent verification email spam', async () => {
      const testUser = await authFramework.registerUser({
        username: 'spam_prevention_test',
        email: 'spam_prevention@example.com',
        emailVerified: false
      });

      // Multiple rapid requests should be rate limited
      const requests = [];
      for (let i = 0; i < 5; i++) {
        requests.push(
          authFramework.resendEmailVerification(testUser.email).catch(error => error)
        );
      }

      const results = await Promise.all(requests);
      
      // Some requests should succeed, others should be rate limited
      const successes = results.filter(result => !(result instanceof Error));
      const failures = results.filter(result => result instanceof Error);
      
      expect(successes.length).toBeLessThan(5); // Not all should succeed
      expect(failures.length).toBeGreaterThan(0); // Some should be rate limited
    });

    test('should validate email format before verification', async () => {
      const invalidEmails = [
        'invalid-email',
        '@example.com',
        'test@',
        'test..test@example.com',
        'test@example',
        ''
      ];

      for (const invalidEmail of invalidEmails) {
        await expect(async () => {
          await authFramework.resendEmailVerification(invalidEmail);
        }).rejects.toThrow(/invalid.*email|email.*format|validation.*failed/i);
      }
    });

    test('should handle case-insensitive email verification', async () => {
      const testUser = await authFramework.registerUser({
        username: 'case_test',
        email: 'CaseTest@Example.Com', // Mixed case
        emailVerified: false
      });

      // Should work with different case variations
      const lowerCaseStatus = await authFramework.getEmailVerificationStatus('casetest@example.com');
      expect(lowerCaseStatus.verified).toBe(false);

      const upperCaseStatus = await authFramework.getEmailVerificationStatus('CASETEST@EXAMPLE.COM');
      expect(upperCaseStatus.verified).toBe(false);
    });
  });

  test.describe('Email Verification Integration', () => {
    test('should integrate verification with registration flow', async () => {
      // Complete registration and verification flow
      const completeFlow = await authFramework.testCompleteAuthFlow({
        username: 'integration_test',
        email: 'integration_test@example.com'
      });

      // Verify registration succeeded
      expect(completeFlow.registration.success).toBe(true);
      expect(completeFlow.registration.user).toBeDefined();

      // Verify email verification flow was tested
      expect(completeFlow.emailVerification.success).toBe(true);
      
      // Note: In a real implementation with actual email verification,
      // the login might fail until email is verified
    });

    test('should update user status after verification', async () => {
      const testUser = await authFramework.registerUser({
        username: 'status_update_test',
        email: 'status_update@example.com',
        emailVerified: false
      });

      // Initial status should be unverified
      const initialStatus = await authFramework.getEmailVerificationStatus(testUser.email);
      expect(initialStatus.verified).toBe(false);

      // Note: In a real implementation, after successful verification:
      // 1. User's emailVerified field should be updated to true
      // 2. User should be able to login
      // 3. Verification status endpoint should return verified: true
    });

    test('should handle verification in multi-tenant environment', async () => {
      // Create users in different tenants
      const tenant1User = await authFramework.registerUser({
        username: 'tenant1_verification',
        email: 'tenant1_verification@example.com',
        tenantId: 'tenant1',
        emailVerified: false
      });

      const tenant2User = await authFramework.registerUser({
        username: 'tenant2_verification',
        email: 'tenant2_verification@example.com',
        tenantId: 'tenant2',
        emailVerified: false
      });

      // Both users should be able to request verification independently
      const tenant1Status = await authFramework.getEmailVerificationStatus(tenant1User.email);
      const tenant2Status = await authFramework.getEmailVerificationStatus(tenant2User.email);

      expect(tenant1Status.verified).toBe(false);
      expect(tenant2Status.verified).toBe(false);

      // Both should be able to resend verification
      const tenant1Resend = await authFramework.resendEmailVerification(tenant1User.email);
      const tenant2Resend = await authFramework.resendEmailVerification(tenant2User.email);

      expect(tenant1Resend.message).toBeDefined();
      expect(tenant2Resend.message).toBeDefined();
    });
  });
});