package dev.murad.shipping.setup;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.item.SpringItem;
import net.minecraft.item.ItemModelsProperties;
import net.minecraft.util.ResourceLocation;

public class ModItemModelProperties {

    public static void register() {
        ItemModelsProperties.register(ModItems.SPRING.get(),
                new ResourceLocation(ShippingMod.MOD_ID, "springstate"), (stack, world, entity) -> {
                    return SpringItem.getState(stack).equals(SpringItem.State.READY) ? 0 : 1;
                });
    }
}
