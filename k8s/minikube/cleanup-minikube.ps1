# Gripday Platform - Minikube Cleanup (PowerShell)
param(
    [switch]$Force,
    [switch]$Help
)

function log { Write-Host "ℹ $args" -ForegroundColor Cyan }
function ok { Write-Host "✓ $args" -ForegroundColor Green }
function warn { Write-Host "⚠ $args" -ForegroundColor Yellow }
function err { Write-Host "✗ $args" -ForegroundColor Red; exit 1 }

if ($Help) {
    @"
Usage: .\cleanup-minikube.ps1 [-Force]

OPTIONS:
    -Force      Force cleanup without confirmation
    -Help       Show this help message
"@
    exit 0
}

if (-not (Get-Command kubectl -ErrorAction SilentlyContinue)) { err "kubectl not found" }
if (-not (Get-Command minikube -ErrorAction SilentlyContinue)) { err "minikube not found" }

$minikubeStatus = minikube status 2>&1
if ($LASTEXITCODE -ne 0) { warn "minikube not running"; exit 0 }

$namespaceExists = kubectl get namespace gripday-dev-env 2>&1
if ($LASTEXITCODE -ne 0) {
    log "Namespace doesn't exist. Nothing to clean."
    exit 0
}

log "Resources to delete:"
kubectl get all -n gripday-dev-env 2>$null
Write-Host ""

if (-not $Force) {
    $confirmation = Read-Host "Delete all resources in gripday-dev-env? (yes/no)"
    if ($confirmation -ne "yes") {
        log "Cancelled"
        exit 0
    }
}

log "Deleting namespace gripday-dev-env..."
kubectl delete namespace gripday-dev-env --timeout=60s

log "Waiting for termination..."
$timeout = 60
$counter = 0
while ((kubectl get namespace gripday-dev-env 2>&1 | Out-Null; $LASTEXITCODE -eq 0) -and ($counter -lt $timeout)) {
    Start-Sleep -Seconds 2
    $counter += 2
}

$namespaceCheck = kubectl get namespace gripday-dev-env 2>&1
if ($LASTEXITCODE -ne 0) {
    ok "Namespace terminated"
} else {
    warn "Still terminating (normal)"
}

ok "Cleanup complete"
log "Redeploy with: .\deploy-minikube.ps1"
