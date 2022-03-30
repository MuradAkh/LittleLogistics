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

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    public LocomotiveNavigator(AbstractLocomotiveEntity locomotive) {
        this.locomotive = locomotive;
        this.decisionCache = new HashMap<>();
        this.visitedNodes = new HashSet<>();
        this.routeNodes = new HashSet<>();
        reset();
    }

    public void serverTick(){
        RailHelper.getRail(locomotive.getOnPos().above(), locomotive.level).ifPresent(railPos ->{
            if(routeNodes.contains(railPos)){
                visitedNodes.add(railPos);
            }
            if(visitedNodes.size() == routeNodes.size()){
                visitedNodes.clear();
            }
            decisionCache.remove(railPos);

            locomotive.getRailHelper().getNext(railPos, locomotive.getDirection()).ifPresent(pair -> {
                var nextRail = pair.getFirst();
                var prevExitTaken = pair.getSecond();
                var state = locomotive.getLevel().getBlockState(nextRail);
                if (state.getBlock() instanceof MultiShapeRail s && s.isAutomaticSwitching()){
                    var choices = s.getPossibleOutputDirections(state, prevExitTaken.getOpposite()).stream().toList();
                    if (choices.size() == 1) {
                        s.setRailState(state, locomotive.level, nextRail, prevExitTaken.getOpposite(), choices.get(0));
                    } else if(choices.size() > 1) {
                        Set<BlockPos> potential = new HashSet<>(routeNodes);
                        potential.removeAll(visitedNodes);
                        if(!decisionCache.containsKey(nextRail)){
                            var decision = locomotive.getRailHelper()
                                   .pickCheaperDir(choices, nextRail,
                                           RailHelper.samePositionHeuristicSet(potential), locomotive.getLevel());

                            decisionCache.put(nextRail, decision);
                        };
                        s.setRailState(state, locomotive.level, nextRail, prevExitTaken.getOpposite(), decisionCache.get(nextRail));
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

        // list of lists
        routeNodes.addAll(convertTagToSet(tag.getList(ROUTE_TAG, 9)));
        visitedNodes.addAll(convertTagToSet(tag.getList(VISITED_TAG, 9)));
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
