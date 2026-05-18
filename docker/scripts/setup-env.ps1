# Check if .env already exists
if (Test-Path .env) {
    Write-Host ".env file already exists. Skipping copy." -ForegroundColor Cyan
} else {
    if (Test-Path .env.example) {
        Copy-Item .env.example .env
        Write-Host ".env file created from .env.example" -ForegroundColor Green
    } else {
        Write-Error "Error: .env.example not found."
    }
}
