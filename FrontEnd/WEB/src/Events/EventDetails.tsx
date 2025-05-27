import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import "./EventDetails.css";
import avatarDefault from "../assets/avatar-default.jpg";

interface Participant {
  id: number;
  name: string;
  rating: number | null;
  avatar_url?: string | null;
}

interface Event {
  id: number;
  name: string;
  sport: string | null;
  date: string;
  place: string;
  max_participants: number;
  latitude?: number;
  longitude?: number;
  // retiramos o participants daqui pois vamos buscá-los à parte
}

const EventDetails: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [event, setEvent] = useState<Event | null>(null);
  const [participants, setParticipants] = useState<Participant[]>([]);
  const [loading, setLoading] = useState(true);
  const nav = useNavigate();
  const token = localStorage.getItem("auth_token") || sessionStorage.getItem("auth_token");

  // 1) buscar detalhes do evento
  useEffect(() => {
    if (!id || !token) return;
    fetch(`/api/events/search?id=${id}`, {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((r) => r.json())
      .then((arr) => setEvent(arr[0] ?? null))
      .catch(console.error);
  }, [id, token]);

  // 2) buscar lista de participantes
  useEffect(() => {
    if (!id || !token) return;
    fetch(`/api/events/${id}/participants`, {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((r) => r.json())
      .then((data) => {
        // o endpoint devolve { event_id, event_name, creator, participants: [...] }
        setParticipants(data.participants ?? []);
      })
      .catch(console.error)
      .finally(() => setLoading(false));
  }, [id, token]);

  if (loading) return <p className="loading">Loading…</p>;
  if (!event) return <p>Evento não encontrado.</p>;

  const start = new Date(event.date);
  const dateStr = start.toLocaleDateString();
  const timeStr = start.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });

  return (
    <section className="event-page">
      {/* cabeçalho */}
      <header className="event-header">
        <h2>{event.name} <span role="img" aria-label="sport">⚽</span></h2>
        <p>
          {participants.length}/{event.max_participants} participantes,&nbsp;
          {dateStr} {timeStr}
        </p>
      </header>

      {/* mapa */}
      <div className="map-wrapper">
        {event.latitude != null && event.longitude != null ? (
          <iframe
            title="map"
            loading="lazy"
            width="100%"
            height="300"
            style={{ border: 0, borderRadius: 8, boxShadow: "0 2px 8px rgba(0,0,0,.2)" }}
            src={`https://www.google.com/maps/embed/v1/place?key=${import.meta.env.VITE_GOOGLE_MAPS_API_KEY
              }&q=${event.latitude},${event.longitude}&zoom=15&maptype=roadmap`}
            allowFullScreen
          />
        ) : (
          <p>Mapa indisponível</p>
        )}
      </div>

      {/* botão flutuante para chat */}
      <button
        className="chat-fab"
        onClick={() => nav(`/chat/${event.id}`)}
        title="Ir para o chat"
      >
        💬
      </button>

      {/* grelha de participantes */}
      <ul className="participants-grid">
        {participants.map((p) => (
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
