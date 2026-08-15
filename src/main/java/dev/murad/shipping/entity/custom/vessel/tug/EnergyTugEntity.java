package dev.murad.shipping.entity.custom.vessel.tug;

import dev.murad.shipping.ShippingConfig;
import dev.murad.shipping.capability.ReadWriteEnergyStorage;
import dev.murad.shipping.entity.accessor.EnergyHeadVehicleDataAccessor;
import dev.murad.shipping.entity.container.EnergyHeadVehicleContainer;
import dev.murad.shipping.setup.ModEntityTypes;
import dev.murad.shipping.setup.ModItems;
import dev.murad.shipping.util.InventoryUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EnergyTugEntity extends AbstractTugEntity {
    private final ItemStackHandler itemHandler = createHandler();
    private static final int MAX_ENERGY = ShippingConfig.Server.ENERGY_TUG_BASE_CAPACITY.get();
    private static final int MAX_TRANSFER = ShippingConfig.Server.ENERGY_TUG_BASE_MAX_CHARGE_RATE.get();
    private static final int ENERGY_USAGE = ShippingConfig.Server.ENERGY_TUG_BASE_ENERGY_USAGE.get();
    private static final int ENERGY_USAGE_INTERVAL = ShippingConfig.Server.ENERGY_TUG_BASE_ENERGY_USAGE_INTERVAL.get();

    // Counts active (moving) ticks since the last energy drain.
    private int energyUsageTicks = 0;

    private final ReadWriteEnergyStorage internalBattery = new ReadWriteEnergyStorage(MAX_ENERGY, MAX_TRANSFER, Integer.MAX_VALUE);

    public EnergyTugEntity(EntityType<? extends WaterAnimal> type, Level world) {
        super(type, world);
        internalBattery.setEnergy(0);
    }

    public EnergyTugEntity(Level worldIn, double x, double y, double z) {
        super(ModEntityTypes.ENERGY_TUG.get(), worldIn, x, y, z);
        internalBattery.setEnergy(0);
    }

    // todo: Store contents?
    @Override
    public Item getDropItem() {
        return ModItems.ENERGY_TUG.get();
    }

    @Override
    protected MenuProvider createContainerProvider() {
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("screen.littlelogistics.energy_tug");
            }

            @Nullable
            @Override
            public AbstractContainerMenu createMenu(int i, Inventory playerInventory, Player player) {
                return new EnergyHeadVehicleContainer<EnergyTugEntity>(i, level(), getDataAccessor(), playerInventory, player);
            }
        };
    }

    private ItemStackHandler createHandler() {
        return new ItemStackHandler(1) {
            @Override
            public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
                return stack.getCapability(Capabilities.EnergyStorage.ITEM) != null;
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

    @Override
    protected void makeSmoke(){

    }

    // Energy tug can be loaded at all times since there is no concern
    // with mix-ups like with fluids and items
    @Override
    public boolean allowDockInterface(){
        return true;
    }

    @Override
    public EnergyHeadVehicleDataAccessor getDataAccessor() {
        return (EnergyHeadVehicleDataAccessor) new EnergyHeadVehicleDataAccessor.Builder()
                .withEnergy(internalBattery::getEnergyStored)
                .withCapacity(internalBattery::getMaxEnergyStored)
                .withLit(() -> internalBattery.getEnergyStored() > 0) // has energy
                .withId(this.getId())
                .withVisitedSize(this::getVisitedRouteNodeCount)
                .withOn(() -> engineOn)
                .withCanMove(ownership::mayMove)
                .withRouteSize(() -> path != null ? path.size() : 0)
                .build();
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        internalBattery.readAdditionalSaveData(compound.getCompound("energy_storage"));
        if(compound.contains("inv")){
            ItemStackHandler old = new ItemStackHandler();
            old.deserializeNBT(this.registryAccess(), compound.getCompound("inv"));
            itemHandler.setStackInSlot(0, old.getStackInSlot(1));
        }else{
            itemHandler.deserializeNBT(this.registryAccess(), compound.getCompound("tugItemHandler"));
        }
        super.readAdditionalSaveData(compound);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        CompoundTag energyNBT = new CompoundTag();
        internalBattery.addAdditionalSaveData(energyNBT);
        compound.put("energy_storage", energyNBT);
        compound.put("tugItemHandler", itemHandler.serializeNBT(this.registryAccess()));
        super.addAdditionalSaveData(compound);
    }

    @Override
    public void tick() {
        // grab energy from capacitor
        if (!level().isClientSide) {
            IEnergyStorage capability = InventoryUtils.getEnergyCapabilityInSlot(0, itemHandler);
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
        // Only actually drain once every ENERGY_USAGE_INTERVAL active ticks; on the other
        // ticks we still report "has power" so long as the battery isn't empty.
        if (++energyUsageTicks < ENERGY_USAGE_INTERVAL) {
            return internalBattery.getEnergyStored() > 0;
        }
        energyUsageTicks = 0;
        return internalBattery.extractEnergy(ENERGY_USAGE, false) > 0;
    }

    @Override
    public boolean isEmpty() {
        return itemHandler.getStackInSlot(0).isEmpty();
    }

    @Override
    public ItemStack getItem(int p_70301_1_) {
        return itemHandler.getStackInSlot(p_70301_1_);
    }


    @Override
    public void setItem(int p_70299_1_, ItemStack p_70299_2_) {
        if (!this.itemHandler.isItemValid(p_70299_1_, p_70299_2_)){
            return;
        }
        this.itemHandler.insertItem(p_70299_1_, p_70299_2_, false);
        if (!p_70299_2_.isEmpty() && p_70299_2_.getCount() > this.getMaxStackSize()) {
            p_70299_2_.setCount(this.getMaxStackSize());
        }
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public ReadWriteEnergyStorage getInternalBattery() {
        return internalBattery;
    }
}
