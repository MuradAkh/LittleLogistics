package dev.murad.shipping.util;

import dev.murad.shipping.ShippingConfig;
import dev.murad.shipping.ShippingMod;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.common.world.ForgeChunkManager;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Getter
@RequiredArgsConstructor
public class MobileChunkLoader {
    private final Entity entity;

    @Nullable
    private ChunkPos loadedChunk = null;

    private ChunkPos offset(ChunkPos chunk, int x, int z) {
        return new ChunkPos(chunk.x + x, chunk.z + z);
    }

    private Set<ChunkPos> getSurroundingChunks(ChunkPos chunk){
        Set<ChunkPos> set = new HashSet<>();
        for(int i = -1; i <= 1; i++){
            for (int j = -1; j <= 1; j++){
                set.add(offset(chunk, i, j));
            }
        }
        return set;
    }

    private void setChunkLoad(boolean add, ChunkPos chunk) {
        ForgeChunkManager.forceChunk((ServerLevel) entity.level, ShippingMod.MOD_ID, entity, chunk.x, chunk.z, add, false);
    }

    public void serverTick(){
        if(ShippingConfig.Server.DISABLE_CHUNKLOADERS.get()){
            // Not the best UX, but if server owners want better UX they should use a datapack to disable the recipe
            entity.remove(Entity.RemovalReason.DISCARDED);
            return;
        }

        ChunkPos currChunk = entity.chunkPosition();
        if (loadedChunk == null){
            getSurroundingChunks(currChunk).forEach(c -> setChunkLoad(true, c));
            loadedChunk = currChunk;
        } else if (!currChunk.equals(loadedChunk)){
            Set<ChunkPos> needsToBeLoaded = getSurroundingChunks(currChunk);

            Set<ChunkPos> toUnload = getSurroundingChunks(loadedChunk);
            toUnload.removeAll(needsToBeLoaded);

            Set<ChunkPos> prevLoaded = getSurroundingChunks(loadedChunk);
            needsToBeLoaded.removeAll(prevLoaded);


            toUnload.forEach(c -> setChunkLoad(false, c));
            needsToBeLoaded.forEach(c -> setChunkLoad(true, c));

            loadedChunk = currChunk;
        }
    }

    public void addAdditionalSaveData(CompoundTag p_213281_1_) {
        if(loadedChunk != null) {
            p_213281_1_.putInt("xchunk", loadedChunk.x);
            p_213281_1_.putInt("zchunk", loadedChunk.z);
        }
    }

    public void readAdditionalSaveData(CompoundTag p_70037_1_) {
        if (p_70037_1_.contains("xchunk")) {
            int x = p_70037_1_.getInt("xchunk");
            int z = p_70037_1_.getInt("zchunk");
            loadedChunk = new ChunkPos(x, z);
        }
    }

    public void remove(){
        if (loadedChunk != null) {
            getSurroundingChunks(loadedChunk).forEach(ch -> this.setChunkLoad(false, ch));
        }
    }
}
