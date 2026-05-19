# Platform Demo All-in-One Launcher (Windows PowerShell)
# This script starts the entire platform stack for demonstration purposes.

Write-Host "Starting Platform Demo Stack..." -ForegroundColor Cyan

# Check for docker compose
if (Get-Command "docker" -ErrorAction SilentlyContinue) {
    # Run the demo stack
    docker compose -f compose.demo.yaml up -d --remove-orphans
} else {
    Write-Error "Docker not found. Please install Docker Desktop."
    exit 1
}

Write-Host ""
Write-Host "Platform is starting!" -ForegroundColor Green
Write-Host "--------------------------------------------------"
Write-Host "Gateway Service: http://api.iqkv.local/"
Write-Host "Admin UI:        http://admin.iqkv.local/"
Write-Host "App UI:          http://app.iqkv.local/"
Write-Host "--------------------------------------------------"
Write-Host "Services Monitoring (Subpaths):"
Write-Host "RabbitMQ:        http://api.iqkv.local/services/rabbitmq/"
Write-Host "Grafana:         http://api.iqkv.local/services/grafana/"
Write-Host "Prometheus:      http://api.iqkv.local/services/prometheus/"
Write-Host "MailHog:         http://api.iqkv.local/services/mailhog/"
Write-Host "--------------------------------------------------"
Write-Host "Note: Ensure you have added the domains to your C:\Windows\System32\drivers\etc\hosts file." -ForegroundColor Yellow
