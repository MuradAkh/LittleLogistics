package dev.murad.shipping.entity.custom.barge;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.setup.ModEntityTypes;
import dev.murad.shipping.setup.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.BoatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.inventory.container.ChestContainer;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.*;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.stream.IntStream;

public class ChestBargeEntity extends AbstractBargeEntity implements IInventory, INamedContainerProvider, ISidedInventory {
    protected final NonNullList<ItemStack> itemStacks = createItemStacks();

    public ChestBargeEntity(EntityType<? extends BoatEntity> type, World world) {
        super(type, world);
    }

    public ChestBargeEntity(World worldIn, double x, double y, double z) {
        super(ModEntityTypes.CHEST_BARGE.get(), worldIn, x, y, z);
    }

    ChestBargeEntity(EntityType<? extends ChestBargeEntity> type, World worldIn, double x, double y, double z) {
        super(type, worldIn, x, y, z);
    }

    protected NonNullList<ItemStack> createItemStacks(){
        return NonNullList.withSize(36, ItemStack.EMPTY);
    }

    @Override
    public boolean hurt(DamageSource p_70097_1_, float p_70097_2_) {
        if (this.isInvulnerableTo(p_70097_1_)) {
            return false;
        } else if (!this.level.isClientSide && !this.removed) {
            this.spawnAtLocation(this.getDropItem());
            InventoryHelper.dropContents(this.level, this, this);
            this.remove();
            return true;
        } else {
            return true;
        }
    }


    @Override
    public Item getDropItem() {
        return ModItems.CHEST_BARGE.get();
    }

    @Override
    public ItemStack getPickedResult(RayTraceResult target) {
        return new ItemStack(ForgeRegistries.ITEMS.getValue(
                new ResourceLocation(ShippingMod.MOD_ID, "barge")));
    }

    protected void doInteract(PlayerEntity player) {
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
        return ItemStackHelper.removeItem(this.itemStacks, p_70298_1_, p_70298_2_);

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
    public boolean stillValid(PlayerEntity p_70300_1_) {
        if (this.removed) {
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
    @Override
    public Container createMenu(int p_createMenu_1_, PlayerInventory p_createMenu_2_, PlayerEntity p_createMenu_3_) {
        if (p_createMenu_3_.isSpectator()) {
            return null;
        }
        return ChestContainer.threeRows(p_createMenu_1_, p_createMenu_2_, this);

    }

    @Override
    protected void addAdditionalSaveData(CompoundNBT p_213281_1_) {
        ItemStackHelper.saveAllItems(p_213281_1_, this.itemStacks);

    }

    @Override
    protected void readAdditionalSaveData(CompoundNBT p_70037_1_) {
        ItemStackHelper.loadAllItems(p_70037_1_, this.itemStacks);
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
