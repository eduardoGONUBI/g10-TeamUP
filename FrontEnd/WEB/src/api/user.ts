// src/api/user.ts
import authFetch from "./event"            // voltar a usar a função authFetch definida
                                         
export interface Sport { id: number; name: string }
export interface User {
  id: number
  name: string
  email?: string
  level: number
  behaviour_score: number
  behaviour_label: string
  avatar_url: string | null
  sports: Sport[]
}

export interface Achievement {
  code: string
  title: string
  icon: string
  unlocked_at: string
}

export interface Reputation {
  score: number
  badges: string[]
}

export async function fetchMe(): Promise<User> {
  return authFetch("/api/auth/me")
}

export async function updateMe(payload: Partial<{name: string; email: string; sports: number[]}>) {
  return authFetch("/api/auth/update", {
    method: "PUT",
    body: JSON.stringify(payload),
  })
}

export async function deleteMe() {
  return authFetch("/api/auth/delete", { method: "DELETE" })
}

export async function logout() {
  return authFetch("/api/auth/logout", { method: "POST" })
}

export async function fetchAchievements(id: number): Promise<Achievement[]> {
  const json = await authFetch(`/api/achievements/${id}`)
  return json.achievements
}
export async function fetchXpLevel(id: number) {
  return authFetch(`/api/profile/${id}`)
}
export async function fetchReputation(id: number): Promise<Reputation> {
 
  return authFetch(`/api/rating/${id}`)       
}

export function fetchUser(id: number): Promise<User> {
  return authFetch(`/api/users/${id}`)      
}