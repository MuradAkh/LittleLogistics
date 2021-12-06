package dev.murad.shipping.setup;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.TugEntity;
import dev.murad.shipping.item.SpringCutterItem;
import dev.murad.shipping.item.SpringItem;
import dev.murad.shipping.util.EntitySpringAPI;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.util.ActionResultType;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ShippingMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEventBusEvents {
    @SubscribeEvent
    public static void addEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntityTypes.TUG.get(), TugEntity.setCustomAttributes().build());
    }

}