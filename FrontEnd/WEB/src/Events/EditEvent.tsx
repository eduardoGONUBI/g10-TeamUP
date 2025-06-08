import React, {
    useState, useEffect, useRef,
    type ChangeEvent, type FormEvent
} from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useLoadScript, Autocomplete } from "@react-google-maps/api";
import {
    type Event,
    updateEvent,
    type NewEventData            // reuse the interface shape
} from "../api/event";
import { fetchSports, type Sport } from "../api/sports";
import "./CreateEvent.css";        // ✔ reuse the same CSS (already scoped)

const LIBS: ("places")[] = ["places"];

export default function EditEvent() {
    /* -------------------------------------------------------------------- */
    const { id } = useParams<{ id: string }>();
    const nav = useNavigate();

    /* -------------------------------------------------------------------- */
    const [original, setOriginal] = useState<Event | null>(null);
    const [sports, setSports] = useState<Sport[]>([]);
    const [error, setError] = useState<string | null>(null);
    const [saving, setSaving] = useState(false);

    /* the form state extends NewEventData with coords */
    const [form, setForm] = useState<NewEventData & {
        latitude: number | null;
        longitude: number | null;
    }>({
        name: "", sport_id: 0, starts_at: "",
        place: "", max_participants: 2,
        latitude: null, longitude: null
    });

    /* -------------------------------------------------------------------- */
    const { isLoaded } = useLoadScript({
        googleMapsApiKey: import.meta.env.VITE_GOOGLE_MAPS_API_KEY,
        libraries: LIBS,
    });
    const acRef = useRef<google.maps.places.Autocomplete | null>(null);

    /* -------------------------------------------------------------------- */
    /* 1) fetch event + 2) sports list */
    useEffect(() => {
        if (!id) return;

        /* event details */
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

                setOriginal(ev);
                setForm({
                    name: ev.name,
                    sport_id: 0,                         // temp – fixed below
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

        /* sports list */
        fetchSports()
            .then(setSports)
            .catch(() => setSports([]));
    }, [id]);

    /* once sports arrive, inject correct sport_id */
    useEffect(() => {
        if (!original || !sports.length) return;
        const match = sports.find(s => s.name.toLowerCase() === (original.sport ?? "").toLowerCase());
        if (match) setForm(f => ({ ...f, sport_id: match.id }));
    }, [original, sports]);

    /* -------------------------------------------------------------------- */
    const handle =
        <K extends keyof typeof form>(k: K) =>
            (e: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
                const v = e.target.type === "number"
                    ? Number(e.target.value) : e.target.value;
                setForm(f => ({ ...f, [k]: v } as typeof form));
                if (k === "place") setForm(f => ({ ...f, latitude: null, longitude: null }));
            };

    const onPlaceChanged = () => {
        const ac = acRef.current;
        if (!ac) return;
        const place = ac.getPlace();
        const loc = place.geometry?.location;
        if (!loc) { setError("Please pick a valid suggestion."); return; }

        setForm(f => ({
            ...f,
            place: place.formatted_address ?? place.name ?? "",
            latitude: loc.lat(),
            longitude: loc.lng(),
        }));
        setError(null);
    };

    /* -------------------------------------------------------------------- */
    const onSubmit = async (e: FormEvent) => {
        e.preventDefault();
        setError(null);

        if (!form.sport_id)
            return setError("Sport is required.");
        if (form.latitude == null || form.longitude == null)
            return setError("Pick a valid location from the dropdown.");

        try {
            setSaving(true);
            await updateEvent(id!, {
                name: form.name.trim(),
                sport_id: form.sport_id,
                starts_at: form.starts_at.replace("T", " ") + ":00",
                place: form.place.trim(),
                max_participants: form.max_participants,
                latitude: form.latitude!,
                longitude: form.longitude!,
            });
            nav(`/events/${id}`);   // back to the details page
        } catch (e: any) {
            setError(e.message || "Failed to update event");
        } finally {
            setSaving(false);
        }
    };

    /* -------------------------------------------------------------------- */
    if (!original) return <p className="loading">Loading…</p>;

    return (
        <section className="create-page">   {/* same wrapper as CreateEvent */}
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
