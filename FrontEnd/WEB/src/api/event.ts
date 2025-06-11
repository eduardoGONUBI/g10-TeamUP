// src/api/event.ts

export interface Participant {
  id: number
  name: string
  rating: number | null
  avatar_url?: string | null;
}

export interface Event {
  id: number
  name: string
  sport: string | null
  starts_at: string 
  place: string
  status: string
  max_participants: number
  creator: { id: number; name: string }
  user_id: number;   
  participants: Participant[]
  latitude: number
  longitude: number
  weather?: Weather;
}

// shape of the form on the front-end
export interface NewEventData {
  name: string
  sport_id: number
  starts_at: string
  place: string
  max_participants: number

}

export interface Weather {
  temp: number;
  high_temp: number;
  low_temp: number;
  description: string;
}
export interface Me {
  id: number;
  name: string;
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

export async function fetchAllMyEvents(
  perPage = 100   // podes subir para 200/500 se o backend permitir
): Promise<Event[]> {
  // 1ª página
  const first = await authFetch(`/api/events?per_page=${perPage}&page=1`);
  if (!Array.isArray(first.data)) {
    throw new Error("Resposta inesperada: falta 'data'");
  }
  let events: Event[] = first.data;

  // se houver mais páginas, vai buscá-las em paralelo
  const { last_page } = first.meta ?? { last_page: 1 };
  if (last_page > 1) {
    const rest = await Promise.all(
      Array.from({ length: last_page - 1 }, (_, i) =>
        authFetch(`/api/events?per_page=${perPage}&page=${i + 2}`)
      )
    );
    rest.forEach(page => (events = events.concat(page.data)));
  }

  return events;
}

export async function createEvent(data: NewEventData): Promise<Event> {
  const json = await authFetch("/api/events", {
    method: "POST",
    body: JSON.stringify(data),
  })
  return json.event as Event
}

/** PUT /api/events/{id} — update */
export async function updateEvent(
  id: string,
  data: {
    name: string;
    sport_id: number;
    starts_at: string;          // “YYYY-MM-DD HH:mm:00”
    place: string;
    max_participants: number;
    latitude: number;
    longitude: number;
  }
) {
  return authFetch(`/api/events/${id}`, {
    method: "PUT",
    body: JSON.stringify(data),
  }) as Promise<{ message: string; event: Event }>;
}


export default authFetch;
