# Next Steps to Complete DDD Refactoring

## Current Status

✅ **Completed:**
- DDD structure designed and documented
- Shared kernel created (Authority, TenantAware, AuthorityRepository)
- User Management context partially created (User, UserRepository)
- Authentication Controller created
- Refactoring plan documented
- Automation script created

⏳ **Remaining:**
- Move 70+ files to new bounded context structure
- Update all package declarations
- Update all imports
- Update test files
- Delete old directory structure

## Quick Start - Complete the Refactoring

### Step 1: Run the Automated Refactoring

The easiest way is to manually move files following the mapping in `DDD-REFACTORING-PLAN.md`. Here's a systematic approach:

### Step 2: Move Files by Bounded Context

#### A. Tenancy Context (Priority 1 - Required by others)
```powershell
# Create directory
New-Item -ItemType Directory -Path "src/main/java/org/gripday/userservice/tenancy" -Force

# Move files (update package to org.gripday.userservice.tenancy)
# - infrastructure/entity/Tenant.java → tenancy/Tenant.java
# - infrastructure/repository/TenantRepository.java → tenancy/TenantRepository.java
# - domain/service/TenantContext.java → tenancy/TenantContext.java
# - domain/service/TenantManagementService.java → tenancy/TenantService.java
# - domain/service/TenantExtractionService.java → tenancy/TenantExtractionService.java
# - presentation/web/admin/TenantManagementResource.java → tenancy/TenantController.java
# - config/TenantExtractionFilter.java → tenancy/TenantExtractionFilter.java
# - config/TenantConfig.java → tenancy/TenantConfig.java
```

#### B. Security Context (Priority 1 - Required by others)
```powershell
# Create directory
New-Item -ItemType Directory -Path "src/main/java/org/gripday/userservice/security" -Force

# Move files (update package to org.gripday.userservice.security)
# - domain/service/AccountLockoutService.java → security/AccountLockoutService.java
# - domain/service/RateLimitingService.java → security/RateLimitingService.java
# - domain/service/SecurityAuditService.java → security/SecurityAuditService.java
# - infrastructure/entity/UserAuditLog.java → security/UserAuditLog.java
# - infrastructure/repository/UserAuditLogRepository.java → security/UserAuditLogRepository.java
# - presentation/validation/InputSanitizer.java → security/InputSanitizer.java
# - presentation/validation/PasswordValidator.java → security/PasswordValidator.java
# - presentation/validation/UsernameValidator.java → security/UsernameValidator.java
# - presentation/validation/ValidPassword.java → security/ValidPassword.java
# - presentation/validation/ValidUsername.java → security/ValidUsername.java
# - config/SecurityConfig.java → security/SecurityConfig.java
# - config/RateLimitingFilter.java → security/RateLimitingFilter.java
# - presentation/exception/GlobalExceptionHandler.java → security/GlobalExceptionHandler.java
```

#### C. Shared Services (Priority 1)
```powershell
# Move to shared/ (update package to org.gripday.userservice.shared)
# - domain/service/EmailService.java → shared/EmailService.java
# - domain/service/EmailOperations.java → shared/EmailOperations.java
# - infrastructure/i18n/MessageService.java → shared/MessageService.java
```

#### D. Authentication Context (Priority 2)
```powershell
# Create directory
New-Item -ItemType Directory -Path "src/main/java/org/gripday/userservice/authentication" -Force

# Move files (update package to org.gripday.userservice.authentication)
# - domain/service/AuthenticationService.java → authentication/AuthenticationService.java
# - domain/service/JwtService.java → authentication/JwtTokenService.java (rename class too!)
# - domain/service/JwtKeyManagementService.java → authentication/JwtKeyManagementService.java
# - presentation/web/JwkSetResource.java → authentication/JwkSetController.java
# - presentation/dto/LoginRequest.java → authentication/LoginRequest.java
# - presentation/dto/RefreshTokenRequest.java → authentication/RefreshTokenRequest.java
# - presentation/dto/TokenResponse.java → authentication/TokenResponse.java
# - presentation/dto/ValidateTokenRequest.java → authentication/ValidateTokenRequest.java
# - presentation/dto/ValidateTokenResponse.java → authentication/ValidateTokenResponse.java
# - presentation/dto/AuthenticationResult.java → authentication/AuthenticationResult.java
```

