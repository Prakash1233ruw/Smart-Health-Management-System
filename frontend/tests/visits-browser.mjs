import { chromium } from "playwright";
import assert from "node:assert/strict";
import { mkdir } from "node:fs/promises";

// Mocked requests only: no real appointments, patient notes, emails, or Gemini calls.
const browser = await chromium.launch({ channel: "chrome", headless: true });
const page = await browser.newPage({ viewport: { width: 1440, height: 1050 } });
await mkdir("test-results", { recursive: true });
const errors = [],
  calls = [];
page.on("pageerror", (e) => errors.push(e.message));
let role = "PATIENT",
  released = null,
  providerAvailable = false,
  booking = null;
const appointment = {
  id: 12,
  patientId: 1,
  doctorId: 3,
  patientName: "Aarav Demo",
  doctorName: "Dr. Kavya Demo",
  dateTime: "2026-01-01T10:00:00",
  status: "Scheduled",
  notes: "I would like to discuss my recent readings.",
  questions: "When should I measure again?",
  summaryAvailable: false,
};
const brief = {
  appointmentId: 12,
  patientName: appointment.patientName,
  doctorName: appointment.doctorName,
  appointmentTime: appointment.dateTime,
  preparedAt: "2026-10-09T10:00:00",
  reasonForVisit: appointment.notes,
  questions: appointment.questions,
  trends: [
    {
      metric: "heartRate",
      unit: "bpm",
      previousValue: 70,
      latestValue: 84,
      absoluteChange: 14,
      percentageChange: 20,
      previousRecordedAt: "2026-10-01T10:00:00",
      latestRecordedAt: "2026-10-06T10:00:00",
    },
  ],
  latestReports: [
    {
      recordId: 2,
      recordedAt: "2026-10-06T10:00:00",
      values: { heartRate: 84, systolicPressure: 120 },
      units: { heartRate: "bpm" },
    },
  ],
  historicalFlags: [
    {
      analysisId: 8,
      recordedAt: "2026-10-06T10:00:00",
      status: "PENDING_REVIEW",
      urgency: "NOT_RECORDED",
    },
  ],
  missingData: [
    "1 measurement lacks matching recorded units and is excluded from trends.",
  ],
  comparisonRule:
    "Matching explicit units only. Numerical movement is not clinical urgency.",
};
await page.route("**/auth/login", async (route) => {
  role = route.request().postDataJSON().username.toUpperCase();
  await route.fulfill({
    contentType: "text/plain",
    body: "JWT Token: mock.payload.signature",
  });
});
await page.route("**/api/**", async (route) => {
  const r = route.request(),
    path = new URL(r.url()).pathname,
    method = r.method();
  calls.push({ path, method });
  let data;
  assert.equal(r.headers().authorization, "Bearer mock.payload.signature");
  if (path === "/api/admin") data = {};
  else if (path.includes("/health-timeline/"))
    data = {
      patientId: 1,
      entries: [],
      page: 0,
      totalPages: 0,
      totalElements: 0,
    };
  else if (path.includes("/healthmetrics/")) data = [];
  else if (path === "/api/patients")
    data = [{ id: 1, name: "Aarav Demo", age: 30 }];
  else if (["/api/doctors", "/api/doctors/directory"].includes(path))
    data = [
      { id: 3, name: "Dr. Kavya Demo", specialization: "General medicine" },
    ];
  else if (["/api/appointments", "/api/appointments/visits"].includes(path))
    data = [appointment];
  else if (path === "/api/appointments/12/brief") {
    assert.equal(role, "DOCTOR");
    data = brief;
  } else if (path === "/api/appointments/12/summary/draft") {
    assert.equal(role, "DOCTOR");
    assert.equal(
      r.postDataJSON().doctorNotes,
      "We discussed the readings and agreed to recheck next week.",
    );
    data = providerAvailable
      ? {
          summary: "We discussed your readings. Recheck them next week.",
          aiAvailable: true,
          notice: "AI wording draft. Review before approving.",
        }
      : {
          summary: "",
          aiAvailable: false,
          notice:
            "AI wording is unavailable. Write the patient summary below; your notes have not been published.",
        };
  } else if (path === "/api/appointments/12/summary" && method === "POST") {
    const body = r.postDataJSON();
    assert.equal(role, "DOCTOR");
    assert.equal(body.approve, true);
    assert.equal(body.visitCompleted, true);
    assert.deepEqual(body.actions, [
      "Bring your readings next time.",
      "Ask the clinic if the plan is unclear.",
    ]);
    released = {
      appointmentId: 12,
      doctorId: 3,
      doctorName: "Dr. Kavya Demo",
      approvedAt: "2026-10-09T10:10:00",
      ...body,
    };
    appointment.summaryAvailable = true;
    appointment.status = "Completed";
    data = released;
  } else if (path === "/api/appointments/12/summary")
    data = {
      status: released ? "APPROVED" : "AWAITING_DOCTOR_APPROVAL",
      summary: released,
    };
  else if (path === "/api/appointments/book") {
    booking = r.postDataJSON();
    data = { id: 13, ...booking };
  } else if (path === "/api/appointments/send-reminders") data = {};
  else
    return route.fulfill({
      status: 404,
      contentType: "application/json",
      body: '{"message":"Unexpected request"}',
    });
  await route.fulfill({
    contentType: "application/json",
    body: JSON.stringify(data),
  });
});
async function login(nextRole) {
  if (await page.getByRole("button", { name: "Sign out", exact: true }).count())
    await page
      .getByRole("button", { name: "Sign out", exact: true })
      .first()
      .click();
  await page
    .getByRole("button", {
      name: nextRole[0] + nextRole.slice(1).toLowerCase(),
      exact: true,
    })
    .click();
  await page
    .getByLabel("Username", { exact: true })
    .fill(nextRole.toLowerCase());
  await page.getByLabel("Password", { exact: true }).fill("MockPassword123");
  await page
    .getByRole("button", { name: "Open my workspace", exact: true })
    .click();
  if (nextRole !== "ADMIN")
    await page
      .getByRole("button", { name: "Appointments", exact: true })
      .waitFor();
}
async function shot(name) {
  await page.evaluate(() => document.fonts.ready);
  await page.screenshot({
    path: `test-results/visit-${name}.png`,
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
  await page.goto(process.env.FRONTEND_TEST_URL || "http://127.0.0.1:5173");
  await login("PATIENT");
  assert.equal(
    await page
      .getByRole("button", { name: "AI insights", exact: true })
      .count(),
    0,
  );
  await page.getByRole("button", { name: "Appointments", exact: true }).click();
  await page
    .getByRole("button", { name: "Summary status", exact: true })
    .click();
  await page
    .getByText("Your visit summary is awaiting your doctor’s approval.", {
      exact: false,
    })
    .waitFor();
  assert.equal(calls.filter((c) => c.path.endsWith("/brief")).length, 0);
  await login("DOCTOR");
  await page.getByRole("button", { name: "Appointments", exact: true }).click();
  await page
    .getByRole("button", { name: "Open visit brief", exact: true })
    .click();
  await page
    .getByRole("heading", { name: "What changed", exact: true })
    .waitFor();
  await page
    .getByText("Largest numerical movements, not a clinical priority ranking.")
    .waitFor();
  await page
    .getByText("Urgency not recorded; clinical assessment needed.")
    .waitFor();
  await shot("doctor-brief");
  assert.ok(await page.locator(".visit-values").getByText("84 bpm", { exact: true }).isVisible(), "Latest measurements must be visible without expanding a report");
  assert.ok(await page.locator(".visit-values").getByText("120 unit not recorded", { exact: true }).isVisible(), "Missing units must not hide saved values");
  await page
    .getByLabel("Doctor’s visit notes", { exact: true })
    .fill("We discussed the readings and agreed to recheck next week.");
  await page
    .getByRole("button", { name: "Help simplify my notes", exact: true })
    .click();
  await page
    .getByText("AI wording is unavailable.", { exact: false })
    .waitFor();
  assert.equal(released, null);
  providerAvailable = true;
  await page
    .getByRole("button", { name: "Help simplify my notes", exact: true })
    .click();
  await page
    .getByLabel("Patient summary", { exact: true })
    .filter({ visible: true })
    .waitFor();
  await page.waitForFunction(() =>
    document
      .querySelector('.visit-editor textarea[maxlength="3000"]')
      .value.startsWith("We discussed"),
  );
  await page
    .getByLabel("Actions for the patient · one per line", { exact: true })
    .fill(
      "Bring your readings next time.\nAsk the clinic if the plan is unclear.",
    );
  await page
    .getByLabel("Suggested follow-up date (optional)")
    .fill("2030-01-02");
  const approve = page.getByRole("button", {
    name: "Approve & release to patient",
    exact: true,
  });
  assert.ok(await approve.isDisabled());
  const check = page.getByRole("checkbox", {
    name: "The visit has taken place.",
    exact: false,
  });
  await check.check();
  assert.ok(await approve.isEnabled());
  await page
    .getByLabel("Patient summary", { exact: true })
    .fill("We discussed your readings. Follow the plan we agreed.");
  assert.ok(await approve.isDisabled(), "Editing resets approval");
  await check.check();
  await page.setViewportSize({ width: 390, height: 844 });
  await noOverflow();
  await shot("doctor-mobile");
  await approve.click();
  await page
    .getByText("Approved and released to this patient in the app.")
    .waitFor();
  assert.ok(released);
  await page.setViewportSize({ width: 1440, height: 1050 });
  await login("PATIENT");
  await page.getByRole("button", { name: "Appointments", exact: true }).click();
  await page
    .getByRole("button", { name: "Read visit summary", exact: true })
    .click();
  await page
    .getByRole("heading", { name: "Your visit, in simple words" })
    .waitFor();
  await shot("patient-summary");
  await page
    .getByRole("checkbox", { name: "Bring your readings next time." })
    .check();
  await page
    .getByRole("button", { name: "Book follow-up", exact: true })
    .click();
  assert.equal(await page.getByLabel("Choose doctor").inputValue(), "3");
  assert.equal(
    await page.getByLabel("Appointment date and time").inputValue(),
    "2030-01-02T09:00",
  );
  assert.equal(
    booking,
    null,
    "Follow-up button must not book without confirmation",
  );
  await page
    .getByLabel("Questions for your doctor (optional)")
    .fill("What should I bring next time?");
  await page
    .getByRole("button", { name: "Confirm booking", exact: true })
    .click();
  await page
    .getByText("Your appointment is booked.", { exact: false })
    .waitFor();
  assert.equal(booking.questions, "What should I bring next time?");
  assert.equal(booking.doctorId, 3);
  await login("ADMIN");
  await page
    .getByRole("heading", { name: "A calm view of the whole system." })
    .waitFor();
  await page.getByRole("cell", { name: "Aarav Demo", exact: true }).waitFor();
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
    calls.filter(
      (c) => c.path.includes("/api/ai/") || c.path.includes("/report-imports"),
    ).length,
    0,
  );
  assert.deepEqual(errors, []);
  console.log(
    "PASS: private pending state, doctor brief, AI fallback, editable draft, explicit approval/reset, patient checklist, follow-up prefills, booking questions, admin reminders, mobile layout, and no retired AI calls.",
  );
} catch (e) {
  await shot("failure");
  console.error((await page.locator("body").innerText()).slice(-4000));
  throw e;
} finally {
  await browser.close();
}
