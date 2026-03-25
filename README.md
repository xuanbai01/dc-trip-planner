# dc-trip-planner

Readme · MD
Copy

# DC Weekend Trip Planner
 
A Java-based trip planning tool that generates optimized day-by-day itineraries for Washington DC. Users input their starting Metro station, available time, and preferences to receive a customized plan with routes, time blocks, and cost estimates.
 
## Tech Stack
 
- **Java 17**
- **Javalin** — embedded REST server
- **Jackson** — JSON parsing
- **Maven** — build tool
- Data stored as static JSON files (no external database)
 
## Getting Started
 
**Prerequisites:** Java 17+, Maven
 
```bash
# Clone the repo
git clone https://github.com/<your-username>/dc-trip-planner.git
cd dc-trip-planner
 
# Build
mvn compile
 
# Run
mvn exec:java -Dexec.mainClass="com.dcplanner.Main"
```
 
Server starts on `http://localhost:7070`.
 
## API Endpoints
 
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/health` | Health check |
| GET | `/attractions` | List all attractions |
| GET | `/attractions/{id}` | Get attraction by ID |
| GET | `/metro/stations` | List all Metro stations |
| GET | `/metro/route?from={id}&to={id}` | Shortest Metro route between two stations |
| POST | `/trip/plan` | Generate a full itinerary |
| GET | `/trip/cost?attractionIds={ids}&startStation={id}` | Estimate trip cost |
 
### Filter Attractions
 
```
GET /attractions?type=museum&maxFee=0&accessible=true
```
 
Supported types: `museum`, `landmark`, `coffee_shop`, `cinema`
 
### Plan a Trip
 
```bash
curl -X POST http://localhost:7070/trip/plan \
  -H "Content-Type: application/json" \
  -d '{
    "startLocation": { "metroStation": "union_station" },
    "tripDuration": "1_day",
    "availableTimePerDay": { "start": "09:00", "end": "18:00" },
    "preferences": {
      "types": ["museum", "landmark"],
      "maxBudget": 20.0,
      "accessibilityRequired": false,
      "optimizationStrategy": "maximize_attractions"
    }
  }'
```
 
`tripDuration` options: `half_day`, `1_day`, `2_days`
 
`optimizationStrategy` options:
- `minimize_travel_time` — greedy nearest-neighbor, minimizes Metro travel between stops
- `maximize_attractions` — fits as many preferred stops as possible into the day
 
## Project Structure
 
```
src/main/java/com/dcplanner/
├── Main.java
├── algorithm/       # MetroGraph, Dijkstra
├── controller/      # Javalin route handlers
├── model/           # POJOs (Attraction, Itinerary, TripRequest, ...)
├── repository/      # JSON file loaders
├── service/         # Business logic
└── strategy/        # Optimization strategies
 
src/main/resources/data/
├── attractions.json
├── metro_stations.json
└── metro_edges.json
```
 
## Authors
 
- **Shawn** — Backend (routing, itinerary generation, REST API)
 
