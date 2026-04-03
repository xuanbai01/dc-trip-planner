/**
 * Displays the day-by-day itinerary as time block cards.
 * Receives itinerary data from the backend POST /trip/plan response.
 */
export default function Itinerary({ itinerary }) {
  if (!itinerary || !itinerary.days) return null

  return (
    <div className="itinerary-section">
      <h2>📋 Suggest Itinerary</h2>
      {itinerary.days.map((day, dayIndex) => (
        <div key={dayIndex} className="day-card">
          <h3>Day {dayIndex + 1} — {day.date || ''}</h3>
          <div className="timeline">
            {day.timeBlocks && day.timeBlocks.map((block, i) => (
              <div key={i} className="time-block">
                <div className="time-badge">
                  {block.startTime} — {block.endTime}
                </div>
                <div className="block-content">
                  <h4>{block.attractionName || block.activity}</h4>
                  {block.type && <span className="type-tag">{block.type}</span>}
                  {block.travelTimeMinutes > 0 && (
                    <p className="travel-info">
                      🚇 {block.travelTimeMinutes} min by Metro
                    </p>
                  )}
                  {block.entranceFee > 0 && (
                    <p className="fee-info">🎟️ ${block.entranceFee.toFixed(2)}</p>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      ))}
    </div>
  )
}