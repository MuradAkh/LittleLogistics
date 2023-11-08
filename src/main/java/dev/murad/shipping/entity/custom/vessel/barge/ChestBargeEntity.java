package dev.murad.shipping.entity.custom.vessel.barge;

import dev.murad.shipping.entity.custom.TrainInventoryProvider;
import dev.murad.shipping.setup.ModEntityTypes;
import dev.murad.shipping.setup.ModItems;
import dev.murad.shipping.util.InventoryUtils;
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
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.stream.IntStream;

public class ChestBargeEntity extends AbstractBargeEntity implements Container, MenuProvider, TrainInventoryProvider {
    protected final ItemStackHandler itemHandler = new ItemStackHandler(27);
    protected final LazyOptional<IItemHandler> itemCapability = LazyOptional.of(() -> itemHandler);

    public ChestBargeEntity(EntityType<? extends ChestBargeEntity> type, Level world) {
        super(type, world);
    }

    public ChestBargeEntity(EntityType<? extends ChestBargeEntity> type, Level world, double x, double y, double z) {
        super(type, world, x, y, z);
    }

    @Override
    public void remove(RemovalReason r) {
        if (!this.level().isClientSide) {
            Containers.dropContents(this.level(), this, this);
        }
        super.remove(r);
    }

    @Override
    public Item getDropItem() {
        if (this.getType().equals(ModEntityTypes.BARREL_BARGE.get())) {
            return ModItems.BARREL_BARGE.get();
        } else {
            return ModItems.CHEST_BARGE.get();
        }
    }

    protected void doInteract(Player player) {
        player.openMenu(this);
    }

    @Override
    public int getContainerSize() {
        return this.itemHandler.getSlots();
    }

    @Override
    public boolean isEmpty() {
        return InventoryUtils.isEmpty(this.itemHandler);
    }

    @Override
    public @NotNull ItemStack getItem(int slot) {
        return this.itemHandler.getStackInSlot(slot);
    }

    @Override
    public @NotNull ItemStack removeItem(int slot, int count) {
        return itemHandler.extractItem(slot, count, false);
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int slot) {
        ItemStack itemstack = itemHandler.getStackInSlot(slot);
        if (itemstack.isEmpty()) {
            return ItemStack.EMPTY;
        } else {
            this.itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
            return itemstack;
        }
    }

    @Override
    public void setItem(int slot, @NotNull ItemStack stack) {
        itemHandler.setStackInSlot(slot, stack);
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.isRemoved()) {
            return false;
        } else {
            return !(player.distanceToSqr(this) > 64.0D);
        }
    }

    @Override
    public void clearContent() {
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
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("Items", itemHandler.serializeNBT());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        itemHandler.deserializeNBT(tag.getCompound("Items"));
    }

    @Override
    public Optional<ItemStackHandler> getTrainInventoryHandler() {
        return Optional.of(itemHandler);
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return this.itemCapability.cast();
        }
        return super.getCapability(cap, side);
    }
}
