// src/Account/Account.tsx
import { useEffect, useState } from "react"
import { useNavigate } from "react-router-dom"
import {
  fetchMe,
  fetchAchievements,
  fetchXpLevel,
  fetchReputation,
  deleteMe,
  logout,
} from "../api/user"
import "./perfil.css"
import type { Achievement } from "../api/user"




function behaviourLabel(score: number): string {
  if (score >= 90) return "Excellent"
  if (score >= 75) return "Very Good"
  if (score >= 60) return "Good"
  if (score >= 40) return "Average"
  return "Poor"
}

export default function Account() {
  const [user, setUser] = useState<any>(null)
  const [achievements, setAch] = useState<Achievement[]>([])
  const [xp, setXp] = useState<number | null>(null)
  const [level, setLevel] = useState<number | null>(null)
  const [reputation, setReputation] = useState<{ score: number; badges: string[] } | null>(null)
  const nav = useNavigate()

  useEffect(() => {
    ; (async () => {
      const me = await fetchMe()
      setUser(me)

      const [ach, prof, rep] = await Promise.all([
        fetchAchievements(me.id),
        fetchXpLevel(me.id),
        fetchReputation(me.id),
      ])

      setAch(ach)
      setXp(prof.xp)
      setLevel(prof.level)
      setReputation(rep)
    })().catch(console.error)
  }, [])

  if (!user) return <p>A carregar…</p>


  const badges = reputation?.badges ?? []

  return (
    <section className="account-page">
      <h1>My Account</h1>

      <div className="account-grid">
        {/* ESQUERDA */}
        <div className="avatar-col">
          <img
            src={user.avatar_url ?? "/placeholder.png"}
            alt="Avatar"
            className="avatar"
          />
          <span className="level">Lvl {level ?? "—"}</span>
          {xp !== null && <small>{xp} XP</small>}
        </div>

        {/* DIREITA */}
        <div className="info-col">
          <div className="row"><strong>Name</strong> {user.name}</div>
          <div className="row"><strong>Email</strong> {user.email}</div>
          <div className="row"><strong>Location</strong> {user.location || "—"}</div>

          <div className="row">
            <strong>Favourite Sports</strong>{" "}
            {user.sports.map((s: any) => s.name).join(", ") || "—"}
          </div>

           <div className="row achievements">
            <strong>Achievement</strong>
            <div className="icons">
              {achievements.length > 0 ? (
                (() => {
                  const a = achievements[achievements.length - 1]  // pega o último desbloqueado
                  return (
                    <img
                      key={a.code}
                      src={a.icon}
                      alt={a.title}
                      title={a.description}   // só descrição no hover
                    />
                  )
                })()
              ) : (
                <span className="no-achievements">—</span>
              )}
            </div>
          </div>

          {/* Behaviour Index */}
          {reputation && (
            <div className="row">
              <strong>Behaviour Index</strong>{" "}
              {reputation.score}{" "}
              <span className="behaviour-label">
                ({behaviourLabel(reputation.score)})
              </span>
            </div>
          )}

          {/* Reputation */}
          <div className="row">
            <strong>Reputation</strong> {badges[0] || "—"}
          </div>

          {badges.length > 1 && (
            <div className="row">
              <strong>Badges</strong> {badges.join(", ")}
            </div>
          )}

          <div className="actions">
            <button onClick={() => nav("/change-password")}>
              Change Password
            </button>
            <button onClick={() => nav("/change-email")}>
              Change Email
            </button>
            <button className="danger" onClick={() => nav("/delete-account")}>
              Delete Account
            </button>
          </div>
        </div>
      </div>
    </section>
  )
}
