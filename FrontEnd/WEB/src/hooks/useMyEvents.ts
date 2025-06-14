import { useQuery } from "@tanstack/react-query";
import { fetchAllMyEvents } from "../api/event";

export function useMyEvents(perPage = 100) {
  return useQuery({
    queryKey: ["myEvents", perPage],
    queryFn: () => fetchAllMyEvents(perPage),
    staleTime: 1000 * 60 * 2, // mantem-se por 2 minutos
  });
}
