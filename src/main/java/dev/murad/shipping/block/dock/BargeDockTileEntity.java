package dev.murad.shipping.block.dock;

import dev.murad.shipping.entity.custom.vessel.VesselEntity;
import dev.murad.shipping.setup.ModTileEntitiesTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class BargeDockTileEntity extends AbstractTailDockTileEntity<VesselEntity> {
    public BargeDockTileEntity(BlockPos pos, BlockState state) {
        super(ModTileEntitiesTypes.BARGE_DOCK.get(), pos, state);
    }

    @Override
    protected boolean canDockFacingDirection(VesselEntity entity, Direction direction) {
        return getBlockState().getValue(DockingBlockStates.FACING).getOpposite().equals(direction);
    }
}
