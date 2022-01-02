package dev.murad.shipping.block.dock;

import dev.murad.shipping.entity.custom.barge.AbstractBargeEntity;
import dev.murad.shipping.entity.custom.barge.ChestBargeEntity;
import dev.murad.shipping.entity.custom.tug.TugEntity;
import dev.murad.shipping.setup.ModTileEntitiesTypes;
import javafx.util.Pair;
import net.minecraft.entity.Entity;
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



    public boolean holdVessel(Entity tug, Direction direction){
        if (!(tug instanceof TugEntity) || !getBlockState().getValue(TugDockBlock.FACING).getOpposite().equals(direction)){
            return false;
        }

        // Tug needs to be loaded
        // TODO: change for liquid powered tugs
        if(getInsertHopper().map(hopper -> mayMoveIntoInventory((IInventory) tug, hopper)).orElse(false)){
            return true;
        }
        List<Pair<AbstractBargeEntity, BargeDockTileEntity>> barges = getBargeDockPairs((TugEntity) tug);

        // Barges with corresponding docks for docking aren't dockable yet
//        if(!barges.stream().map(pair -> pair.getKey().isDockable()).reduce(true, Boolean::logicalAnd)){
//            return true;
//        }

        if (barges.stream().map(pair -> pair.getValue().holdVessel(pair.getKey(), direction)).reduce(false, Boolean::logicalOr)){
            return true;
        }

        return false;
    }

    private List<Pair<AbstractBargeEntity, BargeDockTileEntity>> getBargeDockPairs(TugEntity tug){
        List<AbstractBargeEntity> barges = tug.getTrain().getBarges();
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

}
