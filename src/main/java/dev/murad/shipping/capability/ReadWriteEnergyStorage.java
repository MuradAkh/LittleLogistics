package dev.murad.shipping.capability;

import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;

/**
 * Re-implementation of EnergyStorage so we can read and write it from/to NBT data
 */
public class ReadWriteEnergyStorage implements IEnergyStorage {
    public static final String ENERGY_FORMAT = "%s_energy";
    public static final String CAPACITY_FORMAT = "%s_capacity";

    private final String energyTag, capacityTag;
    private final int maxReceive, maxExtract;
    private EnergyStorage proxyStorage;

    public ReadWriteEnergyStorage(String ident, int maxReceive, int maxExtract)
    {
        this.energyTag = String.format(ENERGY_FORMAT, ident);
        this.capacityTag = String.format(CAPACITY_FORMAT, ident);
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
        proxyStorage = null;
    }

    // when a TileEntity/Item/Entity is created, call this to set it up
    public void setEnergy(int energy, int capacity) {
        proxyStorage = new EnergyStorage(capacity, maxReceive, maxExtract, energy);
    }

    public void readAdditionalSaveData(CompoundNBT compound) {
        int energy = compound.contains(energyTag) ? compound.getInt(energyTag) : 0;
        int capacity = compound.contains(capacityTag) ? compound.getInt(capacityTag) : 0;
        proxyStorage = new EnergyStorage(capacity, maxReceive, maxExtract, energy);
    }

    public void addAdditionalSaveData(CompoundNBT compound) {
        compound.putInt(energyTag, proxyStorage.getEnergyStored());
        compound.putInt(capacityTag, proxyStorage.getMaxEnergyStored());
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
