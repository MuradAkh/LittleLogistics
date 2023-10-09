package dev.murad.shipping.setup;

import com.google.common.collect.ImmutableList;
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
import dev.murad.shipping.util.MultiMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class ModItems {
    private static final MultiMap<ResourceKey<CreativeModeTab>, RegistryObject<? extends Item>> PRIVATE_TAB_REGISTRY = new MultiMap<>();

    /**
     *  Empty Icons
     */

    public static final ResourceLocation LOCO_ROUTE_ICON = new ResourceLocation(ShippingMod.MOD_ID, "item/empty_loco_route");
    public static final ResourceLocation TUG_ROUTE_ICON = new ResourceLocation(ShippingMod.MOD_ID, "item/empty_tug_route");
    public static final ResourceLocation EMPTY_ENERGY = new ResourceLocation(ShippingMod.MOD_ID, "item/empty_energy");


    /**
     * COMMON
     */
    public static final RegistryObject<Item> CONDUCTORS_WRENCH = register("conductors_wrench",
            () -> new WrenchItem(new Item.Properties().stacksTo(1)), ImmutableList.of(CreativeModeTabs.TOOLS_AND_UTILITIES));


    public static final RegistryObject<Item> SPRING = register("spring",
            () -> new SpringItem(new Item.Properties().stacksTo(64)), ImmutableList.of(CreativeModeTabs.TOOLS_AND_UTILITIES));

    public static final RegistryObject<Item> CREATIVE_CAPACITOR = register("creative_capacitor",
            () -> new CreativeCapacitor(new Item.Properties().stacksTo(1)), ImmutableList.of(CreativeModeTabs.TOOLS_AND_UTILITIES));

    /**
     * Vessels
     */

    public static final RegistryObject<Item> CHEST_BARGE = register("barge",
            () -> new VesselItem(
                    new Item.Properties(),
                    (level, x, y, z) -> new ChestBargeEntity(ModEntityTypes.CHEST_BARGE.get(), level, x, y, z)),
            ImmutableList.of(CreativeModeTabs.TOOLS_AND_UTILITIES));

    public static final RegistryObject<Item> BARREL_BARGE = register("barrel_barge",
            () -> new VesselItem(
                    new Item.Properties(),
                    (level, x, y, z) -> new ChestBargeEntity(ModEntityTypes.BARREL_BARGE.get(), level, x, y, z)),
            ImmutableList.of(CreativeModeTabs.TOOLS_AND_UTILITIES));

//    public static final RegistryObject<Item> CHUNK_LOADER_BARGE = register("chunk_loader_barge",
//            () -> new VesselItem(new Item.Properties(), ChunkLoaderBargeEntity::new), ImmutableList.of(CreativeModeTabs.TOOLS_AND_UTILITIES));

    public static final RegistryObject<Item> FISHING_BARGE = register("fishing_barge",
            () -> new VesselItem(new Item.Properties(), FishingBargeEntity::new), ImmutableList.of(CreativeModeTabs.TOOLS_AND_UTILITIES));

    public static final RegistryObject<Item> FLUID_BARGE = register("fluid_barge",
            () -> new VesselItem(new Item.Properties(), FluidTankBargeEntity::new), ImmutableList.of(CreativeModeTabs.TOOLS_AND_UTILITIES));

    public static final RegistryObject<Item> SEATER_BARGE = register("seater_barge",
            () -> new VesselItem(new Item.Properties(), SeaterBargeEntity::new), ImmutableList.of(CreativeModeTabs.TOOLS_AND_UTILITIES));

    public static final RegistryObject<Item> VACUUM_BARGE = register("vacuum_barge",
            () -> new VesselItem(new Item.Properties(), VacuumBargeEntity::new), ImmutableList.of(CreativeModeTabs.TOOLS_AND_UTILITIES));

    public static final RegistryObject<Item> STEAM_TUG = register("tug",
            () -> new VesselItem(new Item.Properties(), SteamTugEntity::new), ImmutableList.of(CreativeModeTabs.TOOLS_AND_UTILITIES));

    public static final RegistryObject<Item> ENERGY_TUG = register("energy_tug",
            () -> new VesselItem(new Item.Properties(), EnergyTugEntity::new), ImmutableList.of(CreativeModeTabs.TOOLS_AND_UTILITIES));

    /**
     * Trains
     */

    public static final RegistryObject<Item> TUG_ROUTE = register("tug_route",
            () -> new TugRouteItem(new Item.Properties().stacksTo(16)), ImmutableList.of(CreativeModeTabs.TOOLS_AND_UTILITIES));

    public static final RegistryObject<Item> CHEST_CAR = register("chest_car",
            () -> new TrainCarItem(ChestCarEntity::new, new Item.Properties().stacksTo(64)), ImmutableList.of(CreativeModeTabs.TOOLS_AND_UTILITIES));

    public static final RegistryObject<Item> FLUID_CAR = register("fluid_car",
            () -> new TrainCarItem(FluidTankCarEntity::new, new Item.Properties().stacksTo(64)), ImmutableList.of(CreativeModeTabs.TOOLS_AND_UTILITIES));

    public static final RegistryObject<Item> SEATER_CAR = register("seater_car",
            () -> new TrainCarItem(SeaterCarEntity::new, new Item.Properties().stacksTo(64)), ImmutableList.of(CreativeModeTabs.TOOLS_AND_UTILITIES));

    public static final RegistryObject<Item> STEAM_LOCOMOTIVE = register("steam_locomotive",
            () -> new TrainCarItem(SteamLocomotiveEntity::new, new Item.Properties().stacksTo(64)), ImmutableList.of(CreativeModeTabs.TOOLS_AND_UTILITIES));

    public static final RegistryObject<Item> ENERGY_LOCOMOTIVE = register("energy_locomotive",
            () -> new TrainCarItem(EnergyLocomotiveEntity::new, new Item.Properties().stacksTo(64)), ImmutableList.of(CreativeModeTabs.TOOLS_AND_UTILITIES));

    public static final RegistryObject<Item> RECEIVER_COMPONENT = register("receiver_component",
            () -> new Item(new Item.Properties().stacksTo(64)), ImmutableList.of(CreativeModeTabs.TOOLS_AND_UTILITIES));

    public static final RegistryObject<Item> TRANSMITTER_COMPONENT = register("transmitter_component",
            () -> new Item(new Item.Properties().stacksTo(64)), ImmutableList.of(CreativeModeTabs.TOOLS_AND_UTILITIES));

    public static final RegistryObject<Item> LOCO_ROUTE = register("locomotive_route",
            () -> new LocoRouteItem(new Item.Properties().stacksTo(16)), ImmutableList.of(CreativeModeTabs.TOOLS_AND_UTILITIES));


    public static void buildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        PRIVATE_TAB_REGISTRY.getOrDefault(event.getTabKey(), new ArrayList<>())
                .forEach(event::accept);
    }

    private static <T extends Item> RegistryObject<T> register(String name, Supplier<T> itemSupplier, List<ResourceKey<CreativeModeTab>> tabs) {
        var res = Registration.ITEMS.register(name, itemSupplier);

        for (var tab : tabs) {
            PRIVATE_TAB_REGISTRY.putInsert(tab, res);
        }

        return res;
    }

    public static void register() {}
}
