package dev.murad.shipping.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** A single directed decision while traversing a compiled rail segment. */
public record LocoRouteStep(BlockPos railPos, Direction incomingDirection, Direction outgoingDirection) {
    private static final Codec<Direction> DIRECTION_CODEC = Codec.INT.xmap(Direction::from3DDataValue, Direction::get3DDataValue);

    public static final Codec<LocoRouteStep> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.fieldOf("x").forGetter(step -> step.railPos.getX()),
        Codec.INT.fieldOf("y").forGetter(step -> step.railPos.getY()),
        Codec.INT.fieldOf("z").forGetter(step -> step.railPos.getZ()),
        DIRECTION_CODEC.fieldOf("incoming").forGetter(LocoRouteStep::incomingDirection),
        DIRECTION_CODEC.fieldOf("outgoing").forGetter(LocoRouteStep::outgoingDirection)
    ).apply(instance, (x, y, z, incoming, outgoing) -> new LocoRouteStep(new BlockPos(x, y, z), incoming, outgoing)));

    public static final StreamCodec<FriendlyByteBuf, LocoRouteStep> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, LocoRouteStep::railPos,
        ByteBufCodecs.VAR_INT, step -> step.incomingDirection.get3DDataValue(),
        ByteBufCodecs.VAR_INT, step -> step.outgoingDirection.get3DDataValue(),
        (pos, incoming, outgoing) -> new LocoRouteStep(pos, Direction.from3DDataValue(incoming), Direction.from3DDataValue(outgoing))
    );

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("pos", railPos.asLong());
        tag.putInt("incoming", incomingDirection.get3DDataValue());
        tag.putInt("outgoing", outgoingDirection.get3DDataValue());
        return tag;
    }

    public static LocoRouteStep fromNBT(CompoundTag tag) {
        return new LocoRouteStep(BlockPos.of(tag.getLong("pos")),
            Direction.from3DDataValue(tag.getInt("incoming")), Direction.from3DDataValue(tag.getInt("outgoing")));
    }
}
