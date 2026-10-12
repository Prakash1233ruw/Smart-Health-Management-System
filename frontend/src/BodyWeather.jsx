import React, { useEffect, useState } from "react";
import { request } from "./api.js";
import { comparisonGroups, shortSummary } from "./ChangeSummary.jsx";

const date = (value) =>
  value
    ? new Date(value).toLocaleDateString(undefined, {
        month: "short",
        day: "numeric",
      })
    : "Date unknown";
export function ThemeSwitch() {
  const [mode, setMode] = useState(
    localStorage.getItem("body-weather-theme") || "auto",
  );
  useEffect(() => {
    const update = () => {
      const hour = new Date().getHours();
      document.documentElement.dataset.weatherTheme =
        mode === "auto" ? (hour >= 18 || hour < 6 ? "night" : "day") : mode;
    };
    update();
    localStorage.setItem("body-weather-theme", mode);
    const timer = setInterval(update, 60000);
    return () => clearInterval(timer);
  }, [mode]);
  return (
    <label className="weather-theme">
      <span className="sr-only">Sky theme</span>
      <select
        aria-label="Sky theme"
        value={mode}
        onChange={(e) => setMode(e.target.value)}
      >
        <option value="auto">◐ Auto sky</option>
        <option value="day">☀ Day sky</option>
        <option value="night">☾ Night sky</option>
      </select>
    </label>
  );
}
export function SkyArt({ mood = "unknown" }) {
  return (
    <div className={`sky-art sky-${mood}`} aria-hidden="true">
      <div className="sky-stars" />
      <div className="sky-sun" />
      <div className="sky-cloud cloud-one" />
      <div className="sky-cloud cloud-two" />
      <div className="sky-horizon" />
      <svg
        className="sky-river"
        viewBox="0 0 1000 300"
        preserveAspectRatio="none"
      >
        <path d="M0 225 Q250 100 450 220 T1000 180 L1000 300 H0Z" />
        <path d="M0 260 Q300 165 550 240 T1000 230" />
      </svg>
    </div>
  );
}
export function BodySky({
  records,
  timeline,
  onRiver,
  onAnalyze,
  doctor = false,
  patientName,
}) {
  const [reading, setReading] = useState(1);
  const latest = records[0]?.values;
  const previous = records.find(
    (r) => r.timestamp < records[0]?.timestamp,
  )?.values;
  const analysis = timeline?.entries?.find(
    (e) => e.eventType === "AI_CHANGE_ANALYSIS",
  )?.details;
  const groups = comparisonGroups(analysis);
  const pending = ["PENDING_REVIEW", "FOLLOW_UP_REQUIRED"].includes(
    analysis?.reviewStatus,
  );
  const reviewed = analysis?.reviewStatus === "REVIEWED";
  const mood = pending
    ? "review"
    : reviewed
      ? "dawn"
      : groups.changed.length
        ? "changing"
        : groups.stable.length && !groups.incomplete.length
          ? "steady"
          : "unknown";
  const headlines = {
    review: "A little context. A human review.",
    dawn: "Your doctor has joined the forecast.",
    changing: "A shift in your recorded readings.",
    steady: "A steady chapter in your records.",
    unknown: "Every observation begins a story.",
  };
  const summary =
    analysis?.narrativeStatus === "AVAILABLE"
      ? shortSummary(analysis.narrative?.summary)
      : "Your measurements, observations, and care — connected over time.";
  const values = reading === 0 && previous ? previous : latest;
  return (
    <section className={`body-sky mood-${mood}`} aria-label="Body Weather home">
      <SkyArt mood={mood} />
      <div className="sky-content">
        <span className="eyebrow">
          {doctor
            ? `PATIENT SKY · ${patientName || "SELECTED PATIENT"}`
            : "YOUR BODY WEATHER"}
        </span>
        <div className="sky-state">
          <i />
          {pending
            ? "Doctor review pending"
            : reviewed
              ? "Doctor review recorded"
              : latest
                ? "Saved measurements"
                : "Waiting for your first record"}
        </div>
        <h2>{headlines[mood]}</h2>
        <p>{summary}</p>
        <div className="sky-actions">
          <button className="sky-button" onClick={onRiver}>
            Explore your river <span>↗</span>
          </button>
          <button className="sky-link" onClick={onAnalyze}>
            Tell the sky →
          </button>
        </div>
        <div className="weather-orbs">
          {[
            ["heartRate", "Heart rate"],
            ["bloodGlucoseLevel", "Blood glucose"],
            ["systolicPressure", "Systolic pressure"],
          ].map(([key, title]) => (
            <button className="weather-orb" onClick={onRiver} key={key}>
              <span>{title}</span>
              <strong>{values?.[key] ?? "—"}</strong>
              <small>
                {values?.[key] != null
                  ? values.units?.[key] || "Unit not recorded"
                  : "Not recorded"}
              </small>
            </button>
          ))}
        </div>
        {previous && (
          <label className="sky-before-after">
            <span>Previous · {date(previous.timestamp)}</span>
            <input
              type="range"
              min="0"
              max="1"
              step="1"
              value={reading}
              aria-label="Choose previous or latest recorded measurements"
              onChange={(e) => setReading(Number(e.target.value))}
            />
            <span>Latest · {date(latest.timestamp)}</span>
          </label>
        )}
        <span className="sky-footnote">
          Visual atmosphere, not a medical forecast. Based on saved records
          {latest ? ` · ${date(latest.timestamp)}` : ""}.
        </span>
      </div>
    </section>
  );
}

