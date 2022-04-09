package dev.murad.shipping.event;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.SpringEntity;
import dev.murad.shipping.item.SpringItem;
import dev.murad.shipping.util.EntitySpringAPI;
import dev.murad.shipping.util.LinkableEntity;
import dev.murad.shipping.util.SpringableEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ShearsItem;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge-wide event bus
 */
@Mod.EventBusSubscriber(modid = ShippingMod.MOD_ID)
public class ForgeEventHandler {

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
            if(item instanceof SpringItem springItem) {
                if(EntitySpringAPI.isValidTarget(target)) {
                    springItem.onUsedOnEntity(event.getItemStack(), event.getPlayer(), event.getWorld(), target);
                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.SUCCESS);
                }
            }

            if(item instanceof ShearsItem) {
                if(target instanceof LinkableEntity v) {
                    v.handleShearsCut();
                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.SUCCESS);
                }
            }
        }
    }
}
