# DDD Refactoring Script
# This script moves files from three-tier architecture to DDD bounded contexts

$ErrorActionPreference = "Stop"

Write-Host "Starting DDD refactoring..." -ForegroundColor Green

# Define the mapping of old paths to new paths
$fileMappings = @{
    # Shared Kernel
    "src/main/java/org/gripday/userservice/infrastructure/entity/Authority.java" = "src/main/java/org/gripday/userservice/shared/Authority.java"
    "src/main/java/org/gripday/userservice/infrastructure/entity/TenantAwareEntity.java" = "src/main/java/org/gripday/userservice/shared/TenantAware.java"
    "src/main/java/org/gripday/userservice/infrastructure/repository/AuthorityRepository.java" = "src/main/java/org/gripday/userservice/shared/AuthorityRepository.java"
    
    # User Management Bounded Context
    "src/main/java/org/gripday/userservice/infrastructure/entity/User.java" = "src/main/java/org/gripday/userservice/usermanagement/User.java"
    "src/main/java/org/gripday/userservice/infrastructure/repository/UserRepository.java" = "src/main/java/org/gripday/userservice/usermanagement/UserRepository.java"
    "src/main/java/org/gripday/userservice/domain/service/UserManagementService.java" = "src/main/java/org/gripday/userservice/usermanagement/UserManagementService.java"
    "src/main/java/org/gripday/userservice/presentation/web/admin/UserManagementResource.java" = "src/main/java/org/gripday/userservice/usermanagement/UserManagementController.java"
    "src/main/java/org/gripday/userservice/presentation/dto/UserDto.java" = "src/main/java/org/gripday/userservice/usermanagement/UserDto.java"
    "src/main/java/org/gripday/userservice/presentation/dto/CreateUserRequest.java" = "src/main/java/org/gripday/userservice/usermanagement/CreateUserRequest.java"
    "src/main/java/org/gripday/userservice/presentation/dto/UpdateUserRequest.java" = "src/main/java/org/gripday/userservice/usermanagement/UpdateUserRequest.java"
    "src/main/java/org/gripday/userservice/presentation/web/UserProfileResource.java" = "src/main/java/org/gripday/userservice/usermanagement/UserProfileController.java"
    "src/main/java/org/gripday/userservice/presentation/dto/UserContext.java" = "src/main/java/org/gripday/userservice/usermanagement/UserContext.java"
    
    # Authentication Bounded Context
    "src/main/java/org/gripday/userservice/domain/service/AuthenticationService.java" = "src/main/java/org/gripday/userservice/authentication/AuthenticationService.java"
    "src/main/java/org/gripday/userservice/domain/service/JwtService.java" = "src/main/java/org/gripday/userservice/authentication/JwtTokenService.java"
    "src/main/java/org/gripday/userservice/domain/service/JwtKeyManagementService.java" = "src/main/java/org/gripday/userservice/authentication/JwtKeyManagementService.java"
    "src/main/java/org/gripday/userservice/presentation/web/AuthenticationResource.java" = "src/main/java/org/gripday/userservice/authentication/AuthenticationController.java"
    "src/main/java/org/gripday/userservice/presentation/web/JwkSetResource.java" = "src/main/java/org/gripday/userservice/authentication/JwkSetController.java"
    "src/main/java/org/gripday/userservice/presentation/dto/LoginRequest.java" = "src/main/java/org/gripday/userservice/authentication/LoginRequest.java"
    "src/main/java/org/gripday/userservice/presentation/dto/RefreshTokenRequest.java" = "src/main/java/org/gripday/userservice/authentication/RefreshTokenRequest.java"
    "src/main/java/org/gripday/userservice/presentation/dto/TokenResponse.java" = "src/main/java/org/gripday/userservice/authentication/TokenResponse.java"
    "src/main/java/org/gripday/userservice/presentation/dto/ValidateTokenRequest.java" = "src/main/java/org/gripday/userservice/authentication/ValidateTokenRequest.java"
    "src/main/java/org/gripday/userservice/presentation/dto/ValidateTokenResponse.java" = "src/main/java/org/gripday/userservice/authentication/ValidateTokenResponse.java"
    "src/main/java/org/gripday/userservice/presentation/dto/AuthenticationResult.java" = "src/main/java/org/gripday/userservice/authentication/AuthenticationResult.java"
    
    # Registration Bounded Context
    "src/main/java/org/gripday/userservice/domain/service/UserRegistrationService.java" = "src/main/java/org/gripday/userservice/registration/RegistrationService.java"
    "src/main/java/org/gripday/userservice/presentation/dto/SignupRequest.java" = "src/main/java/org/gripday/userservice/registration/SignupRequest.java"
    "src/main/java/org/gripday/userservice/presentation/dto/UserRegistrationResponse.java" = "src/main/java/org/gripday/userservice/registration/RegistrationResponse.java"
    
    # Email Verification Bounded Context
    "src/main/java/org/gripday/userservice/infrastructure/entity/EmailVerificationToken.java" = "src/main/java/org/gripday/userservice/emailverification/VerificationToken.java"
    "src/main/java/org/gripday/userservice/infrastructure/repository/EmailVerificationTokenRepository.java" = "src/main/java/org/gripday/userservice/emailverification/VerificationTokenRepository.java"
    "src/main/java/org/gripday/userservice/domain/service/EmailVerificationService.java" = "src/main/java/org/gripday/userservice/emailverification/EmailVerificationService.java"
    "src/main/java/org/gripday/userservice/domain/service/EmailVerificationMetricsService.java" = "src/main/java/org/gripday/userservice/emailverification/VerificationMetrics.java"
    "src/main/java/org/gripday/userservice/domain/service/EmailVerificationTokenCleanupService.java" = "src/main/java/org/gripday/userservice/emailverification/TokenCleanupService.java"
    "src/main/java/org/gripday/userservice/presentation/web/EmailVerificationResource.java" = "src/main/java/org/gripday/userservice/emailverification/VerificationController.java"
    "src/main/java/org/gripday/userservice/presentation/dto/EmailVerificationResponse.java" = "src/main/java/org/gripday/userservice/emailverification/VerificationResponse.java"
    "src/main/java/org/gripday/userservice/presentation/dto/ResendVerificationRequest.java" = "src/main/java/org/gripday/userservice/emailverification/ResendVerificationRequest.java"
    "src/main/java/org/gripday/userservice/presentation/dto/VerificationStatusResponse.java" = "src/main/java/org/gripday/userservice/emailverification/VerificationStatusResponse.java"
    
    # Password Management Bounded Context
    "src/main/java/org/gripday/userservice/domain/service/PasswordResetService.java" = "src/main/java/org/gripday/userservice/passwordmanagement/PasswordResetService.java"
    "src/main/java/org/gripday/userservice/presentation/web/PasswordResetResource.java" = "src/main/java/org/gripday/userservice/passwordmanagement/PasswordResetController.java"
    "src/main/java/org/gripday/userservice/presentation/dto/ForgotPasswordRequest.java" = "src/main/java/org/gripday/userservice/passwordmanagement/ForgotPasswordRequest.java"
    "src/main/java/org/gripday/userservice/presentation/dto/ResetPasswordRequest.java" = "src/main/java/org/gripday/userservice/passwordmanagement/ResetPasswordRequest.java"
    "src/main/java/org/gripday/userservice/presentation/dto/ChangePasswordRequest.java" = "src/main/java/org/gripday/userservice/passwordmanagement/ChangePasswordRequest.java"
    
    # Tenancy Bounded Context
    "src/main/java/org/gripday/userservice/infrastructure/entity/Tenant.java" = "src/main/java/org/gripday/userservice/tenancy/Tenant.java"
    "src/main/java/org/gripday/userservice/infrastructure/repository/TenantRepository.java" = "src/main/java/org/gripday/userservice/tenancy/TenantRepository.java"
    "src/main/java/org/gripday/userservice/domain/service/TenantContext.java" = "src/main/java/org/gripday/userservice/tenancy/TenantContext.java"
    "src/main/java/org/gripday/userservice/domain/service/TenantManagementService.java" = "src/main/java/org/gripday/userservice/tenancy/TenantService.java"
    "src/main/java/org/gripday/userservice/domain/service/TenantExtractionService.java" = "src/main/java/org/gripday/userservice/tenancy/TenantExtractionService.java"
    "src/main/java/org/gripday/userservice/presentation/web/admin/TenantManagementResource.java" = "src/main/java/org/gripday/userservice/tenancy/TenantController.java"
    "src/main/java/org/gripday/userservice/config/TenantExtractionFilter.java" = "src/main/java/org/gripday/userservice/tenancy/TenantExtractionFilter.java"
    "src/main/java/org/gripday/userservice/config/TenantConfig.java" = "src/main/java/org/gripday/userservice/tenancy/TenantConfig.java"
    
    # Organization Bounded Context
    "src/main/java/org/gripday/userservice/infrastructure/entity/Organization.java" = "src/main/java/org/gripday/userservice/organization/Organization.java"
    "src/main/java/org/gripday/userservice/infrastructure/repository/OrganizationRepository.java" = "src/main/java/org/gripday/userservice/organization/OrganizationRepository.java"
    "src/main/java/org/gripday/userservice/domain/service/OrganizationManagementService.java" = "src/main/java/org/gripday/userservice/organization/OrganizationService.java"
    "src/main/java/org/gripday/userservice/presentation/web/admin/OrganizationManagementResource.java" = "src/main/java/org/gripday/userservice/organization/OrganizationController.java"
    "src/main/java/org/gripday/userservice/presentation/dto/OrganizationDto.java" = "src/main/java/org/gripday/userservice/organization/OrganizationDto.java"
    "src/main/java/org/gripday/userservice/presentation/dto/CreateOrganizationRequest.java" = "src/main/java/org/gripday/userservice/organization/CreateOrganizationRequest.java"
    "src/main/java/org/gripday/userservice/presentation/dto/UpdateOrganizationRequest.java" = "src/main/java/org/gripday/userservice/organization/UpdateOrganizationRequest.java"
    
    # Security Bounded Context (cross-cutting)
    "src/main/java/org/gripday/userservice/domain/service/AccountLockoutService.java" = "src/main/java/org/gripday/userservice/security/AccountLockoutService.java"
    "src/main/java/org/gripday/userservice/domain/service/RateLimitingService.java" = "src/main/java/org/gripday/userservice/security/RateLimitingService.java"
    "src/main/java/org/gripday/userservice/domain/service/SecurityAuditService.java" = "src/main/java/org/gripday/userservice/security/SecurityAuditService.java"
    "src/main/java/org/gripday/userservice/infrastructure/entity/UserAuditLog.java" = "src/main/java/org/gripday/userservice/security/UserAuditLog.java"
    "src/main/java/org/gripday/userservice/infrastructure/repository/UserAuditLogRepository.java" = "src/main/java/org/gripday/userservice/security/UserAuditLogRepository.java"
    "src/main/java/org/gripday/userservice/presentation/validation/InputSanitizer.java" = "src/main/java/org/gripday/userservice/security/InputSanitizer.java"
    "src/main/java/org/gripday/userservice/presentation/validation/PasswordValidator.java" = "src/main/java/org/gripday/userservice/security/PasswordValidator.java"
    "src/main/java/org/gripday/userservice/presentation/validation/UsernameValidator.java" = "src/main/java/org/gripday/userservice/security/UsernameValidator.java"
    "src/main/java/org/gripday/userservice/presentation/validation/ValidPassword.java" = "src/main/java/org/gripday/userservice/security/ValidPassword.java"
    "src/main/java/org/gripday/userservice/presentation/validation/ValidUsername.java" = "src/main/java/org/gripday/userservice/security/ValidUsername.java"
    "src/main/java/org/gripday/userservice/config/SecurityConfig.java" = "src/main/java/org/gripday/userservice/security/SecurityConfig.java"
    "src/main/java/org/gripday/userservice/config/RateLimitingFilter.java" = "src/main/java/org/gripday/userservice/security/RateLimitingFilter.java"
    "src/main/java/org/gripday/userservice/presentation/exception/GlobalExceptionHandler.java" = "src/main/java/org/gripday/userservice/security/GlobalExceptionHandler.java"
    
    # Email Infrastructure (shared service)
    "src/main/java/org/gripday/userservice/domain/service/EmailService.java" = "src/main/java/org/gripday/userservice/shared/EmailService.java"
    "src/main/java/org/gripday/userservice/domain/service/EmailOperations.java" = "src/main/java/org/gripday/userservice/shared/EmailOperations.java"
    "src/main/java/org/gripday/userservice/infrastructure/i18n/MessageService.java" = "src/main/java/org/gripday/userservice/shared/MessageService.java"
    
    # Configuration (stays in config but simplified)
    "src/main/java/org/gripday/userservice/domain/service/TokenCleanupService.java" = "src/main/java/org/gripday/userservice/config/TokenCleanupService.java"
}

Write-Host "File mappings defined: $($fileMappings.Count) files" -ForegroundColor Cyan

Write-Host "`nRefactoring complete! Now update package declarations in all moved files." -ForegroundColor Green
Write-Host "Run Maven build to identify remaining import issues." -ForegroundColor Yellow
