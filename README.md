# DC Weekend Trip Planner

A Java-based trip planning application that helps users design optimized half-day to 2-day itineraries in Washington DC. The system generates routes across museums, landmarks, coffee shops, and cinemas — factoring in opening hours, Metro travel time, and user preferences.
![Frontend UI](frontend/src/assets/frontend_sreenshot.png)

---

## Team

| Name | Role | Responsibilities |
|---|---|---|
| Shawn | Backend | Routing algorithm, itinerary generation, REST API |
| Xiang | Frontend | React UI, map view, user interaction |


---

## Architecture Overview

```
┌─────────────────────────────────────┐
│        React + Vite Frontend         │
│  [Trip form, attraction picker,      │
│   Leaflet map, itinerary view]       │
│       (localhost:5173)               │
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
| Language | Java 17, JavaScript |
| Frontend | React + Vite |
| Map | Leaflet + OpenStreetMap |
| REST Server | Javalin 6 |
| JSON Parsing | Jackson |
| Build Tool | Maven |
| Database | Static JSON files |

---

## Getting Started

**Prerequisites:** Java 17+, Maven 3.6+, Node.js 18+

```bash
# Clone the repo
git clone https://github.com/xuanbai01/dc-trip-planner.git
cd dc-trip-planner

# Start backend:
bashmvn compile
mvn exec:java -Dexec.mainClass="com.dcplanner.Main"
Backend starts on http://localhost:7070.

#Start frontend
cd frontend
npm install
npm run dev
Frontend starts on http://localhost:5173
```

> **Frontend:** Instructions will be added once the Swing UI is implemented.

---

## Features

### Implemented
backend(Shawn)
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

frontend(Xiang)
- Trip setup form with station, duration, date, time, and preference selection
- Interactive attraction picker with type filtering, free-only filter, and accessibility filter
- Click-to-select attractions with visual feedback and collapsible panel
- Auto-plan mode (by preferences) and manual mode (by selected attractions)
- Leaflet map with emoji markers, numbered stops, route polyline, and start station
- Day-by-day itinerary display with time blocks
- Cost breakdown summary (Metro fares + entrance fees)
- Auto-scroll to results after planning
- Responsive design for desktop and mobile
---

## Project Structure

```
dc-trip-planner/
├── pom.xml
├── README.md
└── src/
│    ├── main/
│    │   ├── java/com/dcplanner/
│    │   │   ├── Main.java                  # Entry point, server bootstrap
│    │   │   ├── algorithm/                 # MetroGraph, Dijkstra, MetroFareCalculator
│    │   │   ├── controller/                # REST route handlers, validation, error handling
│    │   │   ├── factory/                   # AttractionFactory (Factory pattern)
│    │   │   ├── model/                     # POJOs: Attraction, Itinerary, TripRequest, ...
│    │   │   ├── repository/                # JSON file loaders
│    │   │   ├── service/                   # Business logic layer
│    │   │   └── strategy/                  # Optimization strategies (Strategy pattern)
│    │   └── resources/data/
│    │       ├── attractions.json           # 30 DC attractions
│    │       ├── metro_stations.json        # 11 Metro stations
│    │       └── metro_edges.json           # Bidirectional travel time graph
│    └── test/
│        └── java/com/dcplanner/
│            ├── algorithm/                 # DijkstraTest, MetroFareCalculatorTest
│            ├── controller/                # TripRequestValidatorTest
│            ├── factory/                   # AttractionFactoryTest
│            └── service/                   # TripPlannerServiceTest
│
└── frontend/                              
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── App.jsx                        # Main app, state management
        ├── App.css                        # Global styles
        ├── assets/                        # Background image
        ├── components/
        │   ├── TripForm.jsx               # Search bar with trip preferences
        │   ├── AttractionPicker.jsx       # Clickable attraction cards
        │   ├── MapView.jsx                # Leaflet map with route display
        │   ├── Itinerary.jsx              # Day-by-day time blocks
        │   ├── Cost.jsx                   # Cost breakdown
        │   └── Footer.jsx                 # Credits and links
        └── services/
            └── api.js                     # Backend API calls
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
## Frontend Reference

### User Flow

1. **Set Preferences** — Top search bar: select start station, 
   duration, date, time range, attraction types, budget, strategy
2. **Browse & Select** — Scrollable attraction cards filtered by 
   selected types. Click to select specific destinations
3. **Plan Trip** — Two modes:
   - "Auto Plan": no attractions selected → calls `POST /trip/plan`
   - "Plan N Places": attractions selected → calls `GET /trip/cost` + `GET /metro/route`
4. **View Results** — Attraction picker collapses, page scrolls to:
   - Route map with numbered emoji markers
   - Day-by-day itinerary with time blocks
   - Cost breakdown (Metro + entrance fees)

### Components

| Component | File | Purpose |
|---|---|---|
| TripForm | `TripForm.jsx` | Search bar with all trip preferences |
| AttractionPicker | `AttractionPicker.jsx` | Filterable, clickable attraction cards with collapse/expand |
| MapView | `MapView.jsx` | Leaflet map with emoji markers, route line, start station |
| Itinerary | `Itinerary.jsx` | Time blocks showing visit order, travel time, fees |
| Cost | `Cost.jsx` | Metro fare + entrance fee + total breakdown |
| Footer | `Footer.jsx` | Credits and GitHub link |

### API Usage by Component

| Component | Endpoint Called | When |
|---|---|---|
| TripForm | `GET /metro/stations` | On page load, populates station dropdown |
| AttractionPicker | `GET /attractions` | On page load, populates attraction cards |
| App (Auto Plan) | `POST /trip/plan` | User clicks Plan with no specific selections |
| App (Manual Plan) | `GET /trip/cost` | User clicks Plan with selected attractions |
| App (Manual Plan) | `GET /metro/route` | Per consecutive stop pair, calculates travel time |

### State Management

| State | Owned By | Shared With |
|---|---|---|
| `selectedTypes` | App | TripForm, AttractionPicker |
| `selectedAttractions` | App | AttractionPicker, MapView |
| `itinerary` | App | MapView, Itinerary, Cost |
| `pickerOpen` | App | AttractionPicker |

---

## Known Limitations

- Attraction data is pre-curated and static — no live data or real-time events
- Metro fare uses a simplified distance-based model, not WMATA's exact pricing
- Opening hours are manually entered and may not reflect holiday schedules
