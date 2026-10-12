import React, { useEffect, useState } from "react";
import { request } from "./api.js";
import VisitPanel from "./VisitPanel.jsx";
const localTime = () => {
  const d = new Date();
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000)
    .toISOString()
    .slice(0, 16);
};
const dateTime = (value) =>
  new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
const canceled = (status) =>
  ["canceled", "cancelled"].includes((status || "").toLowerCase());

export default function Appointments({
  session,
  patients = [],
  refreshSignal = 0,
  startBooking = false,
  initialReason = "",
  onBooked = () => {},
}) {
  const [visit, setVisit] = useState(null),
    [followUp, setFollowUp] = useState(null);
  const visitRef = React.useRef(null);
  useEffect(() => {
    if (visit)
      visitRef.current?.scrollIntoView({ behavior: "smooth", block: "start" });
  }, [visit]);
  const [now, setNow] = useState(Date.now());
  const touchStart = React.useRef(null);
  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 60000);
    return () => clearInterval(timer);
  }, []);
  const doctor = session.role === "DOCTOR";
  const [appointments, setAppointments] = useState([]),
    [directory, setDirectory] = useState([]);
  const [loading, setLoading] = useState(true),
    [refresh, setRefresh] = useState(0),
    [error, setError] = useState("");
  const [booking, setBooking] = useState(startBooking),
    [busy, setBusy] = useState(false),
    [cancel, setCancel] = useState(null),
    [message, setMessage] = useState("");
  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError("");
    Promise.all([
      request("/api/appointments/visits", {
        token: session.token,
        signal: controller.signal,
      }),
      doctor
        ? Promise.resolve([])
        : request("/api/doctors/directory", {
            token: session.token,
            signal: controller.signal,
          }),
    ])
      .then(([items, doctors]) => {
        if (!controller.signal.aborted) {
          setAppointments(
            items
              .filter((item) => !doctor || !canceled(item.status))
              .sort(
                (a, b) => b.dateTime.localeCompare(a.dateTime) || b.id - a.id,
              ),
          );
          setDirectory(doctors);
        }
      })
      .catch((error) => {
        if (error.name !== "AbortError") setError(error.message);
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [session.token, doctor, refresh, refreshSignal]);
  async function book(event) {
    event.preventDefault();
    setBusy(true);
    setError("");
    setMessage("");
    const fields = new FormData(event.currentTarget);
    try {
      const stamp = fields.get("dateTime");
      if (new Date(stamp).getTime() <= Date.now())
        throw new Error("Choose a future appointment date and time.");
      const body = {
        doctorId: Number(fields.get("doctorId")),
        dateTime: stamp + ":00",
        notes: fields.get("notes").trim(),
        questions: fields.get("questions").trim(),
      };
      await request("/api/appointments/book", {
        token: session.token,
        method: "POST",
        body,
      });
      setBooking(false);
      setRefresh((n) => n + 1);
      setFollowUp(null);
      onBooked();
      setMessage(
        "Your appointment is booked. A pre-visit brief is ready for your doctor.",
      );
    } catch (error) {
      setError(error.message);
    } finally {
      setBusy(false);
    }
  }
  async function cancelAppointment() {
    setBusy(true);
    setError("");
    setMessage("");
    try {
      await request("/api/appointments/" + cancel.id, {
        token: session.token,
        method: "DELETE",
      });
      setCancel(null);
      setRefresh((n) => n + 1);
      setMessage("Your appointment was canceled.");
    } catch (error) {
      setError(error.message);
    } finally {
      setBusy(false);
    }
  }
  return (
    <section className="panel">
      <div className="section-head">
        <div>
          <span className="eyebrow">MAKE TIME FOR YOUR HEALTH</span>
          <h2>{doctor ? "Your visits & briefs" : "Your visits & summaries"}</h2>
        </div>
        {!doctor && (
          <button
            className="primary"
            disabled={busy || loading}
            onClick={() => {
              setBooking(!booking);
              setCancel(null);
              setError("");
            }}
          >
            {booking ? "Close booking" : "Book appointment"}
          </button>
        )}
      </div>
      <p className="muted small">
        {doctor
          ? "Open a brief before the visit. After the consultation, approve a clear summary for your patient."
          : "Share why you are coming, add your questions, and read your doctor-approved plan after the visit."}
      </p>
      {message && (
        <div className="notice" role="status">
          {message}
        </div>
      )}
      {error && (
        <div className="notice error" role="alert">
          {error}
          <button
            className="text-button"
            onClick={() => setRefresh((n) => n + 1)}
          >
            Refresh appointments
          </button>
        </div>
      )}
      {booking && (
        <div className="booking-form enter">
          <h3>Book a consultation</h3>
          {!directory.length ? (
            <p className="muted">
              No doctors are available in the directory yet. Register a doctor
              first.
            </p>
          ) : (
            <form onSubmit={book}>
              {followUp && (
                <p className="notice">
                  Follow-up with {followUp.doctorName}.{" "}
                  {followUp.followUpDate
                    ? `Suggested date: ${followUp.followUpDate}. Choose a time with the clinic.`
                    : "Choose a suitable date and time."}
                </p>
              )}
              <div className="measurement-grid">
                <label>
                  Choose doctor
                  <select
                    key={followUp?.doctorId || "new"}
                    name="doctorId"
                    required
                    defaultValue={followUp?.doctorId || ""}
                  >
                    <option value="" disabled>
                      Select a doctor
                    </option>
                    {directory.map((d) => (
                      <option key={d.id} value={d.id}>
                        {d.name} · {d.specialization}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  Appointment date and time
                  <input
                    name="dateTime"
                    type="datetime-local"
                    required
                    min={localTime()}
                    defaultValue={
                      followUp?.followUpDate &&
                      followUp.followUpDate >= localTime().slice(0, 10)
                        ? followUp.followUpDate + "T09:00"
                        : ""
                    }
                  />
                </label>
              </div>
              <label>
                Reason for visit (optional)
                <textarea
                  name="notes"
                  rows={3}
                  maxLength={500}
                  placeholder="Describe how you feel and what you would like to discuss."
                  defaultValue={
                    followUp ? "Follow-up consultation" : initialReason
                  }
                />
              </label>
              <label>
                Questions for your doctor (optional)
                <textarea
                  name="questions"
                  rows={3}
                  maxLength={1000}
                  placeholder="What would you like to ask during your visit?"
                />
              </label>
              <p className="small muted">
                {followUp &&
                  "Follow-up times initially show 09:00 as an editable placeholder, not a reserved slot. "}
                Times use your local device time. Selecting a date does not
                guarantee a doctor's available time slot.
              </p>
              <button className="primary" disabled={busy}>
                {busy ? "Booking…" : "Confirm booking"}
              </button>
            </form>
          )}
        </div>
      )}
      {visit && (
        <div ref={visitRef}>
          <VisitPanel
            key={visit.id}
            appointment={visit}
            session={session}
            onClose={() => setVisit(null)}
            onApproved={() => setRefresh((n) => n + 1)}
            onFollowUp={(data) => {
              setFollowUp(data);
              setVisit(null);
              setBooking(true);
            }}
          />
        </div>
      )}
      {cancel && (
        <div className="cancel-confirm" role="alert">
          <h3>Cancel this appointment?</h3>
          <p className="small">
            {dateTime(cancel.dateTime)} · Appointment #{cancel.id}
          </p>
          <div>
            <button
              className="secondary"
              disabled={busy}
              onClick={() => setCancel(null)}
            >
              Keep appointment
            </button>
            <button
              className="danger-button"
              disabled={busy}
              onClick={cancelAppointment}
            >
              {busy ? "Canceling…" : "Yes, cancel appointment"}
            </button>
          </div>
        </div>
      )}
      {loading ? (
        <div className="loading" role="status">
          <span className="spinner" /> Loading appointments…
        </div>
      ) : !appointments.length ? (
        <div className="empty">
          <h3>{doctor ? "No upcoming appointments" : "No appointments yet"}</h3>
          <p>
            {doctor
              ? "New bookings will appear here."
              : "Choose a doctor and a date to book your first consultation."}
          </p>
        </div>
      ) : (
        <div className="appointment-list">
          {appointments.map((item) => {
            const person = directory.find((d) => d.id === item.doctorId);
            const patient = patients.find(
              (p) => String(p.id) === String(item.patientId),
            );
            return (
              <article
                className="appointment-card"
                key={item.id}
                onTouchStart={(e) => {
                  touchStart.current = e.touches[0].clientX;
                }}
                onTouchEnd={(e) => {
                  if (
                    !doctor &&
                    !canceled(item.status) &&
                    new Date(item.dateTime).getTime() > now &&
                    touchStart.current != null &&
                    e.changedTouches[0].clientX - touchStart.current < -70
                  ) {
                    setCancel(item);
                    setBooking(false);
                  }
                  touchStart.current = null;
                }}
              >
                <div className="appointment-date">
                  <strong>{new Date(item.dateTime).getDate()}</strong>
                  <span>
                    {new Intl.DateTimeFormat(undefined, {
                      month: "short",
                    }).format(new Date(item.dateTime))}
                  </span>
                </div>
                <div className="appointment-info">
                  <h3>
                    {doctor
                      ? item.patientName ||
                        patient?.name?.trim() ||
                        "Patient name unavailable"
                      : item.doctorName ||
                        person?.name ||
                        "Doctor #" + item.doctorId}
                  </h3>
                  <p className="small muted">
                    {dateTime(item.dateTime)}
                    {!doctor && person?.specialization
                      ? " · " + person.specialization
                      : ""}
                  </p>
                  {item.notes && (
                    <p className="small appointment-notes">{item.notes}</p>
                  )}
                  {!canceled(item.status) &&
                    new Date(item.dateTime).getTime() > now && (
                      <span className="ticket-countdown">
                        {Math.ceil(
                          (new Date(item.dateTime).getTime() - now) / 3600000,
                        ) < 24
                          ? `In ${Math.ceil((new Date(item.dateTime).getTime() - now) / 3600000)} hours`
                          : `In ${Math.ceil((new Date(item.dateTime).getTime() - now) / 86400000)} days`}{" "}
                        · {doctor ? "Clinic schedule" : "Your care ticket"}
                      </span>
                    )}
                </div>
                <div className="appointment-actions">
                  {!canceled(item.status) && (
                    <button
                      className="secondary"
                      onClick={() => {
                        setVisit(item);
                        setBooking(false);
                      }}
                    >
                      {doctor
                        ? "Open visit brief"
                        : item.summaryAvailable
                          ? "Read visit summary"
                          : "Summary status"}
                    </button>
                  )}
                  <span
                    className={
                      "badge " +
                      (canceled(item.status) ? "dismissed" : "reviewed")
                    }
                  >
                    {item.status || "Scheduled"}
                  </span>
                  {!doctor &&
                    !canceled(item.status) &&
                    new Date(item.dateTime) > new Date() && (
                      <button
                        className="text-button cancel-link"
                        disabled={busy}
                        onClick={() => {
                          setCancel(item);
                          setBooking(false);
                          setError("");
                        }}
                      >
                        Cancel
                      </button>
                    )}
                </div>
              </article>
            );
          })}
        </div>
      )}
      <button
        className="secondary"
        disabled={loading || busy}
        onClick={() => setRefresh((n) => n + 1)}
      >
        Refresh appointments
      </button>
    </section>
  );
}
