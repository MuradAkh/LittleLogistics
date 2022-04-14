package dev.murad.shipping.capability;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;

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
