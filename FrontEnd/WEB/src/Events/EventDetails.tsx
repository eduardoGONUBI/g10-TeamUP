// src/pages/EventDetails.tsx

import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import "./EventDetails.css";
import avatarDefault from "../assets/avatar-default.jpg";
import type { Event, Me, Participant } from "../api/event";
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

const EventDetails: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [event, setEvent] = useState<Event | null>(null);
  const [participants, setParticipants] = useState<Participant[]>([]);
  const [me, setMe] = useState<Me | null>(null);
  const [loading, setLoading] = useState(true);
  const nav = useNavigate();

  const token = localStorage.getItem("auth_token") || sessionStorage.getItem("auth_token");

  const fmt = (n?: number) => n != null ? `${Math.round(n)}°C` : "—";

  function pickIcon(desc: string) {
    const d = desc.toLowerCase();
    if (d.includes("fog") || d.includes("mist")) return <WiFog size={32} />;
    if (d.includes("drizzle")) return <WiRainMix size={32} />;
    if (d.includes("rain") || d.includes("shower")) return <WiRain size={32} />;
    if (d.includes("thunder") || d.includes("storm")) return <WiStormShowers size={32} />;
    if (d.includes("snow") || d.includes("sleet")) return <WiSnow size={32} />;
    if (d.includes("clear")) {
      // simple day/night check
      const hour = new Date(event?.date ?? "").getHours();
      return hour >= 6 && hour < 18
        ? <WiDaySunny size={32} />
        : <WiNightClear size={32} />;
    }
    if (d.includes("cloud") && d.includes("night")) return <WiNightAltCloudy size={32} />;
    if (d.includes("cloud")) return <WiCloudy size={32} />;
    return <WiDaySunny size={32} />;
  }

  /** 1) Load current user */
  useEffect(() => {
    if (!token) return;
    fetch(`/api/auth/me`, { headers: { Authorization: `Bearer ${token}` } })
      .then(r => r.json())
      .then(setMe)
      .catch(console.error);
  }, [token]);

  // 2) Load event details 
  useEffect(() => {
    if (!id || !token) return;
    fetch(`/api/events/${id}`, {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then(r => r.json())
      .then(setEvent)
      .catch(console.error);
  }, [id, token]);

  /** 3) Load participants */
  useEffect(() => {
    if (!id || !token) return;
    fetch(`/api/events/${id}/participants`, { headers: { Authorization: `Bearer ${token}` } })
      .then(r => r.json())
      .then(data => setParticipants(data.participants ?? []))
      .catch(console.error)
      .finally(() => setLoading(false));
  }, [id, token]);

  /** 4) Cancel event */
  async function cancelEvent() {
    if (!id || !token) return;
    if (!window.confirm("Cancel this activity?")) return;
    const res = await fetch(`/api/events/${id}`, {
      method: "DELETE",
      headers: { Authorization: `Bearer ${token}` },
    });
    if (res.ok) {
      alert("Activity cancelled.");
      nav("/my-activities");
    } else {
      const j = await res.json().catch(() => ({}));
      alert(j.error ?? "Failed to cancel.");
    }
  }

  /** 5) Conclude event */
  async function concludeEvent() {
    if (!id || !token) return;
    if (!window.confirm("Mark this activity as concluded?")) return;
    const res = await fetch(`/api/events/${id}/conclude`, {
      method: "PUT",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
    });
    if (res.ok) {
      setEvent(e => e ? { ...e, status: "concluded" } : e);
      alert("Activity concluded!");
    } else {
      const j = await res.json().catch(() => ({}));
      alert(j.error ?? "Failed to conclude.");
    }
  }

  /** 6) Reopen event */
  async function reopenEvent() {
    if (!id || !token) return;
    if (!window.confirm("Reopen this activity?")) return;
    const res = await fetch(`/api/events/${id}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
      body: JSON.stringify({ status: "in progress" }),
    });
    if (res.ok) {
      setEvent(e => e ? { ...e, status: "in progress" } : e);
      alert("Activity reopened!");
    } else {
      const j = await res.json().catch(() => ({}));
      alert(j.error ?? "Failed to reopen.");
    }
  }

  if (loading || !me) return <p className="loading">Loading…</p>;
  if (!event) return <p>Evento não encontrado.</p>;

  const start = new Date(event.date);
  const dateStr = start.toLocaleDateString();
  const timeStr = start.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
  const isOwner = me.id === event.user_id;
  const isDone = event.status === "concluded";

  return (
    <section className="event-page">
      <header className="event-header">
        <div className="event-header-main">
          <h2>
            {event.name} <span role="img" aria-label="sport">⚽</span>
          </h2>
          <p>
            {participants.length}/{event.max_participants}&nbsp;participantes,&nbsp;
            {dateStr}&nbsp;{timeStr}
          </p>
        </div>
        <div className="event-header-actions">
          {isDone && <span className="event-concluded-badge">Concluded</span>}
          {isOwner && (
            <div className="event-actions">
              {!isDone ? (
                <>
                  <button className="btn btn-danger" onClick={cancelEvent}>Cancel</button>
                  <button className="btn btn-success" onClick={concludeEvent}>Conclude</button>
                </>
              ) : (
                <button className="btn btn-warning" onClick={reopenEvent}>Reopen</button>
              )}
            </div>
          )}
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
              border: 0
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

      {/* Forecast header */}
      {event.weather && (
        <div className="weather-forecast-header">
          Forecast for <strong>{dateStr}</strong> at <strong>{timeStr}</strong>
        </div>
      )}

      {/* Weather card */}
      {event.weather && (
        <div className="weather-card">
          <div className="weather-main">
          {pickIcon(event.weather.description ?? "")}
            <span className="weather-temp">{fmt(event.weather.temp)}</span>
          </div>
          <div className="weather-extra">
            <span>H {fmt(event.weather.high_temp)}</span>
            <span>L {fmt(event.weather.low_temp)}</span>
          </div>
        
 <small className="weather-desc">
   {event.weather.description ?? "—"}
 </small>
        </div>
      )}

      <button
        className="chat-fab"
        onClick={() => nav(`/chat/${event.id}`)}
        title="Ir para o chat"
      >
        💬
      </button>

      <ul className="participants-grid">
        {participants.map(p => (
          <li
            key={p.id}
            className="participant"
            onClick={() => nav(`/profile/${p.id}`)}
          >
            <div className="participant-info">
              <img
                src={p.avatar_url ?? avatarDefault}
                alt={p.name}
                className="participant-avatar"
              />
              <span className="participant-name">{p.name}</span>
            </div>
            {p.rating != null && (
              <span className="participant-rating">⭐{p.rating}</span>
            )}
          </li>
        ))}
      </ul>
    </section>
  );
};

export default EventDetails;
