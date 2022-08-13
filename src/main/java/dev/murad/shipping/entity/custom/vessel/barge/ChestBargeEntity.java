package dev.murad.shipping.entity.custom.vessel.barge;

import dev.murad.shipping.setup.ModEntityTypes;
import dev.murad.shipping.setup.ModItems;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.stream.IntStream;

public class ChestBargeEntity extends AbstractBargeEntity implements Container, MenuProvider, WorldlyContainer {
    protected final NonNullList<ItemStack> itemStacks = createItemStacks();

    public ChestBargeEntity(EntityType<? extends ChestBargeEntity> type, Level world) {
        super(type, world);
    }

    public ChestBargeEntity(Level worldIn, double x, double y, double z) {
        super(ModEntityTypes.CHEST_BARGE.get(), worldIn, x, y, z);
    }

    protected NonNullList<ItemStack> createItemStacks(){
        return NonNullList.withSize(36, ItemStack.EMPTY);
    }

    @Override
    public void remove(RemovalReason r) {
        if (!this.level.isClientSide) {
            Containers.dropContents(this.level, this, this);
        }
        super.remove(r);
    }



    @Override
    public Item getDropItem() {
        return ModItems.CHEST_BARGE.get();
    }


    protected void doInteract(Player player) {
        player.openMenu(this);
    }


    @Override
    public int getContainerSize() {
        return 27;
    }

    @Override
    public boolean isEmpty() {
        for(ItemStack itemstack : this.itemStacks) {
            if (!itemstack.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack getItem(int p_70301_1_) {
        return this.itemStacks.get(p_70301_1_);
    }

    @Override
    public ItemStack removeItem(int p_70298_1_, int p_70298_2_) {
        return ContainerHelper.removeItem(this.itemStacks, p_70298_1_, p_70298_2_);

    }

    @Override
    public ItemStack removeItemNoUpdate(int p_70304_1_) {
        ItemStack itemstack = this.itemStacks.get(p_70304_1_);
        if (itemstack.isEmpty()) {
            return ItemStack.EMPTY;
        } else {
            this.itemStacks.set(p_70304_1_, ItemStack.EMPTY);
            return itemstack;
        }
    }

    @Override
    public void setItem(int p_70299_1_, ItemStack p_70299_2_) {
        this.itemStacks.set(p_70299_1_, p_70299_2_);
        if (!p_70299_2_.isEmpty() && p_70299_2_.getCount() > this.getMaxStackSize()) {
            p_70299_2_.setCount(this.getMaxStackSize());
        }
    }

    @Override
    public void setChanged() {

    }

    @Override
    public boolean stillValid(Player p_70300_1_) {
        if (this.isRemoved()) {
            return false;
        } else {
            return !(p_70300_1_.distanceToSqr(this) > 64.0D);
        }
    }

    @Override
    public void clearContent() {
        this.itemStacks.clear();
    }

    @Nullable
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pInventory, Player pPlayer) {
        if (pPlayer.isSpectator()) {
            return null;
        } else {
            return ChestMenu.threeRows(pContainerId, pInventory, this);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag p_213281_1_) {
        super.addAdditionalSaveData(p_213281_1_);
        ContainerHelper.saveAllItems(p_213281_1_, this.itemStacks);

    }

    @Override
    public void readAdditionalSaveData(CompoundTag p_70037_1_) {
        super.readAdditionalSaveData(p_70037_1_);
        ContainerHelper.loadAllItems(p_70037_1_, this.itemStacks);
    }

    @Override
    public int[] getSlotsForFace(Direction p_180463_1_) {
        return IntStream.range(0, getContainerSize()).toArray();
    }

    @Override
    public boolean canPlaceItemThroughFace(int p_180462_1_, ItemStack p_180462_2_, @Nullable Direction p_180462_3_) {
        return isDockable();
    }

    @Override
    public boolean canTakeItemThroughFace(int p_180461_1_, ItemStack p_180461_2_, Direction p_180461_3_) {
        return isDockable();
    }
}
