package dev.murad.shipping.util;

import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import dev.murad.shipping.block.rail.MultiShapeRail;
import dev.murad.shipping.entity.custom.train.AbstractTrainCarEntity;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class RailHelper {
    private final AbstractMinecart minecart;

    public RailHelper (AbstractMinecart minecart){
        this.minecart = minecart;
    }

    public static final Map<RailShape, Pair<Vec3i, Vec3i>> EXITS = Util.make(Maps.newEnumMap(RailShape.class), (map) -> {
        Vec3i west = Direction.WEST.getNormal();
        Vec3i east = Direction.EAST.getNormal();
        Vec3i north = Direction.NORTH.getNormal();
        Vec3i south = Direction.SOUTH.getNormal();
        Vec3i westb = west.below();
        Vec3i eastb = east.below();
        Vec3i nothb = north.below();
        Vec3i southb = south.below();
        map.put(RailShape.NORTH_SOUTH, Pair.of(north, south));
        map.put(RailShape.EAST_WEST, Pair.of(west, east));
        map.put(RailShape.ASCENDING_EAST, Pair.of(westb, east));
        map.put(RailShape.ASCENDING_WEST, Pair.of(west, eastb));
        map.put(RailShape.ASCENDING_NORTH, Pair.of(north, southb));
        map.put(RailShape.ASCENDING_SOUTH, Pair.of(nothb, south));
        map.put(RailShape.SOUTH_EAST, Pair.of(south, east));
        map.put(RailShape.SOUTH_WEST, Pair.of(south, west));
        map.put(RailShape.NORTH_WEST, Pair.of(north, west));
        map.put(RailShape.NORTH_EAST, Pair.of(north, east));
    });
    private static final int MAX_VISITED = 200;

    public static class RailDir {
        public Direction horizontal;
        public boolean above;

        RailDir(Direction h, boolean v){
            horizontal = h;
            above = v;
        }

        RailDir(Direction h){
            horizontal = h;
            above = false;
        }
    }

    public static final Map<RailShape, Pair<RailDir, RailDir>> EXITS_DIRECTION = Util.make(Maps.newEnumMap(RailShape.class), (map) -> {
        map.put(RailShape.NORTH_SOUTH, Pair.of(new RailDir(Direction.NORTH), new RailDir(Direction.SOUTH)));
        map.put(RailShape.EAST_WEST, Pair.of(new RailDir(Direction.WEST), new RailDir(Direction.EAST)));
        map.put(RailShape.ASCENDING_EAST, Pair.of(new RailDir(Direction.WEST), new RailDir(Direction.EAST, true)));
        map.put(RailShape.ASCENDING_WEST, Pair.of(new RailDir(Direction.WEST, true), new RailDir(Direction.EAST)));
        map.put(RailShape.ASCENDING_NORTH, Pair.of(new RailDir(Direction.NORTH, true), new RailDir(Direction.SOUTH)));
        map.put(RailShape.ASCENDING_SOUTH, Pair.of(new RailDir(Direction.NORTH), new RailDir(Direction.SOUTH, true)));
        map.put(RailShape.SOUTH_EAST, Pair.of(new RailDir(Direction.SOUTH), new RailDir(Direction.EAST)));
        map.put(RailShape.SOUTH_WEST, Pair.of(new RailDir(Direction.WEST), new RailDir(Direction.SOUTH)));
        map.put(RailShape.NORTH_WEST, Pair.of(new RailDir(Direction.WEST), new RailDir(Direction.NORTH)));
        map.put(RailShape.NORTH_EAST, Pair.of(new RailDir(Direction.NORTH), new RailDir(Direction.EAST)));
    });

    @NotNull
    public RailShape getShape(BlockPos pos) {
        var state = minecart.level().getBlockState(pos);
        return ((BaseRailBlock) state.getBlock()).getRailDirection(state, minecart.level(), pos, minecart);
    }

    @NotNull
    public static RailShape getShape(BlockPos pos, Level level) {
        var state = level.getBlockState(pos);
        return ((BaseRailBlock) state.getBlock()).getRailDirection(state, level, pos, null);
    }

    @NotNull
    public RailShape getShape(BlockPos pos, Direction direction) {
        var state = minecart.level().getBlockState(pos);
           if(state.getBlock() instanceof MultiShapeRail){
            return ((MultiShapeRail) state.getBlock()).getVanillaRailShapeFromDirection(state, pos, minecart.level(), direction);
        } else {
            return ((BaseRailBlock) state.getBlock()).getRailDirection(state, minecart.level(), pos, minecart);
        }
    }


    public static Optional<BlockPos> getRail(BlockPos inpos, Level level){ // if using with carts, pass in getOnPos.above
        for(var pos: Arrays.asList(inpos, inpos.below())) { // check for ascending rail.
            var state = level.getBlockState(pos);
            if (state.getBlock() instanceof BaseRailBlock) {
                return Optional.of(pos);
            }
        }
        return Optional.empty();
    }

    public static Direction directionFromVelocity(Vec3 deltaMovement){
        if (Math.abs(deltaMovement.x) > Math.abs(deltaMovement.z)) {
            return deltaMovement.x > 0 ? Direction.EAST : Direction.WEST;
        } else {
            return deltaMovement.z > 0 ? Direction.SOUTH : Direction.NORTH;
        }
    }

    public Optional<Pair<Direction, Integer>> traverseBi(BlockPos railPos, BiPredicate<Direction, BlockPos> predicate, int limit, AbstractTrainCarEntity car){
        return getRail(railPos, minecart.level()).flatMap(pos -> {
            var shape = getShape(pos, car.getDirection().getOpposite());
            var dirs = EXITS_DIRECTION.get(shape);
            var first = traverse(pos, minecart.level(), dirs.getSecond().horizontal.getOpposite(), predicate, limit);
            var second = traverse(pos, minecart.level(), dirs.getFirst().horizontal.getOpposite(), predicate, limit);
            if(second.isEmpty()){
                return first.map(i -> Pair.of(dirs.getFirst().horizontal, i));
            } else if (first.isEmpty()){
                return second.map(i -> Pair.of(dirs.getSecond().horizontal, i));
            } else {
                return Optional.of(first.get() < second.get() ?
                        Pair.of(dirs.getFirst().horizontal, first.get()) :
                        Pair.of(dirs.getSecond().horizontal, second.get()));
            }
        });

    }

    public static Optional<RailDir> getOtherExit(Direction direction, RailShape shape){
        var dirs = EXITS_DIRECTION.get(shape);
        if(dirs.getFirst().horizontal.equals(direction)){
            return Optional.of(dirs.getSecond());
        } else if (dirs.getSecond().horizontal.equals(direction)){
            return Optional.of(dirs.getFirst());
        } else {
            return Optional.empty();
        }

    }

    public static Optional<Vec3i> getDirectionToOtherExit(Direction direction, RailShape shape) {
        return getOtherExit(direction, shape).map(other -> direction.getNormal().subtract(other.horizontal.getNormal()));
    }

    public Optional<Integer> traverse(BlockPos railPos, Level level, Direction prevExitTaken, BiPredicate<Direction, BlockPos> predicate, int limit){
        if(predicate.test(prevExitTaken, railPos)){
            return Optional.of(0);
        } else if (limit < 1){
            return Optional.empty();
        }
        var entrance = prevExitTaken.getOpposite();
        return getRail(railPos, level).flatMap(pos -> {
            var shape = getShape(pos, prevExitTaken);
            return getOtherExit(entrance, shape).flatMap(raildir ->
                    traverse(raildir.above ? pos.relative(raildir.horizontal).above() : pos.relative(raildir.horizontal), level, raildir.horizontal, predicate, limit - 1).map(ans -> ans + 1));

        });
    }

    public Optional<Pair<BlockPos, Direction>> getNext(BlockPos railpos, Direction direction){
        var shape = getShape(railpos, direction);
        var entrance = direction.getOpposite();

        return getOtherExit(entrance, shape)
                .flatMap(raildir ->
                    getRail(raildir.above ? railpos.relative(raildir.horizontal).above() : railpos.relative(raildir.horizontal), minecart.level())
                            .map(pos -> Pair.of(pos, raildir.horizontal))
                );
    }

    private static class RailPathFindNode implements Comparable<RailPathFindNode>{
        BlockPos pos;
        Direction prevExitTaken;
        int pathLength;
        double heuristicValue;

        RailPathFindNode(BlockPos pos,
                Direction prevExitTaken,
                int pathLength, double heuristicValue){
            this.pos = pos;
            this.prevExitTaken = prevExitTaken;
            this.pathLength = pathLength;
            this.heuristicValue = heuristicValue;
        }

        @Override
        public int compareTo(@NotNull RailHelper.RailPathFindNode o) {
            if(this.heuristicValue == o.heuristicValue){
                return this.pathLength - o.pathLength;
            } else return this.heuristicValue - o.heuristicValue < 0 ? -1 : 1;
        }
    }

    /**
     * @param prevExitTaken the direction of travel for the train
     */
    private List<RailDir> getNextNodes(BlockPos pos, Direction prevExitTaken) {
        Direction inputSide = prevExitTaken.getOpposite();

        // todo: we need to check if blocks are actually loaded
        BlockState state = minecart.level().getBlockState(pos);
        if (state.getBlock() instanceof MultiShapeRail r) {
            // if rail is a MultiShapeRail, return all possible outputs from the input side
            // it doesn't matter if this rail is automatically switching.
            return r.getPossibleOutputDirections(state, inputSide)
                    .stream()
                    .map(RailDir::new)
                    .collect(Collectors.toList());
        }

        var shape = getShape(pos, prevExitTaken);
        List<RailShape> shapes = List.of(shape);
        return shapes.stream().map(shape1 -> {
            var dirs = EXITS_DIRECTION.get(shape);
            if (dirs.getFirst().horizontal.equals(inputSide)) {
                return dirs.getSecond();
            } else if (dirs.getSecond().horizontal.equals(inputSide)) {
                return dirs.getFirst();
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public Optional<RailPathFindNode> pathfind(BlockPos railPos, Direction prevDirTaken, Function<BlockPos, Double> heuristic) {
        Set<Pair<BlockPos, Direction>> visited = new HashSet<>();
        PriorityQueue<RailPathFindNode> queue = new PriorityQueue<>();
        PriorityQueue<RailPathFindNode> ends = new PriorityQueue<>();
        queue.add(new RailPathFindNode(railPos, prevDirTaken, 0, heuristic.apply(railPos)));

        while(!queue.isEmpty() && visited.size() < MAX_VISITED && queue.peek().heuristicValue > 0D){
            RailHelper.RailPathFindNode curr = queue.poll();
            // already explored this path
            if(visited.contains(Pair.of(curr.pos, curr.prevExitTaken)))
                continue;

            visited.add(Pair.of(curr.pos, curr.prevExitTaken));

            getNextNodes(curr.pos, curr.prevExitTaken).forEach(raildir -> {
                BlockPos pos = raildir.above ? curr.pos.relative(raildir.horizontal).above() : curr.pos.relative(raildir.horizontal);
                if(minecart.level().getBlockState(pos).is(Blocks.VOID_AIR)){
                    ends.add(new RailPathFindNode(pos, raildir.horizontal, curr.pathLength  + 1, heuristic.apply(pos)));
                } else {
                    getRail(pos, minecart.level()).ifPresent(nextPos -> {
                        queue.add(new RailPathFindNode(nextPos, raildir.horizontal, curr.pathLength + 1, heuristic.apply(nextPos)));
                    });
                }
            });
        }

        queue.addAll(ends);
        return queue.isEmpty() ? Optional.empty() : Optional.of(queue.peek());
    }

    public static BiPredicate<Direction, BlockPos> samePositionPredicate(AbstractTrainCarEntity entity){
        var targetRail = getRail(entity.getOnPos().above(), entity.level());
        return (direction, p) -> getRail(p, entity.level())
                .flatMap(pos -> targetRail.map(rp -> rp.equals(pos)))
                .orElse(false);
    }

    public Direction pickCheaperDir(List<Direction> directions, BlockPos pos, Function<BlockPos, Double> heuristic, Level level) {
        // get all directions where output has a possible rail
        List<Pair<Direction, BlockPos>> hasOutputDirections = directions.stream()
                .map(d -> new Pair<>(d, getRail(pos.relative(d), level)))
                .filter(p -> p.getSecond().isPresent())
                .map(p -> new Pair<>(p.getFirst(), p.getSecond().get()))
                .collect(Collectors.toList());

        // fallback
        if (hasOutputDirections.isEmpty()) return directions.get(0);

        List<Pair<Direction, RailHelper.RailPathFindNode>> hasPath = hasOutputDirections.stream()
                .map(p -> new Pair<>(p.getFirst(), pathfind(p.getSecond(), p.getFirst(), heuristic)))
                .filter(p -> p.getSecond().isPresent())
                .map(p -> new Pair<>(p.getFirst(), p.getSecond().get()))
                .collect(Collectors.toList());

        // fallback
        if (hasPath.isEmpty()) return hasOutputDirections.get(0).getFirst();

        Pair<Direction, RailPathFindNode> best = hasPath
                .stream()
                .min(Comparator.comparing(Pair::getSecond))
                .get();
        return best.getFirst();
    }

    public static Function<BlockPos, Double> samePositionHeuristic(BlockPos p){
        return (p::distSqr);
    }

    public static Function<BlockPos, Double> samePositionHeuristicSet(Set<BlockPos> potentialDestinations){
        return ((pos) -> potentialDestinations.stream().map(p -> p.distSqr(pos)).min(Double::compareTo).orElse(0D));
    }

    public static Vec3 toVec3(Vec3i dir) {
        return new Vec3(dir.getX(), dir.getY(), dir.getZ());
    }
}
