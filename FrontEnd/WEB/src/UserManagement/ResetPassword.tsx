import React, { useState, type FormEvent, type ChangeEvent, useEffect } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
import logo from "../assets/logo.png";
import "../UserManagement/Login";

const ResetPassword = () => {
  const [search] = useSearchParams();
  const navigate = useNavigate();

  //  pena to token e mail
  const tokenParam = search.get("token") || "";
  const emailParam = search.get("email") || "";

  // estados iniciais
  const [token] = useState(tokenParam);
  const [email] = useState(emailParam);
  const [password, setPassword] = useState("");
  const [passwordConfirmation, setPasswordConfirmation] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  // Validaçao de password
  const [passwordError, setPasswordError] = useState<string | null>(null);
  const [isPasswordValid, setIsPasswordValid] = useState(false);

  // regex da password
  const passwordRegex = /^(?=.*[A-Z])(?=.*\d).{8,}$/;

  // sem token volta ao login
  useEffect(() => {
    if (!token || !email) {
      navigate("/", { replace: true });
    }
  }, [token, email, navigate]);

  // ------------------------Valida a password-------------------------
  useEffect(() => {
    if (!password) {
      setPasswordError(null);
      setIsPasswordValid(false);
      return;
    }

    if (!passwordRegex.test(password)) {
      setPasswordError(
        "Password must be at least 8 characters long, contain one uppercase letter and one number."
      );
      setIsPasswordValid(false);
    } else {
      setPasswordError(null);
      setIsPasswordValid(true);
    }
  }, [password]);

  // -----------------funçao de submissao do formulario--------------------
  const onSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);

    if (!isPasswordValid) {
      setError("Please choose a stronger password.");
      return;
    }

    if (password !== passwordConfirmation) {
      setError("Passwords do not match");
      return;
    }
 // -------------envia pedido a api---------------
    try {   
      const res = await fetch("/api/password/reset", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          token,
          email,
          password,
          password_confirmation: passwordConfirmation,
        }),
      });

      const json = await res.json();
      if (!res.ok) {
        throw new Error(json.message || "Reset failed");
      }
        // se correr tudo bem mostra sucesso e vai para o login
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
          {passwordError && (
            <div style={{ color: "orange", marginBottom: "1rem" }}>
              {passwordError}
            </div>
          )}

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

          <button type="submit" disabled={!isPasswordValid || !passwordConfirmation}>
            Reset Password
          </button>
        </form>
      </div>

      <div className="logo-panel">
        <img src={logo} alt="TeamUP logo" className="logo" />
      </div>
    </div>
  );
};

export default ResetPassword;