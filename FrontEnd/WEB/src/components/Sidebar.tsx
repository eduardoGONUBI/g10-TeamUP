// src/components/Layout/Sidebar.tsx
import React from "react";
import { useNavigate, useLocation } from "react-router-dom";
import logo from "../assets/up.png";
import "../Dashboard.css";

interface SidebarProps {
  onLogoClick: () => void;
}

const Sidebar: React.FC<SidebarProps> = ({ onLogoClick }) => {
  const location = useLocation();

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
          <li className={location.pathname === "/dashboard" ? "active" : ""}>
            Dashboard
          </li>
          <li>My Activities</li>
          <li>Notifications</li>
          <li>Chat</li>
          <li>Account</li>
        </ul>
      </nav>
    </aside>
  );
};

export default Sidebar;
