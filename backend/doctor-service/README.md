# doctor-service

Service responsible for doctor profiles and availability.

## Implemented endpoints

- `POST /doctors/profile`
- `GET /doctors/me`
- `PUT /doctors/me`
- `GET /doctors`
- `GET /doctors/{id}`
- `POST /doctors/{id}/availability`
- `PUT /doctors/{id}/availability/{availabilityId}`
- `DELETE /doctors/{id}/availability/{availabilityId}`
- `POST /doctors/{id}/blocked-slots`
- `GET /doctors/{id}/schedule?from=YYYY-MM-DD&to=YYYY-MM-DD`

## Internal endpoints (service-to-service)

- `GET /internal/doctors/by-owner-email?email={email}`
- `GET /internal/doctors/{id}`

## Security

- `POST /doctors/profile` requires JWT with role `DOCTOR`
- `GET /doctors/me` requires JWT and returns the doctor profile associated with the authenticated user
- `PUT /doctors/me` requires JWT with role `DOCTOR` and updates the authenticated user's doctor profile
- `GET /doctors/me` returns `404` if the authenticated user does not have a doctor profile
- availability and blocking management require JWT with role `DOCTOR` or `ADMIN`
- a `DOCTOR` can only manage their own profile via `ownerEmail`
- `GET /doctors`, `GET /doctors/{id}`, and `GET /doctors/{id}/schedule` are public

## OpenAPI

- Swagger UI: `/swagger-ui/index.html`
- OpenAPI JSON: `/v3/api-docs`

## Data

- uses PostgreSQL (`doctor_db`) with Flyway
- tables: `doctor_profiles`, `doctor_availabilities`, `blocked_slots`
