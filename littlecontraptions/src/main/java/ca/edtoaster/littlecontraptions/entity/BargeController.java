package ca.edtoaster.littlecontraptions.entity;

import com.simibubi.create.content.contraptions.minecart.capability.MinecartController;
import dev.murad.shipping.capability.StallingCapability;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.UUID;

/**
 * Compat layer with Create's Minecart Controller.
 *
 * Create 6.0 no longer exposes the minecart controller as a Forge capability. The mounted
 * contraption tick instead reads it as the {@code AllAttachmentTypes.MINECART_CONTROLLER} data
 * attachment off its carrier entity and pushes its stall state onto it via
 * {@link #setStalledExternally(boolean)}. {@link ContraptionBargeEntity} installs this bridge as
 * that attachment so the barge's {@link StallingCapability} stays wired up to the contraption.
 */
public class BargeController extends MinecartController {

    public static BargeController EMPTY;
    private final WeakReference<ContraptionBargeEntity> weakRef;
    @Nullable
    private final StallingCapability stallingCapability;

    public BargeController(ContraptionBargeEntity entity) {
        // MinecartController is bound to an AbstractMinecart in 6.0; a barge is not one, so we pass
        // null. The base class only stores it in a WeakReference, and Create never calls cart() on
        // a controller obtained through the mounted-contraption attachment path.
        super(null);
        weakRef = new WeakReference<>(entity);
        // AbstractBargeEntity implements StallingCapability directly in 1.21.1, so the entity IS the cap.
        stallingCapability = entity;
    }

    @Override
    public void tick() {
    }

    @Override
    public boolean isFullyCoupled() {
        return false;
    }

    @Override
    public boolean isLeadingCoupling() {
        return false;
    }

    @Override
    public boolean isConnectedToCoupling() {
        return false;
    }

    @Override
    public boolean isCoupledThroughContraption() {
        return false;
    }

    @Override
    public boolean hasContraptionCoupling(boolean current) {
        return false;
    }

    @Override
    public float getCouplingLength(boolean leading) {
        return 0;
    }

    @Override
    public void decouple() {
    }

    @Override
    public void removeConnection(boolean main) {
    }

    @Override
    public void prepareForCoupling(boolean isLeading) {
    }

    @Override
    public void coupleWith(boolean isLeading, UUID coupled, float length, boolean contraption) {
    }

    @Nullable
    @Override
    public UUID getCoupledCart(boolean asMain) {
        return null;
    }

    @Override
    public boolean isStalled() {
        return stallingCapability != null && stallingCapability.isFrozen();
    }

    @Override
    public void setStalledExternally(boolean stall) {
        if (stallingCapability == null) {
            return;
        }
        if (stall) {
            stallingCapability.freeze();
        } else {
            stallingCapability.unfreeze();
        }
    }

    @Override
    public void sendData() {
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        return new CompoundTag();
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
    }

    @Override
    public boolean isPresent() {
        return weakRef.get() != null && barge().isAlive();
    }

    public ContraptionBargeEntity barge() {
        return weakRef.get();
    }

    public static BargeController empty() {
        return EMPTY != null ? EMPTY : (EMPTY = new BargeController(null));
    }
}
