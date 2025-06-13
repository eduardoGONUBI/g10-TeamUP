
import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import "./EventDetails.css";
import {
  fetchXpLevel,
  fetchAvatar,
} from "../api/user";
import avatarDefault from "../assets/avatar-default.jpg";
import type { Event, Me, Participant } from "../api/event";

import ConfirmModal from "../components/ConfirmModal";


// weather icons 
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

// sport icons 
import FootballIcon from "../assets/Sports_Icon/Football.png";
import FutsalIcon from "../assets/Sports_Icon/futsal.jpg";
import CyclingIcon from "../assets/Sports_Icon/ciclismo.jpg";
import SurfIcon from "../assets/Sports_Icon/surf.jpg";
import VolleyballIcon from "../assets/Sports_Icon/voleyball.jpg";
import BasketballIcon from "../assets/Sports_Icon/Basketball.png";
import TennisIcon from "../assets/Sports_Icon/Tennis.png";
import HandballIcon from "../assets/Sports_Icon/handball.jpg";

const ATTRIBUTES = [  // opçoes do feedback
  { value: "", label: "Give feedback…" },
  { value: "good_teammate", label: "✅ Good teammate" },
  { value: "friendly", label: "😊 Friendly" },
  { value: "team_player", label: "🤝 Team player" },
  { value: "toxic", label: "⚠️ Toxic" },
  { value: "bad_sport", label: "👎 Bad sport" },
  { value: "afk", label: "🚶  No show" },
];


const EventDetails: React.FC = () => {


  const { id } = useParams<{ id: string }>();    // id do evento
  const [event, setEvent] = useState<Event | null>(null);   // dados do evento
  const [participants, setParticipants] = useState<Participant[]>([]);  // participantes do evento
  const [levels, setLevels] = useState<Record<number, number>>({});   // niveis dos participantes
  const [feedbackSent, setFeedbackSent] = useState<Record<number, boolean>>({});      // indica se o participante ja recebeu feedback
  const [avatars, setAvatars] = useState<Record<number, string>>({});      //  foto perfil dos participantes
  const [me, setMe] = useState<Me | null>(null);         // informaçaoes sobre o utilizador autenticado
  const [loading, setLoading] = useState(true);   // loading
  const [openFeedback, setOpenFeedback] = useState<number | null>(null);  // indica qual participante tem o dropdown de feedback aberto


  const navigate = useNavigate();

  const storageKey = (eventId: string, myId: number | null) =>    //chave para guardar quem ja recebeu feedback
    `fb:${eventId}:${myId ?? 0}`;

  const loadRatedIds = (eventId: string, myId: number | null): number[] => {   // carrega do localstorage os participantes a quem ja foi dado feedback
    try {
      return JSON.parse(localStorage.getItem(storageKey(eventId, myId)) || "[]");
    } catch {
      return [];
    }
  };

  const saveRatedIds = (   // guarda no local storage os participantes que receberam feedback
    eventId: string,
    myId: number | null,
    arr: number[]
  ) => {
    localStorage.setItem(storageKey(eventId, myId), JSON.stringify(arr));
  };

  // modal state para pop ups
  const [modal, setModal] = useState<{
    open: boolean;
    action: "cancel" | "conclude" | "reopen" | "kick";
    targetId?: number;
  }>({ open: false, action: "cancel", targetId: undefined });

  const token =      // vai buscar o token
    localStorage.getItem("auth_token") ||
    sessionStorage.getItem("auth_token");


  /* --------------------Helpers ---------------------*/

  const fmt = (n?: number) => (n != null ? `${Math.round(n)}°C` : "—");   // formata um numero para celsius

  const formatDate = (iso: string) =>     // Converte data ISO para formato local DD/MM/YYYY
    new Date(iso).toLocaleDateString("en-GB"); 

  const formatTime = (iso: string) =>   // Converte data ISO para hora no formato HH:MM
    new Date(iso).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });


