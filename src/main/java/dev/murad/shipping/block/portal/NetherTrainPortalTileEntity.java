package dev.murad.shipping.block.portal;

import com.mojang.math.Vector3f;
import dev.murad.shipping.setup.ModTileEntitiesTypes;
import dev.murad.shipping.util.MathUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class NetherTrainPortalTileEntity extends BlockEntity implements IPortalTileEntity {
    private static final DustParticleOptions PARTICLE = new DustParticleOptions(new Vector3f(0.9f, 0.65f, 0.2f), 1.0f);

    // only execute work tasks if the BE isn't removed
    private final Queue<Task> workQueue = new LinkedList<>();
    private final static String X_TAG = "xpos";
    private final static String Y_TAG = "ypos";
    private final static String Z_TAG = "zpos";
    private final static String DIMENSION_TAG = "dimension";

    // todo: serialize and save
    private Optional<PortalLocation> otherPortal = Optional.empty();

    public record PortalLocation(ResourceKey<Level> dimension, BlockPos pos) {}

    public NetherTrainPortalTileEntity(BlockPos pWorldPosition, BlockState pBlockState) {
        super(ModTileEntitiesTypes.NETHER_TRAIN_PORTAL.get(), pWorldPosition, pBlockState);
    }

    public boolean isLinked() {
        return otherPortal.isPresent();
    }

    public void setLink(ServerLevel targetLevel, BlockPos targetPos) {
        level.setBlockAndUpdate(getBlockPos(),
                getBlockState().setValue(NetherTrainPortalBlock.PORTAL_MODE, NetherTrainPortalBlock.PortalMode.LINKED));
        this.otherPortal = Optional.of(new PortalLocation(targetLevel.dimension(), targetPos));
    }

    public void unsetLink() {
        System.out.println("Unsetting");
        level.setBlockAndUpdate(getBlockPos(),
                getBlockState().setValue(NetherTrainPortalBlock.PORTAL_MODE, NetherTrainPortalBlock.PortalMode.UNLINKED));
        this.otherPortal = Optional.empty();
    }

    public boolean isAt(PortalLocation location) {
        return this.level.dimension().equals(location.dimension()) &&
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
        showParticles();
        if (this.level != null && level instanceof ServerLevel sourceLevel) {
            if (!workQueue.isEmpty()) return;

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
                    }
                    return true;
                } else {
                    return remainingTicks.decrementAndGet() < 0;
                }
            });
        }
    }

    private void showParticles() {
        if(this.level != null && this.level.isClientSide) {
            Direction facing = this.getBlockState().getValue(IPortalBlock.FACING);
            for (double i = 0; i < 1; i += 0.1) {
                for (double j = 0; j < 1; j += 0.1) {
                    Vec3 vec = Vec3.atLowerCornerOf(getBlockPos());
                    Vec3 pPos = vec.add(new Vec3(i, j, 0).yRot((float) Math.toRadians(facing.toYRot())));
                    level.addParticle(PARTICLE, pPos.x, pPos.y, pPos.z, 0, 0, 0);
                }
            }
        }
    }

    public void unlinkPortals() {
        if (this.level != null && this.level instanceof ServerLevel) {
            if (!workQueue.isEmpty() || !isLinked() || otherPortal.isEmpty()) return;

            System.out.println("Unlinking portals");

            PortalLocation portalLocation = otherPortal.get();
            ServerLevel targetLevel = this.level.getServer().getLevel(portalLocation.dimension);
            if(targetLevel == null){
                return;
            }
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
                    if ((b instanceof NetherTrainPortalTileEntity targetBE)) {
                        if (targetBE.isLinked() && isAt(targetBE.otherPortal.get())) {
                            targetBE.unsetLink();
                        }
                        unsetLink();
                    }
                    return true;
                } else {
                    return remainingTicks.decrementAndGet() < 0;
                }
            });
        }
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

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        super.saveAdditional(pTag);
        otherPortal.ifPresent(portalLocation -> {
            pTag.putInt(X_TAG, portalLocation.pos.getX());
            pTag.putInt(Z_TAG, portalLocation.pos.getZ());
            pTag.putInt(Y_TAG, portalLocation.pos.getY());
            pTag.putString(DIMENSION_TAG, portalLocation.dimension.location().toString());
        });
    }

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        if(pTag != null && pTag.contains(DIMENSION_TAG)){
            var pos = new BlockPos(pTag.getInt(X_TAG), pTag.getInt(Y_TAG), pTag.getInt(Z_TAG));
            var dimension = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(pTag.getString(DIMENSION_TAG)));
            this.otherPortal = Optional.of(new PortalLocation(dimension, pos));

        }
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
