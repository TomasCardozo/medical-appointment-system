Write-Host "Starting Medical Appointment System (PowerShell)"

# Copy env if not exists
if (-Not (Test-Path ".env")) {
    Write-Host "Creating .env from .env.example"
    Copy-Item .env.example .env
}

Write-Host "Pulling images..."
docker compose pull

Write-Host "Starting services..."
docker compose up -d

Write-Host "System is running!"