#### E. Registration Context (Priority 2)
```powershell
# Create directory
New-Item -ItemType Directory -Path "src/main/java/org/gripday/userservice/registration" -Force

# Move files (update package to org.gripday.userservice.registration)
# - domain/service/UserRegistrationService.java → registration/RegistrationService.java (rename class!)
# - presentation/dto/SignupRequest.java → registration/SignupRequest.java
# - presentation/dto/UserRegistrationResponse.java → registration/RegistrationResponse.java (rename class!)
```

#### F. Email Verification Context (Priority 2)
```powershell
# Create directory
New-Item -ItemType Directory -Path "src/main/java/org/gripday/userservice/emailverification" -Force

# Move files (update package to org.gripday.userservice.emailverification)
# - infrastructure/entity/EmailVerificationToken.java → emailverification/VerificationToken.java (rename class!)
# - infrastructure/repository/EmailVerificationTokenRepository.java → emailverification/VerificationTokenRepository.java
# - domain/service/EmailVerificationService.java → emailverification/EmailVerificationService.java
# - domain/service/EmailVerificationMetricsService.java → emailverification/VerificationMetrics.java (rename class!)
# - domain/service/EmailVerificationTokenCleanupService.java → emailverification/TokenCleanupService.java (rename class!)
# - presentation/web/EmailVerificationResource.java → emailverification/VerificationController.java
# - presentation/dto/EmailVerificationResponse.java → emailverification/VerificationResponse.java (rename class!)
# - presentation/dto/ResendVerificationRequest.java → emailverification/ResendVerificationRequest.java
# - presentation/dto/VerificationStatusResponse.java → emailverification/VerificationStatusResponse.java
```

#### G. Password Management Context (Priority 3)
```powershell
# Create directory
New-Item -ItemType Directory -Path "src/main/java/org/gripday/userservice/passwordmanagement" -Force

# Move files (update package to org.gripday.userservice.passwordmanagement)
# - domain/service/PasswordResetService.java → passwordmanagement/PasswordResetService.java
# - presentation/web/PasswordResetResource.java → passwordmanagement/PasswordResetController.java
# - presentation/dto/ForgotPasswordRequest.java → passwordmanagement/ForgotPasswordRequest.java
# - presentation/dto/ResetPasswordRequest.java → passwordmanagement/ResetPasswordRequest.java
# - presentation/dto/ChangePasswordRequest.java → passwordmanagement/ChangePasswordRequest.java
```

#### H. User Management Context (Priority 3)
```powershell
# Already partially created, complete it:
# - domain/service/UserManagementService.java → usermanagement/UserManagementService.java
# - presentation/web/admin/UserManagementResource.java → usermanagement/UserManagementController.java
# - presentation/web/UserProfileResource.java → usermanagement/UserProfileController.java
# - presentation/dto/UserDto.java → usermanagement/UserDto.java
# - presentation/dto/UserContext.java → usermanagement/UserContext.java
# - presentation/dto/CreateUserRequest.java → usermanagement/CreateUserRequest.java
# - presentation/dto/UpdateUserRequest.java → usermanagement/UpdateUserRequest.java
```

#### I. Organization Context (Priority 3)
```powershell
# Create directory
New-Item -ItemType Directory -Path "src/main/java/org/gripday/userservice/organization" -Force

# Move files (update package to org.gripday.userservice.organization)
# - infrastructure/entity/Organization.java → organization/Organization.java
# - infrastructure/repository/OrganizationRepository.java → organization/OrganizationRepository.java
# - domain/service/OrganizationManagementService.java → organization/OrganizationService.java (rename class!)
# - presentation/web/admin/OrganizationManagementResource.java → organization/OrganizationController.java
# - presentation/dto/OrganizationDto.java → organization/OrganizationDto.java
# - presentation/dto/CreateOrganizationRequest.java → organization/CreateOrganizationRequest.java
# - presentation/dto/UpdateOrganizationRequest.java → organization/UpdateOrganizationRequest.java
```

