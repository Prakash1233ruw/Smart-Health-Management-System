import { chromium } from "playwright";
import assert from "node:assert/strict";
import { mkdir } from "node:fs/promises";

// Fictional intercepted data: no real API writes, emails, Gemini calls, or accounts.
const browser = await chromium.launch({ channel: "chrome", headless: true });
const page = await browser.newPage({ viewport: { width: 1440, height: 1050 } });
await mkdir("test-results", { recursive: true });
const errors = [],
  calls = [];
page.on("pageerror", (e) => errors.push(e.message));
let role = "PATIENT";
const patients = [
  { id: 1, name: "Aarav Demo", age: 30 },
  { id: 2, name: "Meera Demo", age: 42 },
];
const doctors = [
  { id: 3, name: "Dr. Kavya Demo", specialization: "General Medicine" },
];
const readings = [
  {
    id: 2,
    patientId: 1,
    timestamp: "2026-10-08T10:00:00",
    heartRate: 78,
    bloodGlucoseLevel: 112,
    systolicPressure: 128,
    units: {
      heartRate: "bpm",
      bloodGlucoseLevel: "mg/dL",
      systolicPressure: "mmHg",
    },
  },
  {
    id: 1,
    patientId: 1,
    timestamp: "2026-10-01T10:00:00",
    heartRate: 72,
    bloodGlucoseLevel: 98,
    systolicPressure: 118,
    units: {
      heartRate: "bpm",
      bloodGlucoseLevel: "mg/dL",
      systolicPressure: "mmHg",
    },
  },
];
const analysis = {
  analysisId: 7,
  reviewStatus: "PENDING_REVIEW",
  narrativeStatus: "AVAILABLE",
  narrative: {
    summary:
      "Your recorded heart rate increased between visits. Discuss the readings with your doctor.",
    importantChanges: [],
    itemsForDoctorReview: ["Confirm how the readings were taken."],
    questionsForDoctor: [],
  },
  changes: {
    symptomsSubmitted: false,
    metricChanges: [
      {
        metric: "heartRate",
        previousUnit: "bpm",
        unit: "bpm",
        previousValue: 72,
        latestValue: 78,
        absoluteChange: 6,
        direction: "INCREASED",
      },
    ],
  },
};
const appointment = {
  id: 12,
  patientId: 1,
  doctorId: 3,
  dateTime: "2030-10-20T10:00:00",
  status: "Scheduled",
  notes: "Follow-up consultation",
};
await page.route("**/auth/login", async (route) => {
  role = route.request().postDataJSON().username.toUpperCase();
  await route.fulfill({
    status: 200,
    contentType: "text/plain",
    body: "JWT Token: fake.payload.signature",
  });
});
await page.route("**/api/**", async (route) => {
  const req = route.request(),
    url = new URL(req.url()),
    path = url.pathname;
  calls.push({ path, method: req.method() });
  assert.equal(req.headers().authorization, "Bearer fake.payload.signature");
  let data;
  if (path === "/api/admin") {
    if (role !== "ADMIN") return route.fulfill({ status: 403, body: "{}" });
    data = [{ id: 1 }];
  } else if (req.method() === "DELETE") data = {};
  else if (path === "/api/appointments/send-reminders") data = "";
  else if (path === "/api/patients") data = patients;
  else if (path === "/api/doctors" || path === "/api/doctors/directory")
    data = doctors;
  else if (
    [
      "/api/appointments",
      "/api/appointments/myappointments",
      "/api/doctors/myupcoming-appointments",
    ].includes(path)
  )
    data = [appointment];
  else if (path.includes("healthmetrics")) data = readings;
  else if (path.includes("health-timeline")) {
    const index = Number(url.searchParams.get("page") || 0);
    const patientId = path.endsWith("/2") ? 2 : 1;
    data = {
      patientId,
      page: index,
      size: 20,
      totalElements: 3,
      totalPages: patientId === 1 ? 2 : 1,
      entries:
        patientId === 2
          ? []
          : index === 0
            ? [
                {
                  eventId: 7,
                  eventType: "AI_CHANGE_ANALYSIS",
                  title: "Health change summary",
                  reviewStatus: "PENDING_REVIEW",
                  eventTime: "2026-10-08T12:00:00",
                  details: analysis,
                },
                {
                  eventId: 2,
                  eventType: "HEALTH_METRICS_RECORDED",
                  title: "Recent measurements",
                  eventTime: readings[0].timestamp,
                  details: {
                    values: { heartRate: 78, bloodGlucoseLevel: 112 },
                    units: readings[0].units,
                  },
                },
              ]
            : [
                {
                  eventId: 1,
                  eventType: "HEALTH_METRICS_RECORDED",
                  title: "Earlier measurements",
                  eventTime: readings[1].timestamp,
                  details: {
                    values: { heartRate: 72 },
                    units: readings[1].units,
                  },
                },
              ],
    };
  } else return route.fulfill({ status: 404, body: "{}" });
  await route.fulfill({
    status: 200,
    contentType: "application/json",
    body: JSON.stringify(data),
  });
});
async function login(kind) {
  await page.goto("http://127.0.0.1:5173/");
  await page.getByLabel("Sky theme", { exact: true }).selectOption("day");
  await page
    .getByRole("button", {
      name: kind[0] + kind.slice(1).toLowerCase(),
      exact: true,
    })
    .click();
  await page.getByLabel("Username", { exact: true }).fill(kind.toLowerCase());
  await page
    .getByLabel("Password", { exact: true })
    .fill("FictionalPassword123");
  await page.getByRole("button", { name: "Open my workspace" }).click();
}
async function shot(name) {
  await page.screenshot({
    path: `test-results/weather-${name}.png`,
    fullPage: true,
    animations: "disabled",
  });
}
async function noOverflow() {
  assert.ok(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth + 1,
    ),
  );
}
try {
  await login("PATIENT");
  await page.getByRole("region", { name: "Body Weather home" }).waitFor();
  await page.locator(".weather-orb").getByText("78", { exact: true }).waitFor();
  await shot("sky-day");
  await page
    .getByLabel("Choose previous or latest recorded measurements")
    .focus();
  await page
    .getByLabel("Choose previous or latest recorded measurements")
    .press("Home");
  await page.locator(".weather-orb").getByText("72", { exact: true }).waitFor();
  await page.getByLabel("Sky theme", { exact: true }).selectOption("night");
  await shot("sky-night");
  await page.getByLabel("Sky theme", { exact: true }).selectOption("day");
  await page.setViewportSize({ width: 900, height: 1050 });
  await page
    .getByRole("button", { name: "Health timeline", exact: true })
    .click();
  await page.getByText("Health change summary", { exact: true }).waitFor();
  await page
    .locator(".timeline")
    .evaluate((el) => el.scrollTo({ left: el.scrollWidth }));
  await page.getByText("Page 2 of 2", { exact: true }).waitFor();
  await page.getByText("Earlier measurements", { exact: true }).waitFor();
  assert.equal(await page.locator(".timeline-event").count(), 3);
  await shot("river");
  await page.setViewportSize({ width: 390, height: 844 });
  await noOverflow();
  await shot("river-mobile");
  await page.getByRole("button", { name: "Overview", exact: true }).click();
  await noOverflow();
  await shot("sky-mobile");
  await page.setViewportSize({ width: 1440, height: 1050 });
  await login("DOCTOR");
  await page
    .getByRole("heading", { name: "Human attention at the center." })
    .waitFor();
  await page
    .getByRole("button", {
      name: "Select Aarav Demo: 1 pending reviews in loaded events",
      exact: true,
    })
    .waitFor();
  await shot("radar");
  assert.equal(
    calls.filter(
      (c) => c.path.includes("change-analysis") && c.method === "POST",
    ).length,
    0,
    "Radar must not generate Gemini analyses",
  );
  await page
    .getByRole("button", {
      name: "Select Meera Demo: 0 pending reviews in loaded events",
      exact: true,
    })
    .click();
  await page.getByText("A fresh beginning", { exact: true }).waitFor();
  await login("ADMIN");
  await page
    .getByRole("heading", { name: "A calm view of the whole system." })
    .waitFor();
  await page.getByRole("cell", { name: "Aarav Demo", exact: true }).waitFor();
  await shot("tower");
  await page
    .getByRole("button", { name: "Delete account", exact: true })
    .first()
    .click();
  assert.equal(calls.filter((c) => c.method === "DELETE").length, 0);
  await page.getByRole("button", { name: "Keep unchanged" }).click();
  await page
    .getByRole("button", { name: "Send reminders", exact: true })
    .click();
  assert.equal(
    calls.filter((c) => c.path.endsWith("send-reminders")).length,
    0,
  );
  await page
    .getByRole("button", { name: "Send reminder emails", exact: true })
    .click();
  await page
    .getByText("Reminder request completed.", { exact: true })
    .waitFor();
  assert.equal(
    calls.filter((c) => c.path.endsWith("send-reminders")).length,
    1,
  );
  await page.setViewportSize({ width: 390, height: 844 });
  await noOverflow();
  await shot("tower-mobile");
  await page.getByLabel("Sky theme", { exact: true }).selectOption("night");
  await noOverflow();
  await shot("tower-night");
  assert.deepEqual(errors, []);
  console.log(
    "PASS: patient sky, real before/after values, day/night themes, infinite river pagination, radar without Gemini calls, patient selection, verified admin login, guarded deletes/reminders, and mobile layouts.",
  );
} catch (e) {
  await shot("failure");
  console.error((await page.locator("body").innerText()).slice(0, 2200));
  throw e;
} finally {
  await browser.close();
}
