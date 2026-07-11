package dev.murad.shipping.util;

import dev.murad.shipping.ShippingConfig;
import dev.murad.shipping.block.guiderail.TugGuideRailBlock;
import dev.murad.shipping.setup.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;

public final class TugRouteCompiler {
    private static final int BASE_VISITED_LIMIT = 4096;
    private static final double TURN_PENALTY = 0.35D;

    private TugRouteCompiler() {
    }

    public record CompileResult(boolean success, TugRoute route, @Nullable String error) {
        public static CompileResult success(TugRoute route) {
            return new CompileResult(true, route, null);
        }

        public static CompileResult failure(String error) {
            return new CompileResult(false, new TugRoute(), error);
        }
    }

    private record SearchState(BlockPos pos, @Nullable Direction incomingDirection) {
    }

    private record SearchNode(SearchState state, double gScore, double fScore) {
    }

    private record PathResult(TugRouteSegment segment, @Nullable Direction arrivalDirection) {
    }

    private record SegmentCompileResult(boolean success, List<TugRouteSegment> segments, @Nullable String error) {
        private static SegmentCompileResult success(List<TugRouteSegment> segments) {
            return new SegmentCompileResult(true, segments, null);
        }

        private static SegmentCompileResult failure(String error) {
            return new SegmentCompileResult(false, List.of(), error);
        }
    }

    public static CompileResult compile(Level level, TugRoute route, double maxSegmentDistance) {
        TugRoute compiled = route.copy();
        compiled.setDimension(level.dimension().location().toString());
        compiled.setSegments(List.of());

        if (compiled.isEmpty()) {
            return CompileResult.success(compiled);
        }

        List<TugRouteNode> anchoredNodes = new ArrayList<>();
        for (TugRouteNode node : compiled) {
            Optional<BlockPos> anchor = resolveWaypointAnchor(level, node.toBlockPos());
            if (anchor.isEmpty()) {
                return CompileResult.failure("Waypoint at " + node.getDisplayCoords() + " is not on navigable water.");
            }
            anchoredNodes.add(new TugRouteNode(node.getName(), anchor.get().getX(), anchor.get().getY(), anchor.get().getZ()));
        }

        compiled.clear();
        compiled.addAll(anchoredNodes);

        if (compiled.size() == 1) {
            return CompileResult.success(compiled);
        }

        for (int i = 0; i < compiled.size(); i++) {
            TugRouteNode from = compiled.get(i);
            TugRouteNode to = compiled.get((i + 1) % compiled.size());
            double distance = from.toBlockPos().distSqr(to.toBlockPos());
            if (Math.sqrt(distance) > maxSegmentDistance) {
                return CompileResult.failure("Waypoint " + (i + 1) + " is too far from waypoint " + (((i + 1) % compiled.size()) + 1) + ".");
            }
        }

        Set<BlockPos> waypointPositions = new HashSet<>();
        for (TugRouteNode node : compiled) {
            waypointPositions.add(node.toBlockPos());
        }

        List<Direction> initialDirections = getInitialDirections(level, compiled.getFirst().toBlockPos(), compiled.get(1).toBlockPos());
        if (initialDirections.isEmpty()) {
            return CompileResult.failure("Could not find a navigable departure from waypoint 1.");
        }

        String lastError = null;
        for (Direction initialDirection : initialDirections) {
            SegmentCompileResult segmentResult = compileSegments(level, compiled, waypointPositions, initialDirection);
            if (segmentResult.success()) {
                compiled.setSegments(segmentResult.segments());
                return CompileResult.success(compiled);
            }
            lastError = segmentResult.error();
        }

        return CompileResult.failure(lastError != null
            ? lastError
            : "Could not compile a non-intersecting tug rail for this route.");
    }

    public static Optional<BlockPos> resolveWaypointAnchor(Level level, BlockPos pos) {
        if (isNavigableWaypoint(level, pos)) {
            return Optional.of(pos);
        }

        List<BlockPos> candidates = new ArrayList<>();
        candidates.add(pos);
        candidates.add(pos.below());
        candidates.add(pos.above());
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            candidates.add(pos.relative(direction));
            candidates.add(pos.relative(direction).below());
            candidates.add(pos.relative(direction).above());
        }

