import React, { useState, useEffect } from "react";
import { Outlet, useNavigate } from "react-router-dom";
import Sidebar from "./Sidebar";
import Topbar from "./Topbar";
import { fetchMe, type User } from "../api/user";
import "./Layout.css";

const Layout: React.FC = () => {
  // ─── Estado do perfil autenticado ─────────────────────────
  const [user, setUser] = useState<User | null>(null);

  useEffect(() => {
    fetchMe()
      .then(u => setUser(u))
      .catch(console.error);
  }, []);

  // ─── Estado de notificações ───────────────────────────────
  const [notifOpen, setNotifOpen] = useState(false);
  const [bellGlow, setBellGlow]   = useState(false);
  const [notifications, setNotifs] = useState<any[]>([]);

  const toggleBell = () => {
    setNotifOpen(o => !o);
    setBellGlow(false);
  };
  const clearNotifs = () => setNotifs([]);

  // ─── Navegação ────────────────────────────────────────────
  const nav = useNavigate();
  const goHome = () => nav("/dashboard");

  return (
    <div className="layout">
      <Sidebar onLogoClick={goHome} />

      <div className="content-area">
        <Topbar
          username={user?.name ?? "guest"}
          // converte null em undefined para satisfazer a prop
          avatarUrl={user?.avatar_url ?? undefined}
          notifications={notifications}
          notifOpen={notifOpen}
          bellGlow={bellGlow}
          onBellClick={toggleBell}
          onClearNotifications={clearNotifs}
          userId={user?.id ?? 0}
        />

        <main className="page-body">
          <Outlet />
        </main>
      </div>
    </div>
  );
};

export default Layout;
