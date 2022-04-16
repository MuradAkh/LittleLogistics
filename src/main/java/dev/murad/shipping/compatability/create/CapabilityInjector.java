package dev.murad.shipping.compatability.create;

import com.simibubi.create.content.contraptions.components.structureMovement.train.capability.CapabilityMinecartController;
import com.simibubi.create.content.contraptions.components.structureMovement.train.capability.MinecartController;
import dev.murad.shipping.capability.StallingCapability;
import dev.murad.shipping.entity.custom.train.wagon.SeaterCarEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;

public class CapabilityInjector {

    public static class TrainCarController extends MinecartController {
        public static TrainCarController EMPTY;
        private final LazyOptional<StallingCapability> stallingCapability;

        public TrainCarController(SeaterCarEntity entity) {
            super(entity);
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
