// src/Account/Account.tsx
import { useEffect, useState, useRef } from "react"
import { useNavigate } from "react-router-dom"
import {
  fetchMe,
  fetchAchievements,
  fetchXpLevel,
  fetchReputation,
  fetchAvatar,
  uploadAvatar,
} from "../api/user"
import "./perfil.css"
import type { Achievement, Reputation } from "../api/user"

function behaviourLabel(score: number): string {
  if (score >= 90) return "Excellent"
  if (score >= 75) return "Very Good"
  if (score >= 60) return "Good"
  if (score >= 40) return "Average"
  return "Poor"
}

export default function Account() {
  const [user, setUser] = useState<any>(null)
  const [avatarSrc, setAvatarSrc] = useState<string | null>(null)
  const [achievements, setAch] = useState<Achievement[]>([])
  const [xp, setXp] = useState<number | null>(null)
  const [level, setLevel] = useState<number | null>(null)
  const [reputation, setReputation] = useState<Reputation | null>(null)
  const [uploading, setUploading] = useState(false)
  const fileInput = useRef<HTMLInputElement | null>(null)
  const nav = useNavigate()

  // Fetch user + avatar + related stats on mount
  useEffect(() => {
    let isMounted = true
    ;(async () => {
      try {
        const me = await fetchMe()
        if (!isMounted) return
        setUser(me)

        // Avatar ------------------------------------------------------
        try {
          const url = await fetchAvatar(me.id)
          if (isMounted) setAvatarSrc(url)
        } catch (err) {
          console.error("Could not fetch avatar", err)
        }

        // Other profile data -----------------------------------------
        const [ach, prof, rep] = await Promise.all([
          fetchAchievements(me.id),
          fetchXpLevel(me.id),
          fetchReputation(me.id),
        ])
        if (!isMounted) return
        setAch(ach)
        setXp(prof.xp)
        setLevel(prof.level)
        setReputation(rep)
      } catch (e) {
        console.error(e)
      }
    })()

    // Clean-up: revoke ObjectURL & cancel state updates after unmount
    return () => {
      isMounted = false
      if (avatarSrc && avatarSrc.startsWith("blob:")) {
        URL.revokeObjectURL(avatarSrc)
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  if (!user) return <p>Loading…</p>

  return (
    <section className="account-page">
      <h1>My Account</h1>

      <div className="account-grid">
        {/* LEFT */}
        <div className="avatar-col">
          <img
            src={avatarSrc ?? "/placeholder.png"}
            alt="Avatar"
            className="avatar"
            onClick={() => fileInput.current?.click()}
            style={{ cursor: "pointer" }}
          />
          <input
            ref={fileInput}
            type="file"
            accept="image/*"
            style={{ display: "none" }}
            onChange={async (e) => {
              const file = e.target.files?.[0]
              if (!file) return
              setUploading(true)
              try {
                const { url: newUrl } = await uploadAvatar(file)
                if (avatarSrc && avatarSrc.startsWith("blob:")) {
                  URL.revokeObjectURL(avatarSrc)
                }
                setAvatarSrc(newUrl)
              } catch (err) {
                console.error(err)
                alert("Sorry, the avatar could not be updated.")
              } finally {
                setUploading(false)
                // Reset input value so the same file can be re-selected if needed
                if (fileInput.current) fileInput.current.value = ""
              }
            }}
          />
          {uploading && <small>Uploading…</small>}
          <span className="level">Lvl {level ?? 1}</span>
          {xp !== null && <small>{xp} XP</small>}
        </div>

        {/* RIGHT */}
        <div className="info-col">
          <div className="row">
            <strong>Name</strong> {user.name}
          </div>
          <div className="row">
            <strong>Email</strong> {user.email}
          </div>
          <div className="row">
            <strong>Location</strong> {user.location || "—"}
          </div>

          <div className="row">
            <strong>Favourite Sports</strong>{" "}
            {user.sports.map((s: any) => s.name).join(", ") || "—"}
          </div>

          <div className="row achievements">
            <strong>Achievement</strong>
            <div className="icons">
              {achievements.length > 0 ? (
                (() => {
                  const a = achievements[achievements.length - 1]
                  return (
                    <img
                      key={a.code}
                      src={a.icon}
                      alt={a.title}
                      title={a.description}
                    />
                  )
                })()
              ) : (
                <span className="no-achievements">—</span>
              )}
            </div>
          </div>

          {reputation && (
            <div className="row">
              <strong>Behaviour Index</strong> {reputation.score}{" "}
              <span className="behaviour-label">
                ({behaviourLabel(reputation.score)})
              </span>
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
                const top = counts.reduce(
                  (best, curr) => (curr[1] > best[1] ? curr : best),
                  ["—", 0] as [string, number]
                )
                return top[1] > 0 ? `${top[0]} (${top[1]})` : "—"
              })()}
            </div>
          )}

          <div className="actions">
            <button onClick={() => nav("/change-password")}>
              Change Password
            </button>
            <button onClick={() => nav("/change-email")}>Change Email</button>
            <button className="danger" onClick={() => nav("/delete-account")}>
              Delete Account
            </button>
          </div>
        </div>
      </div>
    </section>
  )
}
