package dev.murad.shipping.block.dock;

import dev.murad.shipping.entity.custom.ModBargeEntity;
import dev.murad.shipping.entity.custom.tug.TugEntity;
import dev.murad.shipping.setup.ModTileEntitiesTypes;
import javafx.util.Pair;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TugDockTileEntity extends AbstractDockTileEntity implements ITickableTileEntity {

    public TugDockTileEntity(TileEntityType<?> tileEntityTypeIn) {
        super(tileEntityTypeIn);
    }
    public TugDockTileEntity() {
        super(ModTileEntitiesTypes.TUG_DOCK.get());
    }

    private boolean checkTugFull(TugEntity tug){
        return tug.getItem(1).getCount() == tug.getItem(1).getMaxStackSize();
    }



    public boolean holdVessel(IInventory tug, Direction direction){
        if (!(tug instanceof TugEntity) || !getBlockState().getValue(TugDockBlock.FACING).getOpposite().equals(direction)){
            return false;
        }

        // Tug needs to be loaded
        if(getInsertHopper().map(hopper -> mayMoveIntoInventory(tug, hopper)).orElse(false)){
            return true;
        }
        List<Pair<ModBargeEntity, BargeDockTileEntity>> barges = getBargeDockPairs((TugEntity) tug);

        // Barges with corresponding docks for docking aren't dockable yet
//        if(!barges.stream().map(pair -> pair.getKey().isDockable()).reduce(true, Boolean::logicalAnd)){
//            return true;
//        }

        if (barges.stream().map(pair -> pair.getValue().holdVessel(pair.getKey(), direction)).reduce(false, Boolean::logicalOr)){
            return true;
        }

        return false;
    }

    private List<Pair<ModBargeEntity, BargeDockTileEntity>> getBargeDockPairs(TugEntity tug){
        List<ModBargeEntity> barges = tug.getTrain().getBarges();
        List<BargeDockTileEntity> docks = getBargeDocks();
        return IntStream.range(0, Math.min(barges.size(), docks.size()))
                .mapToObj(i -> new Pair<>(barges.get(i), docks.get(i)))
                .collect(Collectors.toList());
    }

    private List<BargeDockTileEntity> getBargeDocks(){
        Direction facing = this.getBlockState().getValue(TugDockBlock.FACING);
        Direction rowDirection = this.getBlockState().getValue(TugDockBlock.INVERTED) ? facing.getClockWise() : facing.getCounterClockWise();
        List<BargeDockTileEntity> docks = new ArrayList<>();
        for (Optional<BargeDockTileEntity> dock = getNextBargeDock(rowDirection, this.getBlockPos());
             dock.isPresent();
             dock = getNextBargeDock(rowDirection, dock.get().getBlockPos())) {
            docks.add(dock.get());
        }
        return docks;
    }

    private Optional<BargeDockTileEntity> getNextBargeDock(Direction rowDirection, BlockPos pos) {
        BlockPos next = pos.relative(rowDirection);
        return Optional.ofNullable(this.level.getBlockEntity(next))
                .filter(e -> e instanceof BargeDockTileEntity)
                .map(e -> (BargeDockTileEntity) e);
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
