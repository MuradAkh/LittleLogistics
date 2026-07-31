package ca.edtoaster.littlecontraptions.setup;

import ca.edtoaster.littlecontraptions.LCMod;
import ca.edtoaster.littlecontraptions.ponder.AssemblerScenes;
import ca.edtoaster.littlecontraptions.ponder.LocomotiveScenes;
import ca.edtoaster.littlecontraptions.ponder.TugScenes;
import com.simibubi.create.infrastructure.ponder.AllCreatePonderTags;
import dev.murad.shipping.setup.ModBlocks;
import dev.murad.shipping.setup.ModItems;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/**
 * Registers Little Contraptions' client-only Ponder content against the standalone Ponder API
 * (Create 6.0). Replaces the old Create 0.5.1 {@code PonderRegistry}/{@code PonderRegistrationHelper}
 * + Registrate flow.
 */
public class LCPonder implements PonderPlugin {

    public static final ResourceLocation LC_TUGS = ResourceLocation.fromNamespaceAndPath(LCMod.MOD_ID, "tugs");
    public static final ResourceLocation LC_LOCOS = ResourceLocation.fromNamespaceAndPath(LCMod.MOD_ID, "trains");

    private static boolean registered;

    public static void register() {
        if (!registered) {
            PonderIndex.addPlugin(new LCPonder());
            registered = true;
        }
    }

    @Override
    public String getModId() {
        return LCMod.MOD_ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        PonderSceneRegistrationHelper<Item> itemHelper = helper.withKeyFunction(BuiltInRegistries.ITEM::getKey);

        itemHelper.forComponents(
                        LCItems.BARGE_ASSEMBLER.get(),
                        LCItems.CONTRAPTION_BARGE_ITEM.get())
                .addStoryBoard("basic_assembler", AssemblerScenes::basicAssemblerScene);

        itemHelper.forComponents(
                        ModItems.STEAM_TUG.get(),
                        ModItems.ENERGY_TUG.get(),
                        ModItems.TUG_ROUTE.get())
                .addStoryBoard("basic_tug", TugScenes::basicTugScene);

        itemHelper.forComponents(
                        ModBlocks.DOCKING_STATION.get().asItem(),
                        ModItems.STEAM_TUG.get(),
                        ModItems.ENERGY_TUG.get())
                .addStoryBoard("tug_dock", TugScenes::dockingScene);

        itemHelper.forComponents(
                        ModBlocks.DOCKING_STATION.get().asItem(),
                        ModItems.STEAM_LOCOMOTIVE.get(),
                        ModItems.ENERGY_LOCOMOTIVE.get())
                .addStoryBoard("loco_dock", LocomotiveScenes::dockingScene);

        itemHelper.forComponents(
                        ModItems.STEAM_LOCOMOTIVE.get(),
                        ModItems.ENERGY_LOCOMOTIVE.get(),
                        ModItems.LOCO_ROUTE.get(),
                        ModBlocks.AUTOMATIC_SWITCH_RAIL.get().asItem(),
                        ModBlocks.AUTOMATIC_TEE_JUNCTION_RAIL.get().asItem())
                .addStoryBoard("loco_route", LocomotiveScenes::routeScene);
    }

    @Override
    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        PonderTagRegistrationHelper<Item> itemHelper = helper.withKeyFunction(BuiltInRegistries.ITEM::getKey);

        helper.registerTag(LC_TUGS)
                .addToIndex()
                .item(ModItems.STEAM_TUG.get(), true, false)
                .title("Little Logistics Tugs")
                .description("Water trains with pathfinding!")
                .register();

        helper.registerTag(LC_LOCOS)
                .addToIndex()
                .item(ModItems.STEAM_LOCOMOTIVE.get(), true, false)
                .title("Little Logistics Trains")
                .description("Small but smart locomotives!")
                .register();

        itemHelper.addToTag(AllCreatePonderTags.MOVEMENT_ANCHOR)
                .add(LCItems.BARGE_ASSEMBLER.get());

        itemHelper.addToTag(LC_LOCOS)
                .add(ModBlocks.DOCKING_STATION.get().asItem())
                .add(ModItems.LOCO_ROUTE.get())
                .add(ModItems.STEAM_LOCOMOTIVE.get())
                .add(ModItems.ENERGY_LOCOMOTIVE.get())
                .add(ModBlocks.AUTOMATIC_TEE_JUNCTION_RAIL.get().asItem())
                .add(ModBlocks.AUTOMATIC_SWITCH_RAIL.get().asItem());

        itemHelper.addToTag(LC_TUGS)
                .add(ModItems.STEAM_TUG.get())
                .add(ModItems.ENERGY_TUG.get())
                .add(LCItems.BARGE_ASSEMBLER.get())
                .add(LCItems.CONTRAPTION_BARGE_ITEM.get())
                .add(ModBlocks.DOCKING_STATION.get().asItem())
                .add(ModItems.TUG_ROUTE.get());
    }
}
