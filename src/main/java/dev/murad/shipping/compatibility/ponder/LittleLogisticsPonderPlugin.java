package dev.murad.shipping.compatibility.ponder;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.setup.ModBlocks;
import dev.murad.shipping.setup.ModItems;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/**
 * Registers Little Logistics' client-only Ponder content when Ponder is available.
 */
public final class LittleLogisticsPonderPlugin implements PonderPlugin {

    private static boolean registered;

    public static void register() {
        if (!registered) {
            PonderIndex.addPlugin(new LittleLogisticsPonderPlugin());
            registered = true;
        }
    }

    @Override
    public String getModId() {
        return ShippingMod.MOD_ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        PonderSceneRegistrationHelper<Item> itemHelper = helper.withKeyFunction(BuiltInRegistries.ITEM::getKey);

        itemHelper.forComponents(
                        ModItems.STEAM_LOCOMOTIVE.get(),
                        ModItems.ENERGY_LOCOMOTIVE.get(),
                        ModItems.CHEST_CAR.get(),
                        ModItems.BARREL_CAR.get(),
                        ModItems.FLUID_CAR.get(),
                        ModItems.SEATER_CAR.get())
                .addStoryBoard("train_linking", TrainLinkingScene::linking);

        itemHelper.forComponents(
                        ModItems.STEAM_LOCOMOTIVE.get(),
                        ModItems.ENERGY_LOCOMOTIVE.get(),
                        ModItems.LOCO_ROUTE.get())
                .addStoryBoard("train_routing", TrainRoutingScene::routing);

        itemHelper.forComponents(
                        ModBlocks.DOCKING_STATION.get().asItem(),
                        ModItems.STEAM_LOCOMOTIVE.get(),
                        ModItems.ENERGY_LOCOMOTIVE.get(),
                        ModItems.CHEST_CAR.get(),
                        ModItems.BARREL_CAR.get(),
                        ModItems.FLUID_CAR.get())
                .addStoryBoard("train_docking", TrainDockingScene::docking);
    }
}
