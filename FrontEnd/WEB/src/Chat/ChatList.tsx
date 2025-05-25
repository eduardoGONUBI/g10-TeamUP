import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { fetchMyEvents, type Event } from "../api/event";
import "./ChatList.css";

const ChatList: React.FC = () => {
  const [active, setActive]     = useState<Event[]>([]);
  const [archived, setArchived] = useState<Event[]>([]);
  const [loading, setLoading]   = useState(true);
  const [error, setError]       = useState<string | null>(null);
  const nav                     = useNavigate();

  useEffect(() => {
    setLoading(true);
    fetchMyEvents()
      .then((events) => {
        setActive(events.filter((e) => e.status === "in progress"));
        setArchived(events.filter((e) => e.status === "concluded"));
      })
      .catch((err) => {
        console.error(err);
        setError("Não foi possível carregar os chats.");
      })
      .finally(() => setLoading(false));
  }, []);

  const openChat = (id: number) => nav(`/chat/${id}`);

  if (loading) return <p className="loading">A carregar…</p>;
  if (error)   return <p className="error">{error}</p>;

  return (
    <section className="chat-list-page">
      <h2>Chat Management</h2>
      <div className="chat-columns">
        {/* ─────────────────── ACTIVE ─────────────────── */}
        <div className="column">
          <h3>Active Activities</h3>
          {active.length === 0 && <p>Nenhum chat activo.</p>}
          {active.map((ev) => (
            <div
              key={ev.id}
              className="chat-card"
              onClick={() => openChat(ev.id)}
            >
              <span className="icon">💬</span>
              <div className="info">
                <strong className="title">{ev.name}</strong>
                <span className="sport">{ev.sport ?? "—"}</span>
              </div>
              <button
                className="see-btn"
                onClick={(e) => { e.stopPropagation(); openChat(ev.id); }}
              >
                See Chat ›
              </button>
            </div>
          ))}
        </div>

        {/* ────────────────── ARCHIVED ────────────────── */}
        <div className="column">
          <h3>Concluded Activities</h3>
          {archived.length === 0 && <p>Nenhum chat arquivado.</p>}
          {archived.map((ev) => (
            <div
              key={ev.id}
              className="chat-card archived"
              onClick={() => openChat(ev.id)}
            >
              <span className="icon">💬</span>
              <div className="info">
                <strong className="title">{ev.name}</strong>
                <span className="sport">{ev.sport ?? "—"}</span>
              </div>
              <button
                className="see-btn"
                onClick={(e) => { e.stopPropagation(); openChat(ev.id); }}
              >
                See Chat ›
              </button>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
};

export default ChatList;
