// src/api/event.ts

export interface Participant {
  id: number
  name: string
  rating: number | null
}

export interface Event {
  id: number
  name: string
  sport: string | null
  date: string
  place: string
  status: string
  max_participants: number
  creator: { id: number; name: string }
  participants: Participant[]
  latitude: number
  longitude: number
}

// shape of the form on the front-end
export interface NewEventData {
  name: string
  sport_id: number
  date: string
  place: string
  max_participants: number
  latitude: number
  longitude: number
}

async function authFetch(input: RequestInfo, init: RequestInit = {}) {
  const token =
    localStorage.getItem("auth_token") ??
    sessionStorage.getItem("auth_token")
  if (!token) throw new Error("Not authenticated")
  init.headers = {
    ...(init.headers as object),
    Authorization: `Bearer ${token}`,
    "Content-Type": "application/json",
  }
  const res = await fetch(input, init)
  const json = await res.json()
  if (!res.ok) throw new Error(json.error ?? json.message ?? res.statusText)
  return json
}

export async function fetchMyEvents(): Promise<Event[]> {
  return authFetch("/api/events/mine") as Promise<Event[]>
}

export async function createEvent(data: NewEventData): Promise<Event> {
  const json = await authFetch("/api/events", {
    method: "POST",
    body: JSON.stringify(data),
  })
  return json.event as Event
}

export default authFetch;
