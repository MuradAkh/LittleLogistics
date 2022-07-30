package dev.murad.shipping.entity.custom.train.wagon;

import dev.murad.shipping.setup.ModEntityTypes;
import dev.murad.shipping.setup.ModItems;
import dev.murad.shipping.util.MobileChunkLoader;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ChunkLoaderCarEntity extends AbstractWagonEntity {
    private final MobileChunkLoader mobileChunkLoader;

    public ChunkLoaderCarEntity(EntityType<?> p_38087_, Level p_38088_) {
        super(p_38087_, p_38088_);
        mobileChunkLoader = new MobileChunkLoader(this);

    }

    public ChunkLoaderCarEntity(Level level, Double aDouble, Double aDouble1, Double aDouble2) {
        super(ModEntityTypes.CHUNK_LOADER_CAR.get(), level, aDouble, aDouble1, aDouble2);
        mobileChunkLoader = new MobileChunkLoader(this);

    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.SEATER_CAR.get());
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
        super.addAdditionalSaveData(p_213281_1_);
        mobileChunkLoader.addAdditionalSaveData(p_213281_1_);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag p_70037_1_) {
        super.readAdditionalSaveData(p_70037_1_);
        mobileChunkLoader.readAdditionalSaveData(p_70037_1_);
    }

}
