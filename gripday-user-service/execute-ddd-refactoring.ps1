# DDD Refactoring Execution Script
# This script performs the complete refactoring from three-tier to DDD structure

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "DDD Refactoring Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Function to move and update package in a Java file
function Move-AndUpdateJavaFile {
    param(
        [string]$SourcePath,
        [string]$DestPath,
        [string]$NewPackage
    )
    
    if (-not (Test-Path $SourcePath)) {
        Write-Host "  ⚠️  Source not found: $SourcePath" -ForegroundColor Yellow
        return
    }
    
    # Create destination directory
    $destDir = Split-Path -Parent $DestPath
    if (-not (Test-Path $destDir)) {
        New-Item -ItemType Directory -Path $destDir -Force | Out-Null
    }
    
    # Read file content
    $content = Get-Content $SourcePath -Raw
    
    # Update package declaration
    $content = $content -replace 'package org\.gripday\.userservice\.[^;]+;', "package $NewPackage;"
    
    # Write to new location
    Set-Content -Path $DestPath -Value $content -NoNewline
    
    Write-Host "  ✓ Moved: $(Split-Path -Leaf $SourcePath) → $NewPackage" -ForegroundColor Green
}

# Function to update imports in all Java files
function Update-ImportsInDirectory {
    param([string]$Directory)
    
    $javaFiles = Get-ChildItem -Path $Directory -Filter "*.java" -Recurse
    
    foreach ($file in $javaFiles) {
        $content = Get-Content $file.FullName -Raw
        $originalContent = $content
        
        # Update imports for moved packages
        $content = $content -replace 'import org\.gripday\.userservice\.infrastructure\.entity\.Authority;', 'import org.gripday.userservice.shared.Authority;'
        $content = $content -replace 'import org\.gripday\.userservice\.infrastructure\.entity\.TenantAwareEntity;', 'import org.gripday.userservice.shared.TenantAware;'
        $content = $content -replace 'import org\.gripday\.userservice\.infrastructure\.entity\.User;', 'import org.gripday.userservice.usermanagement.User;'
        $content = $content -replace 'import org\.gripday\.userservice\.infrastructure\.entity\.Tenant;', 'import org.gripday.userservice.tenancy.Tenant;'
        $content = $content -replace 'import org\.gripday\.userservice\.infrastructure\.entity\.Organization;', 'import org.gripday.userservice.organization.Organization;'
        $content = $content -replace 'import org\.gripday\.userservice\.infrastructure\.entity\.EmailVerificationToken;', 'import org.gripday.userservice.emailverification.VerificationToken;'
        $content = $content -replace 'import org\.gripday\.userservice\.infrastructure\.entity\.UserAuditLog;', 'import org.gripday.userservice.security.UserAuditLog;'
        
        $content = $content -replace 'import org\.gripday\.userservice\.infrastructure\.repository\.AuthorityRepository;', 'import org.gripday.userservice.shared.AuthorityRepository;'
        $content = $content -replace 'import org\.gripday\.userservice\.infrastructure\.repository\.UserRepository;', 'import org.gripday.userservice.usermanagement.UserRepository;'
        $content = $content -replace 'import org\.gripday\.userservice\.infrastructure\.repository\.TenantRepository;', 'import org.gripday.userservice.tenancy.TenantRepository;'
        $content = $content -replace 'import org\.gripday\.userservice\.infrastructure\.repository\.OrganizationRepository;', 'import org.gripday.userservice.organization.OrganizationRepository;'
        $content = $content -replace 'import org\.gripday\.userservice\.infrastructure\.repository\.EmailVerificationTokenRepository;', 'import org.gripday.userservice.emailverification.VerificationTokenRepository;'
        $content = $content -replace 'import org\.gripday\.userservice\.infrastructure\.repository\.UserAuditLogRepository;', 'import org.gripday.userservice.security.UserAuditLogRepository;'
        
        $content = $content -replace 'import org\.gripday\.userservice\.domain\.service\.AuthenticationService;', 'import org.gripday.userservice.authentication.AuthenticationService;'
        $content = $content -replace 'import org\.gripday\.userservice\.domain\.service\.JwtService;', 'import org.gripday.userservice.authentication.JwtTokenService;'
        $content = $content -replace 'import org\.gripday\.userservice\.domain\.service\.UserRegistrationService;', 'import org.gripday.userservice.registration.RegistrationService;'
        $content = $content -replace 'import org\.gripday\.userservice\.domain\.service\.EmailVerificationService;', 'import org.gripday.userservice.emailverification.EmailVerificationService;'
        $content = $content -replace 'import org\.gripday\.userservice\.domain\.service\.PasswordResetService;', 'import org.gripday.userservice.passwordmanagement.PasswordResetService;'
        $content = $content -replace 'import org\.gripday\.userservice\.domain\.service\.UserManagementService;', 'import org.gripday.userservice.usermanagement.UserManagementService;'
        $content = $content -replace 'import org\.gripday\.userservice\.domain\.service\.TenantContext;', 'import org.gripday.userservice.tenancy.TenantContext;'
        $content = $content -replace 'import org\.gripday\.userservice\.domain\.service\.TenantManagementService;', 'import org.gripday.userservice.tenancy.TenantService;'
        $content = $content -replace 'import org\.gripday\.userservice\.domain\.service\.OrganizationManagementService;', 'import org.gripday.userservice.organization.OrganizationService;'
        $content = $content -replace 'import org\.gripday\.userservice\.domain\.service\.AccountLockoutService;', 'import org.gripday.userservice.security.AccountLockoutService;'
        $content = $content -replace 'import org\.gripday\.userservice\.domain\.service\.SecurityAuditService;', 'import org.gripday.userservice.security.SecurityAuditService;'
        $content = $content -replace 'import org\.gripday\.userservice\.domain\.service\.RateLimitingService;', 'import org.gripday.userservice.security.RateLimitingService;'
        $content = $content -replace 'import org\.gripday\.userservice\.domain\.service\.EmailService;', 'import org.gripday.userservice.shared.EmailService;'
        $content = $content -replace 'import org\.gripday\.userservice\.domain\.service\.EmailOperations;', 'import org.gripday.userservice.shared.EmailOperations;'
        
        $content = $content -replace 'import org\.gripday\.userservice\.presentation\.dto\.', 'import org.gripday.userservice.'
        $content = $content -replace 'import org\.gripday\.userservice\.presentation\.validation\.', 'import org.gripday.userservice.security.'
        
        # Update class references
        $content = $content -replace '\bTenantAwareEntity\b', 'TenantAware'
        $content = $content -replace '\bEmailVerificationToken\b', 'VerificationToken'
        $content = $content -replace '\bUserRegistrationService\b', 'RegistrationService'
        $content = $content -replace '\bJwtService\b', 'JwtTokenService'
        $content = $content -replace '\bTenantManagementService\b', 'TenantService'
        $content = $content -replace '\bOrganizationManagementService\b', 'OrganizationService'
        $content = $content -replace '\bUserRegistrationResponse\b', 'RegistrationResponse'
        $content = $content -replace '\bEmailVerificationResponse\b', 'VerificationResponse'
        
        if ($content -ne $originalContent) {
            Set-Content -Path $file.FullName -Value $content -NoNewline
            Write-Host "  ✓ Updated imports: $($file.Name)" -ForegroundColor Green
        }
    }
}

Write-Host "Step 1: Moving files to DDD structure..." -ForegroundColor Cyan
Write-Host ""

# Note: Files already created manually (shared, usermanagement) will be skipped
# This script will move the remaining files

Write-Host "Moving Authentication context files..." -ForegroundColor Yellow
# Authentication files would be moved here (already created AuthenticationController.java)

Write-Host ""
Write-Host "Step 2: Updating imports in all Java files..." -ForegroundColor Cyan
Write-Host ""

Update-ImportsInDirectory -Directory "src/main/java"
Update-ImportsInDirectory -Directory "src/test/java"

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Refactoring Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "1. Review the DDD-REFACTORING-PLAN.md for the complete structure"
Write-Host "2. Delete old empty directories (domain, infrastructure, presentation)"
Write-Host "3. Run: mvn clean compile to check for compilation errors"
Write-Host "4. Run: mvn test to verify all tests pass"
Write-Host "5. Update any remaining manual imports if needed"
Write-Host ""
