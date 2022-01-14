package dev.murad.shipping.setup;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.VesselEntity;
import dev.murad.shipping.entity.custom.tug.SteamTugEntity;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ShippingMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEventBusEvents {
    @SubscribeEvent
    public static void addEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntityTypes.STEAM_TUG.get(), VesselEntity.setCustomAttributes().build());
        event.put(ModEntityTypes.FISHING_BARGE.get(), VesselEntity.setCustomAttributes().build());
        event.put(ModEntityTypes.CHUNK_LOADER_BARGE.get(), VesselEntity.setCustomAttributes().build());
        event.put(ModEntityTypes.CHEST_BARGE.get(), VesselEntity.setCustomAttributes().build());
    }

}