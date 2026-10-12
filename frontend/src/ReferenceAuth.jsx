import React, { useState } from "react";
import Registration from "./Registration.jsx";
import { parseToken, request } from "./api.js";

export default function ReferenceAuth({ onLogin, expired }) {
  const [registering, setRegistering] = useState(false),
    [role, setRole] = useState("PATIENT"),
    [created, setCreated] = useState(null);
  const [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [showPassword, setShowPassword] = useState(false);
  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setError("");
    const fields = new FormData(event.currentTarget);
    try {
      const username = fields.get("username").trim();
      const token = parseToken(
        await request("/auth/login", {
          method: "POST",
          body: { username, password: fields.get("password") },
        }),
      );
      await request(
        role === "PATIENT"
          ? "/api/health-timeline/me?size=1"
          : role === "ADMIN"
            ? "/api/admin"
            : "/api/patients",
        { token },
      );
      onLogin({ token, role, username });
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }
  return (
    <main className="reference-auth" data-role={role.toLowerCase()}>
      <div className="ref-sun" aria-hidden="true" />
      <div className="ref-cloud c1" aria-hidden="true" />
      <div className="ref-cloud c2" aria-hidden="true" />
      <section className="reference-auth-card">
        {registering ? (
          <Registration
            initialRole={role === "ADMIN" ? "PATIENT" : role}
            onRoleChange={setRole}
            onBack={() => setRegistering(false)}
            onCreated={(account) => {
              setCreated(account);
              setRole(account.role);
              setError("");
              setRegistering(false);
            }}
          />
        ) : (
          <>
            <h1>Welcome back</h1>
            <p className="ref-lead">
              Your records, appointments and care plans in one place.
            </p>
            <div
              className="ref-segment"
              role="tablist"
              aria-label="Account access"
            >
              <button role="tab" aria-selected="true" disabled={busy}>
                Sign in
              </button>
              <button
                role="tab"
                aria-selected="false"
                disabled={busy}
                onClick={() => setRegistering(true)}
              >
                Create account
              </button>
            </div>
            <div className="role-switch" aria-label="Choose workspace">
              {["PATIENT", "DOCTOR"].map((r) => (
                <button
                  key={r}
                  disabled={busy}
                  aria-pressed={role === r}
                  onClick={() => {
                    setRole(r);
                    setError("");
                  }}
                >
                  {r === "PATIENT" ? "Patient" : "Doctor"}
                </button>
              ))}
            </div>
            {role === "ADMIN" && <p className="ref-lead">Admin access</p>}
            {expired && (
              <p className="notice" role="status">
                Your session ended. Please sign in again.
              </p>
            )}
            {created && (
              <p className="notice" role="status">
                Account created. Sign in with your new credentials.
              </p>
            )}
            {error && (
              <p className="notice error" role="alert">
                {error}
              </p>
            )}
            <form onSubmit={submit}>
              <label htmlFor="signin-username">Username</label>
              <input
                id="signin-username"
                name="username"
                autoComplete="username"
                required
                maxLength={255}
                defaultValue={created?.username || ""}
                placeholder="Your existing username"
                disabled={busy}
              />
              <label htmlFor="signin-password">Password</label>
              <div className="ref-password">
                <input
                  id="signin-password"
                  name="password"
                  type={showPassword ? "text" : "password"}
                  autoComplete="current-password"
                  required
                  placeholder="Enter your password"
                  disabled={busy}
                />
                <button
                  type="button"
                  className="ref-eye"
                  aria-label={showPassword ? "Hide password" : "Show password"}
                  onClick={() => setShowPassword(!showPassword)}
                >
                  {showPassword ? "Hide" : "Show"}
                </button>
              </div>
              <button
                className="primary wide"
                aria-label="Open my workspace"
                disabled={busy}
              >
                {busy ? "One moment…" : "Sign in"}
              </button>
            </form>
            <p className="ref-alt">
              New here?{" "}
              <button disabled={busy} onClick={() => setRegistering(true)}>
                Create an account
              </button>
            </p>
            <p className="ref-alt">
              <button
                aria-label="Admin"
                disabled={busy}
                onClick={() => {
                  setRole("ADMIN");
                  setError("");
                }}
              >
                Admin access
              </button>
            </p>
          </>
        )}
      </section>
    </main>
  );
}
