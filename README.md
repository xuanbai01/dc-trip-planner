# DC Weekend Trip Planner

A Java-based trip planning application that helps users design optimized half-day to 2-day itineraries in Washington DC. The system generates routes across museums, landmarks, coffee shops, and cinemas — factoring in opening hours, Metro travel time, and user preferences.

---

## Team

| Name | Role | Responsibilities |
|---|---|---|
| Shawn | Backend | Routing algorithm, itinerary generation, REST API |
| [Teammate] | Frontend | Java Swing UI, map view |

---

## Architecture Overview

```
┌─────────────────────────────────────┐
│         Java Swing Frontend          │
│   [Trip form, itinerary view, map]   │
└────────────────┬────────────────────┘
                 │ HTTP
┌────────────────▼────────────────────┐
│         Javalin REST API             │
│         (localhost:7070)             │
└────────────────┬────────────────────┘
                 │
┌────────────────▼────────────────────┐
│           Service Layer              │
│  TripPlannerService  RouteService    │
│  AttractionService   CostService     │
└────────────────┬────────────────────┘
                 │
┌────────────────▼────────────────────┐
│        Algorithm & Data Layer        │
│  MetroGraph  Dijkstra  JSON files    │
└─────────────────────────────────────┘
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Frontend | Java Swing *(in progress)* |
| REST Server | Javalin 6 |
| JSON Parsing | Jackson |
| Build Tool | Maven |
| Map View | JXMapViewer2 *(planned)* |
| Database | Static JSON files |

---

## Getting Started

**Prerequisites:** Java 17+, Maven 3.6+

```bash
# Clone the repo
git clone https://github.com/xuanbai01/dc-trip-planner.git
cd dc-trip-planner

# Build
mvn compile

# Run the backend server
mvn exec:java -Dexec.mainClass="com.dcplanner.Main"
```

Backend starts on `http://localhost:7070`.

> **Frontend:** Instructions will be added once the Swing UI is implemented.

---

## Features

### Implemented
- REST API with 7 endpoints
- Metro routing via Dijkstra's algorithm on a weighted graph
- Day-by-day itinerary generation with time blocks
- Two optimization strategies: minimize travel time and maximize attractions
- Opening hours validation per day of week
- Metro fare calculation based on trip distance
- Cost breakdown: Metro fares + entrance fees
- Attraction filtering by type, budget, and accessibility
- 30 pre-curated DC attractions across 4 categories
- Input validation and centralized error handling

### In Progress
- Java Swing frontend UI
- Interactive map with attraction pins (JXMapViewer2)

### Planned
- User-facing trip form (start station, date, time, preferences)
- Itinerary display panel with time blocks
- Printable/exportable trip summary

---

## Project Structure

```
dc-trip-planner/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/com/dcplanner/
    │   │   ├── Main.java                  # Entry point, server bootstrap
    │   │   ├── algorithm/                 # MetroGraph, Dijkstra, MetroFareCalculator
    │   │   ├── controller/                # REST route handlers, validation, error handling
    │   │   ├── factory/                   # AttractionFactory (Factory pattern)
    │   │   ├── model/                     # POJOs: Attraction, Itinerary, TripRequest, ...
    │   │   ├── repository/                # JSON file loaders
    │   │   ├── service/                   # Business logic layer
    │   │   └── strategy/                  # Optimization strategies (Strategy pattern)
    │   └── resources/data/
    │       ├── attractions.json           # 30 DC attractions
    │       ├── metro_stations.json        # 11 Metro stations
    │       └── metro_edges.json           # Bidirectional travel time graph
    └── test/
        └── java/com/dcplanner/
            ├── algorithm/                 # DijkstraTest, MetroFareCalculatorTest
            ├── controller/                # TripRequestValidatorTest
            ├── factory/                   # AttractionFactoryTest
            └── service/                   # TripPlannerServiceTest
```

---

## Backend API Reference

All responses are JSON. All errors return `{"error": "description"}`.

### `GET /health`
```json
{"status": "ok"}
```

### `GET /attractions`
List attractions with optional filters.

| Param | Type | Description |
|---|---|---|
| `type` | string | Comma-separated: `museum`, `landmark`, `coffee_shop`, `cinema` |
| `maxFee` | decimal | Max entrance fee (`0` for free only) |
| `accessible` | boolean | `true` for accessible attractions only |

