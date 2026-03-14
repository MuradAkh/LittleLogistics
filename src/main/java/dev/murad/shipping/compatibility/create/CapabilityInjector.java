package dev.murad.shipping.compatibility.create;

import dev.murad.shipping.entity.custom.train.wagon.SeaterCarEntity;

/**
 * Create mod compatibility - stubbed out for NeoForge 1.21.1 migration.
 * Create mod integration will be re-implemented once Create releases for NeoForge 1.21.1.
 */
public class CapabilityInjector {

    // Stubbed: TrainCarController and MinecartController integration removed
    // pending Create mod NeoForge 1.21.1 support

    public static Object constructMinecartControllerCapability(SeaterCarEntity entity) {
        return null;
    }

    public static <T> boolean isMinecartControllerCapability(Object cap) {
        return false;
    }
}
