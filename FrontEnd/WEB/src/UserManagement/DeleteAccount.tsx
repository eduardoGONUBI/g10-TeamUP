import React, { useState, type FormEvent, type ChangeEvent } from "react";
import { useNavigate } from "react-router-dom";
import authFetch from "../api/event";  
import logo from "../assets/logo.png";
import "./Login.css";

export default function DeleteAccountPage() {
  const navigate = useNavigate();
  const [password, setPassword] = useState("");
  const [err, setErr] = useState<string | null>(null);

  // formulario
  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setErr(null);
    try {   // pedido api para apagar a conta
      await authFetch("/api/auth/delete", {
        method: "DELETE",
        body: JSON.stringify({ password }),
      });

      // Limpar tokens
      localStorage.removeItem("auth_token");
      sessionStorage.removeItem("auth_token");

      // Redirecionar para login
      navigate("/", { replace: true });
    } catch (e: any) {
      setErr(e.message);
    }
  };

  return (
    <div className="container perfil-form-layout">
      <div className="form-panel">
        <h1>Delete Account</h1>
        <p>To confirm, enter your password and click <strong>Delete Account.</strong></p>

        {err && <div className="error">{err}</div>}

        <form onSubmit={onSubmit}>
          <label>
            Current Password
            <input
              type="password"
              value={password}
              onChange={(e: ChangeEvent<HTMLInputElement>) => setPassword(e.target.value)}
              required
            />
          </label>

          <button type="submit" className="danger">
            Delete Account
          </button>
        </form>

        <button style={{ marginTop: "1rem" }} onClick={() => navigate("/account")}>
          Back to Profile
        </button>
      </div>
      <div className="logo-panel">
        <img src={logo} alt="TeamUP logo" className="logo" />
      </div>
    </div>
  );
}
