import React, { useEffect, useState } from "react";
import { request } from "./api.js";
import "./visits.css";

const name = (key) =>
  key.replace(/([A-Z])/g, " $1").replace(/^./, (c) => c.toUpperCase());
const date = (stamp) =>
  stamp
    ? new Date(stamp).toLocaleString(undefined, {
        dateStyle: "medium",
        timeStyle: "short",
      })
    : "Date not recorded";
const unit = (value) =>
  !value || value === "NOT_RECORDED" ? "unit not recorded" : value;

function Brief({ data }) {
  return (
    <div className="visit-brief">
      <div className="visit-banner">
        <span className="eyebrow">PRE-VISIT BRIEF · ASSIGNED DOCTOR ONLY</span>
        <h2>{data.patientName}</h2>
        <p>
          {date(data.appointmentTime)} · {data.doctorName}
        </p>
        <small>
          Refreshed {date(data.preparedAt)} from saved records. No new AI risk
          assessment.
        </small>
      </div>
      <div className="visit-columns">
        <section>
          <span className="visit-step">01</span>
          <h3>Why they’re coming</h3>
          <p className="visit-verbatim">
            {data.reasonForVisit || "Reason not provided."}
          </p>
          <small>Patient’s own words at booking</small>
        </section>
        <section>
          <span className="visit-step">02</span>
          <h3>Questions for you</h3>
          <p className="visit-verbatim">
            {data.questions || "No questions added."}
          </p>
        </section>
      </div>
      <section>
        <span className="visit-step">03</span>
        <h3>What changed</h3>
        <p className="small muted">
          Largest numerical movements, not a clinical priority ranking.
        </p>
        {data.trends.length ? (
          <div className="visit-trends">
            {data.trends.map((t) => (
              <article key={t.metric}>
                <h4>{name(t.metric)}</h4>
                <strong>
                  {t.previousValue} <span aria-label="to">→</span>{" "}
                  {t.latestValue} <small>{t.unit}</small>
                </strong>
                <p>
                  {t.absoluteChange > 0 ? "+" : ""}
                  {t.absoluteChange} {t.unit}
                  {t.percentageChange != null
                    ? ` (${t.percentageChange > 0 ? "+" : ""}${t.percentageChange}%)`
                    : ""}
                </p>
                <small>
                  {date(t.previousRecordedAt)} → {date(t.latestRecordedAt)}
                </small>
              </article>
            ))}
          </div>
        ) : (
          <p>No comparable changes available. Saved values are shown below even when a comparison cannot be calculated. Trends require two dated values for the same measurement with matching recorded units.</p>
        )}
      </section>
      <div className="visit-columns">
        <section>
          <span className="visit-step">04</span>
          <h3>Latest saved measurements</h3>
          {data.latestReports.length ? (
            data.latestReports.map((r, index) => (
              <details key={r.recordId} open={index === 0}>
                <summary>
                  {date(r.recordedAt)} · {Object.keys(r.values).length} values
                </summary>
                <dl className="visit-values">
                  {Object.entries(r.values).map(([key, value]) => (
                    <div key={key}>
                      <dt>{name(key)}</dt>
                      <dd>
                        {value} {unit(r.units?.[key])}
                      </dd>
                    </div>
                  ))}
                </dl>
              </details>
            ))
          ) : (
            <p>No eligible measurements found for this appointment’s patient. Check the missing-data notes below, and confirm their measurements are saved under this patient in Overview.</p>
          )}
        </section>
        <section>
          <span className="visit-step">05</span>
          <h3>Historical items to review</h3>
          {data.historicalFlags.length ? (
            data.historicalFlags.map((f) => (
              <div className="visit-flag" key={f.analysisId}>
                <strong>Saved analysis #{f.analysisId}</strong>
                <p>
                  {f.status.replaceAll("_", " ").toLowerCase()} ·{" "}
                  {date(f.recordedAt)}
                </p>
                <small>Urgency not recorded; clinical assessment needed.</small>
              </div>
            ))
          ) : (
            <p>No pending historical analyses recorded.</p>
          )}
        </section>
      </div>
      <details className="visit-gaps" open>
        <summary>What’s missing or limited</summary>
        <ul>
          {data.missingData.map((s) => (
            <li key={s}>{s}</li>
          ))}
        </ul>
        <p className="small">{data.comparisonRule}</p>
      </details>
    </div>
  );
}

