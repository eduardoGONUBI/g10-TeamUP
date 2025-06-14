import authFetch from "./event"  // import o helper
import type { Sport } from "./sports" // importa o desporto

// interface de um utilizador
export interface User {
  id: number
  name: string
  email?: string
  level: number
  behaviour_score: number
  behaviour_label: string
  avatar_url: string | null
  sports: Sport[]
  location?: string
  latitude?: number | null
  longitude?: number | null
}

// interface de um achievement
export interface Achievement {
  code: string
  title: string
  description: string
  icon: string
  unlocked_at: string
}

// interface da reputaçao
export interface Reputation {
  score: number
  badges: string[]
  good_teammate_count: number
  friendly_count: number
  team_player_count: number
  toxic_count: number
  bad_sport_count: number
  afk_count: number
}

// interface do update o utilizador
export interface UpdateMePayload {
  name?: string
  email?: string
  sports?: number[]
  location?: string
  latitude?: number
  longitude?: number
}


// vai buscar os dados do utilizador────────────────────────────────────────────────────────────────────────────────────────────────────
export async function fetchMe(): Promise<User> { 
  return authFetch("/api/auth/me")
}


// update o utilizador ───────────────────────────────────────────────────────────────────────────────────────────────────
export async function updateMe(
  payload: Partial<UpdateMePayload>
) {
  return authFetch("/api/auth/update", {
    method: "PUT",
    body: JSON.stringify(payload),
  })
}

// apagar conta ───────────────────────────────────────────────────────────────────────────────────────────────────
export async function deleteMe() {
  return authFetch("/api/auth/delete", { method: "DELETE" })
}

// logout ───────────────────────────────────────────────────────────────────────────────────────────────────
export async function logout() {
  return authFetch("/api/auth/logout", { method: "POST" })
}

 // buscar achievements de um utilizador ───────────────────────────────────────────────────────────────────────────────────────────────────
export async function fetchAchievements(id: number): Promise<Achievement[]> {
  const json = await authFetch(`/api/achievements/${id}`)
  return json.achievements as Achievement[]
}

// buscar o xp de um  utilizador ───────────────────────────────────────────────────────────────────────────────────────────────────
export async function fetchXpLevel(id: number) {
  return authFetch(`/api/profile/${id}`)
}

// buscar a reputaçao de um  utilizador ───────────────────────────────────────────────────────────────────────────────────────────────────
export async function fetchReputation(id: number): Promise<Reputation> {
  return authFetch(`/api/rating/${id}`) as Promise<Reputation>
}

// buscar dados publicos de um utilizador ───────────────────────────────────────────────────────────────────────────────────────────────────
export function fetchUser(id: number): Promise<User> {
  return authFetch(`/api/users/${id}`)
}

// mudar a passeword ───────────────────────────────────────────────────────────────────────────────────────────────────
export async function changePassword(
  current: string,
  next: string,
  nextConfirm: string
) {
  return authFetch("/api/auth/change-password", {
    method: "POST",
    body: JSON.stringify({
      current_password: current,
      new_password: next,
      new_password_confirmation: nextConfirm,
    }),
  })
}

// mudar email ───────────────────────────────────────────────────────────────────────────────────────────────────
export async function changeEmail(newEmail: string, password: string) { 
  return authFetch("/api/auth/change-email", {
    method: "POST",
    body: JSON.stringify({
      new_email: newEmail,
      password,
    }),
  })
}



// buscar a foto de perfil como um url ───────────────────────────────────────────────────────────────────────────────────────────────────
export async function fetchAvatar(id: number | string): Promise<string> {
  const res = await fetch(`/api/auth/avatar/${id}`, {
    credentials: "include",
    headers: { Accept: "image/*" },
  })
  if (!res.ok) throw new Error(`Avatar fetch failed: ${res.status}`)
  const blob = await res.blob()
  return URL.createObjectURL(blob)
}


 // muda a foto de perfil ───────────────────────────────────────────────────────────────────────────────────────────────────
export async function uploadAvatar(file: File): Promise<{ url: string }> {
  const token =
    localStorage.getItem("auth_token") ??
    sessionStorage.getItem("auth_token");

  if (!token) throw new Error("Not authenticated");

  const form = new FormData();
  form.append("avatar", file);       

  const res = await fetch("/api/auth/avatar", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`, 
      Accept: "application/json",      
    },
    body: form,
  });

  if (!res.ok) {
    let details = "";
    try {
      const j = await res.json();
      details = j.error || j.message || "";
    } catch {
      details = await res.text().catch(() => "");
    }
    throw new Error(`Avatar upload failed: ${res.status} – ${details}`);
  }

  return res.json() as Promise<{ url: string }>;
}


