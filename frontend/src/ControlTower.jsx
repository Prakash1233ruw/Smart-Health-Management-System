import React, { useEffect, useState } from "react";
import { request } from "./api.js";
import { SkyArt, ThemeSwitch } from "./BodyWeather.jsx";

export default function ControlTower({
  session,
  onLogout,
  Brand,
  Modal,
  Notice,
}) {
  const [data, setData] = useState({
    doctors: [],
    patients: [],
    appointments: [],
  });
  const [tab, setTab] = useState("patients"),
    [query, setQuery] = useState("");
  const [loading, setLoading] = useState(true),
    [busy, setBusy] = useState(false),
    [refresh, setRefresh] = useState(0);
  const [error, setError] = useState(""),
    [message, setMessage] = useState(""),
    [action, setAction] = useState(null);
  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError("");
    Promise.all(
      ["doctors", "patients", "appointments"].map((key) =>
        request(`/api/${key}`, {
          token: session.token,
          signal: controller.signal,
        }),
      ),
    )
      .then(([doctors, patients, appointments]) => {
        if (!controller.signal.aborted)
          setData({ doctors, patients, appointments });
      })
      .catch((e) => {
        if (e.name !== "AbortError") setError(e.message);
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [session.token, refresh]);
  const patientName = (id) =>
    data.patients.find((p) => String(p.id) === String(id))?.name ||
    "Patient name unavailable";
  const doctorName = (id) =>
    data.doctors.find((d) => String(d.id) === String(id))?.name ||
    "Doctor name unavailable";
  async function execute() {
    setBusy(true);
    setError("");
    try {
      await request(
        action.kind === "reminders"
          ? "/api/appointments/send-reminders"
          : `/api/${action.tab}/${action.row.id}`,
        {
          token: session.token,
          method: action.kind === "reminders" ? "POST" : "DELETE",
        },
      );
      setMessage(
        action.kind === "reminders"
          ? "Reminder request completed."
          : "Account deleted.",
      );
      setAction(null);
      setRefresh((n) => n + 1);
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }
  return (
    <div className="control-tower">
      <header className="tower-header">
        <Brand />
        <span className="eyebrow">ADMIN · CONTROL TOWER</span>
        <div>
          <ThemeSwitch />
          <button className="secondary" onClick={onLogout}>
            Sign out
          </button>
        </div>
      </header>
      <main>
        <section className="tower-hero">
          <SkyArt mood="steady" />
          <div>
            <span className="eyebrow">CARE OPERATIONS, IN PERSPECTIVE</span>
            <h1>A calm view of the whole system.</h1>
            <p>Manage the people and appointments behind every care journey.</p>
          </div>
        </section>
        <div className="tower-counts">
          {["patients", "doctors", "appointments"].map((key) => (
            <button key={key} onClick={() => setTab(key)}>
              <span className="tower-ring">
                <strong>{loading ? "—" : data[key].length}</strong>
              </span>
              <span>{key}</span>
            </button>
          ))}
        </div>
        {error && <Notice error>{error}</Notice>}
        {message && <Notice>{message}</Notice>}
        <section className="panel">
          <div className="section-head">
            <h2>Care directory</h2>
            <div className="measurement-actions">
              <button
                className="secondary"
                disabled={loading}
                onClick={() => setRefresh((n) => n + 1)}
              >
                Refresh
              </button>
              <button
                className="primary"
                disabled={busy}
                onClick={() => setAction({ kind: "reminders" })}
              >
                Send reminders
              </button>
            </div>
          </div>
          <div className="role-switch">
            {["patients", "doctors", "appointments"].map((key) => (
              <button
                key={key}
                aria-pressed={tab === key}
                onClick={() => {
                  setTab(key);
                  setQuery("");
                }}
              >
                {key}
              </button>
            ))}
          </div>
          <label>
            Search {tab}
            <input
              type="search"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Search by name or ID"
            />
          </label>
          {loading ? (
            <p role="status">Loading care operations…</p>
          ) : (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>
                      {tab === "appointments"
                        ? "Patient / doctor"
                        : "Full name"}
                    </th>
                    <th>
                      {tab === "appointments"
                        ? "Appointment / status"
                        : tab === "doctors"
                          ? "Specialization"
                          : "Age"}
                    </th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {data[tab]
                    .filter((row) =>
                      `${row.id} ${row.name || ""} ${tab === "appointments" ? patientName(row.patientId) + doctorName(row.doctorId) : ""}`
                        .toLowerCase()
                        .includes(query.toLowerCase()),
                    )
                    .map((row) => (
                      <tr key={row.id}>
                        <td>#{row.id}</td>
                        <td>
                          {tab === "appointments" ? (
                            <>
                              {patientName(row.patientId)}
                              <small className="tower-cell-sub">
                                {doctorName(row.doctorId)}
                              </small>
                            </>
                          ) : (
                            row.name || "Name not available"
                          )}
                        </td>
                        <td>
                          {tab === "appointments" ? (
                            <>
                              {new Date(row.dateTime).toLocaleString()}
                              <small className="tower-cell-sub">
                                {row.status}
                              </small>
                            </>
                          ) : tab === "doctors" ? (
                            row.specialization
                          ) : (
                            (row.age ?? "—")
                          )}
                        </td>
                        <td>
                          {tab !== "appointments" ? (
                            <button
                              className="text-button"
                              onClick={() =>
                                setAction({ kind: "delete", tab, row })
                              }
                            >
                              Delete account
                            </button>
                          ) : (
                            "Managed by patient"
                          )}
                        </td>
                      </tr>
                    ))}
                </tbody>
              </table>
              {!data[tab].length && <p>No {tab} recorded yet.</p>}
            </div>
          )}
        </section>
      </main>
      {action && (
        <Modal
          title={
            action.kind === "reminders"
              ? "Send appointment reminders?"
              : "Delete account?"
          }
          busy={busy}
          onClose={() => setAction(null)}
        >
          <p>
            {action.kind === "reminders"
              ? "This sends reminder emails for appointments selected by the backend reminder service."
              : `Delete ${action.row.name || "this account"} (#${action.row.id}) and its associated records? This cannot be undone here.`}
          </p>
          <button
            className="secondary"
            disabled={busy}
            onClick={() => setAction(null)}
          >
            Keep unchanged
          </button>
          <button className="danger-button" disabled={busy} onClick={execute}>
            {busy
              ? "Working…"
              : action.kind === "reminders"
                ? "Send reminder emails"
                : "Delete account permanently"}
          </button>
          {error && <Notice error>{error}</Notice>}
        </Modal>
      )}
    </div>
  );
}
