import React, { type JSX } from "react";
import { Navigate, useLocation } from "react-router-dom";

interface Props {
  children: JSX.Element;
}


const RequireAuth = ({ children }: Props) => {
  const location = useLocation();
  const token =                                 // verifica se o token existe
    localStorage.getItem("auth_token") ||
    sessionStorage.getItem("auth_token");

  if (!token) {
    // se nao existe vai para login e guarda o local onde o utilizador estava
    return <Navigate to="/" state={{ from: location }} replace />;
  }

  return children;
};

export default RequireAuth;
