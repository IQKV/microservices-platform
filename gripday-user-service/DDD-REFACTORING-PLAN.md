# DDD Refactoring Plan

## Overview

Refactoring from three-tier architecture (presentation/domain/infrastructure) to Domain-Driven Design with bounded contexts.

## New Structure (Flat, 2-level depth)

```
src/main/java/org/gripday/userservice/
├── UserServiceApplication.java
│
├── shared/                          # Shared Kernel
│   ├── Authority.java              # Role/permission entity
│   ├── AuthorityRepository.java
│   ├── TenantAware.java            # Base class for tenant-aware entities
│   ├── EmailService.java           # Email sending infrastructure
│   ├── EmailOperations.java
│   └── MessageService.java         # I18n message service
│
├── authentication/                  # Authentication Bounded Context
│   ├── AuthenticationService.java  # Core authentication logic
│   ├── JwtTokenService.java        # JWT generation/validation
│   ├── JwtKeyManagementService.java
│   ├── AuthenticationController.java
│   ├── JwkSetController.java
│   ├── LoginRequest.java
│   ├── RefreshTokenRequest.java
│   ├── TokenResponse.java
│   ├── ValidateTokenRequest.java
│   ├── ValidateTokenResponse.java
│   └── AuthenticationResult.java
│
├── registration/                    # Registration Bounded Context
│   ├── RegistrationService.java    # User signup logic
│   ├── SignupRequest.java
│   └── RegistrationResponse.java
│
├── emailverification/               # Email Verification Bounded Context
│   ├── VerificationToken.java      # Token entity
│   ├── VerificationTokenRepository.java
│   ├── EmailVerificationService.java
│   ├── VerificationMetrics.java
│   ├── TokenCleanupService.java
│   ├── VerificationController.java
│   ├── VerificationResponse.java
│   ├── ResendVerificationRequest.java
│   └── VerificationStatusResponse.java
│
├── passwordmanagement/              # Password Management Bounded Context
│   ├── PasswordResetService.java
│   ├── PasswordResetController.java
│   ├── ForgotPasswordRequest.java
│   ├── ResetPasswordRequest.java
│   └── ChangePasswordRequest.java
│
├── usermanagement/                  # User Management Bounded Context
│   ├── User.java                   # User aggregate root
│   ├── UserRepository.java
│   ├── UserManagementService.java
│   ├── UserManagementController.java  # Admin operations
│   ├── UserProfileController.java     # User self-service
│   ├── UserDto.java
│   ├── UserContext.java
│   ├── CreateUserRequest.java
│   └── UpdateUserRequest.java
│
├── tenancy/                         # Multi-Tenancy Bounded Context
│   ├── Tenant.java                 # Tenant aggregate root
│   ├── TenantRepository.java
│   ├── TenantContext.java          # Thread-local tenant context
│   ├── TenantService.java
│   ├── TenantExtractionService.java
│   ├── TenantController.java
│   ├── TenantExtractionFilter.java
│   └── TenantConfig.java
│
├── organization/                    # Organization Bounded Context
│   ├── Organization.java           # Organization aggregate root
│   ├── OrganizationRepository.java
│   ├── OrganizationService.java
│   ├── OrganizationController.java
│   ├── OrganizationDto.java
│   ├── CreateOrganizationRequest.java
│   └── UpdateOrganizationRequest.java
│
├── security/                        # Security Bounded Context (cross-cutting)
│   ├── AccountLockoutService.java
│   ├── RateLimitingService.java
│   ├── SecurityAuditService.java
│   ├── UserAuditLog.java           # Audit log entity
│   ├── UserAuditLogRepository.java
│   ├── InputSanitizer.java
│   ├── PasswordValidator.java
│   ├── UsernameValidator.java
│   ├── ValidPassword.java
│   ├── ValidUsername.java
│   ├── SecurityConfig.java
│   ├── RateLimitingFilter.java
│   └── GlobalExceptionHandler.java
│
└── config/                          # Infrastructure Configuration
    ├── CorrelationIdFilter.java
    ├── DatabaseConfig.java
    ├── GripdayProperties.java
    ├── HealthCheckConfig.java
    ├── JwtConfiguration.java
    ├── MailConfig.java
    ├── MetricsConfiguration.java
    ├── ObservabilityConfig.java
    ├── OpenApiConfig.java
    ├── RedisConfig.java
    ├── StructuredLoggingConfig.java
    ├── UserServiceConfig.java
    ├── WebConfig.java
    ├── TokenCleanupService.java
    ├── AuthConfigurationProperties.java
    ├── DatabaseConfigurationProperties.java
    └── I18nConfig.java
```

## Bounded Contexts Identified

### 1. **Authentication** (Core Domain)

