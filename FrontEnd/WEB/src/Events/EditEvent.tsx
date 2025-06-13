import React, {
    useState, useEffect, useRef,
    type ChangeEvent, type FormEvent
} from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useLoadScript, Autocomplete } from "@react-google-maps/api";
import {
    type Event,
    updateEvent,
    type NewEventData           
} from "../api/event";
import { fetchSports, type Sport } from "../api/sports";
import "./CreateEvent.css";       

const LIBS: ("places")[] = ["places"];  // google maps api

export default function EditEvent() {
    /* ---------------------------estados----------------------------------------- */
    const { id } = useParams<{ id: string }>();
    const nav = useNavigate();

    const [original, setOriginal] = useState<Event | null>(null);  // dados originais do evento
    const [sports, setSports] = useState<Sport[]>([]);
    const [error, setError] = useState<string | null>(null);
    const [saving, setSaving] = useState(false);  // Estado de carregamento durante a submissao do formulario

    /* estado formulario */
    const [form, setForm] = useState<NewEventData & {
        latitude: number | null;
        longitude: number | null;
    }>({
        name: "", sport_id: 0, starts_at: "",
        place: "", max_participants: 2,
        latitude: null, longitude: null
    });

    /* -------------------------- google maps api------------------------------------------ */
    const { isLoaded } = useLoadScript({
        googleMapsApiKey: import.meta.env.VITE_GOOGLE_MAPS_API_KEY,
        libraries: LIBS,
    });
    const acRef = useRef<google.maps.places.Autocomplete | null>(null);

    /* ------------------------carregar dados-------------------------------------------- */
    useEffect(() => {
        if (!id) return;

        // detalhes do evento
        (async () => {
            try {
                const res = await fetch(`/api/events/${id}`, {
                    headers: {
                        Authorization: `Bearer ${localStorage.getItem("auth_token") ??
                            sessionStorage.getItem("auth_token")}`
                    }
                });
                const ev: Event = await res.json();
                if (!res.ok) throw new Error(ev as any);

                // inicializa os campos do formulario
                setOriginal(ev);    
                setForm({
                    name: ev.name,
                    sport_id: 0,                       
                    starts_at: ev.starts_at.replace(" ", "T").slice(0, 16),
                    place: ev.place,
                    max_participants: ev.max_participants,
                    latitude: ev.latitude,
                    longitude: ev.longitude
                });
            } catch (e: any) {
                setError(e.message || "Failed to load event");
            }
        })();

   
        fetchSports() // carega a lista dos desportos
            .then(setSports)
            .catch(() => setSports([]));
    }, [id]);

 
    useEffect(() => {     // Procura o desporto correspondente ao nome presente no evento original
        if (!original || !sports.length) return;
        const match = sports.find(s => s.name.toLowerCase() === (original.sport ?? "").toLowerCase());
        if (match) setForm(f => ({ ...f, sport_id: match.id }));// Atualiza o formulario com o ID do desporto correto
    }, [original, sports]);

    /* ----------------------funçao handle generica para atualizar os campos do formulario---------------------------------------------- */
    const handle =
        <K extends keyof typeof form>(k: K) =>
            (e: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
                const v = e.target.type === "number"   // Converte valor para numero se o campo for do tipo "number"
                    ? Number(e.target.value) : e.target.value;
                setForm(f => ({ ...f, [k]: v } as typeof form)); // Atualiza o campo especifico no formulario
                if (k === "place") setForm(f => ({ ...f, latitude: null, longitude: null }));  // Se o campo alterado for o "place" limpa as coordenadas 
            };
    /* ---------------------Callback  quando o utilizador escolhe uma localização---------------------------------------------- */
    const onPlaceChanged = () => {
        const ac = acRef.current;
        if (!ac) return;
        const place = ac.getPlace();
        const loc = place.geometry?.location;
        if (!loc) { setError("Please pick a valid suggestion."); return; }  // valida localizaçao

        setForm(f => ({   // atualiza o formulario com a morada e coordenadas
            ...f,
            place: place.formatted_address ?? place.name ?? "",
            latitude: loc.lat(),
            longitude: loc.lng(),
        }));
        setError(null);
    };

    /* ---------------------- submisao do formulario---------------------------------------------- */
    const onSubmit = async (e: FormEvent) => {
        e.preventDefault();
        setError(null);

        if (!form.sport_id)   // valida desporto
            return setError("Sport is required.");
        if (form.latitude == null || form.longitude == null)
            return setError("Pick a valid location from the dropdown.");  // valida localizaçao

        try {
            setSaving(true);    // Desativa estado de carregamento
            await updateEvent(id!, {    //chamada api para atualizar evento
                name: form.name.trim(),
                sport_id: form.sport_id,
                starts_at: form.starts_at.replace("T", " ") + ":00",
                place: form.place.trim(),
                max_participants: form.max_participants,
                latitude: form.latitude!,
                longitude: form.longitude!,
            });
            nav(`/events/${id}`);   // volta para a pagina do evento
        } catch (e: any) {
            setError(e.message || "Failed to update event");
        } finally {
            setSaving(false);
        }
    };

    /* -------------------------------------------------------------------- */
    if (!original) return <p className="loading">Loading…</p>;   // loading

    return (
        <section className="create-page">
            <h2>Edit Event</h2>

            {error && <div className="err">{error}</div>}

            <form onSubmit={onSubmit} className="create-form">
                {/* ---- NAME + SPORT ---- */}
                <div className="row">
                    <label>
                        Name
                        <input type="text" value={form.name} onChange={handle("name")} required />
                    </label>

                    <label>
                        Sport
                        <select value={form.sport_id} onChange={handle("sport_id")} required>
                            <option value="">— choose a sport —</option>
                            {sports.map(s => (
                                <option key={s.id} value={s.id}>{s.name}</option>
                            ))}
                        </select>
                    </label>
                </div>

                {/* ---- DATE/TIME + PLACE ---- */}
                <div className="row">
                    <label>
                        Date&nbsp;&amp;&nbsp;Time
                        <input
                            type="datetime-local"
                            value={form.starts_at}
                            onChange={handle("starts_at")}
                            required
                        />
                    </label>

                    <label className="place-label">
                        Place
                        {isLoaded && (
                            <Autocomplete
                                onLoad={r => (acRef.current = r)}
                                onPlaceChanged={onPlaceChanged}
                            >
                                <input
                                    type="text"
                                    value={form.place}
                                    onChange={handle("place")}
                                    placeholder="Start typing a location…"
                                    required
                                />
                            </Autocomplete>
                        )}
                    </label>
                </div>

                {/* ---- MAX PARTICIPANTS ---- */}
                <div className="row">
                    <label>
                        Max&nbsp;Participants
                        <input
                            type="number"
                            min={2}
                            value={form.max_participants}
                            onChange={handle("max_participants")}
                            required
                        />
                    </label>
                </div>

                <button type="submit" disabled={saving}>
                    {saving ? "Saving…" : "Save Changes"}
                </button>
                <button
                    type="button"
                    className="btn btn-secondary"
                    onClick={() => nav(`/events/${id}`)}
                >
                    Back to Event
                </button>
            </form>
        </section>
    );
}
