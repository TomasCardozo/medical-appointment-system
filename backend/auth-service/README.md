# auth-service

Authentication/authorization service of the system.

## Responsibility

- user registration (`PATIENT` and `DOCTOR`)
- login with JWT issuance
- authenticated identity endpoint (`/auth/me`)

## Endpoints

- `POST /auth/register/patient`
- `POST /auth/register/doctor`
- `POST /auth/login`
- `GET /auth/me`
- `PUT /auth/me`

## Internal endpoints (service-to-service)

- `GET /internal/users/by-email?email={email}`
- `GET /internal/users/{id}`

Both internal endpoints return `id`, `fullName`, `email`, `role`, and `active`.

## Notes

- uses PostgreSQL (`auth_db`) with Flyway
- uses stateless Spring Security + JWT
- base roles seeded: `PATIENT`, `DOCTOR`, `ADMIN`
- `PUT /auth/me` allows updating `fullName`
- `email` is visible in the profile but immutable
- password change requires `currentPassword` and `newPassword`

## OpenAPI

- Swagger UI: `/swagger-ui/index.html`
- OpenAPI JSON: `/v3/api-docs`
