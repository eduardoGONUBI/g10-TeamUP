import React, { useEffect, useState } from "react";
import { fetchMyEvents, type Event } from "../api/event";
import { useNavigate } from 'react-router-dom';

import "./ActivitiesList.css";



const MyActivities: React.FC = () => {
    const [events, setEvents] = useState<Event[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
const navigate = useNavigate();

    useEffect(() => {
        fetchMyEvents()
            .then(setEvents)
            .catch((e) => setError(e.message))
            .finally(() => setLoading(false));
    }, []);

    if (loading) return <p>Loading…</p>;
    if (error) return <p style={{ color: "red" }}>{error}</p>;

    return (
        <>
            <h2>Activities Management</h2>
            <p>Total Active Activities</p>
            <h3>{events.length}</h3>

            <section className="events-grid">
                {events.map((ev) => (
                    <article key={ev.id} className="event-card">
                        <header>
                            <div>
                                <strong>{ev.name}</strong> <span>· {ev.sport}</span>
                            </div>
                            <small>{ev.participants.length}/{ev.max_participants} Participants</small>
                        </header>

                        <ul>
                            <li><b>Location:</b> {ev.place}</li>
                            <li><b>Date:</b> {new Date(ev.date).toLocaleDateString()}</li>
                            <li><b>Organizer:</b> {ev.creator.name}</li>
                        </ul>

                        <footer>
                            <button
                                onClick={() => navigate(`/events/${ev.id}`)}
                                style={{ cursor: "pointer" }}
                                className="see-btn"
                            >
                                See Activity&nbsp;›
                            </button>
                        </footer>
                    </article>
                ))}
            </section>
        </>
    );
};

export default MyActivities;
