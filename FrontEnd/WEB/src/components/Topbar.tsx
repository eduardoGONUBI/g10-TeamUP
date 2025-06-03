// ─── src/Topbar.tsx ────────────────────────────────────────────────────────────
import React, { useState, useEffect, useRef } from "react";
import "./Topbar.css";
import avatarDefault from "../assets/avatar-default.jpg";
import { useNavigate } from "react-router-dom";

import {
  logout as apiLogout,
  fetchMe,
  fetchAvatar,
} from "../api/user";

interface NotificationItem {
  event_name: string;
  message: string;
  created_at: string; // ISO or "YYYY-MM-DD hh:mm:ss"
}

interface TopbarProps {
  username?: string;
  avatarUrl?: string | null;
  bellGlow: boolean;
  notifOpen: boolean;
  onBellClick: () => void;
}

const BRAND = "#0d47ff";

/** Return auth token no matter where the app stored it. */
const getAuthToken = () =>
  localStorage.getItem("auth_token") ?? sessionStorage.getItem("auth_token");

const Topbar: React.FC<TopbarProps> = ({
  username,
  avatarUrl,
  bellGlow,
  notifOpen,
  onBellClick,
}) => {
  // ─── Local state for user, avatar & notifications ─────────────────────────
  const [localName, setLocalName] = useState<string | null>(null);
  const [localAvatar, setLocalAvatar] = useState<string | null>(null);
  const [avatarOpen, setAvatarOpen] = useState(false);

  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [hasUnread, setHasUnread] = useState(false);

  const avatarRef = useRef<HTMLDivElement>(null);
  const notifRef = useRef<HTMLDivElement>(null);
  const bellRef = useRef<SVGSVGElement>(null);
  const wsRef = useRef<WebSocket | null>(null);

  const navigate = useNavigate();

  // ─── 1) Get user + avatar ONCE on mount ────────────────────────────────────
  useEffect(() => {
    let mounted = true;

    (async () => {
      try {
        const me = await fetchMe();
        if (!mounted) return;
        setLocalName(me.name);

        // grab avatar blob, turn into ObjectURL
        try {
          const url = await fetchAvatar(me.id);
          if (mounted) setLocalAvatar(url);
        } catch (err) {
          console.error("Avatar fetch failed:", err);
        }
      } catch (err) {
        console.error("Auth fetch failed:", err);
      }
    })();

    return () => {
      mounted = false;
      if (localAvatar?.startsWith("blob:")) {
        URL.revokeObjectURL(localAvatar);
      }
    };
  }, []); // run only once

  // ─── 2) Open a WebSocket for realtime notifications ─────────────────────────
  useEffect(() => {
    const token = getAuthToken();
    if (!token) return;

    const ws = new WebSocket(`ws://localhost:55333/?token=${token}`);
    wsRef.current = ws;

    ws.onopen = () => {
      console.log("[Topbar] WS connected");
    };

    ws.onmessage = (evt) => {
      try {
        const incoming = JSON.parse(evt.data) as {
          type: string;
          event_id: number;
          event_name: string;
          user_id: number;
          user_name: string;
          message: string;
          timestamp: string;
        };

        // Build a NotificationItem and prepend:
        const newNotif: NotificationItem = {
          event_name: incoming.event_name,
          message: incoming.message,
          created_at: incoming.timestamp,
        };
        setNotifications((prev) => [newNotif, ...prev]);
        setHasUnread(true);
      } catch (err) {
        console.error("[Topbar] Failed to parse WS message:", err);
      }
    };

    ws.onerror = (err) => {
      console.error("[Topbar] WS error:", err);
    };

    ws.onclose = (e) => {
      console.log(
        `[Topbar] WS closed. code=${e.code} reason=${e.reason || "<none>"}`
      );
    };

    return () => {
      if (ws.readyState === WebSocket.OPEN) {
        ws.close();
      } else if (ws.readyState === WebSocket.CONNECTING) {
        ws.addEventListener("open", () => ws.close());
      }
    };
  }, []); // run once

  // ─── 3) When notifOpen toggles → mark as read on open, clear on close ──────
  useEffect(() => {
    if (notifOpen) {
      // If the bell just opened, mark those as “read” (remove glow)
      setHasUnread(false);
    } else {
      // If the bell just closed, clear all fetched notifications
      setNotifications([]);
    }
  }, [notifOpen]);

  // ─── 4) Outside-click / toggles / logout ───────────────────────────────────
  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      const target = e.target as Node;

      if (
        avatarOpen &&
        avatarRef.current &&
        !avatarRef.current.contains(target)
      ) {
        setAvatarOpen(false);
      }

      if (
        notifOpen &&
        notifRef.current &&
        !notifRef.current.contains(target) &&
        bellRef.current &&
        !bellRef.current.contains(target)
      ) {
        onBellClick();
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [avatarOpen, notifOpen, onBellClick]);

  const handleAvatarToggle = () => {
    if (notifOpen) onBellClick();
    setAvatarOpen((o) => !o);
  };
  const handleBellToggle = () => {
    if (avatarOpen) setAvatarOpen(false);
    onBellClick();
  };
  const handleLogout = async () => {
    try {
      await apiLogout();
    } catch {}
    localStorage.removeItem("auth_token");
    sessionStorage.removeItem("auth_token");
    navigate("/", { replace: true });
  };

  // ─── 5) Render ───────────────────────────────────────────────────────────────
  const finalAvatar = localAvatar ?? avatarUrl ?? avatarDefault;
  const finalName = username ?? localName ?? "…";

  return (
    <header className="topbar">
      <div className="profile" ref={avatarRef}>
        <img
          src={finalAvatar}
          alt="Avatar"
          className="topbar-avatar"
          onClick={handleAvatarToggle}
        />
        <span className="username" onClick={handleAvatarToggle}>
          {finalName}
        </span>

        {avatarOpen && (
          <div className="avatar-dropdown">
            <button
              onClick={() => {
                setAvatarOpen(false);
                navigate("/account");
              }}
            >
              Perfil
            </button>
            <button className="danger" onClick={handleLogout}>
              Logout
            </button>
          </div>
        )}

        {/* 🔔 bell & notifications */}
        <svg
          ref={bellRef}
          onClick={handleBellToggle}
          className={`bell-icon ${hasUnread ? "glow" : ""}`}
          xmlns="http://www.w3.org/2000/svg"
          viewBox="0 0 24 24"
          fill={BRAND}
        >
          <path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9" />
          <path d="M13.73 21a2 2 0 0 1-3.46 0" />
        </svg>

        {notifOpen && (
          <div className="notifications-dropdown" ref={notifRef}>
            <h4>Notifications</h4>
            <div className="notifications-list">
              {notifications.length === 0 ? (
                <p style={{ padding: "1rem", color: "#333" }}>
                  No new notifications
                </p>
              ) : (
                notifications.map((n, idx) => {
                  const key = `${n.event_name}-${n.created_at}-${idx}`;
                  return (
                    <div className="notification-item" key={key}>
                      <strong>{n.event_name}</strong>
                      <p>{n.message}</p>
                      <span className="created-at">
                        {new Date(n.created_at).toLocaleString()}
                      </span>
                    </div>
                  );
                })
              )}
            </div>
          </div>
        )}
      </div>
    </header>
  );
};

export default Topbar;
