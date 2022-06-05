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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class NetherTrainPortalTileEntity extends BlockEntity implements IPortalTileEntity {
    // only execute work tasks if the BE isn't removed
    private final Queue<Task> workQueue = new LinkedList<>();

    // todo: serialize and save
    private Optional<PortalLocation> otherPortal = Optional.empty();

    public record PortalLocation(ServerLevel level, BlockPos pos) {}

    public NetherTrainPortalTileEntity(BlockPos pWorldPosition, BlockState pBlockState) {
        super(ModTileEntitiesTypes.NETHER_TRAIN_PORTAL.get(), pWorldPosition, pBlockState);
    }

    public boolean isLinked() {
        return otherPortal.isPresent();
    }

    public void setLink(ServerLevel targetLevel, BlockPos targetPos) {
        level.setBlockAndUpdate(getBlockPos(),
                getBlockState().setValue(NetherTrainPortalBlock.PORTAL_MODE, NetherTrainPortalBlock.PortalMode.LINKED));
        this.otherPortal = Optional.of(new PortalLocation(targetLevel, targetPos));
    }

    public void unsetLink() {
        level.setBlockAndUpdate(getBlockPos(),
                getBlockState().setValue(NetherTrainPortalBlock.PORTAL_MODE, NetherTrainPortalBlock.PortalMode.UNLINKED));
        this.otherPortal = Optional.empty();
    }

    public boolean isAt(PortalLocation location) {
        return this.level.dimension().equals(location.level.dimension()) &&
                this.getBlockPos().equals(location.pos);
    }

    public void tick(){
        Task t;
        if ((t = workQueue.peek()) != null && t.execute()) {
            workQueue.remove();
        }
    }

    private void enqueue(Task task) {
        workQueue.add(task);
    }

    public void linkPortals(ResourceKey<Level> targetLevelKey, BlockPos targetPos){
        if (this.level != null && level instanceof ServerLevel sourceLevel) {
            BlockPos sourcePos = this.getBlockPos();
            ServerLevel targetLevel = sourceLevel.getServer().getLevel(targetLevelKey);
            ChunkPos targetChunk = new ChunkPos(targetPos);
            if (targetLevel == null) return;

            enqueue(() -> {
                chunkLoad(targetLevel, targetPos, targetChunk);
                return true;
            });

            // wait for 2 seconds for the level to load
            AtomicInteger remainingTicks = new AtomicInteger(40);

            enqueue(() -> {
                // check if the destination chunk is loaded
                if (targetLevel.isLoaded(targetPos)) {
                    BlockEntity b = targetLevel.getBlockEntity(targetPos);
                    if ((b instanceof NetherTrainPortalTileEntity targetBE) && !targetBE.isLinked()) {
                        targetBE.setLink(sourceLevel, sourcePos);
                        setLink(targetLevel, targetPos);
                        return false;
                    } else {
                        return true;
                    }
                } else {
                    return remainingTicks.decrementAndGet() < 0;
                }
            });
        }
    }

    public void unlinkPortals() {
        if (!isLinked() || otherPortal.isEmpty()) return;

        PortalLocation portalLocation = otherPortal.get();
        ServerLevel targetLevel = portalLocation.level;
        BlockPos targetPos = portalLocation.pos;
        ChunkPos targetChunk = new ChunkPos(targetPos);

        enqueue(() -> {
            chunkLoad(targetLevel, targetPos, targetChunk);
            return true;
        });

        // wait for 2 seconds for the level to load
        AtomicInteger remainingTicks = new AtomicInteger(40);

        enqueue(() -> {
            // check if the destination chunk is loaded
            if (targetLevel.isLoaded(targetPos)) {
                BlockEntity b = targetLevel.getBlockEntity(targetPos);
                if ((b instanceof NetherTrainPortalTileEntity targetBE) &&
                        !targetBE.isLinked()) {
                    if (isAt(targetBE.otherPortal.get())) {
                        targetBE.unsetLink();
                    }
                    unsetLink();
                    return false;
                } else {
                    return true;
                }
            } else {
                return remainingTicks.decrementAndGet() < 0;
            }
        });

    }

    private static void chunkLoad(ServerLevel targetLevel, BlockPos pos, ChunkPos chunk) {
        // todo: distance
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

    @FunctionalInterface
    public interface Task {
        /**
         * Execute the task
         * @return true if task is finished
         */
        boolean execute();
    }
}
