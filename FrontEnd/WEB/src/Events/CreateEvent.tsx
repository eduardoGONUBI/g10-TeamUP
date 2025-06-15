import React, {
  useState,
  useRef,
  useEffect,
  useMemo,
  type ChangeEvent,
  type FormEvent,
} from "react"
import { useNavigate } from "react-router-dom"
import { LoadScript, Autocomplete } from "@react-google-maps/api"
import type { LoadScriptProps } from "@react-google-maps/api"
import { createEvent, type NewEventData } from "../api/event"
import { fetchSports, type Sport } from "../api/sports"
import "./CreateEvent.css"

// google maps api
const LIBRARIES: LoadScriptProps["libraries"] = ["places"]

const CreateEvent: React.FC = () => {
   // Estado do formulario
  const [form, setForm] = useState<NewEventData>({
    name: "",
    sport_id: 0,
    starts_at: "",
    place: "",
    max_participants: 2,
  })

  // -------------------- estados ----------------------------
  const [sports, setSports] = useState<Sport[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const navigate = useNavigate()

  //  valida coordenadas
  const [latitude, setLatitude] = useState<number | null>(null)
  const [longitude, setLongitude] = useState<number | null>(null)

  // google maps api
  const autocompleteRef = useRef<google.maps.places.Autocomplete | null>(null)

//----------------- hangler generico -------------------------
  const handleChange =
    <K extends keyof NewEventData>(key: K) =>
    (
      e:
        | ChangeEvent<HTMLInputElement>
        | ChangeEvent<HTMLSelectElement>
    ) => {
      const val =
        e.target.type === "number"
          ? Number(e.target.value)
          : e.target.value
      setForm(f => ({ ...f, [key]: val } as NewEventData))
    }

 // carega os desportos
  useEffect(() => {
    fetchSports()
      .then(setSports)
      .catch(err => {
        console.error(err)
        setError("Failed to load sports list")
      })
  }, [])

  // calcula a data minima para evitar marcaçoes no passado
  const minDateTime = useMemo(() => {
    const now = new Date()
    const pad = (n: number) => n.toString().padStart(2, "0")
    const year = now.getFullYear()
    const month = pad(now.getMonth() + 1)
    const day = pad(now.getDate())
    const hours = pad(now.getHours())
    const minutes = pad(now.getMinutes())
    return `${year}-${month}-${day}T${hours}:${minutes}`
  }, [])

  const onSubmit = async (e: FormEvent) => { // envio do formulario
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      // valida a data passada
      if (!form.starts_at || new Date(form.starts_at) <= new Date()) {
        throw new Error("Please choose a future date and time.")
      }

      // datetime-local gives “YYYY-MM-DDTHH:mm”, backend wants “YYYY-MM-DD HH:mm:00”
      const startsAt = form.starts_at.replace("T", " ") + ":00"

      // valida desporto
      if (!form.sport_id) {
        throw new Error("Please choose a sport")
      }
      // valida local
      if (latitude == null || longitude == null) {
        throw new Error("Please select a valid place from the suggestions.")
      }

      await createEvent({ ...form, starts_at: startsAt })  // cria evento
      navigate("/my-activities")  // redireciona para a lista de atividades
    } catch (err: any) {
      setError(err.message || "Failed to create event")
    } finally {
      setLoading(false)
    }
  }

  // guarda a referencia do autocomplete
  const onLoad = (autocomplete: google.maps.places.Autocomplete) => {
    autocompleteRef.current = autocomplete
  }


  // callback quando o user escolhe um local sugerido
  const onPlaceChanged = () => {
    const ac = autocompleteRef.current
    if (!ac) return
    const place = ac.getPlace()    // obtem o local selecionado
      // verifica se e valido com coordenadas
    const addr = place.formatted_address
    if (typeof addr === "string" && place.geometry?.location) {
      const loc = place.geometry.location
      setForm(f => ({ ...f, place: addr }))  // atualiza o campo do formulario
      setLatitude(loc.lat())
      setLongitude(loc.lng())
    }
  }

  return (
    <LoadScript
      googleMapsApiKey={import.meta.env.VITE_GOOGLE_MAPS_API_KEY!}
      libraries={LIBRARIES}
    >
      <section className="create-page">
        <h2>Create Event</h2>

        {error && <div className="err">{error}</div>}

        <form onSubmit={onSubmit} className="create-form">
          {/* — Name + Sport select — */}
          <div className="row">
            <label>
              Name
              <input
                type="text"
                value={form.name}
                onChange={handleChange("name")}
                required
              />
            </label>

            <label>
              Sport
              <select
                value={form.sport_id || ""}
                onChange={handleChange("sport_id")}
                required
              >
                <option value="" disabled>
                  — choose a sport —
                </option>
                {sports.map(s => (
                  <option key={s.id} value={s.id}>
                    {s.name}
                  </option>
                ))}
              </select>
            </label>
          </div>

          {/* — Date & Place Autocomplete — */}
          <div className="row">
            <label>
              Date &amp; Time
              <input
                type="datetime-local"
                value={form.starts_at}
                min={minDateTime}
                onChange={handleChange("starts_at")}
                required
              />
            </label>

            <label className="place-label">
              Place
              <Autocomplete
                onLoad={onLoad}
                onPlaceChanged={onPlaceChanged}
              >
                <input
                  type="text"
                  value={form.place}
                  onChange={e => {
                    handleChange("place")(e)
                    setLatitude(null)
                    setLongitude(null)
                  }}
                  placeholder="Start typing an address…"
                  required
                />
              </Autocomplete>
            </label>
          </div>

          {/* — Max participants — */}
          <div className="row">
            <label>
              Max Participants
              <input
                type="number"
                min={2}
                value={form.max_participants}
                onChange={handleChange("max_participants")}
                required
              />
            </label>
          </div>

          <button type="submit" disabled={loading}>
            {loading ? "Saving…" : "Create Event"}
          </button>
        </form>
      </section>
    </LoadScript>
  )
}

export default CreateEvent