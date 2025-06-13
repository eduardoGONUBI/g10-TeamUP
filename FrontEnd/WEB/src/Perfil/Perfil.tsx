  import { useEffect, useState, useRef } from "react"
  import { useNavigate } from "react-router-dom"
  import {
    fetchMe,
    fetchAchievements,
    fetchXpLevel,
    fetchReputation,
    fetchAvatar,
    uploadAvatar,
    updateMe,
  } from "../api/user"
  import "./perfil.css"
  import type { Achievement, Reputation, User, UpdateMePayload } from "../api/user"
  import type { Sport } from "../api/sports"
  import { fetchSports } from "../api/sports"
  import { useLoadScript, Autocomplete } from "@react-google-maps/api"
  import Select from "react-select"

  // label de reputaçao
  function behaviourLabel(score: number): string {
    if (score >= 90) return "Excellent"
    if (score >= 75) return "Very Good"
    if (score >= 60) return "Good"
    if (score >= 40) return "Average"
    return "Poor"
  }

  const libraries: ("places")[] = ["places"]    // google maps api

  export default function Account() {
    const nav = useNavigate()

 // --------------------ESTADOS-------------------------------
    const [user, setUser] = useState<User | null>(null)
    const [avatarSrc, setAvatarSrc] = useState<string | null>(null)
    const [achievements, setAchievements] = useState<Achievement[]>([])
    const [xp, setXp] = useState<number | null>(null)
    const [level, setLevel] = useState<number | null>(null)
    const [reputation, setReputation] = useState<Reputation | null>(null)
    const [uploading, setUploading] = useState(false)
    const fileInput = useRef<HTMLInputElement>(null)

    
    // -----------------estados do formulario-------------
    
    const [editing, setEditing] = useState(false)
    const [sportsList, setSportsList] = useState<Sport[]>([])
    const [formData, setFormData] = useState<{
      name: string
      location: string
      latitude: number | null
      longitude: number | null
      sports: Sport[]
    }>({
      name: "",
      location: "",
      latitude: null,
      longitude: null,
      sports: [],
    })

 
    // Google Maps api setup
    const { isLoaded, loadError } = useLoadScript({
      googleMapsApiKey: import.meta.env.VITE_GOOGLE_MAPS_API_KEY,
      libraries,
    })
    const autocompleteRef = useRef<google.maps.places.Autocomplete | null>(null)

 // ------------------carregar os dados------------------
  useEffect(() => {
    let isMounted = true;

    (async () => {
      // vai buscar o utilizador
      const me = await fetchMe();
      if (!isMounted) return;
      setUser(me);

      //  Seed form fields
      setFormData({  //preench formulario de editar com os dados do utilizador
        name: me.name,
        location: me.location || "",
        latitude: me.latitude ?? null,
        longitude: me.longitude ?? null,
        sports: me.sports,
      });

      //  vai buscar a imagem de perfil
      try {
        const url = await fetchAvatar(me.id);
        if (isMounted) setAvatarSrc(url);
      } catch (err) {
        console.error("Could not fetch avatar", err);
      }

      // vai buscar achievements / xp / reputaçao / lista de desportos ao mesmo tempo
      try {
        const [ach, prof, rep, sports] = await Promise.all([
          fetchAchievements(me.id),
          fetchXpLevel(me.id),
          fetchReputation(me.id),
          fetchSports(),
        ]);
        if (!isMounted) return;
        setAchievements(ach);
        setXp(prof.xp);
        setLevel(prof.level);
        setReputation(rep);
        setSportsList(sports);
      } catch (err) {
        console.error("Error loading stats or sports list", err);
      }
    })();

    return () => {
      isMounted = false;
      if (avatarSrc?.startsWith("blob:")) {
        URL.revokeObjectURL(avatarSrc);
      }
    };
  }, []);


    // Place-changed handler for Google Autocomplete

    const handlePlaceChanged = () => {
      const place = autocompleteRef.current?.getPlace()     // obtem a localizaçao do campo
      if (!place) return
      // procura uma cidade ou regiao
      const locality = place.address_components?.find(c =>
        c.types.includes("locality") ||
        c.types.includes("administrative_area_level_3")
      )
      // se encontrou e tem coordenadas 
      if (locality && place.geometry?.location) {
        const loc = place.geometry.location
        setFormData({  //atualiza o formulario
          ...formData,
          location: place.formatted_address || locality.long_name,
          latitude: loc.lat(),
          longitude: loc.lng(),
        })
      } else {
        alert("Please select a valid location")
      }
    }

 
    // guardar alteraçoes
    
  const handleSave = async () => {
      try {
        // cria o payload com os dados do formulario
        const payload: Partial<UpdateMePayload> = {
          name: formData.name,
          location: formData.location,
          sports: formData.sports.map((s) => s.id),
        }
        if (formData.latitude != null)  payload.latitude  = formData.latitude
        if (formData.longitude!= null)  payload.longitude = formData.longitude

        await updateMe(payload)   // envia o payload
        //recarrega os dados atualizados e sai de ediçao
        const me = await fetchMe()  
        setUser(me)
        setEditing(false)
      } catch (err: any) {
        alert(err.message || "Failed to update profile.")
      }
    }
    if (!user) return <p>Loading…</p>   // loading
    if (loadError) return <p>Error loading maps</p>

    return (
      <section className="account-page">
        <h1>My Account</h1>
        <div className="account-grid">

          {/* LEFT: avatar / upload / XP / level */}
          <div className="avatar-col">
            <img
              src={avatarSrc ?? "/placeholder.png"}
              alt="Avatar"
              className="avatar"
              onClick={() => fileInput.current?.click()}
              style={{ cursor: "pointer" }}
            />
            <input
              ref={fileInput}
              type="file"
              accept="image/*"
              style={{ display: "none" }}
              onChange={async e => {
                const file = e.target.files?.[0]
                if (!file) return
                setUploading(true)
                try {
                  const { url } = await uploadAvatar(file)
                  setAvatarSrc((prev) => {
                    if (prev?.startsWith("blob:")) URL.revokeObjectURL(prev)
                    return url
                  })
                } catch {
                  alert("Sorry, the avatar could not be updated.")
                } finally {
                  setUploading(false)
                  if (fileInput.current) fileInput.current.value = ""
                }
              }}
            />
            {uploading && <small>Uploading…</small>}
            <span className="level">Lvl {level ?? 1}</span>
            {xp !== null && <small>{xp} XP</small>}
          </div>

          {/* RIGHT: display or edit */}
          <div className="info-col">
            {editing ? (
              <>
                <div className="row">
                  <strong>Name</strong>
                  <input
                    type="text"
                    value={formData.name}
                    onChange={e =>
                      setFormData({ ...formData, name: e.target.value })
                    }
                  />
                </div>

                <div className="row">
                  <strong>Location</strong>
                  {isLoaded && (
                    <Autocomplete
                      onLoad={ref => (autocompleteRef.current = ref)}
                      onPlaceChanged={handlePlaceChanged}
                      options={{ types: ["(cities)"] }}
                    >
                      <input
                        type="text"
                        value={formData.location}
                        onChange={e =>
                          setFormData({ ...formData, location: e.target.value })
                        }
                        placeholder="Enter location"
                      />
                    </Autocomplete>
                  )}
                </div>

                <div className="row">
                  <strong>Favourite Sports</strong>
                  <Select
                    isMulti
                    options={sportsList.map((s) => ({
                      value: s.id,
                      label: s.name,
                    }))}
                    value={formData.sports.map((s) => ({
                      value: s.id,
                      label: s.name,
                    }))}
                    onChange={(vals) => {
                      const sel = (vals as { value: number; label: string }[])
                        .map((v) =>
                          sportsList.find((sp) => sp.id === v.value)!
                        )
                      setFormData({ ...formData, sports: sel })
                    }}
                  />
                </div>

                <div className="actions">
                  <button onClick={handleSave}>Save</button>
                  <button onClick={() => setEditing(false)}>Cancel</button>
                </div>
              </>
            ) : (
              <>
                <div className="row">
                  <strong>Name</strong> {user.name}
                </div>
                <div className="row">
                  <strong>Email</strong> {user.email}
                </div>
                <div className="row">
                  <strong>Location</strong> {user.location || "—"}
                </div>
                <div className="row">
                  <strong>Favourite Sports</strong>{" "}
                  {user.sports.map((s) => s.name).join(", ") || "—"}
                </div>

                <div className="row achievements">
                  <strong>Achievement</strong>
                  <div className="icons">
                    {achievements.length > 0 ? (
                      <img
                        key={achievements.at(-1)!.code}
                        src={achievements.at(-1)!.icon}
                        alt={achievements.at(-1)!.title}
                        title={achievements.at(-1)!.description}
                      />
                    ) : (
                      <span className="no-achievements">—</span>
                    )}
                  </div>
                </div>

                {reputation && (
                  <div className="row">
                    <strong>Behaviour Index</strong> {reputation.score}{" "}
                    <span className="behaviour-label">
                      ({behaviourLabel(reputation.score)})
                    </span>
                  </div>
                )}
                {reputation && (
                  <div className="row">
                    <strong>Reputation</strong>{" "}
                    {(() => {
                      const counts: [string, number][] = [
                        ["Good teammate", reputation.good_teammate_count],
                        ["Friendly player", reputation.friendly_count],
                        ["Team player", reputation.team_player_count],
                        ["Watchlisted", reputation.toxic_count],
                        ["Bad sport", reputation.bad_sport_count],
                        ["Frequent AFK", reputation.afk_count],
                      ]
                      const best = counts.reduce(
                        (a, b) => (b[1] > a[1] ? b : a),
                        ["—", 0] as [string, number]
                      )
                      return best[1] > 0 ? `${best[0]} (${best[1]})` : "—"
                    })()}
                  </div>
                )}

                <div className="actions">
                  <button onClick={() => nav("/account/edit")}>
                    Edit Profile
                  </button>
                  <button onClick={() => nav("/change-password")}>
                    Change Password
                  </button>
                  <button onClick={() => nav("/change-email")}>
                    Change Email
                  </button>
                  <button
                    className="danger"
                    onClick={() => nav("/delete-account")}
                  >
                    Delete Account
                  </button>
                </div>
              </>
            )}
          </div>
        </div>
      </section>
    )
  }
