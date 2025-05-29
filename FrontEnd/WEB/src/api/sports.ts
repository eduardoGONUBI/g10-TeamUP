// src/api/sports.ts
export interface Sport {
  id: number;
  name: string;
}

import authFetch from "./event"; // your existing helper

/** returns something like [{ id: 1, name: "Soccer" }, …] */
export async function fetchSports(): Promise<Sport[]> {
  return authFetch("/api/sports") as Promise<Sport[]>;
}
