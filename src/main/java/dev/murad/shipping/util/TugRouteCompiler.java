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

    public enum PreviewPathStatus {
        SEARCHING,
        FOUND,
        FAILED
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
        RouteCompileSession session = createCompileSession(level, route, maxSegmentDistance);
        while (session.getStatus() == PreviewPathStatus.SEARCHING) {
            session.advance(Integer.MAX_VALUE);
        }
        return session.getResult();
    }

    /**
     * Retained as a reference implementation while the resumable compiler is adopted.
     */
    @SuppressWarnings("unused")
    private static CompileResult compileLegacy(Level level, TugRoute route, double maxSegmentDistance) {
        TugRoute compiled = route.copy();
        compiled.markComplete();
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

    /**
     * Compiles the already-defined portion of an in-progress route without joining its final node back to node zero.
     */
    public static CompileResult compileOpen(Level level, TugRoute route, double maxSegmentDistance) {
        TugRoute compiled = route.copy();
        compiled.beginAppending();
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
        compiled.beginAppending();

        if (compiled.size() == 1) {
            return CompileResult.success(compiled);
        }

        for (int i = 0; i < compiled.size() - 1; i++) {
            if (Math.sqrt(compiled.get(i).toBlockPos().distSqr(compiled.get(i + 1).toBlockPos())) > maxSegmentDistance) {
                return CompileResult.failure("Waypoint " + (i + 1) + " is too far from waypoint " + (i + 2) + ".");
            }
        }

        List<TugRouteSegment> segments = new ArrayList<>();
        Set<BlockPos> occupied = new HashSet<>();
        for (int i = 0; i < compiled.size() - 1; i++) {
            BlockPos start = compiled.get(i).toBlockPos();
            BlockPos goal = compiled.get(i + 1).toBlockPos();
            Set<BlockPos> blockedWaypoints = new HashSet<>();
            for (TugRouteNode node : compiled) {
                blockedWaypoints.add(node.toBlockPos());
            }
            blockedWaypoints.remove(start);
            blockedWaypoints.remove(goal);

            Optional<PathResult> segment = pathfind(level, start, goal, null, null, null, occupied, blockedWaypoints);
            if (segment.isEmpty()) {
                return CompileResult.failure("Could not find a water path between waypoint " + (i + 1) + " and waypoint " + (i + 2) + ".");
            }
            TugRouteSegment compiledSegment = segment.get().segment();
            segments.add(compiledSegment);
            for (TugRoutePoint point : compiledSegment.getPoints()) {
                occupied.add(point.toBlockPos());
            }
        }

        compiled.setSegments(segments);
        return CompileResult.success(compiled);
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

    /**
     * Creates a resumable path search for client-only route previews. Call {@link PreviewPathfinder#advance(int)}
     * from the client tick loop rather than completing the search during rendering.
     */
    public static PreviewPathfinder createPreviewPathfinder(Level level, BlockPos start, BlockPos goal,
                                                            Set<BlockPos> occupied, Set<BlockPos> blockedWaypoints) {
        return new PreviewPathfinder(level, start, goal, null, null, null, occupied, blockedWaypoints);
    }

    public static final class PreviewPathfinder {
        private final Level level;
        private final BlockPos start;
        private final BlockPos goal;
        private final Set<BlockPos> occupied;
        private final Set<BlockPos> blockedWaypoints;
        @Nullable
        private final Direction startIncomingDirection;
        @Nullable
        private final Direction forcedFirstDirection;
        @Nullable
        private final Direction goalNextDirection;
        private final PriorityQueue<SearchNode> openSet = new PriorityQueue<>(Comparator.comparingDouble(SearchNode::fScore));
        private final Map<SearchState, Double> gScores = new HashMap<>();
        private final Map<SearchState, SearchState> cameFrom = new HashMap<>();
        private final Map<BlockPos, Double> bestPositionScores = new HashMap<>();
        private final Set<SearchState> closed = new HashSet<>();
        private final int visitedLimit;
        private PreviewPathStatus status;
        @Nullable
        private TugRouteSegment result;
        @Nullable
        private Direction arrivalDirection;

        private PreviewPathfinder(Level level, BlockPos start, BlockPos goal, @Nullable Direction startIncomingDirection,
                                  @Nullable Direction forcedFirstDirection, @Nullable Direction goalNextDirection,
                                  Set<BlockPos> occupied, Set<BlockPos> blockedWaypoints) {
            this.level = level;
            this.start = start.immutable();
            this.goal = goal.immutable();
            this.occupied = Set.copyOf(occupied);
            this.blockedWaypoints = Set.copyOf(blockedWaypoints);
            this.startIncomingDirection = startIncomingDirection;
            this.forcedFirstDirection = forcedFirstDirection;
            this.goalNextDirection = goalNextDirection;
            this.visitedLimit = BASE_VISITED_LIMIT * ShippingConfig.Server.TUG_PATHFINDING_MULTIPLIER.get();

            if (start.equals(goal) && isGoalArrivalValid(startIncomingDirection, goalNextDirection)) {
                this.result = new TugRouteSegment(List.of(new TugRoutePoint(start)));
                this.arrivalDirection = startIncomingDirection;
                this.status = PreviewPathStatus.FOUND;
                return;
            }

            if (forcedFirstDirection != null && startIncomingDirection != null
                && forcedFirstDirection == startIncomingDirection.getOpposite()) {
                this.status = PreviewPathStatus.FAILED;
                return;
            }

            SearchState startState = new SearchState(this.start, startIncomingDirection);
            this.gScores.put(startState, 0.0D);
            this.bestPositionScores.put(this.start, 0.0D);
            this.openSet.add(new SearchNode(startState, 0.0D, heuristic(this.start, this.goal)));
            this.status = PreviewPathStatus.SEARCHING;
        }

        public PreviewPathStatus advance(int nodeBudget) {
            if (this.status != PreviewPathStatus.SEARCHING) {
                return this.status;
            }

            int processed = 0;
            while (processed < nodeBudget && !this.openSet.isEmpty() && this.closed.size() < this.visitedLimit) {
                SearchNode current = this.openSet.poll();
                if (!this.closed.add(current.state())) {
                    continue;
                }
                processed++;

                SearchState currentState = current.state();
                if (currentState.pos().equals(this.goal)) {
                    if (isGoalArrivalValid(currentState.incomingDirection(), this.goalNextDirection)) {
                        this.result = reconstructPath(currentState, this.cameFrom).segment();
                        this.arrivalDirection = currentState.incomingDirection();
                        this.status = PreviewPathStatus.FOUND;
                        return this.status;
                    }
                    continue;
                }

                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    if (this.forcedFirstDirection != null && currentState.pos().equals(this.start) && direction != this.forcedFirstDirection) {
                        continue;
                    }
                    if (currentState.incomingDirection() != null && direction == currentState.incomingDirection().getOpposite()) {
                        continue;
                    }

                    BlockPos neighbor = currentState.pos().relative(direction);
                    if (!isNavigable(this.level, neighbor) || isOppositeGuideRail(this.level, neighbor, direction)) {
                        continue;
                    }
                    if (this.blockedWaypoints.contains(neighbor)) {
                        continue;
                    }
                    if (this.occupied.contains(neighbor) && !neighbor.equals(this.goal) && !neighbor.equals(this.start)) {
                        continue;
                    }

                    double tentativeScore = current.gScore() + 1.0D + getLandPenalty(this.level, neighbor)
                        + getTurnPenalty(currentState.incomingDirection(), direction);
                    SearchState neighborState = new SearchState(neighbor, direction);
                    if (tentativeScore >= this.gScores.getOrDefault(neighborState, Double.MAX_VALUE)) {
                        continue;
                    }
                    if (!neighbor.equals(this.goal) && tentativeScore >= this.bestPositionScores.getOrDefault(neighbor, Double.MAX_VALUE)) {
                        continue;
                    }

                    this.cameFrom.put(neighborState, currentState);
                    this.gScores.put(neighborState, tentativeScore);
                    if (!neighbor.equals(this.goal)) {
                        this.bestPositionScores.put(neighbor, tentativeScore);
                    }
                    this.openSet.add(new SearchNode(neighborState, tentativeScore, tentativeScore + heuristic(neighbor, this.goal)));
                }
            }

            if (this.openSet.isEmpty() || this.closed.size() >= this.visitedLimit) {
                this.status = PreviewPathStatus.FAILED;
            }
            return this.status;
        }

        public PreviewPathStatus getStatus() {
            return this.status;
        }

        public Optional<TugRouteSegment> getResult() {
            return Optional.ofNullable(this.result);
        }

        @Nullable
        public Direction getArrivalDirection() {
            return this.arrivalDirection;
        }
    }

    /**
     * Resumable form of the closed-route compiler. It deliberately follows the same segment order,
     * initial-direction trials, occupancy rules, and turn constraints as a saved route compilation.
     */
    public static final class RouteCompileSession {
        private final Level level;
        private final double maxSegmentDistance;
        private final TugRoute compiled;
        private final List<Direction> initialDirections;
        private final Set<BlockPos> waypointPositions;
        private final List<TugRouteSegment> segments = new ArrayList<>();
        private final Set<BlockPos> occupied = new HashSet<>();
        private int initialDirectionIndex;
        private int segmentIndex;
        @Nullable
        private Direction incomingDirection;
        @Nullable
        private PreviewPathfinder activePathfinder;
        private PreviewPathStatus status = PreviewPathStatus.SEARCHING;
        @Nullable
        private CompileResult result;

        private RouteCompileSession(Level level, TugRoute route, double maxSegmentDistance) {
            this.level = level;
            this.maxSegmentDistance = maxSegmentDistance;
            this.compiled = route.copy();
            this.compiled.markComplete();
            this.compiled.setDimension(level.dimension().location().toString());
            this.compiled.setSegments(List.of());

            if (compiled.isEmpty()) {
                this.initialDirections = List.of();
                this.waypointPositions = Set.of();
                succeed();
                return;
            }

            List<TugRouteNode> anchoredNodes = new ArrayList<>();
            for (TugRouteNode node : compiled) {
                Optional<BlockPos> anchor = resolveWaypointAnchor(level, node.toBlockPos());
                if (anchor.isEmpty()) {
                    this.initialDirections = List.of();
                    this.waypointPositions = Set.of();
                    fail("Waypoint at " + node.getDisplayCoords() + " is not on navigable water.");
                    return;
                }
                anchoredNodes.add(new TugRouteNode(node.getName(), anchor.get().getX(), anchor.get().getY(), anchor.get().getZ()));
            }
            compiled.clear();
            compiled.addAll(anchoredNodes);

            if (compiled.size() == 1) {
                this.initialDirections = List.of();
                this.waypointPositions = Set.of();
                succeed();
                return;
            }

            for (int i = 0; i < compiled.size(); i++) {
                if (Math.sqrt(compiled.get(i).toBlockPos().distSqr(compiled.get((i + 1) % compiled.size()).toBlockPos())) > maxSegmentDistance) {
                    this.initialDirections = List.of();
                    this.waypointPositions = Set.of();
                    fail("Waypoint " + (i + 1) + " is too far from waypoint " + (((i + 1) % compiled.size()) + 1) + ".");
                    return;
                }
            }

            this.waypointPositions = new HashSet<>();
            for (TugRouteNode node : compiled) {
                this.waypointPositions.add(node.toBlockPos());
            }
            this.initialDirections = getInitialDirections(level, compiled.getFirst().toBlockPos(), compiled.get(1).toBlockPos());
            if (this.initialDirections.isEmpty()) {
                fail("Could not find a navigable departure from waypoint 1.");
            }
        }

        public PreviewPathStatus advance(int nodeBudget) {
            if (status != PreviewPathStatus.SEARCHING) return status;

            if (activePathfinder == null) {
                beginSegment();
            }
            if (status != PreviewPathStatus.SEARCHING) return status;

            PreviewPathStatus pathStatus = activePathfinder.advance(nodeBudget);
            if (pathStatus == PreviewPathStatus.SEARCHING) return status;
            if (pathStatus == PreviewPathStatus.FAILED) {
                restartWithNextInitialDirection();
                return status;
            }

            TugRouteSegment segment = activePathfinder.getResult().orElseThrow();
            segments.add(segment);
            incomingDirection = activePathfinder.getArrivalDirection();
            for (TugRoutePoint point : segment.getPoints()) occupied.add(point.toBlockPos());
            segmentIndex++;
            activePathfinder = null;
            if (segmentIndex == compiled.size()) succeed();
            return status;
        }

        private void beginSegment() {
            if (initialDirectionIndex >= initialDirections.size()) {
                fail("Could not compile a non-intersecting tug rail for this route.");
                return;
            }
            BlockPos start = compiled.get(segmentIndex).toBlockPos();
            BlockPos goal = compiled.get((segmentIndex + 1) % compiled.size()).toBlockPos();
            Set<BlockPos> blocked = new HashSet<>(waypointPositions);
            blocked.remove(start);
            blocked.remove(goal);
            Direction initial = initialDirections.get(initialDirectionIndex);
            activePathfinder = new PreviewPathfinder(level, start, goal, incomingDirection,
                segmentIndex == 0 ? initial : null,
                segmentIndex == compiled.size() - 1 ? initial : null,
                occupied, blocked);
        }

        private void restartWithNextInitialDirection() {
            initialDirectionIndex++;
            segmentIndex = 0;
            incomingDirection = null;
            activePathfinder = null;
            segments.clear();
            occupied.clear();
            if (initialDirectionIndex >= initialDirections.size()) {
                fail("Could not compile a non-intersecting tug rail for this route.");
            }
        }

        private void succeed() {
            compiled.setSegments(segments);
            result = CompileResult.success(compiled);
            status = PreviewPathStatus.FOUND;
        }

        private void fail(String error) {
            result = CompileResult.failure(error);
            status = PreviewPathStatus.FAILED;
        }

        public PreviewPathStatus getStatus() { return status; }
        public CompileResult getResult() { return result; }
    }

    public static RouteCompileSession createCompileSession(Level level, TugRoute route, double maxSegmentDistance) {
        return new RouteCompileSession(level, route, maxSegmentDistance);
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
