# Body Weather frontend

The React UI uses the existing REST API. No new clinical scoring or provider calls were added.

## Patient

- **The Sky / Overview:** ambient SVG/CSS sky, latest saved comparison summary, and three measurement orbs. Selecting the sky opens the river. The before/after control switches between actual previous and latest records; it does not interpolate or convert units.
- **The River / Health timeline:** horizontal, keyboard-accessible event cards. Reaching the right edge loads the next page and appends it. Previous/Next buttons remain available. The visible timeline checks saved records every minute.
- **Tell the Sky / AI insights:** typed symptoms or optional browser speech recognition, existing change/symptom analysis, and a Book a doctor action that opens the booking form.
- **Tickets / Appointments:** real appointment times, relative countdowns updated each minute, booking, and cancellation. Swiping left on a future patient appointment opens the existing cancellation confirmation; it does not immediately cancel.

## Doctor

The radar reads timeline data for up to 20 patients, with up to four concurrent GET requests. Dots nearer the center have pending reviews in their latest 20 loaded events. This is a review queue, not a clinical risk or urgency ranking. Unavailable status is explicitly shown. Search and the patient selector provide access to the full patient list. Opening the radar never generates Gemini analyses. The schedule strip uses the doctor's existing upcoming-appointments endpoint.

Drop & Verify supports dragging a report into the upload area or choosing a file. After extraction, the original PDF/image appears beside editable results. Amber means missing/unsupported information or source evidence requiring review, not a model confidence percentage. Units on unconfirmed rows are corrected inline before confirmation. The separate missing-unit endpoint is for already saved measurement records, not unsaved drafts. All original verification and confirmation requirements remain.

## Admin

Select Admin on the login screen. The frontend checks `/api/admin` with the received token before opening Control Tower. Counts, searchable patient/doctor/appointment tables, and reminder sending use existing APIs. Account deletion and reminder emails require an explicit confirmation dialog. This screen does not display credentials returned by older APIs.

The current JWT does not contain roles. Workspace selection is verified against protected server endpoints; frontend selection never grants backend permissions.

## Atmosphere and accessibility

Auto sky switches to night from 18:00 to 06:00 using local device time; Day/Night overrides are available. Only this preference persists in local storage; the JWT stays in memory. Motion is slow and respects reduced-motion preferences. No people or faces are used.

Weather states describe recorded comparisons or review progress: unknown, changed, steady recorded measurements, pending review, or review recorded. Dawn indicates a doctor review, not clinical improvement. No clear sky promises safety, no storm diagnoses an emergency, and no frontend threshold invents severity. Real values, units, collection dates when available, and full analysis details remain accessible.

Speech recognition depends on browser support and permission. It can process audio through the browser's speech provider; the UI shows this before use and always supports typing. Generated text still follows existing symptom validation.

## Check the frontend

Run `npm run build`, `npm test`, and `npm run test:browser` from `frontend`. Browser tests use intercepted fictional responses and cover existing report/review flows plus sky values, day/night mode, infinite river pagination, radar without Gemini calls, verified admin entry, guarded account deletion/reminder actions, and mobile layouts. No real database writes, Gemini calls, or reminder emails occur in these tests.
