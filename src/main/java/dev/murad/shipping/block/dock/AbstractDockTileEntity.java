package dev.murad.shipping.block.dock;

import dev.murad.shipping.block.IVesselLoader;
import dev.murad.shipping.util.LinkableEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Optional;


public abstract class AbstractDockTileEntity<T extends Entity & LinkableEntity<T>> extends BlockEntity {
    public AbstractDockTileEntity(BlockEntityType<?> p_i48289_1_, BlockPos pos, BlockState s) {
        super(p_i48289_1_, pos, s);
    }

    /**
     * Checks whether the entity vessel should dock. Does not check docking conditions for other docks or entities
     */
    public boolean shouldDock(T vessel, Direction direction) {
        DockingMode mode = getBlockState().getValue(DockingBlockStates.DOCKING_MODE);

        return switch (mode) {
            case WAIT_TIMEOUT -> false; // todo: impl configurable timeout
            case WAIT_UNTIL_EMPTY -> !vessel.isEmptyForDocking();
            case WAIT_UNTIL_FULL -> !vessel.isFullForDocking();
        };
    }

    public Optional<HopperBlockEntity> getHopper(BlockPos p){
        BlockEntity mayBeHopper = this.level.getBlockEntity(p);
        if (mayBeHopper instanceof HopperBlockEntity h) {
            return Optional.of(h);
        }
        else return Optional.empty();
    }

    public Optional<IVesselLoader> getVesselLoader(BlockPos p){
        BlockEntity mayBeHopper = this.level.getBlockEntity(p);
        if (mayBeHopper instanceof IVesselLoader l) {
            return Optional.of(l);
        }
        else return Optional.empty();
    }

    protected abstract List<BlockPos> getTargetBlockPos();

}
