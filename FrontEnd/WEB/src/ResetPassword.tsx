// src/ResetPassword.tsx
import React, { useState, type FormEvent, type ChangeEvent, useEffect } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
import logo from "./assets/logo.png";
import "./App.css";

const ResetPassword = () => {
  const [search] = useSearchParams();
  const navigate = useNavigate();

  // grab token & email from ?token=…&email=…
  const tokenParam = search.get("token") || "";
  const emailParam = search.get("email") || "";

  const [token] = useState(tokenParam);
  const [email] = useState(emailParam);
  const [password, setPassword] = useState("");
  const [passwordConfirmation, setPasswordConfirmation] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  // if someone lands here without token/email, bounce them back
  useEffect(() => {
    if (!token || !email) {
      navigate("/", { replace: true });
    }
  }, [token, email, navigate]);

  const onSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);

    if (password !== passwordConfirmation) {
      setError("Passwords do not match");
      return;
    }

    try {
      const res = await fetch("http://127.0.0.1:80/api/password/reset", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          token,
          email,
          password,
          password_confirmation: passwordConfirmation
        }),
      });

      const json = await res.json();
      if (!res.ok) {
        throw new Error(json.message || "Reset failed");
      }

      setSuccess("Password reset! Redirecting to login…");
      setTimeout(() => navigate("/", { replace: true }), 2000);
    } catch (err: any) {
      setError(err.message);
    }
  };

  return (
    <div className="container">
      <div className="form-panel">
        <h1>Reset Password</h1>
        <p>Enter your new password for <strong>{email}</strong>.</p>

        <form onSubmit={onSubmit}>
          {error && (
            <div style={{ color: "red", marginBottom: "1rem" }}>
              {error}
            </div>
          )}
          {success && (
            <div style={{ color: "green", marginBottom: "1rem" }}>
              {success}
            </div>
          )}

          <label>
            New Password
            <input
              type="password"
              value={password}
              onChange={(e: ChangeEvent<HTMLInputElement>) =>
                setPassword(e.target.value)
              }
              required
            />
          </label>

          <label>
            Confirm Password
            <input
              type="password"
              value={passwordConfirmation}
              onChange={(e: ChangeEvent<HTMLInputElement>) =>
                setPasswordConfirmation(e.target.value)
              }
              required
            />
          </label>

          <button type="submit">Reset Password</button>
        </form>
      </div>

      <div className="logo-panel">
        <img src={logo} alt="TeamUP logo" className="logo" />
      </div>
    </div>
  );
};

export default ResetPassword;
