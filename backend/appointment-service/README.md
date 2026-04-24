# appointment-service

Service responsible for slot search, appointment booking, and cancellation.

## Implemented endpoints

- `GET /appointments/available?doctorId={doctorId}&date=YYYY-MM-DD`
- `POST /appointments`
- `PUT /appointments/{id}/cancel`
- `PUT /appointments/{id}/reschedule`
- `GET /appointments/patient/{patientId}`
- `GET /appointments/doctor/{doctorId}`
- `GET /appointments/doctor/{doctorId}/agenda?from=YYYY-MM-DD&to=YYYY-MM-DD`

## Kafka events published

- topic `appointment.created`
- topic `appointment.cancelled`
- topic `appointment.rescheduled`
- topic `appointment.reminder.requested`

Kafka events are emitted with JSON payloads for patient notifications.

The response of `GET /appointments/patient/{patientId}` includes `doctorFullName` when it can be resolved from `doctor-service` (if the doctor is not found, it returns `null`).

The response of `GET /appointments/doctor/{doctorId}/agenda` includes `patientFullName` when it can be resolved from `auth-service` (if the user is not found, it returns `null`).

## Security

- Requires JWT on all business endpoints
- `PATIENT` and `ADMIN` can book/cancel appointments
- `PATIENT` and `ADMIN` can reschedule
- `PATIENT` can only access their own appointments
- `DOCTOR` and `ADMIN` can view the medical agenda
- `DOCTOR` can only access their own agenda (validated by `ownerEmail`)

## Rules

- `cancel` and `reschedule` are only allowed for appointments in `BOOKED` status
- canceling or rescheduling already started/past appointments is not allowed
- configurable cutoff via config (`app.rules.*`), default is 24h for both flows
- `ADMIN` can bypass the cutoff by default (`app.rules.admin-bypass-cutoffs=true`)

## Main rescheduling rules

- only appointments in `BOOKED` status can be rescheduled
- `PATIENT` can only reschedule their own appointments
- rescheduling to the same slot is not allowed
- rescheduling to a past slot is not allowed
- rescheduling to an occupied slot or outside availability is not allowed

## Data

- uses PostgreSQL (`appointment_db`) with Flyway
- main table: `appointments`
- indexes for medical agenda, patient appointments, and booking conflicts

## Synchronous integrations

- `doctor-service`
  - `GET /doctors/{id}/schedule`
  - `GET /internal/doctors/by-owner-email`
  - `GET /internal/doctors/{id}`
- `auth-service`
  - `GET /internal/users/by-email`
  - `GET /internal/users/{id}`

## Resilience in Feign integrations

- `spring-cloud-starter-circuitbreaker-resilience4j`
- circuit breaker + time limiter enabled in OpenFeign
- per-client timeouts for `doctor-service` and `auth-service`
- scope limited to Feign; circuit breakers are not applied to Kafka at this stage

## Asynchronous integration

- Kafka producer via `spring-kafka`
- bootstrap server configured via config:
  - local: `localhost:9094`
  - docker: `kafka:9092`

## OpenAPI

- Swagger UI: `/swagger-ui/index.html`
- OpenAPI JSON: `/v3/api-docs`
