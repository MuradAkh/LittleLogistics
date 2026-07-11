package dev.murad.shipping.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

public record TugRoutePoint(int x, int y, int z) {
    private static final String X_TAG = "x";
    private static final String Y_TAG = "y";
    private static final String Z_TAG = "z";

    public static final Codec<TugRoutePoint> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.INT.fieldOf("x").forGetter(TugRoutePoint::x),
            Codec.INT.fieldOf("y").forGetter(TugRoutePoint::y),
            Codec.INT.fieldOf("z").forGetter(TugRoutePoint::z)
        ).apply(instance, TugRoutePoint::new)
    );

    public static final StreamCodec<FriendlyByteBuf, TugRoutePoint> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.INT, TugRoutePoint::x,
            ByteBufCodecs.INT, TugRoutePoint::y,
            ByteBufCodecs.INT, TugRoutePoint::z,
            TugRoutePoint::new
        );

    public TugRoutePoint(BlockPos pos) {
        this(pos.getX(), pos.getY(), pos.getZ());
    }

    public BlockPos toBlockPos() {
        return new BlockPos(x, y, z);
    }

    public Vec3 toVec3Center() {
        return Vec3.atCenterOf(toBlockPos());
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(X_TAG, x);
        tag.putInt(Y_TAG, y);
        tag.putInt(Z_TAG, z);
        return tag;
    }

    public static TugRoutePoint fromNBT(CompoundTag tag) {
        return new TugRoutePoint(tag.getInt(X_TAG), tag.getInt(Y_TAG), tag.getInt(Z_TAG));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TugRoutePoint point && x == point.x && y == point.y && z == point.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }
}
