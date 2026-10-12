import React, { useEffect, useState } from "react";
import { request } from "./api.js";

const fields = [
  ["heartRate", "Heart rate"],
  ["systolicPressure", "Systolic pressure"],
  ["diastolicPressure", "Diastolic pressure"],
  ["bloodGlucoseLevel", "Blood glucose"],
  ["bloodOxygenLevel", "Blood oxygen"],
  ["temperature", "Temperature"],
  ["weight", "Weight"],
  ["height", "Height"],
  ["bmi", "BMI"],
  ["respiratoryRate", "Respiratory rate"],
  ["totalCholesterol", "Total cholesterol"],
  ["hdlCholesterol", "HDL cholesterol"],
  ["ldlCholesterol", "LDL cholesterol"],
  ["triglycerides", "Triglycerides"],
  ["waistCircumference", "Waist circumference"],
  ["physicalActivityLevel", "Activity level"],
];
const date = (value) =>
  value
    ? new Date(value).toLocaleString(undefined, {
        dateStyle: "medium",
        timeStyle: "short",
      })
    : "Date unavailable";
const known = (unit) => unit && unit !== "NOT_RECORDED";

export default function HealthDashboard({
  records,
  doctor,
  patientName,
  session,
  refreshSignal,
  loading,
  measurementError,
  onVisits,
  onTimeline,
}) {
  const [visits, setVisits] = useState([]),
    [visitsLoading, setVisitsLoading] = useState(true),
    [visitError, setVisitError] = useState("");
  const [selected, setSelected] = useState("heartRate");
  useEffect(() => {
    const controller = new AbortController();
    setVisitsLoading(true);
    setVisitError("");
    request("/api/appointments/visits", {
      token: session.token,
      signal: controller.signal,
    })
      .then((data) => {
        if (!controller.signal.aborted) setVisits(data);
      })
      .catch((e) => {
        if (e.name !== "AbortError") setVisitError(e.message);
      })
      .finally(() => {
        if (!controller.signal.aborted) setVisitsLoading(false);
      });
    return () => controller.abort();
  }, [session.token, refreshSignal]);
  const available = fields.map(([key, label]) => {
    const rows = records.filter((r) => Number.isFinite(r.values?.[key]));
    return { key, label, latest: rows[0], rows };
  });
  const metric = available.find((m) => m.key === selected);
  const latest = metric.latest;
  const unit = latest?.values?.units?.[selected];
  const points = known(unit)
    ? metric.rows
        .filter((r) => r.values.units?.[selected] === unit && r.timestamp)
        .slice(0, 12)
        .reverse()
    : [];
  const values = points.map((p) => p.values[selected]);
  const min = Math.min(...values),
    max = Math.max(...values);
  const path = points
    .map(
      (p, i) =>
        `${36 + (i * 528) / Math.max(1, points.length - 1)},${150 - ((p.values[selected] - min) / (max - min || 1)) * 100}`,
    )
    .join(" ");
  const upcoming = visits
    .filter(
      (v) =>
        !/cancel|completed/i.test(v.status || "") &&
        new Date(v.dateTime) > new Date(),
    )
    .sort((a, b) => a.dateTime.localeCompare(b.dateTime));
  const approved = visits.filter((v) => v.summaryAvailable);
  return (
    <div className="health-dashboard">
      <section className="dashboard-intro">
        <div>
          <span className="eyebrow">
            {doctor ? "PATIENT OVERVIEW" : "YOUR HEALTH AT A GLANCE"}
          </span>
          <h1>
            {doctor
              ? patientName || "Patient overview"
              : "A clear view of your health."}
          </h1>
          <p>
            Measurements, appointments and your next steps—all in one place.
          </p>
        </div>
        <button className="primary" onClick={onVisits}>
          {doctor ? "View visit briefs" : "View appointments"}
        </button>
      </section>
      <div className="dashboard-stats">
        <article>
          <span>Saved reports</span>
          <strong>{loading ? "…" : records.length}</strong>
          <small>
            {measurementError
              ? "Refresh could not be completed"
              : "Available measurement records"}
          </small>
        </article>
        <article>
          <span>
            {doctor ? "Upcoming clinic visits" : "Upcoming appointments"}
          </span>
          <strong>
            {visitsLoading ? "…" : visitError ? "—" : upcoming.length}
          </strong>
          <small>Scheduled from today onward</small>
        </article>
        <article>
          <span>Approved visit summaries</span>
          <strong>
            {visitsLoading ? "…" : visitError ? "—" : approved.length}
          </strong>
          <small>Released by the assigned doctor</small>
        </article>
      </div>
      {measurementError && (
        <p className="notice error" role="alert">
          Measurements could not be refreshed: {measurementError}
        </p>
      )}
      <section className="dashboard-section">
        <div className="section-head">
          <div>
            <h2>Latest measurements</h2>
            <p className="small muted">
              Each card shows the newest saved value for that measurement.
            </p>
          </div>
          <button className="text-button" onClick={onTimeline}>
            View health timeline →
          </button>
        </div>
        {loading ? (
          <p role="status">Loading measurements…</p>
        ) : (
          <div className="data-cards">
            {available.slice(0, 6).map(({ key, label, latest }) => (
              <button
                key={key}
                className="data-card"
                aria-pressed={selected === key}
                aria-label={`View ${label} history`}
                onClick={() => setSelected(key)}
              >
                <span>{label}</span>
                <strong>
                  {latest?.values[key] ?? "—"}
                  <small>
                    {latest
                      ? known(latest.values.units?.[key])
                        ? latest.values.units[key]
                        : "Unit missing"
                      : "Not recorded"}
                  </small>
                </strong>
                <time>
                  {latest
                    ? date(latest.timestamp)
                    : "Add a measurement to get started"}
                </time>
              </button>
            ))}
          </div>
        )}
      </section>
      <div className="dashboard-columns">
        <section className="dashboard-section">
          <div className="section-head">
            <div>
              <span className="eyebrow">RECORDED HISTORY</span>
              <h2>Measurements over time</h2>
            </div>
            <label className="chart-select">
              Measurement
              <select
                value={selected}
                onChange={(e) => setSelected(e.target.value)}
              >
                {fields.map(([key, label]) => (
                  <option key={key} value={key}>
                    {label}
                  </option>
                ))}
              </select>
            </label>
          </div>
          {points.length >= 2 ? (
            <>
              <svg
                className="measurement-chart"
                viewBox="0 0 600 190"
                role="img"
                aria-label={`${metric.label}: ${points.length} recorded values in ${unit}; oldest to newest. Exact values are in the table below.`}
              >
                <line x1="36" y1="155" x2="564" y2="155" stroke="#dce6eb" />
                <line x1="36" y1="50" x2="564" y2="50" stroke="#edf2f5" />
                <polyline
                  points={path}
                  fill="none"
                  stroke="#207c86"
                  strokeWidth="3"
                  strokeLinejoin="round"
                />
                {path.split(" ").map((pair, i) => (
                  <circle
                    key={i}
                    cx={pair.split(",")[0]}
                    cy={pair.split(",")[1]}
                    r="4"
                    fill="#207c86"
                  />
                ))}
                <text x="4" y="54">
                  {max}
                </text>
                <text x="4" y="154">
                  {min}
                </text>
              </svg>
              <p className="small muted">
                Oldest → newest · {unit}. Points are evenly spaced by record,
                not by elapsed time.
              </p>
            </>
          ) : (
            <div className="chart-empty">
              <span>Not enough comparable readings</span>
              <p>
                {!latest
                  ? "No value has been saved for this measurement yet."
                  : !known(unit)
                    ? "The latest reading needs a recorded unit before it can be charted."
                    : "Save another reading with the same unit to see its history."}
              </p>
            </div>
          )}
          {metric.rows.length > 0 && (
            <details>
              <summary>View exact readings ({metric.rows.length})</summary>
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>Recorded</th>
                      <th>Value</th>
                      <th>Unit</th>
                    </tr>
                  </thead>
                  <tbody>
                    {metric.rows.map((r, i) => (
                      <tr key={r.values.id || i}>
                        <td>{date(r.timestamp)}</td>
                        <td>{r.values[selected]}</td>
                        <td>
                          {known(r.values.units?.[selected])
                            ? r.values.units[selected]
                            : "Not recorded"}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </details>
          )}
        </section>
        <section className="dashboard-section">
          <span className="eyebrow">YOUR SCHEDULE</span>
          <h2>Next appointments</h2>
          {visitsLoading ? (
            <p role="status">Loading appointments…</p>
          ) : visitError ? (
            <p className="notice error" role="alert">
              {visitError}
            </p>
          ) : upcoming.length ? (
            upcoming.slice(0, 3).map((v) => (
              <article className="schedule-row" key={v.id}>
                <span className="schedule-dot" />
                <div>
                  <strong>{doctor ? v.patientName : v.doctorName}</strong>
                  <p>{date(v.dateTime)}</p>
                  <span className="badge">{v.status}</span>
                </div>
              </article>
            ))
          ) : (
            <p className="muted">No upcoming appointments.</p>
          )}
          <button className="secondary" onClick={onVisits}>
            {doctor ? "Open visits & briefs" : "Open visits & summaries"}
          </button>
          <div className="care-note">
            <h3>
              {doctor
                ? "Prepared before. Clear afterward."
                : "Leave each visit with a plan."}
            </h3>
            <p>
              {doctor
                ? "Read the patient’s brief before the appointment. Approve their summary and actions after the consultation."
                : "Your doctor-approved summary includes your next steps and an option to book a follow-up."}
            </p>
          </div>
        </section>
      </div>
      <section className="dashboard-section">
        <h2>All latest recorded values</h2>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Measurement</th>
                <th>Latest value</th>
                <th>Unit</th>
                <th>Recorded</th>
              </tr>
            </thead>
            <tbody>
              {available
                .filter((m) => m.latest)
                .map((m) => (
                  <tr key={m.key}>
                    <td>{m.label}</td>
                    <td>{m.latest.values[m.key]}</td>
                    <td>
                      {known(m.latest.values.units?.[m.key])
                        ? m.latest.values.units[m.key]
                        : "Not recorded"}
                    </td>
                    <td>{date(m.latest.timestamp)}</td>
                  </tr>
                ))}
            </tbody>
          </table>
        </div>
        {!records.length && !loading && (
          <p className="muted">
            No measurements available for this patient yet.
          </p>
        )}
      </section>
    </div>
  );
}
