package dev.murad.shipping.block.dock;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Wrapper around a vehicle's {@link IFluidHandler} that always exists on the dock.
 * When no vehicle is docked the handler reports zero tanks and rejects all operations.
 * When a vehicle docks, {@link #connect(IFluidHandler)} is called so external pipes
 * transparently interact with the vehicle's fluid tank without re-caching.
 */
public class DockFluidHandler implements IFluidHandler {

    private final DockBlockEntity dock;
    @Nullable
    private IFluidHandler delegate;

    public DockFluidHandler(DockBlockEntity dock) {
        this.dock = dock;
    }

    public void connect(IFluidHandler handler) {
        this.delegate = handler;
    }

    public void disconnect() {
        this.delegate = null;
    }

    @Override
    public int getTanks() {
        return delegate != null ? delegate.getTanks() : 0;
    }

    @Nonnull
    @Override
    public FluidStack getFluidInTank(int tank) {
        return delegate != null ? delegate.getFluidInTank(tank) : FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        return delegate != null ? delegate.getTankCapacity(tank) : 0;
    }

    @Override
    public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
        return delegate != null && delegate.isFluidValid(tank, stack);
    }

    @Override
    public int fill(@Nonnull FluidStack resource, FluidAction action) {
        if (delegate == null) return 0;
        int filled = delegate.fill(resource, action);
        if (action == FluidAction.EXECUTE && filled > 0) {
            dock.notifyTransfer();
        }
        return filled;
    }

    @Nonnull
    @Override
    public FluidStack drain(@Nonnull FluidStack resource, FluidAction action) {
        if (delegate == null) return FluidStack.EMPTY;
        FluidStack drained = delegate.drain(resource, action);
        if (action == FluidAction.EXECUTE && !drained.isEmpty()) {
            dock.notifyTransfer();
        }
        return drained;
    }

    @Nonnull
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (delegate == null) return FluidStack.EMPTY;
        FluidStack drained = delegate.drain(maxDrain, action);
        if (action == FluidAction.EXECUTE && !drained.isEmpty()) {
            dock.notifyTransfer();
        }
        return drained;
    }
}
