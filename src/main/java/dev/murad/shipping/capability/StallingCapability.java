package dev.murad.shipping.capability;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.EntityCapability;

public interface StallingCapability {
    EntityCapability<StallingCapability, Void> ENTITY_CAPABILITY =
        EntityCapability.createVoid(
            ResourceLocation.fromNamespaceAndPath("littlelogistics", "stalling"),
            StallingCapability.class);

    boolean isDocked();
    void dock(double x, double y, double z);
    void undock();

    boolean isStalled();
    void stall();
    void unstall();

    boolean isFrozen();
    void freeze();
    void unfreeze();
}