### Step 3: Update Package Declarations

For each moved file, update the package declaration:
```java
// Old
package org.gripday.userservice.domain.service;

// New
package org.gripday.userservice.authentication;
```

### Step 4: Update Imports

Use Find & Replace across the project:

```
Find: import org.gripday.userservice.infrastructure.entity.User;
Replace: import org.gripday.userservice.usermanagement.User;

Find: import org.gripday.userservice.infrastructure.entity.TenantAwareEntity;
Replace: import org.gripday.userservice.shared.TenantAware;

Find: import org.gripday.userservice.domain.service.AuthenticationService;
Replace: import org.gripday.userservice.authentication.AuthenticationService;

Find: import org.gripday.userservice.domain.service.JwtService;
Replace: import org.gripday.userservice.authentication.JwtTokenService;

... (see DDD-REFACTORING-PLAN.md for complete list)
```

### Step 5: Update Class Names

Some classes need renaming:
- `TenantAwareEntity` → `TenantAware`
- `EmailVerificationToken` → `VerificationToken`
- `UserRegistrationService` → `RegistrationService`
- `JwtService` → `JwtTokenService`
- `*Resource` → `*Controller`

### Step 6: Update Tests

Apply the same refactoring to test files:
```powershell
# Update test structure to match main structure
src/test/java/org/gripday/userservice/
├── authentication/
├── registration/
├── emailverification/
├── passwordmanagement/
├── usermanagement/
├── tenancy/
├── organization/
└── security/
```

### Step 7: Verify and Clean Up

```powershell
# Compile
mvn clean compile

# Run tests
mvn test

# Delete old empty directories
Remove-Item -Recurse -Force src/main/java/org/gripday/userservice/domain
Remove-Item -Recurse -Force src/main/java/org/gripday/userservice/infrastructure
Remove-Item -Recurse -Force src/main/java/org/gripday/userservice/presentation

# Delete old test directories
Remove-Item -Recurse -Force src/test/java/org/gripday/userservice/domain
Remove-Item -Recurse -Force src/test/java/org/gripday/userservice/infrastructure
Remove-Item -Recurse -Force src/test/java/org/gripday/userservice/presentation
```

## Tips for Success

1. **Work in Order**: Follow the priority order (Tenancy and Security first)
2. **One Context at a Time**: Complete one bounded context before moving to the next
3. **Test Frequently**: Run `mvn compile` after each context
4. **Use IDE Refactoring**: Use IDE's "Move Class" feature when possible
5. **Commit Often**: Commit after each bounded context is complete

## Estimated Time

- **Automated (if script works perfectly)**: 30 minutes
- **Manual (careful refactoring)**: 3-4 hours
- **With testing and verification**: 4-6 hours

## Common Issues and Solutions

### Issue: Circular Dependencies
**Solution**: Tenancy and Security contexts should be moved first as they're used by others

### Issue: Import Not Found
**Solution**: Check if the class was renamed (e.g., `JwtService` → `JwtTokenService`)

### Issue: Tests Failing
**Solution**: Update test imports and package declarations

### Issue: Compilation Errors
**Solution**: Search for old package names and update them

## Verification Checklist

- [ ] All files moved to new structure
- [ ] All package declarations updated
- [ ] All imports updated
- [ ] All class names updated (where renamed)
- [ ] Tests moved and updated
- [ ] `mvn clean compile` succeeds
- [ ] `mvn test` passes
- [ ] Old directories deleted
- [ ] Code review completed
- [ ] Documentation updated

## Success Criteria

✅ Zero compilation errors
✅ All tests passing
✅ No references to old packages
✅ Old directory structure removed
✅ Code is more navigable and understandable

## Need Help?

Refer to:
- `DDD-REFACTORING-PLAN.md` - Complete file mapping
- `REFACTORING-SUMMARY.md` - Overview and benefits
- `execute-ddd-refactoring.ps1` - Automation script (if you want to enhance it)
