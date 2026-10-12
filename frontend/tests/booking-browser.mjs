import { chromium } from "playwright";
import assert from "node:assert/strict";
import { mkdir } from "node:fs/promises";
const browser = await chromium.launch({ channel: "chrome", headless: true });
const page = await browser.newPage({ viewport: { width: 1440, height: 1100 } });
await mkdir("test-results", { recursive: true });
const errors = [];
page.on("pageerror", (error) => errors.push(error.message));
let registrations = [],
  bookings = [],
  bookBody = null,
  role = "PATIENT",
  failure = false;
const directory = [
  { id: 4, name: "Dr. Kavya Demo", specialization: "General Medicine" },
];
await page.route("**/auth/login", async (route) => {
  role = route.request().postDataJSON().username.includes("doctor")
    ? "DOCTOR"
    : "PATIENT";
  await route.fulfill({
    status: 200,
    contentType: "text/plain",
    body: "JWT Token: mock.payload.signature",
  });
});
await page.route("**/api/**", async (route) => {
  const req = route.request(),
    path = new URL(req.url()).pathname;
  let data;
  if (path.endsWith("/register")) {
    assert.equal(req.headers().authorization, undefined);
    if (failure)
      return route.fulfill({
        status: 500,
        body: '{"message":"private database details"}',
      });
    registrations.push({ path, body: req.postDataJSON() });
    data = { id: 1, name: req.postDataJSON().name };
  } else {
    assert.equal(req.headers().authorization, "Bearer mock.payload.signature");
    if (path.includes("/health-timeline"))
      data = {
        patientId: 1,
        entries: [],
        page: 0,
        size: 20,
        totalElements: 0,
        totalPages: 0,
      };
    else if (path.includes("/healthmetrics")) data = [];
    else if (path === "/api/patients")
      data = [{ id: 1, name: "Aarav Demo", age: 30 }];
    else if (path === "/api/doctors/directory") data = directory;
    else if (path === "/api/appointments/book") {
      bookBody = req.postDataJSON();
      const booking = {
        id: 12,
        patientId: 1,
        status: "Scheduled",
        ...bookBody,
      };
      bookings.push(booking);
      data = booking;
    } else if (req.method() === "DELETE") {
      bookings = bookings.map((item) => ({ ...item, status: "Canceled" }));
      data = "Appointment canceled successfully.";
    } else if (path === "/api/appointments/visits" && role === "PATIENT")
      data = bookings;
    else if (path === "/api/appointments/visits" && role === "DOCTOR")
      data = [
        {
          id: 99,
          patientId: 1,
          doctorId: 4,
          dateTime: "2030-01-03T10:00:00",
          status: "Scheduled",
          notes: "Doctor consultation",
        },
        ...bookings,
      ];
    else return route.fulfill({ status: 404, body: "{}" });
  }
  await route.fulfill({
    status: 200,
    contentType: "application/json",
    body: JSON.stringify(data),
  });
});
async function fillRegistration(username) {
  await page
    .getByLabel("Full name", { exact: true })
    .fill(username.includes("doctor") ? "Demo Doctor" : "Demo Patient");
  await page.getByLabel("Username", { exact: true }).fill(username);
  await page
    .getByLabel("Email", { exact: true })
    .fill(username + "@example.test");
  await page.getByLabel("Password", { exact: true }).fill("MockPassword123");
  await page
    .getByLabel("Confirm password", { exact: true })
    .fill("MockPassword123");
}
async function login() {
  await page.getByLabel("Password", { exact: true }).fill("MockPassword123");
  await page.getByRole("button", { name: "Open my workspace" }).click();
  await page
    .getByRole("button", { name: "Appointments", exact: true })
    .waitFor();
}
async function screenshot(path) {
  await page.evaluate(async () => {
    await document.fonts.ready;
    document.getAnimations().forEach((a) => {
      if (a.effect?.getComputedTiming().iterations !== Infinity) a.finish();
    });
  });
  await page.screenshot({ path, fullPage: true, animations: "disabled" });
}
try {
  await page.goto(process.env.FRONTEND_TEST_URL || "http://127.0.0.1:5173");
  await page.getByRole("button", { name: "Create an account" }).click();
  await fillRegistration("newpatient");
  await page.getByLabel("Age", { exact: true }).fill("30");
  await page
    .getByLabel("Confirm password", { exact: true })
    .fill("OtherPassword123");
  await page
    .getByRole("button", { name: "Create account", exact: true })
    .click();
  await page.getByText("The passwords do not match.").waitFor();
  assert.equal(registrations.length, 0);
  await page
    .getByLabel("Confirm password", { exact: true })
    .fill("MockPassword123");
  failure = true;
  await page
    .getByRole("button", { name: "Create account", exact: true })
    .click();
  await page
    .getByText("Registration could not be completed.", { exact: false })
    .waitFor();
  assert.equal(await page.getByText("private database details").count(), 0);
  failure = false;
  await page
    .getByRole("button", { name: "Create account", exact: true })
    .click();
  await page
    .getByText("Account created. Sign in with your new credentials.")
    .waitFor();
  assert.equal(registrations[0].path, "/api/patients/register");
  assert.equal(registrations[0].body.age, 30);
  await login();
  await page.getByRole("button", { name: "Appointments", exact: true }).click();
  await page.getByText("No appointments yet", { exact: true }).waitFor();
  await page
    .getByRole("button", { name: "Book appointment", exact: true })
    .click();
  await page.getByLabel("Choose doctor").selectOption("4");
  await page.getByLabel("Appointment date and time").fill("2030-01-02T10:00");
  await page
    .getByLabel("Reason for visit (optional)")
    .fill("Discuss recent readings.");
  await screenshot("test-results/booking-form.png");
  await page
    .getByRole("button", { name: "Confirm booking", exact: true })
    .click();
  await page.getByText("Your appointment is booked.").waitFor();
  assert.equal(bookBody.doctorId, 4);
  assert.equal(bookBody.dateTime, "2030-01-02T10:00:00");
  assert.equal(bookBody.patientId, undefined);
  await page.getByRole("heading", { name: "Dr. Kavya Demo" }).waitFor();
  await screenshot("test-results/appointments.png");
  await page.getByRole("button", { name: "Cancel", exact: true }).click();
  await page.getByRole("button", { name: "Keep appointment" }).click();
  assert.equal(bookings[0].status, "Scheduled");
  await page.getByRole("button", { name: "Cancel", exact: true }).click();
  await page.getByRole("button", { name: "Yes, cancel appointment" }).click();
  await page.getByText("Your appointment was canceled.").waitFor();
  await page.getByText("Canceled", { exact: true }).waitFor();
  await page.getByRole("button", { name: "Sign out" }).first().click();
  await page.getByRole("button", { name: "Create an account" }).click();
  await page.getByRole("button", { name: "Doctor", exact: true }).click();
  await fillRegistration("newdoctor");
  await page
    .getByLabel("Specialization", { exact: true })
    .fill("General Medicine");
  await screenshot("test-results/registration.png");
  await page
    .getByRole("button", { name: "Create account", exact: true })
    .click();
  await page
    .getByText("Account created. Sign in with your new credentials.")
    .waitFor();
  assert.equal(registrations[1].path, "/api/doctors/register");
  assert.equal(registrations[1].body.age, undefined);
  await login();
  await page.getByRole("button", { name: "Appointments", exact: true }).click();
  await page.getByRole("heading", { name: "Your visits & briefs" }).waitFor();
  await page.getByText("Doctor consultation", { exact: true }).waitFor();
  await page
    .getByRole("heading", { name: "Aarav Demo", exact: true })
    .waitFor();
  assert.equal(await page.getByText("Patient #1", { exact: true }).count(), 0);
  assert.equal(
    await page
      .getByRole("button", { name: "Book appointment", exact: true })
      .count(),
    0,
  );
  assert.equal(await page.getByText("Canceled", { exact: true }).count(), 0);
  await page.setViewportSize({ width: 390, height: 844 });
  await screenshot("test-results/mobile-appointments.png");
  assert.ok(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth + 1,
    ),
  );
  assert.deepEqual(errors, []);
  console.log(
    "PASS: patient/doctor registration, password confirmation, safe duplicate/error UI, booking, cancellation confirmation, patient list, doctor upcoming list, mobile layout.",
  );
} catch (error) {
  await screenshot("test-results/booking-failure.png");
  console.error((await page.locator("body").innerText()).slice(0, 2500));
  throw error;
} finally {
  await browser.close();
}
