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
  description: string
  icon: string
  unlocked_at: string
}

export interface Reputation {
  score: number;
  badges: string[];
  good_teammate_count: number;
  friendly_count: number;
  team_player_count: number;
  toxic_count: number;
  bad_sport_count: number;
  afk_count: number;
}

export async function fetchMe(): Promise<User> {
  return authFetch("/api/auth/me")
}

export async function updateMe(payload: Partial<{ name: string; email: string; sports: number[] }>) {
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
  return json.achievements as Achievement[]
}
export async function fetchXpLevel(id: number) {
  return authFetch(`/api/profile/${id}`)
}
export async function fetchReputation(id: number): Promise<Reputation> {
  return authFetch(`/api/rating/${id}`) as Promise<Reputation>;
}

export function fetchUser(id: number): Promise<User> {
  return authFetch(`/api/users/${id}`)
}
export async function changePassword(current: string, next: string, nextConfirm: string) {
  return authFetch('/api/auth/change-password', {
    method: 'POST',
    body: JSON.stringify({
      current_password: current,
      new_password: next,
      new_password_confirmation: nextConfirm,
    }),
  });
}

export async function changeEmail(newEmail: string, password: string) {
  return authFetch('/api/auth/change-email', {
    method: 'POST',
    body: JSON.stringify({
      new_email: newEmail,
      password,
    }),
  });
}