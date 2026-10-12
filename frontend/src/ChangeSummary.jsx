import React from "react";

const items = (value) =>
  Array.isArray(value)
    ? value.filter((x) => typeof x === "string" && x.trim())
    : [];
const unit = (value) => (value && value !== "NOT_RECORDED" ? value : "");
const number = (value) =>
  value == null
    ? "—"
    : new Intl.NumberFormat(undefined, { maximumFractionDigits: 20 }).format(
        value,
      );
const date = (value) => {
  if (!value || Number.isNaN(new Date(value).getTime())) return "";
  return new Intl.DateTimeFormat(undefined, { dateStyle: "medium" }).format(
    new Date(value),
  );
};
export function comparisonGroups(data) {
  const metrics = Array.isArray(data?.changes?.metricChanges)
    ? data.changes.metricChanges
    : [];
  const comparable = (m) =>
    m.previousValue != null &&
    m.latestValue != null &&
    unit(m.unit) &&
    unit(m.unit) === unit(m.previousUnit) &&
    m.absoluteChange != null &&
    ["INCREASED", "DECREASED", "STABLE"].includes(m.direction);
  return {
    metrics,
    changed: metrics.filter((m) => comparable(m) && m.direction !== "STABLE"),
    stable: metrics.filter((m) => comparable(m) && m.direction === "STABLE"),
    incomplete: metrics.filter(
      (m) =>
        !comparable(m) && (m.previousValue != null || m.latestValue != null),
    ),
  };
}
export function shortSummary(text) {
  if (typeof text !== "string" || !text.trim()) return "";
  const first =
    [
      ...new Intl.Segmenter(undefined, { granularity: "sentence" }).segment(
        text,
      ),
    ][0]?.segment.trim() || text.trim();
  return first.length > 240
    ? first.slice(0, 240).replace(/\s+\S*$/, "") + "…"
    : first;
}
const reason = (m) =>
  m.latestValue == null
    ? "Latest reading not recorded"
    : m.previousValue == null
      ? "No earlier reading"
      : !unit(m.unit) || !unit(m.previousUnit)
        ? "Missing unit · doctor verification needed"
        : m.unit !== m.previousUnit
          ? "Different units · not compared"
          : "Comparison unavailable";

