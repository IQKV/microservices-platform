# DDD Refactoring Script
# This script moves files from three-tier architecture to DDD bounded contexts

$ErrorActionPreference = "Stop"

Write-Host "Starting DDD refactoring..." -ForegroundColor Green

# Define the mapping of old paths to new paths
$fileMappings = @{
    # Shared Kernel
    "src/main/java/org/gripday/userservice/infrastructure/entity/Authority.java" = "src/main/java/org/gripday/userservice/shared/Authority.java"
    "src/main/java/org/gripday/userservice/infrastructure/entity/TenantAware.java" = "src/main/java/org/gripday/userservice/shared/TenantAware.java"
    "src/main/java/org/gripday/userservice/infrastructure/repository/AuthorityRepository.java" = "src/main/java/org/gripday/userservice/shared/AuthorityRepository.java"
    
    # User Management Bounded Context
    "src/main/java/org/gripday/userservice/infrastructure/entity/User.java" = "src/main/java/org/gripday/userservice/usermanagement/User.java"
    "src/main/java/org/gripday/userservice/infrastructure/repository/UserRepository.java" = "src/main/java/org/gripday/userservice/usermanagement/UserRepository.java"
    "src/main/java/org/gripday/userservice/domain/service/UserManagementService.java" = "src/main/java/org/gripday/userservice/usermanagement/UserManagementService.java"
    "src/main/java/org/gripday/userservice/presentation/web/admin/UserManagementResource.java" = "src/main/java/org/gripday/userservice/usermanagement/UserManagementRestResource.java"
    "src/main/java/org/gripday/userservice/presentation/dto/UserDto.java" = "src/main/java/org/gripday/userservice/usermanagement/UserDto.java"
    "src/main/java/org/gripday/userservice/presentation/dto/CreateUserRequest.java" = "src/main/java/org/gripday/userservice/usermanagement/CreateUserRequest.java"
    "src/main/java/org/gripday/userservice/presentation/dto/UpdateUserRequest.java" = "src/main/java/org/gripday/userservice/usermanagement/UpdateUserRequest.java"
    "src/main/java/org/gripday/userservice/presentation/web/UserProfileResource.java" = "src/main/java/org/gripday/userservice/usermanagement/UserProfileRestResource.java"
    "src/main/java/org/gripday/userservice/presentation/dto/UserContext.java" = "src/main/java/org/gripday/userservice/usermanagement/UserContext.java"
    
    # Authentication Bounded Context
    "src/main/java/org/gripday/userservice/domain/service/AuthenticationService.java" = "src/main/java/org/gripday/userservice/authentication/AuthenticationService.java"
    "src/main/java/org/gripday/userservice/domain/service/JwtService.java" = "src/main/java/org/gripday/userservice/authentication/JwtTokenService.java"
    "src/main/java/org/gripday/userservice/domain/service/JwtKeyManagementService.java" = "src/main/java/org/gripday/userservice/authentication/JwtKeyManagementService.java"
    "src/main/java/org/gripday/userservice/presentation/web/AuthenticationResource.java" = "src/main/java/org/gripday/userservice/authentication/AuthenticationRestResource.java"
    "src/main/java/org/gripday/userservice/presentation/web/JwkSetResource.java" = "src/main/java/org/gripday/userservice/authentication/JwkSetRestResource.java"
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
    "src/main/java/org/gripday/userservice/presentation/web/EmailVerificationResource.java" = "src/main/java/org/gripday/userservice/emailverification/VerificationRestResource.java"
    "src/main/java/org/gripday/userservice/presentation/dto/EmailVerificationResponse.java" = "src/main/java/org/gripday/userservice/emailverification/VerificationResponse.java"
    "src/main/java/org/gripday/userservice/presentation/dto/ResendVerificationRequest.java" = "src/main/java/org/gripday/userservice/emailverification/ResendVerificationRequest.java"
    "src/main/java/org/gripday/userservice/presentation/dto/VerificationStatusResponse.java" = "src/main/java/org/gripday/userservice/emailverification/VerificationStatusResponse.java"
    
    # Password Management Bounded Context
    "src/main/java/org/gripday/userservice/domain/service/PasswordResetService.java" = "src/main/java/org/gripday/userservice/passwordmanagement/PasswordResetService.java"
    "src/main/java/org/gripday/userservice/presentation/web/PasswordResetResource.java" = "src/main/java/org/gripday/userservice/passwordmanagement/PasswordResetRestResource.java"
    "src/main/java/org/gripday/userservice/presentation/dto/ForgotPasswordRequest.java" = "src/main/java/org/gripday/userservice/passwordmanagement/ForgotPasswordRequest.java"
    "src/main/java/org/gripday/userservice/presentation/dto/ResetPasswordRequest.java" = "src/main/java/org/gripday/userservice/passwordmanagement/ResetPasswordRequest.java"
    "src/main/java/org/gripday/userservice/presentation/dto/ChangePasswordRequest.java" = "src/main/java/org/gripday/userservice/passwordmanagement/ChangePasswordRequest.java"
    
    # Tenancy Bounded Context
    "src/main/java/org/gripday/userservice/infrastructure/entity/Tenant.java" = "src/main/java/org/gripday/userservice/tenancy/Tenant.java"
    "src/main/java/org/gripday/userservice/infrastructure/repository/TenantRepository.java" = "src/main/java/org/gripday/userservice/tenancy/TenantRepository.java"
    "src/main/java/org/gripday/userservice/domain/service/TenantContext.java" = "src/main/java/org/gripday/userservice/tenancy/TenantContext.java"
    "src/main/java/org/gripday/userservice/domain/service/TenantManagementService.java" = "src/main/java/org/gripday/userservice/tenancy/TenantService.java"
    "src/main/java/org/gripday/userservice/domain/service/TenantExtractionService.java" = "src/main/java/org/gripday/userservice/tenancy/TenantExtractionService.java"
    "src/main/java/org/gripday/userservice/presentation/web/admin/TenantManagementResource.java" = "src/main/java/org/gripday/userservice/tenancy/TenantManagementRestResource.java"
    "src/main/java/org/gripday/userservice/config/TenantExtractionFilter.java" = "src/main/java/org/gripday/userservice/tenancy/TenantExtractionFilter.java"
    "src/main/java/org/gripday/userservice/config/TenantConfig.java" = "src/main/java/org/gripday/userservice/tenancy/TenantConfig.java"
    
    # Organization Bounded Context
    "src/main/java/org/gripday/userservice/infrastructure/entity/Organization.java" = "src/main/java/org/gripday/userservice/organization/Organization.java"
    "src/main/java/org/gripday/userservice/infrastructure/repository/OrganizationRepository.java" = "src/main/java/org/gripday/userservice/organization/OrganizationRepository.java"
    "src/main/java/org/gripday/userservice/domain/service/OrganizationManagementService.java" = "src/main/java/org/gripday/userservice/organization/OrganizationService.java"
    "src/main/java/org/gripday/userservice/presentation/web/admin/OrganizationManagementResource.java" = "src/main/java/org/gripday/userservice/organization/OrganizationRestResource.java"
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

# Function to create directory if it doesn't exist
function Ensure-Directory {
    param([string]$Path)
    $dir = Split-Path $Path -Parent
    if (!(Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
        Write-Host "Created directory: $dir" -ForegroundColor Gray
    }
}

# Function to update package declaration in a Java file
function Update-PackageDeclaration {
    param(
        [string]$FilePath,
        [string]$OldPackage,
        [string]$NewPackage
    )
    
    if (Test-Path $FilePath) {
        $content = Get-Content $FilePath -Raw
        $packagePattern = "^package\s+$([regex]::Escape($OldPackage))\s*;"
        $newDeclaration = "package $NewPackage;"
        
        if ($content -match $packagePattern) {
            $content = $content -replace $packagePattern, $newDeclaration
            
            $retryCount = 0
            $maxRetries = 3
            $success = $false
            
            while ($retryCount -lt $maxRetries -and !$success) {
                try {
                    Set-Content $FilePath $content -NoNewline -ErrorAction Stop
                    $success = $true
                    Write-Host "Updated package in: $(Split-Path $FilePath -Leaf)" -ForegroundColor Cyan
                }
                catch {
                    $retryCount++
                    if ($retryCount -lt $maxRetries) {
                        Write-Host "File locked, retrying in 1 second... (Attempt $retryCount/$maxRetries)" -ForegroundColor Yellow
                        Start-Sleep -Seconds 1
                    } else {
                        Write-Host "ERROR: Could not update package in $(Split-Path $FilePath -Leaf) after $maxRetries attempts" -ForegroundColor Red
                        Write-Host "  File may be locked by IDE or build tool. Please close the file and try again." -ForegroundColor Yellow
                    }
                }
            }
        }
    }
}

# Function to extract package name from file path
function Get-PackageFromPath {
    param([string]$FilePath)
    $relativePath = $FilePath -replace "^src/main/java/", ""
    $packagePath = Split-Path $relativePath -Parent
    return $packagePath -replace "[\\/]", "."
}

# Track operations for potential rollback
$performedMoves = @()

# Function to rollback moves
function Invoke-Rollback {
    param([array]$MoveLog)
    
    Write-Host "`nStarting rollback..." -ForegroundColor Red
    
    # Process moves in reverse order
    for ($i = $MoveLog.Count - 1; $i -ge 0; $i--) {
        $move = $MoveLog[$i]
        $sourcePath = $move.Source
        $targetPath = $move.Target
        
        Write-Host "Rolling back: $targetPath -> $sourcePath" -ForegroundColor Yellow
        
        try {
            if (Test-Path $targetPath) {
                Move-Item $targetPath $sourcePath -Force
                Write-Host "ROLLED BACK: $targetPath -> $sourcePath" -ForegroundColor Green
            } else {
                Write-Host "WARNING: Target file not found for rollback: $targetPath" -ForegroundColor Yellow
            }
        }
        catch {
            Write-Host "ERROR: Failed to rollback $targetPath : $_" -ForegroundColor Red
        }
    }
    
    Write-Host "Rollback completed." -ForegroundColor Red
}

# Check for rollback parameter
if ($args -contains "-rollback") {
    $logFile = Get-ChildItem -Path "." -Name "refactor-log-*.json" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    
    if ($logFile) {
        Write-Host "Found log file: $logFile" -ForegroundColor Yellow
        $rollbackData = Get-Content $logFile | ConvertFrom-Json
        Invoke-Rollback $rollbackData
        exit
    } else {
        Write-Host "No rollback log file found." -ForegroundColor Red
        exit 1
    }
}

# Check for dry-run parameter
$dryRun = $args -contains "-dryrun"
if ($dryRun) {
    Write-Host "DRY RUN MODE - No files will be moved" -ForegroundColor Yellow
}

# Function to update import statements
function Update-ImportStatements {
    param(
        [string]$FilePath,
        [hashtable]$PackageMappings
    )
    
    if (!(Test-Path $FilePath)) { return }
    
    $content = Get-Content $FilePath -Raw
    $updated = $false
    
    foreach ($mapping in $PackageMappings.GetEnumerator()) {
        $oldImport = "import $($mapping.Key)"
        $newImport = "import $($mapping.Value)"
        
        if ($content -match [regex]::Escape($oldImport)) {
            $content = $content -replace [regex]::Escape($oldImport), $newImport
            $updated = $true
        }
    }
    
    if ($updated) {
        $retryCount = 0
        $maxRetries = 3
        $success = $false
        
        while ($retryCount -lt $maxRetries -and !$success) {
            try {
                Set-Content $FilePath $content -NoNewline -ErrorAction Stop
                $success = $true
                Write-Host "Updated imports in: $(Split-Path $FilePath -Leaf)" -ForegroundColor Cyan
            }
            catch {
                $retryCount++
                if ($retryCount -lt $maxRetries) {
                    Write-Host "File locked, retrying in 1 second... (Attempt $retryCount/$maxRetries)" -ForegroundColor Yellow
                    Start-Sleep -Seconds 1
                } else {
                    Write-Host "ERROR: Could not update imports in $(Split-Path $FilePath -Leaf) after $maxRetries attempts" -ForegroundColor Red
                    Write-Host "  File may be locked by IDE or build tool. Please close the file and try again." -ForegroundColor Yellow
                }
            }
        }
    }
}

Write-Host "`nStarting file moves..." -ForegroundColor Yellow

# Generate package mappings for import updates
$packageMappings = @{}
foreach ($mapping in $fileMappings.GetEnumerator()) {
    $oldPackage = Get-PackageFromPath $mapping.Key
    $newPackage = Get-PackageFromPath $mapping.Value
    $className = [System.IO.Path]::GetFileNameWithoutExtension($mapping.Key)
    
    if ($oldPackage -ne $newPackage) {
        $packageMappings["$oldPackage.$className"] = "$newPackage.$className"
    }
}

foreach ($mapping in $fileMappings.GetEnumerator()) {
    $sourcePath = $mapping.Key
    $targetPath = $mapping.Value
    
    Write-Host "Processing: $($mapping.Name)" -ForegroundColor White
    
    # Check if source file exists
    if (!(Test-Path $sourcePath)) {
        Write-Host "WARNING: Source file not found: $sourcePath" -ForegroundColor Red
        continue
    }
    
    if ($dryRun) {
        Write-Host "DRY RUN: Would move $sourcePath -> $targetPath" -ForegroundColor Cyan
        continue
    }
    
    # Ensure target directory exists
    try {
        Ensure-Directory $targetPath
    }
    catch {
        Write-Host "ERROR: Failed to create directory for $targetPath : $_" -ForegroundColor Red
        continue
    }
    
    # Check if target file already exists
    if (Test-Path $targetPath) {
        Write-Host "WARNING: Target file already exists: $targetPath" -ForegroundColor Yellow
        $choice = Read-Host "Overwrite? (y/N)"
        if ($choice -ne "y") {
            Write-Host "Skipped: $sourcePath" -ForegroundColor Gray
            continue
        }
    }
    
    # Perform the move
    try {
        Move-Item $sourcePath $targetPath -Force
        $performedMoves += @{ Source = $sourcePath; Target = $targetPath }
        Write-Host "MOVED: $sourcePath -> $targetPath" -ForegroundColor Green
        
        # Update package declaration
        $oldPackage = Get-PackageFromPath $sourcePath
        $newPackage = Get-PackageFromPath $targetPath
        
        if ($oldPackage -ne $newPackage) {
            Update-PackageDeclaration $targetPath $oldPackage $newPackage
        }
    }
    catch {
        Write-Host "ERROR: Failed to move $sourcePath to $targetPath : $_" -ForegroundColor Red
    }
}

Write-Host "`nFile moves completed. Total files moved: $($performedMoves.Count)" -ForegroundColor Green

# Update import statements in all Java files if not in dry-run mode
if (!$dryRun -and $performedMoves.Count -gt 0) {
    Write-Host "`nUpdating import statements..." -ForegroundColor Yellow
    
    # Get all Java files recursively
    $javaFiles = Get-ChildItem -Path "src/main/java" -Filter "*.java" -Recurse
    
    foreach ($file in $javaFiles) {
        Update-ImportStatements $file.FullName $packageMappings
    }
    
    Write-Host "Import updates completed." -ForegroundColor Green
}

# Summary of operations
Write-Host "`n=== SUMMARY ===" -ForegroundColor Cyan
Write-Host "Files processed: $($fileMappings.Count)" -ForegroundColor White
Write-Host "Files moved: $($performedMoves.Count)" -ForegroundColor Green
Write-Host "Files skipped/failed: $($fileMappings.Count - $performedMoves.Count)" -ForegroundColor Yellow

# Save operations log for potential rollback (only if actual moves were made)
if (!$dryRun -and $performedMoves.Count -gt 0) {
    $logPath = "refactor-log-$(Get-Date -Format 'yyyyMMdd-HHmmss').json"
    $performedMoves | ConvertTo-Json -Depth 2 | Set-Content $logPath
    Write-Host "Operations log saved to: $logPath" -ForegroundColor Gray
}

Write-Host "`nRefactoring complete!" -ForegroundColor Green      
Write-Host "Usage:" -ForegroundColor Yellow
Write-Host "  Run normally: .\refactor-ddd.ps1" -ForegroundColor White
Write-Host "  Dry run mode: .\refactor-ddd.ps1 -dryrun" -ForegroundColor White
Write-Host "  Rollback last run: .\refactor-ddd.ps1 -rollback" -ForegroundColor White
Write-Host "`nNext steps:" -ForegroundColor Yellow
Write-Host "1. Run Maven build to identify remaining issues" -ForegroundColor White
Write-Host "2. Manually update any remaining import statements" -ForegroundColor White
Write-Host "3. Run tests to verify everything works" -ForegroundColor White
if (!$dryRun -and $performedMoves.Count -gt 0) {
    Write-Host "4. Use rollback if needed: .\refactor-ddd.ps1 -rollback" -ForegroundColor White
}
