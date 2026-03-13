package dev.murad.shipping.compatibility.create;

import com.simibubi.create.content.contraptions.minecart.capability.CapabilityMinecartController;
import com.simibubi.create.content.contraptions.minecart.capability.MinecartController;
import dev.murad.shipping.capability.StallingCapability;
import dev.murad.shipping.entity.custom.train.wagon.SeaterCarEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;

public class CapabilityInjector {

    public static class TrainCarController extends MinecartController {
        public static TrainCarController EMPTY;
        private final StallingCapability stallingCapability;

        public TrainCarController(SeaterCarEntity entity) {
            super(entity);
            stallingCapability = entity;
        }

        public boolean isStalled() {
            return stallingCapability != null && stallingCapability.isFrozen();
        }

        public void setStalledExternally(boolean stall) {
            if (stallingCapability != null) {
                if (stall) {
                    stallingCapability.freeze();
                } else {
                    stallingCapability.unfreeze();
                }
            }
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
