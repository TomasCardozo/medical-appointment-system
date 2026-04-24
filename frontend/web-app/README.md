# web-app

## Stack

- React + Vite
- Tailwind CSS
- Axios
- React Router

## Implemented flow

- login and register (PATIENT / DOCTOR)
- JWT storage and usage
- role-based protected routes
- user profile (PATIENT / DOCTOR)
- password update from profile
- patient dashboard
- doctor listing
- slot search and appointment booking
- patient appointment view and cancellation
- doctor agenda
- minimal doctor onboarding (create profile + availability)

Profile notes:

- `email` is visible but not editable
- for `DOCTOR`, the profile allows editing professional data and synchronizes `fullName` across `auth-service` and `doctor-service`

## Current structure

The frontend was reorganized by feature to better separate responsibilities.

```text
src/
├── app/
│   ├── router/
│   │   ├── ProtectedRoute.jsx
│   │   └── ProtectedRoute.test.jsx
│   ├── shell/
│   │   └── AppShell.jsx
│   └── router.jsx
├── features/
│   ├── auth/
│   │   ├── AuthContext.jsx
│   │   └── pages/
│   │       ├── LoginPage.jsx
│   │       ├── LoginPage.test.jsx
│   │       ├── RegisterPage.jsx
│   │       └── RegisterPage.test.jsx
│   ├── booking/
│   │   └── pages/
│   │       ├── BookingPage.jsx
│   │       ├── BookingPage.test.jsx
│   │       └── booking/
│   ├── patient/
│   │   └── pages/
│   │       ├── PatientDashboardPage.jsx
│   │       ├── PatientDashboardPage.test.jsx
│   │       └── patient-dashboard/
│   ├── doctor/
│   │   └── pages/
│   │       ├── DoctorAgendaPage.jsx
│   │       ├── DoctorAgendaPage.test.jsx
│   │       └── doctor-agenda/
│   └── profile/
│       └── pages/
│           ├── ProfilePage.jsx
│           └── ProfilePage.test.jsx
├── shared/
│   ├── api/
│   ├── auth/
│   └── test/
├── App.jsx
├── main.jsx
└── index.css
```

Organization notes:

- `app/`: global routing and layout
- `features/`: domain-based pages and business logic
- `shared/`: HTTP client, common utilities, and test setup

## Configuration

By default, it consumes the API with a relative base `/api`.

- In development (`npm run dev`), Vite proxies `/api/*` to `http://localhost:8080/*`.
- In Docker (`frontend-web-app`), Nginx proxies `/api/*` to `api-gateway:8080/*`.

Optional variable:

- `VITE_API_BASE_URL` (e.g., `/api` or `http://localhost:8080`)

## Commands

```bash
npm install
npm run dev
npm run build
npm run test
npm run test:run
npm run coverage
```

## Note

The frontend requires the backend to be running and accessible through the gateway.

## Docker

The project includes a Dockerfile for production build and serving with Nginx:

```bash
docker build -t frontend-web-app .
docker run --rm -p 5173:80 frontend-web-app
```
