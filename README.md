# Smart Health Management System

## Overview

The Smart Health Management System is a REST API designed to streamline the management of patient records, appointments, and health statistics for healthcare providers. This system aims to provide an efficient and user-friendly interface for managing healthcare operations.

## Table of Contents

1. [Overview](#overview)
2. [Features](#features)
3. [Technologies Used](#technologies-used)
4. [JWT Authentication](#jwt-authentication)
   - [How It Works](#how-it-works)
   - [Example](#example)
5. [Installation and Setup](#installation-and-setup)
6. [API Endpoints](#api-endpoints)


---

## Features

- **Pre-Visit Summary**: Automatic appointment briefs with code-calculated changes and patient questions; doctor-approved post-visit summaries, action checklists and follow-up booking. Gemini can simplify doctor notes without publishing them. See [the workflow and REST API guide](docs/PRE-VISIT-SUMMARY.md). Standalone symptom/change analysis and AI report extraction are disabled by default; saved history remains available.

- **Patient Management**: Manage patient records, including personal information and medical history.
- **Appointment Scheduling**: Schedule, update, and view appointment history.
- **Health Statistics**: Track and manage health statistics for patients.
- **JWT Authentication**: Secure API endpoints using JWT tokens.
- **Email Notifications**: Send email reminders for upcoming appointments.

---

## Technologies Used

- **Spring Boot**: Provides a robust framework for building enterprise-grade REST APIs.
- **Spring Data JPA**: Simplifies database interactions using object-relational mapping.
- **Spring Security**: Secures the application with comprehensive authentication and authorization.
- **Thymeleaf**: Used for creating dynamic email templates.
- **MySQL**: A reliable relational database for storing application data.
- **Lombok**: Reduces boilerplate code with annotations like `@Getter` and `@Setter`.
- **Springdoc OpenAPI**: Automatically generates interactive API documentation.

---

## JWT Authentication

The Smart Health Management System uses JWT (JSON Web Token) for securing API endpoints. JWT is a compact, URL-safe means of representing claims to be transferred between two parties. The token is generated upon successful authentication and must be included in the `Authorization` header of subsequent requests to protected endpoints.

### How It Works

1. **User Login**: The user sends a POST request with their credentials to the `/auth/login` endpoint.
2. **Token Generation**: If the credentials are valid, the server generates a JWT and returns it in the response.
3. **Token Usage**: The client includes the JWT in the `Authorization` header of subsequent requests to access protected resources.
4. **Token Validation**: The server validates the token on each request to ensure the user is authenticated.

### Example

```http
POST /auth/login
Content-Type: application/json

{
  "username": "user",
  "password": "password"
}

Response:
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

---

## Installation and Setup

### Prerequisites

- Java 17 or higher
- MySQL installed and running
- Maven for dependency management

### Steps

1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/smart-health-management.git
   cd smart-health-management
   ```
2. Configure the application properties:
   - Open `src/main/resources/application.properties`.
   - Set the database URL, username, and password for your MySQL instance.

3. Build and run the application:
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```
4. Import the provided Postman collection to test the API endpoints.

---

## API Endpoints

### Authentication
- **POST** `/auth/login` - Authenticate user and return JWT token.

### Appointments
- **POST** `/api/appointments` - Schedule a new appointment.
- **PUT** `/api/appointments/{id}` - Update an existing appointment.
- **GET** `/api/appointments/{id}` - Get appointment details by ID.
- **GET** `/api/appointments` - List all appointments.
- **DELETE** `/api/appointments/{appointmentId}` - Cancel an appointment.
- **POST** `/api/appointments/send-reminders` - Send email reminders for upcoming appointments.

### Doctors
- **POST** `/api/doctors/register` - Register a new doctor.
- **PUT** `/api/doctors/{id}` - Update doctor details.
- **DELETE** `/api/doctors/{id}` - Delete a doctor.
- **GET** `/api/doctors/{id}` - Get doctor details by ID.
- **GET** `/api/doctors/upcoming-appointments/{doctorId}` - List upcoming appointments for a doctor.
- **GET** `/api/doctors` - List all doctors.

### Patients
- **POST** `/api/patients/register` - Register a new patient.
- **GET** `/api/patients/{id}` - Get patient details by ID.
- **GET** `/api/patients` - List all patients.
- **PUT** `/api/patients/{id}` - Update patient details.
- **DELETE** `/api/patients/{id}` - Delete a patient.
- **GET** `/api/patients/name/{name}` - Search patients by name.
- **GET** `/api/patients/age/{age}` - Search patients by age.
- **GET** `/api/patients/medicalCondition/{medicalCondition}` - Search patients by medical condition.

### Health Metrics
- **POST** `/api/healthmetrics` - Save health metrics.
- **GET** `/api/healthmetrics/myhealthreports` - Retrieves the authenticated patient's health reports. Ensures that the logged-in user can access only their own health reports and not any other patient's data.
- **GET** `/api/healthmetrics` - List all health metrics.
- **GET** `/api/healthmetrics/{id}` - Get health metrics by ID.
- **GET** `/api/healthmetrics/patient/{patientId}` - List health metrics for a patient.


---

Each endpoint is secured based on the user's role (Admin, Doctor, or Patient). Refer to the documentation for detailed role-based access control policies.


## Patient Health Timeline and Doctor Review

Java compares current symptoms and recorded measurements against prior patient history. Gemini summarizes the verified facts in structured JSON, and doctors review saved results, add private notes, and recommend patient-visible follow-ups. This is a demonstration of clinical decision support with doctor oversight.

- Patients: `POST /api/ai/change-analysis/me` and `GET /api/health-timeline/me?page=0&size=20`.
- Doctors: `POST /api/ai/change-analysis/patients/{patientId}`, `GET /api/health-timeline/patients/{patientId}`, and `PATCH /api/ai/analyses/{analysisId}/review`.
- New successful analyses are saved as `PENDING_REVIEW`; doctors select `REVIEWED`, `FOLLOW_UP_REQUIRED`, or `DISMISSED`. Patient responses omit private notes.
- Java owns numeric changes and the disclaimer. Gemini failures return verified changes with `narrativeStatus: UNAVAILABLE`, without saving a successful result.

Before starting the upgraded app against an existing database, apply the [additive SQL migration](docs/db/V2__patient_health_timeline.sql) once as described in the [complete setup and API guide](docs/HEALTH-TIMELINE.md). Existing source data is preserved. Import the [timeline Postman collection](docs/Health-Timeline.postman_collection.json) for the full patient-to-doctor workflow.

Keep `AI_API_KEY` and `AI_MODEL` in your run environment. New settings are `HEALTH_TIMELINE_ENABLED` (true), `HEALTH_TIMELINE_METRICS_HISTORY_LIMIT` (5, range 2–20), and `HEALTH_TIMELINE_MAX_PAGE_SIZE` (100, range 1–100). Original symptom history still follows `AI_SAVE_HISTORY`; new successful analyses always persist for review.

Run `.\mvnw.cmd test -q` from the repository root. Tests use isolated H2 and mocked Gemini/email. Measurement units are not stored in the existing schema, and doctor access follows the existing role-wide policy. The guide explains calculation rules, response examples, security, limitations, and future improvements.

> I built a patient health timeline that compares new symptoms and measurements with historical records. Java performs deterministic change calculations, while Gemini converts the verified changes into a structured clinical summary. Doctors remain in control by reviewing the result, adding private notes and recommending a follow-up. The feature is human-in-the-loop decision support, not an automated diagnosis.


## React frontend

Doctor-reviewed report import is available in **Overview → Upload health report**. Gemini extracts a PDF/photo into an editable draft; the doctor verifies the patient, collection date, values, and units before saving selected measurements together. See the [report-import setup and API guide](docs/REPORT-IMPORT.md). Measurement units are now stored, decimal lab results are supported, and mismatched units are excluded from comparisons. Restart Spring Boot for the schema/code update.

The React 19 Pulse workspace is in [frontend](frontend/README.md). The frontend has been restored to the version from before October 4, 2026 at 7:47 AM IST. It includes patient and doctor workspaces, registration, appointment booking and cancellation, recorded health measurements, health timelines, AI symptom/change analysis, and doctor review. The newer CareFlow frontend is saved in `backups/careflow-before-rollback-20261004`.

Start Spring Boot on port 8080, then run these commands in a second terminal:

~~~powershell
cd frontend
npm.cmd install
npm.cmd run dev
~~~

Open http://127.0.0.1:5173, select **Patient** or **Doctor**, and sign in with **Open my workspace**. Use **Create an account** to register. No Gemini key is needed in the browser.

Restart Spring Boot after updating to load the new booking endpoints. Patients select **Appointments → Book appointment**, choose a registered doctor and a future date/time, and confirm. Doctors see their upcoming appointments. No additional database migration is needed for registration or booking; doctor availability and exclusive time slots are not enforced.

Frontend checks: npm.cmd test, npm.cmd run build, and npm.cmd run test:browser (with the frontend dev server running and Google Chrome installed). The browser suite uses synthetic intercepted responses and does not call live AI or write to your database.
