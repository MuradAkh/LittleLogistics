package dev.murad.shipping.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

public class TugRouteNode {
    private static final String NAME_TAG = "name";
    private static final String X_TAG = "x";
    private static final String Y_TAG = "y";
    private static final String Z_TAG = "z";
    private static final String COORDS_TAG = "coordinates";

    public static final Codec<TugRouteNode> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.optionalFieldOf("name")
                .forGetter(node -> Optional.ofNullable(node.getName())),
            Codec.INT.fieldOf("x")
                .forGetter(TugRouteNode::getX),
            Codec.INT.fieldOf("y")
                .forGetter(TugRouteNode::getY),
            Codec.INT.fieldOf("z")
                .forGetter(TugRouteNode::getZ)
        ).apply(instance, (name, x, y, z) -> new TugRouteNode(name.orElse(null), x, y, z))
    );

    public static final StreamCodec<FriendlyByteBuf, TugRouteNode> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), node -> Optional.ofNullable(node.getName()),
            ByteBufCodecs.INT, TugRouteNode::getX,
            ByteBufCodecs.INT, TugRouteNode::getY,
            ByteBufCodecs.INT, TugRouteNode::getZ,
            (name, x, y, z) -> new TugRouteNode(name.orElse(null), x, y, z)
        );

    @Nullable
    private String name;
    private final int x;
    private final int y;
    private final int z;

    public TugRouteNode(@Nullable String name, int x, int y, int z) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public TugRouteNode(BlockPos pos) {
        this(null, pos.getX(), pos.getY(), pos.getZ());
    }

    public String getDisplayName(int index) {
        if (!this.hasCustomName()) {
            return I18n.get("item.littlelogistics.tug_route.node", index);
        }
        return I18n.get("item.littlelogistics.tug_route.node_named", index, getName());
    }

    public String getDisplayCoords() {
        return this.x + ", " + this.y + ", " + this.z;
    }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    @Nullable
    public String getName() {
        return this.name;
    }

    public boolean hasCustomName() {
        return this.name != null;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getZ() {
        return this.z;
    }

    public BlockPos toBlockPos() {
        return new BlockPos(x, y, z);
    }

    public boolean isAt(BlockPos pos) {
        return this.x == pos.getX() && this.y == pos.getY() && this.z == pos.getZ();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TugRouteNode that = (TugRouteNode) o;
        return x == that.x && y == that.y && z == that.z && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, x, y, z);
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        if (this.hasCustomName()) {
            tag.putString(NAME_TAG, this.getName());
        }

        CompoundTag coords = new CompoundTag();
        coords.putInt(X_TAG, x);
        coords.putInt(Y_TAG, y);
        coords.putInt(Z_TAG, z);

        tag.put(COORDS_TAG, coords);
        return tag;
    }

    public static TugRouteNode fromNBT(CompoundTag tag) {
        String name = null;
        if (tag.contains(NAME_TAG)) {
            name = tag.getString(NAME_TAG);
        }

        CompoundTag coords = tag.getCompound(COORDS_TAG);
        int x = coords.getInt(X_TAG);
        int y = coords.getInt(Y_TAG);
        int z = coords.getInt(Z_TAG);

        return new TugRouteNode(name, x, y, z);
    }

    public static TugRouteNode fromBlockPos(BlockPos pos) {
        return new TugRouteNode(pos);
    }
}
