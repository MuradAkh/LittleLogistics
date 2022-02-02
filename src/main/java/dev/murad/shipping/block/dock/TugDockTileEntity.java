package dev.murad.shipping.block.dock;

import com.mojang.datafixers.util.Pair;
import dev.murad.shipping.block.IVesselLoader;
import dev.murad.shipping.entity.custom.VesselEntity;
import dev.murad.shipping.entity.custom.barge.AbstractBargeEntity;
import dev.murad.shipping.entity.custom.tug.AbstractTugEntity;
import dev.murad.shipping.setup.ModTileEntitiesTypes;
import dev.murad.shipping.util.InventoryUtils;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.HopperTileEntity;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TugDockTileEntity extends AbstractDockTileEntity {

    public TugDockTileEntity(TileEntityType<?> tileEntityTypeIn) {
        super(tileEntityTypeIn);
    }
    public TugDockTileEntity() {
        super(ModTileEntitiesTypes.TUG_DOCK.get());
    }

    private boolean handleItemHopper(VesselEntity tugEntity, HopperTileEntity hopper){
        if(!(tugEntity instanceof IInventory)){
            return false;
        }
        return InventoryUtils.mayMoveIntoInventory((IInventory) tugEntity, hopper);
    }


    public boolean holdVessel(VesselEntity tug, Direction direction){
        if (!(tug instanceof AbstractTugEntity)
                || !getBlockState().getValue(TugDockBlock.FACING).getOpposite().equals(direction)
                || tug.getDirection().equals(getRowDirection(getBlockState().getValue(TugDockBlock.FACING)))
        ){
            return false;
        }


        if(getHopper().map(hopper -> handleItemHopper(tug, hopper))
                .orElse(getVesselLoader().map(l -> l.holdVessel(tug, IVesselLoader.Mode.EXPORT)).orElse(false))){
            return true;
        }


        List<Pair<AbstractBargeEntity, BargeDockTileEntity>> barges = getBargeDockPairs((AbstractTugEntity) tug);


        if (barges.stream().map(pair -> pair.getSecond().holdVessel(pair.getFirst(), direction)).reduce(false, Boolean::logicalOr)){
            return true;
        }

        return false;
    }

    @Override
    protected BlockPos getTargetBlockPos() {
        return this.getBlockPos().above();
    }

    private List<Pair<AbstractBargeEntity, BargeDockTileEntity>> getBargeDockPairs(AbstractTugEntity tug){
        List<AbstractBargeEntity> barges = tug.getTrain().getBarges();
        List<BargeDockTileEntity> docks = getBargeDocks();
        return IntStream.range(0, Math.min(barges.size(), docks.size()))
                .mapToObj(i -> new Pair<>(barges.get(i), docks.get(i)))
                .collect(Collectors.toList());
    }

    private List<BargeDockTileEntity> getBargeDocks(){
        Direction facing = this.getBlockState().getValue(TugDockBlock.FACING);
        Direction rowDirection = getRowDirection(facing);
        List<BargeDockTileEntity> docks = new ArrayList<>();
        for (Optional<BargeDockTileEntity> dock = getNextBargeDock(rowDirection, this.getBlockPos());
             dock.isPresent();
             dock = getNextBargeDock(rowDirection, dock.get().getBlockPos())) {
            docks.add(dock.get());
        }
        return docks;
    }

    private Direction getRowDirection(Direction facing) {
        return this.getBlockState().getValue(TugDockBlock.INVERTED) ? facing.getClockWise() : facing.getCounterClockWise();
    }

    private Optional<BargeDockTileEntity> getNextBargeDock(Direction rowDirection, BlockPos pos) {
        BlockPos next = pos.relative(rowDirection);
        return Optional.ofNullable(this.level.getBlockEntity(next))
                .filter(e -> e instanceof BargeDockTileEntity)
                .map(e -> (BargeDockTileEntity) e);
    }

    @Override
    public CompoundNBT save(CompoundNBT p_189515_1_) {
        return super.save(p_189515_1_);
    }

}
