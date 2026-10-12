import { chromium } from "playwright";
import assert from "node:assert/strict";
import { mkdir } from "node:fs/promises";

// Synthetic, intercepted API responses: no real accounts, database writes, or AI calls.
const browser = await chromium.launch({ channel: "chrome", headless: true });
const base = process.env.FRONTEND_TEST_URL || "http://127.0.0.1:5173";
await mkdir("test-results", { recursive: true });
const errors = [];
const context = await browser.newContext({
  viewport: { width: 1440, height: 1050 },
});
const page = await context.newPage();
page.on("pageerror", (e) => errors.push(e.message));
let role = "PATIENT",
  fallback = false,
  firstComparison = false,
  riskEstimate = "LOW",
  omitSymptomLists = false,
  importSaveFailure = true,
  reportConfirmationBody = null,
  expired = false,
  reviewBody = null,
  metricBody = null,
  unitBody = null,
  reviewed = false;
const narrative = {
  summary:
    "The recorded heart rate increased. A professional review is recommended.",
  importantChanges: ["Cough persists; fever is newly reported."],
  itemsForDoctorReview: ["Confirm measurement conditions."],
  questionsForDoctor: ["When should measurements be repeated?"],
};
const analysis = {
  analysisId: 7,
  reviewStatus: "PENDING_REVIEW",
  narrativeStatus: "AVAILABLE",
  changes: {
    symptomsSubmitted: true,
    currentSymptoms: ["cough", "fever"],
    symptomChanges: {
      newSymptoms: ["fever"],
      persistentSymptoms: ["cough"],
      resolvedSymptoms: ["fatigue"],
      previousRecordAvailable: true,
    },
    metricChanges: [
      {
        metric: "heartRate",
        unit: "bpm",
        previousUnit: "bpm",
        previousRecordedAt: "2026-10-03T10:00:00",
        latestRecordedAt: "2026-10-04T11:00:00",
        previousValue: 72,
        latestValue: 78,
        absoluteChange: 6,
        percentageChange: 8.33,
        direction: "INCREASED",
      },
      {
        metric: "systolicPressure",
        previousUnit: "mmHg",
        unit: "NOT_RECORDED",
        previousValue: 128,
        latestValue: 120,
        absoluteChange: null,
        direction: "INSUFFICIENT_DATA",
      },
      {
        metric: "bmi",
        previousUnit: "kg/m²",
        unit: "kg/m²",
        previousValue: 25.5,
        latestValue: 23.5,
        absoluteChange: -2,
        direction: "DECREASED",
      },
      {
        metric: "weight",
        previousUnit: "kg",
        unit: "kg",
        previousValue: 72,
        latestValue: 72,
        absoluteChange: 0,
        direction: "STABLE",
      },
    ],
  },
  narrative,
  dataConsidered: ["Current symptoms", "Previous symptoms", "Heart rate"],
  insufficientData: ["Units are not recorded."],
  requiresProfessionalReview: true,
  disclaimer:
    "AI-generated decision support only; not a diagnosis. Review by a qualified healthcare professional is required.",
};
const metrics = [
  {
    id: 2,
    timestamp: "2026-10-04T11:00:00",
    heartRate: 78,
    systolicPressure: 125,
    bloodOxygenLevel: 97,
    weight: 72,
  },
  {
    id: 1,
    timestamp: "2026-10-03T10:00:00",
    heartRate: 72,
    systolicPressure: 120,
    bloodOxygenLevel: 98,
    weight: 72,
  },
];
const reportDraft = {
  draftId: 12,
  patientId: 1,
  patientName: "Aarav Demo",
  filename: "fictional-report.pdf",
  reportDate: "2026-01-01",
  status: "DRAFT",
  allowedUnits: {
    bloodGlucoseLevel: ["mg/dL", "mmol/L"],
    weight: ["kg", "lb"],
  },
  rows: [
    {
      index: 0,
      metric: "bloodGlucoseLevel",
      value: "5.2",
      unit: "mmol/L",
      sourceText: "Glucose 5.2 mmol/L",
      page: 1,
      supported: true,
      evidenceStatus: "TEXT_MATCHED",
    },
    {
      index: 1,
      metric: "weight",
      value: "72",
      unit: "kg",
      sourceText: "Weight 72 kg",
      page: 1,
      supported: true,
      evidenceStatus: "TEXT_MATCHED",
    },
    {
      index: 2,
      metric: "Hemoglobin",
      value: "13.8",
      unit: "g/dL",
      sourceText: "Hemoglobin 13.8 g/dL",
      page: 1,
      supported: false,
      evidenceStatus: "VISUAL_REVIEW_REQUIRED",
    },
  ],
  warnings: ["Check identity and results against the original report."],
};
await page.route("**/auth/**", async (route) => {
  const body = route.request().postDataJSON();
  role = body.username === "doctor" ? "DOCTOR" : "PATIENT";
  await route.fulfill({
    status: 200,
    contentType: "text/plain",
    body: "JWT Token: fake.payload.signature",
  });
});
await page.route("**/api/**", async (route) => {
  const req = route.request(),
    path = new URL(req.url()).pathname;
  assert.equal(req.headers().authorization, "Bearer fake.payload.signature");
  if (expired) return route.fulfill({ status: 401, body: "" });
  let data;
  if (path === "/api/report-imports") {
    assert.ok(
      req.headers()["content-type"].startsWith("multipart/form-data"),
      "Browser must set the multipart boundary",
    );
    assert.ok(req.postData().includes('name="patientId"'));
    data = reportDraft;
  } else if (path === "/api/report-imports/12/confirm") {
    reportConfirmationBody = req.postDataJSON();
    if (importSaveFailure)
      return route.fulfill({
        status: 400,
        contentType: "application/json",
        body: JSON.stringify({
          message: "Please check the selected measurement unit.",
        }),
      });
    data = {
      id: 13,
      patientId: 1,
      bloodGlucoseLevel: 5.2,
      weight: 73.5,
      units: { bloodGlucoseLevel: "mmol/L", weight: "kg" },
    };
  } else if (path === "/api/patients" && role !== "DOCTOR")
    return route.fulfill({ status: 403, body: "{}" });
  else if (path === "/api/patients")
    data = [
      { id: 1, name: "Aarav Demo", age: 30 },
      { id: 2, name: "Meera Demo", age: 42 },
    ];
  else if (path.includes("/healthmetrics")) {
    if (req.method() === "PATCH") {
      unitBody = req.postDataJSON();
      const record = metrics.find(
        (item) => item.id === Number(path.split("/").at(-2)),
      );
      record.units = { ...record.units, ...unitBody.units };
      data = record;
    } else if (req.method() === "POST") {
      metricBody = req.postDataJSON();
      data = { id: 3, ...metricBody };
    } else data = metrics;
  } else if (path.includes("/change-analysis"))
    data = fallback
      ? {
          ...analysis,
          analysisId: null,
          reviewStatus: null,
          narrativeStatus: "UNAVAILABLE",
          narrative: null,
        }
      : firstComparison
        ? {
            ...analysis,
            changes: {
              ...analysis.changes,
              symptomChanges: {
                newSymptoms: ["cough", "fever"],
                previousRecordAvailable: false,
              },
              metricChanges: [],
            },
          }
        : analysis;
  else if (path === "/api/ai/symptom-analysis")
    data = {
      riskLevel: riskEstimate,
      possibleConditions: omitSymptomLists
        ? undefined
        : ["Possible respiratory infection"],
      recommendedTests: omitSymptomLists ? undefined : ["Clinical examination"],
      warningSigns: omitSymptomLists
        ? undefined
        : ["Difficulty breathing", "Chest pain"],
      recommendedSpecialist: omitSymptomLists
        ? undefined
        : "Primary care clinician",
      explanation:
        "These symptoms may have several possible explanations. A clinician should assess your symptoms and history. This full explanation is retained for discussion with your doctor.",
      disclaimer: analysis.disclaimer,
    };
  else if (path.endsWith("/review")) {
    reviewBody = req.postDataJSON();
    reviewed = true;
    data = { reviewId: 9, analysisId: 7, ...reviewBody };
  } else if (path.includes("/health-timeline")) {
    const pageNumber = Number(new URL(req.url()).searchParams.get("page") || 0);
    data = {
      patientId: 1,
      page: pageNumber,
      size: 20,
      totalElements: 21,
      totalPages: 2,
      entries:
        pageNumber === 1
          ? []
          : [
              {
                eventId: 7,
                eventType: "AI_CHANGE_ANALYSIS",
                eventTime: "2026-10-04T12:00:00",
                title: "AI summary of verified health changes",
                reviewStatus: reviewed ? "REVIEWED" : "PENDING_REVIEW",
                details: {
                  ...analysis,
                  reviewStatus: reviewed ? "REVIEWED" : "PENDING_REVIEW",
                },
              },
              {
                eventId: 9,
                eventType: "DOCTOR_REVIEW",
                eventTime: "2026-10-04T11:30:00",
                title: "Doctor review",
                reviewStatus: "FOLLOW_UP_REQUIRED",
                details: {
                  internalNotes: "PRIVATE_SENTINEL",
                  followUpRecommendation: "Arrange a clinic appointment.",
                },
              },
              ...metrics.map((m) => ({
                eventId: m.id,
                eventType: "HEALTH_METRICS_RECORDED",
                eventTime: m.timestamp,
                title: "Health measurements recorded",
                details: {
                  timestamp: m.timestamp,
                  values: { heartRate: m.heartRate, weight: m.weight },
                  unitNotice: "Units were not recorded.",
                },
              })),
            ],
    };
  } else return route.fulfill({ status: 404, body: '{"message":"Not found"}' });
  await route.fulfill({
    status: 200,
    contentType: "application/json",
    body: JSON.stringify(data),
  });
});
async function snapshot(options) {
  await page.evaluate(async () => {
    await document.fonts.ready;
    document.getAnimations().forEach((a) => {
      if (a.effect?.getComputedTiming().iterations !== Infinity) a.finish();
    });
  });
  return page.screenshot(options);
}
async function login(username) {
  await page.getByLabel("Username", { exact: true }).fill(username);
  await page.getByLabel("Password", { exact: true }).fill("synthetic-password");
  await page
    .getByRole("button", {
      name: username === "doctor" ? "Doctor" : "Patient",
      exact: true,
    })
    .click();
  await page.getByRole("button", { name: "Open my workspace" }).click();
  await page
    .getByRole("heading", {
      name:
        username === "doctor"
          ? "Care, on your radar."
          : "A forecast of your recorded story.",
    })
    .waitFor();
  await page
    .getByText("Loading your health workspace…")
    .waitFor({ state: "hidden" });
}
try {
  await page.goto(base);
  await page.getByLabel("Username", { exact: true }).fill("patient");
  await page.getByLabel("Password", { exact: true }).fill("synthetic-password");
  await page.getByRole("button", { name: "Doctor", exact: true }).click();
  await page.getByRole("button", { name: "Open my workspace" }).click();
  await page
    .getByText("This account does not have access.", { exact: false })
    .waitFor();
  await page.getByRole("button", { name: "Patient", exact: true }).click();
  await snapshot({
    path: "test-results/login.png",
    fullPage: true,
    animations: "disabled",
  });
  await login("patient");
  assert.equal(
    await page.getByRole("button", { name: "Upload health report" }).count(),
    0,
  );
  await page.locator(".weather-orb").getByText("78", { exact: true }).waitFor();
  await page.screenshot({
    path: "test-results/patient-dashboard.png",
    fullPage: true,
    animations: "disabled",
  });
  await page
    .getByRole("button", { name: "Health timeline", exact: true })
    .click();
  assert.equal(await page.getByText("PRIVATE_SENTINEL").count(), 0);
  await page.getByRole("button", { name: "Next", exact: true }).click();
  await page.getByText("Page 2 of 2").waitFor();
  await page.getByRole("button", { name: "Previous", exact: true }).click();
  await page.getByText("Page 1 of 2").waitFor();
  await page.getByRole("button", { name: "AI insights", exact: false }).click();
  await page.getByLabel("Symptoms", { exact: true }).fill("cough, fever");
  await page.getByRole("button", { name: "Generate analysis" }).click();
  await page
    .locator(".insight-takeaway")
    .getByText("The recorded heart rate increased.", { exact: true })
    .waitFor();
  await page.getByRole("heading", { name: "Awaiting doctor review" }).waitFor();
  await page
    .getByRole("heading", { name: "What changed in your records" })
    .waitFor();
  await page.locator(".insight-metric").getByText(/^72/).waitFor();
  await page.locator(".insight-metric").getByText(/^78/).waitFor();
  assert.equal(await page.locator(".change-reading").count(), 2);
  await page
    .getByText("1 measurement could not be compared", { exact: true })
    .waitFor();
  assert.equal(
    await page
      .locator(".change-reading")
      .getByText("Weight", { exact: true })
      .count(),
    0,
  );
  await page.screenshot({
    path: "test-results/health-change-summary.png",
    fullPage: true,
    animations: "disabled",
  });
  await page
    .getByText("1 measurement could not be compared", { exact: true })
    .click();
  await page
    .locator(".change-incomplete")
    .getByText("Missing unit · doctor verification needed", { exact: false })
    .waitFor();
  await page
    .getByText("Full comparison and AI details", { exact: true })
    .click();
  await page.getByRole("cell", { name: "+6 bpm", exact: true }).waitFor();
  await page.getByRole("cell", { name: "Weight", exact: true }).waitFor();
  await page.setViewportSize({ width: 390, height: 844 });
  await page.screenshot({
    path: "test-results/change-summary-mobile.png",
    fullPage: true,
  });
  assert.ok(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth + 1,
    ),
  );
  await page.setViewportSize({ width: 1440, height: 1050 });
  firstComparison = true;
  await page.getByRole("button", { name: "Generate analysis" }).click();
  await page.getByText("First recorded symptoms", { exact: true }).waitFor();
  await page
    .getByText("No measurements can be reliably compared yet.")
    .waitFor();
  await page
    .getByText("No earlier symptom record is available.", { exact: false })
    .waitFor();
  firstComparison = false;
  fallback = true;
  await page.getByRole("button", { name: "Generate analysis" }).click();
  await page
    .getByText("AI narrative is currently unavailable.", { exact: false })
    .waitFor();
  assert.equal(
    await page.getByRole("button", { name: "Review analysis" }).count(),
    0,
  );
  assert.equal(
    await page.locator(".insight-takeaway").count(),
    0,
    "Do not show an AI interpretation when the provider is unavailable",
  );
  fallback = false;
  await page
    .getByRole("button", { name: "Symptom analyzer", exact: true })
    .click();
  await page.getByRole("button", { name: "Generate analysis" }).click();
  await page.getByRole("heading", { name: "Possible explanations" }).waitFor();
  await page.getByRole("heading", { name: "Suggested next steps" }).waitFor();
  await page
    .locator(".insight-risk")
    .getByText("low", { exact: true })
    .waitFor();
  await page
    .locator(".insight-warning")
    .getByText("Difficulty breathing", { exact: true })
    .waitFor();
  await page
    .getByRole("heading", { name: "No doctor review attached" })
    .waitFor();
  await page.getByText("Read the full AI explanation", { exact: true }).click();
  await page
    .locator(".insight-full")
    .getByText("This full explanation is retained", { exact: false })
    .waitFor();
  await page.screenshot({
    path: "test-results/symptom-summary.png",
    fullPage: true,
    animations: "disabled",
  });
  await page.setViewportSize({ width: 390, height: 844 });
  await page.screenshot({
    path: "test-results/symptom-summary-mobile.png",
    fullPage: true,
    animations: "disabled",
  });
  assert.ok(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth + 1,
    ),
    "AI summary must fit on mobile",
  );
  await page.setViewportSize({ width: 1440, height: 1050 });
  riskEstimate = "EMERGENCY";
  omitSymptomLists = true;
  await page.getByRole("button", { name: "Generate analysis" }).click();
  await page
    .locator(".insight-risk")
    .getByText("emergency", { exact: true })
    .waitFor();
  await page
    .locator(".insight-warning")
    .getByText("This does not rule out an emergency.", { exact: false })
    .waitFor();
  await page.getByText("No specialist suggested in this result.").waitFor();
  await page.getByRole("button", { name: "Sign out" }).first().click();
  await login("doctor");
  await page
    .getByRole("button", { name: "Add missing units", exact: true })
    .click();
  await page
    .getByLabel("Heart rate missing unit", { exact: true })
    .selectOption("bpm");
  const unitVerification = page.getByLabel(
    "I verified these units against the original report or device record.",
    { exact: true },
  );
  const saveUnits = page.getByRole("button", {
    name: "Save verified units",
    exact: true,
  });
  assert.equal(await saveUnits.isEnabled(), false);
  await unitVerification.check();
  await page
    .getByLabel("Weight missing unit", { exact: true })
    .selectOption("kg");
  assert.equal(await unitVerification.isChecked(), false);
  await unitVerification.check();
  await page.setViewportSize({ width: 390, height: 844 });
  await page.screenshot({
    path: "test-results/missing-units-mobile.png",
    fullPage: true,
  });
  assert.ok(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth + 1,
    ),
  );
  await page.setViewportSize({ width: 1440, height: 1050 });
  await saveUnits.click();
  await page
    .getByText("Measurement units saved successfully.", { exact: true })
    .waitFor();
  assert.deepEqual(unitBody, {
    units: { heartRate: "bpm", weight: "kg" },
    verifiedAgainstSource: true,
  });
  assert.equal(metrics[0].heartRate, 78);
  await page
    .getByRole("button", { name: "Upload health report", exact: true })
    .click();
  await page.getByLabel("Report file", { exact: true }).setInputFiles({
    name: "fictional-report.pdf",
    mimeType: "application/pdf",
    buffer: Buffer.from(
      "%PDF-1.4 Synthetic browser fixture; all responses are intercepted",
    ),
  });
  await page
    .getByRole("button", { name: "Extract measurements with Gemini" })
    .click();
  await page.getByRole("heading", { name: "Check before saving" }).waitFor();
  assert.equal(
    metricBody,
    null,
    "Extracting a report must not save health measurements",
  );
  await page.getByRole("button", { name: "Select supported rows" }).click();
  assert.equal(
    await page.getByLabel("Include result 3", { exact: true }).isChecked(),
    false,
  );
  const verifiedReport = page.getByLabel("I checked the patient identity", {
    exact: false,
  });
  await verifiedReport.check();
  await page.getByLabel("Result value 2", { exact: true }).fill("73.5");
  assert.equal(
    await verifiedReport.isChecked(),
    false,
    "Edits invalidate report confirmation",
  );
  assert.equal(
    await page
      .getByRole("button", { name: "Save 2 confirmed measurements" })
      .isEnabled(),
    false,
  );
  await verifiedReport.check();
  await page.screenshot({
    path: "test-results/report-import-review.png",
    fullPage: true,
    animations: "disabled",
  });
  await page.setViewportSize({ width: 390, height: 844 });
  assert.ok(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth + 1,
    ),
    "Report review must fit on mobile",
  );
  await page.screenshot({
    path: "test-results/report-import-mobile.png",
    fullPage: true,
    animations: "disabled",
  });
  await page.setViewportSize({ width: 1440, height: 1050 });
  await page
    .getByRole("button", { name: "Save 2 confirmed measurements" })
    .click();
  await page.getByText("Please check the selected measurement unit.").waitFor();
  assert.equal(
    await page.getByLabel("Result value 2", { exact: true }).inputValue(),
    "73.5",
  );
  importSaveFailure = false;
  await page
    .getByRole("button", { name: "Save 2 confirmed measurements" })
    .click();
  await page.getByText("Measurements saved successfully.").waitFor();
  assert.equal(reportConfirmationBody.recordedDate, "2026-01-01");
  assert.deepEqual(reportConfirmationBody.rows, [
    { index: 0, metric: "bloodGlucoseLevel", value: 5.2, unit: "mmol/L" },
    { index: 1, metric: "weight", value: 73.5, unit: "kg" },
  ]);
  await page
    .getByRole("button", { name: "Record measurements", exact: true })
    .click();
  await page.getByRole("dialog").waitFor();
  await page.getByLabel("Heart rate", { exact: true }).fill("80");
  await page
    .getByRole("button", { name: "Save measurements", exact: true })
    .click();
  await page
    .getByText("Choose the recorded unit for Heart rate.", { exact: true })
    .waitFor();
  await page.getByLabel("Heart rate unit", { exact: true }).selectOption("bpm");
  await page
    .getByRole("button", { name: "Save measurements", exact: true })
    .click();
  await page.getByText("Measurements saved successfully.").waitFor();
  assert.equal(metricBody.patientId, 1);
  assert.equal(metricBody.heartRate, 80);
  assert.deepEqual(metricBody.units, { heartRate: "bpm" });
  assert.ok(metricBody.timestamp.endsWith(":00"));
  await page.getByRole("button", { name: "Review", exact: true }).click();
  await page.getByLabel("Review status").selectOption("FOLLOW_UP_REQUIRED");
  await page.getByRole("button", { name: "Save doctor review" }).click();
  await page
    .getByText("Add a patient-visible recommendation for follow-up.")
    .waitFor();
  await page.getByLabel("Private doctor notes").fill("Private test note");
  await page
    .getByLabel("Patient-visible recommendation")
    .fill("Book a clinic review.");
  await page.getByRole("button", { name: "Save doctor review" }).click();
  await page.getByText("Review saved successfully.").waitFor();
  assert.equal(reviewBody.status, "FOLLOW_UP_REQUIRED");
  assert.equal(reviewBody.internalNotes, "Private test note");
  await page
    .getByRole("button", { name: "Health timeline", exact: true })
    .click();
  await page.getByText("PRIVATE_SENTINEL").waitFor();
  await page
    .getByText("View AI summary and verified changes", { exact: true })
    .click();
  await page.getByRole("heading", { name: "Doctor review recorded" }).waitFor();
  await page.screenshot({
    path: "test-results/doctor-timeline.png",
    fullPage: true,
    animations: "disabled",
  });
  await page.setViewportSize({ width: 390, height: 844 });
  await page.screenshot({
    path: "test-results/mobile.png",
    fullPage: true,
    animations: "disabled",
  });
  assert.ok(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth + 1,
    ),
    "Mobile layout must not overflow",
  );
  await page.emulateMedia({ reducedMotion: "reduce" });
  assert.equal(
    await page
      .locator(".sidebar")
      .evaluate((el) => getComputedStyle(el).animationName),
    "none",
  );
  expired = true;
  await page.getByRole("button", { name: "Refresh", exact: true }).click();
  await page.getByRole("heading", { name: "Good to see you." }).waitFor();
  await page.getByText("Your session ended. Please sign in again.").waitFor();
  assert.deepEqual(errors, []);
  console.log(
    "PASS: report upload/draft correction, selection and verification, save retry, decimal/unit confirmation and mobile review; patient login, metrics, timeline/privacy, AI summaries/fallback, doctor measurements/review, mobile layout, reduced motion and expired-session logout. Screenshots in test-results/.",
  );
} catch (error) {
  await page.screenshot({
    path: "test-results/failure.png",
    fullPage: true,
    animations: "disabled",
  });
  console.error((await page.locator("body").innerText()).slice(0, 2000));
  throw error;
} finally {
  await browser.close();
}
