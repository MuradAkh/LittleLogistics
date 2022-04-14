package dev.murad.shipping.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import javax.annotation.Nullable;
import java.util.*;

public class LocoRoute extends HashSet<LocoRouteNode> {

    private static final String NAME_TAG = "name";
    private static final String OWNER_TAG = "owner";
    private static final String NODES_TAG = "nodes";

    @Nullable
    private String name;
    @Nullable
    private String owner;

    public LocoRoute(@Nullable String name,
                     @Nullable String owner,
                     Set<LocoRouteNode> nodes) {
        super(nodes);
        this.name = name;
        this.owner = owner;
    }

    public LocoRoute(Set<LocoRouteNode> nodes) {
        this(null, null, nodes);
    }

    public LocoRoute() {
        this(null, null, new HashSet<>());
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
        LocoRoute that = (LocoRoute) o;
        return Objects.equals(name, that.name) && Objects.equals(owner, that.owner);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, owner);
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();

        ListTag list = new ListTag();
        for (LocoRouteNode node : this) {
            list.add(node.toNBT());
        }

        tag.put(NODES_TAG, list);
        if (hasCustomName()) {
            tag.putString(NAME_TAG, this.name);
        }

        if (hasOwner()) {
            tag.putString(OWNER_TAG, this.owner);
        }

        return tag;
    }

    public static LocoRoute fromNBT(CompoundTag tag) {
        String name = null, owner = null;
        if (tag.contains(NAME_TAG)) {
            name = tag.getString(NAME_TAG);
        }

        if (tag.contains(OWNER_TAG)) {
            owner = tag.getString(OWNER_TAG);
        }

        // 10 == magic number of Compound Tag
        ListTag nodesNBT = tag.getList(NODES_TAG, 10);
        HashSet<LocoRouteNode> nodes = new HashSet<>();
        for (int i = 0; i < nodesNBT.size(); i++) {
            nodes.add(LocoRouteNode.fromNBT(nodesNBT.getCompound(i)));
        }

        return new LocoRoute(name, owner, nodes);
    }
}
