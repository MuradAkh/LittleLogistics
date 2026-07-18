package dev.murad.shipping.entity.navigation;

import dev.murad.shipping.block.rail.MultiShapeRail;
import dev.murad.shipping.entity.custom.train.AbstractTrainCarEntity;
import dev.murad.shipping.entity.custom.train.locomotive.AbstractLocomotiveEntity;
import dev.murad.shipping.util.LocoRoute;
import dev.murad.shipping.util.LocoRouteSegment;
import dev.murad.shipping.util.LocoRouteStep;
import dev.murad.shipping.util.RailDirectionResolver;
import dev.murad.shipping.util.RailHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;

import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

/** Executes the directed, precompiled rail traversal stored in a locomotive route item. */
public class LocomotiveNavigator {
    private static final int AUTOMATIC_RAIL_LOOKAHEAD = 3;
    private static final long RESERVATION_TIMEOUT_TICKS = 1200L;
    private static final String SEGMENT_TAG = "segment";
    private static final String STEP_TAG = "step";
    private static final String SYNCHRONIZED_TAG = "synchronized";
    private static final String INVALID_TAG = "invalid";

    private final AbstractLocomotiveEntity locomotive;
    private LocoRoute route = new LocoRoute();
    private int segmentIndex;
    private int stepIndex;
    private boolean synchronizedToRoute;
    private boolean invalidRoute;
    private final Map<BlockPos, ActiveReservation> activeReservations = new HashMap<>();

    private static final class ActiveReservation {
        private final Set<UUID> waitingFor;
        private long expiresAt;

        private ActiveReservation(Set<UUID> waitingFor, long expiresAt) {
            this.waitingFor = waitingFor;
            this.expiresAt = expiresAt;
        }
    }

    public LocomotiveNavigator(AbstractLocomotiveEntity locomotive) {
        this.locomotive = locomotive;
    }

    public int getRouteSize() {
        return route.size();
    }

    /** The current completed waypoint count used by the engine screen. */
    public int getVisitedSize() {
        return synchronizedToRoute ? segmentIndex : 0;
    }

    public boolean isRouteInvalid() {
        return invalidRoute;
    }

    public boolean hasUsableRoute() {
        return route.isUsable();
    }

    /**
     * Returns the exact directed route steps ahead of the locomotive. An empty result tells callers
     * to fall back to live rail geometry because the navigator is not synchronized to its route.
     */
    public List<LocoRouteStep> getUpcomingSteps(int maxSteps) {
        if (!route.isUsable() || invalidRoute || !synchronizedToRoute) {
            return List.of();
        }
        return route.getUpcomingSteps(segmentIndex, stepIndex, maxSteps);
    }

    public List<ChunkPos> getUpcomingChunks(int maxSteps) {
        LinkedHashSet<ChunkPos> chunks = new LinkedHashSet<>();
        for (LocoRouteStep step : getUpcomingSteps(maxSteps)) {
            chunks.add(new ChunkPos(step.railPos()));
        }
        return List.copyOf(chunks);
    }

    public void serverTick() {
        tickReservations();
        if (!route.isUsable()) return;
        if (invalidRoute) {
            locomotive.stall();
            return;
        }

        RailHelper.getRail(locomotive.getOnPos().above(), locomotive.level()).ifPresent(railPos -> {
            Direction travelDirection = getTravelDirection();
            LocoRouteStep step = locateCurrentStep(railPos, travelDirection);
            if (step == null) return;

            if (!reserveAndConfigureUpcomingAutomaticRails()) {
                locomotive.stall();
                return;
            }

            BlockState state = locomotive.level().getBlockState(railPos);
            if (state.getBlock() instanceof MultiShapeRail rail) {
                Direction input = step.incomingDirection().getOpposite();
                if (rail.isAutomaticSwitching()) {
                    boolean applied = rail.setRailState(state, locomotive.level(), railPos, input, step.outgoingDirection());
                    if (!applied) invalidate();
                } else if (!rail.getExitDirections(state, input).contains(step.outgoingDirection())) {
                    // Redstone/manual rails remain under player control; a changed state must
                    // stop this precompiled route rather than make it choose another branch.
                    invalidate();
                }
            }
        });
    }

