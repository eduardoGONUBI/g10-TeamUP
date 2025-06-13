
// interface de um desporto
export interface Sport {
  id: number;
  name: string;
}

import authFetch from "./event"; // importa o helper

// vai buscar a api a lista de desportos
export async function fetchSports(): Promise<Sport[]> {
  return authFetch("/api/sports") as Promise<Sport[]>;
}
