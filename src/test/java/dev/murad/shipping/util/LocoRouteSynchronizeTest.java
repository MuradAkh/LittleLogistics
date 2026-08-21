package dev.murad.shipping.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises {@link LocoRoute#synchronize} — the position-recovery logic a locomotive runs after
 * a chunk reload. The interesting cases are routes that cross the same rail, in the same travel
 * direction, at more than one point in the loop, where position + direction alone are ambiguous
 * and the persisted progress must break the tie.
 */
class LocoRouteSynchronizeTest {
    private static final BlockPos SHARED = new BlockPos(5, 64, 0);

    /**
     * A usable two-node loop whose outbound and return segments BOTH cross {@link #SHARED} heading
     * EAST. Occurrence in segment 0 is at ordinal 1; the occurrence in segment 1 is at ordinal 4.
     */
    private static LocoRoute ambiguousRoute() {
        LocoRouteSegment outbound = new LocoRouteSegment(List.of(
            new LocoRouteStep(new BlockPos(1, 64, 0), Direction.EAST, Direction.EAST),
            new LocoRouteStep(SHARED, Direction.EAST, Direction.EAST),          // ordinal 1
            new LocoRouteStep(new BlockPos(6, 64, 0), Direction.EAST, Direction.EAST)
        ), Direction.EAST);
        LocoRouteSegment inbound = new LocoRouteSegment(List.of(
            new LocoRouteStep(new BlockPos(20, 64, 0), Direction.EAST, Direction.EAST),
            new LocoRouteStep(SHARED, Direction.EAST, Direction.EAST),          // ordinal 4
            new LocoRouteStep(new BlockPos(25, 64, 0), Direction.EAST, Direction.EAST)
        ), Direction.EAST);
        return new LocoRoute(null, null, List.of(
            new LocoRouteNode(null, 1, 64, 0),
            new LocoRouteNode(null, 20, 64, 0)
        ), List.of(outbound, inbound), LocoRoute.State.COMPLETE, -1);
    }

    @Test
    void ambiguousRailWithoutAnchorFallsBackToFirstMatch() {
        LocoRoute route = ambiguousRoute();

        LocoRoute.RoutePosition position =
            route.synchronize(SHARED, Direction.EAST, Optional.empty()).orElseThrow();

        // This is the pre-fix behavior: with nothing to anchor to, the earliest crossing wins.
        assertEquals(new LocoRoute.RoutePosition(0, 1), position);
    }

    @Test
    void anchorNearSecondCrossingRecoversSecondCrossing() {
        LocoRoute route = ambiguousRoute();

        // The train was persisted partway through the return segment. The naive first-match would
        // teleport its progress back to segment 0 (the reported "0/n" / lost-progress bug); anchoring
        // must keep it on segment 1.
        LocoRoute.RoutePosition anchor = new LocoRoute.RoutePosition(1, 0);
        LocoRoute.RoutePosition position =
            route.synchronize(SHARED, Direction.EAST, Optional.of(anchor)).orElseThrow();

        assertEquals(new LocoRoute.RoutePosition(1, 1), position);
    }

    @Test
    void anchorNearFirstCrossingRecoversFirstCrossing() {
        LocoRoute route = ambiguousRoute();

        LocoRoute.RoutePosition anchor = new LocoRoute.RoutePosition(0, 0);
        LocoRoute.RoutePosition position =
            route.synchronize(SHARED, Direction.EAST, Optional.of(anchor)).orElseThrow();

        assertEquals(new LocoRoute.RoutePosition(0, 1), position);
    }

    @Test
    void anchorExactlyAtPersistedPositionIsAlwaysPreferred() {
        LocoRoute route = ambiguousRoute();

        // Exact match => cyclic distance 0, which must beat every other crossing regardless of order.
        LocoRoute.RoutePosition anchor = new LocoRoute.RoutePosition(1, 1);
        LocoRoute.RoutePosition position =
            route.synchronize(SHARED, Direction.EAST, Optional.of(anchor)).orElseThrow();

        assertEquals(new LocoRoute.RoutePosition(1, 1), position);
    }

    @Test
    void anchorTiebreakUsesCyclicDistanceAcrossTheLoopSeam() {
        LocoRoute route = ambiguousRoute();
        int total = 6; // three steps in each of the two segments

        // Anchor sitting just past the end of the loop is cyclically closer to the LATE crossing
        // (ordinal 4) than to the EARLY one (ordinal 1), even though 1 comes first in scan order.
        LocoRoute.RoutePosition anchor = new LocoRoute.RoutePosition(1, 2); // ordinal 5
        LocoRoute.RoutePosition position =
            route.synchronize(SHARED, Direction.EAST, Optional.of(anchor)).orElseThrow();

        assertEquals(new LocoRoute.RoutePosition(1, 1), position);
        // Sanity on the seam arithmetic the resolver relies on.
        assertTrue(Math.min(Math.floorMod(4 - 5, total), total - Math.floorMod(4 - 5, total))
                 < Math.min(Math.floorMod(1 - 5, total), total - Math.floorMod(1 - 5, total)));
    }

    @Test
    void oppositeDirectionCrossingsAreNeverAmbiguous() {
        // Same rail traversed EAST in segment 0 and WEST in segment 1: direction alone disambiguates,
        // so the anchor is irrelevant and both directions resolve to their unique crossing.
        LocoRouteSegment outbound = new LocoRouteSegment(List.of(
            new LocoRouteStep(SHARED, Direction.EAST, Direction.EAST)
        ), Direction.EAST);
        LocoRouteSegment inbound = new LocoRouteSegment(List.of(
            new LocoRouteStep(SHARED, Direction.WEST, Direction.WEST)
        ), Direction.WEST);
        LocoRoute route = new LocoRoute(null, null, List.of(
            new LocoRouteNode(null, 5, 64, 0),
            new LocoRouteNode(null, 6, 64, 0)
        ), List.of(outbound, inbound), LocoRoute.State.COMPLETE, -1);

        assertEquals(new LocoRoute.RoutePosition(0, 0),
            route.synchronize(SHARED, Direction.EAST, Optional.of(new LocoRoute.RoutePosition(1, 0))).orElseThrow());
        assertEquals(new LocoRoute.RoutePosition(1, 0),
            route.synchronize(SHARED, Direction.WEST, Optional.of(new LocoRoute.RoutePosition(0, 0))).orElseThrow());
    }

    @Test
    void unmatchedRailOrDirectionYieldsNoPosition() {
        LocoRoute route = ambiguousRoute();

        assertTrue(route.synchronize(new BlockPos(99, 64, 99), Direction.EAST, Optional.empty()).isEmpty());
        assertTrue(route.synchronize(SHARED, Direction.NORTH, Optional.empty()).isEmpty());
    }

    @Test
    void incompleteRouteIsNeverSynchronizable() {
        LocoRoute route = new LocoRoute(null, null, List.of(
            new LocoRouteNode(null, 0, 64, 0),
            new LocoRouteNode(null, 1, 64, 0)
        ), List.of(), LocoRoute.State.IN_PROGRESS, 2);

        assertTrue(route.synchronize(new BlockPos(0, 64, 0), Direction.EAST, Optional.empty()).isEmpty());
    }

    @Test
    void trainParkedAtWaypointSynchronizesToSegmentStart() {
        // No step sits on the waypoint block itself; the loco is parked exactly on a node about to
        // enter the next segment. With no step match, the waypoint fallback must resolve a position.
        LocoRouteSegment first = new LocoRouteSegment(List.of(
            new LocoRouteStep(new BlockPos(0, 64, 0), Direction.EAST, Direction.EAST)
        ), Direction.EAST);
        LocoRouteSegment second = new LocoRouteSegment(List.of(
            new LocoRouteStep(new BlockPos(9, 64, 0), Direction.WEST, Direction.WEST)
        ), Direction.WEST);
        LocoRoute route = new LocoRoute(null, null, List.of(
            new LocoRouteNode(null, 0, 64, 0),
            new LocoRouteNode(null, 10, 64, 0)   // waypoint the train is parked on, not on any step
        ), List.of(first, second), LocoRoute.State.COMPLETE, -1);

        // Sanity: no step covers the parked rail, so this can only resolve via the waypoint fallback.
        assertTrue(route.synchronize(new BlockPos(10, 64, 0), Direction.EAST, Optional.empty()).isEmpty());

        LocoRoute.RoutePosition position =
            route.synchronize(new BlockPos(10, 64, 0), Direction.WEST, Optional.empty()).orElseThrow();

        assertEquals(new LocoRoute.RoutePosition(1, 0), position);
    }
}