    /**
     * Reconstructs transient rail state after entities and block states have been loaded. The
     * locomotive keeps the consist frozen until occupied automatic rails can be reserved.
     */
    public boolean recoverAfterLoad() {
        tickReservations();
        recoverMissingConsistDirections();
        if (!reserveOccupiedAutomaticRails()) return false;
        if (!route.isUsable() || invalidRoute) return true;

        return RailHelper.getRail(locomotive.getOnPos().above(), locomotive.level())
                .map(railPos -> {
                    boolean wasSynchronized = synchronizedToRoute;
                    Direction travelDirection = locomotive.getRailTravelDirectionAt(railPos)
                            .orElse(locomotive.getDirection());
                    synchronizedToRoute = false;
                    synchronizeAt(railPos, travelDirection);
                    if (wasSynchronized && !synchronizedToRoute) {
                        invalidate();
                        return true;
                    }
                    if (!synchronizedToRoute) return true;
                    boolean configured = reserveAndConfigureUpcomingAutomaticRails();
                    return configured || invalidRoute;
                })
                .orElse(true);
    }

    private void recoverMissingConsistDirections() {
        List<AbstractTrainCarEntity> cars = locomotive.getTrain().asList();
        for (int index = 0; index < cars.size(); index++) {
            AbstractTrainCarEntity car = cars.get(index);
            if (!car.needsRailDirectionRecovery()) continue;
            BlockPos railPos = RailHelper.getRail(car.getOnPos().above(), car.level())
                    .orElse(null);
            if (railPos == null) continue;

            Direction recovered = null;
            if (index == 0) {
                recovered = findRouteDirectionNearProgress(railPos).orElse(null);
                if (recovered == null && cars.size() > 1) {
                    recovered = RailDirectionResolver.directionFromMotion(
                            car.position().subtract(cars.get(1).position())).orElse(null);
                }
            } else {
                AbstractTrainCarEntity leader = cars.get(index - 1);
                recovered = RailDirectionResolver.directionFromMotion(
                        leader.position().subtract(car.position())).orElse(null);
            }
            if (recovered == null) {
                recovered = car.getStableRailTravelDirection().orElse(car.getDirection());
            }
            car.recoverRailTravelDirection(recovered, railPos);
        }
    }

    private Optional<Direction> findRouteDirectionNearProgress(BlockPos railPos) {
        if (!route.isUsable()) return Optional.empty();
        int segment = Math.floorMod(segmentIndex, route.getSegments().size());
        LocoRouteSegment current = route.getSegments().get(segment);

        for (int offset : new int[]{0, 1, -1, 2, -2}) {
            int candidateIndex = stepIndex + offset;
            if (candidateIndex < 0 || candidateIndex >= current.getSteps().size()) continue;
            LocoRouteStep candidate = current.getSteps().get(candidateIndex);
            if (candidate.railPos().equals(railPos)) {
                return Optional.of(candidate.incomingDirection());
            }
        }

        BlockPos destination = route.get((segment + 1) % route.size()).toBlockPos();
        if (destination.equals(railPos)) {
            return Optional.of(current.getArrivalDirection());
        }

        for (LocoRouteSegment routeSegment : route.getSegments()) {
            for (LocoRouteStep candidate : routeSegment.getSteps()) {
                if (candidate.railPos().equals(railPos)) {
                    return Optional.of(candidate.incomingDirection());
                }
            }
        }
        return Optional.empty();
    }

    private boolean reserveAndConfigureUpcomingAutomaticRails() {
        long expiresAt = locomotive.level().getGameTime() + RESERVATION_TIMEOUT_TICKS;
        Set<UUID> consistIds = currentConsistIds();
        for (LocoRouteStep step : getUpcomingSteps(AUTOMATIC_RAIL_LOOKAHEAD)) {
            BlockState state = locomotive.level().getBlockState(step.railPos());
            if (!(state.getBlock() instanceof MultiShapeRail rail) || !rail.isAutomaticSwitching()) {
                continue;
            }

            if (isOccupiedByAnotherConsist(step.railPos(), consistIds)) {
                return false;
            }
            if (!AutomaticRailReservations.acquire(
                    locomotive.level(), step.railPos(), locomotive.getUUID(), expiresAt)) {
                return false;
            }

            activeReservations.computeIfAbsent(step.railPos().immutable(), ignored ->
                    new ActiveReservation(currentConsistIds(), expiresAt)).expiresAt = expiresAt;

            Direction input = step.incomingDirection().getOpposite();
            if (!rail.setRailState(state, locomotive.level(), step.railPos(), input, step.outgoingDirection())) {
                invalidate();
                return false;
            }
        }
        return true;
    }

