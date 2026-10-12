import React from "react";
import ChangeSummary from "./ChangeSummary.jsx";
const displayUnit = (unit) => (unit && unit !== "NOT_RECORDED" ? unit : "");

const list = (value) =>
  Array.isArray(value)
    ? value.filter((item) => typeof item === "string" && item.trim())
    : [];
const reviewLabels = {
  PENDING_REVIEW: [
    "Awaiting doctor review",
    "This AI interpretation has not yet been reviewed by a doctor.",
  ],
  REVIEWED: [
    "Doctor review recorded",
    "Read your doctor's separate advice in the health timeline. A recorded review does not make the AI interpretation a diagnosis.",
  ],
  FOLLOW_UP_REQUIRED: [
    "Doctor requested follow-up",
    "Check your health timeline for the doctor's recommendation and follow-up date.",
  ],
  DISMISSED: [
    "Dismissed by doctor",
    "The doctor dismissed this analysis. Check their separate guidance in the health timeline.",
  ],
};
function takeaway(text) {
  if (typeof text !== "string" || !text.trim())
    return "No AI summary was provided for this result.";
  const sentences = [
    ...new Intl.Segmenter(undefined, { granularity: "sentence" }).segment(text),
  ]
    .slice(0, 2)
    .map((item) => item.segment)
    .join("")
    .trim();
  return sentences.length > 480
    ? sentences.slice(0, 480).replace(/\s+\S*$/, "") + "…"
    : sentences;
}
function Items({ items, empty = "No items provided in this result." }) {
  const values = list(items);
  return values.length ? (
    <ul className="insight-list">
      {values.map((item, index) => (
        <li key={index}>{item}</li>
      ))}
    </ul>
  ) : (
    <p className="small muted">{empty}</p>
  );
}
function Source({ children }) {
  return <span className="insight-source">{children}</span>;
}
function Card({
  title,
  source = "Gemini interpretation",
  children,
  className = "",
}) {
  return (
    <section className={`summary-card ${className}`}>
      <Source>{source}</Source>
      <h4>{title}</h4>
      {children}
    </section>
  );
}

