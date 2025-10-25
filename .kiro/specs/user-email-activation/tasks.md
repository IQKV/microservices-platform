# Implementation Plan

- [x] 1. Create database migration and entity for email verification tokens





  - Create Liquibase migration file `007-create-email-verification-tokens-table.xml`
  - Update `db.changelog-master.xml` to include the new migration
  - Create `EmailVerificationToken` entity class extending `TenantAwareEntity`
  - Create `EmailVerificationTokenRepository` interface with required query methods
  - _Requirements: 1.1, 2.1, 4.1, 4.2, 4.3_

- [ ] 2. Implement email service infrastructure
  - [ ] 2.1 Create email configuration properties class
    - Add SMTP configuration properties to `GripdayProperties`
    - Create email-specific configuration class for verification settings
    - _Requirements: 1.2, 3.2, 3.5_

  - [ ] 2.2 Implement core email service
    - Create `EmailService` interface and implementation
    - Implement SMTP integration using Spring Boot Mail
    - Create HTML email template for verification emails
    - Implement `sendVerificationEmail()` and `buildVerificationUrl()` methods
    - _Requirements: 1.2, 3.2_

- [ ] 3. Create email verification domain service
  - [ ] 3.1 Implement EmailVerificationService
    - Create `EmailVerificationService` class with token generation logic
    - Implement `generateVerificationToken()` using secure UUID generation
    - Implement `verifyEmail()` method with token validation and user activation
    - Implement `resendVerificationEmail()` with rate limiting logic
    - Add `cleanupExpiredTokens()` method for scheduled cleanup
    - _Requirements: 1.1, 1.5, 2.1, 2.2, 2.3, 2.5, 3.1, 3.3, 3.4, 4.1, 4.4, 4.5_

  - [ ] 3.2 Write unit tests for EmailVerificationService
    - Test token generation and uniqueness
    - Test email verification flow with valid and invalid tokens
    - Test rate limiting for resend functionality
    - Test token expiration and cleanup logic
    - _Requirements: 1.1, 1.5, 2.1, 2.2, 2.3, 3.1, 3.3, 3.4_

- [ ] 4. Update authentication flow for email verification
  - [ ] 4.1 Modify AuthenticationService
    - Update `authenticateUser()` method to check `emailVerified` status
    - Return specific error response for unverified accounts
    - _Requirements: 1.4, 5.1_

  - [ ] 4.2 Update UserRegistrationService
    - Integrate email verification token generation after user registration
    - Trigger verification email sending after successful registration
    - Ensure `emailVerified` remains false for new users
    - _Requirements: 1.1, 1.2, 1.3_

  - [ ] 4.3 Write unit tests for authentication changes
    - Test login rejection for unverified users
    - Test successful login after email verification
    - Test registration flow with email verification trigger
    - _Requirements: 1.3, 1.4, 5.1_

- [ ] 5. Create email verification REST endpoints
  - [ ] 5.1 Create EmailVerificationResource controller
    - Implement `GET /api/v1/auth/email/verify` endpoint for email verification
    - Implement `POST /api/v1/auth/email/resend` endpoint for resending verification
    - Implement `GET /api/v1/auth/email/status` endpoint for verification status
    - Add proper OpenAPI documentation and examples
    - _Requirements: 2.1, 2.2, 2.3, 3.1, 3.2, 5.2, 5.3, 5.4, 5.5_

  - [ ] 5.2 Create DTOs for email verification
    - Create `EmailVerificationResponse` record
    - Create `ResendVerificationRequest` record with validation
    - Create `VerificationStatusResponse` record
    - _Requirements: 2.2, 2.3, 3.1, 5.2, 5.3, 5.4, 5.5_

  - [ ] 5.3 Update AuthenticationResource error responses
    - Enhance login error responses to include email verification status
    - Add actionable error messages with resend email links
    - _Requirements: 5.1, 5.2, 5.3_

  - [ ] 5.4 Write integration tests for email verification endpoints
    - Test complete email verification flow end-to-end
    - Test error handling for invalid/expired tokens
    - Test rate limiting for resend functionality
    - Test multi-tenant isolation for verification tokens
    - _Requirements: 2.1, 2.2, 2.3, 3.1, 3.2, 3.5_

- [ ] 6. Add configuration and scheduled tasks
  - [ ] 6.1 Update application configuration
    - Add email service configuration to `application.yml`
    - Add environment-specific SMTP settings
    - Configure email templates and verification URL settings
    - _Requirements: 1.2, 3.2, 4.4_

  - [ ] 6.2 Implement scheduled token cleanup
    - Create scheduled task to clean up expired verification tokens
    - Configure cleanup to run daily and remove tokens older than 48 hours
    - Add logging and metrics for cleanup operations
    - _Requirements: 4.3_

  - [ ] 6.3 Write tests for configuration and scheduled tasks
    - Test SMTP configuration loading
    - Test scheduled cleanup task execution
    - Test email template rendering
    - _Requirements: 4.3_

- [ ] 7. Add observability and monitoring
  - [ ] 7.1 Implement metrics and logging
    - Add custom metrics for email verification events
    - Implement structured logging for verification attempts
    - Add OpenTelemetry tracing for email verification flow
    - _Requirements: 1.1, 1.2, 2.1, 2.2, 3.1_

  - [ ] 7.2 Update error handling
    - Create new error codes for email verification scenarios
    - Implement proper exception handling with meaningful messages
    - Add audit logging for security events
    - _Requirements: 2.3, 5.1, 5.2, 5.3_

- [ ] 8. Final integration and testing
  - [ ] 8.1 Update database migration master file
    - Add the new migration to `db.changelog-master.xml`
    - Verify migration runs successfully on clean database
    - _Requirements: 4.2_

  - [ ] 8.2 Integration testing and validation
    - Test complete user registration and email verification flow
    - Verify multi-tenant isolation works correctly
    - Test rate limiting and security measures
    - Validate email delivery and template rendering
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2, 2.3, 2.4, 2.5, 3.1, 3.2, 3.3, 3.4, 3.5_