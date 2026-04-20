import { useState, useRef } from 'react'
import TripForm from './components/TripForm'
import AttractionPicker from './components/AttractionPicker'
import Itinerary from './components/Itinerary'
import MapView from './components/MapView'
import Cost from './components/Cost'
import Footer from './components/Footer'
import { getTripCost, getMetroRoute, planTrip } from './api'
import './App.css'

function timeToMinutes(timeStr) {
  const [h, m] = timeStr.split(':').map(Number)
  return h * 60 + m
}

function minutesToTime(minutes) {
  const h = Math.floor(minutes / 60) % 24
  const m = minutes % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`
}

function App() {
  const [itinerary, setItinerary] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [selectedTypes, setSelectedTypes] = useState(['MUSEUM', 'LANDMARK'])
  const [selectedAttractions, setSelectedAttractions] = useState([])
  const [pickerOpen, setPickerOpen] = useState(true)

  // Ref for scrolling to results
  const resultsRef = useRef(null)

  const toggleAttraction = (attraction) => {
    setSelectedAttractions(prev => {
      const exists = prev.find(a => a.id === attraction.id)
      if (exists) return prev.filter(a => a.id !== attraction.id)
      return [...prev, attraction]
    })
  }

  const clearAllSelections = () => {
    setSelectedAttractions([])
  }

  const handlePlanTrip = async (tripRequest) => {
    setLoading(true)
    setError(null)
    try {
      if (selectedAttractions.length > 0) {
        const ids = selectedAttractions.map(a => a.id)
        const station = tripRequest.startLocation.metroStation

        const costData = await getTripCost(ids, station)

        const stops = [station, ...selectedAttractions.map(a => a.nearestMetroStation)]
        const routes = []
        const routeWarnings = []
        for (let i = 0; i < stops.length - 1; i++) {
          if (stops[i] !== stops[i + 1]) {
            try {
              routes.push(await getMetroRoute(stops[i], stops[i + 1]))
            } catch {
              // Route lookup failed — fall back to a 15-min estimate rather than
              // silently using 0 which would make time-window checks unreliable.
              routes.push({ travelTimeMinutes: 15, fare: 0, estimated: true })
              routeWarnings.push(
                `Could not calculate route from ${stops[i]} to ${stops[i + 1]} — using 15-min estimate`
              )
            }
          } else {
            routes.push({ travelTimeMinutes: 0, fare: 0, stations: [] })
          }
        }

        let currentTime = tripRequest.availableTimePerDay.start
        const endLimit = timeToMinutes(tripRequest.availableTimePerDay.end)
        const timeBlocks = []
        const skipped = []
        let totalTravelMin = 0

        selectedAttractions.forEach((a, i) => {
          const travelMin = routes[i]?.travelTimeMinutes || 0
          const startMin = timeToMinutes(currentTime) + travelMin
          const endMin = startMin + a.estimatedVisitDurationMinutes

          // Skip if this attraction would exceed end time
          if (endMin > endLimit) {
            skipped.push(a.name)
            return
          }

          timeBlocks.push({
            startTime: minutesToTime(startMin),
            endTime: minutesToTime(endMin),
            attractionName: a.name,
            type: a.type,
            travelTimeMinutes: travelMin,
            entranceFee: a.entranceFee,
            nearestStation: a.nearestMetroStation
          })
          totalTravelMin += travelMin
          currentTime = minutesToTime(endMin)
        })

        setItinerary({
          selectedMode: true,
          days: [{ date: tripRequest.tripStartDate || 'Your Trip', timeBlocks }],
          costBreakdown: costData,
          attractions: selectedAttractions.filter(a => !skipped.includes(a.name)),
          startStation: station,
          totalAttractions: timeBlocks.length,
          totalTravelTimeMinutes: totalTravelMin,
          skippedMessage: skipped.length > 0
            ? `Not enough time for: ${skipped.join(', ')}`
            : null,
          warnings: routeWarnings
        })
      } else {
        const data = await planTrip(tripRequest)
        data.startStation = tripRequest.startLocation.metroStation
        setItinerary(data)
      }

      // Collapse picker and scroll to results
      setPickerOpen(false)
      setTimeout(() => {
        resultsRef.current?.scrollIntoView({ behavior: 'smooth' })
      }, 100)

    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="app">
      <header className="header">
        <h1>🏛️ DC Weekend Trip Planner 🏛️</h1>
        <p>🌿 Cherry blossoms are out 🌸 time to explore DC with friends 🌷</p>
      </header>

      <TripForm
        onSubmit={handlePlanTrip}
        loading={loading}
        selectedTypes={selectedTypes}
        onTypesChange={setSelectedTypes}
        selectedCount={selectedAttractions.length}
      />

      <AttractionPicker
        selectedTypes={selectedTypes}
        selectedAttractions={selectedAttractions}
        onToggle={toggleAttraction}
        onClearAll={clearAllSelections}
        isOpen={pickerOpen}
        onToggleOpen={() => setPickerOpen(!pickerOpen)}
      />

      {error && (
        <div className="error-banner">
          <p>{error}</p>
          {/no attractions match|match your filters/i.test(error) && (
            <ul className="filter-suggestions">
              <li>Select additional attraction types using the chips above</li>
              <li>Increase your budget slider</li>
              <li>Extend your available time window (earlier start or later end)</li>
              <li>Uncheck "Accessibility only" if it is enabled</li>
            </ul>
          )}
        </div>
      )}

      <div ref={resultsRef}>
        {itinerary?.skippedMessage && (
          <div className="warning-banner">{itinerary.skippedMessage}</div>
        )}
        {itinerary?.warnings?.length > 0 && itinerary.warnings.map((w, i) => (
          <div key={i} className="warning-banner">⚠️ {w}</div>
        ))}
        {itinerary && (
          <>
            <MapView itinerary={itinerary} />
            <Itinerary itinerary={itinerary} />
            <Cost itinerary={itinerary} />
          </>
        )}
      </div>
      <Footer />
    </div>
  )
}

export default App