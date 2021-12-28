package dev.murad.shipping.block.dock;

import dev.murad.shipping.entity.custom.TugEntity;
import dev.murad.shipping.setup.ModTileEntitiesTypes;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;

public class TugDockTileEntity extends TileEntity implements ITickableTileEntity {

    public TugDockTileEntity(TileEntityType<?> tileEntityTypeIn) {
        super(tileEntityTypeIn);
    }
    public TugDockTileEntity() {
        super(ModTileEntitiesTypes.TUG_DOCK.get());
    }

    public boolean holdTug(TugEntity tug){
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
