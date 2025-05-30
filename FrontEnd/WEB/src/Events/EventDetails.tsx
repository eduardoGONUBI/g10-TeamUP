// ─── src/pages/EventDetails.tsx ──────────────────────────────────────────────
import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import "./EventDetails.css";
import { fetchXpLevel } from "../api/user";

import avatarDefault from "../assets/avatar-default.jpg";
import type { Event, Me, Participant } from "../api/event";

// ── weather icons ────────────────────────────────────────────────────────────
import {
  WiDaySunny,
  WiNightClear,
  WiRain,
  WiRainMix,
  WiSnow,
  WiCloudy,
  WiStormShowers,
  WiFog,
  WiNightAltCloudy,
} from "react-icons/wi";

// ── sport icons ──────────────────────────────────────────────────────────────
import FootballIcon from "../assets/Sports_Icon/Football.png";
import FutsalIcon from "../assets/Sports_Icon/futsal.jpg";
import CyclingIcon from "../assets/Sports_Icon/ciclismo.jpg";
import SurfIcon from "../assets/Sports_Icon/surf.jpg";
import VolleyballIcon from "../assets/Sports_Icon/voleyball.jpg";
import BasketballIcon from "../assets/Sports_Icon/Basketball.png";
import TennisIcon from "../assets/Sports_Icon/Tennis.png";
import HandballIcon from "../assets/Sports_Icon/handball.jpg";

