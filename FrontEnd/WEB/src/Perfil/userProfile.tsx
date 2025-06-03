import { useParams, useNavigate } from "react-router-dom"
import { useEffect, useState } from "react"
import {
  fetchUser,
  fetchAchievements,
  fetchXpLevel,
  fetchReputation,
  fetchAvatar,
  type Achievement,
  type Reputation,
} from "../api/user"
import "./perfil.css"
import avatarDefault from "../assets/avatar-default.jpg"

export default function UserProfile() {
  const { id } = useParams<{ id: string }>()
  const [user, setUser] = useState<any>(null)
  const [achievements, setAchievements] = useState<Achievement[]>([])
  const [xp, setXp] = useState<number | null>(null)
  const [level, setLevel] = useState<number | null>(null)
  const [reputation, setReputation] = useState<Reputation | null>(null)
  const [avatarSrc, setAvatarSrc] = useState<string | null>(null)
  const nav = useNavigate()

  // ────────────────────────────────────────────────────────────────
  // Fetch profile data + avatar
  // ────────────────────────────────────────────────────────────────
  useEffect(() => {
    if (!id) return

    let isMounted = true
    let objectUrl: string | null = null

    ;(async () => {
      try {
        const [u, a, p, r] = await Promise.all([
          fetchUser(+id),
          fetchAchievements(+id),
          fetchXpLevel(+id),
          fetchReputation(+id),
        ])
        if (!isMounted) return

        setUser(u)
        setAchievements(a)
        setXp(p.xp)
        setLevel(p.level)
        setReputation(r)

        // avatar as blob URL
        try {
          objectUrl = await fetchAvatar(+id)
          if (isMounted) setAvatarSrc(objectUrl)
        } catch (err) {
          console.error("Avatar fetch failed", err)
        }
      } catch (err) {
        console.error(err)
      }
    })()

    return () => {
      isMounted = false
      if (objectUrl && objectUrl.startsWith("blob:")) {
        URL.revokeObjectURL(objectUrl)
      }
    }
  }, [id])

  if (!user) return <p>Loading…</p>

  const latest =
    achievements.length > 0 ? achievements[achievements.length - 1] : null

  return (
    <section className="account-page">
      <h1>See Profile</h1>

      <div className="account-grid">
        {/* Avatar + XP */}
        <div className="avatar-col">
          <img
            src={avatarSrc ?? user.avatar_url ?? avatarDefault}
            alt="Avatar"
            className="avatar"
          />
          <span className="level">Lvl {level ?? 1}</span>
          {xp !== null && <small>{xp} XP</small>}
        </div>

        {/* Info */}
        <div className="info-col">
          <div className="row">
            <strong>Name</strong> {user.name}
          </div>
          <div className="row">
            <strong>Location</strong> {user.location || "—"}
          </div>
          <div className="row">
            <strong>Favourite Sports</strong>{" "}
            {user.sports.map((s: any) => s.name).join(", ") || "—"}
          </div>

          <div className="row achievements">
            <strong>Achievements</strong>
            <div className="icons">
              {latest ? (
                <img
                  key={latest.code}
                  src={latest.icon}
                  alt={latest.title}
                  title={latest.description}
                />
              ) : (
                <span className="no-achievements">—</span>
              )}
            </div>
          </div>

          {reputation && (
            <div className="row">
              <strong>Behaviour Index</strong> {reputation.score}
            </div>
          )}

          {reputation && (
            <div className="row">
              <strong>Reputation</strong>{" "}
              {(() => {
                const counts: [string, number][] = [
                  ["Good teammate", reputation.good_teammate_count],
                  ["Friendly player", reputation.friendly_count],
                  ["Team player", reputation.team_player_count],
                  ["Watchlisted", reputation.toxic_count],
                  ["Bad sport", reputation.bad_sport_count],
                  ["Frequent AFK", reputation.afk_count],
                ]
                const [label, count] = counts.reduce(
                  (best, curr) => (curr[1] > best[1] ? curr : best),
                  ["—", 0] as [string, number]
                )
                return count > 0 ? `${label} (${count})` : "—"
              })()}
            </div>
          )}
        </div>
      </div>

      <button style={{ marginTop: "2rem" }} onClick={() => nav(-1)}>
        ← Back
      </button>
    </section>
  )
}