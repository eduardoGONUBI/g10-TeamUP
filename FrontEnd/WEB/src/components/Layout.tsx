import React, { useState } from "react";
import { Outlet, useNavigate } from "react-router-dom";
import Sidebar from "./Sidebar";
import Topbar from "./Topbar";

import "../Dashboard.css";

const Layout: React.FC = () => {
  // estado da Topbar
  const [notifOpen, setNotifOpen]  = useState(false);
  const [bellGlow, setBellGlow]    = useState(false);
  const [notifications, setNotifs] = useState([]);
  const username = sessionStorage.getItem("username")
                ?? localStorage.getItem("username")
                ?? "guest";

  // callbacks da Topbar
  const toggleBell = () => {
    setNotifOpen(!notifOpen);
    setBellGlow(false);
  };
  const clearNotifs = () => setNotifs([]);

  // clique no logo volta ao dashboard
  const nav = useNavigate();
  const goHome = () => nav("/dashboard");

  return (
    <div className="layout">
      <Sidebar onLogoClick={goHome} />
      <div className="content-area">
        <Topbar
          username={username}
          notifications={notifications}
          notifOpen={notifOpen}
          bellGlow={bellGlow}
          onBellClick={toggleBell}
          onClearNotifications={clearNotifs}
        />
        <main className="page-body">
          <Outlet />
        </main>
      </div>
    </div>
  );
};

export default Layout;
