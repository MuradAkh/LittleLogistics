package dev.murad.shipping.setup;

import dev.murad.shipping.block.dockingstation.DockingStationBlockEntity;
import dev.murad.shipping.block.vesseldetector.VesselDetectorTileEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModTileEntitiesTypes {
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VesselDetectorTileEntity>> VESSEL_DETECTOR = register(
            "vessel_detector",
            VesselDetectorTileEntity::new,
            ModBlocks.VESSEL_DETECTOR
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DockingStationBlockEntity>> DOCKING_STATION = Registration.TILE_ENTITIES.register(
            "docking_station",
            () -> BlockEntityType.Builder.of(
                    (pos, state) -> new DockingStationBlockEntity(ModTileEntitiesTypes.DOCKING_STATION.get(), pos, state),
                    ModBlocks.DOCKING_STATION.get()
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
