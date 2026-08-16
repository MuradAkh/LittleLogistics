package ca.edtoaster.littlecontraptions.entity;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.MountedStorageManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nullable;

/**
 * Live capability wrappers that expose a Create contraption's item and fluid storage
 * through the carrier entity (barge or wagon).
 *
 * Each wrapper holds a reference to the carrier entity and resolves the contraption
 * passenger on every operation. This means assembly, disassembly, and entity removal
 * are all handled transparently without any capability invalidation plumbing:
 *  - No contraption assembled → 0 slots/tanks, all operations are no-ops.
 *  - Contraption assembled    → delegates to Create's MountedStorageManager.
 *  - Carrier entity removed   → the docking station's hasValidOccupant() guard fires first.
 */
public class ContraptionStorageHandler {

    @Nullable
    private static MountedStorageManager getStorage(Entity carrier) {
        for (Entity passenger : carrier.getPassengers()) {
            if (passenger instanceof AbstractContraptionEntity ce) {
                var contraption = ce.getContraption();
                if (contraption != null) {
                    return contraption.getStorage();
                }
            }
        }
        return null;
    }

    // ── Item handler ─────────────────────────────────────────────────────────

    public static class ItemHandler implements IItemHandler {

        private final Entity carrier;

        public ItemHandler(Entity carrier) {
            this.carrier = carrier;
        }

        @Nullable
        private IItemHandler delegate() {
            MountedStorageManager storage = getStorage(carrier);
            return storage != null ? storage.getMountedItems() : null;
        }

        @Override
        public int getSlots() {
            IItemHandler d = delegate();
            return d != null ? d.getSlots() : 0;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            IItemHandler d = delegate();
            return d != null ? d.getStackInSlot(slot) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            IItemHandler d = delegate();
            return d != null ? d.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            IItemHandler d = delegate();
            return d != null ? d.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            IItemHandler d = delegate();
            return d != null ? d.getSlotLimit(slot) : 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            IItemHandler d = delegate();
            return d != null && d.isItemValid(slot, stack);
        }
    }

    // ── Fluid handler ────────────────────────────────────────────────────────

    public static class FluidHandler implements IFluidHandler {

        private final Entity carrier;

        public FluidHandler(Entity carrier) {
            this.carrier = carrier;
        }

        @Nullable
        private IFluidHandler delegate() {
            MountedStorageManager storage = getStorage(carrier);
            return storage != null ? storage.getFluids() : null;
        }

        @Override
        public int getTanks() {
            IFluidHandler d = delegate();
            return d != null ? d.getTanks() : 0;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            IFluidHandler d = delegate();
            return d != null ? d.getFluidInTank(tank) : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            IFluidHandler d = delegate();
            return d != null ? d.getTankCapacity(tank) : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            IFluidHandler d = delegate();
            return d != null && d.isFluidValid(tank, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            IFluidHandler d = delegate();
            return d != null ? d.fill(resource, action) : 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            IFluidHandler d = delegate();
            return d != null ? d.drain(resource, action) : FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            IFluidHandler d = delegate();
            return d != null ? d.drain(maxDrain, action) : FluidStack.EMPTY;
        }
    }
}
