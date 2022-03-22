package dev.murad.shipping.entity.custom.train.locomotive;

import dev.murad.shipping.setup.ModEntityTypes;
import dev.murad.shipping.setup.ModItems;
import dev.murad.shipping.util.ItemHandlerVanillaContainerWrapper;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EnergyLocomotiveEntity extends AbstractLocomotiveEntity implements ItemHandlerVanillaContainerWrapper {
    private final ItemStackHandler itemHandler = createHandler();
    private final LazyOptional<IItemHandler> handler = LazyOptional.of(() -> itemHandler);

    private ItemStackHandler createHandler() {
        return new ItemStackHandler(1) {
            @Override
            public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
                return stack.getCapability(CapabilityEnergy.ENERGY).isPresent();
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
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return handler.cast();
        }

        return super.getCapability(cap, side);
    }

    @Override
    public void remove(RemovalReason r) {
        if(!this.level.isClientSide){
            Containers.dropContents(this.level, this, this);
        }
        super.remove(r);
    }

    public EnergyLocomotiveEntity(EntityType<?> type, Level p_38088_) {
        super(type, p_38088_);
    }

    public EnergyLocomotiveEntity(Level level, Double aDouble, Double aDouble1, Double aDouble2) {
        super(ModEntityTypes.ENERGY_LOCOMOTIVE.get(), level, aDouble, aDouble1, aDouble2);
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.ENERGY_LOCOMOTIVE.get());
    }

    @Override
    protected boolean checkMovementAndTickFuel() {
        return engineOn;
    }

    @Override
    public ItemStackHandler getRawHandler() {
        return itemHandler;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        itemHandler.deserializeNBT(compound.getCompound("inv"));
        super.readAdditionalSaveData(compound);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        compound.put("inv", itemHandler.serializeNBT());
        super.addAdditionalSaveData(compound);
    }
}