export default function HealthSummary({
  data,
  doctor,
  onReview,
  onBook,
  Icon,
  Badge,
  Notice,
  label,
  human,
  disclaimer,
}) {
  if (!data) return null;
  if (!data.riskLevel)
    return (
      <ChangeSummary
        {...{ data, doctor, onReview, Icon, Badge, Notice, label, disclaimer }}
      />
    );
  const symptomResult = Boolean(data.riskLevel);
  const unavailable = data.narrativeStatus === "UNAVAILABLE";
  const notRequested = data.narrativeStatus === "NOT_REQUESTED";
  const narrative = !unavailable && !notRequested ? data.narrative : null;
  const explanation = symptomResult ? data.explanation : narrative?.summary;
  const changes = data.changes;
  const metrics = Array.isArray(changes?.metricChanges)
    ? changes.metricChanges
    : [];
  const warnings = list(data.warningSigns);
  const urgentEstimate = ["HIGH", "EMERGENCY"].includes(data.riskLevel);
  const review = (!unavailable &&
    !notRequested &&
    reviewLabels[data.reviewStatus]) || [
    symptomResult ? "No doctor review attached" : "Not saved for doctor review",
    symptomResult
      ? "Discuss this result with a qualified healthcare professional. This symptom analysis does not include a doctor review."
      : "A saved analysis is needed before a doctor can review it in this workspace.",
  ];
  const canReview =
    doctor &&
    !unavailable &&
    !notRequested &&
    data.analysisId &&
    typeof onReview === "function" &&
    ["PENDING_REVIEW", "FOLLOW_UP_REQUIRED"].includes(data.reviewStatus);
  return (
    <section
      className="analysis-result health-summary"
      aria-label={
        symptomResult ? "Symptom analysis summary" : "Health change summary"
      }
    >
      <div className="section-head">
        <div>
          <span className="eyebrow">YOUR HEALTH, EXPLAINED</span>
          <h3>{symptomResult ? "Symptom analysis" : "Your health changes"}</h3>
          <p className="small muted">
            A guide to the result and your next conversation with a doctor.
          </p>
        </div>
        <span className="soft-icon">
          <Icon name="spark" size={24} />
        </span>
      </div>

      {unavailable && (
        <Notice>
          AI narrative is currently unavailable. Your calculated changes are
          below. No analysis was saved for review.
        </Notice>
      )}
      {notRequested && (
        <Notice>
          There is not enough data to request an AI summary. Add symptoms or ask
          your doctor to record measurements.
        </Notice>
      )}
      {(symptomResult || narrative) && (
        <div className="insight-takeaway">
          <Source>Gemini interpretation · requires professional review</Source>
          <h4>Main takeaway</h4>
          <p>{takeaway(explanation)}</p>
          {symptomResult && (
            <div
              className={`insight-risk ${urgentEstimate ? "attention" : ""}`}
            >
              <span>AI risk estimate</span>
              <Badge status={data.riskLevel} />
              <p>
                This estimate is not a diagnosis or a guarantee that symptoms
                are safe.
              </p>
            </div>
          )}
        </div>
      )}

      {symptomResult && (warnings.length > 0 || urgentEstimate) && (
        <Card
          title="Warning signs to act on"
          source="Gemini-listed warning signs"
          className="insight-warning"
        >
          <Items
            items={warnings}
            empty="The model did not list specific warning signs. This does not rule out an emergency."
          />
          <p className="small">
            If you experience a listed warning sign, seek urgent medical help.
            If you believe you are having an emergency, seek immediate
            assistance; do not wait for AI or a review.
          </p>
        </Card>
      )}
      {symptomResult && !warnings.length && !urgentEstimate && (
        <Card title="Warning signs" source="Gemini-listed warning signs">
          <p className="small muted">
            No warning signs were listed in this response. This does not mean
            there are none.
          </p>
        </Card>
      )}

      {changes && (
        <section className="insight-facts">
          <Source>Recorded facts & server-calculated comparisons</Source>
          <h4>What changed in your records</h4>
          <p className="small muted">
            These comparisons come from your submitted symptoms and saved
            measurements, independently of Gemini's interpretation.
          </p>
          {changes.symptomsSubmitted && (
            <>
              <div className="symptom-groups">
                {[
                  [
                    changes.symptomChanges?.previousRecordAvailable
                      ? "Newly reported"
                      : "First recorded symptoms",
                    changes.symptomChanges?.newSymptoms,
                  ],
                  [
                    "Still reported",
                    changes.symptomChanges?.persistentSymptoms,
                  ],
                  [
                    "No longer reported",
                    changes.symptomChanges?.resolvedSymptoms,
                  ],
                ].map(([title, items]) => (
                  <div key={title}>
                    <span className="small muted">{title}</span>
                    <div>
                      {list(items).length ? (
                        list(items).map((item, i) => (
                          <span className="chip" key={i}>
                            {item}
                          </span>
                        ))
                      ) : (
                        <span className="small">None recorded</span>
                      )}
                    </div>
                  </div>
                ))}
              </div>
              <p className="small muted">
                {changes.symptomChanges?.previousRecordAvailable
                  ? "“No longer reported” means absent from this submission; it does not confirm recovery."
                  : "No earlier symptom record is available. These symptoms cannot yet be compared with a previous submission."}
              </p>
            </>
          )}
          {!changes.symptomsSubmitted && (
            <p className="small muted">
              Symptoms were not submitted for this comparison.
            </p>
          )}
          {metrics.length > 0 ? (
            <div className="insight-metrics">
              {metrics.map((metric, index) => (
                <div className="insight-metric" key={metric.metric + index}>
                  <span>{label(metric.metric)}</span>
                  <div className="insight-values">
                    <span>
                      {metric.previousValue ?? "—"}
                      <small>
                        Previous ·{" "}
                        {displayUnit(metric.previousUnit) ||
                          "Unit not recorded"}
                      </small>
                    </span>
                    <span aria-hidden="true">→</span>
                    <span>
                      {metric.latestValue ?? "—"}
                      <small>
                        Latest ·{" "}
                        {displayUnit(metric.unit) || "Unit not recorded"}
                      </small>
                    </span>
                  </div>
                  <p>
                    {metric.direction === "INSUFFICIENT_DATA" ||
                    metric.previousValue == null ||
                    metric.latestValue == null
                      ? metric.previousValue != null &&
                        metric.latestValue != null &&
                        metric.unit !== metric.previousUnit
                        ? !displayUnit(metric.unit) ||
                          !displayUnit(metric.previousUnit)
                          ? "Missing unit; a doctor must verify it before comparison."
                          : "Different units; no comparison calculated."
                        : "Not enough readings to compare"
                      : metric.direction === "STABLE"
                        ? "Unchanged from previous reading"
                        : `${human(metric.direction)}${metric.absoluteChange != null ? ` · change ${metric.absoluteChange}` : ""}`}
                  </p>
                </div>
              ))}
            </div>
          ) : (
            <p className="small muted">
              No measurement comparisons are available.
            </p>
          )}
          <p className="small muted">
            Numerical movement does not establish improvement or deterioration.
            Only explicitly recorded units are shown. Readings with different or
            missing units are not compared against readings with known units.
          </p>
          {metrics.length > 0 && (
            <details>
              <summary>Calculated measurement changes</summary>
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
                      <th>Change</th>
                      <th>Direction</th>
                    </tr>
                  </thead>
                  <tbody>
                    {metrics.map((metric, index) => (
                      <tr key={metric.metric + index}>
                        <td>{label(metric.metric)}</td>
                        <td>
                          {metric.previousValue ?? "—"}{" "}
                          {displayUnit(metric.previousUnit)}
                        </td>
                        <td>
                          {metric.latestValue ?? "—"} {displayUnit(metric.unit)}
                        </td>
                        <td>
                          {metric.absoluteChange ?? "—"}
                          {metric.percentageChange != null && (
                            <small> ({metric.percentageChange}%)</small>
                          )}
                        </td>
                        <td>{human(metric.direction)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </details>
          )}
        </section>
      )}

      {symptomResult && (
        <div className="insight-columns">
          <Card title="Possible explanations">
            <p className="small muted">
              Possibilities to discuss, not confirmed conditions. Their order
              does not indicate likelihood.
            </p>
            <Items items={data.possibleConditions} />
          </Card>
          <Card title="Suggested next steps">
            <p className="small muted">
              Discuss these suggestions with your clinician before arranging
              tests.
            </p>
            <h5>Suggested tests</h5>
            <Items items={data.recommendedTests} />
            <h5>Suggested specialist</h5>
            <p className="insight-specialist">
              {data.recommendedSpecialist ||
                "No specialist suggested in this result."}
            </p>
            {onBook && (
              <button className="primary" onClick={onBook}>
                Book a doctor <Icon name="arrow" />
              </button>
            )}
          </Card>
        </div>
      )}
      {narrative && (
        <div className="insight-columns">
          <Card title="Changes highlighted by Gemini">
            <Items items={narrative.importantChanges} />
          </Card>
          <Card title="Suggested next steps">
            <h5>For doctor review</h5>
            <Items items={narrative.itemsForDoctorReview} />
            <h5>Questions to ask your doctor</h5>
            <Items items={narrative.questionsForDoctor} />
          </Card>
        </div>
      )}

      <section className="insight-review">
        <span className="insight-review-icon">
          <Icon name="shield" size={22} />
        </span>
        <div>
          <Source>Doctor review · separate from AI interpretation</Source>
          <h4>{review[0]}</h4>
          <p className="small muted">{review[1]}</p>
          {data.analysisId != null && (
            <span className="small muted">
              Analysis reference #{data.analysisId}
            </span>
          )}
        </div>
        {canReview && (
          <button className="primary" onClick={() => onReview(data)}>
            Review analysis <Icon name="arrow" />
          </button>
        )}
      </section>
      {(explanation || narrative) && (
        <details className="insight-full">
          <summary>Read the full AI explanation</summary>
          <Source>Gemini interpretation</Source>
          <p className="summary">
            {typeof explanation === "string"
              ? explanation
              : "No explanation provided."}
          </p>
        </details>
      )}
      {(list(data.dataConsidered).length > 0 ||
        list(data.insufficientData).length > 0) && (
        <details>
          <summary>Data considered & limitations</summary>
          <h5>Records used</h5>
          <Items
            items={data.dataConsidered}
            empty="No data sources were listed."
          />
          <h5>Limits of this comparison</h5>
          <Items
            items={data.insufficientData}
            empty="No limitations were listed; this does not establish that the data is complete."
          />
        </details>
      )}
      <p className="disclaimer">
        <Icon name="shield" size={18} />
        {data.disclaimer || disclaimer}
      </p>
    </section>
  );
}
