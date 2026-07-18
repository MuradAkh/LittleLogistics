package dev.murad.shipping.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Resolves rail travel from position history before consulting instantaneous velocity.
 * Coupled train cars intentionally adjust their velocity after vanilla minecart movement,
 * so velocity alone is not a reliable indication of the rail a car entered from.
 */
public final class RailDirectionResolver {
    private static final double MOTION_EPSILON_SQR = 1.0E-6D;
    private static final double CENTER_OFFSET_EPSILON = 1.0E-3D;

    private RailDirectionResolver() {
    }

    public static Optional<Direction> directionBetween(BlockPos previous, BlockPos current) {
        int x = current.getX() - previous.getX();
        int z = current.getZ() - previous.getZ();
        if (x == 0 && z == 0) return Optional.empty();
        if (Math.abs(x) > Math.abs(z)) return Optional.of(x > 0 ? Direction.EAST : Direction.WEST);
        return Optional.of(z > 0 ? Direction.SOUTH : Direction.NORTH);
    }

    public static Optional<Direction> directionFromMotion(Vec3 motion) {
        if (motion.horizontalDistanceSqr() <= MOTION_EPSILON_SQR) return Optional.empty();
        return Optional.of(RailHelper.directionFromVelocity(motion));
    }

    public static Optional<Direction> resolveTrackedDirection(@Nullable BlockPos trackedRail,
                                                              @Nullable Direction stableDirection,
                                                              BlockPos queriedRail) {
        if (trackedRail != null && !trackedRail.equals(queriedRail)) {
            Optional<Direction> transition = directionBetween(trackedRail, queriedRail);
            if (transition.isPresent()) return transition;
        }
        return Optional.ofNullable(stableDirection);
    }

    public static RailShape resolveJunctionShape(@Nullable Direction stableTravelDirection,
                                                 Vec3 motion,
                                                 Vec3 position,
                                                 BlockPos railPos,
                                                 RailShape fallback) {
        if (stableTravelDirection != null && stableTravelDirection.getAxis().isHorizontal()) {
            return shapeForAxis(stableTravelDirection.getAxis());
        }

        Optional<Direction> motionDirection = directionFromMotion(motion);
        if (motionDirection.isPresent()) {
            return shapeForAxis(motionDirection.get().getAxis());
        }

        double xOffset = Math.abs(position.x - (railPos.getX() + 0.5D));
        double zOffset = Math.abs(position.z - (railPos.getZ() + 0.5D));
        if (Math.max(xOffset, zOffset) > CENTER_OFFSET_EPSILON
                && Math.abs(xOffset - zOffset) > CENTER_OFFSET_EPSILON) {
            return xOffset > zOffset ? RailShape.EAST_WEST : RailShape.NORTH_SOUTH;
        }

        return fallback;
    }

    private static RailShape shapeForAxis(Direction.Axis axis) {
        return axis == Direction.Axis.X ? RailShape.EAST_WEST : RailShape.NORTH_SOUTH;
    }
}