const EventDetails: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [event, setEvent] = useState<Event | null>(null);
  const [participants, setParticipants] = useState<Participant[]>([]);
  const [levels, setLevels] = useState<Record<number, number>>({});
  const [me, setMe] = useState<Me | null>(null);
  const [loading, setLoading] = useState(true);
  const nav = useNavigate();

  const token =
    localStorage.getItem("auth_token") ||
    sessionStorage.getItem("auth_token");

  // format helpers
  const fmt = (n?: number) => (n != null ? `${Math.round(n)}°C` : "—");
  const formatDate = (iso: string) => {
    const d = new Date(iso);
    const dd = String(d.getDate()).padStart(2, "0");
    const mm = String(d.getMonth() + 1).padStart(2, "0");
    const yy = String(d.getFullYear()).slice(-2);
    return `${dd}/${mm}/${yy}`;
  };
  const formatTime = (iso: string) =>
    new Date(iso).toLocaleTimeString([], {
      hour: "2-digit",
      minute: "2-digit",
    });

  // weather icon picker
  function pickIcon(desc: string) {
    const d = desc.toLowerCase();
    if (d.includes("fog") || d.includes("mist")) return <WiFog size={32} />;
    if (d.includes("drizzle")) return <WiRainMix size={32} />;
    if (d.includes("rain") || d.includes("shower"))
      return <WiRain size={32} />;
    if (d.includes("thunder") || d.includes("storm"))
      return <WiStormShowers size={32} />;
    if (d.includes("snow") || d.includes("sleet")) return <WiSnow size={32} />;
    if (d.includes("clear")) {
      const hour = new Date(event?.date ?? "").getHours();
      return hour >= 6 && hour < 18 ? (
        <WiDaySunny size={32} />
      ) : (
        <WiNightClear size={32} />
      );
    }
    if (d.includes("cloud") && d.includes("night"))
      return <WiNightAltCloudy size={32} />;
    if (d.includes("cloud")) return <WiCloudy size={32} />;
    return <WiDaySunny size={32} />;
  }

  // sport icons map
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

  // render your sport icon or fallback
  const renderSportIcon = (sportName: string | null) => {
    const key = sportName?.toLowerCase() ?? "";
    const src = sportIcons[key];
    if (src) {
      return <img src={src} alt={sportName || ""} className="sport-icon-img" />;
    }
    return <span role="img" aria-label="sport">🏅</span>;
  };

  // 1) load current user
  useEffect(() => {
    if (!token) return;
    fetch(`/api/auth/me`, {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((r) => r.json())
      .then(setMe)
      .catch(console.error);
  }, [token]);

  // 2) load event details
  useEffect(() => {
    if (!id || !token) return;
    fetch(`/api/events/${id}`, {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((r) => r.json())
      .then(setEvent)
      .catch(console.error);
  }, [id, token]);

// 3) load participants
useEffect(() => {
  if (!id || !token) return;
  fetch(`/api/events/${id}/participants`, {
    headers: { Authorization: `Bearer ${token}` },
  })
    .then((r) => r.json())
    .then((data: { participants?: Participant[] }) => {
      // força o tipo de participants
      const list: Participant[] = data.participants ?? [];
      setParticipants(list);

      // para cada participante faz fetch do level
      return Promise.all(
        list.map((p: Participant) =>
          fetchXpLevel(p.id).then((pr) => ({ id: p.id, level: pr.level }))
        )
      );
    })
    .then((arr) => {
      const map: Record<number, number> = {};
      arr.forEach(({ id, level }) => {
        map[id] = level;
      });
      setLevels(map);
    })
    .catch(console.error)
    .finally(() => setLoading(false));
}, [id, token]);

  // 4) cancel
  async function cancelEvent() {
    if (!id || !token) return;
    if (!window.confirm("Cancel this activity?")) return;
    const res = await fetch(`/api/events/${id}`, {
      method: "DELETE",
      headers: { Authorization: `Bearer ${token}` },
    });
    if (res.ok) nav("/my-activities");
    else {
      const j = await res.json().catch(() => ({}));
      alert(j.error ?? "Failed to cancel.");
    }
  }

  // 5) conclude
  async function concludeEvent() {
    if (!id || !token) return;
    if (!window.confirm("Mark as concluded?")) return;
    const res = await fetch(`/api/events/${id}/conclude`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
    });
    if (res.ok) setEvent((e) => (e ? { ...e, status: "concluded" } : e));
    else {
      const j = await res.json().catch(() => ({}));
      alert(j.error ?? "Failed to conclude.");
    }
  }

  // 6) reopen
  async function reopenEvent() {
    if (!id || !token) return;
    if (!window.confirm("Reopen activity?")) return;
    const res = await fetch(`/api/events/${id}`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({ status: "in progress" }),
    });
    if (res.ok) setEvent((e) => (e ? { ...e, status: "in progress" } : e));
    else {
      const j = await res.json().catch(() => ({}));
      alert(j.error ?? "Failed to reopen.");
    }
  }

  // 7) kick participant
  async function kickParticipant(userId: number) {
    if (!window.confirm("Tens a certeza que queres remover este participante?")) {
      return;
    }
    const res = await fetch(
      `/api/events/${id}/participants/${userId}`,
      {
        method: "DELETE",
        headers: { Authorization: `Bearer ${token}` },
      }
    );
    if (res.ok) {
      setParticipants((prev) => prev.filter((p) => p.id !== userId));
      alert("Participante removido com sucesso.");
    } else {
      const err = await res.json().catch(() => ({}));
      alert(err.error ?? "Falha ao remover participante.");
    }
  }

  if (loading || !me) return <p className="loading">Loading…</p>;
  if (!event) return <p>Evento não encontrado.</p>;

  const dateStr = formatDate(event.date);
  const timeStr = formatTime(event.date);

  const isDone = event.status === "concluded";

  return (
    <section className="event-page">
      <header className="event-header">
        <div className="event-header-main">
          <h2 className="event-title">
            {renderSportIcon(event.sport)}{" "}
            <span className="event-name">{event.name}</span>
          </h2>
          <div className="sport-name">{event.sport}</div>
          <p className="event-meta">
            {participants.length}/{event.max_participants} participantes,{" "}
            {dateStr} {timeStr}
          </p>
        </div>

        <div className="event-header-actions">
          {isDone && <span className="event-concluded-badge">Concluded</span>}
          {
            <div className="event-actions">
              {!isDone ? (
                <>
                  <button className="btn btn-danger" onClick={cancelEvent}>
                    Cancel
                  </button>
                  <button className="btn btn-success" onClick={concludeEvent}>
                    Conclude
                  </button>
                </>
              ) : (
                <button className="btn btn-warning" onClick={reopenEvent}>
                  Reopen
                </button>
              )}
            </div>
          }
        </div>
      </header>

      <div className="map-wrapper">
        {event.latitude != null && event.longitude != null ? (
          <iframe
            title="map"
            loading="lazy"
            width="100%"
            height="300"
            style={{
              borderRadius: 8,
              boxShadow: "0 2px 8px rgba(0,0,0,.2)",
              border: 0,
            }}
            referrerPolicy="no-referrer-when-downgrade"
            src={`https://www.google.com/maps/embed/v1/place?key=${import.meta.env.VITE_GOOGLE_MAPS_API_KEY}
              &q=${event.latitude},${event.longitude}&zoom=15&maptype=roadmap`}
            allowFullScreen
          />
        ) : (
          <p>Mapa indisponível</p>
        )}
      </div>

      {event.weather && (
        <>
          <div className="weather-forecast-header">
            Forecast for <strong>{dateStr}</strong> at <strong>{timeStr}</strong>
          </div>
          <div className="weather-card">
            <div className="weather-main">
              {pickIcon(event.weather.description || "")}
              <span className="weather-temp">{fmt(event.weather.temp)}</span>
            </div>
            <div className="weather-extra">
              <span>H {fmt(event.weather.high_temp)}</span>
              <span>L {fmt(event.weather.low_temp)}</span>
            </div>
            <small className="weather-desc">
              {event.weather.description || "—"}
            </small>
          </div>
        </>
      )}

      <button
        className="chat-fab"
        onClick={() => nav(`/chat/${event.id}`)}
        title="Go to chat"
      >
        💬
      </button>

<ul className="participants-grid">
   {participants.map((p) => (
     <li
       key={p.id}
       className="participant-card"
       onClick={() => nav(`/profile/${p.id}`)}
     >
       <div className="avatar-wrapper">
         <img
           src={p.avatar_url || avatarDefault}
           alt={p.name}
           className="participant-avatar"
         />
         {/* badge de nível */}
         {levels[p.id] != null && (
           <span className="level-badge">Lvl {levels[p.id]}</span>
         )}
       </div>
       <div className="participant-details">
         <span className="participant-name">{p.name}</span>
         {p.rating != null && (
           <span className="participant-rating">⭐ {p.rating}</span>
         )}
       </div>
       {!isDone && (
         <button
           className="kick-btn"
           onClick={(e) => {
             e.stopPropagation();
             kickParticipant(p.id);
           }}
           title="Remover participante"
         >
           ✖
         </button>
       )}
     </li>
   ))}
 </ul>
    </section>
  );
};

export default EventDetails;
