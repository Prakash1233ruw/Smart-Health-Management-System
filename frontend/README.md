> **Current workflow (October 2026):** The AI screens described in the older notes below have been replaced by appointment briefs and doctor-approved visit summaries. Start with [Pre-Visit Summary](../docs/PRE-VISIT-SUMMARY.md) for the active UI, endpoints and testing steps. AI report extraction and standalone symptom/change analysis are no longer exposed. The current browser suite runs `booking-browser.mjs` and `visits-browser.mjs`.

# Pulse · React health workspace

A responsive React frontend for the existing Smart Health Management System.
It uses real Spring Boot APIs and existing patient/doctor accounts. There is no fake data in the application.
Browser tests intercept synthetic API responses; they do not modify your database or call Gemini.

## Run on Windows

1. Start your existing Spring Boot application on port 8080, with its MySQL configuration and Gemini environment variables.
2. Open a second terminal at the repository root:

```powershell
cd frontend
npm.cmd install
npm.cmd run dev
```

3. Open http://127.0.0.1:5173.
4. Choose Patient or Doctor and sign in with the account you already use in Postman.

The login form handles the backend's plain-text response, including its JWT Token prefix.
Selecting a role does not grant permissions: the application checks access with the backend before opening the workspace.
On the login screen, choose **Create an account** to register a patient or doctor. Choose the matching role, fill in your details, then sign in after successful registration. Administrator screens are outside this version.

## Patient workflow

- Overview shows actual saved measurements and up to 12 recorded heart-rate readings.
- AI insights supports both the original Symptom Analyzer and the new health-change analysis.
- Enter symptoms separated by commas/new lines, or choose a measurements-only comparison.
- Successful new analyses are saved for professional review. Unavailable/skipped AI narratives are clearly identified.
- Health timeline shows paginated source events, analyses, reviews and patient-visible follow-ups.
- Empty histories show an empty state; no fabricated readings, risk scores, or example diagnoses are displayed.

## Doctor workflow

- Search/select an existing patient.
- Record measurements with a timestamp. Unfilled fields stay absent rather than becoming zero.
- View that patient's dashboard, AI analyses and timeline.
- Review pending/follow-up analyses using the actual analysis ID.
- Add private notes, patient-visible recommendations, and an optional future follow-up date.
- Saved reviews refresh the timeline; terminal statuses have no review action.
- Private notes render only in the doctor workspace.

The review list contains eligible analyses on the current timeline page; it is not a global doctor inbox.
Older analyses can be found through timeline pagination.
Recording measurements does not automatically request AI analysis; initiate it explicitly from AI insights.

## Design and accessibility

Teal, ivory and soft green surfaces; animated entry cards, SVG trend drawing, floating illustrations, and dialog transitions.
CSS animations respect prefers-reduced-motion.
Responsive layouts support desktop and mobile. Forms have labels, errors, loading states and duplicate-submission prevention.
Dialogs trap keyboard focus, close with Escape when not saving, and restore focus on exit.
Fonts are bundled locally, with system fallbacks. No Google Fonts request is needed at runtime.

No unit or clinical threshold is invented. The current database does not record measurement units.
Numerical increases/decreases are not labeled as improvement or deterioration.
AI-generated text is displayed as text through React; no raw HTML is injected.
The server-owned medical disclaimer is shown with a client fallback notice.

## API and session design

Vite proxies /auth and /api to http://localhost:8080 during development and local preview.
This avoids adding broad CORS permissions or changing existing backend endpoints.
If your backend uses another address:

```powershell
$env:BACKEND_URL = "http://localhost:8081"
npm.cmd run dev
```

JWTs remain in memory: refresh/reload requires login again. No JWT or patient data is stored in localStorage.
401 responses clear the session. Switching patients cancels read requests and clears their previous details.
Leaving the analysis form aborts its browser request; the server may still complete/save an already-started AI request.
400/403/404/409/provider/network errors are surfaced without silently creating successful results.
Never place AI_API_KEY in this frontend or in a VITE_ variable. Gemini is called only by Spring Boot.

For deployment, serve the built frontend behind the same origin as the API, and route /api and /auth to Spring Boot.
Vite's proxy is a development/preview convenience; it is not embedded in the static build.
Do not copy the generated index over Spring Boot's API routes.

