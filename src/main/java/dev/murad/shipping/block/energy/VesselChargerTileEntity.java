package dev.murad.shipping.block.energy;

import dev.murad.shipping.ShippingConfig;
import dev.murad.shipping.block.IVesselLoader;
import dev.murad.shipping.capability.ReadWriteEnergyStorage;
import dev.murad.shipping.setup.ModTileEntitiesTypes;
import dev.murad.shipping.util.LinkableEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class VesselChargerTileEntity extends BlockEntity implements IVesselLoader {
    private static final int MAX_TRANSFER = ShippingConfig.Server.VESSEL_CHARGER_BASE_MAX_TRANSFER.get();
    private static final int MAX_CAPACITY = ShippingConfig.Server.VESSEL_CHARGER_BASE_CAPACITY.get();
    private final ReadWriteEnergyStorage internalBattery = new ReadWriteEnergyStorage(MAX_CAPACITY, MAX_TRANSFER, MAX_TRANSFER);
    private final LazyOptional<IEnergyStorage> holder = LazyOptional.of(() -> internalBattery);
    private int cooldownTime = 0;

    public VesselChargerTileEntity(BlockPos pos, BlockState state) {
        super(ModTileEntitiesTypes.VESSEL_CHARGER.get(), pos, state);
        internalBattery.setEnergy(0);
    }

    @Override
    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction facing) {
        if (capability == CapabilityEnergy.ENERGY)
            return holder.cast();
        return super.getCapability(capability, facing);
    }


    private void serverTickInternal() {
        if (this.level != null) {
            --this.cooldownTime;
            if (this.cooldownTime <= 0) {
                this.cooldownTime = tryChargeEntity() ? 0 : 10;
            }
        }
    }

    public static void serverTick(Level pLevel, BlockPos pPos, BlockState pState, VesselChargerTileEntity e) {
        e.serverTickInternal();
    }


    private boolean tryChargeEntity() {
        return IVesselLoader.getEntityCapability(getBlockPos().relative(getBlockState().getValue(VesselChargerBlock.FACING)),
                CapabilityEnergy.ENERGY, level).map(iEnergyStorage -> {
                    int vesselCap = iEnergyStorage.receiveEnergy(MAX_TRANSFER, true);
                    int toTransfer = internalBattery.extractEnergy(vesselCap, false);
                    return iEnergyStorage.receiveEnergy(toTransfer, false) > 0;
        }).orElse(false);
    }

    @Override
    public void load(CompoundTag compound) {
        super.load(compound);
        internalBattery.readAdditionalSaveData(compound.getCompound("energy_storage"));
    }

    @Override
    public void saveAdditional(CompoundTag compound) {
        CompoundTag energyNBT = new CompoundTag();
        internalBattery.addAdditionalSaveData(energyNBT);
        super.saveAdditional(compound);
        compound.put("energy_storage", energyNBT);
    }

    @Override
    public<T extends Entity & LinkableEntity<T>> boolean hold(T vehicle, Mode mode) {
        return vehicle.getCapability(CapabilityEnergy.ENERGY).map(energyHandler -> {
            switch (mode) {
                case EXPORT:
                    return (energyHandler.getEnergyStored() < energyHandler.getMaxEnergyStored() - 50) && internalBattery.getEnergyStored() > 50;
                default:
                    return false;
            }
        }).orElse(false);
    }

    public void use(Player player, InteractionHand hand) {
        player.displayClientMessage(Component.translatable("block.littlelogistics.vessel_charger.capacity",
                internalBattery.getEnergyStored(), internalBattery.getMaxEnergyStored()), false);
    }
}
