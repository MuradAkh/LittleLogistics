package dev.murad.shipping.util;

import dev.murad.shipping.global.PlayerTrainChunkManager;
import lombok.RequiredArgsConstructor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

@RequiredArgsConstructor
public class EnrollmentHandler {
    private static final String UUID_TAG = "EnrollmentHandlerOwner";
    private UUID uuid = null;
    private boolean enrollMe = false;
    private final Entity entity;

    public void tick(){
        if(enrollMe){
            enrollMe = false;
            PlayerTrainChunkManager.enroll(entity, uuid);
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
            enrollMe = true;
        }
    }

    public Enrollment getEnrollment(Player player){
        if(uuid == null) {
            return Enrollment.NOT_ENROLLED;
        } else return player.getUUID().equals(uuid) ? Enrollment.ENROLLED_TO_ME : Enrollment.ENROLLED_TO_X;
    }

    public enum Enrollment {
        NOT_ENROLLED("screen.littlelogistics.not_enrolled"),
        ENROLLED_TO_ME("screen.littlelogistics.enrolled_to_me"),
        ENROLLED_TO_X("screen.littlelogistics.enrolled_to_x");

        Enrollment(String text){
            this.text = text;
        }

        public final String text;
    }
}
