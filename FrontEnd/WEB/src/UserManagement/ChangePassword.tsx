import React, { useState, type FormEvent, type ChangeEvent } from "react";
import { useNavigate } from "react-router-dom";
import { changePassword } from "../api/user";
import logo from "../assets/logo.png";
import "../Perfil/perfil.css";

export default function ChangePasswordPage() {
  const navigate = useNavigate();
  const [current, setCurrent]     = useState("");
  const [next, setNext]           = useState("");
  const [confirm, setConfirm]     = useState("");
  const [msg, setMsg]             = useState<string | null>(null);
  const [err, setErr]             = useState<string | null>(null);

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setErr(null);
    setMsg(null);

    if (next !== confirm) {
      setErr("The new password does not match.");
      return;
    }

    try {
      const json = await changePassword(current, next, confirm);
      setMsg(json.message);
      setCurrent(""); setNext(""); setConfirm("");
    } catch (e: any) {
      setErr(e.message);
    }
  };

  return (
   <div className="container perfil-form-layout">
      <div className="form-panel">
        <h1>Change Password</h1>
        <p>Enter your current password and set a new one."</p>

        {msg && <div className="success">{msg}</div>}
        {err && <div className="error">{err}</div>}

        <form onSubmit={onSubmit}>
          <label>
            Current Password
            <input
              type="password"
              value={current}
              onChange={(e: ChangeEvent<HTMLInputElement>) =>
                setCurrent(e.target.value)
              }
              required
            />
          </label>

          <label>
            New password
            <input
              type="password"
              value={next}
              onChange={(e) => setNext(e.target.value)}
              required
            />
          </label>

          <label>
            Confirm new password
            <input
              type="password"
              value={confirm}
              onChange={(e) => setConfirm(e.target.value)}
              required
            />
          </label>

          <button type="submit">Change Password</button>
        </form>

        <button
          style={{ marginTop: "1rem" }}
          onClick={() => navigate("/account")}
        >
          Back to Profile
        </button>
      </div>

      <div className="logo-panel">
        <img src={logo} alt="TeamUP logo" className="logo" />
      </div>
    </div>
  );
}
