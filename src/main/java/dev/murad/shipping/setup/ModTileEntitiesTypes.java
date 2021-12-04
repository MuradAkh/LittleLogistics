package dev.murad.shipping.setup;

import dev.murad.shipping.block.shiplock.ShipLockTileEntity;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.fml.RegistryObject;

import java.util.function.Supplier;

public class ModTileEntitiesTypes {
    public static final RegistryObject<TileEntityType<ShipLockTileEntity>> METAL_PRESS = register(
            "ship_lock",
            ShipLockTileEntity::new,
            ModBlocks.SHIP_LOCK
    );

    private static <T extends TileEntity> RegistryObject<TileEntityType<T>> register(String name, Supplier<T> factory, RegistryObject<? extends Block> block) {
        return Registration.TILE_ENTITIES.register(name, () -> TileEntityType.Builder.of(factory, block.get()).build(null));
    }

    public static void register () {

    }
}
