package dev.murad.shipping.setup;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.item.SpringCutterItem;
import dev.murad.shipping.item.SpringItem;
import dev.murad.shipping.util.EntitySpringAPI;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.util.ActionResultType;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ShippingMod.MOD_ID)
public class ModEventHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void entityInteract(PlayerInteractEvent.EntityInteract event) {
        handleEvent(event, event.getTarget());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void entitySpecificInteract(PlayerInteractEvent.EntityInteractSpecific event) {
        handleEvent(event, event.getTarget());
    }

    private static void handleEvent(PlayerInteractEvent event, Entity target) {
        if(!event.getItemStack().isEmpty()) {
            Item item = event.getItemStack().getItem();
            if(item instanceof SpringItem) {
                SpringItem springItem = (SpringItem) item;
                SpringItem.State state = springItem.getState(event.getItemStack());
                if(EntitySpringAPI.isValidTarget(target, state)) {
                    springItem.onUsedOnEntity(event.getItemStack(), event.getPlayer(), event.getWorld(), target);
                    event.setCanceled(true);
                    event.setCancellationResult(ActionResultType.SUCCESS);
                }
            } else if(item instanceof SpringCutterItem) {
                SpringCutterItem cutter = (SpringCutterItem) item;
                cutter.onUsedOnEntity(event.getItemStack(), event.getPlayer(), event.getWorld(), target);
                event.setCanceled(true);
                event.setCancellationResult(ActionResultType.SUCCESS);
            }
        }
    }
}
