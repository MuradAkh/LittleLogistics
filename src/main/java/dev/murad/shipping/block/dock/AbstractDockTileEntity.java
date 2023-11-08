package dev.murad.shipping.block.dock;

import dev.murad.shipping.util.LinkableEntity;
import net.minecraft.client.renderer.entity.EndermanRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;


public abstract class AbstractDockTileEntity<T extends Entity & LinkableEntity<T>> extends BlockEntity {

    private final DockedEntity<T> dockedEntity;
    private final LazyOptional<IItemHandler> delegateItemHandler;
    private final LazyOptional<IEnergyStorage> delegateEnergyStorage;
    private final LazyOptional<IFluidHandler> delegateFluidHandler;

    public AbstractDockTileEntity(BlockEntityType<?> blockEntityType, BlockPos pos, BlockState s) {
        super(blockEntityType, pos, s);

        dockedEntity = DockedEntity.empty();

        delegateItemHandler = LazyOptional.of(() -> new IItemHandler() {
            @Override
            public int getSlots() {
                return Optional.ofNullable(dockedEntity.getEntityItemHandler())
                        .map(IItemHandler::getSlots)
                        .orElse(0);
            }

            @Override
            public @NotNull ItemStack getStackInSlot(int slot) {
                return Optional.ofNullable(dockedEntity.getEntityItemHandler())
                        .map(h -> h.getStackInSlot(slot))
                        .orElse(ItemStack.EMPTY);
            }

            @Override
            public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                return Optional.ofNullable(dockedEntity.getEntityItemHandler())
                        .map(h -> h.insertItem(slot, stack, simulate))
                        .orElse(stack);
            }

            @Override
            public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                return Optional.ofNullable(dockedEntity.getEntityItemHandler())
                        .map(h -> h.extractItem(slot, amount, simulate))
                        .orElse(ItemStack.EMPTY);
            }

            @Override
            public int getSlotLimit(int slot) {
                return Optional.ofNullable(dockedEntity.getEntityItemHandler())
                        .map(h -> h.getSlotLimit(slot))
                        .orElse(0);
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return Optional.ofNullable(dockedEntity.getEntityItemHandler())
                        .map(h -> h.isItemValid(slot, stack))
                        .orElse(false);
            }
        });

        delegateEnergyStorage = LazyOptional.of(() -> new IEnergyStorage() {
            @Override
            public int receiveEnergy(int maxReceive, boolean simulate) {
                return Optional.ofNullable(dockedEntity.getEntityEnergyStorage())
                        .map(h -> h.receiveEnergy(maxReceive, simulate))
                        .orElse(0);
            }

            @Override
            public int extractEnergy(int maxExtract, boolean simulate) {
                return Optional.ofNullable(dockedEntity.getEntityEnergyStorage())
                        .map(h -> h.extractEnergy(maxExtract, simulate))
                        .orElse(0);
            }

            @Override
            public int getEnergyStored() {
                return Optional.ofNullable(dockedEntity.getEntityEnergyStorage())
                        .map(IEnergyStorage::getEnergyStored)
                        .orElse(0);
            }

            @Override
            public int getMaxEnergyStored() {
                return Optional.ofNullable(dockedEntity.getEntityEnergyStorage())
                        .map(IEnergyStorage::getMaxEnergyStored)
                        .orElse(0);
            }

            @Override
            public boolean canExtract() {
                return Optional.ofNullable(dockedEntity.getEntityEnergyStorage())
                        .map(IEnergyStorage::canExtract)
                        .orElse(false);
            }

            @Override
            public boolean canReceive() {
                return Optional.ofNullable(dockedEntity.getEntityEnergyStorage())
                        .map(IEnergyStorage::canReceive)
                        .orElse(false);
            }
        });

        delegateFluidHandler = LazyOptional.of(() -> new IFluidHandler() {
            @Override
            public int getTanks() {
                return Optional.ofNullable(dockedEntity.getEntityFluidHandler())
                        .map(IFluidHandler::getTanks)
                        .orElse(0);
            }

            @Override
            public @NotNull FluidStack getFluidInTank(int tank) {
                return Optional.ofNullable(dockedEntity.getEntityFluidHandler())
                        .map(h -> h.getFluidInTank(tank))
                        .orElse(FluidStack.EMPTY);
            }

            @Override
            public int getTankCapacity(int tank) {
                return Optional.ofNullable(dockedEntity.getEntityFluidHandler())
                        .map(h -> h.getTankCapacity(tank))
                        .orElse(0);
            }

            @Override
            public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
                return Optional.ofNullable(dockedEntity.getEntityFluidHandler())
                        .map(h -> h.isFluidValid(tank, stack))
                        .orElse(false);
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                return Optional.ofNullable(dockedEntity.getEntityFluidHandler())
                        .map(h -> h.fill(resource, action))
                        .orElse(0);
            }

            @Override
            public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
                return Optional.ofNullable(dockedEntity.getEntityFluidHandler())
                        .map(h -> h.drain(resource, action))
                        .orElse(FluidStack.EMPTY);
            }

            @Override
            public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
                return Optional.ofNullable(dockedEntity.getEntityFluidHandler())
                        .map(h -> h.drain(maxDrain, action))
                        .orElse(FluidStack.EMPTY);
            }
        });
    }

    protected abstract boolean shouldHoldEntity(T entity, Direction direction);

    public void dockEntity(T entity) {
        dockedEntity.dockEntity(entity);
    }

    public void undockEntity() {
        dockedEntity.undockEntity();
    }

    @Override
    public @NotNull <C> LazyOptional<C> getCapability(@NotNull Capability<C> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return delegateItemHandler.cast();
        } else if (cap == ForgeCapabilities.ENERGY) {
            return delegateEnergyStorage.cast();
        } else if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return delegateFluidHandler.cast();
        }

        return super.getCapability(cap, side);
    }

    protected abstract boolean canDockFacingDirection(T tug, Direction direction);
}
