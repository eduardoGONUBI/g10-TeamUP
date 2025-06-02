// src/api/user.ts
import authFetch from "./event" // custom wrapper that automatically sends credentials & parses JSON

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
  score: number
  badges: string[]
  good_teammate_count: number
  friendly_count: number
  team_player_count: number
  toxic_count: number
  bad_sport_count: number
  afk_count: number
}

// ────────────────────────────────────────────────────────────────────────────────
// BASIC PROFILE / AUTH
// ────────────────────────────────────────────────────────────────────────────────
export async function fetchMe(): Promise<User> {
  return authFetch("/api/auth/me")
}

export async function updateMe(
  payload: Partial<{ name: string; email: string; sports: number[] }>
) {
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

// ────────────────────────────────────────────────────────────────────────────────
// GAMIFICATION / SOCIAL DATA
// ────────────────────────────────────────────────────────────────────────────────
export async function fetchAchievements(id: number): Promise<Achievement[]> {
  const json = await authFetch(`/api/achievements/${id}`)
  return json.achievements as Achievement[]
}

export async function fetchXpLevel(id: number) {
  return authFetch(`/api/profile/${id}`)
}

export async function fetchReputation(id: number): Promise<Reputation> {
  return authFetch(`/api/rating/${id}`) as Promise<Reputation>
}

// ────────────────────────────────────────────────────────────────────────────────
// USER DIRECTORY / LOOK-UPS
// ────────────────────────────────────────────────────────────────────────────────
export function fetchUser(id: number): Promise<User> {
  return authFetch(`/api/users/${id}`)
}

// ────────────────────────────────────────────────────────────────────────────────
// ACCOUNT SETTINGS
// ────────────────────────────────────────────────────────────────────────────────
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

export async function changeEmail(newEmail: string, password: string) {
  return authFetch("/api/auth/change-email", {
    method: "POST",
    body: JSON.stringify({
      new_email: newEmail,
      password,
    }),
  })
}

// ────────────────────────────────────────────────────────────────────────────────
// AVATAR ENDPOINTS
// ────────────────────────────────────────────────────────────────────────────────
/**
 * Fetches the binary avatar image for the given user and returns an ObjectURL
 * that can be fed directly into an <img src="…" /> tag. Remember to call
 * `URL.revokeObjectURL(url)` when the image is no longer needed to avoid leaks.
 */
export async function fetchAvatar(id: number | string): Promise<string> {
  const res = await fetch(`/api/auth/avatar/${id}`, {
    credentials: "include",
    headers: { Accept: "image/*" },
  })
  if (!res.ok) throw new Error(`Avatar fetch failed: ${res.status}`)
  const blob = await res.blob()
  return URL.createObjectURL(blob)
}

/**
 * Uploads a new avatar for the currently authenticated user.
 * The backend expects multipart/form-data with field name "avatar".
 *
 * @param file The image file selected by the user.
 * @returns The JSON response from the backend (typically `{ url: string }`).
 */
// src/api/user.ts
export async function uploadAvatar(file: File): Promise<{ url: string }> {
  // ← identical to the helper in src/api/event.ts
  const token =
    localStorage.getItem("auth_token") ??
    sessionStorage.getItem("auth_token");

  if (!token) throw new Error("Not authenticated");

  const form = new FormData();
  form.append("avatar", file);        // backend field name

  const res = await fetch("/api/auth/avatar", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`, // ✅ bearer included
      Accept: "application/json",       // ✅ tell Laravel “API mode”
      // do NOT set Content-Type – fetch will add the boundary for you
    },
    body: form,
  });

  if (!res.ok) {
    // try to extract JSON error, else plain text
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