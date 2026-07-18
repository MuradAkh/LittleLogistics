package dev.murad.shipping.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocoRouteTest {
    @Test
    void roundTripPreservesOrderedNodesAndCompiledSegments() {
        LocoRouteSegment first = new LocoRouteSegment(List.of(
            new LocoRouteStep(new BlockPos(1, 64, 1), Direction.EAST, Direction.EAST),
            new LocoRouteStep(new BlockPos(2, 64, 1), Direction.EAST, Direction.SOUTH)
        ), Direction.SOUTH);
        LocoRouteSegment second = new LocoRouteSegment(List.of(
            new LocoRouteStep(new BlockPos(2, 64, 2), Direction.SOUTH, Direction.WEST)
        ), Direction.WEST);
        LocoRoute route = new LocoRoute(null, "minecraft:overworld", List.of(
            new LocoRouteNode(null, 1, 64, 1),
            new LocoRouteNode(null, 2, 64, 2)
        ), List.of(first, second), LocoRoute.State.COMPLETE, -1);

        CompoundTag nbt = route.toNBT();
        LocoRoute decoded = LocoRoute.fromNBT(nbt);

        assertEquals(route, decoded);
        assertTrue(decoded.isUsable());
        assertEquals(new BlockPos(1, 64, 1), decoded.getFirst().toBlockPos());
        assertEquals(Direction.SOUTH, decoded.getSegments().getFirst().getArrivalDirection());
    }

    @Test
    void inProgressRouteIsNeverUsable() {
        LocoRoute route = new LocoRoute(null, null, List.of(
            new LocoRouteNode(null, 0, 64, 0),
            new LocoRouteNode(null, 1, 64, 0)
        ), List.of(), LocoRoute.State.IN_PROGRESS, 2);

        assertTrue(route.isInProgress());
        assertFalse(route.isUsable());
    }

    @Test
    void copyIsIndependentOfSegmentList() {
        LocoRoute route = new LocoRoute(null, null, List.of(
            new LocoRouteNode(null, 0, 64, 0),
            new LocoRouteNode(null, 1, 64, 0)
        ), List.of(new LocoRouteSegment(List.of(
            new LocoRouteStep(new BlockPos(0, 64, 0), Direction.EAST, Direction.EAST)
        ), Direction.EAST)), LocoRoute.State.IN_PROGRESS, 2);

        LocoRoute copy = route.copy();
        copy.getSegments().clear();

        assertEquals(1, route.getSegments().size());
        assertEquals(0, copy.getSegments().size());
    }

    @Test
    void selectedClosingSegmentRemainsAnInsertionEdit() {
        LocoRoute route = new LocoRoute(null, null, List.of(
            new LocoRouteNode(null, 0, 64, 0),
            new LocoRouteNode(null, 1, 64, 0)
        ), List.of(
            new LocoRouteSegment(List.of(new LocoRouteStep(new BlockPos(0, 64, 0), Direction.EAST, Direction.EAST)), Direction.EAST),
            new LocoRouteSegment(List.of(new LocoRouteStep(new BlockPos(1, 64, 0), Direction.WEST, Direction.WEST)), Direction.WEST)
        ), LocoRoute.State.COMPLETE, -1);

        route.beginInsertion(route.size());

        assertTrue(route.isInserting());
        assertEquals(route.size(), route.getNextInsertionIndex());
    }

    @Test
    void splitCompiledSegmentRetainsTraversalDirections() {
        LocoRouteSegment original = new LocoRouteSegment(List.of(
            new LocoRouteStep(new BlockPos(0, 64, 0), Direction.EAST, Direction.EAST),
            new LocoRouteStep(new BlockPos(1, 64, 0), Direction.EAST, Direction.SOUTH),
            new LocoRouteStep(new BlockPos(1, 64, 1), Direction.SOUTH, Direction.SOUTH)
        ), Direction.SOUTH);

        LocoRouteCompiler.SegmentSplit split = LocoRouteCompiler.splitAt(original, new BlockPos(1, 64, 0)).orElseThrow();

        assertEquals(1, split.before().getSteps().size());
        assertEquals(Direction.EAST, split.before().getArrivalDirection());
        assertEquals(2, split.after().getSteps().size());
        assertEquals(Direction.SOUTH, split.after().getArrivalDirection());
        assertEquals(new BlockPos(1, 64, 0), split.after().getSteps().getFirst().railPos());
    }

    @Test
    void upcomingStepsFollowCompiledBranchAcrossSegmentBoundary() {
        LocoRouteStep current = new LocoRouteStep(new BlockPos(0, 64, 0), Direction.EAST, Direction.EAST);
        LocoRouteStep selectedBranch = new LocoRouteStep(new BlockPos(1, 64, 0), Direction.EAST, Direction.NORTH);
        LocoRouteStep afterBranch = new LocoRouteStep(new BlockPos(1, 64, -1), Direction.NORTH, Direction.NORTH);
        LocoRouteStep returnStep = new LocoRouteStep(new BlockPos(1, 64, -2), Direction.NORTH, Direction.SOUTH);
        LocoRoute route = new LocoRoute(null, null, List.of(
            new LocoRouteNode(null, 0, 64, 0),
            new LocoRouteNode(null, 1, 64, -2)
        ), List.of(
            new LocoRouteSegment(List.of(current, selectedBranch, afterBranch), Direction.NORTH),
            new LocoRouteSegment(List.of(returnStep), Direction.SOUTH)
        ), LocoRoute.State.COMPLETE, -1);

        assertEquals(List.of(selectedBranch, afterBranch, returnStep, current),
            route.getUpcomingSteps(0, 1, 4));
    }

    @Test
    void upcomingStepsSkipEmptySegmentsAndRemainBounded() {
        LocoRouteStep onlyStep = new LocoRouteStep(new BlockPos(4, 64, 4), Direction.WEST, Direction.WEST);
        LocoRoute route = new LocoRoute(null, null, List.of(
            new LocoRouteNode(null, 4, 64, 4),
            new LocoRouteNode(null, 3, 64, 4)
        ), List.of(
            new LocoRouteSegment(List.of(), Direction.WEST),
            new LocoRouteSegment(List.of(onlyStep), Direction.WEST)
        ), LocoRoute.State.COMPLETE, -1);

        assertEquals(List.of(onlyStep, onlyStep, onlyStep), route.getUpcomingSteps(0, 0, 3));
        assertTrue(route.getUpcomingSteps(0, 0, 0).isEmpty());
    }
}
