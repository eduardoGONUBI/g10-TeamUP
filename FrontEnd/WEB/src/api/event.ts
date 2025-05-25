export interface Participant {
  id: number;
  name: string;
  rating: number | null;
}
export interface Event {
  id: number;
  name: string;
  sport: string | null;
  date: string;
  place: string;
  status: string;
  max_participants: number;
  creator: { id: number; name: string };
  participants: Participant[];
}

export async function fetchMyEvents(): Promise<Event[]> {
  const token =
    localStorage.getItem("auth_token") ?? sessionStorage.getItem("auth_token");
  if (!token) throw new Error("No auth token");

  const res = await fetch("/api/events/mine", {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) throw new Error("Failed to fetch events");

  return res.json();
}
