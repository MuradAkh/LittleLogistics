package dev.murad.shipping.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import javax.annotation.Nullable;
import java.util.*;

public class LocoRoute extends HashSet<LocoRouteNode> {

    public static final Codec<LocoRoute> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.optionalFieldOf("name")
                .forGetter(r -> Optional.ofNullable(r.name)),
            Codec.STRING.optionalFieldOf("owner")
                .forGetter(r -> Optional.ofNullable(r.owner)),
            LocoRouteNode.CODEC.listOf().fieldOf("nodes")
                .forGetter(r -> List.copyOf(r))
        ).apply(instance, (name, owner, nodes) ->
            new LocoRoute(name.orElse(null), owner.orElse(null), new HashSet<>(nodes)))
    );

    public static final StreamCodec<FriendlyByteBuf, LocoRoute> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), r -> Optional.ofNullable(r.name),
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), r -> Optional.ofNullable(r.owner),
            LocoRouteNode.STREAM_CODEC.apply(ByteBufCodecs.list()), r -> List.copyOf(r),
            (name, owner, nodes) ->
                new LocoRoute(name.orElse(null), owner.orElse(null), new HashSet<>(nodes))
        );

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
