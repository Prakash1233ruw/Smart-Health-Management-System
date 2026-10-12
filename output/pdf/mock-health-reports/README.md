# Mock health reports for Smart Health

All identities, measurements, and comparison bands in this folder are fictional software test data. They are not medical examples or real patient records.

## Start with these two reports

1. Restart Spring Boot after the report-import feature changes, then refresh the React frontend.
2. Sign in as a **doctor**. Create or select a mock patient named **Aarav Demo**. The printed `DEMO-001` is only a report label; use the patient's actual application/database ID when calling APIs.
3. On **Overview**, select that patient and choose **Upload health report**.
4. Upload `01-baseline-report.pdf`, then choose **Extract measurements with Gemini**.
5. Review the original report, patient name, all values and units. Confirm the collection date is **2026-09-20**. Select supported rows, check the verification box, and save.
6. Repeat with `02-follow-up-report.pdf` for the **same patient**, using collection date **2026-09-27**.
7. Open the patient's health timeline / AI insights and compare the two saved records. Choose the measurements-only option if available, then generate the summary.

Expected factual changes include:

| Measurement | Baseline | Follow-up | Change |
|---|---:|---:|---:|
| Heart rate | 72 bpm | 84 bpm | +12 bpm |
| Systolic pressure | 118 mmHg | 128 mmHg | +10 mmHg |
| Blood glucose | 98 mg/dL | 112 mg/dL | +14 mg/dL |
| Total cholesterol | 198 mg/dL | 210 mg/dL | +12 mg/dL |
| Weight | 76.0 kg | 77.2 kg | +1.2 kg |
| Physical activity | 150 minutes/week | 110 minutes/week | -40 minutes/week |

These changes test numerical comparisons; they do not establish diagnoses or whether a patient's health improved or worsened. All 16 supported metrics are present in the first three reports.

## Additional scenarios

| File | What it tests | Expected review |
|---|---|---|
| `03-different-units-report.pdf` | Decimal lab values, mmol/L, pounds, inches, Fahrenheit | Preserve the printed values and units. The collection date is 2026-10-01. Do not convert units manually. Comparisons across incompatible units should be suppressed. |
| `04-review-required-report.pdf` | Missing date, missing weight unit, an inequality, unsupported tests, and a reference band | Keep this draft **unconfirmed** unless missing information is obtained from the source. Do not guess a date or weight unit. Glucose `<110` must not become 110. Hemoglobin and HbA1c must not be mapped to other metrics. Total cholesterol is 213 mg/dL, not the reference-band endpoints. |
| `05-scanned-follow-up.pdf` | Image-only PDF, with no extractable text layer | Use as an **alternative** to report 02, or review without saving. It contains the same follow-up data. |
| `02-follow-up-image.png` | Direct image upload | Also an alternative to report 02. Review every extracted value against the image. |

Do not confirm the digital, scanned, and image versions of the same follow-up report into the same patient: re-uploading creates another draft and can create duplicate records if confirmed.

## What to check

- Extraction creates a draft; it must not save measurements before doctor confirmation.
- Editing a value, unit, selected row, or date requires checking verification again.
- Unsupported and unclear results stay excluded until corrected with actual source information.
- `expected-results.json` contains the intended values for manual comparison. Gemini's extraction can vary; the original report remains the source to review.
- The scan has no text layer, so it exercises the image extraction path. The configured Gemini model must support images.

No Gemini request or database write was made to create this bundle. Upload extraction uses your existing Gemini configuration and quota. See `docs/REPORT-IMPORT.md` in the project for the API and implementation details.
