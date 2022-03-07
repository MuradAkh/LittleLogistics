package dev.murad.shipping.setup;

import dev.murad.shipping.entity.custom.train.locomotive.EnergyLocomotiveEntity;
import dev.murad.shipping.entity.custom.train.locomotive.SteamLocomotiveEntity;
import dev.murad.shipping.entity.custom.train.wagon.ChestCarEntity;
import dev.murad.shipping.entity.custom.train.locomotive.AbstractLocomotiveEntity;
import dev.murad.shipping.item.*;
import dev.murad.shipping.item.creative.CreativeCapacitor;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final RegistryObject<Item> CHEST_BARGE = Registration.ITEMS.register("barge",
            () -> new ChestBargeItem(new Item.Properties().tab(CreativeModeTab.TAB_TRANSPORTATION)));

    public static final RegistryObject<Item> CHUNK_LOADER_BARGE = Registration.ITEMS.register("chunk_loader_barge",
            () -> new ChunkLoaderBargeItem(new Item.Properties().tab(CreativeModeTab.TAB_TRANSPORTATION)));

    public static final RegistryObject<Item> FISHING_BARGE = Registration.ITEMS.register("fishing_barge",
            () -> new FishingBargeItem(new Item.Properties().tab(CreativeModeTab.TAB_TRANSPORTATION)));

    public static final RegistryObject<Item> FLUID_BARGE = Registration.ITEMS.register("fluid_barge",
            () -> new FluidTankBargeItem(new Item.Properties().tab(CreativeModeTab.TAB_TRANSPORTATION)));

    public static final RegistryObject<Item> SEATER_BARGE = Registration.ITEMS.register("seater_barge",
            () -> new SeaterBargeItem(new Item.Properties().tab(CreativeModeTab.TAB_TRANSPORTATION)));

    public static final RegistryObject<Item> STEAM_TUG = Registration.ITEMS.register("tug",
            () -> new SteamTugItem(new Item.Properties().tab(CreativeModeTab.TAB_TRANSPORTATION)));

    public static final RegistryObject<Item> ENERGY_TUG = Registration.ITEMS.register("energy_tug",
            () -> new EnergyTugItem(new Item.Properties().tab(CreativeModeTab.TAB_TRANSPORTATION)));

    public static final RegistryObject<Item> SPRING = Registration.ITEMS.register("spring",
            () -> new SpringItem(new Item.Properties().stacksTo(64).tab(CreativeModeTab.TAB_TRANSPORTATION)));

    public static final RegistryObject<Item> TUG_ROUTE = Registration.ITEMS.register("tug_route",
            () -> new TugRouteItem(new Item.Properties().stacksTo(16).tab(CreativeModeTab.TAB_TRANSPORTATION)));

    public static final RegistryObject<Item> CHEST_CAR = Registration.ITEMS.register("chest_car",
            () -> new TrainCarItem(ChestCarEntity::new, new Item.Properties().stacksTo(64).tab(CreativeModeTab.TAB_TRANSPORTATION)));

    public static final RegistryObject<Item> STEAM_LOCOMOTIVE = Registration.ITEMS.register("steam_locomotive",
            () -> new TrainCarItem(SteamLocomotiveEntity::new, new Item.Properties().stacksTo(64).tab(CreativeModeTab.TAB_TRANSPORTATION)));

    public static final RegistryObject<Item> ENERGY_LOCOMOTIVE = Registration.ITEMS.register("energy_locomotive",
            () -> new TrainCarItem(EnergyLocomotiveEntity::new, new Item.Properties().stacksTo(64).tab(CreativeModeTab.TAB_TRANSPORTATION)));

    public static final RegistryObject<Item> CREATIVE_CAPACITOR = Registration.ITEMS.register("creative_capacitor",
            () -> new CreativeCapacitor(new Item.Properties().stacksTo(1).tab(CreativeModeTab.TAB_TRANSPORTATION)));

    public static void register () {

    }

}
