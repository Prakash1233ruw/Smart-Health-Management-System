import test from "node:test";
import assert from "node:assert/strict";
import { parseToken, parseSymptoms, request, ApiError } from "../src/api.js";

test("login handles the actual Spring Boot plain-text token format", () => {
  assert.equal(parseToken("JWT Token: abc.def.ghi "), "abc.def.ghi");
  assert.throws(() => parseToken("Unauthorized"), ApiError);
  assert.throws(() => parseToken(""), ApiError);
});
test("symptoms handle comma/newline input, duplicates, and size boundaries", () => {
  assert.deepEqual(parseSymptoms(" cough, fever\n cough,  short   breath "), [
    "cough",
    "fever",
    "short breath",
  ]);
  assert.deepEqual(parseSymptoms(" , \n"), []);
  assert.throws(() => parseSymptoms("x".repeat(201)));
  assert.throws(() =>
    parseSymptoms(Array.from({ length: 21 }, (_, i) => "s" + i).join(",")),
  );
});
test("authorized JSON requests preserve bodies and surface backend errors", async () => {
  const original = global.fetch;
  try {
    global.fetch = async (url, options) => {
      assert.equal(url, "/api/ai/change-analysis/me");
      assert.equal(options.headers.Authorization, "Bearer token");
      assert.deepEqual(JSON.parse(options.body), { symptoms: ["cough"] });
      return new Response('{"analysisId":12}', { status: 200 });
    };
    assert.deepEqual(
      await request("/api/ai/change-analysis/me", {
        token: "token",
        method: "POST",
        body: { symptoms: ["cough"] },
      }),
      { analysisId: 12 },
    );
    global.fetch = async () =>
      new Response('{"message":"Analysis not found"}', { status: 404 });
    await assert.rejects(
      request("/api/ai/analyses/99/review"),
      (error) => error.status === 404 && error.message === "Analysis not found",
    );
    global.fetch = async () => {
      throw new TypeError("fetch failed");
    };
    await assert.rejects(
      request("/auth/login"),
      (error) => error.status === 0 && error.message.includes("Spring Boot"),
    );
  } finally {
    global.fetch = original;
  }
});
test("an expired JWT dispatches logout and aborts are not converted to network failures", async () => {
  const originalFetch = global.fetch,
    originalWindow = global.window;
  try {
    let expired = false;
    global.window = {
      dispatchEvent: (e) => {
        expired = e.type === "session-expired";
      },
    };
    global.fetch = async () => new Response("", { status: 401 });
    await assert.rejects(
      request("/api/health-timeline/me", { token: "expired" }),
      (error) => error.status === 401,
    );
    assert.equal(expired, true);
    global.fetch = async () => {
      throw new DOMException("Aborted", "AbortError");
    };
    await assert.rejects(
      request("/api/patients"),
      (error) => error.name === "AbortError",
    );
  } finally {
    global.fetch = originalFetch;
    global.window = originalWindow;
  }
});
