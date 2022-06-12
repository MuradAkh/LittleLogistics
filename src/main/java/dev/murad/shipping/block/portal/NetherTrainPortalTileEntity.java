package dev.murad.shipping.block.portal;

import com.mojang.math.Vector3f;
import dev.murad.shipping.setup.ModBlocks;
import dev.murad.shipping.setup.ModTileEntitiesTypes;
import dev.murad.shipping.util.LinkableEntity;
import dev.murad.shipping.util.Train;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class NetherTrainPortalTileEntity extends BlockEntity implements IPortalTileEntity {
    private static final DustParticleOptions PARTICLE = new DustParticleOptions(new Vector3f(0.788f, 0.2f, 0.901f), 1.0f);

    // only execute work tasks if the BE isn't removed
    private final Queue<Task> workQueue = new LinkedList<>();
    private final static String X_TAG = "xpos";
    private final static String Y_TAG = "ypos";
    private final static String Z_TAG = "zpos";
    private final static String DIMENSION_TAG = "dimension";

    private Optional<PortalLocation> otherPortal = Optional.empty();

    public record PortalLocation(ResourceKey<Level> dimension, BlockPos pos) {
    }

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

    public void tick() {
        Task t;
        // todo: this is a cringe if statement :P
        if ((t = workQueue.peek()) != null && (t.runnable.getAsBoolean() || t.remainingAttempts-- <= 0)) {
            workQueue.remove();
        }
    }

    private void enqueue(BooleanSupplier task) {
        workQueue.add(new Task(task, 1));
    }

    private void enqueue(BooleanSupplier task, int maxAttempts) {
        workQueue.add(new Task(task, maxAttempts));
    }

    public void linkPortals(ResourceKey<Level> targetLevelKey, BlockPos targetPos) {
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
                    return false;
                }
            }, 40);
        } else if (level != null) {
            showParticles(level, getBlockState(), getBlockPos());
        }
    }

    static void showParticles(LevelAccessor level, BlockState state, BlockPos pos) {
        if (level != null) {
            Direction facing = state.getValue(IPortalBlock.FACING);
            if (facing.getAxis().equals(Direction.Axis.Z)) {
                facing = facing.getOpposite();
            }
            for (double i = 0; i < 1; i += 0.1) {
                for (double j = 0; j < 1; j += 0.1) {
                    Vec3 vec = Vec3.atLowerCornerOf(pos);
                    Vec3 point = new Vec3(i, j, 0)
                            .add(-0.5, 0, -0.5)
                            .yRot((float) Math.toRadians(facing.toYRot()))
                            .subtract(-0.5, 0, -0.5);
                    Vec3 pPos = vec.add(point);
                    level.addParticle(new DustParticleOptions(new Vector3f(0.788f, 0.2f, 0.901f), 2.0f), pPos.x, pPos.y, pPos.z, 0, 0, 0);
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
            if (targetLevel == null) {
                return;
            }
            BlockPos targetPos = portalLocation.pos;
            ChunkPos targetChunk = new ChunkPos(targetPos);

            enqueue(() -> {
                chunkLoad(targetLevel, targetPos, targetChunk);
                return true;
            });

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
                    return false;
                }
            }, 40);
        }
    }

    private static void chunkLoad(ServerLevel targetLevel, BlockPos pos, ChunkPos chunk) {
        // todo: distance
        targetLevel.getChunkSource().addRegionTicket(TicketType.PORTAL, chunk, 2, pos);
    }

    /**
     * Validate all chunks around pos are loaded at a certain distance
     */
    private static boolean areChunksLoaded(ServerLevel level, int distance, ChunkPos pos) {
        for (int i = -distance + 1; i < distance; i++) {
            for (int j = -distance + 1; j < distance; j++) {
                if (!level.getChunkSource().hasChunk(pos.x + i, pos.z + j)) {
                    return false;
                }
            }
        }
        return true;
    }

    public <T extends Entity & LinkableEntity<T>> void selfTeleport(T head) {
        if (this.level.isClientSide) {
            return;
        }
        otherPortal.ifPresent(portalLocation -> {
            BlockPos targetPos = portalLocation.pos;
            ChunkPos targetChunk = new ChunkPos(targetPos);
            ServerLevel targetLevel = this.level.getServer().getLevel(portalLocation.dimension);
            if (targetLevel == null) {
                return;
            }

            enqueue(() -> {
                chunkLoad(targetLevel, targetPos, targetChunk);
                return true;
            });

            enqueue(() -> {
                // check if the destination chunk is loaded
                if (areChunksLoaded(targetLevel, 2, targetChunk)) {
                    validateAndWarpTrain(head.getTrain(), targetLevel, targetPos);
                    return true;
                } else {
                    return false;
                }
            }, 40);
        });
    }

    public boolean validateTrainSubstrate(int trainSize,
                                          Level sourceLevel, NetherTrainPortalTileEntity sourcePortal,
                                          Level destLevel, NetherTrainPortalTileEntity destPortal) {
        // todo make configuration
        if (trainSize > 15) {
            return false;
        }

        trainSize++;

        Direction sourceFacing = sourcePortal.getBlockState().getValue(NetherTrainPortalBlock.FACING);
        Direction destFacing = destPortal.getBlockState().getValue(NetherTrainPortalBlock.FACING);

        // make sure source has enough substrate blocks
        for (int i = 1; i <= trainSize; i++) {
            BlockPos sourcePos = sourcePortal.getBlockPos().offset(sourceFacing.getNormal().multiply(i));
            BlockPos destPos = destPortal.getBlockPos().offset(destFacing.getNormal().multiply(i));
            // assert that sourcePos and destPos are all obsidian rails
            if (!sourceLevel.getBlockState(sourcePos).getBlock().equals(ModBlocks.OBSIDIAN_RAIL.get()) ||
                !destLevel.getBlockState(destPos).getBlock().equals(ModBlocks.OBSIDIAN_RAIL.get()))
                return false;
        }

        return true;
    }

    private <T extends Entity & LinkableEntity<T>> void validateAndWarpTrain(Train<T> train, ServerLevel targetLevel, BlockPos portalLocation) {
        int trainSize = train.asList().size();
        BlockEntity be = targetLevel.getBlockEntity(portalLocation);
        if (be instanceof NetherTrainPortalTileEntity p) {
            // check that source has enough rails
            if (!validateTrainSubstrate(trainSize, getLevel(), this, targetLevel, p)) {
                return;
            }
            warpTrain(train, p.getBlockState().getValue(IPortalBlock.FACING), p.getBlockPos(), targetLevel);
        }
    }


    private <T extends Entity & LinkableEntity<T>> void warpTrain(Train<T> train, Direction targetFacing, BlockPos targetPos, ServerLevel targetLevel) {
        List<T> targets = train.asList();
        targets.forEach(LinkableEntity::removeDominated);
        targets.forEach(LinkableEntity::removeDominant);
        AtomicInteger num = new AtomicInteger(targets.size() + 1);

        targets
            .stream()
            .map(e -> teleportEntity(e, num.getAndDecrement(), targetFacing, targetPos, targetLevel))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .reduce((entity, entity2) -> {
                entity.setDominated(entity2);
                entity2.setDominant(entity);
                return entity2;
            });
    }

    private <T extends Entity> Optional<T> teleportEntity(T oldEntity, int num, Direction portalDirection, BlockPos portalPos, ServerLevel targetLevel) {
        var targetPos = Vec3.atCenterOf(portalPos.relative(portalDirection, num));

        if(oldEntity instanceof ServerPlayer player){
            player.teleportTo(targetLevel, targetPos.x, targetPos.y, targetPos.z, portalDirection.toYRot(), 90);
            return Optional.of(oldEntity);
        }

        var passengers = oldEntity.getPassengers().stream().map(passenger -> {
            passenger.removeVehicle();
            return teleportEntity(passenger, num, portalDirection, portalPos, targetLevel);
        }).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toSet());

        return Optional.ofNullable((Entity) oldEntity.getType().create(targetLevel))
                .map(e -> (T) e)
                .map(newEntity -> {
                    newEntity.restoreFrom(oldEntity);
                    oldEntity.remove(Entity.RemovalReason.CHANGED_DIMENSION);
                    newEntity.setPos(targetPos);
                    targetLevel.addDuringTeleport(newEntity);
                    passengers.forEach(passenger -> {
                        passenger.startRiding(newEntity);
                    });
                    return newEntity;
                });
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
        if (pTag != null && pTag.contains(DIMENSION_TAG)) {
            var pos = new BlockPos(pTag.getInt(X_TAG), pTag.getInt(Y_TAG), pTag.getInt(Z_TAG));
            var dimension = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(pTag.getString(DIMENSION_TAG)));
            this.otherPortal = Optional.of(new PortalLocation(dimension, pos));

        }
    }

    @AllArgsConstructor
    private class Task {
        private final BooleanSupplier runnable;
        private int remainingAttempts;
    }
}