    private boolean reserveOccupiedAutomaticRails() {
        long expiresAt = locomotive.level().getGameTime() + RESERVATION_TIMEOUT_TICKS;
        List<AbstractTrainCarEntity> cars = locomotive.getTrain().asList();
        List<UUID> expectedOrder = locomotive.getExpectedConsistUUIDs();

        for (int carIndex = 0; carIndex < cars.size(); carIndex++) {
            AbstractTrainCarEntity car = cars.get(carIndex);
            BlockPos railPos = RailHelper.getRail(car.getOnPos().above(), car.level())
                    .orElse(null);
            if (railPos == null) continue;
            BlockState state = locomotive.level().getBlockState(railPos);
            if (!(state.getBlock() instanceof MultiShapeRail rail) || !rail.isAutomaticSwitching()) {
                continue;
            }

            if (!AutomaticRailReservations.acquire(
                    locomotive.level(), railPos, locomotive.getUUID(), expiresAt)) {
                return false;
            }

            ActiveReservation reservation = activeReservations.computeIfAbsent(
                    railPos.immutable(), ignored -> new ActiveReservation(new HashSet<>(), expiresAt));
            reservation.expiresAt = expiresAt;

            int expectedIndex = expectedOrder.indexOf(car.getUUID());
            if (expectedIndex >= 0) {
                reservation.waitingFor.addAll(expectedOrder.subList(expectedIndex, expectedOrder.size()));
            } else {
                for (int followerIndex = carIndex; followerIndex < cars.size(); followerIndex++) {
                    reservation.waitingFor.add(cars.get(followerIndex).getUUID());
                }
            }
        }
        return true;
    }

    private boolean isOccupiedByAnotherConsist(BlockPos railPos, Set<UUID> consistIds) {
        return !locomotive.level().getEntitiesOfClass(
                AbstractTrainCarEntity.class,
                new AABB(railPos),
                car -> !consistIds.contains(car.getUUID())).isEmpty();
    }

    private Set<UUID> currentConsistIds() {
        Set<UUID> ids = new HashSet<>(locomotive.getExpectedConsistUUIDs());
        locomotive.getTrain().asList().forEach(car -> ids.add(car.getUUID()));
        return ids;
    }

