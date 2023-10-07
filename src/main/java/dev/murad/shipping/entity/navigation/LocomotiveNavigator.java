package dev.murad.shipping.entity.navigation;

import dev.murad.shipping.block.rail.MultiShapeRail;
import dev.murad.shipping.block.rail.SwitchRail;
import dev.murad.shipping.entity.custom.train.locomotive.AbstractLocomotiveEntity;
import dev.murad.shipping.util.LocoRoute;
import dev.murad.shipping.util.LocoRouteNode;
import dev.murad.shipping.util.RailHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class LocomotiveNavigator {
    private final Set<BlockPos> routeNodes;
    private final Set<BlockPos> visitedNodes;
    private final HashMap<BlockPos, Direction> decisionCache;

    private final AbstractLocomotiveEntity locomotive;

    private static final String ROUTE_TAG = "route";
    private static final String VISITED_TAG = "visited";

    public int getRouteSize(){
        return routeNodes.size();
    }

    public int getVisitedSize(){
        return visitedNodes.size();
    }


    private void reset(){
        this.visitedNodes.clear();
        this.routeNodes.clear();
        this.decisionCache.clear();
    }

    public  LocomotiveNavigator(AbstractLocomotiveEntity locomotive) {
        this.locomotive = locomotive;
        this.decisionCache = new HashMap<>();
        this.visitedNodes = new HashSet<>();
        this.routeNodes = new HashSet<>();
        reset();
    }

    private Optional<Direction> getDirectionFromHorizontalOffset(int x, int z) {
        if (x > 0) return Optional.of(Direction.EAST);
        if (x < 0) return Optional.of(Direction.WEST);
        if (z > 0) return Optional.of(Direction.SOUTH);
        if (z < 0) return Optional.of(Direction.NORTH);
        return Optional.empty();
    }

    public void serverTick(){
        RailHelper.getRail(locomotive.getOnPos().above(), locomotive.level()).ifPresent(railPos ->{
            if(routeNodes.contains(railPos)){
                visitedNodes.add(railPos);
            }
            if(visitedNodes.size() == routeNodes.size()){
                visitedNodes.clear();
            }
            decisionCache.remove(railPos);

            // guaranteed not null on serverside
            BlockPos oldHorizontalBlockPos = locomotive.getOldHorizontalBlockPos();
            BlockPos blockPos = locomotive.getBlockPos();

            // figure out direction the locomotive came from.
            BlockPos offset = blockPos.offset(oldHorizontalBlockPos.multiply(-1));
            Optional<Direction> moveDirOpt = getDirectionFromHorizontalOffset(offset.getX(), offset.getZ());
            Direction moveDir = moveDirOpt.orElse(locomotive.getDirection());

            locomotive.getRailHelper().getNext(railPos, moveDir).ifPresent(pair -> {
                var nextRail = pair.getFirst();
                var prevExitTaken = pair.getSecond();
                var state = locomotive.level().getBlockState(nextRail);
                if (state.getBlock() instanceof MultiShapeRail s && s.isAutomaticSwitching()){
                    var choices = s.getPossibleOutputDirections(state, prevExitTaken.getOpposite()).stream().toList();
                    if (choices.size() == 1) {
                        s.setRailState(state, locomotive.level(), nextRail, prevExitTaken.getOpposite(), choices.get(0));
                    } else if(choices.size() > 1 && !routeNodes.isEmpty()) {
                        Set<BlockPos> potential = new HashSet<>(routeNodes);
                        potential.removeAll(visitedNodes);
                        if(!decisionCache.containsKey(nextRail)){
                            var decision = locomotive.getRailHelper()
                                   .pickCheaperDir(choices, nextRail,
                                           RailHelper.samePositionHeuristicSet(potential), locomotive.level());
                            decisionCache.put(nextRail, decision);
                        };
                        s.setRailState(state, locomotive.level(), nextRail, prevExitTaken.getOpposite(), decisionCache.get(nextRail));
                    }
                }
            });
        });
    }

    public void updateWithLocoRouteItem(LocoRoute route) {
        Set<BlockPos> newRouteNodes = route.stream().map(LocoRouteNode::toBlockPos).collect(Collectors.toSet());
        if (newRouteNodes.equals(routeNodes)) return;

        reset();
        routeNodes.addAll(newRouteNodes);
    }

    public void loadFromNbt(@Nullable CompoundTag tag) {
        reset();
        if (tag == null) return;

        // list of intarrays (type 11)
        routeNodes.addAll(convertTagToSet(tag.getList(ROUTE_TAG, Tag.TAG_INT_ARRAY)));
        visitedNodes.addAll(convertTagToSet(tag.getList(VISITED_TAG, Tag.TAG_INT_ARRAY)));
    }

    public CompoundTag saveToNbt(){
        CompoundTag tag = new CompoundTag();
        tag.put(ROUTE_TAG, convertSetToTag(routeNodes));
        tag.put(VISITED_TAG, convertSetToTag(visitedNodes));
        return tag;
    }

    private static Set<BlockPos> convertTagToSet(@Nullable ListTag tag) {
        if (tag == null) return new HashSet<>();
        HashSet<BlockPos> set = new HashSet<>();

        for (int i = 0; i < tag.size(); i++) {
            int[] pos = tag.getIntArray(i);
            if (pos.length != 3) continue;
            set.add(new BlockPos(pos[0], pos[1], pos[2]));
        }
        return set;
    }

    private static ListTag convertSetToTag(Set<BlockPos> set) {
        ListTag tag = new ListTag();
        for (BlockPos pos : set) {
            tag.add(new IntArrayTag(List.of(pos.getX(), pos.getY(), pos.getZ())));
        }
        return tag;
    }


}
