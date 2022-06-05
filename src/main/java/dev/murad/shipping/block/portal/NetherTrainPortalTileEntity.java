package dev.murad.shipping.block.portal;

import dev.murad.shipping.setup.ModTileEntitiesTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class NetherTrainPortalTileEntity extends BlockEntity implements IPortalTileEntity {
    private final Queue<Runnable> taskQueue = new LinkedList<>();

    // todo: serialize and save
    private Optional<PortalLocation> otherPortal = Optional.empty();

    public record PortalLocation(Level level, BlockPos pos) {}

    public NetherTrainPortalTileEntity(BlockPos pWorldPosition, BlockState pBlockState) {
        super(ModTileEntitiesTypes.NETHER_TRAIN_PORTAL.get(), pWorldPosition, pBlockState);
    }

    public void tick(){
        if(!taskQueue.isEmpty()){
            taskQueue.remove().run();
        }
    }

    public void linkPortals(ResourceKey<Level> targetLevel, BlockPos pos){
        if (this.level != null && level instanceof ServerLevel serverLevel) {
            enqueueLinkTasks(serverLevel.getServer().getLevel(targetLevel), pos);
        }
    }

    private void enqueueLinkTasks(ServerLevel targetLevel, BlockPos pos){
        taskQueue.add(() -> {
                chunkLoad(targetLevel, pos, new ChunkPos(pos));
        });

        taskQueue.add(() -> {
           Optional.ofNullable(targetLevel.getBlockEntity(pos))
                   .flatMap(b -> b instanceof NetherTrainPortalTileEntity t ? Optional.of(t) : Optional.empty())
                   .filter(f -> f.getBlockState().getValue(NetherTrainPortalBlock.PORTAL_MODE) == NetherTrainPortalBlock.PortalMode.UNLINKED)
                   .ifPresent(found -> {
                       targetLevel.setBlockAndUpdate(pos, found.getBlockState().setValue(NetherTrainPortalBlock.PORTAL_MODE, NetherTrainPortalBlock.PortalMode.LINKED));
                       this.level.setBlockAndUpdate(getBlockPos(), this.getBlockState().setValue(NetherTrainPortalBlock.PORTAL_MODE, NetherTrainPortalBlock.PortalMode.LINKED));
                       found.otherPortal = Optional.of(new PortalLocation(this.level, this.getBlockPos()));
                       this.otherPortal = Optional.of(new PortalLocation(targetLevel, pos));
                   });
        });
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

    public static void serverTick(Level level, BlockPos blockPos, BlockState blockState, NetherTrainPortalTileEntity e) {
        e.tick();
    }

}
