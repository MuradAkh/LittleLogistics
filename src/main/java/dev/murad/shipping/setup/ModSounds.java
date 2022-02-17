package dev.murad.shipping.setup;

import dev.murad.shipping.ShippingMod;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.RegistryObject;

public class ModSounds {

    public static final RegistryObject<SoundEvent> STEAM_TUG_WHISTLE = Registration.SOUND_EVENTS.register("steam_tug_whistle",
            () -> new SoundEvent(new ResourceLocation(ShippingMod.MOD_ID, "steam_tug_whistle")));

    public static final RegistryObject<SoundEvent> TUG_DOCKING = Registration.SOUND_EVENTS.register("tug_docking",
            () -> new SoundEvent(new ResourceLocation(ShippingMod.MOD_ID, "tug_docking")));

    public static final RegistryObject<SoundEvent> TUG_UNDOCKING = Registration.SOUND_EVENTS.register("tug_undocking",
            () -> new SoundEvent(new ResourceLocation(ShippingMod.MOD_ID, "tug_undocking")));

    public static void register () {}
}
