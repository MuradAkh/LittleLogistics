package dev.murad.shipping.entity.custom.barge;

import com.mojang.datafixers.util.Pair;
import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.setup.ModEntityTypes;
import dev.murad.shipping.setup.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.BoatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.world.ForgeChunkManager;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class ChunkLoaderBargeEntity extends AbstractBargeEntity{
    private Optional<Pair<Integer, Integer>> loadedChunk = Optional.empty();

    public ChunkLoaderBargeEntity(EntityType<? extends BoatEntity> type, World world) {
        super(type, world);
    }

    public ChunkLoaderBargeEntity(World worldIn, double x, double y, double z) {
        super(ModEntityTypes.CHUNK_LOADER_BARGE.get(), worldIn, x, y, z);
    }

    @Override
    public void remove(){
        super.remove();
        if(!this.level.isClientSide){
            loadedChunk.ifPresent(c -> getSurroundingChunks(c).forEach(ch -> this.setChunkLoad(false, ch)));
        }
    }

    @Override
    public void tick(){
        super.tick();
        if(this.level.isClientSide){
            return;
        }
        Pair<Integer, Integer> currChunk = new Pair<>(this.xChunk, this.zChunk);
        if (!loadedChunk.isPresent()){
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

    @Override
    protected void addAdditionalSaveData(CompoundNBT p_213281_1_) {
        if(loadedChunk.isPresent()) {
            p_213281_1_.putInt("xchunk", loadedChunk.get().getFirst());
            p_213281_1_.putInt("zchunk", loadedChunk.get().getSecond());
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundNBT p_70037_1_) {
        if (p_70037_1_.contains("xchunk")) {
            int x = p_70037_1_.getInt("xchunk");
            int z = p_70037_1_.getInt("zchunk");
            loadedChunk = Optional.of(new Pair<>(x, z));
        }
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
        ForgeChunkManager.forceChunk((ServerWorld) this.level, ShippingMod.MOD_ID, this, chunk.getFirst(), chunk.getSecond(), add, true);
    }

    @Override
    public Item getDropItem() {
        return ModItems.CHUNK_LOADER_BARGE.get();
    }

    @Override
    protected void doInteract(PlayerEntity player) {

    }
}
