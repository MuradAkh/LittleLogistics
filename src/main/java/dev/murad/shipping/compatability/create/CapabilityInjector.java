package dev.murad.shipping.compatability.create;

import com.simibubi.create.content.contraptions.components.structureMovement.train.capability.CapabilityMinecartController;
import com.simibubi.create.content.contraptions.components.structureMovement.train.capability.MinecartController;
import dev.murad.shipping.capability.StallingCapability;
import dev.murad.shipping.entity.custom.train.wagon.SeaterCarEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.UUID;

public class CapabilityInjector {

    public static class TrainCarController extends MinecartController {
        public static TrainCarController EMPTY;
        private final WeakReference<SeaterCarEntity> weakRef;
        private final LazyOptional<StallingCapability> stallingCapability;

        public TrainCarController(SeaterCarEntity entity) {
            super(entity);
            weakRef = new WeakReference<>(entity);
            stallingCapability = entity == null ? LazyOptional.empty() : entity.getCapability(StallingCapability.STALLING_CAPABILITY);
        }
        public boolean isStalled() {
            return stallingCapability.map(StallingCapability::isFrozen).orElse(false);
        }

        public void setStalledExternally(boolean stall) {
            stallingCapability.ifPresent(cap -> {
                if (stall) {
                    cap.freeze();
                } else {
                    cap.unfreeze();
                }
            });
        }

        public boolean isPresent() {
            return weakRef.get() != null && barge().isAlive();
        }

        public SeaterCarEntity barge() {
            return weakRef.get();
        }

        public static TrainCarController empty() {
            return EMPTY != null ? EMPTY : (EMPTY = new TrainCarController(null));
        }
    }

    public static LazyOptional<?> constructMinecartControllerCapability(SeaterCarEntity entity) {
        return LazyOptional.of(() -> new TrainCarController(entity));
    }

    public static <T> boolean isMinecartControllerCapability(@NotNull Capability<T> cap) {
        return cap == CapabilityMinecartController.MINECART_CONTROLLER_CAPABILITY;
    }
}