export function Radar({
  patients,
  session,
  selected,
  onSelect,
  refreshSignal,
}) {
  const [statuses, setStatuses] = useState({});
  const [schedule, setSchedule] = useState([]);
  const [scheduleError, setScheduleError] = useState("");
  useEffect(() => {
    const controller = new AbortController();
    setStatuses({});
    const subset = patients.slice(0, 20);
    let cursor = 0;
    async function worker() {
      while (cursor < subset.length && !controller.signal.aborted) {
        const patient = subset[cursor++];
        let info;
        try {
          const data = await request(
            `/api/health-timeline/patients/${patient.id}?page=0&size=20`,
            { token: session.token, signal: controller.signal },
          );
          const pending = data.entries.filter(
            (e) =>
              e.eventType === "AI_CHANGE_ANALYSIS" &&
              ["PENDING_REVIEW", "FOLLOW_UP_REQUIRED"].includes(e.reviewStatus),
          ).length;
          info = { pending, checked: true };
        } catch (e) {
          if (e.name === "AbortError") return;
          info = { checked: false };
        }
        if (!controller.signal.aborted)
          setStatuses((s) => ({ ...s, [patient.id]: info }));
      }
    }
    Promise.all(Array.from({ length: Math.min(4, subset.length) }, worker));
    setScheduleError("");
    request("/api/doctors/myupcoming-appointments", {
      token: session.token,
      signal: controller.signal,
    })
      .then((items) => {
        if (!controller.signal.aborted)
          setSchedule(
            items.filter(
              (a) =>
                !["canceled", "cancelled"].includes(a.status?.toLowerCase()),
            ),
          );
      })
      .catch((e) => {
        if (e.name !== "AbortError") setScheduleError(e.message);
      });
    return () => controller.abort();
  }, [patients, session.token, refreshSignal]);
  return (
    <section className="radar-panel panel">
      <div className="section-head">
        <div>
          <span className="eyebrow">CLINICIAN RADAR</span>
          <h2>Human attention at the center.</h2>
        </div>
        <span className="chip">Review queue</span>
      </div>
      <p className="small muted">
        Position reflects pending reviews in the latest 20 events, not clinical
        urgency. Showing up to 20 patients; use search for the full directory.
      </p>
      <div className="radar-layout">
        <div className="radar-scope" aria-label="Patient review radar">
          <div className="radar-sweep" />
          <span className="radar-center">✦</span>
          {patients.slice(0, 20).map((p, i) => {
            const info = statuses[p.id];
            const angle = i * 2.39996;
            const radius = info?.pending ? 15 + (i % 3) * 6 : 30 + (i % 3) * 5;
            return (
              <button
                key={p.id}
                className={`radar-dot ${info?.pending ? "needs-review" : ""} ${String(p.id) === String(selected) ? "selected" : ""}`}
                style={{
                  left: `${50 + Math.cos(angle) * radius}%`,
                  top: `${50 + Math.sin(angle) * radius}%`,
                }}
                onClick={() => onSelect(p.id)}
                aria-label={`Select ${p.name || "patient"}: ${info?.checked ? `${info.pending} pending reviews in loaded events` : "Review status not available"}`}
                title={p.name}
              >
                {(p.name || "?").slice(0, 1)}
              </button>
            );
          })}
        </div>
        <div className="radar-directory">
          {patients.slice(0, 20).map((p) => (
            <button
              key={p.id}
              onClick={() => onSelect(p.id)}
              className={String(p.id) === String(selected) ? "active" : ""}
            >
              <span>{p.name || "Unnamed patient"}</span>
              <small>
                {statuses[p.id]?.checked
                  ? statuses[p.id].pending
                    ? `${statuses[p.id].pending} pending review`
                    : "No pending review in loaded events"
                  : statuses[p.id]
                    ? "Could not check"
                    : "Checking records…"}
              </small>
            </button>
          ))}
        </div>
      </div>
      <div className="schedule-strip">
        <span className="eyebrow">NEXT ON YOUR SCHEDULE</span>
        {scheduleError ? (
          <p className="small">{scheduleError}</p>
        ) : schedule.length ? (
          schedule.slice(0, 3).map((a) => (
            <div key={a.id}>
              <strong>
                {patients.find((p) => String(p.id) === String(a.patientId))
                  ?.name || "Patient name unavailable"}
              </strong>
              <small>
                {new Date(a.dateTime).toLocaleString(undefined, {
                  month: "short",
                  day: "numeric",
                  hour: "numeric",
                  minute: "2-digit",
                })}
              </small>
            </div>
          ))
        ) : (
          <span className="small muted">No upcoming appointments loaded.</span>
        )}
      </div>
    </section>
  );
}