function Approved({ data, onFollowUp, doctor }) {
  const [checked, setChecked] = useState({});
  return (
    <section className="visit-approved">
      <span className="badge reviewed">
        Doctor approved · {date(data.approvedAt)}
      </span>
      <h2>Your visit, in simple words</h2>
      <p className="visit-verbatim">{data.summary}</p>
      <h3>Your next steps</h3>
      {data.actions.length ? (
        <>
          <div className="visit-checklist">
            {data.actions.map((action, i) => (
              <label key={i}>
                <input
                  type="checkbox"
                  checked={!!checked[i]}
                  onChange={(e) =>
                    setChecked({ ...checked, [i]: e.target.checked })
                  }
                />
                <span>{action}</span>
              </label>
            ))}
          </div>
          <small>
            Ticks are a reading aid for this session; they are not saved to your
            medical record.
          </small>
        </>
      ) : (
        <p>No checklist actions were specified by your doctor.</p>
      )}
      <div className="visit-followup">
        <div>
          <h3>Follow-up</h3>
          <p>
            {data.followUpDate
              ? `Suggested date: ${data.followUpDate}`
              : "No follow-up date specified."}{" "}
            · {data.doctorName}
          </p>
        </div>
        {!doctor && (
          <button className="primary" onClick={() => onFollowUp(data)}>
            Book follow-up
          </button>
        )}
      </div>
      <small>
        Approved by {data.doctorName}. Contact your clinic if any instruction is
        unclear.
      </small>
    </section>
  );
}

