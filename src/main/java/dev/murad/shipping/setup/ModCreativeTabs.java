package dev.murad.shipping.setup;

import dev.murad.shipping.ShippingMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ShippingMod.MOD_ID);

    @SuppressWarnings("unused")
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> LITTLE_LOGISTICS = CREATIVE_MODE_TABS.register(
            "little_logistics",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.littlelogistics.little_logistics"))
                    .icon(() -> new ItemStack(ModItems.STEAM_TUG.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.STEAM_TUG.get());
                        output.accept(ModItems.ENERGY_TUG.get());
                        output.accept(ModItems.CHEST_BARGE.get());
                        output.accept(ModItems.BARREL_BARGE.get());
                        output.accept(ModItems.FISHING_BARGE.get());
                        output.accept(ModItems.FLUID_BARGE.get());
                        output.accept(ModItems.SEATER_BARGE.get());
                        output.accept(ModItems.VACUUM_BARGE.get());
                        output.accept(ModItems.TUG_ROUTE.get());
                        output.accept(ModItems.STEAM_LOCOMOTIVE.get());
                        output.accept(ModItems.ENERGY_LOCOMOTIVE.get());
                        output.accept(ModItems.CHEST_CAR.get());
                        output.accept(ModItems.BARREL_CAR.get());
                        output.accept(ModItems.FLUID_CAR.get());
                        output.accept(ModItems.SEATER_CAR.get());
                        output.accept(ModItems.LOCO_ROUTE.get());
                        output.accept(ModItems.CONDUCTORS_WRENCH.get());
                        output.accept(ModItems.SPRING.get());
                        output.accept(ModItems.CREATIVE_CAPACITOR.get());
                        output.accept(ModItems.RECEIVER_COMPONENT.get());
                        output.accept(ModItems.TRANSMITTER_COMPONENT.get());
                        output.accept(ModBlocks.GUIDE_RAIL_TUG.get());
                        output.accept(ModBlocks.GUIDE_RAIL_CORNER.get());
                        output.accept(ModBlocks.VESSEL_DETECTOR.get());
                        output.accept(ModBlocks.DOCKING_STATION.get());
                        output.accept(ModBlocks.SWITCH_RAIL.get());
                        output.accept(ModBlocks.AUTOMATIC_SWITCH_RAIL.get());
                        output.accept(ModBlocks.TEE_JUNCTION_RAIL.get());
                        output.accept(ModBlocks.AUTOMATIC_TEE_JUNCTION_RAIL.get());
                        output.accept(ModBlocks.JUNCTION_RAIL.get());
                    })
                    .build()
    );

    public static void register() {}
}