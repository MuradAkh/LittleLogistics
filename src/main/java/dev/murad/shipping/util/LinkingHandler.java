package dev.murad.shipping.util;

import dev.murad.shipping.capability.StallingCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
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

    public Optional<T> dominant = Optional.empty();
    public Optional<T> dominated = Optional.empty();
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
        if (entity.level.isClientSide) {
            fetchDominantClient();
            fetchDominatedClient();
        } else {
            if (dominant.isEmpty() && dominantNBT != null) {
                tryToLoadFromNBT(dominantNBT).ifPresent(entity::setDominant);
                dominant.ifPresent(d -> {
                    d.setDominated(entity);
                    dominantNBT = null; // done loading
                });
            }
            if (dominated.isPresent()){
                waitForDominated = false;
                if(!((ServerLevel) entity.level).isPositionEntityTicking(dominated.get().blockPosition())){
                    entity.getCapability(StallingCapability.STALLING_CAPABILITY).ifPresent(StallingCapability::stall);
                }
            } else if (waitForDominated) {
                entity.getCapability(StallingCapability.STALLING_CAPABILITY).ifPresent(StallingCapability::stall);
            }
            entity.getEntityData().set(dominantID, dominant.map(Entity::getId).orElse(-1));
            entity.getEntityData().set(dominatedID, dominated.map(Entity::getId).orElse(-1));
        }
    }

    public void readAdditionalSaveData(CompoundTag compound) {
        dominantNBT = compound.getCompound("dominant");
        waitForDominated = compound.getBoolean("hasChild");
    }

    public void addAdditionalSaveData(CompoundTag compound) {
        if (dominant.isPresent()) {
            writeNBT(dominant.get(), compound);
        } else if (dominantNBT != null) {
            compound.put(LinkableEntity.LinkSide.DOMINANT.name(), dominantNBT);
        }

        compound.putBoolean("hasChild", dominated.isPresent());

    }

    private void writeNBT(Entity entity, CompoundTag globalCompound) {
        CompoundTag compound = new CompoundTag();
        compound.putInt("X", (int) Math.floor(entity.getX()));
        compound.putInt("Y", (int) Math.floor(entity.getY()));
        compound.putInt("Z", (int) Math.floor(entity.getZ()));

        compound.putString("UUID", entity.getUUID().toString());

        globalCompound.put("dominant", compound);
    }

    public static void defineSynchedData(Entity entity, EntityDataAccessor<Integer> dominantID, EntityDataAccessor<Integer> dominatedID) {
        entity.getEntityData().define(dominantID, -1);
        entity.getEntityData().define(dominatedID, -1);
    }

    public void onSyncedDataUpdated(@NotNull EntityDataAccessor<?> key) {

        if (entity.level.isClientSide) {
            if (dominatedID.equals(key) || dominantID.equals(key)) {
                fetchDominantClient();
                fetchDominatedClient();
            }
        }
    }

    private void fetchDominantClient() {
        Entity potential = entity.level.getEntity(entity.getEntityData().get(dominantID));
        if (clazz.isInstance(potential)) {
            dominant = Optional.of(clazz.cast(potential));
        } else {
            dominant = Optional.empty();
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
            List<Entity> entities = entity.level.getEntities(entity, searchBox, e -> e.getStringUUID().equals(uuid) && clazz.isInstance(e));
            return entities.stream().findFirst().map(e -> clazz.cast(e));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private void fetchDominatedClient() {
        Entity potential = entity.level.getEntity(entity.getEntityData().get(dominatedID));
        if (clazz.isInstance(potential)) {
            dominated = Optional.of((clazz.cast(potential)));
        } else {
            dominated = Optional.empty();
        }
    }
}
