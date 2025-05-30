// src/Account/UserProfile.tsx
import { useParams, useNavigate } from "react-router-dom"
import { useEffect, useState } from "react"
import {
    fetchUser,
    fetchAchievements,
    fetchXpLevel,
    fetchReputation,
    type Achievement,
    type Reputation,
} from "../api/user"
import "./perfil.css"
import avatarDefault from "../assets/avatar-default.jpg"

export default function UserProfile() {
    const { id } = useParams<{ id: string }>()
    const [user, setUser] = useState<any>(null)
    const [ach, setAch] = useState<Achievement[]>([])
    const [xp, setXp] = useState<number | null>(null)
    const [lvl, setLvl] = useState<number | null>(null)
    const [rep, setRep] = useState<Reputation | null>(null)
    const nav = useNavigate()

    useEffect(() => {
        if (!id) return
        ;(async () => {
            const [u, a, p, r] = await Promise.all([
                fetchUser(+id),
                fetchAchievements(+id),
                fetchXpLevel(+id),
                fetchReputation(+id),
            ])
            setUser(u)
            setAch(a)
            setXp(p.xp)
            setLvl(p.level)
            setRep(r)
        })().catch(console.error)
    }, [id])

    if (!user) return <p>Loading…</p>

    const latest = ach.length > 0 ? ach[ach.length - 1] : null

    return (
        <section className="account-page">
            <h1>See Profile</h1>

            <div className="account-grid">
                {/* Avatar + XP */}
                <div className="avatar-col">
                    <img
                        src={user.avatar_url ?? avatarDefault}
                        alt="Avatar"
                        className="avatar"
                    />
                    <span className="level">Lvl {lvl ?? 1}</span>
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

                    {rep && (
                        <div className="row">
                            <strong>Behaviour Index</strong> {rep.score}
                        </div>
                    )}

                    {rep && (
                        <div className="row">
                            <strong>Reputation</strong>{" "}
                            {(() => {
                                const counts: [string, number][] = [
                                    ["Good teammate", rep.good_teammate_count],
                                    ["Friendly player", rep.friendly_count],
                                    ["Team player", rep.team_player_count],
                                    ["Watchlisted", rep.toxic_count],
                                    ["Bad sport", rep.bad_sport_count],
                                    ["Frequent AFK", rep.afk_count],
                                ]
                                const [name, cnt] = counts.reduce(
                                    (best, curr) => (curr[1] > best[1] ? curr : best),
                                    ["—", 0] as [string, number]
                                )
                                return cnt > 0 ? `${name} (${cnt})` : "—"
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
