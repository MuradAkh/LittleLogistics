package dev.murad.shipping.util;

import dev.murad.shipping.global.VehicleRegistrationData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/** Persistent owner metadata for an automatically registered head vehicle. */
public final class VehicleOwnership {
    private static final String OWNER_TAG = "VehicleOwner";

    private final Entity entity;
    @Nullable private UUID owner;

    public VehicleOwnership(Entity entity) {
        this.entity = entity;
    }

    public void assign(UUID owner) {
        this.owner = owner;
        if (entity.level() instanceof ServerLevel level) {
            VehicleRegistrationData.get(level.getServer()).register(entity, owner);
        }
    }

    public void tick() {
        if (owner != null && entity.level() instanceof ServerLevel level) {
            VehicleRegistrationData.get(level.getServer()).register(entity, owner);
        }
    }

    public boolean mayMove() {
        if (owner == null || !(entity.level() instanceof ServerLevel level)) {
            return true;
        }
        return VehicleRegistrationData.get(level.getServer()).mayVehicleMove(entity, owner);
    }

    public boolean hasOwner() {
        return owner != null;
    }

    public Optional<UUID> owner() {
        return Optional.ofNullable(owner);
    }

    public void save(CompoundTag tag) {
        if (owner != null) {
            tag.putUUID(OWNER_TAG, owner);
        }
    }

    public void load(CompoundTag tag) {
        owner = tag.contains(OWNER_TAG) ? tag.getUUID(OWNER_TAG) : null;
    }
}
