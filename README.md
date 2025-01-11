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

1. **User Login**: The user sends a POST request with their credentials to the `/api/auth/signin` endpoint.
2. **Token Generation**: If the credentials are valid, the server generates a JWT and returns it in the response.
3. **Token Usage**: The client includes the JWT in the `Authorization` header of subsequent requests to access protected resources.
4. **Token Validation**: The server validates the token on each request to ensure the user is authenticated.

### Example

```http
POST /api/auth/login
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