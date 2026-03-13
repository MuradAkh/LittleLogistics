package dev.murad.shipping.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class TugRoute extends ArrayList<TugRouteNode> {

    public static final Codec<TugRoute> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.optionalFieldOf("name")
                .forGetter(r -> Optional.ofNullable(r.name)),
            TugRouteNode.CODEC.listOf().fieldOf("nodes")
                .forGetter(r -> List.copyOf(r))
        ).apply(instance, (name, nodes) -> new TugRoute(name.orElse(null), nodes))
    );

    public static final StreamCodec<FriendlyByteBuf, TugRoute> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), r -> Optional.ofNullable(r.name),
            TugRouteNode.STREAM_CODEC.apply(ByteBufCodecs.list()), r -> List.copyOf(r),
            (name, nodes) -> new TugRoute(name.orElse(null), nodes)
        );

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

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();

        ListTag list = new ListTag();
        for (TugRouteNode node : this) {
            list.add(node.toNBT());
        }

        tag.put(NODES_TAG, list);
        if (hasCustomName()) {
            tag.putString(NAME_TAG, this.name);
        }
        return tag;
    }

    public static TugRoute fromNBT(CompoundTag tag) {
        String name = null;
        if (tag.contains(NAME_TAG)) {
            name = tag.getString(NAME_TAG);
        }

        // 10 == magic number of Compound Tag
        ListTag nodesNBT = tag.getList(NODES_TAG, 10);
        ArrayList<TugRouteNode> nodes = new ArrayList<>();
        for (int i = 0; i < nodesNBT.size(); i++) {
            nodes.add(TugRouteNode.fromNBT(nodesNBT.getCompound(i)));
        }

        return new TugRoute(name, nodes);
    }
}