// escolhe o icon para a metereologia
  function pickIcon(desc: string) {
    const d = desc.toLowerCase();
    if (d.includes("fog") || d.includes("mist")) return <WiFog size={32} />;
    if (d.includes("drizzle")) return <WiRainMix size={32} />;
    if (d.includes("rain") || d.includes("shower")) return <WiRain size={32} />;
    if (d.includes("thunder") || d.includes("storm")) return <WiStormShowers size={32} />;
    if (d.includes("snow") || d.includes("sleet")) return <WiSnow size={32} />;
    if (d.includes("clear")) {
      const hour = new Date(event?.starts_at ?? "").getHours();
      return hour >= 6 && hour < 18 ? <WiDaySunny size={32} /> : <WiNightClear size={32} />;
    }
    if (d.includes("cloud") && d.includes("night")) return <WiNightAltCloudy size={32} />;
    if (d.includes("cloud")) return <WiCloudy size={32} />;
    return <WiDaySunny size={32} />;
  }

  //funpçao para quando o user confirma uma açao
  function onModalConfirm() {
    setModal(m => ({ ...m, open: false }));
    switch (modal.action) {
      case "cancel":
        cancelEvent();
        break;
      case "conclude":
        concludeEvent();
        break;
      case "reopen":
        reopenEvent();
        break;
      case "kick":
        if (modal.targetId != null) kickParticipant(modal.targetId);
        break;
    }
  }

  function onModalCancel() {  // cancelar o modal
    setModal(m => ({ ...m, open: false }));
  }



  // liga o desporto ao seu icon
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
  // devolve o icon correspodente
  const renderSportIcon = (sportName: string | null) => {
    const key = sportName?.toLowerCase() ?? "";
    const src = sportIcons[key];
    if (src) {
      return <img src={src} className="sport-icon-img" />;
    }
    return <span role="img" aria-label="sport">🏅</span>;
  };

  /*-------------------- API calls ---------------------------- */

  // carrega o utilizador autenticado
  useEffect(() => {
    if (!token) return;
    fetch("/api/auth/me", {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((res) => res.json())
      .then(setMe)
      .catch(console.error);
  }, [token]);

  // carrega os detalhes do evento
  useEffect(() => {
   if (!id || !token) return;  
    fetch(`/api/events/${id}`, {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((res) => res.json())
      .then(setEvent)
      .catch(console.error);
  }, [id, token]);

  // carrega participantes niveis e fotos de perfil
  useEffect(() => {
    if (!id || !token || !me) return;  
    let mounted = true;

    fetch(`/api/events/${id}/participants`, { // vai buscar participantes
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((res) => res.json())
      .then(async (data: { participants?: Participant[] }) => {
        if (!mounted) return;
        const list: Participant[] = data.participants ?? [];   // garante que tem uma lista de participantes mesmo estando vazia
        setParticipants(list);       //set participantes

        // fetch paralelo de levels 
        const lvlArray = await Promise.all(
          list.map((p) =>
            fetchXpLevel(p.id).then((pr) => ({ id: p.id, level: pr.level }))
          )
        );
        if (!mounted) return;
        const lvlMap: Record<number, number> = {};  // faz a ligaçao de user e lvl
        lvlArray.forEach(({ id, level }) => {
          lvlMap[id] = level;
        });
        setLevels(lvlMap);       // set level

        // verifica se ja foi enviado o feedback para cada participante
        const alreadyRated = loadRatedIds(id!, me?.id ?? null);
        const flags: Record<number, boolean> = {};
        list.forEach((p) => {
          flags[p.id] = alreadyRated.includes(p.id);
        });
        setFeedbackSent(flags);    // set feedback

        // fetch paralelo das fotos de perfil
        const avatarPairs = await Promise.all(
          list.map(async (p) => {
            try {
              const url = await fetchAvatar(p.id);  // devolve o url da imagem
              return { id: p.id, url };    
            } catch {
              return { id: p.id, url: "" };
            }
          })
        );
        if (!mounted) return;

        // liberta memoria
        Object.values(avatars).forEach((u) => {
          if (u.startsWith("blob:")) URL.revokeObjectURL(u);
        });

        const avMap: Record<number, string> = {};  // liga os avatares com os participantes
        avatarPairs.forEach(({ id, url }) => {
          if (url) avMap[id] = url;
        });
        setAvatars(avMap);    // set avatar
      })
      .catch(console.error)
      .finally(() => {
        if (mounted) setLoading(false);
      });

    return () => {
      mounted = false;  // impede atualizaçoes
      Object.values(avatars).forEach((u) => {   // liberta memoria
        if (u.startsWith("blob:")) URL.revokeObjectURL(u);
      });
    };
  }, [id, token, me]); 

  /* --------------Feedback helper ----------------------*/
  async function giveFeedback(ratedId: number, attr: string) {
    if (!attr || !id || !token) return;

  if (feedbackSent[ratedId]) return;   // se ja tiver sido enviado o feedback
    const attribute = attr.trim();
    if (!attribute) return;

    try {
      const res = await fetch(`/api/events/${id}/feedback`, {  // envia o feedback para o backend
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          user_id: ratedId,
          attribute,
        }),
      });
      const json = await res.json().catch(() => ({}));   // le a resposta
      if (!res.ok) {
        alert(json.error ?? res.statusText);
        return;
      }


    setFeedbackSent((prev) => ({ ...prev, [ratedId]: true }));    // marca feedback como enviado

    // guarda no localstorage para persistir
    const ratedArr = loadRatedIds(id!, me?.id ?? null);
    if (!ratedArr.includes(ratedId)) {
      saveRatedIds(id!, me?.id ?? null, [...ratedArr, ratedId]);
    }
    } catch (err) {
      console.error(err);
      alert("Failed to send feedback.");
    }
  }

  /* ----------------------funçoes------------------------- */
  async function cancelEvent() {   // cancela evento
    if (!id || !token) return;

    const res = await fetch(`/api/events/${id}`, {
      method: "DELETE",
      headers: { Authorization: `Bearer ${token}` },
    });
    if (res.ok) navigate("/my-activities");
    else {
      const json = await res.json().catch(() => ({}));
      alert(json.error ?? "Failed to cancel.");
    }
  }

  async function concludeEvent() {   // conclui evento
    if (!id || !token) return;
    const res = await fetch(`/api/events/${id}/conclude`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
    });
    if (res.ok) setEvent((e) => (e ? { ...e, status: "concluded" } : e));
    else {
      const json = await res.json().catch(() => ({}));
      alert(json.error ?? "Failed to conclude.");
    }
  }

  async function reopenEvent() {   // reabre evento
    if (!id || !token) return;
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
      const json = await res.json().catch(() => ({}));
      alert(json.error ?? "Failed to reopen.");
    }
  }

  async function kickParticipant(userId: number) {    //expulsa participante
    const res = await fetch(`/api/events/${id}/participants/${userId}`, {
      method: "DELETE",
      headers: { Authorization: `Bearer ${token}` },
    });
    if (res.ok) {
      setParticipants((prev) => prev.filter((p) => p.id !== userId));
    } else {
      const json = await res.json().catch(() => ({}));
      alert(json.error ?? "Failed to remove participant.");
    }
  }

  /* -----------------------Render----------------------------- */
  if (loading || !me) return <p className="loading">Loading…</p>;  // loading
  if (!event) return <p>Event not found.</p>;  

  const isDone = event.status === "concluded";  // bool para ver se evento esta concluido

  return (
    <section className="event-page">
      <ConfirmModal
        isOpen={modal.open}
        title={
          modal.action === "kick"
            ? "Remove this participant?"
            : modal.action === "cancel"
              ? "Cancel this event?"
              : modal.action === "conclude"
                ? "Mark as concluded?"
                : "Re-open this event?"
        }
        message={
          modal.action === "kick"
            ? "Are you sure you want to remove this participant from the event?"
            : modal.action === "cancel"
              ? "All participants will be notified."
              : modal.action === "conclude"
                ? "Participants will no longer be able to join."
                : "Participants will be able to join again."
        }
        confirmText={
          modal.action === "kick"
            ? "Yes, remove"
            : modal.action === "cancel"
              ? "Yes, cancel"
              : modal.action === "conclude"
                ? "Yes, conclude"
                : "Yes, reopen"
        }
        cancelText="No, go back"
        onConfirm={onModalConfirm}
        onCancel={onModalCancel}
      />
      <header className="event-header">
        <div className="event-header-main">
          <h2 className="event-title">
            {renderSportIcon(event.sport)}{" "}
            <span className="event-name">{event.name}</span>
          </h2>
          <div className="sport-name">{event.sport}</div>
          <p className="event-meta">
            {participants.length}/{event.max_participants} participants,{" "}
            {formatDate(event.starts_at)} {formatTime(event.starts_at)}
          </p>
        </div>

        <div className="event-header-actions">
          {isDone && <span className="event-concluded-badge">Concluded</span>}
          <div className="event-actions">
            {!isDone ? (
              <>
                <button
                  className="btn btn-primary"
                  onClick={() => navigate(`/events/${event.id}/edit`)}
                >
                  Edit
                </button>
                <button
                  className="btn btn-danger"
                  onClick={() => setModal({ open: true, action: "cancel" })}
                >
                  Cancel
                </button>
                <button
                  className="btn btn-success"
                  onClick={() => setModal({ open: true, action: "conclude" })}
                >
                  Conclude
                </button>
              </>
            ) : (
              <button
                className="btn btn-warning"
                onClick={() => setModal({ open: true, action: "reopen" })}
              >
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
            style={{
              borderRadius: 8,
              boxShadow: "0 2px 8px rgba(0,0,0,.2)",
              border: 0,
            }}
            referrerPolicy="no-referrer-when-downgrade"
            src={`https://www.google.com/maps/embed/v1/place?key=${import.meta.env.VITE_GOOGLE_MAPS_API_KEY}&q=${event.latitude},${event.longitude}&zoom=15&maptype=roadmap`}
            allowFullScreen
          />
        ) : (
          <p>Map unavailable</p>
        )}
      </div>

      {event.weather && (
        <>
          <div className="weather-forecast-header">
            Forecast for <strong>{formatDate(event.starts_at)}</strong> at{" "}
            <strong>{formatTime(event.starts_at)}</strong>
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
        onClick={() => navigate(`/chat/${event.id}`)}
        title="Go to chat"
      >
        💬
      </button>

      <ul className="participants-grid">
        {participants.map((p) => (
          <li
            key={p.id}
            className="participant-card"
            onClick={() => navigate(`/profile/${p.id}`)}
          >
            <div className="avatar-wrapper">
              <img
                src={avatars[p.id] || p.avatar_url || avatarDefault}
                alt={p.name}
                className="participant-avatar"
              />
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

            {/* Feedback dropdown */}
            {isDone && p.id !== me.id && (
              <div
                className={`feedback-dropdown ${feedbackSent[p.id] ? "sent" : ""
                  }`}
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
                onClick={e => {
                  e.stopPropagation();
                  setModal({ open: true, action: "kick", targetId: p.id });
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
