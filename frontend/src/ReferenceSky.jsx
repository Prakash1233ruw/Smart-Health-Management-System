import React, { useState } from "react";

export function SkyAppearance({ value, onChange }) {
  return (
    <div
      className="ref-appearance"
      role="group"
      aria-label="Sky appearance only, not a health assessment"
    >
      {["clear", "clouds", "storm", "sunrise"].map((sky) => (
        <button
          key={sky}
          aria-pressed={value === sky}
          onClick={() => onChange(sky)}
          title="Changes the background only"
        >
          {sky[0].toUpperCase() + sky.slice(1)}
        </button>
      ))}
    </div>
  );
}

export default function ReferenceSky({
  records,
  doctor,
  patientName,
  onVisits,
}) {
  const [selected, setSelected] = useState(0);
  const metrics = [
    ["bloodGlucoseLevel", "Glucose", "#f2c27a"],
    ["systolicPressure", "BP", "#f2c27a"],
    ["heartRate", "Heart rate", "#4fc3c9"],
  ];
  const [field, title] = metrics[selected];
  const readings = records.filter((r) => Number.isFinite(r.values?.[field]));
  const latest = readings[0],
    previous = readings.find((r) => r.timestamp < latest?.timestamp);
  const unit = latest?.values?.units?.[field];
  const knownUnit = unit && unit !== "NOT_RECORDED";
  const comparable = knownUnit && previous?.values?.units?.[field] === unit;
  return (
    <section className="ref-sky-content">
      <h1>
        {doctor
          ? `${patientName || "Your patient"}, in perspective.`
          : "Your health, in one sky."}
      </h1>
      <p className="ref-sub">
        {records.length
          ? "Your saved measurements, ready for your next conversation."
          : "Your story starts with a saved health record."}
      </p>
      <span className="ref-status">
        <i />
        {records.length
          ? "Recorded values · not a health forecast"
          : "No measurements recorded yet"}
      </span>
      <div className="ref-orbs">
        {metrics.map(([key, label, color], index) => (
          <button
            className="ref-orb"
            key={key}
            style={{
              "--orb-color": color,
              width: index === 0 ? 116 : index === 1 ? 106 : 92,
              height: index === 0 ? 116 : index === 1 ? 106 : 92,
            }}
            aria-label={`View ${label} measurement`}
            aria-pressed={selected === index}
            onClick={() => setSelected(index)}
          >
            <b>{label}</b>
            <span>tap</span>
          </button>
        ))}
      </div>
      <section className="ref-sheet" aria-live="polite">
        <h3>{title === "BP" ? "Systolic blood pressure" : title}</h3>
        <div className="ref-number">
          {latest
            ? `${latest.values[field]} ${knownUnit ? unit : "· unit not recorded"}`
            : "No recorded value"}
        </div>
        {latest && (
          <p>
            Recorded{" "}
            {new Date(latest.timestamp).toLocaleString(undefined, {
              dateStyle: "medium",
              timeStyle: "short",
            })}
          </p>
        )}
        <p>
          {comparable
            ? `Earlier: ${previous.values[field]} ${unit}, recorded ${new Date(previous.timestamp).toLocaleDateString()}. Your visit brief calculates changes for your doctor.`
            : latest
              ? "A comparison needs an earlier reading with the same recorded unit."
              : "Saved measurements will appear here once added to your patient record."}
        </p>
      </section>
      <section className="ref-sheet">
        <h3>{doctor ? "Before & after the visit" : "A clearer next step"}</h3>
        <p>
          {doctor
            ? "Read the appointment brief, then approve the patient’s plan after the consultation."
            : "Add your questions when booking. Read your doctor-approved summary after the visit."}
        </p>
        <button className="primary" onClick={onVisits}>
          {doctor ? "Open visits & briefs" : "Open visits & summaries"}
        </button>
      </section>
    </section>
  );
}
