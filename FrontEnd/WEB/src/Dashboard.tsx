// src/Dashboard.tsx
import React, { useState, useEffect, useCallback, useRef } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip as BarTooltip,
  RadialBarChart,
  RadialBar,
  Legend,
  AreaChart,
  Area,
  Tooltip as AreaTooltip,
} from "recharts";
import "./Dashboard.css";
import logo from "./assets/up.png";

interface Stats {
  user_id: string;
  total_active_activities: number;
  created_this_week: number;
  created_this_week_last_year: number;
  participants_joined_this_month: number;
  participants_left_this_month: number;
  participants_joined_last_month: number;
  top_locations: { place: string; total: number }[];
  top_sports: { sport_id: number; sport_name: string; total: number }[];
}

interface NotificationPayload {
  type: string;
  event_id: number;
  event_name: string;
  user_id: number;       // initiator
  user_name: string;     // initiator name
  message: string;
  timestamp: string;
}

interface NotificationItem {
  id: string;
  title: string;
  subtitle: string;
}

const BRAND = "#0d47ff";
const ACCENT = "#6f9bff";
const WARNING = "#ff6b6b";

const Dashboard: React.FC = () => {
  const [stats, setStats] = useState<Stats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notifOpen, setNotifOpen] = useState(false);
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [bellGlow, setBellGlow] = useState(false);

  const wsRef = useRef<WebSocket | null>(null);
  const navigate = useNavigate();
  const location = useLocation();

  // Extract user_id from JWT in storage
  const getUserId = (): number | null => {
    const token =
      localStorage.getItem("auth_token") ||
      sessionStorage.getItem("auth_token");
    if (!token) return null;
    try {
      const payload = JSON.parse(atob(token.split(".")[1]));
      return Number(payload.sub);
    } catch {
      return null;
    }
  };

  // WebSocket connection
  useEffect(() => {
    const token =
      localStorage.getItem("auth_token") ||
      sessionStorage.getItem("auth_token");
    if (!token) return;

    const port = import.meta.env.VITE_WS_PORT || "55333";
    const ws = new WebSocket(`ws://localhost:${port}/?token=${token}`);
    wsRef.current = ws;

    ws.onopen = () => console.log("🟢 WS connected");
    ws.onclose = () => console.log("⚪ WS disconnected");
    ws.onerror = (e) => console.error("🔴 WS error", e);

    ws.onmessage = ({ data }) => {
      let msg: NotificationPayload;
      try {
        msg = JSON.parse(data);
      } catch {
        console.warn("Invalid WS payload:", data);
        return;
      }

      // Only process if addressed to this user (server filters but extra safety)
      const myId = getUserId();
      if (myId == null) return;

      // Create display item
      const item: NotificationItem = {
        id: `${msg.timestamp}-${msg.event_id}`,
        title: `${msg.user_name} → ${msg.event_name}`,
        subtitle: `${msg.message} • ${new Date(msg.timestamp).toLocaleString()}`,
      };

      setNotifications((prev) => [item, ...prev]);

      // trigger bell glow
      setBellGlow(true);
      setTimeout(() => setBellGlow(false), 3000);
    };

    return () => {
      ws.close();
    };
  }, []);

  // Fetch dashboard stats
  const fetchStats = useCallback(() => {
    setLoading(true);
    setError(null);
    const token =
      localStorage.getItem("auth_token") ||
      sessionStorage.getItem("auth_token");
    if (!token) {
      setError("Not authenticated");
      setLoading(false);
      return;
    }
    fetch("http://127.0.0.1:8081/api/stats", {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then(async (res) => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return (await res.json()) as Stats;
      })
      .then(setStats)
      .catch((err) => {
        console.error(err);
        setError(err.message);
      })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    fetchStats();
  }, [fetchStats]);

  const handleBellClick = () => setNotifOpen((open) => !open);
  const handleLogoClick = () => {
    if (location.pathname === "/dashboard") fetchStats();
    else navigate("/dashboard");
  };

  // Dropdown sizing
  const itemHeight = 80;
  const maxItemsVisible = 4;
  const dropdownMaxH = itemHeight * maxItemsVisible;

  // Chart data
  const weeklyBars = [
    { name: "This Week", value: stats?.created_this_week ?? 0 },
    { name: "Same Week ’24", value: stats?.created_this_week_last_year ?? 0 },
  ];
  const radialData = [
    { name: "Joined", value: stats?.participants_joined_this_month ?? 0, fill: BRAND },
    { name: "Left", value: stats?.participants_left_this_month ?? 0, fill: WARNING },
  ];
  const locationBars = (stats?.top_locations ?? [])
    .sort((a, b) => b.total - a.total)
    .map((l) => ({ name: l.place, total: l.total }));
  const joinTrend = [
    {
      period: "01",
      thisMonth: stats ? stats.participants_joined_this_month * 0.25 : 0,
      lastMonth: stats ? stats.participants_joined_last_month * 0.2 : 0,
    },
    {
      period: "02",
      thisMonth: stats ? stats.participants_joined_this_month * 0.5 : 0,
      lastMonth: stats ? stats.participants_joined_last_month * 0.4 : 0,
    },
    {
      period: "03",
      thisMonth: stats ? stats.participants_joined_this_month * 0.75 : 0,
      lastMonth: stats ? stats.participants_joined_last_month * 0.6 : 0,
    },
    {
      period: "04",
      thisMonth: stats?.participants_joined_this_month ?? 0,
      lastMonth: stats?.participants_joined_last_month ?? 0,
    },
  ];

  return (
    <div className="app-container">
      {/* Sidebar */}
      <aside className="sidebar">
        <div className="sidebar-logo" onClick={handleLogoClick} style={{ cursor: "pointer" }}>
          <img src={logo} alt="TeamUP Logo" className="logo-icon" />
          TeamUP
        </div>
        <nav className="sidebar-nav">
          <ul>
            <li className="active">Dashboard</li>
            <li>My Activities</li>
            <li>Notifications</li>
            <li>Chat</li>
            <li>Account</li>
          </ul>
        </nav>
      </aside>

      {/* Main */}
      <div className="main">
        <header className="topbar">
          <div className="search">
            <input type="text" placeholder="Search…" />
          </div>
          <div className="profile">
            <div className="avatar">S</div>
            <span className="username">sad3</span>
            <svg
              onClick={handleBellClick}
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
                <button className="clear-btn" onClick={() => setNotifications([])}>
                  Clear
                </button>
              </div>
            )}
          </div>
        </header>

        <main className="dashboard-container">
          {/* Total Activities */}
          <div className="card">
            <h3>Total Activities</h3>
            <p className="big">{loading ? "…" : stats?.total_active_activities ?? 0}</p>
          </div>

          {/* Created This Week */}
          <div className="chart-card">
            <h3>Created This Week</h3>
            {loading ? (
              <div className="chart-loading">Loading chart…</div>
            ) : (
              <ResponsiveContainer width="100%" height={180}>
                <BarChart data={weeklyBars} barCategoryGap={25}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} />
                  <XAxis dataKey="name" />
                  <YAxis allowDecimals={false} />
                  <BarTooltip />
                  <Bar
                    dataKey="value"
                    radius={[8, 8, 0, 0]}
                    label={{ position: "top", fill: BRAND }}
                    fill={BRAND}
                  />
                </BarChart>
              </ResponsiveContainer>
            )}
          </div>

          {/* Participants This Month */}
          <div className="chart-card">
            <h3>Participants This Month</h3>
            {loading ? (
              <div className="chart-loading">Loading chart…</div>
            ) : (
              <ResponsiveContainer width="100%" height={180}>
                <RadialBarChart
                  innerRadius="45%"
                  outerRadius="80%"
                  data={radialData}
                  startAngle={180}
                  endAngle={-180}
                >
                  <RadialBar background dataKey="value" cornerRadius={6} />
                  <Legend iconSize={12} layout="vertical" verticalAlign="middle" align="right" />
                  <BarTooltip />
                </RadialBarChart>
              </ResponsiveContainer>
            )}
          </div>

          {/* Top Locations */}
          <div className="chart-card">
            <h3>Top Locations</h3>
            {loading ? (
              <div className="chart-loading">Loading chart…</div>
            ) : (
              <ResponsiveContainer width="100%" height={220}>
                <BarChart
                  layout="vertical"
                  data={locationBars}
                  margin={{ top: 5, right: 20, bottom: 5, left: 20 }}
                >
                  <CartesianGrid strokeDasharray="3 3" horizontal={false} />
                  <XAxis type="number" allowDecimals={false} />
                  <YAxis dataKey="name" type="category" width={80} />
                  <BarTooltip />
                  <Bar
                    dataKey="total"
                    fill={ACCENT}
                    label={{ position: "right", fill: BRAND }}
                  />
                </BarChart>
              </ResponsiveContainer>
            )}
          </div>

          {/* Top Sports */}
          <div className="card list-card">
            <h3>Top Sports</h3>
            {loading ? (
              <p>Loading…</p>
            ) : (
              <ul>
                {stats!.top_sports.map((s) => (
                  <li key={s.sport_id}>
                    {s.sport_name} — {s.total}
                  </li>
                ))}
              </ul>
            )}
          </div>

          {/* Monthly Joins Trend */}
          <div className="chart-card">
            <h3>Monthly Joins Trend</h3>
            {loading ? (
              <div className="chart-loading">Loading chart…</div>
            ) : (
              <ResponsiveContainer width="100%" height={180}>
                <AreaChart data={joinTrend}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="period" />
                  <YAxis allowDecimals={false} />
                  <AreaTooltip />
                  <Area
                    type="monotone"
                    dataKey="thisMonth"
                    stroke={BRAND}
                    fill="none"
                    name="This Month"
                    dot={{ r: 3 }}
                    activeDot={{ r: 5 }}
                  />
                  <Area
                    type="monotone"
                    dataKey="lastMonth"
                    stroke="#d3d3d3"
                    fill="none"
                    name="Last Month"
                    strokeDasharray="5 5"
                    dot={{ r: 3 }}
                    activeDot={{ r: 5 }}
                  />
                </AreaChart>
              </ResponsiveContainer>
            )}
          </div>
        </main>
      </div>
    </div>
  );
};

export default Dashboard;
