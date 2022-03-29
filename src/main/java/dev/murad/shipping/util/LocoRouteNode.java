package dev.murad.shipping.util;

import lombok.Getter;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Objects;

public class LocoRouteNode {
    private static final String NAME_TAG = "name";
    private static final String X_TAG = "x";
    private static final String Y_TAG = "y";
    private static final String Z_TAG = "z";
    private static final String COORDS_TAG = "coordinates";

    @Nullable
    @Getter
    private String name;
    @Getter
    private final double x, y, z;

    public LocoRouteNode(@Nullable String name, double x, double y, double z) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public LocoRouteNode(double x, double y, double z) {
        this(null, x, y, z);
    }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    public boolean hasCustomName() {
        return this.name != null;
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
        double x = coords.getDouble(X_TAG);
        double y = coords.getDouble(Y_TAG);
        double z = coords.getDouble(Z_TAG);

        return new LocoRouteNode(name, x, y, z);
    }

    public static LocoRouteNode fromBlocKPos(BlockPos pos) {
        return new LocoRouteNode(null, pos.getX(), pos.getY(), pos.getZ());
    }
}
