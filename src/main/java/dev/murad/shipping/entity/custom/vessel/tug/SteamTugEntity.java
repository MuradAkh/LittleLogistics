package dev.murad.shipping.entity.custom.vessel.tug;

import dev.murad.shipping.ShippingConfig;
import dev.murad.shipping.entity.accessor.SteamHeadVehicleDataAccessor;
import dev.murad.shipping.entity.container.SteamHeadVehicleContainer;
import dev.murad.shipping.setup.ModEntityTypes;
import dev.murad.shipping.setup.ModItems;
import dev.murad.shipping.setup.ModSounds;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SteamTugEntity extends AbstractTugEntity {
    private static final int FURNACE_FUEL_MULTIPLIER= ShippingConfig.Server.STEAM_TUG_FUEL_MULTIPLIER.get();
    private final ItemStackHandler itemHandler = createHandler();
    private final LazyOptional<IItemHandler> handler = LazyOptional.of(() -> itemHandler);
    protected int burnTime = 0;
    protected int burnCapacity = 0;

    public SteamTugEntity(EntityType<? extends WaterAnimal> type, Level world) {
        super(type, world);
    }

    public SteamTugEntity(Level worldIn, double x, double y, double z) {
        super(ModEntityTypes.STEAM_TUG.get(), worldIn, x, y, z);
    }

    private ItemStackHandler createHandler() {
        return new ItemStackHandler(1) {
            @Override
            public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
                return FurnaceBlockEntity.isFuel(stack);
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
    protected MenuProvider createContainerProvider() {
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("screen.littlelogistics.tug");
            }

            @Nullable
            @Override
            public AbstractContainerMenu createMenu(int i, Inventory playerInventory, Player Player) {
                return new SteamHeadVehicleContainer<SteamTugEntity>(i, level, getDataAccessor(), playerInventory, Player);
            }
        };
    }

    public int getBurnProgress() {
        int i = burnCapacity;
        if (i == 0) {
            i = 200;
        }

        return burnTime * 13 / i;
    }

    // CONTAINER STUFF
    public boolean isLit() {
        return burnTime > 0;
    }

    @Override
    public SteamHeadVehicleDataAccessor getDataAccessor() {
        return new SteamHeadVehicleDataAccessor.Builder(this.getId())
                .withBurnProgress(this::getBurnProgress)
                .withLit(this::isLit)
                .withVisitedSize(() -> nextStop)
                .withOn(() -> engineOn)
                .withRouteSize(() -> path != null ? path.size() : 0)
                .build();
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
    protected boolean tickFuel() {
        if (burnTime > 0) {
            burnTime--;
            return true;
        } else {
            ItemStack stack = itemHandler.getStackInSlot(0);
            if (!stack.isEmpty()) {
                burnCapacity = (ForgeHooks.getBurnTime(stack, null) * FURNACE_FUEL_MULTIPLIER) - 1;
                burnTime = burnCapacity - 1;
                stack.shrink(1);
                return true;
            } else {
                burnCapacity = 0;
                burnTime = 0;
                return false;
            }
        }
    }

    public Item getDropItem() {
        return ModItems.STEAM_TUG.get();
    }


    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        burnTime = compound.contains("burn") ? compound.getInt("burn") : 0;
        burnCapacity = compound.contains("burn_capacity") ? compound.getInt("burn_capacity") : 0;
        if(compound.contains("inv")){
            ItemStackHandler old = new ItemStackHandler();
            old.deserializeNBT(compound.getCompound("inv"));
            itemHandler.setStackInSlot(0, old.getStackInSlot(1));
        }else{
            itemHandler.deserializeNBT(compound.getCompound("tugItemHandler"));
        }
        super.readAdditionalSaveData(compound);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        compound.putInt("burn", burnTime);
        compound.putInt("burn_capacity", burnCapacity);
        compound.put("tugItemHandler", itemHandler.serializeNBT());
        super.addAdditionalSaveData(compound);
    }

    @Override
    protected void onUndock() {
        super.onUndock();
        this.playSound(ModSounds.STEAM_TUG_WHISTLE.get(), 1, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
    }

    // Have to implement IInventory to work with hoppers
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

}
