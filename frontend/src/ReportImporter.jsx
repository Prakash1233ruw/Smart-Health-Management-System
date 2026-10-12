import React, { useEffect, useRef, useState } from "react";
import { request } from "./api.js";

export default function ReportImporter({
  session,
  patientId,
  patientName,
  onClose,
  onSaved,
  Modal,
  Notice,
  label,
}) {
  const [file, setFile] = useState(null),
    [preview, setPreview] = useState(""),
    [draft, setDraft] = useState(null),
    [rows, setRows] = useState([]),
    [date, setDate] = useState(""),
    [verified, setVerified] = useState(false),
    [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  const controller = useRef(null);
  useEffect(() => () => controller.current?.abort(), []);
  useEffect(() => {
    if (!file) return;
    const url = URL.createObjectURL(file);
    setPreview(url);
    return () => URL.revokeObjectURL(url);
  }, [file]);
  function changeRow(index, patch) {
    setRows((items) =>
      items.map((row, i) => (i === index ? { ...row, ...patch } : row)),
    );
    setVerified(false);
  }
  async function extract() {
    setError("");
    if (!file || file.size > 8 * 1024 * 1024) {
      setError("Choose a PDF, PNG, or JPEG up to 8 MB.");
      return;
    }
    setBusy(true);
    controller.current = new AbortController();
    try {
      const form = new FormData();
      form.append("file", file);
      form.append("patientId", String(patientId));
      const result = await request("/api/report-imports", {
        token: session.token,
        method: "POST",
        body: form,
        signal: controller.current.signal,
      });
      setDraft(result);
      setDate(result.reportDate || "");
      setRows(result.rows.map((row) => ({ ...row, selected: false })));
      setVerified(false);
    } catch (ex) {
      if (ex.name !== "AbortError") setError(ex.message);
    } finally {
      setBusy(false);
    }
  }
  function validRow(row) {
    return (
      draft?.allowedUnits?.[row.metric]?.includes(row.unit) &&
      row.value !== "" &&
      Number.isFinite(Number(row.value)) &&
      Number(row.value) >= 0
    );
  }
  async function confirm(event) {
    event.preventDefault();
    setError("");
    const selected = rows.filter((row) => row.selected);
    if (!selected.length || !verified || !date) {
      setError(
        "Select measurements, enter the collection date, and verify the report before saving.",
      );
      return;
    }
    if (selected.some((row) => !validRow(row))) {
      setError("Correct the selected values and units, or exclude those rows.");
      return;
    }
    if (new Set(selected.map((row) => row.metric)).size !== selected.length) {
      setError(
        "Choose one result per measurement. Do not combine different collection dates.",
      );
      return;
    }
    setBusy(true);
    controller.current = new AbortController();
    try {
      await request(`/api/report-imports/${draft.draftId}/confirm`, {
        token: session.token,
        method: "POST",
        signal: controller.current.signal,
        body: {
          recordedDate: date,
          verifiedAgainstReport: verified,
          rows: selected.map(({ index, metric, value, unit }) => ({
            index,
            metric,
            value: Number(value),
            unit,
          })),
        },
      });
      onSaved();
    } catch (ex) {
      if (ex.name !== "AbortError") setError(ex.message);
    } finally {
      setBusy(false);
    }
  }
  return (
    <Modal title="Upload health report" busy={busy} onClose={onClose}>
      <p className="muted">
        For <strong>{patientName || `Patient #${patientId}`}</strong> · Patient
        #{patientId}
      </p>
      <Notice>
        Gemini will receive this report to extract measurements. Nothing is
        saved as a health measurement until you review and confirm it. Use
        reports you are authorized to process.
      </Notice>
      {!draft ? (
        <div className="report-upload">
          <div
            className="report-drop-zone"
            onDragOver={(e) => e.preventDefault()}
            onDrop={(e) => {
              e.preventDefault();
              if (!busy) {
                setFile(e.dataTransfer.files?.[0] || null);
                setError("");
              }
            }}
          >
            <span className="report-drop-symbol" aria-hidden="true">
              ↧
            </span>
            <h3>Drop & verify</h3>
            <p>
              {file ? file.name : "Drop a report here, or choose a file below."}
            </p>
            <label>
              Report file
              <input
                type="file"
                accept="application/pdf,image/png,image/jpeg"
                disabled={busy}
                onChange={(e) => {
                  setFile(e.target.files?.[0] || null);
                  setError("");
                }}
              />
            </label>
          </div>
          <p className="small muted">
            PDF (up to 5 pages), PNG, or JPEG · up to 8 MB. Use a clear report
            showing results, units, and collection dates.
          </p>
          <button
            className="primary wide"
            onClick={extract}
            disabled={!file || busy}
          >
            {busy
              ? "Reading report with Gemini…"
              : "Extract measurements with Gemini"}
          </button>
        </div>
      ) : (
        <div className="report-split">
          <aside className="report-original">
            <span className="eyebrow">ORIGINAL REPORT</span>
            <h3>Read the source.</h3>
            {preview &&
              (file?.type?.startsWith("image/") ? (
                <img src={preview} alt="Original report" />
              ) : (
                <iframe src={preview} title="Original PDF report" />
              ))}
            <a
              className="secondary"
              href={preview}
              target="_blank"
              rel="noopener noreferrer"
            >
              Open report in new tab
            </a>
          </aside>
          <form onSubmit={confirm} className="report-review">
            <div className="section-head">
              <div>
                <span className="eyebrow">UNCONFIRMED EXTRACTION</span>
                <h3>Check before saving</h3>
              </div>
              {preview && (
                <a
                  className="secondary"
                  href={preview}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  Open original report
                </a>
              )}
            </div>
            {file?.type?.startsWith("image/") && preview && (
              <img
                className="report-preview"
                src={preview}
                alt="Original uploaded report for checking extracted measurements"
              />
            )}
            {draft.warnings?.length > 0 && (
              <div className="report-warnings">
                <h4>Things to check</h4>
                <ul>
                  {draft.warnings.map((warning, i) => (
                    <li key={i}>{warning}</li>
                  ))}
                </ul>
              </div>
            )}
            <label>
              Collection / measurement date
              <input
                type="date"
                required
                max={new Date().toLocaleDateString("en-CA")}
                value={date}
                disabled={busy}
                onChange={(e) => {
                  setDate(e.target.value);
                  setVerified(false);
                }}
              />
            </label>
            <p className="small muted">
              Use the date these selected results were measured, not the upload
              date. Import results from different dates separately. The report
              supplies a date only; no measurement time is inferred.
            </p>
            <button
              type="button"
              className="secondary"
              disabled={busy}
              onClick={() => {
                setRows((items) =>
                  items.map((row) => ({ ...row, selected: validRow(row) })),
                );
                setVerified(false);
              }}
            >
              Select supported rows
            </button>
            <div className="report-rows">
              {rows.map((row, i) => (
                <section
                  className={`report-row ${!validRow(row) || row.evidenceStatus !== "TEXT_MATCHED" ? "needs-verification" : ""}`}
                  key={row.index}
                >
                  {!row.unit && (
                    <span className="badge follow_up_required">
                      Missing unit · verify
                    </span>
                  )}
                  <label className="checkbox">
                    <input
                      type="checkbox"
                      aria-label={`Include result ${i + 1}`}
                      checked={row.selected}
                      disabled={busy}
                      onChange={(e) =>
                        changeRow(i, { selected: e.target.checked })
                      }
                    />
                    Include result {i + 1} ·{" "}
                    {row.supported
                      ? label(row.metric)
                      : row.metric || "Unrecognized measurement"}
                  </label>
                  <p className="small report-evidence">
                    Page {row.page} ·{" "}
                    {row.evidenceStatus === "TEXT_MATCHED"
                      ? "Excerpt found in PDF; value still needs review"
                      : row.evidenceStatus === "VISUAL_REVIEW_REQUIRED"
                        ? "Check the original image against this excerpt"
                        : "Excerpt could not be verified; check the original"}
                  </p>
                  <blockquote>
                    {row.sourceText || "No source excerpt was provided."}
                  </blockquote>
                  <div className="report-fields">
                    <label>
                      Measurement {i + 1}
                      <select
                        value={row.metric}
                        disabled={busy}
                        onChange={(e) =>
                          changeRow(i, { metric: e.target.value, unit: "" })
                        }
                      >
                        <option value="">Choose supported measurement</option>
                        {!draft.allowedUnits[row.metric] && row.metric && (
                          <option value={row.metric}>
                            {row.metric} (unsupported)
                          </option>
                        )}
                        {Object.keys(draft.allowedUnits).map((key) => (
                          <option key={key} value={key}>
                            {label(key)}
                          </option>
                        ))}
                      </select>
                    </label>
                    <label>
                      Result value {i + 1}
                      <input
                        type="text"
                        inputMode="decimal"
                        value={row.value}
                        disabled={busy}
                        onChange={(e) =>
                          changeRow(i, { value: e.target.value })
                        }
                      />
                    </label>
                    <label>
                      Unit {i + 1}
                      <select
                        value={row.unit}
                        disabled={busy}
                        onChange={(e) => changeRow(i, { unit: e.target.value })}
                      >
                        <option value="">Confirm unit from report</option>
                        {row.unit &&
                          !draft.allowedUnits[row.metric]?.includes(
                            row.unit,
                          ) && (
                            <option value={row.unit}>
                              {row.unit} (unsupported)
                            </option>
                          )}
                        {(draft.allowedUnits[row.metric] || []).map((unit) => (
                          <option key={unit}>{unit}</option>
                        ))}
                      </select>
                    </label>
                  </div>
                  {!validRow(row) && (
                    <p className="small report-unresolved">
                      Needs correction or exclusion: use a supported
                      measurement, a numeric result, and its explicit unit. Do
                      not guess or substitute a reference-range value.
                    </p>
                  )}
                </section>
              ))}
            </div>
            <label className="checkbox report-confirm">
              <input
                type="checkbox"
                checked={verified}
                disabled={busy}
                onChange={(e) => setVerified(e.target.checked)}
              />
              I checked the patient identity, collection date, selected values,
              and units against the original report.
            </label>
            <button
              className="primary wide"
              disabled={busy || !verified || !rows.some((row) => row.selected)}
            >
              {busy
                ? "Saving confirmed measurements…"
                : `Save ${rows.filter((row) => row.selected).length} confirmed measurements`}
            </button>
            <p className="small muted">
              One new health record will be created. Unselected rows are
              excluded. Unit conversion and diagnosis are not performed.
            </p>
          </form>
        </div>
      )}
      {error && <Notice error>{error}</Notice>}
    </Modal>
  );
}
