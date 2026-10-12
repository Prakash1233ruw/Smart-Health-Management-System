# Patient Health Timeline with AI Change Detection and Doctor Review

This feature demonstrates AI-assisted clinical decision support with doctor oversight.
The existing Gemini Symptom Analyzer remains at POST /api/ai/symptom-analysis; its request, response, and optional AI_SAVE_HISTORY setting are preserved.

## Architecture

### Frontend quick summary

The health-change view presents a short AI takeaway, doctor-review status, symptom changes, and changed measurements with previous/latest values, dates when supplied, and backend-calculated differences. Only comparisons with matching explicitly recorded units appear as reliable changes. Unchanged readings are counted; missing units, mismatched units, and missing readings are grouped into expandable details. No clinical thresholds, unit conversions, or improvement/deterioration labels are inferred in the browser.

Up to three AI suggestions appear initially. Full narrative, all suggestions, questions, measurement table, source lists, limitations, and disclaimer remain in **Full comparison and AI details**. Timeline entries show a short analysis preview and four initial readings with additional measurements expandable.

The visible timeline checks saved backend records every 60 seconds and when returning to the tab. It does not generate new Gemini requests. AI analyses remain snapshots; generate a new analysis after adding or correcting measurements. The existing Refresh button remains available.

### Backend workflow

1. PatientAccessService maps the principal to a persisted patient or doctor.
2. HealthChangeCalculator compares normalized symptoms and recent measurements in Java.
3. HealthChangeAnalysisService sends verified changes and missing-data information to the existing Gemini client.
4. ClinicalJson validates four narrative fields. Java owns numbers, data considered, missing-data notices, professional-review flag and disclaimer.
5. ChangeAnalysisStore saves successful summaries as PENDING_REVIEW after the remote call, in a short transaction.
6. DoctorReviewService locks the analysis and atomically appends a review and updates its status.
7. PatientTimelineService pages a database union of source event IDs and loads only that page's details.

No timeline table copies measurements. Analysis snapshots retain calculated facts and validated narrative.
Separate health_change_analyses and doctor_reviews tables preserve the original consultation schema: its required risk level does not apply to change summaries.

## Database setup

The project uses Hibernate schema update and has no Flyway/Liquibase runner.
The SQL file is a **manual migration**, not an automatically executed startup script.

1. Stop the application and back up the existing MySQL database.
2. Open MySQL Workbench and select the same schema configured for the application.
3. Open [V2__patient_health_timeline.sql](db/V2__patient_health_timeline.sql) and execute it once.
4. Restart the upgraded application. Never use create or create-drop against real data.

The script adds two tables and indexes without modifying existing records.
If Hibernate has already created the new tables, inspect the tables/indexes before applying the script: index creation is not repeatable.
Deleting a patient cascades its new analyses/reviews. Deleting a doctor clears the relationship but preserves the reviewer ID and audit content.
Migration tests use H2 in MySQL compatibility mode. This script has not been applied to your live MySQL database.

## Configuration

Keep AI_API_KEY in the application's run environment and select an AI_MODEL supported by your project.
Never paste a Gemini key into Git, JWT fields, or request bodies.

| Environment variable | Default | Purpose |
|---|---|---|
| HEALTH_TIMELINE_ENABLED | true | Enables new analyses; saved history/reviews remain accessible |
| HEALTH_TIMELINE_METRICS_HISTORY_LIMIT | 5 | Recent records searched, validated range 2–20 |
| HEALTH_TIMELINE_MAX_PAGE_SIZE | 100 | Maximum requested page size, range 1–100 |
| AI_CONNECT_TIMEOUT_SECONDS | 5 | Existing connection timeout |
| AI_READ_TIMEOUT_SECONDS | 30 | Existing read timeout |
| AI_SAVE_HISTORY | false | Optional history for the original Symptom Analyzer |

Successful **new** change analyses are always persisted because doctor review needs a saved analysis.
The original analyzer still follows AI_SAVE_HISTORY. Unsaved original results cannot supply comparison history.

## Test manually with Postman

Use existing accounts or the [demo collection](Smart-Health-Demo-Data.postman_collection.json).
Import [the timeline collection](Health-Timeline.postman_collection.json).
Set patientId to your actual patient ID and patientToken/doctorToken to their JWTs, without the Bearer prefix.
Log in using POST /auth/login with each account's username/password to obtain tokens.

### Record measurements

The existing endpoint allows doctors/admins to save measurements. Use a **doctor JWT**:

~~~http
POST http://localhost:8080/api/healthmetrics
Authorization: Bearer <doctor JWT>
Content-Type: application/json

{
  "patientId": 1,
  "date": "2026-01-01",
  "timestamp": "2026-01-01T10:00:00",
  "heartRate": 60,
  "systolicPressure": 120,
  "diastolicPressure": 80,
  "weight": 80.0
}
~~~

Repeat with a later date/timestamp, heartRate 75, weight 78, systolicPressure 125, diastolicPressure 78.
Replace patientId with your real ID. Choose dates later than any existing measurements for a predictable demonstration.
Saving measurements immediately adds a timeline event. It does not invoke Gemini automatically; request analysis explicitly after saving.

