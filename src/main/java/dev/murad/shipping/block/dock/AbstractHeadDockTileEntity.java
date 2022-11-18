package dev.murad.shipping.block.dock;

import com.mojang.datafixers.util.Pair;
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

    /**
     * Checks if the whole chain of vessels should dock
     * @param vessel    any vessel, non-null, might not be head entity
     * @param direction direction of travel of the vessel
     */
    public boolean shouldDockChain(T vessel, Direction direction) {
        // checks vessel head is a head entity
        if (!(vessel instanceof LinkableEntityHead)) {
            return false;
        }

        // check direction of vessel
        if (isEntityWrongDirectionForDocking(vessel, direction)) {
            return false;
        }

        // if head dock is powered or needs to dock
        if (getBlockState().getValue(DockingBlockStates.POWERED) || this.shouldDock(vessel, direction)) {
            return true;
        }

        // check each dock in the rest of the chain
        List<Pair<T, AbstractTailDockTileEntity<T>>> tails = getTailDockPairs(vessel);
        return tails.stream().map(pair -> pair.getSecond().shouldDock(pair.getFirst(), direction)).reduce(false, Boolean::logicalOr);
    }

    /**
     * Checks if entity `direction` is facing the wrong way to be able to dock to this dock.
     */
    protected abstract boolean isEntityWrongDirectionForDocking(T tug, Direction direction);

    protected abstract Direction getRowDirection(Direction facing);

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

    private Optional<AbstractTailDockTileEntity<T>> getNextBargeDock(Direction rowDirection, BlockPos pos) {
        BlockPos next = pos.relative(rowDirection);
        return Optional.ofNullable(this.level.getBlockEntity(next))
                .filter(e -> e instanceof AbstractTailDockTileEntity)
                .map(e -> (AbstractTailDockTileEntity<T>) e);
    }

}
