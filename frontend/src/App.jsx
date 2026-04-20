import { useState, useRef } from 'react'
import TripForm from './components/TripForm'
import AttractionPicker from './components/AttractionPicker'
import Itinerary from './components/Itinerary'
import MapView from './components/MapView'
import Cost from './components/Cost'
import Footer from './components/Footer'
import { planTrip } from './api'
import './App.css'

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
      // If the user picked attractions, send them as must-include anchors.
      // The backend pre-schedules them on day 1 and fills the rest of the day
      // via the chosen strategy. This replaces the old "manual mode" that only
      // visited the picks and nothing else.
      if (selectedAttractions.length > 0) {
        tripRequest.preferences.mustIncludeAttractionIds =
          selectedAttractions.map(a => a.id)
      }

      const data = await planTrip(tripRequest)
      data.startStation = tripRequest.startLocation.metroStation
      setItinerary(data)

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