### Patient requests analysis

~~~http
POST http://localhost:8080/api/ai/change-analysis/me
Authorization: Bearer <patient JWT>
Content-Type: application/json

{"symptoms":["cough","fatigue"]}
~~~

Repeat with {"symptoms":["cough","fever"]}: cough persists, fever appears, and fatigue resolves relative to the prior saved analysis.
Omit symptoms ({}) for metrics only. An empty list explicitly records no current symptoms.
Doctors can use POST /api/ai/change-analysis/patients/{patientId}.

Successful response, abridged (narrative varies):

~~~json
{
  "analysisId": 42,
  "reviewStatus": "PENDING_REVIEW",
  "narrativeStatus": "AVAILABLE",
  "changes": {
    "symptomsSubmitted": true,
    "currentSymptoms": ["cough","fever"],
    "symptomChanges": {
      "newSymptoms": ["fever"],
      "persistentSymptoms": ["cough"],
      "resolvedSymptoms": ["fatigue"],
      "previousRecordAvailable": true
    },
    "metricChanges": [{
      "metric": "heartRate",
      "unit": "NOT_RECORDED",
      "previousValue": 60,
      "latestValue": 75,
      "absoluteChange": 15,
      "percentageChange": 25.00,
      "direction": "INCREASED",
      "previousRecordedAt": "2026-01-01T10:00:00",
      "latestRecordedAt": "2026-01-02T10:00:00"
    }]
  },
  "narrative": {
    "summary": "Recorded changes need professional review.",
    "importantChanges": ["The recorded heart rate increased."],
    "itemsForDoctorReview": ["Confirm measurement conditions."],
    "questionsForDoctor": ["When should measurements be repeated?"]
  },
  "dataConsidered": ["Current submitted symptoms","Previous recorded symptoms","Latest heartRate","Previous heartRate"],
  "insufficientData": ["Measurement units are not recorded; clinical interpretation is limited."],
  "requiresProfessionalReview": true,
  "disclaimer": "AI-generated decision support only; not a diagnosis. Review by a qualified healthcare professional is required. If you believe you are experiencing an emergency, seek immediate medical assistance; do not wait for this analysis."
}
~~~

Actual responses include all 16 metric entries with missing-data notices.

### Doctor reviews the saved analysis

Use the returned analysisId and a **doctor JWT**:

~~~http
PATCH http://localhost:8080/api/ai/analyses/42/review
Authorization: Bearer <doctor JWT>
Content-Type: application/json

{
  "status": "FOLLOW_UP_REQUIRED",
  "internalNotes": "Private professional assessment for the demonstration.",
  "followUpRecommendation": "Arrange a clinic appointment to discuss these readings.",
  "recommendedFollowUpDate": "2030-01-01"
}
~~~

The date is optional and must be today or later. Choose an appropriate future date.
Response fields: reviewId, analysisId, doctorId, status, internalNotes, followUpRecommendation, optional date, reviewedAt.
The review response is doctor-only. Patient timeline responses omit the internalNotes field entirely.

| Current status | Allowed next status |
|---|---|
| PENDING_REVIEW | REVIEWED, FOLLOW_UP_REQUIRED, DISMISSED |
| FOLLOW_UP_REQUIRED | REVIEWED, DISMISSED |
| REVIEWED or DISMISSED | None |

Follow-up status requires a nonblank patient-visible recommendation.
Reviews are immutable events. Subsequent permitted reviews append rows; repeating/reopening terminal status returns 409.
Row locking and optimistic versioning protect concurrent updates.
Review IDs refer only to new change analyses, not original symptom consultations.

### Read the timeline

~~~http
GET http://localhost:8080/api/health-timeline/me?page=0&size=20
Authorization: Bearer <patient JWT>
~~~

~~~http
GET http://localhost:8080/api/health-timeline/patients/1?page=0&size=20
Authorization: Bearer <doctor JWT>
~~~

Response contains patientId, entries, page, size, totalElements, totalPages.
Each entry has eventId, eventType, eventTime, title, typed details, and reviewStatus.
Event types: HEALTH_METRICS_RECORDED, SYMPTOM_ANALYSIS, AI_CHANGE_ANALYSIS, DOCTOR_REVIEW, FOLLOW_UP_RECOMMENDED.
Use **eventType + eventId** for identity because source-table IDs may overlap.
Ordering is event time descending, then type descending, then ID descending for deterministic ties.
Follow-up events use review time, with the due date in details. They do not create appointments or notifications.
Analysis entries show current status; review events retain historical status.
Pagination is zero-based, defaults to size 20, limits size to configured maximum and page to 100000.
The database pages the combined events before detail records are loaded.

## Calculation and security decisions

