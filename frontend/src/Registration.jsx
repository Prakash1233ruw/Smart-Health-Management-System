import React, { useState } from "react";
import { request } from "./api.js";

export default function Registration({
  onBack,
  onCreated,
  initialRole = "PATIENT",
  onRoleChange = () => {},
}) {
  const [role, setRole] = useState(initialRole);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setError("");
    const fields = new FormData(event.currentTarget);
    try {
      if (fields.get("password") !== fields.get("confirm"))
        throw new Error("The passwords do not match.");
      if (new TextEncoder().encode(fields.get("password")).length > 72)
        throw new Error(
          "Password must fit within 72 UTF-8 bytes. Use fewer characters.",
        );
      const body = {
        name: fields.get("name").trim(),
        username: fields.get("username").trim(),
        email: fields.get("email").trim(),
        password: fields.get("password"),
      };
      if (!body.name || !body.username)
        throw new Error("Enter your name and username.");
      if (role === "PATIENT") {
        body.age = Number(fields.get("age"));
        body.medicalCondition = fields.get("condition").trim();
      } else body.specialization = fields.get("specialization").trim();
      await request(
        role === "PATIENT" ? "/api/patients/register" : "/api/doctors/register",
        { method: "POST", body },
      );
      onCreated({ role, username: body.username });
    } catch (error) {
      setError(
        error.status >= 500
          ? "Registration could not be completed. Your username or email may already be used; try different details or check the server."
          : error.message,
      );
    } finally {
      setBusy(false);
    }
  }
  return (
    <div className="login-form registration-form">
      <h1>Create your account</h1>
      <p className="ref-lead">
        Create your account to start managing your care.
      </p>
      <div className="ref-segment" role="tablist" aria-label="Account access">
        <button
          role="tab"
          aria-selected="false"
          onClick={onBack}
          disabled={busy}
        >
          Sign in
        </button>
        <button role="tab" aria-selected="true" disabled={busy}>
          Create account
        </button>
      </div>
      <div className="role-switch">
        <button
          disabled={busy}
          aria-pressed={role === "PATIENT"}
          onClick={() => {
            setRole("PATIENT");
            onRoleChange("PATIENT");
            setError("");
          }}
        >
          Patient
        </button>
        <button
          disabled={busy}
          aria-pressed={role === "DOCTOR"}
          onClick={() => {
            setRole("DOCTOR");
            onRoleChange("DOCTOR");
            setError("");
          }}
        >
          Doctor
        </button>
      </div>
      <form onSubmit={submit}>
        <label>
          Full name
          <input name="name" autoComplete="name" required maxLength={120} />
        </label>
        <div className="measurement-grid">
          <label>
            Username
            <input
              name="username"
              autoComplete="username"
              required
              maxLength={100}
            />
          </label>
          <label>
            Email
            <input
              name="email"
              type="email"
              autoComplete="email"
              required
              maxLength={200}
            />
          </label>
        </div>
        {role === "PATIENT" ? (
          <>
            <label>
              Age
              <input
                name="age"
                type="number"
                min="0"
                max="130"
                step="1"
                required
              />
            </label>
            <label>
              Medical condition (optional)
              <input
                name="condition"
                maxLength={250}
                placeholder="Existing conditions you want to record"
              />
            </label>
          </>
        ) : (
          <label>
            Specialization
            <input
              name="specialization"
              required
              maxLength={200}
              placeholder="For example: General Medicine"
            />
          </label>
        )}
        <div className="measurement-grid">
          <label>
            Password
            <input
              name="password"
              type="password"
              autoComplete="new-password"
              required
              minLength={8}
              maxLength={72}
            />
          </label>
          <label>
            Confirm password
            <input
              name="confirm"
              type="password"
              autoComplete="new-password"
              required
              maxLength={72}
            />
          </label>
        </div>
        <p className="small muted">
          Use at least 8 characters. For this demonstration, doctor registration
          follows the existing backend’s public registration policy.
        </p>
        {error && (
          <div className="notice error" role="alert">
            {error}
          </div>
        )}
        <button className="primary wide" disabled={busy}>
          {busy ? "Creating account…" : "Create account"}
        </button>
      </form>
    </div>
  );
}
