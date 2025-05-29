// src/components/Topbar.tsx
import React, { useState, useEffect, useRef } from "react";
import "./Topbar.css";
import avatarDefault from "../assets/avatar-default.jpg";
import { useNavigate } from "react-router-dom";
import { logout as apiLogout } from "../api/user";

interface NotificationItem {
  id: string;
  title: string;
  subtitle: string;
}

interface TopbarProps {
  username: string;
  avatarUrl?: string | null;
  notifications: NotificationItem[];
  notifOpen: boolean;
  bellGlow: boolean;
  onBellClick: () => void;
  onClearNotifications: () => void;
}

const BRAND = "#0d47ff";

const Topbar: React.FC<TopbarProps> = ({
  username,
  avatarUrl,
  notifications,
  notifOpen,
  bellGlow,
  onBellClick,
  onClearNotifications,
}) => {
  const [avatarOpen, setAvatarOpen] = useState(false);
  const avatarRef = useRef<HTMLDivElement>(null);
  const notifRef = useRef<HTMLDivElement>(null);
  const bellRef = useRef<SVGSVGElement>(null);
  const navigate = useNavigate();

  // Fecha dropdowns ao clicar fora
  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      const target = e.target as Node;
      // fecha avatar
      if (
        avatarOpen &&
        avatarRef.current &&
        !avatarRef.current.contains(target)
      ) {
        setAvatarOpen(false);
      }
      // fecha notificações
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
    return () =>
      document.removeEventListener("mousedown", handleClickOutside);
  }, [avatarOpen, notifOpen, onBellClick]);

  // Abre/fecha Avatar, fecha Notificações se estiverem abertas
  const handleAvatarToggle = () => {
    if (notifOpen) onBellClick();
    setAvatarOpen((o) => !o);
  };

  // Abre/fecha Notificações, fecha Avatar se estiver aberto
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

  return (
    <header className="topbar">
      <div className="profile" ref={avatarRef}>
        <img
          src={avatarUrl ?? avatarDefault}
          alt="Avatar"
          className="topbar-avatar"
          onClick={handleAvatarToggle}
        />
        <span className="username" onClick={handleAvatarToggle}>
          {username}
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

        <svg
          ref={bellRef}
          onClick={handleBellToggle}
          className={`bell-icon ${bellGlow ? "glow" : ""}`}
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
                notifications.map((n) => (
                  <div className="notification-item" key={n.id}>
                    <strong>{n.title}</strong>
                    <p>{n.subtitle}</p>
                  </div>
                ))
              )}
            </div>
            <button className="clear-btn" onClick={onClearNotifications}>
              Clear
            </button>
          </div>
        )}
      </div>
    </header>
  );
};

export default Topbar;
