import { MapContainer, TileLayer, Marker, Popup, Polyline } from 'react-leaflet'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

// Emoji markers — no ugly blue arrows
const emojiIcon = (emoji, size = 28) => L.divIcon({
  html: `<span style="font-size:${size}px;line-height:1">${emoji}</span>`,
  className: 'emoji-marker',
  iconSize: [size, size],
  iconAnchor: [size / 2, size / 2],
  popupAnchor: [0, -size / 2]
})

const TYPE_EMOJI = {
  MUSEUM: '🏛️',
  LANDMARK: '🗽',
  COFFEE_SHOP: '☕',
  CINEMA: '🎬'
}

// Metro station coordinates
const STATION_COORDS = {
  union_station:  [38.8972, -77.0064],
  judiciary_sq:   [38.8964, -77.0167],
  gallery_place:  [38.8983, -77.0219],
  metro_center:   [38.8983, -77.0281],
  archives:       [38.8938, -77.0220],
  l_enfant_plaza: [38.8849, -77.0219],
  smithsonian:    [38.8880, -77.0281],
  capitol_south:  [38.8849, -77.0052],
  eastern_market: [38.8846, -76.9960],
  foggy_bottom:   [38.9007, -77.0505],
  woodley_park:   [38.9249, -77.0524]
}

const DC_CENTER = [38.8950, -77.0300]

const formatStation = (id) =>
  id.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase())

export default function MapView({ itinerary }) {
  const attractions = []

  if (itinerary?.selectedMode && itinerary?.attractions) {
    itinerary.attractions.forEach(a => {
      attractions.push({
        name: a.name,
        type: a.type,
        lat: a.latitude,
        lng: a.longitude,
        fee: a.entranceFee,
        duration: a.estimatedVisitDurationMinutes,
        station: a.nearestMetroStation
      })
    })
  } else if (itinerary?.days) {
    itinerary.days.forEach(day => {
      day.timeBlocks?.forEach(block => {
        if (block.latitude && block.longitude) {
          attractions.push({
            name: block.attractionName || block.activity,
            type: block.type,
            lat: block.latitude,
            lng: block.longitude
          })
        }
      })
    })
  }

  if (attractions.length === 0) return null

  // Start station
  const startStation = itinerary?.startStation
  const startCoord = startStation && STATION_COORDS[startStation]

  // Route line
  const routePoints = []
  if (startCoord) routePoints.push(startCoord)
  attractions.forEach(a => {
    if (a.lat && a.lng) routePoints.push([a.lat, a.lng])
  })

  return (
    <div className="map-section">
      <h2>🗺️ Your Route</h2>
      <div className="map-container">
        <MapContainer
          center={DC_CENTER}
          zoom={13}
          style={{ height: '420px', width: '100%', borderRadius: '16px' }}
        >
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />

          {/* Start station marker */}
          {startCoord && (
            <Marker position={startCoord} icon={emojiIcon('🚇', 32)}>
              <Popup>
                <strong>📍 Start: {formatStation(startStation)}</strong>
              </Popup>
            </Marker>
          )}

          {/* Attraction markers with number badges */}
          {attractions.map((a, i) => (
            <Marker
              key={i}
              position={[a.lat, a.lng]}
              icon={emojiIcon(
                `<span class="map-badge">${i + 1}</span>${TYPE_EMOJI[a.type] || '📍'}`,
                36
              )}
            >
              <Popup>
                <strong>#{i + 1} {a.name}</strong><br />
                {a.fee > 0 ? `💰 $${a.fee.toFixed(2)}` : '🆓 Free'}<br />
                {a.duration && `⏱️ ~${a.duration} min`}<br />
                {a.station && `🚇 ${formatStation(a.station)}`}
              </Popup>
            </Marker>
          ))}

          {/* Route line */}
          {routePoints.length > 1 && (
            <Polyline
              positions={routePoints}
              color="#4a6cf7"
              weight={3}
              dashArray="8 6"
              opacity={0.7}
            />
          )}
        </MapContainer>
      </div>

      {/* Legend */}
      <div className="map-legend">
        <span className="legend-item">🚇 Start</span>
        <span className="legend-item">🏛️ Museum</span>
        <span className="legend-item">🗽 Landmark</span>
        <span className="legend-item">☕ Coffee</span>
        <span className="legend-item">🎬 Cinema</span>
        <span className="legend-item">--- Route</span>
      </div>
    </div>
  )
}