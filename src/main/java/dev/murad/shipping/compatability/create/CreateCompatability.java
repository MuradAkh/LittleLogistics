package dev.murad.shipping.compatability.create;

import net.minecraftforge.fml.ModList;

public class CreateCompatability {
    public static final String MOD_ID = "create";

    public static boolean enabled() {
        return (ModList.get() != null && ModList.get().isLoaded(MOD_ID));
    }
}
