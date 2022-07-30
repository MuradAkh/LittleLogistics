package dev.murad.shipping.util;

import com.mojang.authlib.GameProfile;
import dev.murad.shipping.ShippingConfig;
import dev.murad.shipping.global.PlayerTrainChunkManager;
import lombok.RequiredArgsConstructor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class EnrollmentHandler {
    private static final String UUID_TAG = "EnrollmentHandlerOwner";
    private UUID uuid = null;
    private int enrollMe = -1;
    private final Entity entity;

    public void tick(){
        if(enrollMe >= 0){
            if(enrollMe == 0 && !PlayerTrainChunkManager.enroll(entity, uuid)) {
              enrollMe = 100;
            } else {
                enrollMe--;
            }
        }
    }

    public boolean hasOwner(){
        return uuid != null;
    }

    public boolean mayMove(){
        if(uuid == null){
            return true;
        } else if (ShippingConfig.Server.OFFLINE_LOADING.get()){
            return true;
        } else {
            return PlayerTrainChunkManager.get((ServerLevel) entity.level, uuid).isActive() && enrollMe < 0;
        }
    }

    public void enroll(UUID uuid){
        if(PlayerTrainChunkManager.enrollIfAllowed(entity, uuid)){
            this.uuid = uuid;
        }
    }

    public void save(CompoundTag tag){
        if(uuid != null) {
            tag.putUUID(UUID_TAG, uuid);
        }
    }

    public void load(CompoundTag tag){
        if(tag.contains(UUID_TAG)) {
            uuid = tag.getUUID(UUID_TAG);
            enrollMe = 5;
        }
    }

    public Optional<String> getPlayerName(){
        if(uuid == null)
            return Optional.empty();
        else return ((ServerLevel) entity.level).getServer().getProfileCache().get(uuid).map(GameProfile::getName);
    }
}
