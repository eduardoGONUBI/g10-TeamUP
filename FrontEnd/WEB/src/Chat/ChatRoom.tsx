// ─── src/Chat/ChatRoom.tsx ────────────────────────────────────────────────────
import React, { useEffect, useState, useRef } from "react";
import { useParams, useNavigate } from "react-router-dom";
import "./ChatRoom.css";

interface ChatMessage {
  id?: number;
  event_id: number;
  user_id: number | string;
  user_name: string;
  message: string;
  timestamp: string;
}

interface Participant {
  id: number;
  name: string;
  rating: number | null;
}

interface EventDTO {
  id: number;
  name: string;
  sport: string;
  date: string;
  place: string;
  status: string;
  max_participants: number;
  latitude: string;
  longitude: string;
  creator: { id: number; name: string };
  weather: {
    app_max_temp: number;
    app_min_temp: number;
    temp: number;
    high_temp: number;
    low_temp: number;
    description: string;
  };
  participants: Participant[];
}

const getAuthToken = (): string | null =>
  localStorage.getItem("auth_token") || sessionStorage.getItem("auth_token");

const getUserId = (): number | null => {
  const token = getAuthToken();
  if (!token) return null;
  try {
    const payload = JSON.parse(atob(token.split(".")[1]));
    return Number(payload.sub);
  } catch {
    return null;
  }
};

const ChatRoom: React.FC = () => {
  const { id } = useParams();
  const eventId = Number(id);
  const nav = useNavigate();

  const [eventName, setEventName] = useState<string>("");
  const [participants, setParticipants] = useState<Participant[]>([]);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const wsRef = useRef<WebSocket | null>(null);
  const containerRef = useRef<HTMLDivElement | null>(null);
  const token = getAuthToken();
  const myId = getUserId();

  // ── Redirect if not authenticated ─────────────────────────────────────────
  useEffect(() => {
    if (!token) {
      setError("Acesso não autorizado.");
      nav("/", { replace: true });
    }
  }, [token, nav]);

  // ── Fetch event name & participants ────────────────────────────────────────
  useEffect(() => {
    if (!token) return;

    fetch("http://localhost:8081/api/events", {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then(async (res) => {
        if (res.status === 401) throw new Error("Não autorizado.");
        return res.json();
      })
      .then((data: EventDTO[]) => {
        const ev = data.find((e) => e.id === eventId);
        if (ev) {
          setEventName(ev.name);
          setParticipants(ev.participants);
        } else {
          setEventName(`Evento ${eventId}`);
          setParticipants([]);
        }
      })
      .catch((err) => {
        console.error("Fetch events error:", err);
        setEventName(`Evento ${eventId}`);
        setParticipants([]);
      });
  }, [eventId, token]);

  // ── Fetch past messages ─────────────────────────────────────────────────────
  useEffect(() => {
    if (!token) return;

    setLoading(true);
    fetch(`http://localhost:8082/api/fetchMessages/${eventId}`, {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then(async (res) => {
        if (res.status === 401) throw new Error("Não autorizado.");
        return res.json();
      })
      .then((data) => {
        setMessages(data.messages || []);
      })
      .catch((err) => {
        console.error("Fetch messages error:", err);
        setError("Não foi possível carregar as mensagens.");
      })
      .finally(() => setLoading(false));
  }, [eventId, token]);

  // ── Auto-scroll to bottom whenever messages change ───────────────────────────
  useEffect(() => {
    if (containerRef.current) {
      containerRef.current.scrollTop = containerRef.current.scrollHeight;
    }
  }, [messages]);

  // ── WebSocket for real-time updates ───────────────────────────────────────────
  useEffect(() => {
    if (!token) return;

    const port = import.meta.env.VITE_WS_PORT || "55333";
    const ws = new WebSocket(`ws://localhost:${port}/?token=${token}`);
    wsRef.current = ws;

    ws.onmessage = ({ data }) => {
      let msg: ChatMessage;
      try {
        msg = JSON.parse(data);
      } catch {
        return;
      }
      if (msg.event_id === eventId) {
        setMessages((prev) => [...prev, msg]);
      }
    };

    ws.onerror = (e) => console.error("WebSocket error:", e);
    ws.onclose = () => console.log("WebSocket closed");

    return () => ws.close();
  }, [eventId, token]);

  // ── Send a new message ───────────────────────────────────────────────────────
  const sendMessage = async () => {
    if (!input.trim() || !token) return;

    try {
      const res = await fetch(
        `http://localhost:8082/api/sendMessage/${eventId}`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify({ message: input }),
        }
      );

      if (res.status === 401) {
        setError("Sessão expirada.");
        nav("/", { replace: true });
        return;
      }

      setInput("");
      // The new message will arrive via WebSocket
    } catch (err) {
      console.error("Send message error:", err);
      setError("Erro ao enviar a mensagem.");
    }
  };

  // ── Render Loading / Error ───────────────────────────────────────────────────
  if (loading) return <p className="loading">Loading your messages...</p>;
  if (error) return <p className="error">{error}</p>;

  return (
    <section className="chat-room-container">
      <aside className="participants-list">
        <h3>Participants</h3>
        {participants.length === 0 ? (
          <p className="no-participants">Nobody.</p>
        ) : (
          <ul>
            {participants.map((p) => (
              <li key={p.id} className="participant-item">
                {p.name}
              </li>
            ))}
          </ul>
        )}
      </aside>

      <div className="chat-area">
        <h2 className="chat-header">{eventName}</h2>

        <div ref={containerRef} className="chat-history">
          {messages.map((msg, i) => {
            // If it’s "User joined the event", only show it if first for that user
            if (msg.message === "User joined the event") {
              const firstIndex = messages.findIndex(
                (m) => Number(m.user_id) === Number(msg.user_id)
              );
              if (firstIndex === i) {
                return (
                  <div key={i} className="system-message">
                    {`${msg.user_name} joined the event`}
                  </div>
                );
              }
              return null;
            }

            // Otherwise, align left/right based on user_id
            const isMe = myId !== null && Number(msg.user_id) === myId;
            return (
              <div
                key={i}
                className={isMe ? "chat-bubble me" : "chat-bubble other"}
              >
                <span className="bubble-sender">{msg.user_name}</span>
                <div className="bubble-text">{msg.message}</div>
              </div>
            );
          })}
        </div>

        <div className="chat-input">
          <input
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && sendMessage()}
            placeholder="Type your message..."
          />
          <button className="send-btn" onClick={sendMessage}>
            Enviar
          </button>
        </div>
      </div>
    </section>
  );
};

export default ChatRoom;
