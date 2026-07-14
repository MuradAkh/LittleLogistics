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

/**
 * An ordered, cyclic locomotive route.
 *
 * <p>A completed route has one compiled segment for every node: segment {@code i}
 * leads from node {@code i} to node {@code (i + 1) % size}.  An in-progress route
 * has one segment for each already-defined pair of consecutive nodes.  Keeping the
 * compiled rail traversal with the item is deliberate: a train must not discover a
 * different path merely because a distant part of its loop is currently unloaded.</p>
 */
public class LocoRoute extends ArrayList<LocoRouteNode> {
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

        public static State fromName(String name) {
            for (State state : values()) {
                if (state.name.equals(name)) return state;
            }
            return BLANK;
        }
    }

    public static final Codec<LocoRoute> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.optionalFieldOf("name").forGetter(route -> Optional.ofNullable(route.name)),
            Codec.STRING.optionalFieldOf("dimension").forGetter(route -> Optional.ofNullable(route.dimension)),
            LocoRouteNode.CODEC.listOf().fieldOf("nodes").forGetter(route -> List.copyOf(route)),
            LocoRouteSegment.CODEC.listOf().optionalFieldOf("segments", List.of())
                .forGetter(route -> List.copyOf(route.segments)),
            State.CODEC.optionalFieldOf("state").forGetter(route -> Optional.of(route.state)),
            Codec.INT.optionalFieldOf("next_insertion_index")
                .forGetter(route -> Optional.of(route.nextInsertionIndex))
        ).apply(instance, (name, dimension, nodes, segments, state, insertionIndex) ->
            new LocoRoute(name.orElse(null), dimension.orElse(null), nodes, segments,
                state.orElse(nodes.isEmpty() ? State.BLANK : State.IN_PROGRESS), insertionIndex.orElse(-1)))
    );

    public static final StreamCodec<FriendlyByteBuf, LocoRoute> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), route -> Optional.ofNullable(route.name),
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), route -> Optional.ofNullable(route.dimension),
            LocoRouteNode.STREAM_CODEC.apply(ByteBufCodecs.list()), route -> List.copyOf(route),
            LocoRouteSegment.STREAM_CODEC.apply(ByteBufCodecs.list()), route -> List.copyOf(route.segments),
            ByteBufCodecs.VAR_INT, route -> route.state.getId(),
            ByteBufCodecs.VAR_INT, route -> route.nextInsertionIndex,
            (name, dimension, nodes, segments, state, insertionIndex) ->
                new LocoRoute(name.orElse(null), dimension.orElse(null), nodes, segments,
                    State.fromId(state), insertionIndex)
        );

    private static final String NAME_TAG = "name";
    private static final String DIMENSION_TAG = "dimension";
    private static final String NODES_TAG = "nodes";
    private static final String SEGMENTS_TAG = "segments";
    private static final String STATE_TAG = "state";
    private static final String NEXT_INSERTION_INDEX_TAG = "next_insertion_index";

    @Nullable private String name;
    @Nullable private String dimension;
    private final List<LocoRouteSegment> segments;
    private State state;
    private int nextInsertionIndex;

    public LocoRoute(@Nullable String name, @Nullable String dimension, List<LocoRouteNode> nodes,
                     List<LocoRouteSegment> segments, State state, int nextInsertionIndex) {
        super(nodes);
        this.name = name;
        this.dimension = dimension;
        this.segments = new ArrayList<>(segments);
        this.state = nodes.isEmpty() ? State.BLANK : state;
        this.nextInsertionIndex = this.state == State.IN_PROGRESS
            ? Math.clamp(nextInsertionIndex, 0, nodes.size()) : -1;
    }

    public LocoRoute() {
        this(null, null, List.of(), List.of(), State.BLANK, -1);
    }

    @Nullable public String getName() { return name; }
    @Nullable public String getDimension() { return dimension; }
    public void setDimension(@Nullable String dimension) { this.dimension = dimension; }
    public List<LocoRouteSegment> getSegments() { return segments; }
    public void setSegments(List<LocoRouteSegment> compiledSegments) {
        segments.clear();
        segments.addAll(compiledSegments);
    }
    public State getState() { return state; }
    public boolean isComplete() { return state == State.COMPLETE; }
    public boolean isInProgress() { return state == State.IN_PROGRESS; }
    public int getNextInsertionIndex() { return nextInsertionIndex; }
    public boolean isInserting() {
        return isInProgress() && nextInsertionIndex >= 0
            && (nextInsertionIndex < size() || segments.size() == size());
    }

    public boolean isUsable() {
        return isComplete() && size() >= 2 && segments.size() == size();
    }

    public void beginAppending() {
        state = isEmpty() ? State.BLANK : State.IN_PROGRESS;
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

    public LocoRoute copy() {
        return new LocoRoute(name, dimension, new ArrayList<>(this),
            segments.stream().map(LocoRouteSegment::copy).toList(), state, nextInsertionIndex);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof LocoRoute route) || !super.equals(object)) return false;
        return Objects.equals(name, route.name) && Objects.equals(dimension, route.dimension)
            && Objects.equals(segments, route.segments) && state == route.state
            && nextInsertionIndex == route.nextInsertionIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, dimension, segments, state, nextInsertionIndex);
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        ListTag nodes = new ListTag();
        for (LocoRouteNode node : this) nodes.add(node.toNBT());
        tag.put(NODES_TAG, nodes);
        ListTag compiledSegments = new ListTag();
        for (LocoRouteSegment segment : segments) compiledSegments.add(segment.toNBT());
        tag.put(SEGMENTS_TAG, compiledSegments);
        if (name != null) tag.putString(NAME_TAG, name);
        if (dimension != null) tag.putString(DIMENSION_TAG, dimension);
        tag.putString(STATE_TAG, state.getSerializedName());
        tag.putInt(NEXT_INSERTION_INDEX_TAG, nextInsertionIndex);
        return tag;
    }

    public static LocoRoute fromNBT(CompoundTag tag) {
        List<LocoRouteNode> nodes = new ArrayList<>();
        ListTag nodesTag = tag.getList(NODES_TAG, 10);
        for (int index = 0; index < nodesTag.size(); index++) nodes.add(LocoRouteNode.fromNBT(nodesTag.getCompound(index)));
        List<LocoRouteSegment> segments = new ArrayList<>();
        ListTag segmentsTag = tag.getList(SEGMENTS_TAG, 10);
        for (int index = 0; index < segmentsTag.size(); index++) segments.add(LocoRouteSegment.fromNBT(segmentsTag.getCompound(index)));
        String name = tag.contains(NAME_TAG) ? tag.getString(NAME_TAG) : null;
        String dimension = tag.contains(DIMENSION_TAG) ? tag.getString(DIMENSION_TAG) : null;
        State state = tag.contains(STATE_TAG) ? State.fromName(tag.getString(STATE_TAG))
            : (nodes.isEmpty() ? State.BLANK : State.IN_PROGRESS);
        int insertion = tag.contains(NEXT_INSERTION_INDEX_TAG) ? tag.getInt(NEXT_INSERTION_INDEX_TAG) : -1;
        return new LocoRoute(name, dimension, nodes, segments, state, insertion);
    }
}
