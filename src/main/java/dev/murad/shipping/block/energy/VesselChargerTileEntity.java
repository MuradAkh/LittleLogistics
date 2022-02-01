package dev.murad.shipping.block.energy;

import dev.murad.shipping.block.IVesselLoader;
import dev.murad.shipping.capability.ReadWriteEnergyStorage;
import dev.murad.shipping.entity.custom.VesselEntity;
import dev.murad.shipping.setup.ModTileEntitiesTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class VesselChargerTileEntity extends TileEntity implements ITickableTileEntity, IVesselLoader {
    private static final int MAX_RECEIVE = 100;
    private static final int MAX_EXTRACT = 100;
    private static final int MAX_CAPACITY = 10000;
    private final ReadWriteEnergyStorage internalBattery = new ReadWriteEnergyStorage("Internal", MAX_RECEIVE, MAX_EXTRACT);
    private final LazyOptional<IEnergyStorage> holder = LazyOptional.of(() -> internalBattery);
    private int cooldownTime = 0;

    public VesselChargerTileEntity() {
        super(ModTileEntitiesTypes.VESSEL_CHARGER.get());
        internalBattery.setEnergy(0, MAX_CAPACITY);
    }

    @Override
    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction facing) {
        if (capability == CapabilityEnergy.ENERGY)
            return holder.cast();
        return super.getCapability(capability, facing);
    }


    @Override
    public void tick() {
        if (this.level != null && !this.level.isClientSide) {
            --this.cooldownTime;
            if (this.cooldownTime <= 0) {
                this.cooldownTime = tryChargeEntity() ? 0 : 10;
            }
        }
    }


    private boolean tryChargeEntity() {
        return IVesselLoader.getEntityCapability(getBlockPos().relative(getBlockState().getValue(VesselChargerBlock.FACING)),
                CapabilityEnergy.ENERGY, level).map(iEnergyStorage -> {
                    int vesselCap = iEnergyStorage.receiveEnergy(MAX_EXTRACT, true);
                    int toTransfer = internalBattery.extractEnergy(vesselCap, false);
                    return iEnergyStorage.receiveEnergy(toTransfer, false) > 0;
        }).orElse(false);
    }

    @Override
    public boolean holdVessel(VesselEntity vessel, Mode mode) {
        return vessel.getCapability(CapabilityEnergy.ENERGY).map(energyHandler -> {
            switch (mode) {
                case EXPORT:
                    return (energyHandler.getEnergyStored() < energyHandler.getMaxEnergyStored() - 50) && internalBattery.getEnergyStored() > 50;
                default:
                    return false;
            }
        }).orElse(false);
    }

    public void use(PlayerEntity player, Hand hand) {
        player.displayClientMessage(new StringTextComponent(internalBattery.getEnergyStored() + "/" + internalBattery.getMaxEnergyStored() + "FE"), false);
    }
}
