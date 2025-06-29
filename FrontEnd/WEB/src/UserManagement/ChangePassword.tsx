import React, {
  useState,
  type FormEvent,
  type ChangeEvent,
  useEffect,
} from "react";
import { useNavigate } from "react-router-dom";
import { changePassword } from "../api/user";
import logo from "../assets/logo.png";
import "../Perfil/perfil.css";

export default function ChangePasswordPage() {
  const navigate = useNavigate();

  // -----campos de input ---------
  const [current, setCurrent] = useState("");
  const [next, setNext] = useState("");
  const [confirm, setConfirm] = useState("");

    // ------- mensagens de feedback ----------------------
  const [msg, setMsg] = useState<string | null>(null);
  const [err, setErr] = useState<string | null>(null);

 // ------validaçoes -----------------
  const [pwErr, setPwErr] = useState<string | null>(null);           
  const [mismatchErr, setMismatchErr] = useState<string | null>(null); 
  const [isPwValid, setIsPwValid] = useState(false);
//regex da password
  const pwRegex = /^(?=.*[A-Z])(?=.*\d).{8,}$/;

  // valida a password em tempo real
  useEffect(() => {
    if (!next) {
      setPwErr(null);
      setIsPwValid(false);
      return;
    }

    if (!pwRegex.test(next)) {
      setPwErr(
        "Password must be at least 8 characters long and include one uppercase letter and one number."
      );
      setIsPwValid(false);
    } else {
      setPwErr(null);
      setIsPwValid(true);
    }
  }, [next]);

  // Valida se a password ta igual
  useEffect(() => {
    if (!confirm) {
      setMismatchErr(null);
      return;
    }

    if (next !== confirm) {
      setMismatchErr("The new passwords do not match.");
    } else {
      setMismatchErr(null);
    }
  }, [next, confirm]);
// --------------- envio de formulario ----------
  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setErr(null);
    setMsg(null);

    // validaçao extra. nao deve chegar aqui
    if (!isPwValid) {
      setErr("Please choose a stronger password.");
      return;
    }

    if (mismatchErr) {
      setErr(mismatchErr);
      return;
    }

    try {    // chama a api
      const json = await changePassword(current, next, confirm);
      setMsg(json.message);
      setCurrent("");
      setNext("");
      setConfirm("");
    } catch (e: any) {
      setErr(e.message || "Unknown error");
    }
  };

  return (
    <div className="container perfil-form-layout">
      <div className="form-panel">
        <h1>Change Password</h1>
        <p>Enter your current password and set a new one.</p>

        {msg && (
          <div style={{ color: "green", marginBottom: "1rem" }}>{msg}</div>
        )}
        {err && (
          <div style={{ color: "red", marginBottom: "1rem" }}>{err}</div>
        )}
        {pwErr && (
          <div style={{ color: "red", marginBottom: "1rem" }}>{pwErr}</div>
        )}
        {mismatchErr && (
          <div style={{ color: "red", marginBottom: "1rem" }}>{mismatchErr}</div>
        )}

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

          <button
            type="submit"
            disabled={!isPwValid || !!mismatchErr}
          >
            Change Password
          </button>
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