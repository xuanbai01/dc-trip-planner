/**
 * Displays the day-by-day itinerary as time block cards.
 * Handles both auto-plan (from POST /trip/plan) and manual selection mode.
 */

const TYPE_EMOJI = {
  MUSEUM: '🏛️',
  LANDMARK: '🗽',
  COFFEE_SHOP: '☕',
  CINEMA: '🎬'
}

const formatStation = (id) =>
  id?.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase()) || ''

export default function Itinerary({ itinerary }) {
  if (!itinerary || !itinerary.days) return null

  const totalTravel = itinerary.totalTravelTimeMinutes
  const execMs = itinerary.executionTimeMs

  return (
    <div className="itinerary-section">
      <h2>📋 Your Itinerary ({itinerary.totalAttractions || 0} stops)</h2>
      {(totalTravel > 0 || execMs > 0) && (
        <div className="itinerary-metrics">
          {totalTravel > 0 && (
            <span className="metric-chip">🚇 {totalTravel} min total travel</span>
          )}
          {execMs > 0 && (
            <span className="metric-chip">⚡ Planned in {execMs} ms</span>
          )}
        </div>
      )}
      {itinerary.days.map((day, dayIndex) => (
        <div key={dayIndex} className="day-card">
          <h3>
            Day {day.dayNumber || dayIndex + 1}
            {day.date && ` — ${day.date}`}
            {day.dayCost > 0 && ` · $${day.dayCost.toFixed(2)}`}
          </h3>
          <div className="timeline">
            {day.timeBlocks && day.timeBlocks.map((block, i) => {
              // Auto-plan: data nested in block.attraction
              // Manual: data flat on block
              const name = block.attraction?.name || block.attractionName || 'Stop'
              const type = block.attraction?.type || block.type
              const fee = block.attraction?.entranceFee ?? block.entranceFee ?? 0
              const travel = block.travel
              const travelMin = travel?.walkingMinutes || block.travelTimeMinutes || 0
              const travelCost = travel?.travelCost || 0
              const metroStops = travel?.metroStations || []
              const station = block.attraction?.nearestMetroStation || block.nearestStation

              return (
                <div key={i} className="time-block">
                  <div className="time-badge">
                    {block.startTime} — {block.endTime}
                  </div>
                  <div className="block-content">
                    <h4>{TYPE_EMOJI[type] || '📍'} {name}</h4>
                    {type && <span className="type-tag">{type.replace('_', ' ')}</span>}
                    {travelMin > 0 && (
                      <p className="travel-info">
                        🚇 {travelMin} min walk + metro
                        {metroStops.length > 1 && ` (${metroStops.length} stops)`}
                        {travelCost > 0 && ` · $${travelCost.toFixed(2)}`}
                      </p>
                    )}
                    {travelMin === 0 && station && (
                      <p className="travel-info">🚶 Same area as previous stop</p>
                    )}
                    {fee > 0 ? (
                      <p className="fee-info">🎟️ ${fee.toFixed(2)}</p>
                    ) : (
                      <p className="fee-info">🆓 Free</p>
                    )}
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      ))}
    </div>
  )
}