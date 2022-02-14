package dev.murad.shipping.util;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TugRoute extends ArrayList<TugRouteNode> {

    private static final String NAME_TAG = "name";
    private static final String NODES_TAG = "nodes";
    private static final String HASH_TAG = "hash"; // # :)

    private String name;

    public TugRoute(String name, List<TugRouteNode> nodes) {
        super(nodes);
        this.name = name;
    }

    public TugRoute(List<TugRouteNode> nodes) {
        this(null, nodes);
    }

    public TugRoute() {
        this(null, new ArrayList<>());
    }

    public boolean hasCustomName() {
        return this.name != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TugRoute that = (TugRoute) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name);
    }

    public CompoundNBT toNBT() {
        CompoundNBT tag = new CompoundNBT();

        ListNBT list = new ListNBT();
        for (TugRouteNode node : this) {
            list.add(node.toNBT());
        }

        tag.put(NODES_TAG, list);
        if (hasCustomName()) {
            tag.putString(NAME_TAG, this.name);
        }
        return tag;
    }

    public static TugRoute fromNBT(CompoundNBT tag) {
        String name = null;
        if (tag.contains(NAME_TAG)) {
            name = tag.getString(NAME_TAG);
        }

        // 10 == magic number of Compound Tag
        ListNBT nodesNBT = tag.getList(NODES_TAG, 10);
        ArrayList<TugRouteNode> nodes = new ArrayList<>();
        for (int i = 0; i < nodesNBT.size(); i++) {
            nodes.add(TugRouteNode.fromNBT(nodesNBT.getCompound(i)));
        }

        return new TugRoute(name, nodes);
    }
}
