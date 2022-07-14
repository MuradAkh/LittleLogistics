package dev.murad.shipping.util;

import com.mojang.datafixers.util.Pair;
import dev.murad.shipping.ShippingConfig;
import dev.murad.shipping.ShippingMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.world.ForgeChunkManager;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class MobileChunkLoader {
    private Optional<Pair<Integer, Integer>> loadedChunk = Optional.empty();
    private Entity entity;

    public MobileChunkLoader(Entity entity){
        this.entity = entity;
    }

    private Set<Pair<Integer, Integer>> getSurroundingChunks(Pair<Integer, Integer> chunk){
        Set<Pair<Integer, Integer>> set = new HashSet<>();
        for(int i = -1; i <= 1; i++){
            for (int j = -1; j <= 1; j++){
                set.add(new Pair<>(chunk.getFirst() + i, chunk.getSecond() + j));
            }
        }
        return set;
    }

    private void setChunkLoad(boolean add, Pair<Integer, Integer> chunk) {
        ForgeChunkManager.forceChunk((ServerLevel) entity.level, ShippingMod.MOD_ID, entity, chunk.getFirst(), chunk.getSecond(), add, false);
    }

    public void serverTick(){
        Pair<Integer, Integer> currChunk = new Pair<>(entity.chunkPosition().x, entity.chunkPosition().z);
        if (loadedChunk.isEmpty()){
            getSurroundingChunks(currChunk).forEach(c -> setChunkLoad(true, c));
            loadedChunk = Optional.of(currChunk);
        } else if (!currChunk.equals(loadedChunk.get())){
            Set<Pair<Integer, Integer>> needsToBeLoaded = getSurroundingChunks(currChunk);

            Set<Pair<Integer, Integer>> toUnload = getSurroundingChunks(loadedChunk.get());
            toUnload.removeAll(needsToBeLoaded);

            Set<Pair<Integer, Integer>> prevLoaded = getSurroundingChunks(loadedChunk.get());
            needsToBeLoaded.removeAll(prevLoaded);


            toUnload.forEach(c -> setChunkLoad(false, c));
            needsToBeLoaded.forEach(c -> setChunkLoad(true, c));

            loadedChunk = Optional.of(currChunk);
        }
    }

    public void addAdditionalSaveData(CompoundTag p_213281_1_) {
        if(loadedChunk.isPresent()) {
            p_213281_1_.putInt("xchunk", loadedChunk.get().getFirst());
            p_213281_1_.putInt("zchunk", loadedChunk.get().getSecond());
        }
    }

    public void readAdditionalSaveData(CompoundTag p_70037_1_) {
        if (p_70037_1_.contains("xchunk")) {
            int x = p_70037_1_.getInt("xchunk");
            int z = p_70037_1_.getInt("zchunk");
            loadedChunk = Optional.of(new Pair<>(x, z));
        }
    }

    public void remove(){
        loadedChunk.ifPresent(c -> getSurroundingChunks(c).forEach(ch -> this.setChunkLoad(false, ch)));
    }
}
