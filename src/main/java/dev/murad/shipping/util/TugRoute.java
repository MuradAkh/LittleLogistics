package dev.murad.shipping.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class TugRoute extends ArrayList<TugRouteNode> {

    public static final Codec<TugRoute> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.optionalFieldOf("name")
                .forGetter(route -> Optional.ofNullable(route.name)),
            Codec.STRING.optionalFieldOf("dimension")
                .forGetter(route -> Optional.ofNullable(route.dimension)),
            TugRouteNode.CODEC.listOf().fieldOf("nodes")
                .forGetter(route -> List.copyOf(route)),
            TugRouteSegment.CODEC.listOf().optionalFieldOf("segments", List.of())
                .forGetter(route -> List.copyOf(route.segments))
        ).apply(instance, (name, dimension, nodes, segments) ->
            new TugRoute(name.orElse(null), dimension.orElse(null), nodes, segments))
    );

    public static final StreamCodec<FriendlyByteBuf, TugRoute> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), route -> Optional.ofNullable(route.name),
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), route -> Optional.ofNullable(route.dimension),
            TugRouteNode.STREAM_CODEC.apply(ByteBufCodecs.list()), route -> List.copyOf(route),
            TugRouteSegment.STREAM_CODEC.apply(ByteBufCodecs.list()), route -> List.copyOf(route.segments),
            (name, dimension, nodes, segments) ->
                new TugRoute(name.orElse(null), dimension.orElse(null), nodes, segments)
        );

    private static final String NAME_TAG = "name";
    private static final String DIMENSION_TAG = "dimension";
    private static final String NODES_TAG = "nodes";
    private static final String SEGMENTS_TAG = "segments";

    @Nullable
    private String name;
    @Nullable
    private String dimension;
    private final List<TugRouteSegment> segments;

    public TugRoute(@Nullable String name, @Nullable String dimension, List<TugRouteNode> nodes, List<TugRouteSegment> segments) {
        super(nodes);
        this.name = name;
        this.dimension = dimension;
        this.segments = new ArrayList<>(segments);
    }

    public TugRoute(@Nullable String name, List<TugRouteNode> nodes, List<TugRouteSegment> segments) {
        this(name, null, nodes, segments);
    }

    public TugRoute(@Nullable String name, List<TugRouteNode> nodes) {
        this(name, null, nodes, List.of());
    }

    public TugRoute() {
        this(null, null, List.of(), List.of());
    }

    @Nullable
    public String getName() {
        return name;
    }

    @Nullable
    public String getDimension() {
        return dimension;
    }

    public void setDimension(@Nullable String dimension) {
        this.dimension = dimension;
    }

    public List<TugRouteSegment> getSegments() {
        return segments;
    }

    public void setSegments(List<TugRouteSegment> compiledSegments) {
        this.segments.clear();
        this.segments.addAll(compiledSegments);
    }

    public boolean hasCustomName() {
        return this.name != null;
    }

    public TugRoute copy() {
        List<TugRouteSegment> segmentCopies = this.segments.stream()
            .map(TugRouteSegment::copy)
            .toList();
        return new TugRoute(this.name, this.dimension, new ArrayList<>(this), segmentCopies);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TugRoute tugRoute = (TugRoute) o;
        return Objects.equals(name, tugRoute.name)
            && Objects.equals(dimension, tugRoute.dimension)
            && Objects.equals(segments, tugRoute.segments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, dimension, segments);
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();

        ListTag nodesTag = new ListTag();
        for (TugRouteNode node : this) {
            nodesTag.add(node.toNBT());
        }
        tag.put(NODES_TAG, nodesTag);

        ListTag segmentsTag = new ListTag();
        for (TugRouteSegment segment : this.segments) {
            segmentsTag.add(segment.toNBT());
        }
        tag.put(SEGMENTS_TAG, segmentsTag);

        if (hasCustomName()) {
            tag.putString(NAME_TAG, this.name);
        }
        if (this.dimension != null) {
            tag.putString(DIMENSION_TAG, this.dimension);
        }
        return tag;
    }

    public static TugRoute fromNBT(CompoundTag tag) {
        String name = tag.contains(NAME_TAG) ? tag.getString(NAME_TAG) : null;
        String dimension = tag.contains(DIMENSION_TAG) ? tag.getString(DIMENSION_TAG) : null;

        List<TugRouteNode> nodes = new ArrayList<>();
        ListTag nodesTag = tag.getList(NODES_TAG, 10);
        for (int i = 0; i < nodesTag.size(); i++) {
            nodes.add(TugRouteNode.fromNBT(nodesTag.getCompound(i)));
        }

        List<TugRouteSegment> segments = new ArrayList<>();
        if (tag.contains(SEGMENTS_TAG, 9)) {
            ListTag segmentsTag = tag.getList(SEGMENTS_TAG, 10);
            for (int i = 0; i < segmentsTag.size(); i++) {
                segments.add(TugRouteSegment.fromNBT(segmentsTag.getCompound(i)));
            }
        }

        return new TugRoute(name, dimension, nodes, segments);
    }
}
