package dev.murad.shipping.capability;

import dev.murad.shipping.block.dock.AbstractDockTileEntity;
import dev.murad.shipping.util.LinkableEntity;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;

import javax.annotation.Nullable;

public interface StallingCapability {
    Capability<StallingCapability> STALLING_CAPABILITY = CapabilityManager.get(new CapabilityToken<StallingCapability>(){});
    static void register(RegisterCapabilitiesEvent event) {
        event.register(StallingCapability.class);
    }

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
