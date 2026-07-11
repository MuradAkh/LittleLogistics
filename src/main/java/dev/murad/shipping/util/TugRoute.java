package dev.murad.shipping.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class TugRoute extends ArrayList<TugRouteNode> {

    public enum State implements StringRepresentable {
        BLANK("blank", 0),
        IN_PROGRESS("in_progress", 1),
        COMPLETE("complete", 2);

        public static final Codec<State> CODEC = StringRepresentable.fromEnum(State::values);

        private final String name;
        private final int id;

        State(String name, int id) {
            this.name = name;
            this.id = id;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        public int getId() {
            return id;
        }

        public static State fromId(int id) {
            for (State state : values()) {
                if (state.id == id) return state;
            }
            return BLANK;
        }

        public static State fromName(String name, State fallback) {
            for (State state : values()) {
                if (state.name.equals(name)) return state;
            }
            return fallback;
        }
    }

    public static final Codec<TugRoute> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.optionalFieldOf("name")
                .forGetter(route -> Optional.ofNullable(route.name)),
            Codec.STRING.optionalFieldOf("dimension")
                .forGetter(route -> Optional.ofNullable(route.dimension)),
            TugRouteNode.CODEC.listOf().fieldOf("nodes")
                .forGetter(route -> List.copyOf(route)),
            TugRouteSegment.CODEC.listOf().optionalFieldOf("segments", List.of())
                .forGetter(route -> List.copyOf(route.segments)),
            State.CODEC.optionalFieldOf("state")
                .forGetter(route -> Optional.of(route.state)),
            Codec.INT.optionalFieldOf("next_insertion_index")
                .forGetter(route -> Optional.of(route.nextInsertionIndex))
        ).apply(instance, (name, dimension, nodes, segments, state, nextInsertionIndex) ->
            new TugRoute(name.orElse(null), dimension.orElse(null), nodes, segments,
                state.orElse(nodes.isEmpty() ? State.BLANK : State.COMPLETE), nextInsertionIndex.orElse(-1)))
    );

    public static final StreamCodec<FriendlyByteBuf, TugRoute> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), route -> Optional.ofNullable(route.name),
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), route -> Optional.ofNullable(route.dimension),
            TugRouteNode.STREAM_CODEC.apply(ByteBufCodecs.list()), route -> List.copyOf(route),
            TugRouteSegment.STREAM_CODEC.apply(ByteBufCodecs.list()), route -> List.copyOf(route.segments),
            ByteBufCodecs.VAR_INT, route -> route.state.getId(),
            ByteBufCodecs.VAR_INT, route -> route.nextInsertionIndex,
            (name, dimension, nodes, segments, state, nextInsertionIndex) ->
                new TugRoute(name.orElse(null), dimension.orElse(null), nodes, segments, State.fromId(state), nextInsertionIndex)
        );

    private static final String NAME_TAG = "name";
    private static final String DIMENSION_TAG = "dimension";
    private static final String NODES_TAG = "nodes";
    private static final String SEGMENTS_TAG = "segments";
    private static final String STATE_TAG = "state";
    private static final String NEXT_INSERTION_INDEX_TAG = "next_insertion_index";

    @Nullable
    private String name;
    @Nullable
    private String dimension;
    private final List<TugRouteSegment> segments;
    private State state;
    private int nextInsertionIndex;

    public TugRoute(@Nullable String name, @Nullable String dimension, List<TugRouteNode> nodes, List<TugRouteSegment> segments) {
        this(name, dimension, nodes, segments, nodes.isEmpty() ? State.BLANK : State.COMPLETE, -1);
    }

    public TugRoute(@Nullable String name, @Nullable String dimension, List<TugRouteNode> nodes, List<TugRouteSegment> segments,
                    State state, int nextInsertionIndex) {
        super(nodes);
        this.name = name;
        this.dimension = dimension;
        this.segments = new ArrayList<>(segments);
        this.state = nodes.isEmpty() ? State.BLANK : state;
        this.nextInsertionIndex = this.state == State.IN_PROGRESS ? Math.clamp(nextInsertionIndex, 0, nodes.size()) : -1;
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

    public State getState() {
        return state;
    }

    public boolean isComplete() {
        return state == State.COMPLETE;
    }

    public boolean isInProgress() {
        return state == State.IN_PROGRESS;
    }

    public int getNextInsertionIndex() {
        return nextInsertionIndex;
    }

    /**
     * Insertion mode preserves the previously completed segment list until the selected gap is filled.
     */
    public boolean isInserting() {
        return isInProgress() && (nextInsertionIndex < size() || segments.size() == size());
    }

    public void beginAppending() {
        state = State.IN_PROGRESS;
        nextInsertionIndex = size();
    }

    public void beginInsertion(int insertionIndex) {
        state = State.IN_PROGRESS;
        nextInsertionIndex = Math.clamp(insertionIndex, 0, size());
    }

    public void markComplete() {
        state = isEmpty() ? State.BLANK : State.COMPLETE;
        nextInsertionIndex = -1;
    }

    public void normalizeState() {
        if (isEmpty()) {
            state = State.BLANK;
            nextInsertionIndex = -1;
        } else if (state == State.IN_PROGRESS) {
            nextInsertionIndex = Math.clamp(nextInsertionIndex, 0, size());
        } else {
            nextInsertionIndex = -1;
        }
    }

    public boolean hasCustomName() {
        return this.name != null;
    }

    public TugRoute copy() {
        List<TugRouteSegment> segmentCopies = this.segments.stream()
            .map(TugRouteSegment::copy)
            .toList();
        return new TugRoute(this.name, this.dimension, new ArrayList<>(this), segmentCopies, this.state, this.nextInsertionIndex);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TugRoute tugRoute = (TugRoute) o;
        return Objects.equals(name, tugRoute.name)
            && Objects.equals(dimension, tugRoute.dimension)
            && Objects.equals(segments, tugRoute.segments)
            && state == tugRoute.state
            && nextInsertionIndex == tugRoute.nextInsertionIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, dimension, segments, state, nextInsertionIndex);
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
        tag.putString(STATE_TAG, this.state.getSerializedName());
        tag.putInt(NEXT_INSERTION_INDEX_TAG, this.nextInsertionIndex);
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

        State fallback = nodes.isEmpty() ? State.BLANK : State.COMPLETE;
        State state = tag.contains(STATE_TAG) ? State.fromName(tag.getString(STATE_TAG), fallback) : fallback;
        int nextInsertionIndex = tag.contains(NEXT_INSERTION_INDEX_TAG) ? tag.getInt(NEXT_INSERTION_INDEX_TAG) : -1;
        return new TugRoute(name, dimension, nodes, segments, state, nextInsertionIndex);
    }
}
