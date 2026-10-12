# Doctor-reviewed report import

## Try it

Restart Spring Boot, refresh the frontend, and sign in as a doctor. Select the correct patient on Overview, then choose **Upload health report**. Select a PDF, PNG, or JPEG and choose **Extract measurements with Gemini**. Open the original report and review the source excerpts, patient identity, collection date, values, and units. Select supported rows, correct mistakes, exclude unsupported results, check the verification box, and save the confirmed measurements together.

Extraction creates a draft only. Confirmation creates one new health-metric record, visible through the existing APIs and patient timeline. Repeat confirmation of the same draft and identical request returns the existing record; edited requests cannot overwrite it. Re-uploading a file creates a separate draft: do not confirm multiple drafts for the same report.

Use fictional reports for the demo. Each extraction sends report text or page images to Gemini using the existing server-side `AI_API_KEY` and `AI_MODEL`. Provider quota/billing applies to that request. Confirmation does not call Gemini. The configured model must support image input for scans/photos. Neither API keys nor file contents are logged by the new feature.

## Storage and schema

Your development configuration uses `spring.jpa.hibernate.ddl-auto=update`; restarting adds the draft table and units column and widens five lab fields to accept decimal values. Existing measurements and their unknown units are retained. If automatic schema changes are disabled, apply [V3__report_imports.sql](db/V3__report_imports.sql) once. Do not apply its ADD COLUMN again after Hibernate has already created that column.

Supported measurements remain the existing 16 fields. Glucose and cholesterol/triglyceride values support decimals, including values such as 5.2 mmol/L. Blood pressure, heart/respiratory rate, oxygen, and activity retain whole-number storage; fractional values must be excluded rather than rounded. Unsupported results (for example hemoglobin) must be excluded; do not substitute unrelated measurements.

Units are stored per measurement. No unit conversions or clinical reference ranges are inferred. Comparisons suppress differences when one record has a unit and the other does not, or when units differ. Older records without units retain their previous numerical-only comparisons, with unknown-unit limitations. The frontend charts and overview also avoid comparing mixed units. Date-only reports are stored at the start of the confirmed date, representing an unknown time; no real collection time is inferred.

The audit draft retains its source hash, filename, original extracted values/excerpts, owning doctor/patient, corrected confirmation request, confirmation time, and saved measurement ID. Raw PDF/image bytes are not stored by the application; the browser's preview lasts only while the upload dialog is open. A draft is confirmable for 24 hours. Draft expiry does not delete audit records. The uploading doctor is the only account allowed to read or confirm it. Patient access follows the existing doctor-wide policy; clinician assignments and production document retention remain future work.

## Add units to older measurements

Doctors can choose **Overview → Add missing units**, select a measurement record, and fill only units verified against its original report or device record. Unknown units can remain unselected. Saving changes no measurement values, timestamps, or existing units and creates no duplicate measurement record. New manual readings require a unit for each entered value. Existing clients can still submit older requests without units.

The doctor/admin API is `PATCH /api/healthmetrics/{id}/units` with `{"units":{"heartRate":"bpm"},"verifiedAgainstSource":true}`. Unsupported units, units for absent values, unverified requests, and replacement of existing units are rejected. Matching known units enable subsequent comparisons. Generate a new analysis after saving; historical AI analyses retain their original snapshots. Verification is required by the request; this endpoint does not store a separate audit event or source document.

## Upload limits

- Maximum file: 8 MB; maximum PDF: five unencrypted pages.
- Real PDF/PNG/JPEG contents are checked regardless of supplied filename/MIME type; malformed and excessive-dimension files are rejected.
- Digital PDFs send bounded extracted text. PDFs with any page lacking usable text send rendered page images; photos use Gemini vision. Excessive rendered payloads are rejected with feedback.
- Source text found on the reported PDF page is labeled as matched text, not a verified measurement. Image extraction always needs visual review. Missing units, dates, inequalities, unclear excerpts, and unsupported measurements require correction or exclusion.
- Reports with results from several collection dates must be imported separately for each date. The model is instructed to flag ambiguity, but the doctor must check it.
- Prompt instructions prohibit treating report content as instructions, guessing results, using reference-range thresholds as results, or diagnosing. Structured output is validated on the server; human verification remains essential.
- Original reports cannot be reopened from server storage after closing the dialog. There is no automated lab/device integration or local OCR fallback.

## REST API

| Method | Endpoint | Request |
|---|---|---|
| POST | `/api/report-imports` | Multipart `file`, `patientId`; returns an unconfirmed draft |
| GET | `/api/report-imports/{id}` | Owning doctor's JWT; returns draft/status |
| POST | `/api/report-imports/{id}/confirm` | JSON below; saves verified rows |

```json
{
  "recordedDate": "2026-01-01",
  "verifiedAgainstReport": true,
  "rows": [
    { "index": 0, "metric": "bloodGlucoseLevel", "value": 5.2, "unit": "mmol/L" }
  ]
}
```

`index` identifies an extracted row; patient and doctor identity come from the stored draft and authenticated principal. Duplicate metric/index choices, unsupported units, missing confirmation, future dates, and expired drafts are rejected before persistence.

## Validation

Backend tests use H2 and mocked Gemini/email. They cover draft-only extraction, decimal/unit persistence, retry idempotency, doctor ownership, patient rejection, malformed/oversized/overlong reports, missing dates/source evidence, provider failures, PDF/image preparation, and incompatible-unit comparisons. Browser tests intercept all requests and cover multipart upload, selection/exclusion, correction, invalidated verification after edits, failed-save retry, confirmation payloads, and mobile layout. They do not contact Gemini or write to your database.

Implementation references: [Apache PDFBox 3 API](https://pdfbox.apache.org/3.0/migration.html), [Gemini image understanding](https://ai.google.dev/gemini-api/docs/image-understanding), and [Gemini OpenAI compatibility](https://ai.google.dev/gemini-api/docs/openai).
