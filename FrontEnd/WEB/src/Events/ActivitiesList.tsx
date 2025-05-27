// src/Events/ActivitiesList.tsx

import React, { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { fetchMyEvents, type Event } from "../api/event";
import "./ActivitiesList.css";

const MyActivities: React.FC = () => {
  const [events, setEvents]     = useState<Event[]>([]);
  const [loading, setLoading]   = useState(true);
  const [error, setError]       = useState<string | null>(null);
  const navigate                = useNavigate();

  useEffect(() => {
    fetchMyEvents()
      .then(setEvents)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <p>Loading…</p>;
  if (error)   return <p className="err">{error}</p>;

  // split by status
  const active    = events.filter(e => e.status === "in progress");
  const concluded = events.filter(e => e.status === "concluded");

  return (
    <>
      <div className="list-header">
        <h2>Activities Management</h2>
        <Link to="/events/create" className="create-btn">
          + Create Event
        </Link>
      </div>

      <div className="activities-columns">
        {/* ─── ACTIVE ─────────────────────────────── */}
        <div className="column">
          <h3>Active Activities ({active.length})</h3>
          {active.length === 0 && <p>No active activities.</p>}
          {active.map((ev) => (
            <article
              key={ev.id}
              className="event-card"
              onClick={() => navigate(`/events/${ev.id}`)}
            >
              <header>
                <div>
                  <strong>{ev.name}</strong> <span>· {ev.sport}</span>
                </div>
                <small>
                  {ev.participants.length}/{ev.max_participants} Participants
                </small>
              </header>

              <ul>
                <li><b>Location:</b> {ev.place}</li>
                <li>
                  <b>Date:</b>{" "}
                  {new Date(ev.date).toLocaleDateString()}{" "}
                  {new Date(ev.date).toLocaleTimeString([], {
                    hour: "2-digit",
                    minute: "2-digit",
                  })}
                </li>
                <li><b>Organizer:</b> {ev.creator.name}</li>
              </ul>

              <footer>
                <button
                  onClick={e => { e.stopPropagation(); navigate(`/events/${ev.id}`); }}
                  className="see-btn"
                >
                  See Activity&nbsp;›
                </button>
              </footer>
            </article>
          ))}
        </div>

        {/* ─── CONCLUDED ─────────────────────────── */}
        <div className="column">
          <h3>Concluded Activities ({concluded.length})</h3>
          {concluded.length === 0 && <p>No concluded activities.</p>}
          {concluded.map((ev) => (
            <article
              key={ev.id}
              className="event-card archived"
              onClick={() => navigate(`/events/${ev.id}`)}
            >
              <header>
                <div>
                  <strong>{ev.name}</strong> <span>· {ev.sport}</span>
                </div>
                <small>
                  {ev.participants.length}/{ev.max_participants} Participants
                </small>
              </header>

              <ul>
                <li><b>Location:</b> {ev.place}</li>
                <li>
                  <b>Date:</b>{" "}
                  {new Date(ev.date).toLocaleDateString()}{" "}
                  {new Date(ev.date).toLocaleTimeString([], {
                    hour: "2-digit",
                    minute: "2-digit",
                  })}
                </li>
                <li><b>Organizer:</b> {ev.creator.name}</li>
              </ul>

              <footer>
                <button
                  onClick={e => { e.stopPropagation(); navigate(`/events/${ev.id}`); }}
                  className="see-btn"
                >
                  See Activity&nbsp;›
                </button>
              </footer>
            </article>
          ))}
        </div>
      </div>
    </>
  );
};

export default MyActivities;