- **Purpose**: User login, logout, JWT token management, session management
- **Aggregate Root**: Session (implicit)
- **Key Services**: AuthenticationService, JwtTokenService
- **Key Entities**: None (stateless, uses Redis for sessions)

### 2. **Registration** (Core Domain)

- **Purpose**: New user signup and account creation
- **Aggregate Root**: User (shared with User Management)
- **Key Services**: RegistrationService
- **Dependencies**: EmailVerification, UserManagement

### 3. **Email Verification** (Supporting Domain)

- **Purpose**: Email verification tokens and workflows
- **Aggregate Root**: VerificationToken
- **Key Services**: EmailVerificationService
- **Key Entities**: VerificationToken

### 4. **Password Management** (Supporting Domain)

- **Purpose**: Password reset and change operations
- **Aggregate Root**: None (operates on User)
- **Key Services**: PasswordResetService
- **Dependencies**: UserManagement, EmailService

### 5. **User Management** (Core Domain)

- **Purpose**: User CRUD operations, user profile management
- **Aggregate Root**: User
- **Key Services**: UserManagementService
- **Key Entities**: User

### 6. **Tenancy** (Generic Subdomain)

- **Purpose**: Multi-tenant isolation and context management
- **Aggregate Root**: Tenant
- **Key Services**: TenantService, TenantContext
- **Key Entities**: Tenant

### 7. **Organization** (Supporting Domain)

- **Purpose**: Organization/company management
- **Aggregate Root**: Organization
- **Key Services**: OrganizationService
- **Key Entities**: Organization

### 8. **Security** (Generic Subdomain - Cross-cutting)

- **Purpose**: Account lockout, rate limiting, audit logging, input validation
- **Key Services**: AccountLockoutService, SecurityAuditService, RateLimitingService
- **Key Entities**: UserAuditLog

## File Mapping (Old → New)

### Shared Kernel

- `infrastructure/entity/Authority.java` → `shared/Authority.java`
- `infrastructure/entity/TenantAwareEntity.java` → `shared/TenantAware.java`
- `infrastructure/repository/AuthorityRepository.java` → `shared/AuthorityRepository.java`
- `domain/service/EmailService.java` → `shared/EmailService.java`
- `domain/service/EmailOperations.java` → `shared/EmailOperations.java`
- `infrastructure/i18n/MessageService.java` → `shared/MessageService.java`

### Authentication

- `domain/service/AuthenticationService.java` → `authentication/AuthenticationService.java`
- `domain/service/JwtService.java` → `authentication/JwtTokenService.java`
- `domain/service/JwtKeyManagementService.java` → `authentication/JwtKeyManagementService.java`
- `presentation/web/AuthenticationResource.java` → `authentication/AuthenticationController.java`
- `presentation/web/JwkSetResource.java` → `authentication/JwkSetController.java`
- `presentation/dto/LoginRequest.java` → `authentication/LoginRequest.java`
- `presentation/dto/RefreshTokenRequest.java` → `authentication/RefreshTokenRequest.java`
- `presentation/dto/TokenResponse.java` → `authentication/TokenResponse.java`
- `presentation/dto/ValidateTokenRequest.java` → `authentication/ValidateTokenRequest.java`
- `presentation/dto/ValidateTokenResponse.java` → `authentication/ValidateTokenResponse.java`
- `presentation/dto/AuthenticationResult.java` → `authentication/AuthenticationResult.java`

### Registration

- `domain/service/UserRegistrationService.java` → `registration/RegistrationService.java`
- `presentation/dto/SignupRequest.java` → `registration/SignupRequest.java`
- `presentation/dto/UserRegistrationResponse.java` → `registration/RegistrationResponse.java`

### Email Verification

- `infrastructure/entity/EmailVerificationToken.java` → `emailverification/VerificationToken.java`
- `infrastructure/repository/EmailVerificationTokenRepository.java` → `emailverification/VerificationTokenRepository.java`
- `domain/service/EmailVerificationService.java` → `emailverification/EmailVerificationService.java`
- `domain/service/EmailVerificationMetricsService.java` → `emailverification/VerificationMetrics.java`
- `domain/service/EmailVerificationTokenCleanupService.java` → `emailverification/TokenCleanupService.java`
- `presentation/web/EmailVerificationResource.java` → `emailverification/VerificationController.java`
- `presentation/dto/EmailVerificationResponse.java` → `emailverification/VerificationResponse.java`
- `presentation/dto/ResendVerificationRequest.java` → `emailverification/ResendVerificationRequest.java`
- `presentation/dto/VerificationStatusResponse.java` → `emailverification/VerificationStatusResponse.java`

### Password Management

