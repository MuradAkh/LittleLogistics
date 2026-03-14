package dev.murad.shipping.compatibility.create;

public class CreateCompatibility {
    public static final String MOD_ID = "create";

    public static boolean enabled() {
        // Create compatibility deferred for NeoForge 1.21.1 migration
        return false;
    }
}
