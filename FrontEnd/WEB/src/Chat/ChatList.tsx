// src/ChatList.tsx
import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { fetchAllMyEvents, type Event } from "../api/event";
import "./ChatList.css";

const PER_PAGE = 5;

const ChatList: React.FC = () => {
  const [active,    setActive]    = useState<Event[]>([]);
  const [archived,  setArchived]  = useState<Event[]>([]);
  const [loading,   setLoading]   = useState(true);
  const [error,     setError]     = useState<string | null>(null);
  const [pageA,     setPageA]     = useState(1);
  const [pageC,     setPageC]     = useState(1);
  const nav = useNavigate();

  useEffect(() => {
    setLoading(true);
   fetchAllMyEvents()
  .then((events) => {
    const act = events
      .filter((e) => e.status === "in progress")
    const arch = events
      .filter((e) => e.status === "concluded")


    setActive(act);
    setArchived(arch);
  })
      .catch(() => setError("Não foi possível carregar os chats."))
      .finally(() => setLoading(false));
  }, []);

  const openChat = (id: number) => nav(`/chat/${id}`);

  if (loading) return <p className="loading">A carregar…</p>;
  if (error)   return <p className="error">{error}</p>;

  // calculate slices
  const totalA = active.length;
  const totalC = archived.length;
  const pagesA = Math.ceil(totalA / PER_PAGE);
  const pagesC = Math.ceil(totalC / PER_PAGE);
  const sliceA = active.slice((pageA - 1) * PER_PAGE, pageA * PER_PAGE);
  const sliceC = archived.slice((pageC - 1) * PER_PAGE, pageC * PER_PAGE);

  const Pager = ({
    page,
    pages,
    onPrev,
    onNext,
  }: {
    page: number;
    pages: number;
    onPrev: () => void;
    onNext: () => void;
  }) => (
    <div className="pager">
      <button onClick={onPrev} disabled={page <= 1}>‹ Prev</button>
      <span>{page} / {pages}</span>
      <button onClick={onNext} disabled={page >= pages}>Next ›</button>
    </div>
  );

  return (
    <section className="chat-list-page">
      <h2>Chat Management</h2>
      <div className="chat-columns">

        {/* ─────────────────── ACTIVE ─────────────────── */}
        <div className="column">
          <h3>Active Activities</h3>
          {totalA === 0 && <p>Nenhum chat activo.</p>}
          {sliceA.map((ev) => (
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
          {pagesA > 1 && (
            <Pager
              page={pageA}
              pages={pagesA}
              onPrev={() => setPageA((p) => Math.max(1, p - 1))}
              onNext={() => setPageA((p) => Math.min(pagesA, p + 1))}
            />
          )}
        </div>

        {/* ────────────────── ARCHIVED ────────────────── */}
        <div className="column">
          <h3>Concluded Activities</h3>
          {totalC === 0 && <p>Nenhum chat arquivado.</p>}
          {sliceC.map((ev) => (
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
          {pagesC > 1 && (
            <Pager
              page={pageC}
              pages={pagesC}
              onPrev={() => setPageC((p) => Math.max(1, p - 1))}
              onNext={() => setPageC((p) => Math.min(pagesC, p + 1))}
            />
          )}
        </div>

      </div>
    </section>
  );
};

export default ChatList;