- `domain/service/PasswordResetService.java` → `passwordmanagement/PasswordResetService.java`
- `presentation/web/PasswordResetResource.java` → `passwordmanagement/PasswordResetController.java`
- `presentation/dto/ForgotPasswordRequest.java` → `passwordmanagement/ForgotPasswordRequest.java`
- `presentation/dto/ResetPasswordRequest.java` → `passwordmanagement/ResetPasswordRequest.java`
- `presentation/dto/ChangePasswordRequest.java` → `passwordmanagement/ChangePasswordRequest.java`

### User Management

- `infrastructure/entity/User.java` → `usermanagement/User.java`
- `infrastructure/repository/UserRepository.java` → `usermanagement/UserRepository.java`
- `domain/service/UserManagementService.java` → `usermanagement/UserManagementService.java`
- `presentation/web/admin/UserManagementResource.java` → `usermanagement/UserManagementController.java`
- `presentation/web/UserProfileResource.java` → `usermanagement/UserProfileController.java`
- `presentation/dto/UserDto.java` → `usermanagement/UserDto.java`
- `presentation/dto/UserContext.java` → `usermanagement/UserContext.java`
- `presentation/dto/CreateUserRequest.java` → `usermanagement/CreateUserRequest.java`
- `presentation/dto/UpdateUserRequest.java` → `usermanagement/UpdateUserRequest.java`

### Tenancy

- `infrastructure/entity/Tenant.java` → `tenancy/Tenant.java`
- `infrastructure/repository/TenantRepository.java` → `tenancy/TenantRepository.java`
- `domain/service/TenantContext.java` → `tenancy/TenantContext.java`
- `domain/service/TenantManagementService.java` → `tenancy/TenantService.java`
- `domain/service/TenantExtractionService.java` → `tenancy/TenantExtractionService.java`
- `presentation/web/admin/TenantManagementResource.java` → `tenancy/TenantController.java`
- `config/TenantExtractionFilter.java` → `tenancy/TenantExtractionFilter.java`
- `config/TenantConfig.java` → `tenancy/TenantConfig.java`

### Organization

- `infrastructure/entity/Organization.java` → `organization/Organization.java`
- `infrastructure/repository/OrganizationRepository.java` → `organization/OrganizationRepository.java`
- `domain/service/OrganizationManagementService.java` → `organization/OrganizationService.java`
- `presentation/web/admin/OrganizationManagementResource.java` → `organization/OrganizationController.java`
- `presentation/dto/OrganizationDto.java` → `organization/OrganizationDto.java`
- `presentation/dto/CreateOrganizationRequest.java` → `organization/CreateOrganizationRequest.java`
- `presentation/dto/UpdateOrganizationRequest.java` → `organization/UpdateOrganizationRequest.java`

### Security

- `domain/service/AccountLockoutService.java` → `security/AccountLockoutService.java`
- `domain/service/RateLimitingService.java` → `security/RateLimitingService.java`
- `domain/service/SecurityAuditService.java` → `security/SecurityAuditService.java`
- `infrastructure/entity/UserAuditLog.java` → `security/UserAuditLog.java`
- `infrastructure/repository/UserAuditLogRepository.java` → `security/UserAuditLogRepository.java`
- `presentation/validation/InputSanitizer.java` → `security/InputSanitizer.java`
- `presentation/validation/PasswordValidator.java` → `security/PasswordValidator.java`
- `presentation/validation/UsernameValidator.java` → `security/UsernameValidator.java`
- `presentation/validation/ValidPassword.java` → `security/ValidPassword.java`
- `presentation/validation/ValidUsername.java` → `security/ValidUsername.java`
- `config/SecurityConfig.java` → `security/SecurityConfig.java`
- `config/RateLimitingFilter.java` → `security/RateLimitingFilter.java`
- `presentation/exception/GlobalExceptionHandler.java` → `security/GlobalExceptionHandler.java`

## Benefits of DDD Structure

1. **Business-Aligned**: Each package represents a clear business capability
2. **Flat Structure**: Only 2 levels deep (bounded context + files)
3. **Developer-Friendly**: Easy to navigate and understand
4. **Bounded Contexts**: Clear separation of concerns
5. **Aggregate Roots**: User, Tenant, Organization, VerificationToken
6. **Shared Kernel**: Common domain objects (Authority, TenantAware)
7. **No Enterprise Bloat**: Simple, pragmatic structure

## Implementation Steps

1. ✅ Create shared kernel (Authority, TenantAware, repositories)
2. ✅ Create User Management context (User entity, repository)
3. ⏳ Move Authentication context files
4. ⏳ Move Registration context files
5. ⏳ Move Email Verification context files
6. ⏳ Move Password Management context files
7. ⏳ Move Tenancy context files
8. ⏳ Move Organization context files
9. ⏳ Move Security context files
10. ⏳ Update all package declarations
11. ⏳ Update all imports
12. ⏳ Update test files
13. ⏳ Run Maven build and fix compilation errors
14. ⏳ Run tests and verify functionality
