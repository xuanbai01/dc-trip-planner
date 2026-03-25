package com.dcplanner.algorithm;

/**
 * Calculates DC Metro fare based on number of stops traversed.
 *
 * Simplified model based on WMATA's distance-based fare structure:
 *   1-2 stops  → $2.25  (short trip)
 *   3-4 stops  → $2.85  (medium trip)
 *   5-6 stops  → $3.45  (longer trip)
 *   7+ stops   → $4.00  (cross-system trip)
 *
 * This is applied once per journey, not per hop.
 */
public class MetroFareCalculator {

    private MetroFareCalculator() {}

    /**
     * Calculate fare for a journey given the number of stations in the path.
     * Note: pathSize includes both origin and destination stations.
     * A path of ["A", "B"] = 1 hop = 2 stations.
     */
    public static double calculate(int pathSize) {
        int hops = Math.max(0, pathSize - 1);

        if (hops == 0) return 0.0;
        if (hops <= 2) return 2.25;
        if (hops <= 4) return 2.85;
        if (hops <= 6) return 3.45;
        return 4.00;
    }
}
