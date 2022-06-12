package dev.murad.shipping.util;

import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class LocoRoutes {

    private static final String NAME_TAG = "name";
    private static final String OWNER_TAG = "owner";
    private static final String NODES_TAG = "nodes";

    /**
     * Old Format
     * {
     *     name: "string",
     *     owner: "string",
     *     nodes: [
     *          { ... },
     *          { ... }
     *     ]
     * }
     *
     * New Format
     * {
     *     name: "string",
     *     owner: "string",
     *     nodes: {
     *         "dimension1": [
     *              { ... }
     *         ],
     *         "dimension2": [
     *              { ... }
     *         ]
     *     }
     * }
     */

    @Nullable
    private String name;
    @Nullable
    private String owner;

    @Getter
    private HashMap<ResourceLocation, Set<LocoRouteNode>> nodes;

    public LocoRoutes(@Nullable String name,
                      @Nullable String owner,
                      Map<ResourceLocation, Set<LocoRouteNode>> nodes) {
        this.nodes = new HashMap<>(nodes);
        this.name = name;
        this.owner = owner;
    }

    public LocoRoutes(@Nullable String name, @Nullable String owner) {
        this(name, owner, new HashMap<>());
    }

    public LocoRoutes(Level level, Set<LocoRouteNode> nodes) {
        this(null, null, getNodeMapWithDefaultLevel(level, nodes));
    }

    public LocoRoutes() {
        this(null, null, new HashMap<>());
    }

    public Set<LocoRouteNode> getNodesForDimension(Level level) {
        return nodes.computeIfAbsent(level.dimension().location(), k -> new HashSet<>());
    }

    public boolean hasCustomName() {
        return this.name != null;
    }

    public boolean hasOwner() {
        return this.owner != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        LocoRoutes that = (LocoRoutes) o;
        return Objects.equals(name, that.name) && Objects.equals(owner, that.owner);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, owner);
    }

    public CompoundTag toNBT() {
        CompoundTag nodesTag = new CompoundTag();
        CompoundTag rootTag = new CompoundTag();

        for (var entry : this.nodes.entrySet()) {
            ListTag list = new ListTag();
            for (LocoRouteNode node : entry.getValue()) {
                list.add(node.toNBT());
            }

            nodesTag.put(entry.getKey().toString(), list);
        }

        if (this.name != null) {
            rootTag.putString(NAME_TAG, this.name);
        }

        if (this.owner != null) {
            rootTag.putString(OWNER_TAG, this.owner);
        }

        rootTag.put(NODES_TAG, nodesTag);
        return rootTag;
    }

    private static HashSet<LocoRouteNode> parseListOfNodes(ListTag tag) {
        if (tag == null) {
            return new HashSet<>();
        }

        HashSet<LocoRouteNode> nodes = new HashSet<>();
        for (int i = 0; i < tag.size(); i++) {
            nodes.add(LocoRouteNode.fromNBT(tag.getCompound(i)));
        }
        return nodes;
    }

    private static HashMap<ResourceLocation, Set<LocoRouteNode>> getNodeMapWithDefaultLevel(Level level, Set<LocoRouteNode> nodes) {
        HashMap<ResourceLocation, Set<LocoRouteNode>> nodeMap = new HashMap<>();
        nodeMap.put(level.dimension().location(), nodes);
        return nodeMap;
    }

    public static LocoRoutes fromNBT(CompoundTag rootTag, @Nullable Level defaultLevel) {
        if (rootTag == null) {
            return new LocoRoutes();
        }

        String name = null, owner = null;
        if (rootTag.contains(NAME_TAG)) {
            name = rootTag.getString(NAME_TAG);
        }

        if (rootTag.contains(OWNER_TAG)) {
            owner = rootTag.getString(OWNER_TAG);
        }

        Map<ResourceLocation, Set<LocoRouteNode>> nodesMap;

        // check if the tag is old style
        // todo: remove this when we migrate to 1.19
        if (rootTag.contains(NODES_TAG, Tag.TAG_LIST)) {
            // old style
            nodesMap = new HashMap<>();
            ResourceLocation defaultLocation = defaultLevel == null ?
                    new ResourceLocation("this-doesnt-matter") :
                    defaultLevel.dimension().location();
            nodesMap.put(defaultLocation,
                    parseListOfNodes(rootTag.getList(NODES_TAG, Tag.TAG_COMPOUND)));
        } else if (rootTag.contains(NODES_TAG, Tag.TAG_COMPOUND)) {
            // new style
            CompoundTag nodesTag = rootTag.getCompound(NODES_TAG);
            nodesMap = nodesTag.getAllKeys()
                    .stream()
                    .collect(Collectors.<String, ResourceLocation, Set<LocoRouteNode>>toMap(
                        ResourceLocation::new,
                        (k) -> parseListOfNodes(nodesTag.getList(k, Tag.TAG_COMPOUND))
                    ));
        } else {
            return new LocoRoutes(name, owner);
        }

        return new LocoRoutes(name, owner, nodesMap);
    }

    public boolean isEmpty(Level level) {
        return !this.nodes.containsKey(level.dimension().location()) ||
                this.nodes.get(level.dimension().location()).isEmpty();
    }

    public boolean isEmpty() {
        return this.nodes.values()
                .stream()
                .allMatch(Set::isEmpty);
    }

    public void cleanup() {
        for (ResourceLocation k : this.nodes.keySet()) {
            if (this.nodes.getOrDefault(k, new HashSet<>()).isEmpty()) {
                this.nodes.remove(k);
            }
        }
    }
}
