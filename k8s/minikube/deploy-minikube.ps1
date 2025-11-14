# Gripday Platform - Minikube Deployment Script (PowerShell)
# Automates the deployment of all services to minikube

param(
    [switch]$Build,
    [switch]$NoWait,
    [switch]$Open,
    [switch]$Help
)

# Colors for output
$ErrorColor = "Red"
$SuccessColor = "Green"
$WarningColor = "Yellow"
$InfoColor = "Cyan"

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
Usage: .\deploy-minikube.ps1 [OPTIONS]

Deploy Gripday Platform to Minikube

OPTIONS:
    -Build              Build Docker images before deploying
    -NoWait            Don't wait for pods to be ready
    -Open              Open service URLs in browser after deployment
    -Help              Show this help message

EXAMPLES:
    .\deploy-minikube.ps1                  # Deploy with existing images
    .\deploy-minikube.ps1 -Build           # Build images and deploy
    .\deploy-minikube.ps1 -Build -Open     # Build, deploy, and open services

"@
}

if ($Help) {
    Show-Usage
    exit 0
}

# Check prerequisites
Write-Header "Checking Prerequisites"

if (-not (Get-Command kubectl -ErrorAction SilentlyContinue)) {
    Write-Error "kubectl is not installed"
    exit 1
}
Write-Success "kubectl found"

if (-not (Get-Command minikube -ErrorAction SilentlyContinue)) {
    Write-Error "minikube is not installed"
    exit 1
}
Write-Success "minikube found"

$minikubeStatus = minikube status 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Error "minikube is not running. Start it with: minikube start"
    exit 1
}
Write-Success "minikube is running"

# Get minikube info
$MINIKUBE_IP = minikube ip
Write-Info "Minikube IP: $MINIKUBE_IP"

# Build Docker images if requested
if ($Build) {
    Write-Header "Building Docker Images"
    
    # Check if we can build from this directory
    $PROJECT_ROOT = (Get-Item (Join-Path $PSScriptRoot "..\..")).FullName
    
    if (-not (Test-Path (Join-Path $PROJECT_ROOT "pom.xml"))) {
        Write-Error "Cannot find project root. Please run from k8s\minikube directory"
        exit 1
    }
    
    Write-Info "Project root: $PROJECT_ROOT"
    Write-Info "Setting Docker environment to use minikube..."
    
    # Set docker environment
    $env:DOCKER_TLS_VERIFY = "1"
    $env:DOCKER_HOST = (minikube docker-env | Select-String "DOCKER_HOST" | ForEach-Object { $_ -replace '.*="(.*)".*', '$1' })
    $env:DOCKER_CERT_PATH = (minikube docker-env | Select-String "DOCKER_CERT_PATH" | ForEach-Object { $_ -replace '.*="(.*)".*', '$1' })
    $env:MINIKUBE_ACTIVE_DOCKERD = "minikube"
    
    Push-Location $PROJECT_ROOT
    
    # Build User Service
    Write-Info "Building User Service..."
    docker build -t gripday/user-service:latest -f gripday-user-service/Dockerfile .
    if ($LASTEXITCODE -eq 0) {
        Write-Success "User Service image built"
    } else {
        Write-Error "Failed to build User Service image"
        Pop-Location
        exit 1
    }
    
    # Build Gateway Service
    Write-Info "Building Gateway Service..."
    docker build -t gripday/gateway-service:latest -f gripday-gateway-service/Dockerfile .
    if ($LASTEXITCODE -eq 0) {
        Write-Success "Gateway Service image built"
    } else {
        Write-Error "Failed to build Gateway Service image"
        Pop-Location
        exit 1
    }
    
    # Build Bookstore Service
    Write-Info "Building Bookstore Service..."
    docker build -t gripday/bookstore-service:latest -f gripday-bookstore-service/Dockerfile .
    if ($LASTEXITCODE -eq 0) {
        Write-Success "Bookstore Service image built"
    } else {
        Write-Error "Failed to build Bookstore Service image"
        Pop-Location
        exit 1
    }
    
    Pop-Location
    Write-Success "All images built successfully"
}

# Deploy to Kubernetes
Write-Header "Deploying to Minikube"

