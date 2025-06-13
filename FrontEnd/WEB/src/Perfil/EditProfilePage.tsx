
import React, { useState, useEffect, useRef, type FormEvent, type ChangeEvent } from "react"
import { useNavigate } from "react-router-dom"
import { fetchMe, updateMe } from "../api/user"
import logo from "../assets/logo.png"
import "./perfil.css"
import { useLoadScript, Autocomplete } from "@react-google-maps/api"
import Select from "react-select"
import type { User, UpdateMePayload } from "../api/user"
import { fetchSports } from "../api/sports"
import type { Sport } from "../api/sports"

export default function EditProfilePage() {
  const navigate = useNavigate()

  // ---------estados ----------------//
  const [name, setName] = useState("")
  const [location, setLocation] = useState("")
  const [latitude, setLatitude] = useState<number | null>(null)
  const [longitude, setLongitude] = useState<number | null>(null)
  const [sportsOptions, setSportsOptions] = useState<{ value: number; label: string }[]>([])
  const [selectedSports, setSelectedSports] = useState<{ value: number; label: string }[]>([])
   // ---------mensagem de sucesso/erro ----------------//
  const [msg, setMsg] = useState<string | null>(null)
  const [err, setErr] = useState<string | null>(null)

  // google maps autocomplete
  const autocompleteRef = useRef<google.maps.places.Autocomplete | null>(null)
  const { isLoaded } = useLoadScript({
    googleMapsApiKey: import.meta.env.VITE_GOOGLE_MAPS_API_KEY,
    libraries: ["places"],
  })

  useEffect(() => {
    let mounted = true
    ;(async () => {
      try {
        const me: User = await fetchMe()    // vai buscar os dados do utilizador

        if (!mounted) return
        // preenche os campos com os dados do utilizador
        setName(me.name)
        setLocation(me.location || "")
        setLatitude(me.latitude ?? null)
        setLongitude(me.longitude ?? null)
        setSelectedSports(me.sports.map(s => ({ value: s.id, label: s.name })))

        const allSports: Sport[] = await fetchSports()   //vai buscar os desportos
        if (!mounted) return
        setSportsOptions(allSports.map(s => ({ value: s.id, label: s.name })))
      } catch (e) {
        console.error(e)
      }
    })()
    return () => { mounted = false }
  }, [])

  // ----------handler google maps autocomplete---------------
  const handlePlaceChanged = () => {
    const place = autocompleteRef.current?.getPlace()
    if (!place) return
    // tenta encontrar a localidade
    const locality = place.address_components?.find(c =>
      c.types.includes("locality") || c.types.includes("administrative_area_level_3")
    )
    //Se encontrar uma localização valida com coordenadasguarda-a
    if (locality && place.geometry?.location) {
      const loc = place.geometry.location
      setLocation(place.formatted_address || locality.long_name)
      setLatitude(loc.lat())
      setLongitude(loc.lng())
      setErr(null)
    } else {
      setErr("Please select a valid city or village from the suggestions.")
    }
  }
 // ----------handler formulario---------------
  const onSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setErr(null)
    setMsg(null)

    // validaçoes
    if (!name.trim()) {
      setErr("Name cannot be empty.")
      return
    }
    if (latitude == null || longitude == null) {
      setErr("Please select your location from the dropdown suggestions.")
      return
    }

    try {   // cria o payload
      const payload: Partial<UpdateMePayload> = {
        name: name.trim(),
        location: location.trim(),
        latitude,
        longitude,
        sports: selectedSports.map(s => s.value),
      }

      const json = await updateMe(payload)   // envia para a api
      setMsg(json.message)
    } catch (e: any) {
      setErr(e.message)
    }
  }

  return (
<div className="container perfil-form-layout">
      <div className="form-panel">
        <h1>Edit Profile</h1>
        <p>Update your name, location (must choose from suggestions), and favourite sports.</p>

        {msg && <div className="success">{msg}</div>}
        {err && <div className="error">{err}</div>}

        <form onSubmit={onSubmit}>
          <label>
            Name
            <input
              type="text"
              value={name}
              onChange={(e: ChangeEvent<HTMLInputElement>) => setName(e.target.value)}
              required
            />
          </label>

          <label>
            Location
            {isLoaded && (
              <Autocomplete
                onLoad={ref => (autocompleteRef.current = ref)}
                onPlaceChanged={handlePlaceChanged}
                options={{ types: ["(cities)"] }}
              >
         <input
     type="text"
     value={location}
     onChange={e => {
       setLocation(e.target.value)
       // clear coords until they choose again
       setLatitude(null)
       setLongitude(null)
       setErr("Please select a city from the dropdown.")
     }}
     placeholder="Start typing and select a city"
     required
   />
              </Autocomplete>
            )}
          </label>

          <label>
            Favourite Sports
            <Select
              isMulti
              options={sportsOptions}
              value={selectedSports}
              onChange={vals => setSelectedSports(vals as { value: number; label: string }[])}
            />
          </label>

          <button type="submit">Save</button>
        </form>

        <button style={{ marginTop: "1rem" }} onClick={() => navigate("/account")}>Back to Profile</button>
      </div>

      <div className="logo-panel">
        <img src={logo} alt="TeamUP logo" className="logo" />
      </div>
    </div>
  )
}
