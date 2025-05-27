// src/Events/CreateEvent.tsx
import React, { useState, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { LoadScript, Autocomplete } from "@react-google-maps/api";
import type { LoadScriptProps } from "@react-google-maps/api";
import { createEvent, type NewEventData } from "../api/event";
import "./CreateEvent.css";

// define once, outside the component, as a mutable Library[]
const LIBRARIES: LoadScriptProps["libraries"] = ["places"];

const CreateEvent: React.FC = () => {
  const [form, setForm] = useState<NewEventData>({
    name: "",
    sport_id: 1,
    date: "",
    place: "",
    max_participants: 2,
  });
  const [loading, setLoading] = useState(false);
  const [error, setError]     = useState<string | null>(null);
  const navigate              = useNavigate();

  // keep the Autocomplete widget in a ref
  const autocompleteRef = useRef<google.maps.places.Autocomplete | null>(null);

  const handleChange =
    <K extends keyof NewEventData>(key: K) =>
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const val =
        e.target.type === "number" ? Number(e.target.value) : e.target.value;
      setForm(f => ({ ...f, [key]: val } as NewEventData));
    };

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await createEvent(form);
      navigate("/my-activities");
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  // store the Autocomplete instance
  const onLoad = (autocomplete: google.maps.places.Autocomplete) => {
    autocompleteRef.current = autocomplete;
  };

  // when the user picks a suggestion
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
              Sport ID
              <input
                type="number"
                min={1}
                value={form.sport_id}
                onChange={handleChange("sport_id")}
                required
              />
            </label>
          </div>

          <div className="row">
            <label>
              Date &amp; Time
              <input
                type="datetime-local"
                value={form.date}
                onChange={handleChange("date")}
                required
              />
            </label>

             <label className="place-label">
              Place
              <Autocomplete onLoad={onLoad} onPlaceChanged={onPlaceChanged}>
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