        return candidates.stream()
            .filter(candidate -> isNavigableWaypoint(level, candidate))
            .min(Comparator.comparingDouble(candidate -> candidate.distSqr(pos)));
    }

    /**
     * Returns whether a block can be used directly as a tug route waypoint.
     */
    public static boolean isNavigableWaypoint(Level level, BlockPos pos) {
        return isNavigable(level, pos);
    }

    private static SegmentCompileResult compileSegments(Level level, TugRoute route, Set<BlockPos> waypointPositions, Direction initialDirection) {
        List<TugRouteSegment> segments = new ArrayList<>();
        Set<BlockPos> occupied = new HashSet<>();
        @Nullable Direction incomingDirection = null;

        for (int i = 0; i < route.size(); i++) {
            BlockPos start = route.get(i).toBlockPos();
            BlockPos goal = route.get((i + 1) % route.size()).toBlockPos();
            Set<BlockPos> blockedWaypoints = new HashSet<>(waypointPositions);
            blockedWaypoints.remove(start);
            blockedWaypoints.remove(goal);

            Direction forcedFirstDirection = i == 0 ? initialDirection : null;
            Direction goalNextDirection = i == route.size() - 1 ? initialDirection : null;
            Optional<PathResult> segment = pathfind(level, start, goal, incomingDirection, forcedFirstDirection, goalNextDirection, occupied, blockedWaypoints);
            if (segment.isEmpty()) {
                return SegmentCompileResult.failure("Could not find a non-intersecting water path between waypoint "
                    + (i + 1) + " and waypoint " + (((i + 1) % route.size()) + 1) + ".");
            }

            TugRouteSegment compiledSegment = segment.get().segment();
            segments.add(compiledSegment);
            incomingDirection = segment.get().arrivalDirection();

            for (TugRoutePoint point : compiledSegment.getPoints()) {
                occupied.add(point.toBlockPos());
            }
        }

        return SegmentCompileResult.success(segments);
    }

    private static List<Direction> getInitialDirections(Level level, BlockPos start, BlockPos goal) {
        List<Direction> directions = new ArrayList<>();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighbor = start.relative(direction);
            if (!isNavigable(level, neighbor) || isOppositeGuideRail(level, neighbor, direction)) {
                continue;
            }
            directions.add(direction);
        }
        directions.sort(Comparator.comparingDouble(direction -> heuristic(start.relative(direction), goal)));
        return directions;
    }

    private static Optional<PathResult> pathfind(Level level,
                                                 BlockPos start,
                                                 BlockPos goal,
                                                 @Nullable Direction startIncomingDirection,
                                                 @Nullable Direction forcedFirstDirection,
                                                 @Nullable Direction goalNextDirection,
                                                 Set<BlockPos> occupied,
                                                 Set<BlockPos> blockedWaypoints) {
        if (start.equals(goal)) {
            return isGoalArrivalValid(startIncomingDirection, goalNextDirection)
                ? Optional.of(new PathResult(new TugRouteSegment(List.of(new TugRoutePoint(start))), startIncomingDirection))
                : Optional.empty();
        }

        if (forcedFirstDirection != null && startIncomingDirection != null && forcedFirstDirection == startIncomingDirection.getOpposite()) {
            return Optional.empty();
        }

        PriorityQueue<SearchNode> openSet = new PriorityQueue<>(Comparator.comparingDouble(SearchNode::fScore));
        Map<SearchState, Double> gScores = new HashMap<>();
        Map<SearchState, SearchState> cameFrom = new HashMap<>();
        Map<BlockPos, Double> bestPositionScores = new HashMap<>();
        Set<SearchState> closed = new HashSet<>();

        SearchState startState = new SearchState(start, startIncomingDirection);
        gScores.put(startState, 0.0D);
        bestPositionScores.put(start, 0.0D);
        openSet.add(new SearchNode(startState, 0.0D, heuristic(start, goal)));

        int visitedLimit = BASE_VISITED_LIMIT * ShippingConfig.Server.TUG_PATHFINDING_MULTIPLIER.get();
        while (!openSet.isEmpty() && closed.size() < visitedLimit) {
            SearchNode current = openSet.poll();
            if (!closed.add(current.state())) {
                continue;
            }

            SearchState currentState = current.state();
            if (currentState.pos().equals(goal)) {
                if (isGoalArrivalValid(currentState.incomingDirection(), goalNextDirection)) {
                    return Optional.of(reconstructPath(currentState, cameFrom));
                }
                continue;
            }

            for (Direction direction : Direction.Plane.HORIZONTAL) {
                if (forcedFirstDirection != null && currentState.pos().equals(start) && direction != forcedFirstDirection) {
                    continue;
                }
                if (currentState.incomingDirection() != null && direction == currentState.incomingDirection().getOpposite()) {
                    continue;
                }

                BlockPos neighbor = currentState.pos().relative(direction);
                if (!isNavigable(level, neighbor) || isOppositeGuideRail(level, neighbor, direction)) {
                    continue;
                }
                if (blockedWaypoints.contains(neighbor)) {
                    continue;
                }
                if (occupied.contains(neighbor) && !neighbor.equals(goal) && !neighbor.equals(start)) {
                    continue;
                }

                double tentativeScore = current.gScore() + 1.0D + getLandPenalty(level, neighbor) + getTurnPenalty(currentState.incomingDirection(), direction);
                SearchState neighborState = new SearchState(neighbor, direction);
                if (tentativeScore >= gScores.getOrDefault(neighborState, Double.MAX_VALUE)) {
                    continue;
                }
                if (!neighbor.equals(goal) && tentativeScore >= bestPositionScores.getOrDefault(neighbor, Double.MAX_VALUE)) {
                    continue;
                }

                cameFrom.put(neighborState, currentState);
                gScores.put(neighborState, tentativeScore);
                if (!neighbor.equals(goal)) {
                    bestPositionScores.put(neighbor, tentativeScore);
                }
                openSet.add(new SearchNode(neighborState, tentativeScore, tentativeScore + heuristic(neighbor, goal)));
            }
        }

        return Optional.empty();
    }

    private static PathResult reconstructPath(SearchState goalState, Map<SearchState, SearchState> cameFrom) {
        List<TugRoutePoint> reversed = new ArrayList<>();
        SearchState current = goalState;
        reversed.add(new TugRoutePoint(current.pos()));
        while (cameFrom.containsKey(current)) {
            current = cameFrom.get(current);
            reversed.add(new TugRoutePoint(current.pos()));
        }
        List<TugRoutePoint> points = new ArrayList<>(reversed.size());
        for (int i = reversed.size() - 1; i >= 0; i--) {
            points.add(reversed.get(i));
        }
        return new PathResult(new TugRouteSegment(points), goalState.incomingDirection());
    }

    private static boolean isOppositeGuideRail(Level level, BlockPos pos, Direction direction) {
        BlockState state = level.getBlockState(pos.below());
        if (!state.is(ModBlocks.GUIDE_RAIL_TUG.get())) {
            return false;
        }
        return TugGuideRailBlock.getArrowsDirection(state).getOpposite() == direction;
    }

    private static double heuristic(BlockPos pos, BlockPos goal) {
        return Math.abs(goal.getX() - pos.getX()) + Math.abs(goal.getZ() - pos.getZ());
    }

    private static double getTurnPenalty(@Nullable Direction incomingDirection, Direction nextDirection) {
        if (incomingDirection == null || incomingDirection == nextDirection) {
            return 0.0D;
        }
        return TURN_PENALTY;
    }

    private static boolean isGoalArrivalValid(@Nullable Direction arrivalDirection, @Nullable Direction goalNextDirection) {
        return goalNextDirection == null
            || arrivalDirection == null
            || arrivalDirection != goalNextDirection.getOpposite();
    }

    private static double getLandPenalty(Level level, BlockPos pos) {
        double penalty = 0.0D;
        for (BlockPos neighbor : List.of(
            pos.east(),
            pos.west(),
            pos.north(),
            pos.south(),
            pos.north().east(),
            pos.north().west(),
            pos.south().east(),
            pos.south().west()
        )) {
            FluidState fluid = level.getFluidState(neighbor);
            if (!fluid.is(FluidTags.WATER)) {
                penalty = 5.0D;
            }

            BlockState state = level.getBlockState(neighbor);
            BlockEntity blockEntity = level.getBlockEntity(neighbor);
            if (state.is(ModBlocks.GUIDE_RAIL_CORNER.get()) || state.is(ModBlocks.DOCKING_STATION.get()) || blockEntity != null) {
                return 0.0D;
            }
        }
        return penalty;
    }

    private static boolean isNavigable(Level level, BlockPos pos) {
        FluidState fluid = level.getFluidState(pos);
        if (!fluid.is(FluidTags.WATER)) {
            return false;
        }

        BlockState state = level.getBlockState(pos);
        if (!state.getCollisionShape(level, pos).isEmpty()) {
            return false;
        }

        BlockState above = level.getBlockState(pos.above());
        if (!above.getCollisionShape(level, pos.above()).isEmpty() && !above.getFluidState().is(FluidTags.WATER)) {
            return false;
        }

        return true;
    }
}
