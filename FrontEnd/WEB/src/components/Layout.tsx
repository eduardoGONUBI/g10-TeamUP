import React, { useState, useEffect } from "react";
import { Outlet, useNavigate } from "react-router-dom";
import Sidebar from "./Sidebar";
import Topbar from "./Topbar";
import { fetchMe, type User } from "../api/user";
import "./Layout.css";

const Layout: React.FC = () => {
  // ─── Estado do perfil autenticado ─────────────────────────
  // Guarda os dados do utilizador autenticado
  const [user, setUser] = useState<User | null>(null);
// chama a api
  useEffect(() => {
    fetchMe()
      .then(u => setUser(u))
      .catch(console.error);
  }, []);

  // ─── Estado de notificações ───────────────────────────────
  const [notifOpen, setNotifOpen] = useState(false);   // notificaçoes esta aberto
  const [bellGlow, setBellGlow]   = useState(false);  // sino a abanar
  const [notifications, setNotifs] = useState<any[]>([]);   // lista de notificaçoes

  const toggleBell = () => {     // desativa o abanar do sino e alterna o estado aberto ou fechado das notificaçoes
    setNotifOpen(o => !o);
    setBellGlow(false);
  };
  const clearNotifs = () => setNotifs([]);   // limpa notificaçoes

  // ─── Navegação ────────────────────────────────────────────
  const nav = useNavigate();
  const goHome = () => nav("/dashboard");

  return (
    <div className="layout">
      <Sidebar onLogoClick={goHome} />    

      <div className="content-area">
        <Topbar
          username={user?.name ?? "Guest"}
          avatarUrl={user?.avatar_url ?? undefined}
          notifications={notifications}
          notifOpen={notifOpen}
          bellGlow={bellGlow}
          onBellClick={toggleBell}
          onClearNotifications={clearNotifs}
          userId={user?.id ?? 0}
        />

        <main className="page-body">
          <Outlet />     {/*pagina dinamica*/}
        </main>
      </div>
    </div>
  );
};

export default Layout;