    private void tickReservations() {
        if (activeReservations.isEmpty()) return;

        long now = locomotive.level().getGameTime();
        List<AbstractTrainCarEntity> cars = locomotive.getTrain().asList();
        Iterator<Map.Entry<BlockPos, ActiveReservation>> iterator =
                activeReservations.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, ActiveReservation> entry = iterator.next();
            BlockPos reservedRail = entry.getKey();
            ActiveReservation reservation = entry.getValue();
            boolean occupiedByConsist = false;

            for (AbstractTrainCarEntity car : cars) {
                boolean onReservedRail = RailHelper.getRail(car.getOnPos().above(), car.level())
                        .map(reservedRail::equals)
                        .orElse(false);
                if (onReservedRail) {
                    occupiedByConsist = true;
                    reservation.waitingFor.remove(car.getUUID());
                }
            }

            if ((reservation.waitingFor.isEmpty() && !occupiedByConsist) || now >= reservation.expiresAt) {
                AutomaticRailReservations.release(
                        locomotive.level(), reservedRail, locomotive.getUUID());
                iterator.remove();
            }
        }
    }

    private Direction getTravelDirection() {
        BlockPos old = locomotive.getOldHorizontalBlockPos();
        BlockPos current = locomotive.getBlockPos();
        int x = current.getX() - old.getX();
        int z = current.getZ() - old.getZ();
        if (x > 0) return Direction.EAST;
        if (x < 0) return Direction.WEST;
        if (z > 0) return Direction.SOUTH;
        if (z < 0) return Direction.NORTH;
        return locomotive.getStableRailTravelDirection().orElse(locomotive.getDirection());
    }

    @Nullable
    private LocoRouteStep locateCurrentStep(BlockPos railPos, Direction travelDirection) {
        if (!synchronizedToRoute) {
            synchronizeAt(railPos, travelDirection);
            if (!synchronizedToRoute) return null;
        }

        // The train normally reaches exactly one expected rail per tick.  The loop also
        // handles a hitch across a rail boundary without attempting to re-pathfind.
        for (int attempts = 0; attempts < 2; attempts++) {
            LocoRouteSegment segment = route.getSegments().get(segmentIndex);
            if (stepIndex < segment.getSteps().size()) {
                LocoRouteStep expected = segment.getSteps().get(stepIndex);
                if (expected.railPos().equals(railPos)) {
                    if (expected.incomingDirection() != travelDirection) {
                        invalidate();
                        return null;
                    }
                    return expected;
                }

                if (stepIndex + 1 < segment.getSteps().size()) {
                    LocoRouteStep next = segment.getSteps().get(stepIndex + 1);
                    if (next.railPos().equals(railPos) && next.incomingDirection() == travelDirection) {
                        stepIndex++;
                        continue;
                    }
                }
            }

            BlockPos destination = route.get((segmentIndex + 1) % route.size()).toBlockPos();
            if (destination.equals(railPos) && segment.getArrivalDirection() == travelDirection) {
                segmentIndex = (segmentIndex + 1) % route.getSegments().size();
                stepIndex = 0;
                continue;
            }
            invalidate();
            return null;
        }
        invalidate();
        return null;
    }

    private void synchronizeAt(BlockPos railPos, Direction travelDirection) {
        for (int segment = 0; segment < route.getSegments().size(); segment++) {
            ListLoop:
            for (int step = 0; step < route.getSegments().get(segment).getSteps().size(); step++) {
                LocoRouteStep candidate = route.getSegments().get(segment).getSteps().get(step);
                if (candidate.railPos().equals(railPos) && candidate.incomingDirection() == travelDirection) {
                    segmentIndex = segment;
                    stepIndex = step;
                    synchronizedToRoute = true;
                    break ListLoop;
                }
            }
            if (synchronizedToRoute) return;
        }

        // A train can be parked precisely at a waypoint before it enters the next segment.
        for (int segment = 0; segment < route.getSegments().size(); segment++) {
            LocoRouteSegment candidate = route.getSegments().get(segment);
            if (!route.get(segment).toBlockPos().equals(railPos) || candidate.getSteps().isEmpty()) continue;
            if (candidate.getStartIncomingDirection() == travelDirection) {
                segmentIndex = segment;
                stepIndex = 0;
                synchronizedToRoute = true;
                return;
            }
        }
    }

    private void invalidate() {
        invalidRoute = true;
        locomotive.stall();
    }

    public void updateWithLocoRouteItem(LocoRoute newRoute) {
        if (newRoute.equals(route)) return;
        route = newRoute.copy();
        segmentIndex = 0;
        stepIndex = 0;
        synchronizedToRoute = false;
        invalidRoute = false;
    }

    public void loadFromNbt(@Nullable CompoundTag tag) {
        if (tag == null) return;
        segmentIndex = Math.max(0, tag.getInt(SEGMENT_TAG));
        stepIndex = Math.max(0, tag.getInt(STEP_TAG));
        synchronizedToRoute = tag.getBoolean(SYNCHRONIZED_TAG);
        invalidRoute = tag.getBoolean(INVALID_TAG);
    }

    public CompoundTag saveToNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(SEGMENT_TAG, segmentIndex);
        tag.putInt(STEP_TAG, stepIndex);
        tag.putBoolean(SYNCHRONIZED_TAG, synchronizedToRoute);
        tag.putBoolean(INVALID_TAG, invalidRoute);
        return tag;
    }
}
