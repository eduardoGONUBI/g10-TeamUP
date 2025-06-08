// src/Events/ActivitiesList.tsx
import React, { useEffect, useState } from "react";
import { Link, useNavigate }          from "react-router-dom";
import { fetchMyEvents, type Event }  from "../api/event";
import "./ActivitiesList.css";

/* sport icons ---------------------------------------------------------------- */
import FootballIcon   from "../assets/Sports_Icon/Football.png";
import FutsalIcon     from "../assets/Sports_Icon/futsal.jpg";
import CyclingIcon    from "../assets/Sports_Icon/ciclismo.jpg";
import SurfIcon       from "../assets/Sports_Icon/surf.jpg";
import VolleyballIcon from "../assets/Sports_Icon/voleyball.jpg";
import BasketballIcon from "../assets/Sports_Icon/Basketball.png";
import TennisIcon     from "../assets/Sports_Icon/Tennis.png";
import HandballIcon   from "../assets/Sports_Icon/handball.jpg";

/* helpers -------------------------------------------------------------------- */
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

const renderSportIcon = (sport?: string | null) => {
  const src = sportIcons[(sport || "").toLowerCase()];
  return src
    ? <img src={src} alt={sport ?? ""} className="sport-icon-img" />
    : <span role="img" aria-label="sport">🏅</span>;
};

const formatDate = (iso: string) => {
  const d  = new Date(iso);
  const dd = String(d.getDate()).padStart(2, "0");
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  const yy = String(d.getFullYear()).slice(-2);
  return `${dd}/${mm}/${yy}`;
};

const formatTime = (iso: string) =>
  new Date(iso).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });

/* component ------------------------------------------------------------------ */
const PAGE_SIZE = 5;

const MyActivities: React.FC = () => {
  const [events,         setEvents]        = useState<Event[]>([]);
  const [loading,        setLoading]       = useState(true);
  const [error,          setError]         = useState<string | null>(null);
  const [activePage,     setActivePage]    = useState(1);
  const [concludedPage,  setConcludedPage] = useState(1);
  const navigate                          = useNavigate();

  useEffect(() => {
    fetchMyEvents()
      .then(setEvents)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <p>Loading…</p>;
  if (error)   return <p className="err">{error}</p>;

  const active    = events.filter(e => e.status === "in progress");
  const concluded = events.filter(e => e.status === "concluded");

  const totalActivePages    = Math.ceil(active.length / PAGE_SIZE) || 1;
  const totalConcludedPages = Math.ceil(concluded.length / PAGE_SIZE) || 1;

  const renderPaginated = (arr: Event[], page: number) => {
    const start = (page - 1) * PAGE_SIZE;
    return arr.slice(start, start + PAGE_SIZE).map(ev => {
      const when = ev.starts_at;
      return (
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
              {(ev.participants?.length ?? 0)}/{ev.max_participants} Participants
            </small>
          </header>

          <ul className="event-details">
            <li><b>Location:</b> {ev.place}</li>
            <li><b>Date:</b> {formatDate(when)}</li>
          </ul>

          <footer className="event-card-footer">
            <span className="event-time">{formatTime(when)}</span>
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
      );
    });
  };

  const renderPager = (page: number, total: number, onPrev: () => void, onNext: () => void) => (
    <div className="pager">
      <button onClick={onPrev} disabled={page <= 1}>‹ Prev</button>
      <span>{page} / {total}</span>
      <button onClick={onNext} disabled={page >= total}>Next ›</button>
    </div>
  );

  return (
    <>
      <div className="list-header">
        <h2>Activities Management</h2>
        <Link to="/events/create" className="create-btn">+ Create Event</Link>
      </div>

      <div className="activities-columns">
        <div className="column">
          <h3>Active Activities ({active.length})</h3>
          {active.length === 0
            ? <p>No active activities.</p>
            : renderPaginated(active, activePage)
          }
          {active.length > PAGE_SIZE && renderPager(
            activePage,
            totalActivePages,
            () => setActivePage(p => Math.max(1, p - 1)),
            () => setActivePage(p => Math.min(totalActivePages, p + 1))
          )}
        </div>
        <div className="column">
          <h3>Concluded Activities ({concluded.length})</h3>
          {concluded.length === 0
            ? <p>No concluded activities.</p>
            : renderPaginated(concluded, concludedPage)
          }
          {concluded.length > PAGE_SIZE && renderPager(
            concludedPage,
            totalConcludedPages,
            () => setConcludedPage(p => Math.max(1, p - 1)),
            () => setConcludedPage(p => Math.min(totalConcludedPages, p + 1))
          )}
        </div>
      </div>
    </>
  );
};

export default MyActivities;
