package dev.murad.shipping.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;

/**
 * Re-implementation of EnergyStorage so we can read and write it from/to NBT data
 */
public class ReadWriteEnergyStorage implements IEnergyStorage {
    public static final String ENERGY_TAG = "energy";

    private final int maxCapacity, maxReceive, maxExtract;
    private EnergyStorage proxyStorage;

    public ReadWriteEnergyStorage(int maxCapacity, int maxReceive, int maxExtract)
    {
        this.maxCapacity = maxCapacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
        proxyStorage = null;
    }

    private int clampInclusive(int n, int lo, int hi) {
        return Math.max(lo, Math.min(n, hi));
    }

    // when a TileEntity/Item/Entity is created, call this to set it up
    public void setEnergy(int energy) {
        proxyStorage = new EnergyStorage(maxCapacity, maxReceive, maxExtract, clampInclusive(energy, 0, maxCapacity));
    }

    public void readAdditionalSaveData(CompoundTag compound) {
        int energy = compound.contains(ENERGY_TAG) ? compound.getInt(ENERGY_TAG) : 0;
        proxyStorage = new EnergyStorage(maxCapacity, maxReceive, maxExtract, clampInclusive(energy, 0, maxCapacity));
    }

    public void addAdditionalSaveData(CompoundTag compound) {
        compound.putInt(ENERGY_TAG, proxyStorage.getEnergyStored());
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return proxyStorage.receiveEnergy(maxReceive, simulate);
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return proxyStorage.extractEnergy(maxExtract, simulate);
    }

    @Override
    public int getEnergyStored() {
        return proxyStorage.getEnergyStored();
    }

    @Override
    public int getMaxEnergyStored() {
        return proxyStorage.getMaxEnergyStored();
    }

    @Override
    public boolean canExtract() {
        return proxyStorage.canExtract();
    }

    @Override
    public boolean canReceive() {
        return proxyStorage.canReceive();
    }
}
