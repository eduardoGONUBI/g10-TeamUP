import React from "react";
import "../Dashboard.css";
import avatarDefault from "../assets/avatar-default.jpg";

interface NotificationItem {
  id: string;
  title: string;
  subtitle: string;
}

interface TopbarProps {
  username: string;
  // agora aceita string | undefined | null
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
  const itemHeight = 80;
  const maxItemsVisible = 4;
  const dropdownMaxH = itemHeight * maxItemsVisible;

  return (
    <header className="topbar">
      <div className="search">
        <input type="text" placeholder="Search…" />
      </div>

      <div className="profile">
        <img
          src={avatarUrl ?? avatarDefault}
          alt="Avatar"
          className="topbar-avatar"
        />
        <span className="username">{username}</span>

        <svg
          onClick={onBellClick}
          className={`bell-icon ${bellGlow ? "glow" : ""}`}
          xmlns="http://www.w3.org/2000/svg"
          viewBox="0 0 24 24"
          fill={BRAND}
          style={{ cursor: "pointer", marginLeft: "0.5rem", width: 20, height: 20 }}
        >
          <path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9" />
          <path d="M13.73 21a2 2 0 0 1-3.46 0" />
        </svg>

        {notifOpen && (
          <div
            className="notifications-dropdown"
            style={
              notifications.length > maxItemsVisible
                ? { maxHeight: `${dropdownMaxH}px`, overflowY: "auto" }
                : undefined
            }
          >
            <h4>Notifications</h4>
            <div className="notifications-list">
              {notifications.length === 0 ? (
                <p style={{ padding: "1rem" }}>No new notifications</p>
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