### `GET /attractions/{id}`
Get a single attraction by ID.

### `GET /metro/stations`
List all Metro station IDs.

### `GET /metro/route?from={id}&to={id}`
Shortest Metro route between two stations.

**Response:**
```json
{
  "stations": ["union_station", "metro_center", "smithsonian"],
  "travelTimeMinutes": 6,
  "fare": 2.25
}
```

### `POST /trip/plan`
Generate a full day-by-day itinerary.

**Request body:**
```json
{
  "startLocation": { "metroStation": "union_station" },
  "tripDuration": "1_day",
  "tripStartDate": "2026-03-29",
  "availableTimePerDay": { "start": "09:00", "end": "18:00" },
  "preferences": {
    "types": ["museum", "landmark"],
    "maxBudget": 30.0,
    "accessibilityRequired": false,
    "optimizationStrategy": "maximize_attractions"
  }
}
```

| Field | Required | Values |
|---|---|---|
| `startLocation.metroStation` | yes | Any valid station ID |
| `tripDuration` | yes | `half_day`, `1_day`, `2_days` |
| `tripStartDate` | no | `YYYY-MM-DD` — defaults to Saturday if omitted |
| `availableTimePerDay.start` | yes | `HH:mm` |
| `availableTimePerDay.end` | yes | `HH:mm` |
| `preferences.types` | no | `museum`, `landmark`, `coffee_shop`, `cinema` |
| `preferences.maxBudget` | no | Decimal USD |
| `preferences.accessibilityRequired` | no | `true` / `false` |
| `preferences.optimizationStrategy` | no | `minimize_travel_time`, `maximize_attractions` |

### `GET /trip/cost?attractionIds={ids}&startStation={id}`
Estimate cost for an ordered list of attraction IDs.

**Response:**
```json
{
  "metroFare": 5.70,
  "entranceFees": 0.00,
  "totalCost": 5.70,
  "warnings": []
}
```

---

## Metro Stations Reference

| ID | Name | Lines |
|---|---|---|
| `union_station` | Union Station | Red |
| `judiciary_sq` | Judiciary Square | Red |
| `gallery_place` | Gallery Place | Red, Green, Yellow |
| `metro_center` | Metro Center | Red, Blue, Orange, Silver |
| `archives` | Archives | Green, Yellow |
| `l_enfant_plaza` | L'Enfant Plaza | Blue, Orange, Silver, Green, Yellow |
| `smithsonian` | Smithsonian | Blue, Orange, Silver |
| `capitol_south` | Capitol South | Blue, Orange, Silver |
| `eastern_market` | Eastern Market | Blue, Orange, Silver |
| `foggy_bottom` | Foggy Bottom | Blue, Orange, Silver |
| `woodley_park` | Woodley Park | Red |

---

## Design Patterns Used

| Pattern | Where |
|---|---|
| **Strategy** | `OptimizationStrategy` — swappable itinerary algorithms |
| **Factory** | `AttractionFactory` — typed construction and validation of attractions |
| **MVC** | Controllers handle HTTP, Services handle logic, Repositories handle data |
| **Observer** | *(planned for frontend itinerary update events)* |

---

## Running Tests

```bash
mvn test
```

43 unit and integration tests across:
- `DijkstraTest` — shortest path correctness
- `MetroFareCalculatorTest` — fare tier logic
- `AttractionFactoryTest` — validation and normalization
- `TripRequestValidatorTest` — API input validation
- `TripPlannerServiceTest` — end-to-end itinerary generation

---

## Frontend *(in progress)*

> This section will be updated by [Teammate] once the Swing UI is implemented.

Planned UI screens:
- **Trip Setup Form** — input start station, date, time window, and preferences
- **Itinerary View** — day-by-day time blocks with attraction details and travel info
- **Map View** — DC map with pinned attractions using JXMapViewer2
- **Cost Summary** — Metro fare and entrance fee breakdown

---

## Known Limitations

- Attraction data is pre-curated and static — no live data or real-time events
- Metro fare uses a simplified distance-based model, not WMATA's exact pricing
- Opening hours are manually entered and may not reflect holiday schedules
