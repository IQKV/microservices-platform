# Gripday Platform - Minikube Cleanup Script (PowerShell)
# Removes all deployed resources from minikube

param(
    [switch]$Force,
    [switch]$Help
)

function Write-Header {
    param([string]$Message)
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host $Message -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
}

function Write-Success {
    param([string]$Message)
    Write-Host "✓ $Message" -ForegroundColor Green
}

function Write-Warn {
    param([string]$Message)
    Write-Host "⚠ $Message" -ForegroundColor Yellow
}

function Write-Error {
    param([string]$Message)
    Write-Host "✗ $Message" -ForegroundColor Red
}

function Write-Info {
    param([string]$Message)
    Write-Host "ℹ $Message" -ForegroundColor Cyan
}

function Show-Usage {
    @"
Usage: .\cleanup-minikube.ps1 [OPTIONS]

Cleanup Gripday Platform from Minikube

OPTIONS:
    -Force             Force cleanup without confirmation
    -Help              Show this help message

EXAMPLES:
    .\cleanup-minikube.ps1           # Cleanup with confirmation
    .\cleanup-minikube.ps1 -Force    # Cleanup without confirmation

"@
}

if ($Help) {
    Show-Usage
    exit 0
}

# Check prerequisites
if (-not (Get-Command kubectl -ErrorAction SilentlyContinue)) {
    Write-Error "kubectl is not installed"
    exit 1
}

if (-not (Get-Command minikube -ErrorAction SilentlyContinue)) {
    Write-Error "minikube is not installed"
    exit 1
}

$minikubeStatus = minikube status 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Warn "minikube is not running"
    exit 0
}

# Check if namespace exists
$namespaceExists = kubectl get namespace gripday 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Info "Namespace 'gripday' does not exist. Nothing to clean up."
    exit 0
}

# Show what will be deleted
Write-Header "Resources to be Deleted"

Write-Host ""
Write-Host "Namespace: " -NoNewline -ForegroundColor Yellow
Write-Host "gripday"
Write-Host ""
Write-Host "Resources in namespace:" -ForegroundColor Yellow
kubectl get all -n gripday 2>$null
Write-Host ""

# Confirm deletion
if (-not $Force) {
    Write-Host "This will delete all resources in the 'gripday' namespace." -ForegroundColor Yellow
    $confirmation = Read-Host "Are you sure you want to continue? (yes/no)"
    
    if ($confirmation -ne "yes") {
        Write-Info "Cleanup cancelled"
        exit 0
    }
}

# Delete resources
Write-Header "Cleaning Up Resources"

Write-Info "Deleting all resources in namespace 'gripday'..."

kubectl delete namespace gripday --timeout=60s
if ($LASTEXITCODE -eq 0) {
    Write-Success "Namespace 'gripday' deleted successfully"
} else {
    Write-Error "Failed to delete namespace (it may take a moment to fully terminate)"
}

# Wait for namespace deletion
Write-Info "Waiting for namespace termination..."
$timeout = 60
$counter = 0
while ((kubectl get namespace gripday 2>&1 | Out-Null; $LASTEXITCODE -eq 0) -and ($counter -lt $timeout)) {
    Start-Sleep -Seconds 2
    $counter += 2
    Write-Host "." -NoNewline
}
Write-Host ""

$namespaceCheck = kubectl get namespace gripday 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Success "Namespace fully terminated"
} else {
    Write-Warn "Namespace is still terminating (this is normal)"
}

Write-Header "Cleanup Complete"

Write-Success "All Gripday Platform resources have been removed from minikube"
Write-Info "You can redeploy with: .\deploy-minikube.ps1"

Write-Host ""
