package com.dcplanner.algorithm;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MetroFareCalculatorTest {

    @Test
    void sameStation_returnsZero() {
        // path of size 1 = no travel
        assertEquals(0.0, MetroFareCalculator.calculate(1));
    }

    @Test
    void zeroPathSize_returnsZero() {
        assertEquals(0.0, MetroFareCalculator.calculate(0));
    }

    @Test
    void oneHop_returnsShortFare() {
        // path = [A, B] → size 2 → 1 hop
        assertEquals(2.25, MetroFareCalculator.calculate(2));
    }

    @Test
    void twoHops_returnsShortFare() {
        // path = [A, B, C] → size 3 → 2 hops
        assertEquals(2.25, MetroFareCalculator.calculate(3));
    }

    @Test
    void threeHops_returnsMediumFare() {
        // size 4 → 3 hops
        assertEquals(2.85, MetroFareCalculator.calculate(4));
    }

    @Test
    void fourHops_returnsMediumFare() {
        // size 5 → 4 hops
        assertEquals(2.85, MetroFareCalculator.calculate(5));
    }

    @Test
    void fiveHops_returnsLongFare() {
        // size 6 → 5 hops
        assertEquals(3.45, MetroFareCalculator.calculate(6));
    }

    @Test
    void sevenHops_returnsCrossSystemFare() {
        // size 8 → 7 hops
        assertEquals(4.00, MetroFareCalculator.calculate(8));
    }
}
