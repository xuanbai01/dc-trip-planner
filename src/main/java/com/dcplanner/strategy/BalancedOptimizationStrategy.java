package com.dcplanner.strategy;

import com.dcplanner.algorithm.Dijkstra;
import com.dcplanner.model.Attraction;
import com.dcplanner.model.Itinerary;
import com.dcplanner.model.TripRequest;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Strategy: Cluster-First Greedy ("Balanced")
 *
 * Instead of picking one attraction at a time, plan at the metro-station level.
 * Each station is a "cluster" of attractions that share a location. The algorithm:
 *
 *   1. Group all candidate attractions by nearestMetroStation → clusters.
 *   2. At each step, evaluate every unvisited cluster and score it as:
 *
 *         score = totalValueReachable / (travelTimeToCluster + dwellTimeInCluster)
 *
 *      where "reachable" means the attractions that actually fit in the remaining
 *      time window and are open on this day.
 *   3. Commit to the highest-scoring cluster, visit all feasible attractions there
 *      in value-descending then duration-ascending order, then move on.
 *   4. Repeat until no cluster has any feasible attraction.
 *
 * Why this is better than the previous efficiency greedy:
 *
 *   - Matches how humans actually plan DC trips ("do Smithsonian in the morning,
 *     Capitol Hill in the afternoon") — commits to a region instead of ping-ponging.
 *
 *   - Only two tunable constants (BASE + TYPE_MATCH) versus the old five.
 *     No arbitrary RICHNESS_PER_HOUR or FEE_PENALTY fudge factors.
 *
 *   - Clustering behaviour is explicit and documented, not an emergent side effect
 *     of clamping the score denominator.
 *
 *   - Produces itineraries that visibly differ from MinimizeTravelTimeStrategy:
 *     MinimizeTravelTime picks the nearest single attraction at every step, while
 *     cluster-first commits to the densest-value region within reach first.
 *
 * Candidate pool contract (same as before):
 * This strategy must receive the FULL attraction pool (budget + accessibility only,
 * no type filter). Pre-filtering by type eliminates the value variance that makes
 * cluster-level differentiation meaningful. TripPlannerService handles this via
 * filterCandidatesForBalanced().
 */
public class BalancedOptimizationStrategy implements OptimizationStrategy {

    // Every attraction has this floor value so an off-type cluster is still reachable
    // if it happens to sit right on the user's path.
    private static final double BASE_ATTRACTION_VALUE = 1.0;

    // Dominant signal. Sized large enough that a cluster of one matched attraction
    // outranks a cluster of several unmatched ones at similar distance.
    private static final double TYPE_MATCH_BONUS = 10.0;

    @Override
    public List<Itinerary.TimeBlock> buildDaySchedule(
            List<Attraction> candidates,
            TripRequest request,
            Dijkstra dijkstra,
            String dayStartStation,
            int dayOffset
    ) {
        String dayOfWeek = DateUtils.resolveDayOfWeek(request, dayOffset);
        List<Itinerary.TimeBlock> schedule = new ArrayList<>();
        Set<String> exhaustedClusters = new HashSet<>();

        LocalTime currentTime = ScheduleUtils.parseTime(request.getAvailableTimePerDay().getStart());
        LocalTime endTime     = ScheduleUtils.parseTime(request.getAvailableTimePerDay().getEnd());
        String currentStation = dayStartStation;

        List<String> preferredTypes = request.getPreferences() != null
                ? request.getPreferences().getTypes()
                : Collections.emptyList();

        // Group attractions by station — this is the "cluster" structure.
        Map<String, List<Attraction>> clusters = candidates.stream()
                .collect(Collectors.groupingBy(Attraction::getNearestMetroStation));

        while (true) {
            ClusterChoice best = null;

            for (Map.Entry<String, List<Attraction>> entry : clusters.entrySet()) {
                String clusterStation = entry.getKey();
                if (exhaustedClusters.contains(clusterStation)) continue;

                int travelTime = ScheduleUtils.getTravelMinutes(dijkstra, currentStation, clusterStation);
                if (travelTime == 999) continue; // unreachable

                ClusterChoice choice = evaluateCluster(
                        entry.getValue(),
                        preferredTypes,
                        dayOfWeek,
                        currentTime,
                        endTime,
                        travelTime,
                        clusterStation
                );

                if (choice == null) {
                    // No feasible attraction in this cluster — don't re-check it.
                    exhaustedClusters.add(clusterStation);
                    continue;
                }

                if (best == null || choice.score > best.score) {
                    best = choice;
                }
            }

            if (best == null) break;

            // Commit: travel to the winning cluster and visit everything feasible.
            Dijkstra.RouteResult clusterRoute = best.travelTime > 0
                    ? dijkstra.findShortestPath(currentStation, best.station)
                    : null;
            LocalTime arrival = currentTime.plusMinutes(best.travelTime);

            for (int i = 0; i < best.orderedFeasible.size(); i++) {
                Attraction a = best.orderedFeasible.get(i);
                // Only the first attraction in the cluster carries the transit
                // leg. Subsequent ones are same-station, so travel info is null.
                Dijkstra.RouteResult route = (i == 0) ? clusterRoute : null;
                schedule.add(ScheduleUtils.buildTimeBlock(a, arrival, route));
                arrival = arrival.plusMinutes(a.getEstimatedVisitDurationMinutes());
            }

            currentTime    = arrival;
            currentStation = best.station;
            exhaustedClusters.add(best.station);
        }

        return schedule;
    }

    /**
     * Evaluate one cluster from the caller's current position and time.
     *
     * Returns the ordered list of attractions that actually fit in the remaining
     * time window (respecting opening hours), the travel cost to reach the cluster,
     * and a score expressing "value per minute of the whole detour".
     *
     * Within-cluster ordering is value-descending, then duration-ascending:
     *   - Matched attractions first so user intent is served if the cluster has
     *     mixed types and not everything fits.
     *   - Among equally-valued attractions, shorter ones first so more of the
     *     cluster survives the time-window check.
     *
     * Returns null when nothing in the cluster is feasible.
     */
    private ClusterChoice evaluateCluster(
            List<Attraction> clusterAttractions,
            List<String> preferredTypes,
            String dayOfWeek,
            LocalTime currentTime,
            LocalTime endTime,
            int travelTime,
            String clusterStation
    ) {
        List<Attraction> ordered = new ArrayList<>(clusterAttractions);
        ordered.sort(Comparator
                .comparingDouble((Attraction a) -> attractionValue(a, preferredTypes)).reversed()
                .thenComparingInt(Attraction::getEstimatedVisitDurationMinutes));

        LocalTime projectedArrival = currentTime.plusMinutes(travelTime);
        List<Attraction> feasible = new ArrayList<>();
        double clusterValue = 0;
        int dwellTime = 0;

        for (Attraction a : ordered) {
            LocalTime projectedDeparture = projectedArrival.plusMinutes(a.getEstimatedVisitDurationMinutes());
            if (projectedDeparture.isAfter(endTime)) continue;
            if (!ScheduleUtils.isOpen(a, dayOfWeek, projectedArrival, projectedDeparture)) continue;

            feasible.add(a);
            clusterValue += attractionValue(a, preferredTypes);
            dwellTime    += a.getEstimatedVisitDurationMinutes();
            projectedArrival = projectedDeparture;
        }

        if (feasible.isEmpty()) return null;

        // Score: value earned per minute invested in this whole cluster visit.
        // Higher value, shorter transit, and shorter dwell all push the score up.
        double totalTime = Math.max(travelTime + dwellTime, 1);
        double score = clusterValue / totalTime;

        return new ClusterChoice(clusterStation, travelTime, feasible, score);
    }

    /**
     * Per-attraction value — just BASE + TYPE_MATCH. Deliberately simple: the
     * cluster-level aggregation is where the algorithm's intelligence lives, not
     * in a tower of per-attraction fudge factors.
     */
    private double attractionValue(Attraction a, List<String> preferredTypes) {
        double value = BASE_ATTRACTION_VALUE;
        if (preferredTypes != null && !preferredTypes.isEmpty()) {
            String typeName = a.getType().name().toLowerCase();
            if (preferredTypes.stream().anyMatch(t -> t.equalsIgnoreCase(typeName))) {
                value += TYPE_MATCH_BONUS;
            }
        }
        return value;
    }

    /**
     * Snapshot of a single cluster's evaluation — what we'd visit, where, at what
     * cost, and how good the deal is. Immutable; recomputed each outer iteration.
     */
    private record ClusterChoice(
            String station,
            int travelTime,
            List<Attraction> orderedFeasible,
            double score
    ) {}
}
