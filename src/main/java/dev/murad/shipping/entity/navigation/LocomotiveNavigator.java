package dev.murad.shipping.entity.navigation;

import dev.murad.shipping.block.rail.MultiShapeRail;
import dev.murad.shipping.entity.custom.train.locomotive.AbstractLocomotiveEntity;
import dev.murad.shipping.util.LocoRoutes;
import dev.murad.shipping.util.LocoRouteNode;
import dev.murad.shipping.util.RailHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class LocomotiveNavigator {
    private final Map<ResourceLocation, Set<BlockPos>> routeNodeMap;
    private final Map<ResourceLocation, Set<BlockPos>> visitedNodeMap;

    // only for the current dimension
    private final HashMap<BlockPos, Direction> decisionCache;

    private final AbstractLocomotiveEntity locomotive;

    private static final String ROUTE_TAG = "route";
    private static final String VISITED_TAG = "visited";

    public int getRouteSize(){
        return getCurrentRouteNodes().size();
    }

    public int getVisitedSize(){
        return getCurrentVisitedNodes().size();
    }


    private void reset(){
        this.visitedNodeMap.clear();
        this.routeNodeMap.clear();
        this.decisionCache.clear();
    }

    public LocomotiveNavigator(AbstractLocomotiveEntity locomotive) {
        this.locomotive = locomotive;
        this.decisionCache = new HashMap<>();
        this.visitedNodeMap = new HashMap<>();
        this.routeNodeMap = new HashMap<>();
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
        ResourceLocation currentDimension = locomotive.level.dimension().location();

        RailHelper.getRail(locomotive.getOnPos().above(), locomotive.level).ifPresent(railPos ->{
            if(getCurrentRouteNodes().contains(railPos)){
                getCurrentVisitedNodes().add(railPos);
            }
            if(getCurrentVisitedNodes().size() == getCurrentRouteNodes().size()){
                getCurrentVisitedNodes().clear();
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
                var state = locomotive.getLevel().getBlockState(nextRail);
                if (state.getBlock() instanceof MultiShapeRail s && s.isAutomaticSwitching()){
                    var choices = s.getPossibleOutputDirections(state, prevExitTaken.getOpposite()).stream().toList();
                    if (choices.size() == 1) {
                        s.setRailState(state, locomotive.level, nextRail, prevExitTaken.getOpposite(), choices.get(0));
                    } else if(choices.size() > 1 && !getCurrentRouteNodes().isEmpty()) {
                        Set<BlockPos> potential = new HashSet<>(getCurrentRouteNodes());
                        potential.removeAll(getCurrentVisitedNodes());
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

    public void updateWithLocoRouteItem(LocoRoutes route) {
        Map<ResourceLocation, Set<BlockPos>> newRouteNodes = route.getNodes()
                .entrySet().stream()
                .collect(Collectors.<Map.Entry<ResourceLocation, Set<LocoRouteNode>>, ResourceLocation, Set<BlockPos>>toMap(Map.Entry::getKey,
                        e -> e.getValue().stream().map(LocoRouteNode::toBlockPos).collect(Collectors.toSet())));

        if (routesAreIdentical(newRouteNodes,routeNodeMap)) return;

        reset();
        routeNodeMap.putAll(newRouteNodes);
    }

    /**
     * Handles the edge case:
     * a => null
     * b => empty set
     */
    private boolean routesAreIdentical(Map<ResourceLocation, Set<BlockPos>> a, Map<ResourceLocation, Set<BlockPos>> b) {
        // check every key in a is in b
        return a.entrySet().stream().allMatch(e -> {
            ResourceLocation l = e.getKey();
            Set<BlockPos> route = e.getValue();
            return (route.isEmpty() && !b.containsKey(l)) || route.equals(b.get(l));
        }) && b.entrySet().stream().allMatch(e -> {
            ResourceLocation l = e.getKey();
            Set<BlockPos> route = e.getValue();
            return (route.isEmpty() && !a.containsKey(l)) || route.equals(a.get(l));
        });
    }

    public void loadFromNbt(@Nullable CompoundTag tag) {
        reset();
        if (tag == null) return;

        // check if tag is legacy version
        if (tag.contains(ROUTE_TAG, Tag.TAG_INT_ARRAY)) {
            getCurrentRouteNodes().addAll(convertTagToSet(tag.getList(ROUTE_TAG, Tag.TAG_INT_ARRAY)));
        }

        if (tag.contains(ROUTE_TAG, Tag.TAG_COMPOUND)) {
            routeNodeMap.putAll(convertTagToMap(tag.getCompound(ROUTE_TAG)));
        }

        // check if tag is legacy version
        if (tag.contains(VISITED_TAG, Tag.TAG_INT_ARRAY)) {
            getCurrentVisitedNodes().addAll(convertTagToSet(tag.getList(VISITED_TAG, Tag.TAG_INT_ARRAY)));
        }

        if (tag.contains(VISITED_TAG, Tag.TAG_COMPOUND)) {
            visitedNodeMap.putAll(convertTagToMap(tag.getCompound(VISITED_TAG)));
        }
    }

    public CompoundTag saveToNbt(){
        CompoundTag tag = new CompoundTag();
        tag.put(ROUTE_TAG, convertMapToTag(routeNodeMap));
        tag.put(VISITED_TAG, convertMapToTag(visitedNodeMap));
        return tag;
    }

    private static Map<ResourceLocation, Set<BlockPos>> convertTagToMap(@Nullable CompoundTag tag) {
        if (tag == null) return new HashMap<>();
        return tag.getAllKeys().stream().collect(Collectors.toMap(ResourceLocation::new,
            key -> convertTagToSet(tag.getList(key, Tag.TAG_INT_ARRAY))
        ));
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

    private static CompoundTag convertMapToTag(Map<ResourceLocation, Set<BlockPos>> map) {
        CompoundTag tag = new CompoundTag();
        map.forEach((key, value) -> tag.put(key.toString(), convertSetToTag(value)));

        return tag;
    }

    private Set<BlockPos> getCurrentRouteNodes() {
        return routeNodeMap.computeIfAbsent(locomotive.level.dimension().location(), k -> new HashSet<>());
    }

    private Set<BlockPos> getCurrentVisitedNodes() {
        return visitedNodeMap.computeIfAbsent(locomotive.level.dimension().location(), k -> new HashSet<>());
    }
}
