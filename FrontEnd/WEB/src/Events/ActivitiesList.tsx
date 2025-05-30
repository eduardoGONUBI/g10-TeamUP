// src/Events/ActivitiesList.tsx

import React, { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { fetchMyEvents, type Event } from "../api/event";
import "./ActivitiesList.css";

// import your images
import FootballIcon   from "../assets/Sports_Icon/Football.png";
import FutsalIcon     from "../assets/Sports_Icon/futsal.jpg";
import CyclingIcon    from "../assets/Sports_Icon/ciclismo.jpg";
import SurfIcon       from "../assets/Sports_Icon/surf.jpg";
import VolleyballIcon from "../assets/Sports_Icon/voleyball.jpg";
import BasketballIcon from "../assets/Sports_Icon/Basketball.png";
import TennisIcon     from "../assets/Sports_Icon/Tennis.png";
import HandballIcon   from "../assets/Sports_Icon/handball.jpg";

const MyActivities: React.FC = () => {
  const [events, setEvents]   = useState<Event[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState<string | null>(null);
  const navigate              = useNavigate();

  useEffect(() => {
    fetchMyEvents()
      .then(setEvents)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <p>Loading…</p>;
  if (error)   return <p className="err">{error}</p>;

  const active    = events.filter((e) => e.status === "in progress");
  const concluded = events.filter((e) => e.status === "concluded");

// map lowercased sport.name → your imported image
const sportIcons: Record<string, string> = {
  football:   FootballIcon,
  futsal:     FutsalIcon,
  cycling:    CyclingIcon,    
  cicling:    CyclingIcon,    
  surf:       SurfIcon,
  volleyball: VolleyballIcon,
  basketball: BasketballIcon,
  tennis:     TennisIcon,
  handball:   HandballIcon,
};

  // renders your sport image or a medal fallback
  const renderSportIcon = (sport?: string | null) => {
    const key = sport?.toLowerCase() ?? "";
    const src = sportIcons[key];
    if (src) {
      return (
        <img
          src={src}
          alt={sport ?? ""}
          className="sport-icon-img"
        />
      );
    }
    return <span role="img" aria-label="sport">🏅</span>;
  };

  // format date as DD/MM/YY
  const formatDate = (iso: string) => {
    const d  = new Date(iso);
    const dd = String(d.getDate()).padStart(2, "0");
    const mm = String(d.getMonth() + 1).padStart(2, "0");
    const yy = String(d.getFullYear()).slice(-2);
    return `${dd}/${mm}/${yy}`;
  };

  // format time as HH:MM
  const formatTime = (iso: string) =>
    new Date(iso).toLocaleTimeString([], {
      hour:   "2-digit",
      minute: "2-digit",
    });

  const renderList = (arr: Event[]) =>
    arr.map((ev) => (
      <article
        key={ev.id}
        className={`event-card${ev.status === "concluded" ? " archived" : ""}`}
        onClick={() => navigate(`/events/${ev.id}`)}
      >
        <header>
          <div className="event-card-header">
            {renderSportIcon(ev.sport)}
            <strong className="event-name">{ev.name}</strong>
            <span className="event-sport">· {ev.sport}</span>
          </div>
          <small className="event-participants">
            {ev.participants.length}/{ev.max_participants} Participants
          </small>
        </header>

        <ul className="event-details">
          <li><b>Location:</b> {ev.place}</li>
          <li><b>Date:</b> {formatDate(ev.date)}</li>
        </ul>

        <footer className="event-card-footer">
          <span className="event-time">{formatTime(ev.date)}</span>
          <button
            onClick={(e) => {
              e.stopPropagation();
              navigate(`/events/${ev.id}`);
            }}
            className="see-btn"
          >
            See Activity&nbsp;›
          </button>
        </footer>
      </article>
    ));

  return (
    <>
      <div className="list-header">
        <h2>Activities Management</h2>
        <Link to="/events/create" className="create-btn">
          + Create Event
        </Link>
      </div>

      <div className="activities-columns">
        <div className="column">
          <h3>Active Activities ({active.length})</h3>
          {active.length === 0 ? <p>No active activities.</p> : renderList(active)}
        </div>
        <div className="column">
          <h3>Concluded Activities ({concluded.length})</h3>
          {concluded.length === 0 ? <p>No concluded activities.</p> : renderList(concluded)}
        </div>
      </div>
    </>
  );
};

export default MyActivities;
