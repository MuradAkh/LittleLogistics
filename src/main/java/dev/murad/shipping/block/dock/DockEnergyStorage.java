package dev.murad.shipping.block.dock;

import net.neoforged.neoforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

/**
 * Wrapper around a vehicle's {@link IEnergyStorage} that always exists on the dock.
 * When no vehicle is docked the storage reports zero capacity and rejects all operations.
 * When a vehicle docks, {@link #connect(IEnergyStorage)} is called so external energy
 * networks transparently interact with the vehicle's battery without re-caching.
 */
public class DockEnergyStorage implements IEnergyStorage {

    private final DockBlockEntity dock;
    @Nullable
    private IEnergyStorage delegate;

    public DockEnergyStorage(DockBlockEntity dock) {
        this.dock = dock;
    }

    public void connect(IEnergyStorage storage) {
        this.delegate = storage;
    }

    public void disconnect() {
        this.delegate = null;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (delegate == null) return 0;
        int received = delegate.receiveEnergy(maxReceive, simulate);
        if (!simulate && received > 0) {
            dock.notifyTransfer();
        }
        return received;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        if (delegate == null) return 0;
        int extracted = delegate.extractEnergy(maxExtract, simulate);
        if (!simulate && extracted > 0) {
            dock.notifyTransfer();
        }
        return extracted;
    }

    @Override
    public int getEnergyStored() {
        return delegate != null ? delegate.getEnergyStored() : 0;
    }

    @Override
    public int getMaxEnergyStored() {
        return delegate != null ? delegate.getMaxEnergyStored() : 0;
    }

    @Override
    public boolean canExtract() {
        return delegate != null && delegate.canExtract();
    }

    @Override
    public boolean canReceive() {
        return delegate != null && delegate.canReceive();
    }
}
