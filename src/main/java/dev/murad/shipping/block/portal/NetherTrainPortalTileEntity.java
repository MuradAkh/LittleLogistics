package dev.murad.shipping.block.portal;

import com.mojang.datafixers.util.Pair;
import com.mojang.math.Vector3f;
import dev.murad.shipping.setup.ModTileEntitiesTypes;
import dev.murad.shipping.util.LinkableEntity;
import dev.murad.shipping.util.Train;
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

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class NetherTrainPortalTileEntity extends BlockEntity implements IPortalTileEntity {
    private static final DustParticleOptions PARTICLE = new DustParticleOptions(new Vector3f(0.788f, 0.2f, 0.901f), 1.0f);

    // only execute work tasks if the BE isn't removed
    private final Queue<Task> workQueue = new LinkedList<>();
    private final static String X_TAG = "xpos";
    private final static String Y_TAG = "ypos";
    private final static String Z_TAG = "zpos";
    private final static String DIMENSION_TAG = "dimension";

    // todo: serialize and save
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
        if ((t = workQueue.peek()) != null && t.execute()) {
            workQueue.remove();
        }

    }

    private void enqueue(Task task) {
        workQueue.add(task);
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

            // wait for 2 seconds for the level to load
            AtomicInteger remainingTicks = new AtomicInteger(40);

            enqueue(() -> {
                // check if the destination chunk is loaded
                if (targetLevel.isLoaded(targetPos)) {
                    validateAndWarpTrain(head.getTrain(), targetLevel, targetPos);
                    return true;
                } else {
                    return remainingTicks.decrementAndGet() < 0;
                }
            });
        });

    }

    private <T extends Entity & LinkableEntity<T>> void validateAndWarpTrain(Train<T> train, ServerLevel targetLevel, BlockPos portalLocation) {
        Optional.ofNullable(targetLevel.getBlockEntity(portalLocation))
                .flatMap(e -> e instanceof NetherTrainPortalTileEntity p ? Optional.of(p) : Optional.empty())
                .ifPresent(portal -> {
                    warpTrain(train, portal.getBlockState().getValue(IPortalBlock.FACING), portal.getBlockPos(), targetLevel);
                });

    }


    private <T extends Entity & LinkableEntity<T>> void warpTrain(Train<T> train, Direction portalDirection, BlockPos portalPos, ServerLevel targetLevel) {
        List<T> targets = train.asList();
        targets.forEach(LinkableEntity::removeDominated);
        targets.forEach(LinkableEntity::removeDominant);
        AtomicInteger num = new AtomicInteger(train.asList().size() + 2);
        targets
            .stream()
            .map(e -> teleportEntity(e, num.getAndDecrement(), portalDirection, portalPos, targetLevel))
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
            passenger.stopRiding();
            return teleportEntity(passenger, num, portalDirection, portalPos, targetLevel);
        }).filter(Optional::isPresent).map(Optional::get);

        return Optional.ofNullable((Entity) oldEntity.getType().create(targetLevel))
                .map(e -> (T) e)
                .map(newEntity -> {
                    newEntity.restoreFrom(oldEntity);
                    oldEntity.remove(Entity.RemovalReason.DISCARDED);
                    targetLevel.addDuringTeleport(newEntity);
                    newEntity.setPos(targetPos);
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

    @FunctionalInterface
    public interface Task {
        /**
         * Execute the task
         *
         * @return true if task is finished
         */
        boolean execute();
    }
}
