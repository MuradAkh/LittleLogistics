package dev.murad.shipping.util;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TugRouteCompilerHeadingTest {
    @Test
    void diagonalHeadingsMoveAcrossBothHorizontalAxes() {
        BlockPos start = new BlockPos(4, 62, 9);

        assertEquals(new BlockPos(5, 62, 8), TugRouteCompiler.RouteHeading.NORTH_EAST.move(start));
        assertEquals(new BlockPos(5, 62, 10), TugRouteCompiler.RouteHeading.SOUTH_EAST.move(start));
        assertEquals(new BlockPos(3, 62, 10), TugRouteCompiler.RouteHeading.SOUTH_WEST.move(start));
        assertEquals(new BlockPos(3, 62, 8), TugRouteCompiler.RouteHeading.NORTH_WEST.move(start));
    }

    @Test
    void diagonalHeadingsHaveAnOppositeAndEuclideanStepCost() {
        TugRouteCompiler.RouteHeading heading = TugRouteCompiler.RouteHeading.NORTH_EAST;

        assertEquals(TugRouteCompiler.RouteHeading.SOUTH_WEST, heading.opposite());
        assertEquals(Math.sqrt(2.0D), heading.travelCost());
        assertTrue(heading.isDiagonal());
    }

    @Test
    void headingTransitionsAllowAtMost45DegreesPerStep() {
        // 45-degree turns and straight-ahead are permitted...
        assertTrue(TugRouteCompiler.isHeadingTransitionAllowed(
            TugRouteCompiler.RouteHeading.NORTH, TugRouteCompiler.RouteHeading.NORTH_WEST));
        assertTrue(TugRouteCompiler.isHeadingTransitionAllowed(
            TugRouteCompiler.RouteHeading.NORTH_EAST, TugRouteCompiler.RouteHeading.NORTH));
        assertTrue(TugRouteCompiler.isHeadingTransitionAllowed(
            TugRouteCompiler.RouteHeading.NORTH, TugRouteCompiler.RouteHeading.NORTH));

        // ...but anything sharper (90, 135, 180 degrees) is rejected; the route must bend
        // gradually through several chained 45-degree steps instead.
        assertFalse(TugRouteCompiler.isHeadingTransitionAllowed(
            TugRouteCompiler.RouteHeading.NORTH, TugRouteCompiler.RouteHeading.WEST));
        assertFalse(TugRouteCompiler.isHeadingTransitionAllowed(
            TugRouteCompiler.RouteHeading.NORTH, TugRouteCompiler.RouteHeading.SOUTH_WEST));
        assertFalse(TugRouteCompiler.isHeadingTransitionAllowed(
            TugRouteCompiler.RouteHeading.NORTH, TugRouteCompiler.RouteHeading.SOUTH));
    }

    @Test
    void ninetyDegreeTurnsAreCheaperThanOneHundredThirtyFiveDegreeTurns() {
        assertEquals(0.7D, TugRouteCompiler.getTurnPenalty(
            TugRouteCompiler.RouteHeading.NORTH, TugRouteCompiler.RouteHeading.NORTH_WEST));
        assertEquals(0.35D, TugRouteCompiler.getTurnPenalty(
            TugRouteCompiler.RouteHeading.NORTH, TugRouteCompiler.RouteHeading.WEST));
        assertEquals(1.4D, TugRouteCompiler.getTurnPenalty(
            TugRouteCompiler.RouteHeading.NORTH, TugRouteCompiler.RouteHeading.SOUTH_WEST));
    }
}
