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

    protected boolean handleItemHopper(T tugEntity, HopperBlockEntity hopper){
        if(!(tugEntity instanceof Container)){
            return false;
        }
        return InventoryUtils.mayMoveIntoInventory((Container) tugEntity, hopper);
    }

    public boolean shouldHoldEntireTrain(T headEntity, Direction direction) {
        // Check head should hold entity
        if (shouldHoldEntity(headEntity, direction)) {
            return true;
        }

        return getTailDockPairs(headEntity).stream()
                .anyMatch(pair -> pair.getSecond().shouldHoldEntity(pair.getFirst(), direction));
    }

    protected boolean shouldHoldEntity(T entity, Direction direction){
        if (!(entity instanceof LinkableEntityHead) || !canDockFacingDirection(entity, direction)) {
            return false;
        }

        // force tug to be docked when powered
        // todo: add UI for inverted mode toggle?
        if (getBlockState().getValue(DockingBlockStates.POWERED)) {
            return true;
        }

        // TODO: Check dock's capability transfer cooldown


        return false;
    }

    protected abstract boolean canDockFacingDirection(T tug, Direction direction);

    protected abstract Direction getRowDirection(Direction facing);

    public List<Pair<T, AbstractTailDockTileEntity<T>>> getTailDockPairs(T tug){
        List<T> barges = tug.getTrain().asListOfTugged();
        List<AbstractTailDockTileEntity<T>> docks = getTailDocks();
        return IntStream.range(0, Math.min(barges.size(), docks.size()))
                .mapToObj(i -> new Pair<>(barges.get(i), docks.get(i)))
                .collect(Collectors.toList());
    }

    public List<AbstractTailDockTileEntity<T>> getTailDocks(){
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

    private Optional<AbstractTailDockTileEntity<T>> getNextBargeDock(Direction rowDirection, BlockPos pos) {
        BlockPos next = pos.relative(rowDirection);
        return Optional.ofNullable(this.level.getBlockEntity(next))
                .filter(e -> e instanceof AbstractTailDockTileEntity)
                .map(e -> (AbstractTailDockTileEntity<T>) e);
    }

}
