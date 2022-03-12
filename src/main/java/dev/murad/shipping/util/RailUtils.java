package dev.murad.shipping.util;

import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import dev.murad.shipping.block.rail.MultiExitRailBlock;
import dev.murad.shipping.entity.custom.train.AbstractTrainCarEntity;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class RailUtils {
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
    public static RailShape getShape(BlockPos pos, Level level, Optional<AbstractMinecart> car) {
        var state = level.getBlockState(pos);
        return ((BaseRailBlock) state.getBlock()).getRailDirection(state, level, pos, car.orElse(null));
    }

    @NotNull
    public static RailShape getShape(BlockPos pos, Level level, Direction direction) {

        var state = level.getBlockState(pos);
        if(state.getBlock() instanceof MultiExitRailBlock){
            return ((MultiExitRailBlock) state.getBlock()).getRailShapeFromDirection(state, pos, level, direction);
        } else {
            return ((BaseRailBlock) state.getBlock()).getRailDirection(state, level, pos, null);
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

    public static Optional<Pair<Direction, Integer>> traverseBi(BlockPos railPos, Level level, BiPredicate<Level, BlockPos> predicate, int limit, AbstractTrainCarEntity car){
        return getRail(railPos, level).flatMap(pos -> {
            var shape = getShape(pos, level, car.getDirection().getOpposite());
            var dirs = EXITS_DIRECTION.get(shape);
            var first = traverse(pos, level, dirs.getSecond().horizontal.getOpposite(), predicate, limit);
            var second = traverse(pos, level, dirs.getFirst().horizontal.getOpposite(), predicate, limit);
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

    public static Optional<Integer> traverse(BlockPos railPos, Level level, Direction prevExitTaken, BiPredicate<Level, BlockPos> predicate, int limit){
        if(predicate.test(level, railPos)){
            return Optional.of(0);
        } else if (limit < 1){
            return Optional.empty();
        }
        var entrance = prevExitTaken.getOpposite();
        return getRail(railPos, level).flatMap(pos -> {
            var shape = getShape(pos, level, prevExitTaken);
            return getOtherExit(entrance, shape).flatMap(raildir ->
                    traverse(raildir.above ? pos.relative(raildir.horizontal).above() : pos.relative(raildir.horizontal), level, raildir.horizontal, predicate, limit - 1).map(ans -> ans + 1));

        });
    }

    public static BiPredicate<Level, BlockPos> samePositionPredicate(AbstractTrainCarEntity entity){
        return (level, p) -> getRail(p, level).flatMap(pos ->
            getRail(entity.getOnPos().above(), level).map(rp -> rp.equals(pos))).orElse(false);
    }

    public static Vec3 toVec3(Vec3i dir) {
        return new Vec3(dir.getX(), dir.getY(), dir.getZ());
    }
}
