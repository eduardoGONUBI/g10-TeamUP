// ─── src/pages/EventDetails.tsx ──────────────────────────────────────────────
import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import "./EventDetails.css";
import {
  fetchXpLevel,
  fetchAvatar,                    // ← ADDED
} from "../api/user";
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

const ATTRIBUTES = [
  { value: "", label: "Give feedback…" },
  { value: "good_teammate", label: "✅ Good teammate" },
  { value: "friendly", label: "😊 Friendly" },
  { value: "team_player", label: "🤝 Team player" },
  { value: "toxic", label: "⚠️ Toxic" },
  { value: "bad_sport", label: "👎 Bad sport" },
  { value: "afk", label: "🚶  No show" },
];

const EventDetails: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [event, setEvent] = useState<Event | null>(null);
  const [participants, setParticipants] = useState<Participant[]>([]);
  const [levels, setLevels] = useState<Record<number, number>>({});
  const [feedbackSent, setFeedbackSent] = useState<Record<number, boolean>>({});
  const [avatars, setAvatars] = useState<Record<number, string>>({}); // ← ADDED
  const [me, setMe] = useState<Me | null>(null);
  const [loading, setLoading] = useState(true);
  const [openFeedback, setOpenFeedback] = useState<number | null>(null);
  const nav = useNavigate();

  const token =
    localStorage.getItem("auth_token") ||
    sessionStorage.getItem("auth_token");

  /* ───────────────────────── Helpers ───────────────────────── */
  const fmt = (n?: number) => (n != null ? `${Math.round(n)}°C` : "—");
  const formatDate = (iso: string) =>
    new Date(iso).toLocaleDateString("en-GB");
  const formatTime = (iso: string) =>
    new Date(iso).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });

  function pickIcon(desc: string) {
    const d = desc.toLowerCase();
    if (d.includes("fog") || d.includes("mist")) return <WiFog size={32} />;
    if (d.includes("drizzle")) return <WiRainMix size={32} />;
    if (d.includes("rain") || d.includes("shower")) return <WiRain size={32} />;
    if (d.includes("thunder") || d.includes("storm")) return <WiStormShowers size={32} />;
    if (d.includes("snow") || d.includes("sleet")) return <WiSnow size={32} />;
    if (d.includes("clear")) {
      const hour = new Date(event?.date ?? "").getHours();
      return hour >= 6 && hour < 18 ? <WiDaySunny size={32} /> : <WiNightClear size={32} />;
    }
    if (d.includes("cloud") && d.includes("night")) return <WiNightAltCloudy size={32} />;
    if (d.includes("cloud")) return <WiCloudy size={32} />;
    return <WiDaySunny size={32} />;
  }

  // map lower-cased sport name → image
  const sportIcons: Record<string, string> = {
    football: FootballIcon,
    futsal: FutsalIcon,
    cycling: CyclingIcon,
    cicling: CyclingIcon,
    surf: SurfIcon,
    volleyball: VolleyballIcon,
    basketball: BasketballIcon,
    tennis: TennisIcon,
    handball: HandballIcon,
  };
  const renderSportIcon = (sportName: string | null) => {
    const key = sportName?.toLowerCase() ?? "";
    const src = sportIcons[key];
    if (src) {
      return <img src={src} alt={sportName || ""} className="sport-icon-img" />;
    }
    return <span role="img" aria-label="sport">🏅</span>;
  };

  /* ───────────────────────── API calls ───────────────────────── */

  // 1) load current user
  useEffect(() => {
    if (!token) return;
    fetch(`/api/auth/me`, { headers: { Authorization: `Bearer ${token}` } })
      .then((r) => r.json())
      .then(setMe)
      .catch(console.error);
  }, [token]);

  // 2) load event details
  useEffect(() => {
    if (!id || !token) return;
    fetch(`/api/events/${id}`, { headers: { Authorization: `Bearer ${token}` } })
      .then((r) => r.json())
      .then(setEvent)
      .catch(console.error);
  }, [id, token]);

  // 3) load participants + their levels + avatars
  useEffect(() => {
    if (!id || !token) return;
    let mounted = true;

    fetch(`/api/events/${id}/participants`, {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((r) => r.json())
      .then(async (data: { participants?: Participant[] }) => {
        if (!mounted) return;
        const list: Participant[] = data.participants ?? [];
        setParticipants(list);

        // levels
        const lvlArray = await Promise.all(
          list.map((p) =>
            fetchXpLevel(p.id).then((pr) => ({ id: p.id, level: pr.level }))
          )
        );
        if (!mounted) return;
        const lvlMap: Record<number, number> = {};
        lvlArray.forEach(({ id, level }) => {
          lvlMap[id] = level;
        });
        setLevels(lvlMap);

        // initialise feedbackSent flags
        const flags: Record<number, boolean> = {};
        list.forEach((p) => {
          flags[p.id] = false;
        });
        setFeedbackSent(flags);

        // avatars
        const avatarPairs = await Promise.all(
          list.map(async (p) => {
            try {
              const url = await fetchAvatar(p.id);
              return { id: p.id, url };
            } catch {
              return { id: p.id, url: "" };
            }
          })
        );
        if (!mounted) return;

        // revoke any old blob URLs before setting new ones
        Object.values(avatars).forEach((u) => {
          if (u.startsWith("blob:")) URL.revokeObjectURL(u);
        });

        const avMap: Record<number, string> = {};
        avatarPairs.forEach(({ id, url }) => {
          if (url) avMap[id] = url;
        });
        setAvatars(avMap);
      })
      .catch(console.error)
      .finally(() => {
        if (mounted) setLoading(false);
      });

    return () => {
      mounted = false;
      // revoke blob URLs on unmount
      Object.values(avatars).forEach((u) => {
        if (u.startsWith("blob:")) URL.revokeObjectURL(u);
      });
    };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id, token]);

  /* ─────────────── Feedback helper ─────────────── */
  async function giveFeedback(ratedId: number, attr: string) {
    if (!attr) return;
    if (!id || !token) return;

    const attribute = attr.trim();
    if (!attribute) return;

    try {
      const res = await fetch(`/api/events/${id}/feedback`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          user_id: ratedId,
          attribute: attribute,
        }),
      });

      const j = await res.json().catch(() => ({}));
      if (!res.ok) {
        alert(j.error ?? res.statusText);
        return;
      }
      alert("Feedback sent!");
      setFeedbackSent((prev) => ({ ...prev, [ratedId]: true }));
    } catch (err) {
      console.error(err);
      alert("Failed to send feedback.");
    }
  }

  /* ───────────────────────── Misc actions ───────────────────────── */
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

  async function concludeEvent() {
    if (!id || !token) return;
    if (!window.confirm("Mark as concluded?")) return;
    const res = await fetch(`/api/events/${id}/conclude`, {
      method: "PUT",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
    });
    if (res.ok) setEvent((e) => (e ? { ...e, status: "concluded" } : e));
    else {
      const j = await res.json().catch(() => ({}));
      alert(j.error ?? "Failed to conclude.");
    }
  }

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

  async function kickParticipant(userId: number) {
    if (!window.confirm("Are you sure you want to remove this participant?")) return;
    const res = await fetch(`/api/events/${id}/participants/${userId}`, {
      method: "DELETE",
      headers: { Authorization: `Bearer ${token}` },
    });
    if (res.ok) {
      setParticipants((prev) => prev.filter((p) => p.id !== userId));
      alert("Participant removed.");
    } else {
      const err = await res.json().catch(() => ({}));
      alert(err.error ?? "Failed to remove participant.");
    }
  }

  /* ───────────────────────── Render ───────────────────────── */
  if (loading || !me) return <p className="loading">Loading…</p>;
  if (!event) return <p>Event not found.</p>;

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
            {participants.length}/{event.max_participants} participants,{" "}
            {formatDate(event.date)} {formatTime(event.date)}
          </p>
        </div>

        <div className="event-header-actions">
          {isDone && <span className="event-concluded-badge">Concluded</span>}
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
        </div>
      </header>

      <div className="map-wrapper">
        {event.latitude != null && event.longitude != null ? (
          <iframe
            title="map"
            loading="lazy"
            width="100%"
            height="300"
            style={{ borderRadius: 8, boxShadow: "0 2px 8px rgba(0,0,0,.2)", border: 0 }}
            referrerPolicy="no-referrer-when-downgrade"
            src={`https://www.google.com/maps/embed/v1/place?key=${import.meta.env.VITE_GOOGLE_MAPS_API_KEY}
              &q=${event.latitude},${event.longitude}&zoom=15&maptype=roadmap`}
            allowFullScreen
          />
        ) : (
          <p>Map unavailable</p>
        )}
      </div>

      {event.weather && (
        <>
          <div className="weather-forecast-header">
            Forecast for <strong>{formatDate(event.date)}</strong> at <strong>{formatTime(event.date)}</strong>
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
                src={avatars[p.id] || p.avatar_url || avatarDefault}
                alt={p.name}
                className="participant-avatar"
              />
              {levels[p.id] != null && (
                <span className="level-badge">
                  Lvl {levels[p.id] ?? 1}
                </span>
              )}
            </div>

            <div className="participant-details">
              <span className="participant-name">{p.name}</span>
              {p.rating != null && (
                <span className="participant-rating">⭐ {p.rating}</span>
              )}
            </div>

            {/* Feedback dropdown (only if concluded & not me) */}
            {isDone && p.id !== me.id && (
              <div
                className={`feedback-dropdown ${feedbackSent[p.id] ? "sent" : ""}`}
                onClick={(e) => e.stopPropagation()}
              >
                <button
                  className="feedback-toggle"
                  onClick={() =>
                    setOpenFeedback(openFeedback === p.id ? null : p.id)
                  }
                  aria-expanded={openFeedback === p.id}
                >
                  {feedbackSent[p.id] ? "✓ Feedback sent" : "Give feedback"}
                  {!feedbackSent[p.id] && (
                    <span className="arrow">
                      {openFeedback === p.id ? "▴" : "▾"}
                    </span>
                  )}
                </button>

                {openFeedback === p.id && !feedbackSent[p.id] && (
                  <ul className="feedback-menu">
                    {ATTRIBUTES.slice(1).map((op) => (
                      <li key={op.value}>
                        <button
                          className="feedback-item"
                          onClick={() => {
                            giveFeedback(p.id, op.value);
                            setOpenFeedback(null);
                          }}
                        >
                          {op.label}
                        </button>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            )}

            {!isDone && (
              <button
                className="kick-btn"
                onClick={(e) => {
                  e.stopPropagation();
                  kickParticipant(p.id);
                }}
                title="Remove participant"
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
