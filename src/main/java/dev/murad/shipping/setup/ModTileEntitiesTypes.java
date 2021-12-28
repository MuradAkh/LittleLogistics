package dev.murad.shipping.setup;

import dev.murad.shipping.block.dock.TugDockTileEntity;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.fml.RegistryObject;

import java.util.function.Supplier;

public class ModTileEntitiesTypes {
    public static final RegistryObject<TileEntityType<TugDockTileEntity>> TUG_DOCK = register(
            "ship_lock",
            TugDockTileEntity::new,
            ModBlocks.TUG_DOCK
    );

    private static <T extends TileEntity> RegistryObject<TileEntityType<T>> register(String name, Supplier<T> factory, RegistryObject<? extends Block> block) {
        return Registration.TILE_ENTITIES.register(name, () -> TileEntityType.Builder.of(factory, block.get()).build(null));
    }

    public static void register () {

    }
}
