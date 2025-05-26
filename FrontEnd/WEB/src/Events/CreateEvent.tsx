// src/Events/CreateEvent.tsx
import React, { useState } from "react"
import { useNavigate } from "react-router-dom"
import { createEvent, type NewEventData } from "../api/event"
import "./CreateEvent.css"

const CreateEvent: React.FC = () => {
  const [form, setForm] = useState<NewEventData>({
    name: "",
    sport_id: 1,
    date: "",
    place: "",
    max_participants: 2,
    latitude: 41.55,
    longitude: -8.42,
  })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const nav = useNavigate()

  const handleChange =
    <K extends keyof NewEventData>(key: K) =>
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const val =
        e.target.type === "number" ? Number(e.target.value) : e.target.value
      setForm((f) => ({ ...f, [key]: val } as NewEventData))
    }

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      await createEvent(form)
      nav("/my-activities")
    } catch (e: any) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  return (
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
            Date & Time
            <input
              type="datetime-local"
              value={form.date}
              onChange={handleChange("date")}
              required
            />
          </label>

          <label>
            Place
            <input
              type="text"
              value={form.place}
              onChange={handleChange("place")}
              required
            />
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

          <label>
            Latitude
            <input
              type="number"
              step="0.0001"
              value={form.latitude}
              onChange={handleChange("latitude")}
              required
            />
          </label>

          <label>
            Longitude
            <input
              type="number"
              step="0.0001"
              value={form.longitude}
              onChange={handleChange("longitude")}
              required
            />
          </label>
        </div>

        <button type="submit" disabled={loading}>
          {loading ? "Saving…" : "Create Event"}
        </button>
      </form>
    </section>
  )
}

export default CreateEvent
