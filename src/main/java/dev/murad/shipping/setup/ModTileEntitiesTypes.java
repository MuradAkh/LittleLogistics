package dev.murad.shipping.setup;

import dev.murad.shipping.block.dock.BargeDockTileEntity;
import dev.murad.shipping.block.dock.TugDockTileEntity;
import dev.murad.shipping.block.vessel_detector.VesselDetectorTileEntity;
import dev.murad.shipping.block.energy.VesselChargerTileEntity;
import dev.murad.shipping.block.fluid.FluidHopperTileEntity;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.fml.RegistryObject;

import java.util.function.Supplier;

public class ModTileEntitiesTypes {
    public static final RegistryObject<TileEntityType<TugDockTileEntity>> TUG_DOCK = register(
            "tug_dock",
            TugDockTileEntity::new,
            ModBlocks.TUG_DOCK
    );

    public static final RegistryObject<TileEntityType<BargeDockTileEntity>> BARGE_DOCK = register(
            "barge_dock",
            BargeDockTileEntity::new,
            ModBlocks.BARGE_DOCK
    );

    public static final RegistryObject<TileEntityType<VesselDetectorTileEntity>> VESSEL_DETECTOR = register(
            "vessel_detector",
            VesselDetectorTileEntity::new,
            ModBlocks.VESSEL_DETECTOR
    );

    public static final RegistryObject<TileEntityType<FluidHopperTileEntity>> FLUID_HOPPER = register(
            "fluid_hopper",
            FluidHopperTileEntity::new,
            ModBlocks.FLUID_HOPPER
    );

    public static final RegistryObject<TileEntityType<VesselChargerTileEntity>> VESSEL_CHARGER = register(
            "vessel_charger",
            VesselChargerTileEntity::new,
            ModBlocks.VESSEL_CHARGER
    );

    private static <T extends TileEntity> RegistryObject<TileEntityType<T>> register(String name, Supplier<T> factory, RegistryObject<? extends Block> block) {
        return Registration.TILE_ENTITIES.register(name, () -> TileEntityType.Builder.of(factory, block.get()).build(null));
    }

    public static void register () {

    }
}
