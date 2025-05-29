// src/RequireAuth.tsx
import React, { type JSX } from "react";
import { Navigate, useLocation } from "react-router-dom";

interface Props {
  children: JSX.Element;
}

/**
 * If there's no auth_token in either localStorage or sessionStorage,
 * redirect back to "/" (login). Otherwise render `children`.
 */
const RequireAuth = ({ children }: Props) => {
  const location = useLocation();
  const token =
    localStorage.getItem("auth_token") ||
    sessionStorage.getItem("auth_token");

  if (!token) {
    // Redirect to login, keeping track of where we were trying to go
    return <Navigate to="/" state={{ from: location }} replace />;
  }

  return children;
};

export default RequireAuth;
