package dev.murad.shipping.setup;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.item.LocoRouteItem;
import dev.murad.shipping.item.SpringItem;
import dev.murad.shipping.item.TugRouteItem;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;

public class ModItemModelProperties {

    public static void register() {
        ItemProperties.register(ModItems.SPRING.get(),
                new ResourceLocation(ShippingMod.MOD_ID, "springstate"), (stack, world, entity, i) -> {
                    return SpringItem.getState(stack).equals(SpringItem.State.READY) ? 0 : 1;
                });

        ItemProperties.register(ModItems.TUG_ROUTE.get(),
                new ResourceLocation(ShippingMod.MOD_ID, "routestate"), (stack, world, entity, i) -> {
                    return !TugRouteItem.getRoute(stack).isEmpty() ? 0 : 1;
                });

        ItemProperties.register(ModItems.LOCO_ROUTE.get(),
                new ResourceLocation(ShippingMod.MOD_ID, "locoroutestate"), (stack, world, entity, i) -> {
                    return !LocoRouteItem.getRoute(stack).isEmpty() ? 0 : 1;
                });
    }
}
