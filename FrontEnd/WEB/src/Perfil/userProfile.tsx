import { useParams, useNavigate } from "react-router-dom"
import { useEffect, useState } from "react"
import {
    fetchUser,
    fetchAchievements,
    fetchXpLevel,
    fetchReputation,
} from "../api/user"
import "./perfil.css"
import avatarDefault from "../assets/avatar-default.jpg"


export default function UserProfile() {
    const { id } = useParams<{ id: string }>()
    const [user, setUser] = useState<any>(null)
    const [ach, setAch] = useState<any[]>([])
    const [xp, setXp] = useState<number | null>(null)
    const [lvl, setLvl] = useState<number | null>(null)
    const [rep, setRep] = useState<{ score: number; badges: string[] } | null>(null)
    const nav = useNavigate()

    useEffect(() => {
        if (!id) return
            ; (async () => {
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

    if (!user) return <p>A carregar…</p>

    const badges = rep?.badges ?? []

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
                    <span className="level">Lvl {lvl ?? "—"}</span>
                    {xp !== null && <small>{xp} XP</small>}
                </div>

                {/* Info */}
                <div className="info-col">
                    <div className="row"><strong>Name</strong> {user.name}</div>
                    <div className="row"><strong>Location</strong> {user.location || "—"}</div>
                    <div className="row">
                        <strong>Favourite Sports</strong>{" "}
                        {user.sports.map((s: any) => s.name).join(", ") || "—"}
                    </div>

                    <div className="row achievements">
                        <strong>Achievements</strong>
                        <div className="icons">
                            {ach.map((a) => (
                                <img key={a.code} src={a.icon} title={a.title} />
                            ))}
                        </div>
                    </div>

                    {rep && (
                        <div className="row">
                            <strong>Behaviour Index</strong> {rep.score}
                        </div>
                    )}

                    <div className="row">
                        <strong>Reputation</strong> {badges[0] || "—"}
                    </div>
                </div>
            </div>

            <button style={{ marginTop: "2rem" }} onClick={() => nav(-1)}>← Back</button>
        </section>
    )
}
