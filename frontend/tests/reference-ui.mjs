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
        : [];
  await r.fulfill({
    contentType: "application/json",
    body: JSON.stringify(data),
  });
});
async function shot(name) {
  await page.evaluate(() => document.fonts.ready);
  await page.screenshot({
    path: `test-results/reference-${name}.png`,
    animations: "disabled",
  });
}
try {
  await page.goto(process.env.FRONTEND_TEST_URL || "http://127.0.0.1:5173");
  await page.getByRole("heading", { name: "Welcome back" }).waitFor();
  assert.equal(
    await page
      .locator(".reference-auth-card")
      .evaluate((e) => Math.round(e.getBoundingClientRect().width)),
    410,
  );
  await shot("login");
  await page.getByRole("tab", { name: "Create account", exact: true }).click();
  await page.getByLabel("Full name").waitFor();
  await shot("register");
  await page.getByRole("tab", { name: "Sign in", exact: true }).click();
  await page.getByLabel("Username", { exact: true }).fill("patient");
  await page.getByLabel("Password", { exact: true }).fill("MockPassword123");
  await page
    .getByRole("button", { name: "Show password", exact: true })
    .click();
  assert.equal(
    await page.getByLabel("Password", { exact: true }).getAttribute("type"),
    "text",
  );
  await page
    .getByRole("button", { name: "Hide password", exact: true })
    .click();
  await page.getByRole("button", { name: "Open my workspace" }).click();
  await page
    .getByRole("heading", { name: "Your health, in one sky." })
    .waitFor();
  await page.getByText("112 mg/dL", { exact: true }).waitFor();
  assert.equal(
    await page
      .locator(".reference-shell")
      .evaluate((e) => Math.round(e.getBoundingClientRect().width)),
    400,
  );
  assert.equal(
    await page
      .locator(".sidebar nav")
      .evaluate((e) => getComputedStyle(e).flexDirection),
    "row",
    "Bottom tabs must sit side by side like the HTML reference",
  );
  await shot("sky");
  await page
    .locator(".reference-shell")
    .screenshot({
      path: "test-results/reference-phone.png",
      animations: "disabled",
    });
  await page
    .locator(".reference-shell")
    .screenshot({
      path: "test-results/reference-phone.png",
      animations: "disabled",
    });
  await page.getByRole("button", { name: "Sunrise", exact: true }).click();
  assert.equal(
    await page.locator(".reference-shell").getAttribute("data-sky"),
    "sunrise",
  );
  await page.getByText("112 mg/dL", { exact: true }).waitFor();
  await page
    .getByRole("button", { name: "View Heart rate measurement", exact: true })
    .click();
  await page.getByText("78 bpm", { exact: true }).waitFor();
  await shot("sunrise");
  await page
    .getByRole("button", { name: "Health timeline", exact: true })
    .click();
  await page.getByRole("heading", { name: "Your river" }).waitFor();
  await page.getByRole("button", { name: /Earlier measurements ·/ }).click();
  assert.match(await page.locator(".timeline-event").innerText(), /72/);
  await shot("river");
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
  await page.setViewportSize({ width: 390, height: 844 });
  await shot("booking-mobile");
  assert.ok(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth + 1,
    ),
  );
  await page.getByRole("button", { name: "Overview", exact: true }).click();
  await shot("sky-mobile");
  assert.equal(
    await page
      .locator(".ref-orb")
      .first()
      .evaluate((e) => getComputedStyle(e).animationName),
    "none",
  );
  assert.deepEqual(errors, []);
  console.log(
    "PASS: reference login/register, password visibility, 400px phone shell, real metric orbs, appearance-only sky controls, river selection, visit prep to booking, mobile and reduced motion.",
  );
} finally {
  await browser.close();
}
