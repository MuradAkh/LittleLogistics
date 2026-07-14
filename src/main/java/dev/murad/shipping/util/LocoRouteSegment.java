package dev.murad.shipping.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Compiled directed traversal from one route node to the following route node. */
public class LocoRouteSegment {
    private static final Codec<Direction> DIRECTION_CODEC = Codec.INT.xmap(Direction::from3DDataValue, Direction::get3DDataValue);
    public static final Codec<LocoRouteSegment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        LocoRouteStep.CODEC.listOf().fieldOf("steps").forGetter(segment -> List.copyOf(segment.steps)),
        DIRECTION_CODEC.fieldOf("arrival").forGetter(LocoRouteSegment::getArrivalDirection)
    ).apply(instance, LocoRouteSegment::new));
    public static final StreamCodec<FriendlyByteBuf, LocoRouteSegment> STREAM_CODEC = StreamCodec.composite(
        LocoRouteStep.STREAM_CODEC.apply(ByteBufCodecs.list()), segment -> List.copyOf(segment.steps),
        ByteBufCodecs.VAR_INT, segment -> segment.arrivalDirection.get3DDataValue(),
        (steps, arrival) -> new LocoRouteSegment(steps, Direction.from3DDataValue(arrival))
    );

    private final List<LocoRouteStep> steps;
    private final Direction arrivalDirection;

    public LocoRouteSegment(List<LocoRouteStep> steps, Direction arrivalDirection) {
        this.steps = new ArrayList<>(steps);
        this.arrivalDirection = arrivalDirection;
    }

    public List<LocoRouteStep> getSteps() { return steps; }
    public Direction getArrivalDirection() { return arrivalDirection; }
    public Direction getStartIncomingDirection() {
        return steps.isEmpty() ? arrivalDirection : steps.getFirst().incomingDirection();
    }
    public LocoRouteSegment copy() { return new LocoRouteSegment(steps, arrivalDirection); }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (LocoRouteStep step : steps) list.add(step.toNBT());
        tag.put("steps", list);
        tag.putInt("arrival", arrivalDirection.get3DDataValue());
        return tag;
    }

    public static LocoRouteSegment fromNBT(CompoundTag tag) {
        List<LocoRouteStep> steps = new ArrayList<>();
        ListTag list = tag.getList("steps", 10);
        for (int index = 0; index < list.size(); index++) steps.add(LocoRouteStep.fromNBT(list.getCompound(index)));
        return new LocoRouteSegment(steps, Direction.from3DDataValue(tag.getInt("arrival")));
    }

    @Override public boolean equals(Object object) {
        return object instanceof LocoRouteSegment segment && Objects.equals(steps, segment.steps)
            && arrivalDirection == segment.arrivalDirection;
    }
    @Override public int hashCode() { return Objects.hash(steps, arrivalDirection); }
}
