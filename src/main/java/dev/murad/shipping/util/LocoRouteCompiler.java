package dev.murad.shipping.util;

import com.mojang.datafixers.util.Pair;
import dev.murad.shipping.block.rail.MultiShapeRail;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;

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

/**
 * Compiles one ordered locomotive-route segment against the actual directed rail graph.
 * The compiler never scans or validates the rest of a route.  Previously compiled
 * segments therefore remain useful even when their chunks are no longer loaded.
 */
public final class LocoRouteCompiler {
    private static final int MAX_VISITED_STATES = 32_768;

    private LocoRouteCompiler() {
    }

    public record CompileResult(boolean success, @Nullable LocoRouteSegment segment, @Nullable String error) {
        static CompileResult success(LocoRouteSegment segment) { return new CompileResult(true, segment, null); }
        static CompileResult failure(String error) { return new CompileResult(false, null, error); }
    }

    public record ReplacementCompileResult(boolean success, @Nullable LocoRouteSegment before,
                                           @Nullable LocoRouteSegment after, @Nullable String error) {
        static ReplacementCompileResult success(LocoRouteSegment before, LocoRouteSegment after) {
            return new ReplacementCompileResult(true, before, after, null);
        }

        static ReplacementCompileResult failure(String error) {
            return new ReplacementCompileResult(false, null, null, error);
        }
    }

    public record SegmentSplit(LocoRouteSegment before, LocoRouteSegment after) {
    }

    public enum PreviewStatus {
        SEARCHING,
        FOUND,
        FAILED
    }

    private record RailState(BlockPos pos, Direction travelDirection) {
    }

    private record QueueNode(RailState state, int distance) {
    }

    private record Previous(RailState state, LocoRouteStep step) {
    }

    private record Candidate(RailState state, LocoRouteStep step) {
    }

    /** Resolves a clicked position to the actual rail position, including a rail directly above it. */
    public static Optional<BlockPos> resolveRailWaypoint(Level level, BlockPos clicked) {
        for (BlockPos candidate : List.of(clicked, clicked.above(), clicked.below())) {
            if (level.getBlockState(candidate).getBlock() instanceof BaseRailBlock) {
                return Optional.of(candidate.immutable());
            }
        }
        return Optional.empty();
    }

