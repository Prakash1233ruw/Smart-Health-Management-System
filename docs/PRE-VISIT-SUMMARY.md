# Pre-Visit Summary

The active AI workflow is now appointment-based. Standalone symptom analysis, health-change generation/review, and AI report extraction are removed from the UI and their controllers are disabled by default. Saved records are retained and historical timeline entries remain readable. Manual measurements, unit correction, appointments, registration and administration remain available.

## Try it in the app

1. Restart the Spring Boot application, then refresh the React app. Development `ddl-auto=update` adds the appointment columns below. No patient data needs to be deleted.
2. Sign in as a **patient**. Open **Appointments → Book appointment**. Select a doctor and future time; enter the reason in your own words and your questions. Confirm booking.
3. Sign in as **that doctor**. Open **Appointments → Open visit brief**. The patient name, booking reason, questions, calculated changes, saved measurements and missing-data notes appear together.
4. Once the appointment time has arrived and the consultation has actually taken place, enter the doctor's notes. **Help simplify my notes** optionally asks Gemini for simpler wording. You can instead write the patient summary manually. Drafts are held only in the current screen, not saved or shared.
5. Edit the patient summary, enter agreed actions (one per line), and optionally select a follow-up date. Check the explicit approval checkbox, then choose **Approve & release to patient**. The appointment becomes Completed; the released summary is immutable.
6. Sign in as the same patient. Open **Appointments → Read visit summary**. Read the plan and checklist. **Book follow-up** preselects the same doctor and suggested date; 09:00 is only an editable time placeholder. The patient must choose a suitable time and confirm the booking. There is no slot-availability service.

For a quick test, book a few minutes ahead and wait for the appointment time. Do not change real appointment dates or clinical records just to bypass the completion check. Automated tests use fictional past appointments in H2 instead.

## REST API

Send `Authorization: Bearer <JWT>` and `Content-Type: application/json` for JSON requests.

| Method | Path | Access / behavior |
| --- | --- | --- |
| POST | `/api/appointments/book` | Patient; saves booking plus deterministic brief snapshot |
| GET | `/api/appointments/visits` | Own patient appointments or assigned doctor's appointments, including past visits; names and summary availability included |
| GET | `/api/appointments/{id}/brief` | Assigned doctor only; recalculates current facts, no Gemini call |
| POST | `/api/appointments/{id}/summary/draft` | Assigned doctor after appointment starts; optional Gemini wording from supplied doctor notes, never publishes |
| POST | `/api/appointments/{id}/summary` | Assigned doctor; explicit approval and completed-visit confirmation; atomically publishes once |
| GET | `/api/appointments/{id}/summary` | Owning patient or assigned doctor; approved summary only, or awaiting-approval status |

Booking body:

```json
{
  "doctorId": 3,
  "dateTime": "2030-01-02T10:00:00",
  "notes": "I would like to discuss my recent readings.",
  "questions": "When should I measure again?"
}
```

Use actual IDs and suitable dates. The server assigns patient identity from the JWT and status Scheduled. Notes are limited to 500 characters and questions to 1000. Since the old symptom checker is retired, the current reason is entered directly during booking rather than inferred from a historical symptom check.

Draft body:

```json
{"doctorNotes":"We reviewed the recorded measurements and discussed the agreed follow-up plan."}
```

Only these doctor notes are sent to Gemini, with instructions to rewrite without adding clinical advice, calculations or new facts. Output is a private, editable proposal requiring human verification. The doctor supplies checklist actions and follow-up dates. A missing key, timeout, quota error or invalid provider response returns `aiAvailable: false` and a safe notice; manual completion remains available. No Gemini call is made on booking, brief reads, list refreshes or approval.

Approval body (example wording only):

```json
{
  "summary": "We reviewed your records and agreed on the next steps below.",
  "actions": ["Bring your recent readings to the next appointment."],
  "followUpDate": "2030-01-09",
  "approve": true,
  "visitCompleted": true
}
```

`followUpDate` may be null. Actions may be an empty list, otherwise at most 12 items of 300 characters. Summary maximum: 3000 characters. Approval is rejected for future appointments, cancellations, another doctor's appointment, or an already-approved summary. A published summary is visible **in the app**; this release does not send an email or push message. Checklist ticks are session-only reading aids, not persisted clinical completion records.

## Facts, limits and storage

- Booking stores `brief_json`. Reading the brief builds a fresh view so records added after booking are included.
- Comparison uses the latest 100 saved measurement records: each field’s latest recorded value against its nearest strictly earlier value, including when reports contain different subsets of measurements. Both units must be explicitly recorded and equal. No assumed units or conversions are used.
- Up to three nonzero movements are selected by absolute percentage movement, with a stable metric-name tie-break. This is a numerical ordering, **not medical urgency**. Zero baselines have no percentage. Both original values, units and timestamps remain visible.
- The newest eligible saved measurement record is expanded immediately; the preceding record is available to expand. Future-dated, undated and empty records are explicitly identified when excluded. Unconfirmed import drafts are not used. No claim of a new report since a previous consultation is made when the data cannot establish that.
- Missing reasons/questions, missing comparisons/units and no measurement in the last 30 days are stated explicitly. A lack of changes or pending analyses does not establish good health.
- Up to ten pending/follow-up historical change analyses are listed. That legacy feature did not persist urgency, so the brief says urgency was not recorded; it does not invent a risk level.
- Appointment additions: `questions`, `brief_json`, `approved_summary_json`, `summary_approved_at`. Development schema update is additive. Deployments with schema validation instead of update need an additive migration before starting the app.
- Approval and cancellation lock the appointment row. The old appointment update path now requires the assigned doctor, prohibits participant reassignment and cannot edit completed visits. This closes an indirect way to gain access to another doctor's visit.
- Default controller retirement is controlled by `app.legacy-ai-enabled`; absent/false disables old AI creation, review and extraction endpoints. Compatibility tests explicitly enable that property to continue testing retained history code. The React interface does not expose those tools.

## Verification

```powershell
.\mvnw.cmd clean test -q
cd frontend
npm run build
npm test
npm run test:browser
```

Run Vite first for browser tests. Tests use H2 and mocked providers/email, and intercepted fictional browser requests. They cover booking snapshots, ownership, explicit approval, immutability, future/canceled visits, missing/stale units, AI fallback, patient privacy, follow-up prefills, booking questions and responsive layouts. No live Gemini request is needed for this suite.

The older `browser.mjs` and `weather-browser.mjs` browser scripts document retired UI flows and are not part of the active browser suite. The active suites are `booking-browser.mjs` and `visits-browser.mjs`.
