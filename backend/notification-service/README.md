# notification-service

Service responsible for consuming appointment events and emitting asynchronous notifications.

## Delivery mode

- default: `log` (stub for development and quick tests)
- optional: `mail` via SMTP (MailHog recommended locally)

Relevant variables:

- `NOTIFICATIONS_PROVIDER` (`log` or `mail`)
- `NOTIFICATIONS_FROM_ADDRESS`

## Data

- uses PostgreSQL (`notification_db`) with Flyway
- main table: `notifications`

## Consumed topics

- `appointment.created`
- `appointment.cancelled`
- `appointment.rescheduled`
- `appointment.reminder.requested`

## OpenAPI

- Swagger UI: `/swagger-ui/index.html`
- OpenAPI JSON: `/v3/api-docs`

## Notification history

Endpoint:

- `GET /notifications`

Optional filters:

- `appointmentId`
- `eventType`
- `status`
