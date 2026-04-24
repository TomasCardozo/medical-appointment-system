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
docker compose up -d

echo "System is running!"