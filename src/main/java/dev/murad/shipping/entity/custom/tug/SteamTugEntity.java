package dev.murad.shipping.entity.custom.tug;

import dev.murad.shipping.ShippingConfig;
import dev.murad.shipping.data.accessor.SteamTugDataAccessor;
import dev.murad.shipping.entity.container.SteamTugContainer;
import dev.murad.shipping.setup.ModEntityTypes;
import dev.murad.shipping.setup.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.WaterMobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.FurnaceTileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SteamTugEntity extends AbstractTugEntity {
    private static final int FURNACE_FUEL_MULTIPLIER= ShippingConfig.steam_tug_fuel_multiplier.get();

    protected int burnTime = 0;
    protected int burnCapacity = 0;

    public SteamTugEntity(EntityType<? extends WaterMobEntity> type, World world) {
        super(type, world);
    }

    public SteamTugEntity(World worldIn, double x, double y, double z) {
        super(ModEntityTypes.STEAM_TUG.get(), worldIn, x, y, z);
    }

    @Override
    protected int getNonRouteItemSlots() {
        return 1; // 1 extra slot for fuel
    }

    @Override
    protected INamedContainerProvider createContainerProvider() {
        return new INamedContainerProvider() {
            @Override
            public ITextComponent getDisplayName() {
                return new TranslationTextComponent("screen.littlelogistics.tug");
            }

            @Nullable
            @Override
            public Container createMenu(int i, PlayerInventory playerInventory, PlayerEntity playerEntity) {
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
