package dev.murad.shipping.compatibility.create;

import dev.murad.shipping.ShippingConfig;
import net.minecraftforge.fml.ModList;

public class CreateCompatibility {
    public static final String MOD_ID = "create";

    public static boolean enabled() {
        return (ShippingConfig.Common.CREATE_COMPAT.get() && ModList.get() != null && ModList.get().isLoaded(MOD_ID));
    }
}
