package dev.murad.shipping.entity.custom.vessel.barge;

import dev.murad.shipping.setup.ModEntityTypes;
import dev.murad.shipping.setup.ModItems;
import dev.murad.shipping.util.MobileChunkLoader;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class ChunkLoaderBargeEntity extends AbstractBargeEntity {
    private final MobileChunkLoader mobileChunkLoader;

    public ChunkLoaderBargeEntity(EntityType<? extends ChunkLoaderBargeEntity> type, Level world) {
        super(type, world);
        mobileChunkLoader = new MobileChunkLoader(this);
    }

    public ChunkLoaderBargeEntity(Level worldIn, double x, double y, double z) {
        super(ModEntityTypes.CHUNK_LOADER_BARGE.get(), worldIn, x, y, z);
        mobileChunkLoader = new MobileChunkLoader(this);
    }

    @Override
    public void remove(RemovalReason r){
        super.remove(r);
        if(!this.level.isClientSide){
            mobileChunkLoader.remove();
        }
    }

    @Override
    public void tick(){
        super.tick();
        if(!this.level.isClientSide){
            mobileChunkLoader.serverTick();
        }

    }

    @Override
    public void addAdditionalSaveData(CompoundTag p_213281_1_) {
        mobileChunkLoader.addAdditionalSaveData(p_213281_1_);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag p_70037_1_) {
        mobileChunkLoader.readAdditionalSaveData(p_70037_1_);
    }

    @Override
    public Item getDropItem() {
        return ModItems.CHUNK_LOADER_BARGE.get();
    }

    @Override
    protected void doInteract(Player player) {

    }
}
