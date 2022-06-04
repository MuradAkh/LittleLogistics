package dev.murad.shipping.block.portal;

import dev.murad.shipping.setup.ModTileEntitiesTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class NetherTrainPortalTileEntity extends BlockEntity implements IPortalTileEntity{
    private final Queue<Runnable> taskQueue = new LinkedList<>();
    private Optional<PairPortal> pair = Optional.empty();

    private static record PairPortal(BlockPos pos, List<BlockPos> rails){}

    public NetherTrainPortalTileEntity(BlockPos pWorldPosition, BlockState pBlockState) {
        super(ModTileEntitiesTypes.NETHER_TRAIN_SENDER.get(), pWorldPosition, pBlockState);
    }

    public void tick(){
        if(!taskQueue.isEmpty()){
            taskQueue.remove().run();
        }
    }

    public void linkPortals(BlockPos pos){
        if (this.level != null && level instanceof ServerLevel serverLevel) {
            var target = Level.OVERWORLD;
            if (this.level.dimension().equals(Level.OVERWORLD)){
                target = Level.NETHER;
            } else if(!this.level.dimension().equals(Level.NETHER)) return;

            setUpLinkingTasks(
                    serverLevel.getServer().getLevel(target),
                    pos
            );
        }
    }

    private void setUpLinkingTasks(ServerLevel targetLevel, BlockPos pos){

        taskQueue.add(() -> {
                chunkLoad(targetLevel, pos, new ChunkPos(pos));
        });

        taskQueue.add(() -> {
           Optional.ofNullable(targetLevel.getBlockEntity(pos))
                   .flatMap(b -> b instanceof NetherTrainPortalTileEntity t ? Optional.of(t) : Optional.empty())
                   .filter(f -> f.getBlockState().getValue(NetherTrainPortalBlock.PORTAL_MODE) == NetherTrainPortalBlock.PortalMode.UNLINKED)
                   .ifPresent(found -> {
                       targetLevel.setBlockAndUpdate(pos, found.getBlockState().setValue(NetherTrainPortalBlock.PORTAL_MODE, NetherTrainPortalBlock.PortalMode.RECEIVER));
                       this.level.setBlockAndUpdate(getBlockPos(), this.getBlockState().setValue(NetherTrainPortalBlock.PORTAL_MODE, NetherTrainPortalBlock.PortalMode.SENDER));
                       found.pair = Optional.of(new PairPortal(this.getBlockPos(), getRails()));
                       this.pair = Optional.of(new PairPortal(pos, found.getRails()));
                   });
        });
    }

    private List<BlockPos> getRails(){
        return new ArrayList<>();
    }

    private static void chunkLoad(ServerLevel targetLevel, BlockPos pos, ChunkPos chunk) {
        targetLevel.getChunkSource().addRegionTicket(TicketType.PORTAL, chunk, 2, pos);
    }

    private void teleport(){

//        AbstractTrainCarEntity car = null;
//        var e = car.getType().create(car.getLevel());
//        e.restoreFrom(e);
//        car.getServer().getLevel(Level.NETHER);
//        ((ServerLevel) level).getChunkSource().addRegionTicket(TicketType.PORTAL);
    }

    private static BlockPos getNetherCoords(BlockPos overworldCoords){
        return new BlockPos(Math.floor(overworldCoords.getX() / 8f), overworldCoords.getY(), Math.floor(overworldCoords.getZ() / 8f));
    }

    private static BlockPos getOverworldCoords(BlockPos netherCoords){
        return new BlockPos(netherCoords.getX() * 8, netherCoords.getY(), netherCoords.getZ() * 8);
    }

    public static void serverTick(Level level, BlockPos blockPos, BlockState blockState, NetherTrainPortalTileEntity e) {
        e.tick();
    }

}
