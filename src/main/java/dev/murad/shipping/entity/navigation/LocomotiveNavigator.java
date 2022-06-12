package dev.murad.shipping.entity.navigation;

import dev.murad.shipping.block.rail.MultiShapeRail;
import dev.murad.shipping.entity.custom.train.locomotive.AbstractLocomotiveEntity;
import dev.murad.shipping.util.LocoRoute;
import dev.murad.shipping.util.LocoRouteNode;
import dev.murad.shipping.util.RailHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class LocomotiveNavigator {
    private final Map<ResourceKey<Level>, Set<BlockPos>> routeNodes;
    private final Map<ResourceKey<Level>, Set<BlockPos>> visitedNodes;
    private final HashMap<BlockPos, Direction> decisionCache; // not saved

    private final AbstractLocomotiveEntity locomotive;

    private static final String ROUTE_TAG_LEGACY = "route";
    private static final String VISITED_TAG_LEGACY = "visited";

    private static final String ROUTE_TAG = "routeN";
    private static final String VISITED_TAG = "visitedN";

    public int getRouteSize(){
        return routeNodes().size();
    }

    public int getVisitedSize(){
        return visitedNodes().size();
    }


    private void reset(){
        this.visitedNodes.clear();
        this.routeNodes.clear();
        this.decisionCache.clear();
    }

    public LocomotiveNavigator(AbstractLocomotiveEntity locomotive) {
        this.locomotive = locomotive;
        this.decisionCache = new HashMap<>();
        this.visitedNodes = new HashMap<>();
        this.routeNodes = new HashMap<>();
        reset();
    }

    private Optional<Direction> getDirectionFromHorizontalOffset(int x, int z) {
        if (x > 0) return Optional.of(Direction.EAST);
        if (x < 0) return Optional.of(Direction.WEST);
        if (z > 0) return Optional.of(Direction.SOUTH);
        if (z < 0) return Optional.of(Direction.NORTH);
        return Optional.empty();
    }

    private Set<BlockPos> routeNodes(){
        return routeNodes.getOrDefault(locomotive.level.dimension(), new HashSet<>());
    }

    private Set<BlockPos> visitedNodes(){
        if(!visitedNodes.containsKey(locomotive.level.dimension())){
            visitedNodes.put(locomotive.level.dimension(), new HashSet<>());
        }
        return visitedNodes.get(locomotive.level.dimension());
    }


    public void serverTick(){
        RailHelper.getRail(locomotive.getOnPos().above(), locomotive.level).ifPresent(railPos ->{
            if(routeNodes().contains(railPos)){
                visitedNodes().add(railPos);
            }
            if(visitedNodes().size() == routeNodes().size()){
                visitedNodes().clear();
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
                    } else if(choices.size() > 1 && !routeNodes.isEmpty()) {
                        Set<BlockPos> potential = new HashSet<>(routeNodes());
                        potential.removeAll(visitedNodes());
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

    public void updateWithLocoRouteItem(Map<ResourceKey<Level>, LocoRoute> routes) {
        Map<ResourceKey<Level>, Set<BlockPos>> newRouteNodes = new HashMap<>();

        loadNodes(routes, newRouteNodes);

        if (newRouteNodes.equals(routeNodes)) return;

        reset();
        routeNodes.putAll(newRouteNodes);
    }

    private void loadNodes(Map<ResourceKey<Level>, LocoRoute> source, Map<ResourceKey<Level>, Set<BlockPos>> target) {
        source.forEach((key, value) -> {
            target.put(key, value.stream().map(LocoRouteNode::toBlockPos).collect(Collectors.toSet()));
        });
    }

    public void loadFromNbt(@Nullable CompoundTag tag) {
        reset();
        if (tag == null) return;

        if(tag.contains(ROUTE_TAG)) {
            routeNodes.putAll(convertTagToMap(tag.getCompound(ROUTE_TAG)));
        }

        if(tag.contains(VISITED_TAG)) {
            visitedNodes.putAll(convertTagToMap(tag.getCompound(VISITED_TAG)));
        }

        // legacy routes
        if(tag.contains(ROUTE_TAG_LEGACY, Tag.TAG_INT_ARRAY)) {
            routeNodes().addAll(convertTagToSet(tag.getList(ROUTE_TAG_LEGACY, Tag.TAG_INT_ARRAY)));
        }

        if(tag.contains(VISITED_TAG_LEGACY, Tag.TAG_INT_ARRAY)) {
            visitedNodes().addAll(convertTagToSet(tag.getList(VISITED_TAG_LEGACY, Tag.TAG_INT_ARRAY)));
        }
    }

    public CompoundTag saveToNbt(){
        CompoundTag tag = new CompoundTag();
        tag.put(ROUTE_TAG, convertMapToTag(routeNodes));
        tag.put(VISITED_TAG, convertMapToTag(visitedNodes));
        return tag;
    }

    private static Map<ResourceKey<Level>, Set<BlockPos>> convertTagToMap(CompoundTag tag){
        return tag
                .getAllKeys()
                .stream()
                .collect(Collectors.toMap(key -> ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(key)),
                        key -> convertTagToSet(tag.getList(key, Tag.TAG_INT_ARRAY))
                ));
    }

    private static CompoundTag convertMapToTag(Map<ResourceKey<Level>, Set<BlockPos>> map){
        CompoundTag tag = new CompoundTag();
        map.forEach((key, value) -> tag.put(key.location().toString(), convertSetToTag(value)));

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
