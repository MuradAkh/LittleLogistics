package dev.murad.shipping.setup;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.train.locomotive.EnergyLocomotiveEntity;
import dev.murad.shipping.entity.custom.train.locomotive.SteamLocomotiveEntity;
import dev.murad.shipping.entity.custom.train.wagon.ChestCarEntity;
import dev.murad.shipping.entity.custom.train.wagon.ChunkLoaderCarEntity;
import dev.murad.shipping.entity.custom.train.wagon.FluidTankCarEntity;
import dev.murad.shipping.entity.custom.train.wagon.SeaterCarEntity;
import dev.murad.shipping.entity.custom.vessel.barge.*;
import dev.murad.shipping.entity.custom.vessel.tug.EnergyTugEntity;
import dev.murad.shipping.entity.custom.vessel.tug.SteamTugEntity;
import dev.murad.shipping.item.*;
import dev.murad.shipping.item.creative.CreativeCapacitor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

import java.util.Map;

public class ModItems {

    /**
     *  Empty Icons
     */

    public static final ResourceLocation LOCO_ROUTE_ICON = new ResourceLocation(ShippingMod.MOD_ID, "item/empty_loco_route");
    public static final ResourceLocation TUG_ROUTE_ICON = new ResourceLocation(ShippingMod.MOD_ID, "item/empty_tug_route");
    public static final ResourceLocation EMPTY_ENERGY = new ResourceLocation(ShippingMod.MOD_ID, "item/empty_energy");


    /**
     * COMMON
     */
    public static final RegistryObject<Item> CONDUCTORS_WRENCH = Registration.ITEMS.register("conductors_wrench",
            () -> new WrenchItem(new Item.Properties().tab(CreativeModeTab.TAB_TRANSPORTATION).stacksTo(1)));


    public static final RegistryObject<Item> SPRING = Registration.ITEMS.register("spring",
            () -> new SpringItem(new Item.Properties().stacksTo(64).tab(CreativeModeTab.TAB_TRANSPORTATION)));

    public static final RegistryObject<Item> CREATIVE_CAPACITOR = Registration.ITEMS.register("creative_capacitor",
            () -> new CreativeCapacitor(new Item.Properties().stacksTo(1).tab(CreativeModeTab.TAB_TRANSPORTATION)));

    /**
     * Vessels
     */

    public static final RegistryObject<Item> CHEST_BARGE = Registration.ITEMS.register("barge",
            () -> new VesselItem(new Item.Properties().tab(CreativeModeTab.TAB_TRANSPORTATION), ChestBargeEntity::new));

    public static final RegistryObject<Item> CHUNK_LOADER_BARGE = Registration.ITEMS.register("chunk_loader_barge",
            () -> new VesselItem(new Item.Properties().tab(CreativeModeTab.TAB_TRANSPORTATION), ChunkLoaderBargeEntity::new));

    public static final RegistryObject<Item> FISHING_BARGE = Registration.ITEMS.register("fishing_barge",
            () -> new VesselItem(new Item.Properties().tab(CreativeModeTab.TAB_TRANSPORTATION), FishingBargeEntity::new));

    public static final RegistryObject<Item> FLUID_BARGE = Registration.ITEMS.register("fluid_barge",
            () -> new VesselItem(new Item.Properties().tab(CreativeModeTab.TAB_TRANSPORTATION), FluidTankBargeEntity::new));

    public static final RegistryObject<Item> SEATER_BARGE = Registration.ITEMS.register("seater_barge",
            () -> new VesselItem(new Item.Properties().tab(CreativeModeTab.TAB_TRANSPORTATION), SeaterBargeEntity::new));

    public static final RegistryObject<Item> STEAM_TUG = Registration.ITEMS.register("tug",
            () -> new VesselItem(new Item.Properties().tab(CreativeModeTab.TAB_TRANSPORTATION), SteamTugEntity::new));

    public static final RegistryObject<Item> ENERGY_TUG = Registration.ITEMS.register("energy_tug",
            () -> new VesselItem(new Item.Properties().tab(CreativeModeTab.TAB_TRANSPORTATION), EnergyTugEntity::new));

    /**
     * Trains
     */

    public static final RegistryObject<Item> TUG_ROUTE = Registration.ITEMS.register("tug_route",
            () -> new TugRouteItem(new Item.Properties().stacksTo(16).tab(CreativeModeTab.TAB_TRANSPORTATION)));

    public static final RegistryObject<Item> CHEST_CAR = Registration.ITEMS.register("chest_car",
            () -> new TrainCarItem(ChestCarEntity::new, new Item.Properties().stacksTo(64).tab(CreativeModeTab.TAB_TRANSPORTATION)));

    public static final RegistryObject<Item> FLUID_CAR = Registration.ITEMS.register("fluid_car",
            () -> new TrainCarItem(FluidTankCarEntity::new, new Item.Properties().stacksTo(64).tab(CreativeModeTab.TAB_TRANSPORTATION)));

    public static final RegistryObject<Item> CHUNK_LOADER_CAR = Registration.ITEMS.register("chunk_loader_car",
            () -> new TrainCarItem(ChunkLoaderCarEntity::new, new Item.Properties().stacksTo(64).tab(CreativeModeTab.TAB_TRANSPORTATION)));

    public static final RegistryObject<Item> SEATER_CAR = Registration.ITEMS.register("seater_car",
            () -> new TrainCarItem(SeaterCarEntity::new, new Item.Properties().stacksTo(64).tab(CreativeModeTab.TAB_TRANSPORTATION)));

    public static final RegistryObject<Item> STEAM_LOCOMOTIVE = Registration.ITEMS.register("steam_locomotive",
            () -> new TrainCarItem(SteamLocomotiveEntity::new, new Item.Properties().stacksTo(64).tab(CreativeModeTab.TAB_TRANSPORTATION)));

    public static final RegistryObject<Item> ENERGY_LOCOMOTIVE = Registration.ITEMS.register("energy_locomotive",
            () -> new TrainCarItem(EnergyLocomotiveEntity::new, new Item.Properties().stacksTo(64).tab(CreativeModeTab.TAB_TRANSPORTATION)));

    public static final RegistryObject<Item> RECEIVER_COMPONENT = Registration.ITEMS.register("receiver_component",
            () -> new Item(new Item.Properties().stacksTo(64).tab(CreativeModeTab.TAB_TRANSPORTATION)));

    public static final RegistryObject<Item> TRANSMITTER_COMPONENT = Registration.ITEMS.register("transmitter_component",
            () -> new Item(new Item.Properties().stacksTo(64).tab(CreativeModeTab.TAB_TRANSPORTATION)));

    public static final RegistryObject<Item> LOCO_ROUTE = Registration.ITEMS.register("locomotive_route",
            () -> new LocoRouteItem(new Item.Properties().stacksTo(16).tab(CreativeModeTab.TAB_TRANSPORTATION)));


    public static void register () {

    }

}
