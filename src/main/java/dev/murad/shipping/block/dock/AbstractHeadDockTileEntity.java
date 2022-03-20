package dev.murad.shipping.block.dock;

import com.mojang.datafixers.util.Pair;
import dev.murad.shipping.block.IVesselLoader;
import dev.murad.shipping.util.InventoryUtils;
import dev.murad.shipping.util.LinkableEntity;
import dev.murad.shipping.util.LinkableEntityHead;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class AbstractHeadDockTileEntity<T extends Entity & LinkableEntity<T>> extends AbstractDockTileEntity<T> {
    public AbstractHeadDockTileEntity(BlockEntityType<?> t, BlockPos pos, BlockState state) {
        super(t, pos, state);
    }

    private boolean handleItemHopper(T tugEntity, HopperBlockEntity hopper){
        if(!(tugEntity instanceof Container)){
            return false;
        }
        return InventoryUtils.mayMoveIntoInventory((Container) tugEntity, hopper);
    }


    public boolean hold(T tug, Direction direction){
        if (!(tug instanceof LinkableEntityHead)
                || !getBlockState().getValue(DockingBlockStates.FACING).getOpposite().equals(direction)
                || tug.getDirection().equals(getRowDirection(getBlockState().getValue(DockingBlockStates.FACING)))
        ){
            return false;
        }

        // force tug to be docked when powered
        // todo: add UI for inverted mode toggle?
        if (getBlockState().getValue(DockingBlockStates.POWERED)) {
            return true;
        }

        if(getHopper().map(hopper -> handleItemHopper(tug, hopper))
                .orElse(getVesselLoader().map(l -> l.hold(tug, IVesselLoader.Mode.EXPORT)).orElse(false))){
            return true;
        }


        List<Pair<T, AbstractTailDockTileEntity<T>>> barges = getTailDockPairs(tug);


        if (barges.stream().map(pair -> pair.getSecond().hold(pair.getFirst(), direction)).reduce(false, Boolean::logicalOr)){
            return true;
        }

        return false;
    }

    @Override
    protected abstract BlockPos getTargetBlockPos();

    private List<Pair<T, AbstractTailDockTileEntity<T>>> getTailDockPairs(T tug){
        List<T> barges = tug.getTrain().asListOfTugged();
        List<AbstractTailDockTileEntity<T>> docks = getTailDocks();
        return IntStream.range(0, Math.min(barges.size(), docks.size()))
                .mapToObj(i -> new Pair<>(barges.get(i), docks.get(i)))
                .collect(Collectors.toList());
    }

    private List<AbstractTailDockTileEntity<T>> getTailDocks(){
        Direction facing = this.getBlockState().getValue(DockingBlockStates.FACING);
        Direction rowDirection = getRowDirection(facing);
        List<AbstractTailDockTileEntity<T>> docks = new ArrayList<>();
        for (Optional<AbstractTailDockTileEntity<T>> dock = getNextBargeDock(rowDirection, this.getBlockPos());
             dock.isPresent();
             dock = getNextBargeDock(rowDirection, dock.get().getBlockPos())) {
            docks.add(dock.get());
        }
        return docks;
    }

    private Direction getRowDirection(Direction facing) {
        return this.getBlockState().getValue(DockingBlockStates.INVERTED) ? facing.getClockWise() : facing.getCounterClockWise();
    }

    private Optional<AbstractTailDockTileEntity<T>> getNextBargeDock(Direction rowDirection, BlockPos pos) {
        BlockPos next = pos.relative(rowDirection);
        return Optional.ofNullable(this.level.getBlockEntity(next))
                .filter(e -> e instanceof AbstractTailDockTileEntity)
                .map(e -> (AbstractTailDockTileEntity<T>) e);
    }

}
