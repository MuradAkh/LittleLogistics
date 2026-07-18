package dev.murad.shipping.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RailDirectionResolverTest {
    private static final BlockPos JUNCTION = new BlockPos(10, 64, 20);
    private static final Vec3 CENTER = new Vec3(10.5D, 64.0D, 20.5D);

    @Test
    void stableDirectionWinsWhenCouplingTemporarilyStopsCart() {
        assertEquals(RailShape.EAST_WEST, RailDirectionResolver.resolveJunctionShape(
                Direction.WEST, Vec3.ZERO, CENTER, JUNCTION, RailShape.NORTH_SOUTH));
        assertEquals(RailShape.NORTH_SOUTH, RailDirectionResolver.resolveJunctionShape(
                Direction.SOUTH, Vec3.ZERO, CENTER, JUNCTION, RailShape.EAST_WEST));
    }

    @Test
    void meaningfulMotionSelectsAxisWithoutStableHistory() {
        assertEquals(RailShape.EAST_WEST, RailDirectionResolver.resolveJunctionShape(
                null, new Vec3(-0.2D, 0.0D, 0.01D), CENTER, JUNCTION, RailShape.NORTH_SOUTH));
        assertEquals(RailShape.NORTH_SOUTH, RailDirectionResolver.resolveJunctionShape(
                null, new Vec3(0.01D, 0.0D, 0.2D), CENTER, JUNCTION, RailShape.EAST_WEST));
    }

    @Test
    void stoppedVanillaCartUsesPositionBeforeBlockStateFallback() {
        assertEquals(RailShape.EAST_WEST, RailDirectionResolver.resolveJunctionShape(
                null, Vec3.ZERO, CENTER.add(0.3D, 0.0D, 0.0D), JUNCTION, RailShape.NORTH_SOUTH));
        assertEquals(RailShape.NORTH_SOUTH, RailDirectionResolver.resolveJunctionShape(
                null, Vec3.ZERO, CENTER.add(0.0D, 0.0D, -0.3D), JUNCTION, RailShape.EAST_WEST));
        assertEquals(RailShape.EAST_WEST, RailDirectionResolver.resolveJunctionShape(
                null, Vec3.ZERO, CENTER, JUNCTION, RailShape.EAST_WEST));
    }

    @Test
    void blockTransitionProducesPersistentCardinalDirection() {
        assertEquals(Direction.EAST, RailDirectionResolver.directionBetween(
                new BlockPos(1, 64, 3), new BlockPos(2, 64, 3)).orElseThrow());
        assertEquals(Direction.NORTH, RailDirectionResolver.directionBetween(
                new BlockPos(1, 64, 3), new BlockPos(1, 64, 2)).orElseThrow());
        assertTrue(RailDirectionResolver.directionBetween(JUNCTION, JUNCTION).isEmpty());
    }

    @Test
    void tinyCouplingNoiseIsNotTreatedAsTravel() {
        assertTrue(RailDirectionResolver.directionFromMotion(
                new Vec3(0.0001D, 0.0D, -0.0001D)).isEmpty());
    }
}
