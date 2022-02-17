package dev.murad.shipping.util;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.vector.Vector2f;

import javax.annotation.Nullable;
import java.util.Objects;

public class TugRouteNode {
    private static final String NAME_TAG = "name";
    private static final String X_TAG = "x";
    private static final String Z_TAG = "z";
    private static final String COORDS_TAG = "coordinates";

    private String name;
    private final double x, z;

    public TugRouteNode(String name, double x, double z) {
        this.name = name;
        this.x = x;
        this.z = z;
    }

    public TugRouteNode(double x, double y) {
        this(null, x, y);
    }

    public String getDisplayName(int index) {
        if (!this.hasCustomName()) {
            return I18n.get("item.littlelogistics.tug_route.node", index);
        } else {
            return I18n.get("item.littlelogistics.tug_route.node_named", index, getName());
        }
    }

    public String getDisplayCoords() {
        return this.x + ", " + this.z;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Nullable
    public String getName() {
        return this.name;
    }

    public boolean hasCustomName() {
        return this.name != null;
    }

    public double getX() {
        return this.x;
    }

    public double getZ() {
        return this.z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TugRouteNode that = (TugRouteNode) o;
        return Double.compare(that.x, x) == 0 && Double.compare(that.z, z) == 0 && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, x, z);
    }

    public CompoundNBT toNBT() {
        CompoundNBT tag = new CompoundNBT();
        if (this.hasCustomName()) {
            //noinspection ConstantConditions
            tag.putString(NAME_TAG, this.getName());
        }

        CompoundNBT coords = new CompoundNBT();
        coords.putDouble(X_TAG, x);
        coords.putDouble(Z_TAG, z);

        tag.put(COORDS_TAG, coords);
        return tag;
    }

    public static TugRouteNode fromNBT(CompoundNBT tag) {
        String name = null;
        if (tag.contains(NAME_TAG)) {
            name = tag.getString(NAME_TAG);
        }

        CompoundNBT coords = tag.getCompound(COORDS_TAG);
        double x = coords.getDouble(X_TAG);
        double z = coords.getDouble(Z_TAG);

        return new TugRouteNode(name, x, z);
    }

    public static TugRouteNode fromVector2f(Vector2f node) {
        double x = node.x, z = node.y;
        return new TugRouteNode(null, x, z);
    }
}
