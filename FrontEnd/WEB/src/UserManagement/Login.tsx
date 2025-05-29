// src/App.tsx
import React, {
  useState,
  type FormEvent,
  type ChangeEvent,
  useEffect,
} from "react";
import { useNavigate } from "react-router-dom";
import logo from "../assets/logo.png";
import "./Login.css";

const App: React.FC = () => {
 // Limpa tokens antigos ao montar o componente
    useEffect(() => {
    localStorage.removeItem("auth_token");
    sessionStorage.removeItem("auth_token");
  }, []);

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [remember, setRemember] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const navigate = useNavigate();

  const onSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      const res = await fetch("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password, remember }),
      });

      const json = await res.json();
      if (!res.ok) throw new Error(json.message || "Login failed");

      const storage = remember ? localStorage : sessionStorage;
      storage.setItem("auth_token", json.access_token);

      // immediately navigate to dashboard
      navigate("/dashboard", { replace: true });
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleForgotPassword = async (
    e: React.MouseEvent<HTMLAnchorElement>
  ) => {
    e.preventDefault();
    if (!email) {
      alert("Please enter your email address above first.");
      return;
    }

    try {
      const res = await fetch("/api/password/email", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email }),
      });
      const json = await res.json();
      if (!res.ok) {
        throw new Error(json.message || "Failed to send reset link");
      }
      alert(json.message || "Reset link sent—check your inbox!");
    } catch (err: any) {
      alert(err.message);
    }
  };

  return (
    <div className="container">
      <div className="form-panel">
        <h1>TeamUP: Creator Management</h1>
        <p>Welcome back! Please login to your account.</p>

        <form onSubmit={onSubmit}>
          {error && (
            <div style={{ color: "red", marginBottom: "1rem" }}>
              {error}
            </div>
          )}

          <label>
            Email Address
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
            Password
            <input
              type="password"
              value={password}
              onChange={(e: ChangeEvent<HTMLInputElement>) =>
                setPassword(e.target.value)
              }
              required
            />
          </label>

          <div className="options">
            <label>
              <input
                type="checkbox"
                checked={remember}
                onChange={(e: ChangeEvent<HTMLInputElement>) =>
                  setRemember(e.target.checked)
                }
              />
              Remember Me
            </label>
            <a href="#" onClick={handleForgotPassword}>
              Forgot Password?
            </a>
          </div>

          <button type="submit" disabled={loading}>
            {loading ? "Logging in…" : "Login"}
          </button>
        </form>
      </div>

      <div className="logo-panel">
        <img src={logo} alt="TeamUP logo" className="logo" />
      </div>
    </div>
  );
};

export default App;
