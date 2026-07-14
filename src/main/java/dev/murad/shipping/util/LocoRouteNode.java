package dev.murad.shipping.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

/**
 * TODO: Model using schemas to easier serialize/deserialize
 */
public class LocoRouteNode {
    private static final String NAME_TAG = "name";
    private static final String X_TAG = "x";
    private static final String Y_TAG = "y";
    private static final String Z_TAG = "z";
    private static final String COORDS_TAG = "coordinates";

    public static final Codec<LocoRouteNode> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.optionalFieldOf("name")
                .forGetter(n -> Optional.ofNullable(n.getName())),
            Codec.INT.fieldOf("x")
                .forGetter(LocoRouteNode::getX),
            Codec.INT.fieldOf("y")
                .forGetter(LocoRouteNode::getY),
            Codec.INT.fieldOf("z")
                .forGetter(LocoRouteNode::getZ)
        ).apply(instance, (name, x, y, z) -> new LocoRouteNode(name.orElse(null), x, y, z))
    );

    public static final StreamCodec<FriendlyByteBuf, LocoRouteNode> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), n -> Optional.ofNullable(n.getName()),
            ByteBufCodecs.INT, LocoRouteNode::getX,
            ByteBufCodecs.INT, LocoRouteNode::getY,
            ByteBufCodecs.INT, LocoRouteNode::getZ,
            (name, x, y, z) -> new LocoRouteNode(name.orElse(null), x, y, z)
        );

    @Nullable
    private String name;
    private final int x, y, z;

    @Nullable
    public String getName() {
        return name;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public LocoRouteNode(@Nullable String name, int x, int y, int z) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    public boolean hasCustomName() {
        return this.name != null;
    }

    public String getDisplayName(int index) {
        return hasCustomName() ? (index + 1) + ". " + name : (index + 1) + ". Node";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LocoRouteNode that = (LocoRouteNode) o;
        return Double.compare(that.x, x) == 0 && Double.compare(that.y, y) == 0 && Double.compare(that.z, z) == 0 && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, x, y, z);
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        if (this.hasCustomName()) {
            //noinspection ConstantConditions
            tag.putString(NAME_TAG, this.getName());
        }

        CompoundTag coords = new CompoundTag();
        coords.putDouble(X_TAG, x);
        coords.putDouble(Y_TAG, y);
        coords.putDouble(Z_TAG, z);

        tag.put(COORDS_TAG, coords);
        return tag;
    }

    public static LocoRouteNode fromNBT(CompoundTag tag) {
        String name = null;
        if (tag.contains(NAME_TAG)) {
            name = tag.getString(NAME_TAG);
        }

        CompoundTag coords = tag.getCompound(COORDS_TAG);
        // Backwards incompatible. Used to be double, but now it is int (BlockPos recently change to int only)
        int x = coords.getInt(X_TAG);
        int y = coords.getInt(Y_TAG);
        int z = coords.getInt(Z_TAG);

        return new LocoRouteNode(name, x, y, z);
    }

    public static LocoRouteNode fromBlocKPos(BlockPos pos) {
        return new LocoRouteNode(null, pos.getX(), pos.getY(), pos.getZ());
    }

    public static LocoRouteNode fromBlockPos(BlockPos pos) {
        return fromBlocKPos(pos);
    }

    public BlockPos toBlockPos() {
        return new BlockPos(x, y, z);
    }

    public boolean isAt(BlockPos pos) {
        return this.x == pos.getX() && this.y == pos.getY() && this.z == pos.getZ();
    }
}
