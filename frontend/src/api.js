/**
 * Centralized API calls to Shawn's Javalin backend.
 * Backend runs at http://localhost:7070
 */
const BASE_URL = 'http://localhost:7070'

export async function getAttractions(filters = {}) {
  const params = new URLSearchParams()
  if (filters.type) params.set('type', filters.type)
  if (filters.maxFee !== undefined) params.set('maxFee', filters.maxFee)
  if (filters.accessible) params.set('accessible', 'true')

  const res = await fetch(`${BASE_URL}/attractions?${params}`)
  if (!res.ok) throw new Error('Failed to fetch attractions')
  return res.json()
}

export async function getMetroStations() {
  const res = await fetch(`${BASE_URL}/metro/stations`)
  if (!res.ok) throw new Error('Failed to fetch stations')
  return res.json()
}

export async function getMetroRoute(from, to) {
  const res = await fetch(`${BASE_URL}/metro/route?from=${from}&to=${to}`)
  if (!res.ok) throw new Error('Failed to fetch route')
  return res.json()
}

export async function planTrip(tripRequest) {
  const res = await fetch(`${BASE_URL}/trip/plan`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(tripRequest)
  })
  if (!res.ok) {
    const err = await res.json()
    throw new Error(err.error || 'Failed to plan trip')
  }
  return res.json()
}

export async function getTripCost(attractionIds, startStation) {
  const ids = attractionIds.join(',')
  const res = await fetch(
    `${BASE_URL}/trip/cost?attractionIds=${ids}&startStation=${startStation}`
  )
  if (!res.ok) throw new Error('Failed to fetch cost')
  return res.json()
}