## Build and verification

```powershell
npm.cmd test
npm.cmd run build
npm.cmd run dev
```

With the dev server running, in another terminal:

```powershell
npm.cmd run test:browser
```

The browser suite uses Playwright and an installed Google Chrome browser.
It verifies login, authorized requests, patient privacy, pagination, valid analysis, AI fallback,
doctor measurements/review, follow-up validation, mobile overflow, reduced motion and expired-session logout.
Screenshots are written to test-results (ignored by Git).

API unit tests cover the actual token format, symptom validation, requests/errors, network failures, aborts and expired-session notification.
The backend suite passes 81 tests, including isolated registration/booking integration tests with mocked email and AI.

## Source files

- src/App.jsx: workspace, dashboards, timeline, analysis and doctor forms.
- src/Registration.jsx and src/Appointments.jsx: account creation, booking, cancellation and doctor schedules.
- src/api.js: authenticated API transport and boundary validation.
- src/styles.css: visual system, responsive layouts and animations.
- vite.config.js: local API proxy.
- tests/api.test.js, tests/browser.mjs and tests/booking-browser.mjs: isolated unit/browser checks.

Future additions could include verified clinician onboarding, availability/slot management, clinician assignments and a backend-paginated measurements API.
This is a demonstration of AI-assisted clinical decision support with doctor oversight, not production-ready medical software.

## Verified results · 2026-10-04

- npm.cmd test: 4 tests passed, zero failures.
- npm.cmd run build: passed; static assets generated in dist.
- npm.cmd run test:browser: passed; patient/doctor flows, privacy, pagination, analysis/fallback, review/measurement forms, wrong-role rejection, mobile layout and session expiry.
- npm dependency audit at installation: zero reported vulnerabilities.
- Desktop and mobile screenshots inspected using synthetic data. Live account testing remains a local environment step.

Build tooling reference: [Vite setup requirements](https://vite.dev/guide/).

## Clear AI health summaries

Doctors can use **Overview → Upload health report** for PDF/image extraction, review, correction, and bulk confirmation. See [the report-import guide](../docs/REPORT-IMPORT.md) for supported measurements, limits, units, schema setup, and API details. Manual measurement entry also supports optional unit selection.

AI insights and saved timeline analyses now distinguish Gemini's interpretation from recorded symptoms, server-calculated measurement changes, and doctor review status. A short main takeaway leads into warning signs, possible explanations, suggested tests/specialist or questions for the doctor. The original AI explanation remains expandable; no additional Gemini calls are made to shorten it. Measurement movement is not labeled as improvement or deterioration, and missing symptoms are not presented as confirmed recovery.

The browser suite also checks missing comparison baselines, absent response lists, warning visibility with both low and emergency AI estimates, expandable full explanations, review status, unavailable AI narratives, and the summary's mobile layout. All automated requests use synthetic intercepted data.

## Registration and appointments

Restart Spring Boot after this update to load the new booking endpoints, then refresh the browser. No new database migration is required.

- On the login screen: Create an account → select Patient or Doctor → enter details → Create account → sign in. Use distinct usernames across patient and doctor accounts.
- Patients: Appointments → Book appointment → select a registered doctor → choose a future date/time → Confirm booking.
- Bookings appear in Your appointments. Cancel requires a second confirmation and updates the status.
- Doctors: Appointments shows future bookings belonging to their logged-in doctor account; canceled entries are omitted.
- Registration uses the existing public endpoints and BCrypt password storage. Doctor self-registration remains the existing demo policy.
- GET /api/doctors/directory exposes only ID, name and specialization to authenticated patient/doctor/admin roles; the admin-only full directory stays protected.
- POST /api/appointments/book accepts doctorId, future dateTime, and optional notes (max 500). The server assigns the patient from the JWT principal and sets Scheduled status.
- Appointment lists/cancellation use existing APIs. Confirmation email is attempted by the existing service; failure does not invalidate a saved booking. SMTP delivery was mocked in tests.
- This version does not enforce doctor availability or reserve exclusive time slots, and does not support rescheduling.
- The booking browser suite covers both registrations, password confirmation, error feedback, booking, cancellation, doctor schedule and mobile layout.
