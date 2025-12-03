# IQ Scaffold Platform - Minikube Deployment (PowerShell)
param(
    [switch]$Build,
    [switch]$NoWait,
    [switch]$Help
)

function log { Write-Host "ℹ $args" -ForegroundColor Cyan }
function ok { Write-Host "✓ $args" -ForegroundColor Green }
function warn { Write-Host "⚠ $args" -ForegroundColor Yellow }
function err { Write-Host "✗ $args" -ForegroundColor Red; exit 1 }

if ($Help) {
    @"
Usage: .\deploy-minikube.ps1 [-Build] [-NoWait]

OPTIONS:
    -Build      Build Docker images before deploying
    -NoWait     Don't wait for pods to be ready
    -Help       Show this help message
"@
    exit 0
}

if (-not (Get-Command kubectl -ErrorAction SilentlyContinue)) { err "kubectl not found" }
if (-not (Get-Command minikube -ErrorAction SilentlyContinue)) { err "minikube not found" }

$minikubeStatus = minikube status 2>&1
if ($LASTEXITCODE -ne 0) { err "minikube not running. Start with: minikube start" }

$MINIKUBE_IP = minikube ip
log "Minikube IP: $MINIKUBE_IP"

if ($Build) {
    log "Building Docker images..."
    $PROJECT_ROOT = (Get-Item (Join-Path $PSScriptRoot "..\..")).FullName
    
    if (-not (Test-Path (Join-Path $PROJECT_ROOT "pom.xml"))) {
        err "Cannot find project root"
    }
    
    # Set docker environment for minikube
    & minikube -p minikube docker-env --shell powershell | Invoke-Expression
    
    Push-Location $PROJECT_ROOT
    
    foreach ($svc in @("user", "gateway")) {
        log "Building $svc service..."
        docker build -t "iqscaffold/${svc}-service:latest" -f "iqscaffold-${svc}-service/Dockerfile" .
        if ($LASTEXITCODE -ne 0) { Pop-Location; err "Failed to build $svc service" }
    }
    
    Pop-Location
    ok "Images built"
}

log "Deploying to minikube..."
kubectl apply -f all-in-one.yaml
if ($LASTEXITCODE -ne 0) { err "Failed to apply manifests" }
ok "Manifests applied"

$ingressEnabled = minikube addons list | Select-String "ingress.*enabled"
if ($ingressEnabled) {
    log "Applying ingress..."
    kubectl apply -f ingress.yaml
    ok "Ingress applied"
    log "Add to hosts file: $MINIKUBE_IP api.iqscaffold.site user.iqscaffold.site"
} else {
    warn "Ingress addon not enabled. Enable with: minikube addons enable ingress"
}

if (-not $NoWait) {
    log "Waiting for pods..."
    
    kubectl wait --for=condition=ready pod -l app=postgres-user -n iqscaffold-dev-env --timeout=300s 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) { warn "Postgres user not ready" }
    
    kubectl wait --for=condition=ready pod -l app=redis -n iqscaffold-dev-env --timeout=300s 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) { warn "Redis not ready" }
    
    kubectl wait --for=condition=ready pod -l app=user-service -n iqscaffold-dev-env --timeout=300s 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) { warn "User service not ready" }
    
    kubectl wait --for=condition=ready pod -l app=gateway-service -n iqscaffold-dev-env --timeout=300s 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) { warn "Gateway service not ready" }
    
    ok "All pods ready"
}

Write-Host ""
kubectl get pods -n iqscaffold-dev-env
Write-Host ""

log "Service URLs:"
Write-Host "  Gateway:   http://${MINIKUBE_IP}:30080"
Write-Host "  User:      http://${MINIKUBE_IP}:30081"
Write-Host ""

if ($ingressEnabled) {
    Write-Host "  API Gateway: http://api.iqscaffold.site"
    Write-Host "  User:        http://user.iqscaffold.site"
    Write-Host ""
}

ok "Deployment complete"
log "View resources: kubectl get all -n iqscaffold-dev-env"
log "Cleanup: .\cleanup-minikube.ps1"