export default function ChangeSummary({
  data,
  doctor,
  onReview,
  Icon,
  Badge,
  Notice,
  label,
  disclaimer,
}) {
  const { metrics, changed, stable, incomplete } = comparisonGroups(data);
  const available =
    !["UNAVAILABLE", "NOT_REQUESTED"].includes(data.narrativeStatus) &&
    data.narrative;
  const narrative = available ? data.narrative : null;
  const symptom = data.changes?.symptomChanges;
  const symptomGroups = [
    [
      symptom?.previousRecordAvailable
        ? "Newly reported"
        : "First recorded symptoms",
      items(symptom?.newSymptoms),
    ],
    ["Still reported", items(symptom?.persistentSymptoms)],
    ["No longer reported", items(symptom?.resolvedSymptoms)],
  ].filter(([, values]) => values.length);
  const reviewTitle =
    {
      PENDING_REVIEW: "Awaiting doctor review",
      REVIEWED: "Doctor review recorded",
      FOLLOW_UP_REQUIRED: "Doctor requested follow-up",
      DISMISSED: "Dismissed by doctor",
    }[data.reviewStatus] || "Not saved for doctor review";
  const reviewable =
    doctor &&
    narrative &&
    data.analysisId &&
    ["PENDING_REVIEW", "FOLLOW_UP_REQUIRED"].includes(data.reviewStatus) &&
    onReview;
  const nextSteps = items(narrative?.itemsForDoctorReview);
  return (
    <section
      className="analysis-result health-summary change-summary"
      aria-label="Health change summary"
    >
      <div className="section-head">
        <div>
          <span className="eyebrow">PATIENT HEALTH TIMELINE</span>
          <h3>Your health changes</h3>
        </div>
        <span className="chip">Saved record comparison</span>
      </div>
      <section className="change-review-strip">
        <div>
          <h4>{reviewTitle}</h4>
          <span className="small muted">AI support · not a diagnosis</span>
        </div>
        {reviewable && (
          <button className="primary" onClick={() => onReview(data)}>
            Review analysis <Icon name="arrow" />
          </button>
        )}
      </section>
      {narrative && (
        <div className="insight-takeaway">
          <span className="insight-source">AI summary</span>
          <h4>Main takeaway</h4>
          <p>
            {shortSummary(narrative.summary) || "No AI summary was provided."}
          </p>
        </div>
      )}
      {data.narrativeStatus === "UNAVAILABLE" && (
        <Notice>
          AI narrative is currently unavailable. Your calculated changes are
          below. No analysis was saved for review.
        </Notice>
      )}
      {data.narrativeStatus === "NOT_REQUESTED" && (
        <Notice>
          There is not enough data to request an AI summary. Add symptoms or ask
          your doctor to record measurements.
        </Notice>
      )}
      <section className="insight-facts">
        <h4>What changed in your records</h4>
        <p className="small muted">
          Calculated from saved records. An increase or decrease does not
          establish improvement or deterioration.
        </p>
        {data.changes?.symptomsSubmitted && (
          <>
            {symptomGroups.length ? (
              <div className="symptom-groups">
                {symptomGroups.map(([title, values]) => (
                  <div key={title}>
                    <span className="small muted">{title}</span>
                    <div>
                      {values.map((value, i) => (
                        <span className="chip" key={i}>
                          {value}
                        </span>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <p className="small muted">No symptom changes were reported.</p>
            )}
            {!symptom?.previousRecordAvailable ? (
              <p className="small muted">
                No earlier symptom record is available.
              </p>
            ) : (
              items(symptom.resolvedSymptoms).length > 0 && (
                <p className="small muted">
                  “No longer reported” does not confirm recovery.
                </p>
              )
            )}
          </>
        )}
        <div className="change-counts">
          <span>
            <strong>{changed.length}</strong>{" "}
            {changed.length === 1 ? "measurement" : "measurements"} changed
          </span>
          {stable.length > 0 && (
            <span>
              <strong>{stable.length}</strong> unchanged
            </span>
          )}
          {incomplete.length > 0 && (
            <span>
              <strong>{incomplete.length}</strong> need more information
            </span>
          )}
        </div>
        {changed.length > 0 ? (
          <div className="change-reading-list">
            {changed.map((m, i) => (
              <article
                className="change-reading insight-metric"
                key={m.metric + i}
              >
                <div>
                  <strong>{label(m.metric)}</strong>
                  <small className="muted">
                    {date(m.previousRecordedAt) && date(m.latestRecordedAt)
                      ? `${date(m.previousRecordedAt)} → ${date(m.latestRecordedAt)}`
                      : "Previous → latest recorded reading"}
                  </small>
                </div>
                <div className="change-reading-values">
                  <span>{number(m.previousValue)}</span>
                  <span aria-hidden="true">→</span>
                  <strong>
                    {number(m.latestValue)} <small>{unit(m.unit)}</small>
                  </strong>
                </div>
                <span className="change-delta">
                  {m.direction === "INCREASED" ? "↑ Increased" : "↓ Decreased"}{" "}
                  · {Number(m.absoluteChange) > 0 ? "+" : ""}
                  {number(m.absoluteChange)} {unit(m.unit)}
                </span>
              </article>
            ))}
          </div>
        ) : (
          <p className="small">
            {stable.length
              ? "No numerical changes in measurements with matching recorded units."
              : "No measurements can be reliably compared yet."}
          </p>
        )}
        {incomplete.length > 0 && (
          <details className="change-incomplete">
            <summary>
              {incomplete.length}{" "}
              {incomplete.length === 1 ? "measurement" : "measurements"} could
              not be compared
            </summary>
            <p className="small muted">
              Ask your doctor to verify missing units in Overview → Add missing
              units. Unknown units are never assumed here.
            </p>
            <ul className="insight-list">
              {incomplete.map((m, i) => (
                <li key={m.metric + i}>
                  <strong>{label(m.metric)}</strong> · {reason(m)}
                </li>
              ))}
            </ul>
          </details>
        )}
      </section>
      {nextSteps.length > 0 && (
        <section className="change-next">
          <span className="insight-source">
            AI suggestions · discuss with your doctor
          </span>
          <h4>Suggested next steps</h4>
          <ul className="insight-list">
            {nextSteps.slice(0, 3).map((step, i) => (
              <li key={i}>{step}</li>
            ))}
          </ul>
        </section>
      )}
      <details className="change-details">
        <summary>Full comparison and AI details</summary>
        {metrics.length > 0 && (
          <>
            <h4>Calculated measurement changes</h4>
            <div
              className="table-wrap"
              role="region"
              aria-label="Measurement comparison table"
              tabIndex={0}
            >
              <table>
                <thead>
                  <tr>
                    <th>Measurement</th>
                    <th>Previous</th>
                    <th>Latest</th>
                    <th>Change / status</th>
                  </tr>
                </thead>
                <tbody>
                  {metrics
                    .filter(
                      (m) => m.previousValue != null || m.latestValue != null,
                    )
                    .map((m, i) => (
                      <tr key={m.metric + i}>
                        <td>{label(m.metric)}</td>
                        <td>
                          {number(m.previousValue)}{" "}
                          {unit(m.previousUnit) ||
                            (m.previousValue != null ? "(unit unknown)" : "")}
                        </td>
                        <td>
                          {number(m.latestValue)}{" "}
                          {unit(m.unit) ||
                            (m.latestValue != null ? "(unit unknown)" : "")}
                        </td>
                        <td>
                          {incomplete.includes(m)
                            ? reason(m)
                            : `${Number(m.absoluteChange) > 0 ? "+" : ""}${number(m.absoluteChange)} ${unit(m.unit)}`}
                        </td>
                      </tr>
                    ))}
                </tbody>
              </table>
            </div>
          </>
        )}
        {narrative && (
          <>
            <h4>Full AI explanation</h4>
            <p>{narrative.summary}</p>
            {[
              ["Changes highlighted by AI", narrative.importantChanges],
              [
                "All suggestions for doctor review",
                narrative.itemsForDoctorReview,
              ],
              ["Questions to ask your doctor", narrative.questionsForDoctor],
            ].map(
              ([title, values]) =>
                items(values).length > 0 && (
                  <section key={title}>
                    <h5>{title}</h5>
                    <ul className="insight-list">
                      {items(values).map((value, i) => (
                        <li key={i}>{value}</li>
                      ))}
                    </ul>
                  </section>
                ),
            )}
          </>
        )}
        {items(data.dataConsidered).length > 0 && (
          <>
            <h5>Records used</h5>
            <ul className="insight-list">
              {items(data.dataConsidered).map((x, i) => (
                <li key={i}>{x}</li>
              ))}
            </ul>
          </>
        )}
        {items(data.insufficientData).length > 0 && (
          <>
            <h5>Limitations</h5>
            <ul className="insight-list">
              {items(data.insufficientData).map((x, i) => (
                <li key={i}>{x}</li>
              ))}
            </ul>
          </>
        )}
        {data.analysisId != null && (
          <p className="small muted">
            Analysis reference #{data.analysisId}. This is a saved snapshot;
            generate a new analysis after measurements change.
          </p>
        )}
        <p className="small muted">{data.disclaimer || disclaimer}</p>
      </details>
    </section>
  );
}
