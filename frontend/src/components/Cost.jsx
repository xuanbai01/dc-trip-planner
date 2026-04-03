/**
 * Displays the cost breakdown for the trip.
 * Data comes from the itinerary response or GET /trip/cost endpoint.
 */
export default function Cost({ itinerary }) {
  if (!itinerary?.costBreakdown) return null

  const { metroFare, entranceFees, totalCost } = itinerary.costBreakdown

  return (
    <div className="cost-section">
      <h2>💰 Cost Breakdown</h2>
      <div className="cost-cards">
        <div className="cost-card">
          <span className="cost-icon">🚇</span>
          <span className="cost-label">Metro Fares</span>
          <span className="cost-value">${metroFare?.toFixed(2) || '0.00'}</span>
        </div>
        <div className="cost-card">
          <span className="cost-icon">🎟️</span>
          <span className="cost-label">Entrance Fees</span>
          <span className="cost-value">${entranceFees?.toFixed(2) || '0.00'}</span>
        </div>
        <div className="cost-card total">
          <span className="cost-icon">💵</span>
          <span className="cost-label">Total</span>
          <span className="cost-value">${totalCost?.toFixed(2) || '0.00'}</span>
        </div>
      </div>
    </div>
  )
}