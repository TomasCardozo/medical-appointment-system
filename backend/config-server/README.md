# config-server

Centralizes configuration for services via Spring Cloud Config Server.

## Configuration backend

- Uses `spring.profiles.active=native`
- Resolves `spring.cloud.config.server.native.search-locations` from `CONFIG_REPO_PATH`

## Required variable

- `CONFIG_REPO_PATH`

Valid examples:

- Windows (local): `file:///C:/path/to/project/infrastructure/config-repo`
- Linux/macOS (local): `file:///abs/path/to/project/infrastructure/config-repo`
- Docker: `file:/config-repo`

## Important about .env

- Docker Compose does use `.env` for variable interpolation
- Spring Boot does NOT automatically load `.env` when running `mvn spring-boot:run`
- In local environments, `CONFIG_REPO_PATH` must come from the process environment or the IDE Run Configuration

## Local startup (PowerShell)

```powershell
$env:CONFIG_REPO_PATH="file:///C:/path/to/project/infrastructure/config-repo"
cd backend/config-server
mvn spring-boot:run
```

## Docker startup

`docker-compose.yml` mounts `./infrastructure/config-repo` into `/config-repo` and exposes `CONFIG_REPO_PATH=file:/config-repo` to the container.

For publishing, the main compose uses a prebuilt GHCR image of `config-server` and keeps this mount for a simple local experience (`docker compose up -d`).
