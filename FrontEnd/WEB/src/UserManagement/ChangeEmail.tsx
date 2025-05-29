import React, { useState, type FormEvent, type ChangeEvent } from "react";
import { useNavigate } from "react-router-dom";
import { changeEmail } from "../api/user";
import logo from "../assets/logo.png";
import "../Perfil/perfil.css";

export default function ChangeEmailPage() {
  const navigate = useNavigate();
  const [email, setEmail]   = useState("");
  const [pwd, setPwd]       = useState("");
  const [msg, setMsg]       = useState<string | null>(null);
  const [err, setErr]       = useState<string | null>(null);

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setErr(null);
    setMsg(null);

    try {
      const json = await changeEmail(email, pwd);
      setMsg(json.message);
      setEmail(""); setPwd("");
    } catch (e: any) {
      setErr(e.message);
    }
  };

  return (
    <div className="container">
      <div className="form-panel">
        <h1>Alterar E-mail</h1>
        <p>Insere o novo e-mail e confirma com a tua password.</p>

        {msg && <div className="success">{msg}</div>}
        {err && <div className="error">{err}</div>}

        <form onSubmit={onSubmit}>
          <label>
            Novo e-mail
            <input
              type="email"
              value={email}
              onChange={(e: ChangeEvent<HTMLInputElement>) =>
                setEmail(e.target.value)
              }
              required
            />
          </label>

          <label>
            Password actual
            <input
              type="password"
              value={pwd}
              onChange={(e) => setPwd(e.target.value)}
              required
            />
          </label>

          <button type="submit">Alterar E-mail</button>
        </form>

        <button
          style={{ marginTop: "1rem" }}
          onClick={() => navigate("/account")}
        >
          Voltar ao Perfil
        </button>
      </div>

      <div className="logo-panel">
        <img src={logo} alt="TeamUP logo" className="logo" />
      </div>
    </div>
  );
}
