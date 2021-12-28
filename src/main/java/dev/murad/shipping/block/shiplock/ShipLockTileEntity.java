package dev.murad.shipping.block.shiplock;

import dev.murad.shipping.setup.ModTileEntitiesTypes;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.LockableLootTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nullable;

public class ShipLockTileEntity extends TileEntity implements ITickableTileEntity {

    public ShipLockTileEntity(TileEntityType<?> tileEntityTypeIn) {
        super(tileEntityTypeIn);
    }
    public ShipLockTileEntity() {
        super(ModTileEntitiesTypes.SHIP_LOCK.get());
    }

    public boolean holdTug(){
        return true;
    }


    @Override
    public void tick() {

    }

    @Override
    public CompoundNBT save(CompoundNBT p_189515_1_) {
        return super.save(p_189515_1_);
    }


//    @Override
//    public int[] getSlotsForFace(Direction p_180463_1_) {
//        return new int[0];
//    }
//
//    @Override
//    public boolean canPlaceItemThroughFace(int p_180462_1_, ItemStack p_180462_2_, @Nullable Direction p_180462_3_) {
//        return false;
//    }
//
//    @Override
//    public boolean canTakeItemThroughFace(int p_180461_1_, ItemStack p_180461_2_, Direction p_180461_3_) {
//        return false;
//    }
//
//    @Override
//    protected NonNullList<ItemStack> getItems() {
//        return null;
//    }
//
//    @Override
//    protected void setItems(NonNullList<ItemStack> p_199721_1_) {
//
//    }
//
//    @Override
//    protected ITextComponent getDefaultName() {
//        return null;
//    }
//
//    @Override
//    protected Container createMenu(int p_213906_1_, PlayerInventory p_213906_2_) {
//        return null;
//    }
//
//    @Override
//    public int getContainerSize() {
//        return 0;
//    }

}
