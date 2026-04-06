/**
 * Displays the cost breakdown for the trip.
 * Auto-plan: uses itinerary.totalCost and computes from day data
 * Manual: uses itinerary.costBreakdown from GET /trip/cost
 */
export default function Cost({ itinerary }) {
  if (!itinerary) return null

  let metroFare = 0
  let entranceFees = 0
  let totalCost = 0

  if (itinerary.costBreakdown) {
    // Manual mode: data from GET /trip/cost
    metroFare = itinerary.costBreakdown.metroFare || 0
    entranceFees = itinerary.costBreakdown.entranceFees || 0
    totalCost = itinerary.costBreakdown.totalCost || 0
  } else if (itinerary.totalCost !== undefined) {
    // Auto-plan mode: compute from day data
    totalCost = itinerary.totalCost

    // Sum up travel costs and entrance fees from time blocks
    itinerary.days?.forEach(day => {
      day.timeBlocks?.forEach(block => {
        metroFare += block.travel?.travelCost || 0
        entranceFees += block.attraction?.entranceFee || 0
      })
    })
  } else {
    return null
  }

  return (
    <div className="cost-section">
      <h2>💰 Cost Breakdown</h2>
      <div className="cost-cards">
        <div className="cost-card">
          <span className="cost-icon">🚇</span>
          <span className="cost-label">Metro Fares</span>
          <span className="cost-value">${metroFare.toFixed(2)}</span>
        </div>
        <div className="cost-card">
          <span className="cost-icon">🎟️</span>
          <span className="cost-label">Entrance Fees</span>
          <span className="cost-value">${entranceFees.toFixed(2)}</span>
        </div>
        <div className="cost-card total">
          <span className="cost-icon">💵</span>
          <span className="cost-label">Total</span>
          <span className="cost-value">${totalCost.toFixed(2)}</span>
        </div>
      </div>
      {itinerary.totalAttractions && (
        <p className="cost-note">
          📍 {itinerary.totalAttractions} attractions visited
        </p>
      )}
    </div>
  )
}