export default function VisitPanel({
  appointment,
  session,
  onClose,
  onApproved,
  onFollowUp,
}) {
  const doctor = session.role === "DOCTOR";
  const [brief, setBrief] = useState(null),
    [published, setPublished] = useState(null);
  const [loading, setLoading] = useState(true),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [notice, setNotice] = useState("");
  const [notes, setNotes] = useState(""),
    [summary, setSummary] = useState(""),
    [actions, setActions] = useState(""),
    [followUpDate, setFollowUpDate] = useState(""),
    [approved, setApproved] = useState(false);
  const [reload, setReload] = useState(0);
  const started = new Date(appointment.dateTime).getTime() <= Date.now();
  useEffect(() => {
    const controller = new AbortController();
    const options = { token: session.token, signal: controller.signal };
    setLoading(true);
    setError("");
    Promise.all([
      doctor
        ? request(`/api/appointments/${appointment.id}/brief`, options)
        : Promise.resolve(null),
      request(`/api/appointments/${appointment.id}/summary`, options),
    ])
      .then(([b, s]) => {
        if (!controller.signal.aborted) {
          setBrief(b);
          setPublished(s.summary);
        }
      })
      .catch((e) => {
        if (e.name !== "AbortError") setError(e.message);
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [appointment.id, session.token, doctor, reload]);
  async function draft() {
    setBusy(true);
    setError("");
    setNotice("");
    setApproved(false);
    try {
      const data = await request(
        `/api/appointments/${appointment.id}/summary/draft`,
        { token: session.token, method: "POST", body: { doctorNotes: notes } },
      );
      if (data.aiAvailable) setSummary(data.summary);
      setNotice(data.notice);
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }
  async function publish(event) {
    event.preventDefault();
    setBusy(true);
    setError("");
    const steps = actions
      .split("\n")
      .map((s) => s.trim())
      .filter(Boolean);
    if (steps.length > 12 || steps.some((s) => s.length > 300)) {
      setError("Use up to 12 actions, each no longer than 300 characters.");
      setBusy(false);
      return;
    }
    try {
      const data = await request(
        `/api/appointments/${appointment.id}/summary`,
        {
          token: session.token,
          method: "POST",
          body: {
            summary: summary.trim(),
            actions: steps,
            followUpDate: followUpDate || null,
            approve: approved,
            visitCompleted: approved,
          },
        },
      );
      setPublished(data);
      setNotice("Approved and released to this patient in the app.");
      onApproved();
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }
  return (
    <section className="visit-panel" aria-label="Visit workspace">
      <div className="section-head">
        <span className="eyebrow">APPOINTMENT #{appointment.id}</span>
        <button className="secondary" disabled={busy} onClick={onClose}>
          Close visit
        </button>
      </div>
      {loading ? (
        <p role="status">Preparing your visit workspace…</p>
      ) : (
        <>
          {brief && <Brief data={brief} />}
          {published ? (
            <Approved
              data={published}
              doctor={doctor}
              onFollowUp={onFollowUp}
            />
          ) : doctor && brief ? (
            started ? (
              <form
                className="visit-editor"
                onSubmit={publish}
                onChange={() => setApproved(false)}
              >
                <span className="eyebrow">AFTER THE CONSULTATION</span>
                <h2>Give your patient a clear next step</h2>
                <p>
                  Drafts stay in this screen until approval and are lost when
                  you close it. AI only helps rewrite your notes; it does not
                  decide the care plan.
                </p>
                <fieldset disabled={busy}>
                  <label>
                    Doctor’s visit notes
                    <textarea
                      rows={4}
                      maxLength={4000}
                      value={notes}
                      onChange={(e) => setNotes(e.target.value)}
                      placeholder="What you discussed, findings, and the agreed plan…"
                    />
                  </label>
                  <button
                    className="secondary"
                    type="button"
                    disabled={busy || !notes.trim()}
                    onClick={draft}
                  >
                    {busy ? "Working…" : "Help simplify my notes"}
                  </button>
                  <p className="small muted">
                    This optional action sends these notes to the configured
                    Gemini service. You can also write the summary yourself.
                  </p>
                  <label htmlFor="visit-summary">Patient summary</label>
                  <textarea
                    id="visit-summary"
                    rows={5}
                    maxLength={3000}
                    required
                    value={summary}
                    onChange={(e) => setSummary(e.target.value)}
                    placeholder="Write or edit the exact summary your patient will receive."
                  />
                  <label>
                    Actions for the patient · one per line
                    <textarea
                      rows={4}
                      maxLength={3600}
                      value={actions}
                      onChange={(e) => setActions(e.target.value)}
                      placeholder="Enter only actions you agreed with the patient."
                    />
                  </label>
                  <label>
                    Suggested follow-up date (optional)
                    <input
                      type="date"
                      min={new Date().toLocaleDateString("en-CA")}
                      value={followUpDate}
                      onChange={(e) => setFollowUpDate(e.target.value)}
                    />
                  </label>
                </fieldset>
                <label className="visit-approval">
                  <input
                    type="checkbox"
                    required
                    checked={approved}
                    disabled={busy}
                    onChange={(e) => {
                      e.stopPropagation();
                      setApproved(e.target.checked);
                    }}
                  />
                  <span>
                    The visit has taken place. I checked the summary, actions
                    and follow-up date, and approve releasing them to this
                    patient.
                  </span>
                </label>
                <button
                  className="primary"
                  disabled={busy || !approved || !summary.trim()}
                >
                  {busy ? "Working…" : "Approve & release to patient"}
                </button>
                <small>
                  Approval completes this visit. The released summary cannot be
                  edited.
                </small>
              </form>
            ) : (
              <div className="notice">
                The brief is ready. Prepare the patient summary after the
                appointment starts and the visit has taken place.
              </div>
            )
          ) : (
            !doctor &&
            !error && (
              <div className="notice">
                Your visit summary is awaiting your doctor’s approval. Only the
                approved summary will appear here.
              </div>
            )
          )}
        </>
      )}
      {notice && (
        <p className="notice" role="status">
          {notice}
        </p>
      )}
      {error && (
        <p className="notice error" role="alert">
          {error}
        </p>
      )}
      <button
        className="text-button"
        disabled={busy || loading}
        onClick={() => setReload((r) => r + 1)}
      >
        Refresh visit data
      </button>
    </section>
  );
}
