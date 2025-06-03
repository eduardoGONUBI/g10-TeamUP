// src/components/Layout/Sidebar.tsx
import React from "react";
import { useNavigate, useLocation } from "react-router-dom";
import logo from "../assets/up.png";
import "./Sidebar.css";

interface SidebarProps {
  onLogoClick: () => void;
}

const Sidebar: React.FC<SidebarProps> = ({ onLogoClick }) => {
  const location = useLocation();
  const nav = useNavigate();

  return (
    <aside className="sidebar">
      <div
        className="sidebar-logo"
        onClick={onLogoClick}
        style={{ cursor: "pointer" }}
      >
        <img src={logo} alt="TeamUP Logo" className="logo-icon" />
        TeamUP
      </div>
      <nav className="sidebar-nav">
        <ul>
          <li
            className={location.pathname === "/dashboard" ? "active" : ""}
            onClick={() => nav("/dashboard")}
          >
            Dashboard
          </li>
          <li
            className={location.pathname === "/my-activities" ? "active" : ""}
            onClick={() => nav("/my-activities")}
          >
            My Activities
          </li>
          <li
            className={location.pathname === "/chat" ? "active" : ""}
            onClick={() => nav("/chat")}
          >
            Chat
          </li>
          <li
            className={location.pathname === "/account" ? "active" : ""}
            onClick={() => nav("/account")}
          >
            Account
          </li>
        </ul>
      </nav>
    </aside>
  );
};

export default Sidebar;
