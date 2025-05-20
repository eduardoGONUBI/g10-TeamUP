// src/main.tsx
import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import App from "./App";
import Dashboard from "./Dashboard";
import ResetPassword from "./ResetPassword";
import RequireAuth from "./RequireAuth";
import "./index.css";

ReactDOM.createRoot(document.getElementById("root")!).render(
  <BrowserRouter>
    <Routes>
      {/* login */}
      <Route path="/" element={<App />} />

      {/* reset password, expects ?token=…&email=… */}
      <Route path="/reset-password" element={<ResetPassword />} />

      {/* protected dashboard */}
      <Route
        path="/dashboard"
        element={
          <RequireAuth>
            <Dashboard />
          </RequireAuth>
        }
      />
    </Routes>
  </BrowserRouter>
);
