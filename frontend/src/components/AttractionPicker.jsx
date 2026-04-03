import { useState, useEffect } from 'react'

const TYPE_ICONS = {
  MUSEUM: '🏛️',
  LANDMARK: '🗽',
  COFFEE_SHOP: '☕',
  CINEMA: '🎬'
}

export default function AttractionPicker({
  selectedTypes, selectedAttractions, onToggle, onClearAll, isOpen, onToggleOpen
}) {
  const [attractions, setAttractions] = useState([])
  const [freeOnly, setFreeOnly] = useState(false)
  const [accessibleOnly, setAccessibleOnly] = useState(false)

  useEffect(() => {
    fetch('http://localhost:7070/attractions')
      .then(res => res.json())
      .then(data => setAttractions(data))
      .catch(err => console.error('Failed to load attractions:', err))
  }, [])

  const filtered = attractions.filter(a => {
    if (selectedTypes.length > 0 && !selectedTypes.includes(a.type)) return false
    if (freeOnly && a.entranceFee > 0) return false
    if (accessibleOnly && !a.accessibilityFriendly) return false
    return true
  })

  const isSelected = (id) => selectedAttractions.some(a => a.id === id)

  const formatStation = (id) =>
    id.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase())

  const formatDuration = (min) => {
    if (min >= 60) return `${Math.floor(min / 60)}h ${min % 60 > 0 ? min % 60 + 'min' : ''}`
    return `${min} min`
  }

  const getTodayHours = (hours) => {
    const days = ['sunday','monday','tuesday','wednesday','thursday','friday','saturday']
    const today = days[new Date().getDay()]
    const h = hours[today]
    if (!h || h.open === 'closed') return 'Closed today'
    return `${h.open} — ${h.close}`
  }

  return (
    <div className="attraction-picker">
      {/* Collapsible header */}
      <div className="picker-header" onClick={onToggleOpen}>
        <h2>
          📍 {isOpen ? 'Explore DC Attractions' : `${selectedAttractions.length} places selected`}
          ({filtered.length})
        </h2>
        <span className="toggle-arrow">{isOpen ? '▲' : '▼'}</span>
      </div>

      {/* Selected summary — always visible */}
      {selectedAttractions.length > 0 && (
        <div className="selected-summary">
          <span>✅ Selected {selectedAttractions.length}: </span>
          {selectedAttractions.map(a => (
            <span key={a.id} className="selected-chip">
              {TYPE_ICONS[a.type]} {a.name}
              <button className="remove-btn" onClick={() => onToggle(a)}>×</button>
            </span>
          ))}
          <button className="clear-all-btn" onClick={onClearAll}>
            Clear All
          </button>
        </div>
      )}

      {/* Collapsible content */}
      {isOpen && (
        <>
          <div className="filter-bar">
            <label className="free-toggle">
              <input
                type="checkbox"
                checked={freeOnly}
                onChange={e => setFreeOnly(e.target.checked)}
              />
              Free only
            </label>
            <label className="free-toggle">
              <input
                type="checkbox"
                checked={accessibleOnly}
                onChange={e => setAccessibleOnly(e.target.checked)}
              />
              ♿ Accessible only
            </label>
            <span className="filter-hint">Click cards to select destinations</span>
          </div>

          {filtered.length === 0 ? (
            <div className="no-results">
              No attractions match your current filters. Try selecting more interests above.
            </div>
          ) : (
            <div className="attraction-grid">
              {filtered.map(a => (
                <div
                  key={a.id}
                  className={`attraction-card ${isSelected(a.id) ? 'selected' : ''}`}
                  onClick={() => onToggle(a)}
                >
                  <div className="card-header">
                    <span className="card-icon">{TYPE_ICONS[a.type]}</span>
                    <div className="card-header-right">
                      <span className={`card-fee ${a.entranceFee === 0 ? 'free' : 'paid'}`}>
                        {a.entranceFee === 0 ? 'FREE' : `$${a.entranceFee.toFixed(2)}`}
                      </span>
                      {isSelected(a.id) && <span className="check-mark">✅</span>}
                    </div>
                  </div>
                  <h3>{a.name}</h3>
                  <div className="card-details">
                    <p>🚇 {formatStation(a.nearestMetroStation)}</p>
                    <p>⏱️ {formatDuration(a.estimatedVisitDurationMinutes)}</p>
                    <p>🕐 {getTodayHours(a.openingHours)}</p>
                    {a.accessibilityFriendly && <p>♿ Accessible</p>}
                  </div>
                  <div className="card-tags">
                    {a.tags.map(tag => (
                      <span key={tag} className="tag">{tag}</span>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          )}
        </>
      )}
    </div>
  )
}