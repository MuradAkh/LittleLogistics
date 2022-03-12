package dev.murad.shipping.entity.custom.train.wagon;

import dev.murad.shipping.setup.ModEntityTypes;
import dev.murad.shipping.setup.ModItems;
import dev.murad.shipping.util.FluidDisplayUtil;
import dev.murad.shipping.util.ItemHandlerVanillaContainerWrapper;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.stream.IntStream;

public class ChestCarEntity extends AbstractWagonEntity implements ItemHandlerVanillaContainerWrapper, WorldlyContainer, MenuProvider {
    protected final ItemStackHandler itemHandler = createHandler();

    protected final LazyOptional<IItemHandler> handler = LazyOptional.of(() -> itemHandler);
    public ChestCarEntity(EntityType<ChestCarEntity> p_38087_, Level p_38088_) {
        super(p_38087_, p_38088_);
    }

    public ChestCarEntity(Level level, Double aDouble, Double aDouble1, Double aDouble2) {
        super(ModEntityTypes.CHEST_CAR.get(), level, aDouble, aDouble1, aDouble2);

    }

    @Override
    public void remove(RemovalReason r) {
        if (!this.level.isClientSide) {
            Containers.dropContents(this.level, this, this);
        }
        super.remove(r);
    }

    private ItemStackHandler createHandler() {
        return new ItemStackHandler(27);
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.CHEST_CAR.get());
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand){
        if(!this.level.isClientSide){
            player.openMenu(this);
        }
        return InteractionResult.CONSUME;
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
    public ItemStackHandler getRawHandler() {
        return itemHandler;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.put("inv", itemHandler.serializeNBT());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        itemHandler.deserializeNBT(t.getCompound("inv"));
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return handler.cast();
        }

        return super.getCapability(cap, side);
    }

    // hack to disable hoppers before docking complete

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
