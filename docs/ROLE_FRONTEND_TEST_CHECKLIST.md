# CareFlow: fictional-data test checklist

Restart Spring Boot after pulling the account-controller change. Start the React frontend with `npm.cmd install` and `npm.cmd run dev` from `frontend/`. Open http://127.0.0.1:5173. Use unique usernames across all three role tables. Keep real clinical data out of this demo.

## Patient and doctor

1. Open Create an account → Register as a doctor. Enter a fictional name, unique username, email such as `doctor@example.test`, a password of at least 8 characters, and `General medicine` as specialization.
2. Register a patient at `/register/patient` with another unique username, age 30, and `patient@example.test`. Save your chosen credentials locally; the UI never displays passwords after registration.
3. Sign in as the patient using the username, not email. Open Book appointment, search the doctor, select their card, choose a future date/time and enter a fictional reason. Confirm. The request uses `/book` and does not promise exclusive availability.
4. Open My appointments. Check the doctor, date, reason and server status. Reload the page: the session should restore. Test cancellation with another booking; Go back must leave the booking intact, Confirm must change its status.
5. Sign out and sign in as the doctor. Open Patients; test name, age and medical-condition searches. Open the patient and choose Metrics.
6. Record a measurement with heart rate 72, systolic pressure 120, diastolic pressure 80, weight 72 and temperature 36.7. Save, then record another measurement with heart rate 78 and temperature 37. Use only fictional values; no clinical meaning is implied.
7. In the patient's Change analysis tab, add `cough`, confirm the displayed context, and run history comparison. If the provider is configured, the response should include an analysis ID. An AI/provider failure must show feedback and permit retry without losing the symptoms.
8. Run another comparison with `cough, fever`. Inspect the recorded/calculated facts separately from the AI narrative. When the backend saves the response, select Review analysis or use the review queue. Do not type an analysis ID manually.
9. Add a private note and a patient-visible recommendation such as `Arrange a clinic appointment.` Review the confirmation summary, then save. Verify the review status updates.
10. Sign in as the patient. Open the timeline: see follow-up guidance and the review status, but never the private doctor note. Check pagination, event filters, chart metric/date-range selectors and the chart table.
11. Use the separate Symptom decision support page to exercise `/api/ai/symptom-analysis`. It accepts symptom strings; duration/severity are intentionally omitted. This result is decision support, not a diagnosis or saved doctor review.

## Admin

1. Sign in with an existing admin account. If no admin exists, a developer must bootstrap the first account using the existing backend setup/API; there is no public frontend admin registration page. Do not insert plaintext passwords into SQL. The existing `/api/admin/register` backend registration service handles password hashing.
2. Verify list-derived counts and charts. Open Doctors: create, view, edit name/specialization, and test both cancelling and confirming deletion on a disposable record.
3. Open Patients and edit a disposable patient's supported fields. Patient profile edits are unavailable in the patient workspace because the legacy route lacks ownership checks.
4. Open Appointments: filter by date/status/doctor, inspect a detail drawer and export CSV. Open Health metrics, filter by patient/date and export CSV. Exports must exclude passwords and tokens.
5. Open Administrators: create a disposable admin, verify credentials never appear, and test deletion confirmation. Your own Delete button must be disabled.
6. Test Send reminders only with a safe local email setup. The dialog warns that the server may email eligible patients across the clinic. Opening or cancelling the dialog must not send a request. Automated tests mock this action.

## Cross-cutting checks

- Reload while signed in, then sign out. No previous account's cached records should remain visible after logout.
- Visit another role's URL while signed in: show Access denied without destroying the valid session.
- An authenticated 401 should clear the session and return to login with a session-expired message. A 403 should retain the session and show friendly feedback.
- At 360/768/1024/1440px, check navigation, scrollable tables, dialogs and no page-level horizontal overflow.
- Test light/dark mode, Tab/Shift+Tab/Escape, visible focus, reduced motion, loading/empty/error states and retry buttons.
- In development, expand API Debug: values must be redacted, including patient search strings, symptoms, notes and secrets. Production builds must not expose the panel.

Automated equivalents: `npm.cmd test`, `npm.cmd run build`, and `npm.cmd run test:browser` (with Chrome and the dev/preview server running). These use synthetic intercepted records and never send live AI requests or email.
