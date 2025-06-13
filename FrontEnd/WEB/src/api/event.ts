// interface de um participante
export interface Participant {
  id: number
  name: string
  rating: number | null
  avatar_url?: string | null;
}

//interface de uma atividade
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

// dados de criaçao de uma atividade
export interface NewEventData {
  name: string
  sport_id: number
  starts_at: string
  place: string
  max_participants: number

}
// interface da resposta do weather
export interface Weather {
  temp: number;
  high_temp: number;
  low_temp: number;
  description: string;
}
//interface simples do utilizador
export interface Me {
  id: number;
  name: string;
}

// funçao auxiliar para fazer fetch autenticado
async function authFetch(input: RequestInfo, init: RequestInit = {}) {
  const token =   // vai buscar o token
    localStorage.getItem("auth_token") ?? 
    sessionStorage.getItem("auth_token")


  if (!token) throw new Error("Not authenticated")    //erro no token
  init.headers = {  // Garante que os headers existem e adiciona o Authorization + JSON content-type
    ...(init.headers as object),
    Authorization: `Bearer ${token}`,
    "Content-Type": "application/json",
  }
  const res = await fetch(input, init)  // Faz a chamada HTTP com os parametros preparados
  const json = await res.json()   //converte a resposta para json
  if (!res.ok) throw new Error(json.error ?? json.message ?? res.statusText) 
  return json //se correr bem retorna o json
}

export async function fetchAllMyEvents(   // vai buscar todos as atividades
  perPage = 100   // limite de atividades por pagina
): Promise<Event[]> {

  const first = await authFetch(`/api/events?per_page=${perPage}&page=1`); // fetch a 1 pagina
  if (!Array.isArray(first.data)) {
    throw new Error("Resposta inesperada: falta 'data'");
  }
  let events: Event[] = first.data;   // eventos da primeira pagina

  // se houver mais páginas, vai buscá-las todas ao mesmo tempo
  const { last_page } = first.meta ?? { last_page: 1 };
  if (last_page > 1) {
    const rest = await Promise.all(
      Array.from({ length: last_page - 1 }, (_, i) =>
        authFetch(`/api/events?per_page=${perPage}&page=${i + 2}`)
      )
    );
    rest.forEach(page => (events = events.concat(page.data)));  // junta todas as atividades
  }

  return events;   //devolve todas as atividades
}

export async function createEvent(data: NewEventData): Promise<Event> {     // criar uma atividade
  const json = await authFetch("/api/events", {   //chama api com a funçao auxiliar
    method: "POST",
    body: JSON.stringify(data),    
  })
  return json.event as Event    // devolve uma atividade criada
}


export async function updateEvent(     //atualiza uma atividade
  id: string,
  data: {
    name: string;
    sport_id: number;
    starts_at: string;          // YYYY-MM-DD HH:mm:00
    place: string;
    max_participants: number;
    latitude: number;
    longitude: number;
  }
) {
  return authFetch(`/api/events/${id}`, {    //envia um PUT para o backend com os novos dados
    method: "PUT",
    body: JSON.stringify(data),
  }) as Promise<{ message: string; event: Event }>;   // espera a resposta como um objeto com messagem e o evento
}


export default authFetch;
