import React, { useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import Login from "./ReferenceAuth.jsx";
import HealthDashboard from "./HealthDashboard.jsx";
import Appointments from "./Appointments.jsx";
import HealthSummary from "./HealthSummary.jsx";
import { comparisonGroups, shortSummary } from "./ChangeSummary.jsx";

import { SkyArt, ThemeSwitch } from "./BodyWeather.jsx";
import ControlTower from "./ControlTower.jsx";
import { metricUnits } from "./metricUnits.js";
import { parseToken, request } from "./api.js";

const DISCLAIMER =
  "AI-generated decision support only; not a diagnosis. Review by a qualified healthcare professional is required. If you believe you are experiencing an emergency, seek immediate medical assistance; do not wait for this analysis.";
const metricFields = [
  ["heartRate", "Heart rate"],
  ["systolicPressure", "Systolic pressure"],
  ["diastolicPressure", "Diastolic pressure"],
  ["temperature", "Temperature"],
  ["respiratoryRate", "Respiratory rate"],
  ["bloodOxygenLevel", "Blood oxygen"],
  ["bloodGlucoseLevel", "Blood glucose"],
  ["weight", "Weight"],
  ["height", "Height"],
  ["bmi", "BMI"],
  ["totalCholesterol", "Total cholesterol"],
  ["hdlCholesterol", "HDL cholesterol"],
  ["ldlCholesterol", "LDL cholesterol"],
  ["triglycerides", "Triglycerides"],
  ["waistCircumference", "Waist circumference"],
  ["physicalActivityLevel", "Activity level"],
];
const label = (key) => metricFields.find(([k]) => k === key)?.[1] || key;
const dateTime = (value) =>
  value
    ? new Intl.DateTimeFormat(undefined, {
        month: "short",
        day: "numeric",
        year: "numeric",
        hour: "numeric",
        minute: "2-digit",
      }).format(new Date(value))
    : "Not recorded";
const human = (value) => (value || "").replaceAll("_", " ").toLowerCase();
const localDate = () => {
  const d = new Date();
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000)
    .toISOString()
    .slice(0, 16);
};
function Icon({ name = "pulse", size = 20 }) {
  const paths = {
    pulse: <path d="M2 12h5l3-8 4 16 3-8h5" />,
    grid: (
      <>
        <rect x="3" y="3" width="7" height="7" rx="2" />
        <rect x="14" y="3" width="7" height="7" rx="2" />
        <rect x="3" y="14" width="7" height="7" rx="2" />
        <rect x="14" y="14" width="7" height="7" rx="2" />
      </>
    ),
    clock: (
      <>
        <circle cx="12" cy="12" r="9" />
        <path d="M12 7v5l3 2" />
      </>
    ),
    spark: (
      <>
        <path d="m12 3 2.5 6.5L21 12l-6.5 2.5L12 21l-2.5-6.5L3 12l6.5-2.5Z" />
        <path d="M20 2v4m-2-2h4" />
      </>
    ),
    arrow: <path d="M5 12h14m-6-6 6 6-6 6" />,
    plus: <path d="M12 5v14M5 12h14" />,
    shield: (
      <>
        <path d="m12 3 8 3v6c0 5-8 9-8 9s-8-4-8-9V6Z" />
        <path d="m8 12 3 3 5-6" />
      </>
    ),
    exit: (
      <>
        <path d="M10 4H4v16h6m4-8H8m6-4 4 4-4 4" />
      </>
    ),
    check: <path d="m5 12 4 4 10-10" />,
    people: (
      <>
        <circle cx="9" cy="8" r="3" />
        <path d="M3 21v-3a6 6 0 0 1 12 0v3m2-16a3 3 0 0 1 0 6m2 10v-3a6 6 0 0 0-2-4" />
      </>
    ),
    close: <path d="m6 6 12 12M6 18 18 6" />,
  };
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.6"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {paths[name] || paths.pulse}
    </svg>
  );
}
function Brand() {
  return (
    <div className="brand">
      <span className="brand-icon">
        <Icon />
      </span>
      <span>
        Smart Health<span className="brand-dot">.</span>
      </span>
    </div>
  );
}
function Badge({ status }) {
  return status ? (
    <span className={"badge " + status.toLowerCase()}>{human(status)}</span>
  ) : null;
}
function Notice({ children, error = false }) {
  return (
    <div
      className={"notice " + (error ? "error" : "")}
      role={error ? "alert" : "status"}
    >
      <Icon name="shield" size={18} />
      <span>{children}</span>
    </div>
  );
}
function Empty({ title, children }) {
  return (
    <div className="empty">
      <span className="empty-icon">
        <Icon name="clock" size={28} />
      </span>
      <h3>{title}</h3>
      <p>{children}</p>
    </div>
  );
}
function Modal({ title, children, onClose, busy }) {
  const dialog = useRef(null);
  useEffect(() => {
    const previous = document.activeElement,
      oldOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    dialog.current.focus();
    const key = (e) => {
      if (e.key === "Escape" && !busy) onClose();
      if (e.key === "Tab") {
        const nodes = [
            ...dialog.current.querySelectorAll(
              'button:not(:disabled),input,select,textarea,[tabindex="0"]',
            ),
          ],
          first = nodes[0],
          last = nodes.at(-1);
        if (
          e.shiftKey &&
          (document.activeElement === first ||
            document.activeElement === dialog.current)
        ) {
          e.preventDefault();
          last?.focus();
        } else if (!e.shiftKey && document.activeElement === last) {
          e.preventDefault();
          first?.focus();
        }
      }
    };
    document.addEventListener("keydown", key);
    return () => {
      document.body.style.overflow = oldOverflow;
      document.removeEventListener("keydown", key);
      previous?.focus();
    };
  }, [onClose, busy]);
  return createPortal(
    <div
      className="modal-backdrop"
      onClick={(e) => {
        if (e.target === e.currentTarget && !busy) onClose();
      }}
    >
      <section
        className="modal"
        role="dialog"
        aria-modal="true"
        aria-label={title}
        tabIndex={-1}
        ref={dialog}
      >
        <div className="section-head">
          <h2>{title}</h2>
          <button
            className="icon-button"
            aria-label="Close dialog"
            disabled={busy}
            onClick={onClose}
          >
            <Icon name="close" />
          </button>
        </div>
        {children}
      </section>
    </div>,
    document.body,
  );
}
function Trend({ records }) {
  const points = records
    .filter(
      (r) =>
        Number.isFinite(r.values?.heartRate) &&
        r.values?.units?.heartRate ===
          records.find((x) => Number.isFinite(x.values?.heartRate))?.values
            ?.units?.heartRate,
    )
    .slice(0, 12)
    .reverse();
  if (points.length < 2)
    return (
      <Empty title="Your trend starts here">
        Two recorded heart-rate measurements are needed to show a comparison.
      </Empty>
    );
  const numbers = points.map((p) => p.values.heartRate),
    min = Math.min(...numbers) - 5,
    max = Math.max(...numbers) + 5;
  const coords = numbers.map((v, i) => [
    24 + (i / (numbers.length - 1)) * 552,
    135 - ((v - min) / (max - min)) * 110,
  ]);
  const line = coords.map(([x, y]) => x + "," + y).join(" ");
  return (
    <div className="chart">
      <svg
        viewBox="0 0 600 165"
        role="img"
        aria-label={
          "Recorded heart rate: " +
          numbers.join(", ") +
          ". Unit: " +
          (points[0].values.units?.heartRate || "not recorded") +
          "."
        }
      >
        <defs>
          <linearGradient id="chartFill" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#3c9b88" stopOpacity=".22" />
            <stop offset="100%" stopColor="#3c9b88" stopOpacity="0" />
          </linearGradient>
        </defs>
        {[35, 85, 135].map((y) => (
          <line
            key={y}
            x1="24"
            x2="576"
            y1={y}
            y2={y}
            stroke="#e2e9e3"
            strokeDasharray="4 5"
          />
        ))}
        <polygon
          points={"24,155 " + line + " 576,155"}
          fill="url(#chartFill)"
        />
        <polyline
          className="chart-line"
          points={line}
          fill="none"
          stroke="#247f6d"
          strokeWidth="3"
        />
        {coords.map(([x, y], i) => (
          <circle
            key={i}
            cx={x}
            cy={y}
            r="4"
            fill="#fff"
            stroke="#247f6d"
            strokeWidth="2"
          >
            <title>
              {dateTime(points[i].timestamp)}: {numbers[i]}
            </title>
          </circle>
        ))}
      </svg>
      <div className="chart-labels">
        <span>{dateTime(points[0].timestamp)}</span>
        <span>{dateTime(points.at(-1).timestamp)}</span>
      </div>
      <p className="muted small">
        Latest {points.length} available readings. Numerical movement does not
        indicate improvement or deterioration.
      </p>
    </div>
  );
}
function Analysis(props) {
  return (
    <HealthSummary
      {...props}
      Icon={Icon}
      Badge={Badge}
      Notice={Notice}
      label={label}
      human={human}
      disclaimer={DISCLAIMER}
    />
  );
}
function Timeline({ timeline, page, setPage, doctor, onReview }) {
  const [entries, setEntries] = useState(timeline?.entries || []);
  const [chosenEvent, setChosenEvent] = useState(null);
  const lastPage = useRef(page);
  const loadingNext = useRef(false);
  const patient = useRef(timeline?.patientId);
  useEffect(() => {
    if (!timeline || timeline.page !== page) return;
    setEntries((current) =>
      page === 0 ||
      page < lastPage.current ||
      patient.current !== timeline.patientId
        ? timeline.entries
        : [
            ...current,
            ...timeline.entries.filter(
              (e) =>
                !current.some(
                  (old) =>
                    old.eventId === e.eventId && old.eventType === e.eventType,
                ),
            ),
          ],
    );
    lastPage.current = page;
    patient.current = timeline.patientId;
    loadingNext.current = false;
  }, [timeline, page]);
  return (
    <section className="panel">
      <div className="section-head">
        <div>
          <span className="eyebrow">THE FULL PICTURE</span>
          <h2>Health timeline</h2>
          <p className="small muted">
            Checks for new saved records every minute while this view is open.
          </p>
        </div>
        <span className="muted small">
          {timeline?.totalElements || 0} events · Oldest loaded first
        </span>
      </div>
      {!entries.length && (
        <Empty title="A fresh beginning">
          Measurements, saved AI analyses, and doctor reviews will appear here.
        </Empty>
      )}
      <div
        className="ref-river"
        aria-label="Timeline events, oldest loaded event first"
      >
        {[...entries].reverse().map((entry) => (
          <button
            className="ref-stone"
            key={entry.eventType + entry.eventId}
            aria-label={entry.title + " · " + dateTime(entry.eventTime)}
            aria-pressed={
              (chosenEvent || entries[0]?.eventType + entries[0]?.eventId) ===
              entry.eventType + entry.eventId
            }
            style={{
              "--stone-color": entry.eventType.includes("REVIEW")
                ? "#8be0a4"
                : entry.eventType.includes("ANALYSIS")
                  ? "#f2c27a"
                  : "#4fc3c9",
            }}
            onClick={() => setChosenEvent(entry.eventType + entry.eventId)}
          >
            <small>
              {entry.title}
              <br />
              {new Date(entry.eventTime).toLocaleDateString(undefined, {
                month: "short",
                day: "numeric",
              })}
            </small>
            <i />
          </button>
        ))}
      </div>
      <div
        className="timeline"
        tabIndex={0}
        aria-label="Scroll through your health river"
        onScroll={(event) => {
          const target = event.currentTarget;
          if (
            !loadingNext.current &&
            timeline?.page === page &&
            page + 1 < timeline.totalPages &&
            target.scrollLeft > 0 &&
            target.scrollLeft + target.clientWidth >= target.scrollWidth - 35
          ) {
            loadingNext.current = true;
            setPage(page + 1);
          }
        }}
      >
        {entries
          .filter(
            (entry) =>
              entry.eventType + entry.eventId ===
              (entries.some((e) => e.eventType + e.eventId === chosenEvent)
                ? chosenEvent
                : entries[0]?.eventType + entries[0]?.eventId),
          )
          .map((entry, i) => (
            <article
              className="timeline-event"
              key={entry.eventType + entry.eventId}
              style={{ "--delay": Math.min(i, 8) * 40 + "ms" }}
            >
              <span className={"event-icon " + entry.eventType.toLowerCase()}>
                <Icon
                  name={
                    entry.eventType.includes("ANALYSIS")
                      ? "spark"
                      : entry.eventType === "HEALTH_METRICS_RECORDED"
                        ? "pulse"
                        : "shield"
                  }
                />
              </span>
              <div className="event-body">
                <div className="section-head">
                  <h3>{entry.title}</h3>
                  <Badge status={entry.reviewStatus} />
                </div>
                <time className="small muted">{dateTime(entry.eventTime)}</time>
                {entry.eventType === "HEALTH_METRICS_RECORDED" && (
                  <>
                    <div className="readings">
                      {Object.entries(entry.details.values)
                        .slice(0, 4)
                        .map(([k, v]) => (
                          <span key={k}>
                            {label(k)}{" "}
                            <strong>
                              {v} {entry.details.units?.[k] || ""}
                            </strong>
                          </span>
                        ))}
                    </div>
                    {Object.keys(entry.details.values).length > 4 && (
                      <details>
                        <summary>
                          All {Object.keys(entry.details.values).length}{" "}
                          measurements
                        </summary>
                        <div className="readings">
                          {Object.entries(entry.details.values)
                            .slice(4)
                            .map(([k, v]) => (
                              <span key={k}>
                                {label(k)}{" "}
                                <strong>
                                  {v}{" "}
                                  {entry.details.units?.[k] || "(unit unknown)"}
                                </strong>
                              </span>
                            ))}
                        </div>
                      </details>
                    )}
                    {entry.details.unitNotice && (
                      <p className="small muted">{entry.details.unitNotice}</p>
                    )}
                  </>
                )}
                {entry.eventType === "AI_CHANGE_ANALYSIS" && (
                  <>
                    <p className="timeline-change-preview">
                      {entry.details.narrativeStatus === "AVAILABLE"
                        ? shortSummary(entry.details.narrative?.summary) ||
                          "Recorded health comparison"
                        : "Recorded changes available; AI summary unavailable."}
                    </p>
                    <span className="small muted">
                      {comparisonGroups(entry.details).changed.length}{" "}
                      measurements changed · Saved comparison
                    </span>
                    <details>
                      <summary>View AI summary and verified changes</summary>
                      <Analysis data={entry.details} doctor={false} />
                    </details>
                  </>
                )}
                {entry.eventType === "SYMPTOM_ANALYSIS" && (
                  <details>
                    <summary>View symptom decision support</summary>
                    <Analysis data={entry.details.analysis} />
                  </details>
                )}
                {entry.eventType === "DOCTOR_REVIEW" && (
                  <>
                    <p>
                      {entry.details.followUpRecommendation ||
                        "Doctor review recorded."}
                    </p>
                    {doctor && entry.details.internalNotes && (
                      <div className="private-note">
                        <span className="eyebrow">PRIVATE DOCTOR NOTE</span>
                        <p>{entry.details.internalNotes}</p>
                      </div>
                    )}
                    {entry.details.recommendedFollowUpDate && (
                      <p className="small">
                        Recommended follow-up:{" "}
                        {entry.details.recommendedFollowUpDate}
                      </p>
                    )}
                  </>
                )}
                {entry.eventType === "FOLLOW_UP_RECOMMENDED" && (
                  <>
                    <p>
                      {entry.details.recommendation || "Follow-up recommended."}
                    </p>
                    {entry.details.recommendedDate && (
                      <span className="chip">
                        Recommended date · {entry.details.recommendedDate}
                      </span>
                    )}
                  </>
                )}
              </div>
            </article>
          ))}
      </div>
      <div className="pagination">
        <button
          className="secondary"
          disabled={page === 0}
          onClick={() => setPage(page - 1)}
        >
          Previous
        </button>
        <span className="small muted">
          Page {page + 1} of {Math.max(1, timeline?.totalPages || 0)}
        </span>
        <button
          className="secondary"
          disabled={page + 1 >= (timeline?.totalPages || 0)}
          onClick={() => setPage(page + 1)}
        >
          Next
        </button>
      </div>
    </section>
  );
}
function MeasurementForm({ session, patientId, onClose, onSaved }) {
  const [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  async function submit(e) {
    e.preventDefault();
    setBusy(true);
    setError("");
    const fields = new FormData(e.currentTarget);
    try {
      const stamp = fields.get("timestamp"),
        body = {
          patientId: Number(patientId),
          date: stamp.slice(0, 10),
          timestamp: stamp.length === 16 ? stamp + ":00" : stamp,
        };
      metricFields.forEach(([key]) => {
        const value = fields.get(key);
        if (value !== "") {
          body[key] = Number(value);
          const unit = fields.get(key + "Unit");
          if (!unit)
            throw new Error("Choose the recorded unit for " + label(key) + ".");
          body.units ||= {};
          body.units[key] = unit;
        }
      });
      if (!metricFields.some(([key]) => body[key] !== undefined))
        throw new Error("Enter at least one measurement.");
      await request("/api/healthmetrics", {
        token: session.token,
        method: "POST",
        body,
      });
      onSaved();
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }
  const decimals = [
    "temperature",
    "weight",
    "height",
    "bmi",
    "waistCircumference",
    "bloodGlucoseLevel",
    "totalCholesterol",
    "hdlCholesterol",
    "ldlCholesterol",
    "triglycerides",
  ];
  return (
    <Modal title="Record measurements" busy={busy} onClose={onClose}>
      <p className="muted">
        Patient #{patientId}. Only enter readings you have recorded.
      </p>
      <Notice>
        Every entered reading needs its recorded unit. Check the source before
        choosing; leave the reading empty if its unit is unknown.
      </Notice>
      <form onSubmit={submit}>
        <label>
          Recorded at
          <input
            name="timestamp"
            type="datetime-local"
            required
            defaultValue={localDate()}
          />
        </label>
        <div className="measurement-grid">
          {metricFields.map(([key, title]) => (
            <div key={key}>
              <label>
                {title}
                <input
                  type="number"
                  name={key}
                  step={decimals.includes(key) ? "any" : "1"}
                  placeholder="Not recorded"
                />
              </label>
              <label className="measurement-unit">
                {title} unit
                <select
                  aria-label={title + " unit"}
                  name={key + "Unit"}
                  defaultValue=""
                >
                  <option value="">Not recorded</option>
                  {metricUnits[key].map((unit) => (
                    <option key={unit}>{unit}</option>
                  ))}
                </select>
              </label>
            </div>
          ))}
        </div>
        {error && <Notice error>{error}</Notice>}
        <button className="primary wide" disabled={busy}>
          {busy ? "Saving measurements…" : "Save measurements"}
          <Icon name="check" />
        </button>
      </form>
    </Modal>
  );
}
function MissingUnitsForm({ session, measurements, onClose, onSaved }) {
  const records = measurements.filter(({ values }) =>
    metricFields.some(([key]) => values[key] != null && !values.units?.[key]),
  );
  const [recordId, setRecordId] = useState(String(records[0]?.values.id || ""));
  const [units, setUnits] = useState({});
  const [verified, setVerified] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const record = records.find(
    ({ values }) => String(values.id) === recordId,
  )?.values;
  async function submit(event) {
    event.preventDefault();
    if (!verified || !Object.keys(units).length) return;
    setBusy(true);
    setError("");
    try {
      await request(`/api/healthmetrics/${recordId}/units`, {
        token: session.token,
        method: "PATCH",
        body: { units, verifiedAgainstSource: true },
      });
      onSaved();
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }
  return (
    <Modal title="Add missing measurement units" busy={busy} onClose={onClose}>
      <Notice>
        Check the original report or device record before choosing units.
        Values, dates, and existing units stay unchanged. Leave unknown units
        unselected. Appointment briefs use the updated units; historical
        analyses keep their original comparisons.
      </Notice>
      <form onSubmit={submit}>
        <label>
          Measurement record
          <select
            value={recordId}
            disabled={busy}
            onChange={(event) => {
              setRecordId(event.target.value);
              setUnits({});
              setVerified(false);
              setError("");
            }}
          >
            {records.map(({ values }) => (
              <option key={values.id} value={values.id}>
                {dateTime(values.timestamp)} · Record #{values.id}
              </option>
            ))}
          </select>
        </label>
        <div className="measurement-grid">
          {metricFields
            .filter(([key]) => record?.[key] != null && !record.units?.[key])
            .map(([key, title]) => (
              <label key={key}>
                {title}: {record[key]}
                <select
                  aria-label={title + " missing unit"}
                  disabled={busy}
                  value={units[key] || ""}
                  onChange={(event) => {
                    const next = { ...units };
                    if (event.target.value) next[key] = event.target.value;
                    else delete next[key];
                    setUnits(next);
                    setVerified(false);
                  }}
                >
                  <option value="">Unknown — leave unchanged</option>
                  {metricUnits[key].map((unit) => (
                    <option key={unit}>{unit}</option>
                  ))}
                </select>
              </label>
            ))}
        </div>
        <label className="report-verification">
          <input
            type="checkbox"
            checked={verified}
            disabled={busy}
            onChange={(event) => setVerified(event.target.checked)}
          />
          I verified these units against the original report or device record.
        </label>
        {error && <Notice error>{error}</Notice>}
        <button
          className="primary wide"
          disabled={busy || !verified || !Object.keys(units).length}
        >
          {busy ? "Saving units…" : "Save verified units"}
        </button>
      </form>
    </Modal>
  );
}
function Workspace({ session, onLogout }) {
  const doctor = session.role === "DOCTOR";
  const [metricsLoading, setMetricsLoading] = useState(true);
  const [measurementError, setMeasurementError] = useState("");
  const [visitReason, setVisitReason] = useState("");
  const [startBooking, setStartBooking] = useState(false);
  const [view, setView] = useState("overview"),
    [patients, setPatients] = useState([]),
    [selected, setSelected] = useState(""),
    [search, setSearch] = useState("");
  const [timeline, setTimeline] = useState(null),
    [measurements, setMeasurements] = useState([]),
    [page, setPage] = useState(0),
    [refresh, setRefresh] = useState(0);
  const [loading, setLoading] = useState(true),
    [error, setError] = useState(""),
    [recording, setRecording] = useState(false),
    [editingUnits, setEditingUnits] = useState(false),
    [message, setMessage] = useState("");
  useEffect(() => {
    if (!["timeline", "overview"].includes(view)) return;
    const refreshVisibleTimeline = () => {
      if (document.visibilityState === "visible") setRefresh((n) => n + 1);
    };
    const interval = setInterval(refreshVisibleTimeline, 60000);
    document.addEventListener("visibilitychange", refreshVisibleTimeline);
    return () => {
      clearInterval(interval);
      document.removeEventListener("visibilitychange", refreshVisibleTimeline);
    };
  }, [view]);
  useEffect(() => {
    if (!doctor) return;
    const controller = new AbortController();
    request("/api/patients", {
      token: session.token,
      signal: controller.signal,
    })
      .then((data) => {
        setPatients(data);
        setSelected((current) =>
          data.some((p) => String(p.id) === current)
            ? current
            : String(data[0]?.id || ""),
        );
      })
      .catch((e) => {
        if (e.name !== "AbortError") {
          setError(e.message);
          setLoading(false);
        }
      });
    return () => controller.abort();
  }, [doctor, session.token, refresh]);
  useEffect(() => {
    if (doctor && !selected) {
      setLoading(false);
      return;
    }
    const controller = new AbortController();
    setLoading(true);
    setError("");
    const base = doctor
      ? "/api/health-timeline/patients/" + selected
      : "/api/health-timeline/me";
    const metricsPath = doctor
      ? "/api/healthmetrics/patient/" + selected
      : "/api/healthmetrics/myhealthreports";
    request(base + "?page=" + page + "&size=20", {
      token: session.token,
      signal: controller.signal,
    })
      .then((data) => {
        if (!controller.signal.aborted) setTimeline(data);
      })
      .catch((e) => {
        if (e.name !== "AbortError") setError(e.message);
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    setMetricsLoading(true);
    setMeasurementError("");
    request(metricsPath, { token: session.token, signal: controller.signal })
      .then((data) => {
        if (!controller.signal.aborted)
          setMeasurements(
            data
              .map((m) => ({ timestamp: m.timestamp, values: m }))
              .sort(
                (a, b) =>
                  (b.timestamp || "").localeCompare(a.timestamp || "") ||
                  b.values.id - a.values.id,
              ),
          );
      })
      .catch((e) => {
        if (e.name !== "AbortError") {
          if (e.status === 404) setMeasurements([]);
          else setMeasurementError(e.message);
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) setMetricsLoading(false);
      });
    return () => controller.abort();
  }, [doctor, selected, page, refresh, session.token]);
  useEffect(() => {
    if (!message) return;
    const timeout = setTimeout(() => setMessage(""), 6000);
    return () => clearTimeout(timeout);
  }, [message]);
  const patientId = doctor ? Number(selected) : timeline?.patientId,
    patient = patients.find((p) => String(p.id) === selected);
  const latest = measurements[0]?.values || {},
    previous =
      measurements.find((m) => m.timestamp < measurements[0]?.timestamp)
        ?.values || {};
  function saved(kind) {
    setRecording(false);
    setEditingUnits(false);
    setPage(0);
    setRefresh((n) => n + 1);
    setMessage(kind + " saved successfully.");
  }
  return (
    <div className="workspace clean-workspace">
      <aside className="sidebar">
        <Brand />
        <span className="sidebar-label">
          {doctor ? "CLINICIAN WORKSPACE" : "MY HEALTH WORKSPACE"}
        </span>
        <nav aria-label="Main navigation">
          {[
            ["overview", "grid", "Overview"],
            ["timeline", "clock", "Health timeline"],
            ["prepare", "spark", "Visit preparation"],
            ["appointments", "clock", "Appointments"],
          ].map(([id, icon, title]) => (
            <button
              key={id}
              className={view === id ? "active" : ""}
              aria-label={title}
              aria-current={view === id ? "page" : undefined}
              onClick={() => {
                if (id === "overview") setPage(0);
                setView(id);
              }}
            >
              <Icon name={icon} />
              <span>{title}</span>
            </button>
          ))}
        </nav>
        <div className="sidebar-note">
          <Icon name="shield" size={25} />
          <h4>Care with a human touch.</h4>
          <p>Your questions. Your doctor’s plan. One connected visit.</p>
        </div>
        <div className="profile">
          <span className="avatar">
            {session.username.slice(0, 2).toUpperCase()}
          </span>
          <div>
            <strong>{session.username}</strong>
            <span>{doctor ? "Doctor" : "Patient"} account</span>
          </div>
          <button
            className="icon-button"
            aria-label="Sign out"
            onClick={onLogout}
          >
            <Icon name="exit" />
          </button>
        </div>
      </aside>
      <div className="main-shell">
        <header className="topbar">
          <span>
            Health workspace{" "}
            <span className="crumb">
              /{" "}
              {view === "overview"
                ? "Overview"
                : view === "timeline"
                  ? "Timeline"
                  : view === "appointments"
                    ? "Appointments"
                    : "Visit care"}
            </span>
          </span>
          <div className="topbar-right">
            <span className="connection">
              <i /> Connected care
            </span>
            <button
              className="secondary"
              onClick={() => setRefresh((n) => n + 1)}
              disabled={loading}
            >
              Refresh
            </button>
            <button
              className="icon-button mobile-signout"
              aria-label="Sign out"
              onClick={onLogout}
            >
              <Icon name="exit" />
            </button>
          </div>
        </header>
        <main data-view={view}>
          <div className="page-heading enter">
            <div>
              <span className="eyebrow">
                {doctor
                  ? "VISIT CARE · CLINICIAN WORKSPACE"
                  : "VISIT CARE · YOUR HEALTH JOURNEY"}
              </span>
              <h1>
                {view === "appointments"
                  ? doctor
                    ? "Your clinic schedule."
                    : "Your appointments."
                  : doctor
                    ? "Every visit, better prepared."
                    : "Your care, before and after."}
              </h1>
              <p>
                {doctor
                  ? "Review changes. Add context. Guide the next step."
                  : "Small observations today. A clearer picture over time."}
              </p>
            </div>
            <span className="today">
              <Icon name="clock" size={17} />
              {new Intl.DateTimeFormat(undefined, {
                month: "short",
                day: "numeric",
                year: "numeric",
              }).format(new Date())}
            </span>
          </div>
          {doctor && view !== "appointments" && view !== "prepare" && (
            <section className="patient-picker">
              <div>
                <Icon name="people" />
                <strong>Patient workspace</strong>
              </div>
              <label className="sr-only" htmlFor="patient-search">
                Search patients
              </label>
              <input
                id="patient-search"
                type="search"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Search patients by name…"
              />
              <label className="sr-only" htmlFor="patient-select">
                Select patient
              </label>
              <select
                id="patient-select"
                value={selected}
                onChange={(e) => {
                  setSelected(e.target.value);
                  setTimeline(null);
                  setMeasurements([]);
                  setPage(0);
                  setMessage("");
                }}
              >
                <option value="" disabled>
                  Select a patient
                </option>
                {patients
                  .filter(
                    (p) =>
                      String(p.id) === selected ||
                      (p.name || "Unnamed patient")
                        .toLowerCase()
                        .includes(search.toLowerCase()),
                  )
                  .map((p) => (
                    <option value={p.id} key={p.id}>
                      {p.name} · #{p.id}
                    </option>
                  ))}
              </select>
              {patient && (
                <span className="small muted">Age {patient.age}</span>
              )}
            </section>
          )}
          {message && <Notice>{message}</Notice>}
          {error && (
            <Notice error>
              {error}{" "}
              <button
                className="text-button"
                onClick={() => setRefresh((n) => n + 1)}
              >
                Try again
              </button>
            </Notice>
          )}
          {doctor &&
            view !== "appointments" &&
            !selected &&
            !loading &&
            !error && (
              <section className="panel">
                <Empty title="No patients yet">
                  Register a patient through your existing backend to start a
                  care timeline.
                </Empty>
              </section>
            )}
          {view === "prepare" && (
            <section className="ref-sky-content">
              <h1>
                {doctor
                  ? "Prepare for your next visit."
                  : "What’s on your mind?"}
              </h1>
              <p className="ref-sub">
                {doctor
                  ? "Open the assigned appointment to review the patient’s questions and recorded measurements."
                  : "Describe how you feel in your own words. Add this to your appointment for your doctor to read."}
              </p>
              {!doctor && (
                <label className="ref-prep-label">
                  Reason for your visit
                  <textarea
                    maxLength={500}
                    rows={5}
                    value={visitReason}
                    onChange={(e) => setVisitReason(e.target.value)}
                    placeholder="What would you like to discuss with your doctor?"
                  />
                </label>
              )}
              <button
                className="primary ref-prep-button"
                onClick={() => {
                  setStartBooking(!doctor);
                  setView("appointments");
                }}
              >
                {doctor ? "Open appointment briefs" : "Book a doctor"}
              </button>
              <div className="ref-sheet">
                <h3>Care with context</h3>
                <p>
                  {doctor
                    ? "Only your assigned appointments have briefs you can access. Approve the patient summary after the consultation."
                    : "Your words are saved only when you confirm a booking. After the visit, your doctor’s approved summary appears with your appointment."}
                </p>
              </div>
            </section>
          )}
          {view === "appointments" && (
            <Appointments
              session={session}
              patients={patients}
              refreshSignal={refresh}
              startBooking={startBooking}
              initialReason={visitReason}
              onBooked={() => {
                setVisitReason("");
                setStartBooking(false);
              }}
            />
          )}
          {loading && !timeline && view === "timeline" ? (
            <div className="loading" role="status">
              <span className="spinner" /> Loading your health workspace…
            </div>
          ) : (
            (!doctor || patientId) &&
            view !== "appointments" && (
              <>
                {view === "overview" && (
                  <>
                    <HealthDashboard
                      records={measurements}
                      doctor={doctor}
                      patientName={patient?.name}
                      onVisits={() => setView("appointments")}
                      onTimeline={() => {
                        setPage(0);
                        setView("timeline");
                      }}
                      session={session}
                      refreshSignal={refresh}
                      loading={metricsLoading}
                      measurementError={measurementError}
                    />
                    <div className="section-head overview-heading">
                      <h2>
                        {doctor
                          ? "Patient observations"
                          : "Recorded observations"}
                      </h2>
                      {doctor && (
                        <div className="measurement-actions">
                          {measurements.some(({ values }) =>
                            metricFields.some(
                              ([key]) =>
                                values[key] != null && !values.units?.[key],
                            ),
                          ) && (
                            <button
                              className="secondary"
                              onClick={() => setEditingUnits(true)}
                            >
                              Add missing units
                            </button>
                          )}
                          <button
                            className="secondary"
                            onClick={() => setRecording(true)}
                          >
                            <Icon name="plus" size={16} /> Record measurements
                          </button>
                        </div>
                      )}
                    </div>
                  </>
                )}
                {view === "timeline" && (
                  <Timeline
                    timeline={timeline}
                    page={page}
                    setPage={setPage}
                    doctor={false}
                  />
                )}
              </>
            )
          )}
          <footer>
            <span>
              <Icon name="shield" size={16} /> Clinical decision support ·
              Doctor oversight required
            </span>
            <span>Smart Health Management</span>
          </footer>
        </main>
      </div>
      {recording && (
        <MeasurementForm
          session={session}
          patientId={patientId}
          onClose={() => setRecording(false)}
          onSaved={() => saved("Measurements")}
        />
      )}
      {editingUnits && doctor && (
        <MissingUnitsForm
          key={patientId}
          session={session}
          measurements={measurements}
          onClose={() => setEditingUnits(false)}
          onSaved={() => saved("Measurement units")}
        />
      )}
    </div>
  );
}
export default function App() {
  // JWT stays in memory: signing out or reloading removes the session.
  const [session, setSession] = useState(null),
    [expired, setExpired] = useState(false);
  useEffect(() => {
    const listener = () => {
      setSession(null);
      setExpired(true);
    };
    window.addEventListener("session-expired", listener);
    return () => window.removeEventListener("session-expired", listener);
  }, []);
  return session?.role === "ADMIN" ? (
    <ControlTower
      session={session}
      onLogout={() => {
        setSession(null);
        setExpired(false);
      }}
      Brand={Brand}
      Modal={Modal}
      Notice={Notice}
    />
  ) : session ? (
    <Workspace
      session={session}
      onLogout={() => {
        setSession(null);
        setExpired(false);
      }}
    />
  ) : (
    <Login
      expired={expired}
      onLogin={(value) => {
        setSession(value);
        setExpired(false);
      }}
    />
  );
}
