#!/usr/bin/env bash

set -e

echo "Starting Medical Appointment System (Bash)"

# Copy env if not exists
if [ ! -f .env ]; then
  echo "Creating .env from .env.example"
  cp .env.example .env
fi

echo "Pulling images..."
docker compose pull

echo "Starting services..."
if ! docker compose up -d; then
  echo ""
  echo "Docker Compose failed to start all services."
  echo ""
  echo "Current container status:"
  docker compose ps

  echo ""
  echo "Kafka logs:"
  docker compose logs kafka --tail=80

  exit 1
fi

echo ""
echo "System is running!"
echo "Frontend: http://localhost:5173"
echo "API Gateway: http://localhost:8080"
echo "Eureka: http://localhost:8761"