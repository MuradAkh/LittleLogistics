package dev.murad.shipping.block.dock;

import dev.murad.shipping.entity.custom.vessel.VesselEntity;
import dev.murad.shipping.setup.ModTileEntitiesTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class TugDockTileEntity extends AbstractHeadDockTileEntity<VesselEntity> {
    public TugDockTileEntity(BlockPos pos, BlockState state) {
        super(ModTileEntitiesTypes.TUG_DOCK.get(), pos, state);
    }

    @Override
    protected boolean checkBadDirCondition(VesselEntity tug, Direction direction) {
        return !getBlockState().getValue(DockingBlockStates.FACING).getOpposite().equals(direction)
                ||
                tug.getDirection().equals(getRowDirection(getBlockState().getValue(DockingBlockStates.FACING)));
    }

    @Override
    protected Direction getRowDirection(Direction facing) {
        return this.getBlockState().getValue(DockingBlockStates.INVERTED) ? facing.getClockWise() : facing.getCounterClockWise();
    }

}
