// ─── src/main.tsx ──────────────────────────────────────────────────────────────
import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter, Routes, Route } from "react-router-dom";

import App from "./UserManagement/Login";                         // login
import ResetPassword from "./UserManagement/ResetPassword";     // recuperação de password
import Dashboard from "./Dashboard";             // dashboard 

import RequireAuth from "./UserManagement/RequireAuth";         // protecção
import Layout from "./components/Layout"; // sidebar + topbar + <Outlet />
import MyActivities from "./Events/ActivitiesList";
import EventDetails from "./Events/EventDetails";
import ChatList from "./Chat/ChatList";
import CreateEvent from "./Events/CreateEvent";
import Account from "./Perfil/Perfil"
import UserProfile from "./Perfil/userProfile";

import ChangePassword from "./UserManagement/ChangePassword";
import ChangeEmail from "./UserManagement/ChangeEmail";
import DeleteAccountPage from "./UserManagement/DeleteAccount";

import "./main.css";

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <BrowserRouter>
      <Routes>
        {/* públicas ----------------------------------------------------------- */}
        <Route path="/" element={<App />} />
        <Route path="/reset-password" element={<ResetPassword />} />

        {/* privadas (RequireAuth ⇒ Layout ⇒ Outlet) --------------------------- */}
        <Route
          element={
            <RequireAuth>
              <Layout />
            </RequireAuth>
          }
        >
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/my-activities" element={<MyActivities />} />
          <Route path="/events/:id" element={<EventDetails />} />
          <Route path="/chat" element={<ChatList />} />
          <Route path="/events/create" element={<CreateEvent />} />
          <Route path="/account" element={<Account />} />
          <Route path="/profile/:id" element={<UserProfile />} />
          <Route path="/change-password" element={<ChangePassword />} />
          +         <Route path="/change-email" element={<ChangeEmail />} />
          <Route path="/delete-account" element={<DeleteAccountPage />} />

        </Route>

        {/* fallback 404 -------------------------------------------------------- */}
        <Route path="*" element={<p>Not Found</p>} />
      </Routes>
    </BrowserRouter>
  </React.StrictMode>
);
