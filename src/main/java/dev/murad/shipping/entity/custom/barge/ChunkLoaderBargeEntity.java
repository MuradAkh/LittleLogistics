package dev.murad.shipping.entity.custom.barge;

import dev.murad.shipping.setup.ModEntityTypes;
import dev.murad.shipping.setup.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.BoatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public class ChunkLoaderBargeEntity extends AbstractBargeEntity{
    public ChunkLoaderBargeEntity(EntityType<? extends BoatEntity> type, World world) {
        super(type, world);
    }

    public ChunkLoaderBargeEntity(World worldIn, double x, double y, double z) {
        super(ModEntityTypes.CHUNK_LOADER_BARGE.get(), worldIn, x, y, z);
    }

    @Override
    public Item getDropItem() {
        return ModItems.CHUNK_LOADER_BARGE.get();
    }

    @Override
    protected void doInteract(PlayerEntity player) {

    }
}
