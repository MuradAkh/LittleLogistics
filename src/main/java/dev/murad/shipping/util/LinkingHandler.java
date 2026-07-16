package dev.murad.shipping.util;

import dev.murad.shipping.capability.StallingCapability;
import dev.murad.shipping.entity.custom.HeadVehicle;
import dev.murad.shipping.global.VehicleRegistrationData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class LinkingHandler<T extends Entity & LinkableEntity<T>> {
    private boolean waitForDominated = false;
    private final T entity;
    private final Class<T> clazz;

    public Optional<T> leader = Optional.empty();
    public Optional<T> follower = Optional.empty();

    public Train<T> train;

    private @Nullable
    CompoundTag dominantNBT;
    private final EntityDataAccessor<Integer> dominantID;
    private final EntityDataAccessor<Integer> dominatedID;


    public LinkingHandler(T entity, Class<T> clazz, EntityDataAccessor<Integer> dominantID, EntityDataAccessor<Integer> dominatedID) {
        this.entity = entity;
        this.clazz = clazz;
        this.dominantID = dominantID;
        this.dominatedID = dominatedID;
    }

    public void tickLoad() {
        if (entity.level().isClientSide) {
            fetchDominantClient();
            fetchDominatedClient();
        } else {
            // Clear stale references left behind by chunk unloads.
            // We do NOT call removeDominant/removeDominated here — the head entity's
            // consist list owns reconnection; just null out the dead Java reference.
            if (leader.isPresent() && leader.get().isRemoved()) {
                leader = Optional.empty();
            }
            if (follower.isPresent() && follower.get().isRemoved()) {
                follower = Optional.empty();
            }

            if (leader.isEmpty() && dominantNBT != null) {
                tryToLoadFromNBT(dominantNBT).ifPresent(entity::setDominant);
                leader.ifPresent(d -> {
                    d.setDominated(entity);
                    dominantNBT = null; // done loading
                });
            }
            if (follower.isPresent()){
                waitForDominated = false;
                stallNonTicking();
            } else if (waitForDominated) {
                if (entity instanceof StallingCapability s) {
                    s.stall();
                }
            }
            entity.getEntityData().set(dominantID, leader.map(Entity::getId).orElse(-1));
            entity.getEntityData().set(dominatedID, follower.map(Entity::getId).orElse(-1));
        }
    }

    private void stallNonTicking() {
        if (follower.isEmpty()) return;

        boolean managed = entity.getTrain()
                .getTug()
                .filter(tug -> tug instanceof HeadVehicle)
                .map(tug -> VehicleRegistrationData.get(((ServerLevel) entity.level()).getServer())
                    .isManaging((Entity) tug))
                .orElse(false);

        if (!managed && !((ServerLevel) entity.level()).isPositionEntityTicking(follower.get().blockPosition())) {
            if (entity instanceof StallingCapability s) {
                s.stall();
            }
        }
    }

    public void readAdditionalSaveData(CompoundTag compound) {
        dominantNBT = compound.getCompound("dominant");
        waitForDominated = compound.getBoolean("hasChild");
    }

    public void addAdditionalSaveData(CompoundTag compound) {
        if (leader.isPresent()) {
            writeNBT(leader.get(), compound);
        } else if (dominantNBT != null) {
            compound.put(LinkableEntity.LinkSide.DOMINANT.name(), dominantNBT);
        }

        compound.putBoolean("hasChild", follower.isPresent());

    }

    private void writeNBT(Entity entity, CompoundTag globalCompound) {
        CompoundTag compound = new CompoundTag();
        compound.putInt("X", (int) Math.floor(entity.getX()));
        compound.putInt("Y", (int) Math.floor(entity.getY()));
        compound.putInt("Z", (int) Math.floor(entity.getZ()));

        compound.putString("UUID", entity.getUUID().toString());

        globalCompound.put("dominant", compound);
    }

    public static void defineSynchedData(SynchedEntityData.Builder builder, EntityDataAccessor<Integer> dominantID, EntityDataAccessor<Integer> dominatedID) {
        builder.define(dominantID, -1);
        builder.define(dominatedID, -1);
    }

    public void onSyncedDataUpdated(@NotNull EntityDataAccessor<?> key) {

        if (entity.level().isClientSide) {
            if (dominatedID.equals(key) || dominantID.equals(key)) {
                fetchDominantClient();
                fetchDominatedClient();
            }
        }
    }

    private void fetchDominantClient() {
        Entity potential = entity.level().getEntity(entity.getEntityData().get(dominantID));
        if (clazz.isInstance(potential)) {
            leader = Optional.of(clazz.cast(potential));
        } else {
            leader = Optional.empty();
        }
    }

    private Optional<T> tryToLoadFromNBT(CompoundTag compound) {
        try {
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            pos.set(compound.getInt("X"), compound.getInt("Y"), compound.getInt("Z"));
            String uuid = compound.getString("UUID");
            AABB searchBox = new AABB(
                    pos.getX() - 2,
                    pos.getY() - 2,
                    pos.getZ() - 2,
                    pos.getX() + 2,
                    pos.getY() + 2,
                    pos.getZ() + 2
            );
            List<Entity> entities = entity.level().getEntities(entity, searchBox, e -> e.getStringUUID().equals(uuid) && clazz.isInstance(e));
            return entities.stream().findFirst().map(e -> clazz.cast(e));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private void fetchDominatedClient() {
        Entity potential = entity.level().getEntity(entity.getEntityData().get(dominatedID));
        if (clazz.isInstance(potential)) {
            follower = Optional.of((clazz.cast(potential)));
        } else {
            follower = Optional.empty();
        }
    }
}
