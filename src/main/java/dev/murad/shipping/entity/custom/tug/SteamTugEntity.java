package dev.murad.shipping.entity.custom.tug;

import dev.murad.shipping.ShippingConfig;
import dev.murad.shipping.entity.accessor.SteamTugDataAccessor;
import dev.murad.shipping.entity.container.SteamTugContainer;
import dev.murad.shipping.setup.ModEntityTypes;
import dev.murad.shipping.setup.ModItems;
import dev.murad.shipping.setup.ModSounds;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.MenuProvider;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.FurnaceTileEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeHooks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SteamTugEntity extends AbstractTugEntity {
    private static final int FURNACE_FUEL_MULTIPLIER= ShippingConfig.Server.STEAM_TUG_FUEL_MULTIPLIER.get();

    protected int burnTime = 0;
    protected int burnCapacity = 0;

    public SteamTugEntity(EntityType<? extends WaterAnimal> type, Level world) {
        super(type, world);
    }

    public SteamTugEntity(Level worldIn, double x, double y, double z) {
        super(ModEntityTypes.STEAM_TUG.get(), worldIn, x, y, z);
    }

    @Override
    protected int getNonRouteItemSlots() {
        return 1; // 1 extra slot for fuel
    }

    @Override
    protected MenuProvider createContainerProvider() {
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return new TranslatableComponent("screen.littlelogistics.tug");
            }

            @Nullable
            @Override
            public AbstractContainerMenu createMenu(int i, Inventory playerInventory, Player playerEntity) {
                return new SteamTugContainer(i, level, getDataAccessor(), playerInventory, playerEntity);
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
    public SteamTugDataAccessor getDataAccessor() {
        return new SteamTugDataAccessor.Builder(this.getId())
                .withBurnProgress(this::getBurnProgress)
                .withLit(this::isLit)
                .build();
    }

    @Override
    protected boolean isTugSlotItemValid(int slot, @Nonnull ItemStack stack){
        return slot == 1 && FurnaceTileEntity.isFuel(stack);
    }

    @Override
    protected int getTugSlotLimit(int slot){
        return slot == 1 ? 64 : 0;
    }

    @Override
    protected boolean tickFuel() {
        if (burnTime > 0) {
            burnTime--;
            return true;
        } else {
            ItemStack stack = itemHandler.getStackInSlot(1);
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
    public void readAdditionalSaveData(CompoundNBT compound) {
        burnTime = compound.contains("burn") ? compound.getInt("burn") : 0;
        burnCapacity = compound.contains("burn_capacity") ? compound.getInt("burn_capacity") : 0;
        super.readAdditionalSaveData(compound);
    }

    @Override
    public void addAdditionalSaveData(CompoundNBT compound) {
        compound.putInt("burn", burnTime);
        compound.putInt("burn_capacity", burnCapacity);
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
        return itemHandler.getStackInSlot(1).isEmpty();
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

}
