package dev.murad.shipping.setup;

import dev.murad.shipping.ShippingMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {

    public static final RegistryObject<SoundEvent> STEAM_TUG_WHISTLE = Registration.SOUND_EVENTS.register("steam_tug_whistle",
            () -> SoundEvent.createFixedRangeEvent(new ResourceLocation(ShippingMod.MOD_ID, "steam_tug_whistle"), 64f));

    public static final RegistryObject<SoundEvent> TUG_DOCKING = Registration.SOUND_EVENTS.register("tug_docking",
            () -> SoundEvent.createFixedRangeEvent(new ResourceLocation(ShippingMod.MOD_ID, "tug_docking"), 64f));

    public static final RegistryObject<SoundEvent> TUG_UNDOCKING = Registration.SOUND_EVENTS.register("tug_undocking",
            () -> SoundEvent.createFixedRangeEvent(new ResourceLocation(ShippingMod.MOD_ID, "tug_undocking"), 64f));

    public static void register () {}
}
