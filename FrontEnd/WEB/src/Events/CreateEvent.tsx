// src/Events/CreateEvent.tsx
import React, {
  useState,
  useRef,
  useEffect,
  type ChangeEvent,
  type FormEvent,
} from "react";
import { useNavigate } from "react-router-dom";
import { LoadScript, Autocomplete } from "@react-google-maps/api";
import type { LoadScriptProps } from "@react-google-maps/api";
import { createEvent, type NewEventData } from "../api/event";
import { fetchSports, type Sport } from "../api/sports";
import "./CreateEvent.css";

// load-script needs a mutable array reference
const LIBRARIES: LoadScriptProps["libraries"] = ["places"];

const CreateEvent: React.FC = () => {
  const [form, setForm] = useState<NewEventData>({
    name: "",
    sport_id: 0,          // we'll require the user pick one
    starts_at: "",
    place: "",
    max_participants: 2,
  });
  const [sports, setSports]   = useState<Sport[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError]     = useState<string | null>(null);
  const navigate              = useNavigate();

  // for the Google Autocomplete widget
  const autocompleteRef = useRef<google.maps.places.Autocomplete | null>(null);

  // wider typing: handles both input and select
  const handleChange =
    <K extends keyof NewEventData>(key: K) =>
    (
      e: ChangeEvent<HTMLInputElement> |
         ChangeEvent<HTMLSelectElement>
    ) => {
      const val =
        e.target.type === "number"
          ? Number(e.target.value)
          : e.target.value;
      setForm(f => ({ ...f, [key]: val } as NewEventData));
    };

  // load sports once
  useEffect(() => {
    fetchSports()
      .then(setSports)
      .catch(err => {
        console.error(err);
        setError("Failed to load sports list");
      });
  }, []);

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
          // datetime-local gives “YYYY-MM-DDTHH:mm”, backend wants “YYYY-MM-DD HH:mm:00”
     const startsAt = form.starts_at.replace("T", " ") + ":00";
      // ensure they actually picked a sport
      if (!form.sport_id) {
        throw new Error("Please choose a sport");
      }
      await createEvent({ ...form, starts_at: startsAt });
      navigate("/my-activities");
    } catch (err: any) {
      setError(err.message || "Failed to create event");
    } finally {
      setLoading(false);
    }
  };

  // store the Autocomplete instance
  const onLoad = (autocomplete: google.maps.places.Autocomplete) => {
    autocompleteRef.current = autocomplete;
  };

  // when user picks a place from the dropdown
  const onPlaceChanged = () => {
    const ac = autocompleteRef.current;
    if (!ac) return;
    const place = ac.getPlace();
    const addr = place.formatted_address;
    if (typeof addr === "string") {
      setForm(f => ({ ...f, place: addr }));
    }
  };

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
                  onChange={handleChange("place")}
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
  );
};

export default CreateEvent;
