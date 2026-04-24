Write-Host "Starting Medical Appointment System (PowerShell)"

# Copy env if not exists
if (-Not (Test-Path ".env")) {
    Write-Host "Creating .env from .env.example"
    Copy-Item .env.example .env
}

Write-Host "Pulling images..."
docker compose pull

if ($LASTEXITCODE -ne 0) {
    Write-Host "Failed to pull Docker images." -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "Starting services..."
docker compose up -d

if ($LASTEXITCODE -ne 0) {
    $exitCode = $LASTEXITCODE

    Write-Host ""
    Write-Host "Docker Compose failed to start all services." -ForegroundColor Red
    Write-Host ""
    Write-Host "Current container status:"
    docker compose ps

    Write-Host ""
    Write-Host "Kafka logs:"
    docker compose logs kafka --tail=80

    exit $exitCode
}

Write-Host ""
Write-Host "System is running!" -ForegroundColor Green
Write-Host "Frontend: http://localhost:5173"
Write-Host "API Gateway: http://localhost:8080"
Write-Host "Eureka: http://localhost:8761"