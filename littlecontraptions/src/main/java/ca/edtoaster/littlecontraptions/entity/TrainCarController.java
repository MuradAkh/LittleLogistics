package ca.edtoaster.littlecontraptions.entity;

import com.simibubi.create.content.contraptions.minecart.capability.MinecartController;
import dev.murad.shipping.capability.StallingCapability;
import dev.murad.shipping.entity.custom.train.wagon.AbstractWagonEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.UUID;

/**
 * Compat layer: wires Create's minecart-controller stall signal into a wagon's StallingCapability.
 *
 * Installed as {@code AllAttachmentTypes.MINECART_CONTROLLER} on every {@link AbstractWagonEntity}
 * by {@link ca.edtoaster.littlecontraptions.event.LCForgeEvents}. Mirrors {@link BargeController}
 * for the rail vehicle side.
 */
public class TrainCarController extends MinecartController {

    private final WeakReference<AbstractWagonEntity> weakRef;
    @Nullable
    private final StallingCapability stallingCapability;

    public TrainCarController(AbstractWagonEntity entity) {
        super(null);
        weakRef = new WeakReference<>(entity);
        stallingCapability = entity;
    }

    @Override
    public void tick() {}

    @Override
    public boolean isFullyCoupled() { return false; }

    @Override
    public boolean isLeadingCoupling() { return false; }

    @Override
    public boolean isConnectedToCoupling() { return false; }

    @Override
    public boolean isCoupledThroughContraption() { return false; }

    @Override
    public boolean hasContraptionCoupling(boolean current) { return false; }

    @Override
    public float getCouplingLength(boolean leading) { return 0; }

    @Override
    public void decouple() {}

    @Override
    public void removeConnection(boolean main) {}

    @Override
    public void prepareForCoupling(boolean isLeading) {}

    @Override
    public void coupleWith(boolean isLeading, UUID coupled, float length, boolean contraption) {}

    @Nullable
    @Override
    public UUID getCoupledCart(boolean asMain) { return null; }

    @Override
    public boolean isStalled() {
        return stallingCapability != null && stallingCapability.isFrozen();
    }

    @Override
    public void setStalledExternally(boolean stall) {
        if (stallingCapability == null) return;
        if (stall) {
            stallingCapability.freeze();
        } else {
            stallingCapability.unfreeze();
        }
    }

    @Override
    public void sendData() {}

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        return new CompoundTag();
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {}

    @Override
    public boolean isPresent() {
        AbstractWagonEntity car = weakRef.get();
        return car != null && car.isAlive();
    }
}
