package dev.murad.shipping.block.dock;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nullable;

/**
 * Wrapper around a vehicle's {@link IItemHandler} that always exists on the dock.
 * When no vehicle is docked the handler reports zero slots and rejects all operations.
 * When a vehicle docks, {@link #connect(IItemHandler)} is called so external pipes
 * transparently interact with the vehicle's inventory without re-caching.
 */
public class DockItemHandler implements IItemHandler {

    private final DockBlockEntity dock;
    @Nullable
    private IItemHandler delegate;

    public DockItemHandler(DockBlockEntity dock) {
        this.dock = dock;
    }

    public void connect(IItemHandler handler) {
        this.delegate = handler;
    }

    public void disconnect() {
        this.delegate = null;
    }

    @Override
    public int getSlots() {
        return delegate != null ? delegate.getSlots() : 0;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return delegate != null ? delegate.getStackInSlot(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (delegate == null) return stack;
        ItemStack result = delegate.insertItem(slot, stack, simulate);
        if (!simulate && result.getCount() != stack.getCount()) {
            dock.notifyTransfer();
        }
        return result;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (delegate == null) return ItemStack.EMPTY;
        ItemStack result = delegate.extractItem(slot, amount, simulate);
        if (!simulate && !result.isEmpty()) {
            dock.notifyTransfer();
        }
        return result;
    }

    @Override
    public int getSlotLimit(int slot) {
        return delegate != null ? delegate.getSlotLimit(slot) : 0;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return delegate != null && delegate.isItemValid(slot, stack);
    }
}