    public static CompileResult compile(Level level, BlockPos start, BlockPos goal,
                                        @Nullable Direction startIncoming,
                                        @Nullable Direction requiredArrival) {
        Optional<BlockPos> startRail = resolveRailWaypoint(level, start);
        Optional<BlockPos> goalRail = resolveRailWaypoint(level, goal);
        if (startRail.isEmpty() || goalRail.isEmpty()) {
            return CompileResult.failure("Both locomotive route waypoints must be rails.");
        }
        if (startRail.get().equals(goalRail.get())) {
            return CompileResult.failure("A locomotive route segment cannot begin and end on the same rail.");
        }

        List<Direction> initialDirections = startIncoming == null
            ? List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)
            : List.of(startIncoming);
        CompileResult best = null;
        for (Direction direction : initialDirections) {
            CompileResult result = compileFrom(level, startRail.get(), goalRail.get(), direction, requiredArrival);
            if (!result.success()) continue;
            if (best == null || result.segment().getSteps().size() < best.segment().getSteps().size()) {
                best = result;
            }
        }
        return best != null ? best : CompileResult.failure(
            "Could not find a loaded rail path between these locomotive route waypoints.");
    }

    /**
     * Replaces one compiled segment without examining any other part of the route.
     * The two replacements retain the original entry and arrival directions, which is
     * what keeps a vertical/branching rail route mechanically continuous.
     */
    public static ReplacementCompileResult compileReplacement(Level level, BlockPos start, BlockPos target, BlockPos end,
                                                               LocoRouteSegment original) {
        Optional<SegmentSplit> existingSplit = splitAt(original, target);
        if (existingSplit.isPresent()) {
            return ReplacementCompileResult.success(existingSplit.get().before(), existingSplit.get().after());
        }

        ReplacementCompileResult best = null;
        for (Direction midpointDirection : List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)) {
            CompileResult before = compile(level, start, target, original.getStartIncomingDirection(), midpointDirection);
            if (!before.success()) continue;
            CompileResult after = compile(level, target, end, midpointDirection, original.getArrivalDirection());
            if (!after.success()) continue;
            ReplacementCompileResult result = ReplacementCompileResult.success(before.segment(), after.segment());
            if (best == null || result.before().getSteps().size() + result.after().getSteps().size()
                < best.before().getSteps().size() + best.after().getSteps().size()) {
                best = result;
            }
        }
        return best != null ? best : ReplacementCompileResult.failure(
            "Could not compile a directional replacement for this locomotive route segment.");
    }

    public static Optional<SegmentSplit> splitAt(LocoRouteSegment original, BlockPos target) {
        for (int index = 1; index < original.getSteps().size(); index++) {
            LocoRouteStep splitStep = original.getSteps().get(index);
            if (!splitStep.railPos().equals(target)) continue;
            return Optional.of(new SegmentSplit(
                new LocoRouteSegment(new ArrayList<>(original.getSteps().subList(0, index)), splitStep.incomingDirection()),
                new LocoRouteSegment(new ArrayList<>(original.getSteps().subList(index, original.getSteps().size())),
                    original.getArrivalDirection())
            ));
        }
        return Optional.empty();
    }

    public static PreviewPathfinder createPreviewPathfinder(Level level, BlockPos start, BlockPos goal,
                                                              @Nullable Direction startIncoming,
                                                              @Nullable Direction requiredArrival) {
        return new PreviewPathfinder(level, start, goal, startIncoming, requiredArrival);
    }

    private static CompileResult compileFrom(Level level, BlockPos start, BlockPos goal, Direction startIncoming,
                                             @Nullable Direction requiredArrival) {
        Comparator<QueueNode> comparator = Comparator
            .comparingInt(QueueNode::distance)
            .thenComparingLong(node -> node.state().pos().asLong())
            .thenComparingInt(node -> node.state().travelDirection().get3DDataValue());
        PriorityQueue<QueueNode> queue = new PriorityQueue<>(comparator);
        RailState initial = new RailState(start, startIncoming);
        Map<RailState, Integer> distance = new HashMap<>();
        Map<RailState, Previous> previous = new HashMap<>();
        Set<RailState> visited = new HashSet<>();
        queue.add(new QueueNode(initial, 0));
        distance.put(initial, 0);

        while (!queue.isEmpty() && visited.size() < MAX_VISITED_STATES) {
            QueueNode current = queue.poll();
            if (!visited.add(current.state())) continue;
            if (current.state().pos().equals(goal)
                && (requiredArrival == null || current.state().travelDirection() == requiredArrival)) {
                return CompileResult.success(reconstruct(current.state(), previous));
            }

            for (Candidate candidate : getNextStates(level, current.state())) {
                if (visited.contains(candidate.state())) continue;
                int nextDistance = current.distance() + 1;
                if (nextDistance < distance.getOrDefault(candidate.state(), Integer.MAX_VALUE)) {
                    distance.put(candidate.state(), nextDistance);
                    previous.put(candidate.state(), new Previous(current.state(), candidate.step()));
                    queue.add(new QueueNode(candidate.state(), nextDistance));
                }
            }
        }
        return CompileResult.failure("No compatible rail traversal was found for this segment.");
    }

    /** Incremental version of the segment compiler used only for client-side visual previews. */
    public static final class PreviewPathfinder {
        private final Level level;
        private final BlockPos goal;
        @Nullable private final Direction requiredArrival;
        private final PriorityQueue<QueueNode> queue;
        private final Map<RailState, Integer> distance = new HashMap<>();
        private final Map<RailState, Previous> previous = new HashMap<>();
        private final Set<RailState> visited = new HashSet<>();
        private PreviewStatus status = PreviewStatus.SEARCHING;
        @Nullable private LocoRouteSegment result;
        @Nullable private String error;

        private PreviewPathfinder(Level level, BlockPos requestedStart, BlockPos requestedGoal,
                                  @Nullable Direction startIncoming, @Nullable Direction requiredArrival) {
            this.level = level;
            this.requiredArrival = requiredArrival;
            this.queue = new PriorityQueue<>(Comparator
                .comparingInt(QueueNode::distance)
                .thenComparingLong(node -> node.state().pos().asLong())
                .thenComparingInt(node -> node.state().travelDirection().get3DDataValue()));
            Optional<BlockPos> start = resolveRailWaypoint(level, requestedStart);
            Optional<BlockPos> resolvedGoal = resolveRailWaypoint(level, requestedGoal);
            if (start.isEmpty() || resolvedGoal.isEmpty()) {
                goal = BlockPos.ZERO;
                fail("Both locomotive route waypoints must be rails.");
                return;
            }
            goal = resolvedGoal.get();
            if (start.get().equals(goal)) {
                fail("A locomotive route segment cannot begin and end on the same rail.");
                return;
            }
            List<Direction> initialDirections = startIncoming == null
                ? List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)
                : List.of(startIncoming);
            for (Direction direction : initialDirections) {
                RailState state = new RailState(start.get(), direction);
                queue.add(new QueueNode(state, 0));
                distance.put(state, 0);
            }
        }

        public PreviewStatus advance(int nodeBudget) {
            if (status != PreviewStatus.SEARCHING) return status;
            int remaining = Math.max(1, nodeBudget);
            while (remaining-- > 0 && !queue.isEmpty() && visited.size() < MAX_VISITED_STATES) {
                QueueNode current = queue.poll();
                if (!visited.add(current.state())) continue;
                if (current.state().pos().equals(goal)
                    && (requiredArrival == null || current.state().travelDirection() == requiredArrival)) {
                    result = reconstruct(current.state(), previous);
                    status = PreviewStatus.FOUND;
                    return status;
                }
                for (Candidate candidate : getNextStates(level, current.state())) {
                    if (visited.contains(candidate.state())) continue;
                    int nextDistance = current.distance() + 1;
                    if (nextDistance < distance.getOrDefault(candidate.state(), Integer.MAX_VALUE)) {
                        distance.put(candidate.state(), nextDistance);
                        previous.put(candidate.state(), new Previous(current.state(), candidate.step()));
                        queue.add(new QueueNode(candidate.state(), nextDistance));
                    }
                }
            }
            if (queue.isEmpty()) fail("No compatible rail traversal was found for this segment.");
            else if (visited.size() >= MAX_VISITED_STATES) fail("Locomotive route preview exceeded its rail search limit.");
            return status;
        }

        public PreviewStatus getStatus() { return status; }
        public Optional<LocoRouteSegment> getResult() { return Optional.ofNullable(result); }
        @Nullable public String getError() { return error; }

        private void fail(String message) {
            error = message;
            status = PreviewStatus.FAILED;
        }
    }

    private static LocoRouteSegment reconstruct(RailState goal, Map<RailState, Previous> previous) {
        List<LocoRouteStep> reversed = new ArrayList<>();
        RailState current = goal;
        Previous link;
        while ((link = previous.get(current)) != null) {
            reversed.add(link.step());
            current = link.state();
        }
        List<LocoRouteStep> steps = new ArrayList<>(reversed.size());
        for (int index = reversed.size() - 1; index >= 0; index--) steps.add(reversed.get(index));
        return new LocoRouteSegment(steps, goal.travelDirection());
    }

    private static List<Candidate> getNextStates(Level level, RailState state) {
        if (!level.hasChunkAt(state.pos())) return List.of();
        BlockState blockState = level.getBlockState(state.pos());
        if (!(blockState.getBlock() instanceof BaseRailBlock)) return List.of();

        List<Exit> exits = getExits(level, state, blockState);
        List<Candidate> next = new ArrayList<>(exits.size());
        for (Exit exit : exits) {
            BlockPos candidatePos = exit.above()
                ? state.pos().relative(exit.direction()).above()
                : state.pos().relative(exit.direction());
            if (!level.hasChunkAt(candidatePos)) continue;
            Optional<BlockPos> nextRail = RailHelper.getRail(candidatePos, level);
            if (nextRail.isEmpty()) continue;
            LocoRouteStep step = new LocoRouteStep(state.pos(), state.travelDirection(), exit.direction());
            next.add(new Candidate(new RailState(nextRail.get().immutable(), exit.direction()), step));
        }
        return next;
    }

    private record Exit(Direction direction, boolean above) {
    }

    private static List<Exit> getExits(Level level, RailState state, BlockState blockState) {
        Direction inputSide = state.travelDirection().getOpposite();
        if (blockState.getBlock() instanceof MultiShapeRail multiShapeRail) {
            return multiShapeRail.getExitDirections(blockState, inputSide).stream()
                .sorted(Comparator.comparingInt(Direction::get3DDataValue))
                .map(direction -> new Exit(direction, false)).toList();
        }

        RailShape shape = RailHelper.getShape(state.pos(), level);
        Pair<RailHelper.RailDir, RailHelper.RailDir> exits = RailHelper.EXITS_DIRECTION.get(shape);
        if (exits == null) return List.of();
        RailHelper.RailDir first = exits.getFirst();
        RailHelper.RailDir second = exits.getSecond();
        if (first.horizontal == inputSide) return List.of(new Exit(second.horizontal, second.above));
        if (second.horizontal == inputSide) return List.of(new Exit(first.horizontal, first.above));
        return List.of();
    }
}
