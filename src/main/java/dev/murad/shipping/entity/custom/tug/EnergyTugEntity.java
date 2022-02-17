package dev.murad.shipping.entity.custom.tug;

import dev.murad.shipping.ShippingConfig;
import dev.murad.shipping.capability.ReadWriteEnergyStorage;
import dev.murad.shipping.entity.accessor.EnergyTugDataAccessor;
import dev.murad.shipping.entity.container.EnergyTugContainer;
import dev.murad.shipping.setup.ModEntityTypes;
import dev.murad.shipping.setup.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.WaterMobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public class EnergyTugEntity extends AbstractTugEntity {
    private static final int MAX_ENERGY = ShippingConfig.Server.ENERGY_TUG_BASE_CAPACITY.get();
    private static final int MAX_TRANSFER = ShippingConfig.Server.ENERGY_TUG_BASE_MAX_CHARGE_RATE.get();
    private static final int ENERGY_USAGE = ShippingConfig.Server.ENERGY_TUG_BASE_ENERGY_USAGE.get();

    private final ReadWriteEnergyStorage internalBattery = new ReadWriteEnergyStorage(MAX_ENERGY, MAX_TRANSFER, Integer.MAX_VALUE);
    private final LazyOptional<IEnergyStorage> holder = LazyOptional.of(() -> internalBattery);

    public EnergyTugEntity(EntityType<? extends WaterMobEntity> type, World world) {
        super(type, world);
        internalBattery.setEnergy(0);
    }

    public EnergyTugEntity(World worldIn, double x, double y, double z) {
        super(ModEntityTypes.ENERGY_TUG.get(), worldIn, x, y, z);
        internalBattery.setEnergy(0);
    }

    // todo: Store contents?
    @Override
    public Item getDropItem() {
        return ModItems.ENERGY_TUG.get();
    }

    @Override
    protected int getNonRouteItemSlots() {
        return 1; // for capacitor
    }

    @Override
    protected boolean isTugSlotItemValid(int slot, @Nonnull ItemStack stack){
        return slot == 1 && stack.getCapability(CapabilityEnergy.ENERGY).isPresent();
    }

    @Override
    protected int getTugSlotLimit(int slot){
        return 1; // only one capacitor
    }

    @Override
    protected INamedContainerProvider createContainerProvider() {
        return new INamedContainerProvider() {
            @Override
            public ITextComponent getDisplayName() {
                return new TranslationTextComponent("screen.littlelogistics.energy_tug");
            }

            @Nullable
            @Override
            public Container createMenu(int i, PlayerInventory playerInventory, PlayerEntity playerEntity) {
                return new EnergyTugContainer(i, level, getDataAccessor(), playerInventory, playerEntity);
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
    public EnergyTugDataAccessor getDataAccessor() {
        return new EnergyTugDataAccessor.Builder(this.getId())
                .withEnergy(internalBattery::getEnergyStored)
                .withCapacity(internalBattery::getMaxEnergyStored)
                .withLit(() -> internalBattery.getEnergyStored() > 0) // has energy
                .build();
    }

    @Override
    public void readAdditionalSaveData(CompoundNBT compound) {
        internalBattery.readAdditionalSaveData(compound.getCompound("energy_storage"));
        super.readAdditionalSaveData(compound);
    }

    @Override
    public void addAdditionalSaveData(CompoundNBT compound) {
        CompoundNBT energyNBT = new CompoundNBT();
        internalBattery.addAdditionalSaveData(energyNBT);
        compound.put("energy_storage", energyNBT);
        super.addAdditionalSaveData(compound);
    }

    @Nullable
    private IEnergyStorage getEnergyCapabilityInSlot(int slot) {
        ItemStack stack = itemHandler.getStackInSlot(slot);
        if (!stack.isEmpty()) {
            LazyOptional<IEnergyStorage> capabilityLazyOpt = stack.getCapability(CapabilityEnergy.ENERGY);
            if (capabilityLazyOpt.isPresent()) {
                Optional<IEnergyStorage> capabilityOpt = capabilityLazyOpt.resolve();
                if (capabilityOpt.isPresent()) {
                    return capabilityOpt.get();
                }
            }
        }
        return null;
    }

    @Override
    public void tick() {
        // grab energy from capacitor
        if (!level.isClientSide) {
            IEnergyStorage capability = getEnergyCapabilityInSlot(1);
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
    public boolean isEmpty() {
        return false;
    }

    @Override
    public void setItem(int p_70299_1_, ItemStack p_70299_2_) {
        if (!this.itemHandler.isItemValid(1, p_70299_2_)){
            return;
        }
        this.itemHandler.insertItem(1, p_70299_2_, false);
        if (!p_70299_2_.isEmpty() && p_70299_2_.getCount() > this.getMaxStackSize()) {
            p_70299_2_.setCount(this.getMaxStackSize());
        }
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityEnergy.ENERGY) {
            return holder.cast();
        }

        return super.getCapability(cap, side);
    }
}
