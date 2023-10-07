package dev.murad.shipping.entity.custom.train.locomotive;

import dev.murad.shipping.ShippingConfig;
import dev.murad.shipping.capability.ReadWriteEnergyStorage;
import dev.murad.shipping.entity.accessor.EnergyHeadVehicleDataAccessor;
import dev.murad.shipping.entity.container.EnergyHeadVehicleContainer;
import dev.murad.shipping.setup.ModEntityTypes;
import dev.murad.shipping.setup.ModItems;
import dev.murad.shipping.util.InventoryUtils;
import dev.murad.shipping.util.ItemHandlerVanillaContainerWrapper;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EnergyLocomotiveEntity extends AbstractLocomotiveEntity implements ItemHandlerVanillaContainerWrapper, WorldlyContainer {
    private final ItemStackHandler energyItemHandler = createHandler();
    private final LazyOptional<IItemHandler> energyItemHandlerOpt = LazyOptional.of(() -> energyItemHandler);
    private static final int MAX_ENERGY = ShippingConfig.Server.ENERGY_LOCO_BASE_CAPACITY.get();
    private static final int MAX_TRANSFER = ShippingConfig.Server.ENERGY_LOCO_BASE_MAX_CHARGE_RATE.get();
    private static final int ENERGY_USAGE = ShippingConfig.Server.ENERGY_LOCO_BASE_ENERGY_USAGE.get();

    private final ReadWriteEnergyStorage internalBattery = new ReadWriteEnergyStorage(MAX_ENERGY, MAX_TRANSFER, Integer.MAX_VALUE);
    private final LazyOptional<IEnergyStorage> internalBatteryOpt = LazyOptional.of(() -> internalBattery);

    public EnergyLocomotiveEntity(EntityType<?> type, Level level) {
        super(type, level);
        internalBattery.setEnergy(0);
    }

    public EnergyLocomotiveEntity(Level level, Double x, Double y, Double z) {
        super(ModEntityTypes.ENERGY_LOCOMOTIVE.get(), level, x, y, z);
        internalBattery.setEnergy(0);
    }

    private ItemStackHandler createHandler() {
        return new ItemStackHandler() {
            @Override
            public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
                return stack.getCapability(ForgeCapabilities.ENERGY).isPresent();
            }

            @Nonnull
            @Override
            public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
                if (!isItemValid(slot, stack)) {
                    return stack;
                }

                return super.insertItem(slot, stack, simulate);
            }
        };
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return energyItemHandlerOpt.cast();
        } else if (cap == ForgeCapabilities.ENERGY) {
            return internalBatteryOpt.cast();
        }

        return super.getCapability(cap, side);
    }

    @Override
    public void remove(RemovalReason r) {
        if(!this.level().isClientSide){
            Containers.dropContents(this.level(), this, this);
        }
        super.remove(r);
    }

    @Override
    protected MenuProvider createContainerProvider() {
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("entity.littlelogistics.energy_locomotive");
            }

            @Override
            public AbstractContainerMenu createMenu(int i, @NotNull Inventory playerInventory, @NotNull Player player) {
                return new EnergyHeadVehicleContainer<EnergyLocomotiveEntity>(i, level(), getDataAccessor(), playerInventory, player);
            }
        };
    }

    @Override
    public EnergyHeadVehicleDataAccessor getDataAccessor() {
        return (EnergyHeadVehicleDataAccessor) new EnergyHeadVehicleDataAccessor.Builder()
                .withEnergy(internalBattery::getEnergyStored)
                .withCapacity(internalBattery::getMaxEnergyStored)
                .withLit(() -> internalBattery.getEnergyStored() > 0) // has energy
                .withId(this.getId())
                .withOn(() -> engineOn)
                .withRouteSize(() -> navigator.getRouteSize())
                .withVisitedSize(() -> navigator.getVisitedSize())
                .withCanMove(enrollmentHandler::mayMove)
                .build();
    }

    @Override
    public void tick() {
        // grab energy from capacitor
        if (!level().isClientSide) {
            IEnergyStorage capability = InventoryUtils.getEnergyCapabilityInSlot(0, energyItemHandler);
            if (capability != null) {
                // simulate first
                int toExtract = capability.extractEnergy(MAX_TRANSFER, true);
                toExtract = internalBattery.receiveEnergy(toExtract, false);
                capability.extractEnergy(toExtract, false);
            }
        }

        super.tick();
    }

    @Override
    protected boolean tickFuel() {
        return internalBattery.extractEnergy(ENERGY_USAGE, false) > 0;
    }


    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.ENERGY_LOCOMOTIVE.get());
    }

    @Override
    public ItemStackHandler getRawHandler() {
        return energyItemHandler;
    }

    @Override
    public int @NotNull [] getSlotsForFace(@NotNull Direction dir) {
        return new int[]{0};
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, @NotNull ItemStack itemStack, @Nullable Direction dir) {
        return stalling.isDocked();
    }

    @Override
    public boolean canTakeItemThroughFace(int index, @NotNull ItemStack itemStack, @NotNull Direction dir) {
        return false;
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        energyItemHandler.deserializeNBT(compound.getCompound("inv"));
        internalBattery.readAdditionalSaveData(compound.getCompound("energy_storage"));
        super.readAdditionalSaveData(compound);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        compound.put("inv", energyItemHandler.serializeNBT());
        CompoundTag energyNBT = new CompoundTag();
        internalBattery.addAdditionalSaveData(energyNBT);
        compound.put("energy_storage", energyNBT);
        super.addAdditionalSaveData(compound);
    }
}