Write-Info "Applying Kubernetes manifests..."
kubectl apply -f all-in-one.yaml
if ($LASTEXITCODE -eq 0) {
    Write-Success "Manifests applied successfully"
} else {
    Write-Error "Failed to apply manifests"
    exit 1
}

# Wait for pods to be ready
if (-not $NoWait) {
    Write-Header "Waiting for Pods to be Ready"
    
    Write-Info "Waiting for databases..."
    kubectl wait --for=condition=ready pod -l app=postgres-user -n gripday --timeout=300s 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) { Write-Warn "Postgres Auth not ready" }
    
    kubectl wait --for=condition=ready pod -l app=postgres-bookstore -n gripday --timeout=300s 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) { Write-Warn "Postgres Bookstore not ready" }
    
    kubectl wait --for=condition=ready pod -l app=redis -n gripday --timeout=300s 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) { Write-Warn "Redis not ready" }
    
    Write-Info "Waiting for microservices..."
    kubectl wait --for=condition=ready pod -l app=user-service -n gripday --timeout=300s 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) { Write-Warn "User Service not ready" }
    
    kubectl wait --for=condition=ready pod -l app=bookstore-service -n gripday --timeout=300s 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) { Write-Warn "Bookstore Service not ready" }
    
    kubectl wait --for=condition=ready pod -l app=gateway-service -n gripday --timeout=300s 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) { Write-Warn "Gateway Service not ready" }
    
    Write-Success "All pods are ready"
}

# Display deployment status
Write-Header "Deployment Status"

Write-Host ""
kubectl get pods -n gripday
Write-Host ""
kubectl get services -n gripday
Write-Host ""

# Get service URLs
Write-Header "Service Access Information"

$GATEWAY_URL = "http://${MINIKUBE_IP}:30080"
$USER_URL = "http://${MINIKUBE_IP}:30081"
$BOOKSTORE_URL = "http://${MINIKUBE_IP}:30082"

Write-Host ""
Write-Host "Gateway Service:   " -NoNewline -ForegroundColor Green
Write-Host $GATEWAY_URL
Write-Host "User Service:      " -NoNewline -ForegroundColor Green
Write-Host $USER_URL
Write-Host "Bookstore Service: " -NoNewline -ForegroundColor Green
Write-Host $BOOKSTORE_URL
Write-Host ""

# Test health endpoints
Write-Header "Testing Health Endpoints"

Start-Sleep -Seconds 5

function Test-Health {
    param(
        [string]$Name,
        [string]$Url
    )
    
    try {
        $response = Invoke-WebRequest -Uri "$Url/actuator/health" -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
        if ($response.StatusCode -eq 200) {
            Write-Success "$Name is healthy"
            return $true
        }
    } catch {
        Write-Warn "$Name health check failed (may still be starting)"
        return $false
    }
}

Test-Health -Name "Gateway" -Url $GATEWAY_URL
Test-Health -Name "Auth" -Url $USER_URL
Test-Health -Name "Bookstore" -Url $BOOKSTORE_URL

# Show example commands
Write-Header "Quick Start Commands"

@"
Test the API:

1. Register a user:
   `$body = @{
       username = "testuser"
       email = "test@example.com"
       password = "TestPass123!"
       firstName = "Test"
       lastName = "User"
   } | ConvertTo-Json
   
   Invoke-RestMethod -Uri "$GATEWAY_URL/api/v1/auth/signup" ``
     -Method Post ``
     -ContentType "application/json" ``
     -Body `$body

2. View logs:
   kubectl logs -f deployment/gateway-service -n gripday

3. Access pods:
   kubectl get pods -n gripday

4. Port forward (alternative to NodePort):
   kubectl port-forward -n gripday svc/gateway-service 8080:8080

"@

# Open services in browser if requested
if ($Open) {
    Write-Header "Opening Services"
    Start-Process "$GATEWAY_URL/actuator/health"
}

Write-Header "Deployment Complete!"

Write-Success "Gripday Platform is running on minikube"
Write-Info "Use 'kubectl get all -n gripday' to see all resources"
Write-Info "Use '.\cleanup-minikube.ps1' to remove everything"

Write-Host ""
