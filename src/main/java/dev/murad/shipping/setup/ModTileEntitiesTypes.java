package dev.murad.shipping.setup;

import dev.murad.shipping.block.dock.BargeDockTileEntity;
import dev.murad.shipping.block.dock.DockBlockEntity;
import dev.murad.shipping.block.dock.TugDockTileEntity;
import dev.murad.shipping.block.energy.VesselChargerTileEntity;
import dev.murad.shipping.block.fluid.FluidHopperTileEntity;
import dev.murad.shipping.block.rail.blockentity.LocomotiveDockTileEntity;
import dev.murad.shipping.block.rail.blockentity.TrainCarDockTileEntity;
import dev.murad.shipping.block.rapidhopper.RapidHopperTileEntity;
import dev.murad.shipping.block.vesseldetector.VesselDetectorTileEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModTileEntitiesTypes {
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TugDockTileEntity>> TUG_DOCK = register(
            "tug_dock",
            TugDockTileEntity::new,
            ModBlocks.TUG_DOCK
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BargeDockTileEntity>> BARGE_DOCK = register(
            "barge_dock",
            BargeDockTileEntity::new,
            ModBlocks.BARGE_DOCK
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LocomotiveDockTileEntity>> LOCOMOTIVE_DOCK = register(
            "locomotive_dock",
            LocomotiveDockTileEntity::new,
            ModBlocks.LOCOMOTIVE_DOCK_RAIL
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TrainCarDockTileEntity>> CAR_DOCK = register(
            "car_dock",
            TrainCarDockTileEntity::new,
            ModBlocks.CAR_DOCK_RAIL
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VesselDetectorTileEntity>> VESSEL_DETECTOR = register(
            "vessel_detector",
            VesselDetectorTileEntity::new,
            ModBlocks.VESSEL_DETECTOR
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidHopperTileEntity>> FLUID_HOPPER = register(
            "fluid_hopper",
            FluidHopperTileEntity::new,
            ModBlocks.FLUID_HOPPER
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VesselChargerTileEntity>> VESSEL_CHARGER = register(
            "vessel_charger",
            VesselChargerTileEntity::new,
            ModBlocks.VESSEL_CHARGER
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RapidHopperTileEntity>> RAPID_HOPPER = register(
            "rapid_hopper",
            RapidHopperTileEntity::new,
            ModBlocks.RAPID_HOPPER
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DockBlockEntity>> DOCK = Registration.TILE_ENTITIES.register(
            "dock",
            () -> BlockEntityType.Builder.of(
                    (pos, state) -> new DockBlockEntity(ModTileEntitiesTypes.DOCK.get(), pos, state),
                    ModBlocks.DOCK_BLOCK.get(), ModBlocks.DOCK_RAIL.get()
            ).build(null)
    );

    private static <T extends BlockEntity> DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> register(
            String name,
            BlockEntityType.BlockEntitySupplier<T> factory,
            DeferredHolder<Block, ? extends Block> block) {
        return Registration.TILE_ENTITIES.register(name, () ->
                BlockEntityType.Builder.of(factory, block.get()).build(null));
    }

    public static void register () {

    }
}