- Symptoms are trimmed, whitespace collapsed, deduplicated and compared case-insensitively. First normalized spelling is displayed.
- The latest saved symptom-bearing analysis across old/new tables provides the symptom baseline.
- The newest timestamp/ID provides the latest metric record. For each field the nearest strictly earlier nonnull reading supplies its baseline within the history window.
- A missing newest field stays missing. Same-timestamp readings do not create a false trend.
- Absolute change is signed latest minus previous. Percentage uses HALF_UP rounding to two decimals; zero baseline means no percentage.
- All 16 numeric entity fields are supported, including separate blood-pressure components and height. Nonfinite readings are unavailable.
- Units are NOT_RECORDED because the existing entity stores none. No thresholds, risk labels, or improvement judgments are invented.
- Patient identity comes from the principal, never a client-selected patient ID.
- Existing doctors have role-wide patient access; the repository has no assignment/consent relationship. New endpoints preserve that policy and require a persisted doctor identity. Admins cannot use these new doctor-only routes.
- Gemini and new timeline DTOs exclude passwords, hashes, JWTs, usernames, emails and unnecessary identity fields.
- Private notes are doctor-only and are never sent to Gemini.
- Provider output rejects extra/missing fields, duplicate keys, trailing JSON, scalar coercion, blank items and oversized strings/lists.
- Java-owned facts and disclaimers cannot be replaced by model fields. Narrative text is probabilistic and needs professional review.

## Errors and fallback

| Situation | Result |
|---|---|
| Missing/invalid JWT | 401 |
| Wrong role/unmapped identity | 403 |
| Missing patient/analysis | 404 |
| Invalid fields, dates, IDs or pagination | 400 |
| Invalid review transition | 409 |
| Database failure/corrupt stored snapshot | Sanitized 503 |
| Gemini missing key, quota/error/timeout or invalid JSON | 200 with UNAVAILABLE narrativeStatus and Java facts retained |
| No meaningful symptoms or latest measurements | 200 with NOT_REQUESTED; provider skipped |

Fallbacks have null analysisId, reviewStatus and narrative and are not stored as successful analyses.
Check narrativeStatus before offering doctor review.
No clinical prompt, key or provider body is logged. Failures log only a safe status notice.
Requests call the provider synchronously; a retry may create another saved analysis.

## Files and testing

Run from the repository root:

~~~powershell
.\mvnw.cmd test -q
.\mvnw.cmd package -DskipTests -q
~~~

Tests use isolated H2, mocked Gemini and mocked email. They do not use live credentials or billable requests.
Coverage includes normalization, calculations, zero/null/tie/rounding, strict JSON, minimized context, fallback, persistence failure,
JWT/ownership/private notes, pagination, review audit/transitions, original consultation baseline/timeline,
existing patient/doctor/metrics/appointment smoke flows, and migration preservation/deletion behavior.
The actual Spring AI adapter is tested with a mock HTTP server for both schemas.

Added production files:

- Config: HealthTimelineProperties
- Controllers: HealthTimelineController, HealthChangeAnalysisController
- DTOs/enums: HealthTimelineDtos, HealthTimelineTypes
- Entities/repositories: HealthChangeAnalysis, DoctorReview, their repositories
- Security: PatientAccessService
- Services: HealthChangeCalculator, ClinicalJson, ChangeAnalysisStore, HealthChangeAnalysisService, DoctorReviewService, PatientTimelineService
- Exceptions: TimelineStorageException, ReviewConflictException

Modified integration points: ClinicalAIClient, SpringAIClinicalClient, AIConsultationRepository, HealthMetricsRepository,
GlobalExceptionHandler, SecurityConfig, ai.properties and README.md.
Added tests: HealthChangeCalculatorTest, ClinicalJsonTest, ChangeAnalysisStoreTest, HealthTimelineMigrationTest, HealthTimelineIntegrationTest.
Extended GeminiClientConfigurationTest.

## Limitations and next improvements

This is not production-ready medical software. There is no RAG, vector database, frontend, medication advice or automated diagnosis.
Schema validation cannot prove that each generated sentence is medically correct.
Legacy measurements lack units; history searches are bounded and cannot detect unrecorded events.
Concurrent submissions may compare against the same baseline. Offset pages can move when new events arrive.
Source patient deletion removes associated events; this is not a legally compliant immutable retention system.
Live Gemini/project compatibility and live MySQL deployment require environment testing.

Next improvements: clinician-patient assignments and consent, units/provenance, idempotent requests,
validated narrative evaluation, cursor pagination, a reviewed frontend workflow and audited retention policy.

## Interview explanation

> I built a patient health timeline that compares new symptoms and measurements
> with historical records. Java performs deterministic change calculations,
> while Gemini converts the verified changes into a structured clinical summary.
> Doctors remain in control by reviewing the result, adding private notes and
> recommending a follow-up. The feature is human-in-the-loop decision support,
> not an automated diagnosis.

## Verified local results (2026-10-03)

- `.\mvnw.cmd test -q`: exit 0; 76 tests, 0 failures, 0 errors, 0 skipped.
- `.\mvnw.cmd package -DskipTests -q`: exit 0; runnable JAR produced at target/smart-health-management-system-0.0.1-SNAPSHOT.jar.
- `git diff --check`: no whitespace errors.
- Postman collection parsed successfully as JSON.

These results cover isolated automated tests and packaging. They do not claim that the live MySQL migration was applied or that a live Gemini request was made.
