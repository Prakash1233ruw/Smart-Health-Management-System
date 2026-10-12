import { chromium } from "playwright";
import assert from "node:assert/strict";
import { mkdir } from "node:fs/promises";
const browser = await chromium.launch({ channel: "chrome", headless: true });
const page = await browser.newPage({
  viewport: { width: 1280, height: 900 },
  reducedMotion: "reduce",
});
const errors = [];
page.on("pageerror", (e) => errors.push(e.message));
await mkdir("test-results", { recursive: true });
const records = [
  {
    id: 2,
    timestamp: "2026-10-08T10:00:00",
    heartRate: 78,
    temperature: 36.8,
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
const timeline = {
  patientId: 1,
  page: 0,
  totalPages: 1,
  totalElements: 2,
  entries: records.map((r, i) => ({
    eventId: r.id,
    eventType: "HEALTH_METRICS_RECORDED",
    eventTime: r.timestamp,
    title: i ? "Earlier measurements" : "Latest measurements",
    details: {
      timestamp: r.timestamp,
      values: { heartRate: r.heartRate },
      units: r.units,
    },
  })),
};
await page.route("**/auth/login", (r) =>
  r.fulfill({
    contentType: "text/plain",
    body: "JWT Token: mock.payload.signature",
  }),
);
await page.route("**/api/**", async (r) => {
  const path = new URL(r.request().url()).pathname;
  const data = path.includes("/health-timeline/")
    ? timeline
    : path.includes("/healthmetrics/")
      ? records
      : path === "/api/doctors/directory"
        ? [{ id: 3, name: "Dr. Demo", specialization: "General medicine" }]
        : path === "/api/appointments/visits"
          ? [
              {
                id: 12,
                doctorName: "Dr. Kavya Demo",
                dateTime: "2099-10-10T10:00:00",
                status: "Scheduled",
                summaryAvailable: false,
              },
              {
                id: 11,
                doctorName: "Dr. Kavya Demo",
                dateTime: "2026-01-01T10:00:00",
                status: "Completed",
                summaryAvailable: true,
              },
            ]
          : [];
  await r.fulfill({
    contentType: "application/json",
    body: JSON.stringify(data),
  });
});
async function shot(name) {
  await page.evaluate(() => document.fonts.ready);
  await page.screenshot({
    path: `test-results/dashboard-${name}.png`,
    animations: "disabled",
  });
}
try {
  await page.goto(process.env.FRONTEND_TEST_URL || "http://127.0.0.1:5173");
  await shot("login");
  await page.getByLabel("Username", { exact: true }).fill("patient");
  await page.getByLabel("Password", { exact: true }).fill("MockPassword123");
  await page.getByRole("button", { name: "Open my workspace" }).click();
  await page
    .getByRole("heading", { name: "A clear view of your health." })
    .waitFor();
  await page
    .getByRole("img", { name: /Heart rate: 2 recorded values in bpm/ })
    .waitFor();
  assert.match(
    await page.locator(".dashboard-stats").innerText(),
    /Saved reports\s+2/,
  );
  assert.match(
    await page.locator(".dashboard-stats").innerText(),
    /Upcoming appointments\s+1/,
  );
  assert.match(
    await page.locator(".dashboard-stats").innerText(),
    /Approved visit summaries\s+1/,
  );
  assert.match(
    await page.locator(".schedule-row").innerText(),
    /Dr. Kavya Demo/,
  );
  assert.match(
    await page
      .getByRole("button", { name: "View Blood glucose history" })
      .innerText(),
    /112\s+mg\/dL/,
  );
  await shot("desktop");
  await page.getByRole("button", { name: "View Temperature history" }).click();
  await page
    .getByText(
      "The latest reading needs a recorded unit before it can be charted.",
    )
    .waitFor();
  assert.equal(await page.locator(".measurement-chart").count(), 0);
  await page.getByRole("button", { name: "View Heart rate history" }).click();
  await page.setViewportSize({ width: 390, height: 844 });
  await shot("mobile");
  assert.ok(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth + 1,
    ),
    "Mobile page must not overflow",
  );
  assert.equal(
    await page
      .locator(".health-dashboard")
      .evaluate((e) => getComputedStyle(e).animationName),
    "none",
  );
  await page
    .getByRole("button", { name: "Health timeline", exact: true })
    .click();
  await page.getByRole("button", { name: /Earlier measurements ·/ }).click();
  assert.match(await page.locator(".timeline-event").innerText(), /72/);
  await page
    .getByRole("button", { name: "Visit preparation", exact: true })
    .click();
  await page
    .getByLabel("Reason for your visit")
    .fill("Discuss my recent measurements.");
  await page
    .getByRole("button", { name: "Book a doctor", exact: true })
    .click();
  assert.equal(
    await page.getByLabel("Reason for visit (optional)").inputValue(),
    "Discuss my recent measurements.",
  );
  await shot("booking-mobile");
  assert.deepEqual(errors, []);
  console.log(
    "PASS: backend measurement mapping, history chart, missing-data state, mobile layout, reduced motion, timeline selection and booking handoff.",
  );
} finally {
  await browser.close();
}
