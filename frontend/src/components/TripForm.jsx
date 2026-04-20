import { useState, useEffect } from 'react'

const TRIP_DURATIONS = [
  { value: 'half_day', label: 'Half Day' },
  { value: '1_day', label: '1 Day' },
  { value: '2_days', label: '2 Days' }
]

const ATTRACTION_TYPES = [
  { value: 'MUSEUM', label: 'Museums', icon: '🏛️' },
  { value: 'LANDMARK', label: 'Landmarks', icon: '🗽' },
  { value: 'COFFEE_SHOP', label: 'Coffee', icon: '☕' },
  { value: 'CINEMA', label: 'Cinema', icon: '🎬' }
]

const STRATEGIES = [
  { value: 'maximize_attractions', label: 'See More Places' },
  { value: 'minimize_travel_time', label: 'Less Travel Time' },
  { value: 'balanced', label: 'Balanced' }
]

export default function TripForm({ onSubmit, loading, selectedTypes, onTypesChange, selectedCount }) {
  const [stations, setStations] = useState([])
  const [form, setForm] = useState({
    startStation: 'union_station',
    duration: '1_day',
    date: '',
    startTime: '09:00',
    endTime: '18:00',
    maxBudget: 50,
    accessible: false,
    strategy: 'maximize_attractions'
  })

  // Fetch metro stations from backend on mount
  useEffect(() => {
    fetch('http://localhost:7070/metro/stations')
      .then(res => res.json())
      .then(data => setStations(data))
      .catch(() => setStations([
        'union_station', 'metro_center', 'smithsonian',
        'gallery_place', 'l_enfant_plaza', 'archives',
        'judiciary_sq', 'capitol_south', 'eastern_market',
        'foggy_bottom', 'woodley_park'
      ]))
  }, [])

  const toggleType = (type) => {
    if (selectedTypes.includes(type)) {
      onTypesChange(selectedTypes.filter(t => t !== type))
    } else {
      onTypesChange([...selectedTypes, type])
    }
  }

  const handleSubmit = () => {
    const tripRequest = {
      startLocation: { metroStation: form.startStation },
      tripDuration: form.duration,
      tripStartDate: form.date || undefined,
      availableTimePerDay: { start: form.startTime, end: form.endTime },
      preferences: {
        types: selectedTypes.map(t => t.toLowerCase()),
        maxBudget: parseFloat(form.maxBudget),
        accessibilityRequired: form.accessible,
        optimizationStrategy: form.strategy
      }
    }
    onSubmit(tripRequest)
  }

  const formatStation = (id) => {
    return id.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase())
  }

  return (
    <div className="trip-form">
      <div className="form-row">
        {/* Start Station */}
        <div className="form-group">
          <label>📍 Start Station</label>
          <select
            value={form.startStation}
            onChange={e => setForm({ ...form, startStation: e.target.value })}
          >
            {stations.map(s => (
              <option key={s} value={s}>{formatStation(s)}</option>
            ))}
          </select>
        </div>

        {/* Duration */}
        <div className="form-group">
          <label>⏱️ Duration</label>
          <select
            value={form.duration}
            onChange={e => setForm({ ...form, duration: e.target.value })}
          >
            {TRIP_DURATIONS.map(d => (
              <option key={d.value} value={d.value}>{d.label}</option>
            ))}
          </select>
        </div>

        {/* Date */}
        <div className="form-group">
          <label>📅 Date</label>
          <input
            type="date"
            value={form.date}
            onChange={e => setForm({ ...form, date: e.target.value })}
          />
        </div>

        {/* Time Range */}
        <div className="form-group">
          <label>🕐 Time</label>
          <div className="time-range">
            <input
              type="time"
              value={form.startTime}
              onChange={e => setForm({ ...form, startTime: e.target.value })}
            />
            <span>to</span>
            <input
              type="time"
              value={form.endTime}
              onChange={e => setForm({ ...form, endTime: e.target.value })}
            />
          </div>
        </div>
      </div>

      <div className="form-row">
        {/* Attraction Types — synced with AttractionPicker */}
        <div className="form-group">
          <label>🏛️ Interests</label>
          <div className="chip-group">
            {ATTRACTION_TYPES.map(({ value, label, icon }) => (
              <button
                key={value}
                className={`chip ${selectedTypes.includes(value) ? 'active' : ''}`}
                onClick={() => toggleType(value)}
              >
                {icon} {label}
              </button>
            ))}
          </div>
        </div>

        {/* Budget */}
        <div className="form-group">
          <label>💰 Budget: ${form.maxBudget}</label>
          <input
            type="range"
            min="0"
            max="100"
            value={form.maxBudget}
            onChange={e => setForm({ ...form, maxBudget: e.target.value })}
          />
        </div>

        {/* Strategy */}
        <div className="form-group">
          <label>🎯 Strategy</label>
          <select
            value={form.strategy}
            onChange={e => setForm({ ...form, strategy: e.target.value })}
          >
            {STRATEGIES.map(s => (
              <option key={s.value} value={s.value}>{s.label}</option>
            ))}
          </select>
        </div>

        {/* Accessibility */}
        <div className="form-group checkbox-group">
          <label>
            <input
              type="checkbox"
              checked={form.accessible}
              onChange={e => setForm({ ...form, accessible: e.target.checked })}
            />
            ♿ Accessible only
          </label>
        </div>

        {/* Submit */}
        <button
          className="plan-btn"
          onClick={handleSubmit}
          disabled={loading || (selectedTypes.length === 0 && selectedCount === 0)}
        >
          {loading ? '⏳ Planning...' :
           selectedCount > 0 ? `🚀 Plan Trip (incl. ${selectedCount} picks)` : '🚀 Auto Plan'}
        </button>
      </div>
    </div>
  )
}