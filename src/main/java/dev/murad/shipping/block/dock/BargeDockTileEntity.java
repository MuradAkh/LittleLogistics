package dev.murad.shipping.block.dock;

import dev.murad.shipping.entity.custom.vessel.VesselEntity;
import dev.murad.shipping.setup.ModTileEntitiesTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class BargeDockTileEntity extends AbstractTailDockTileEntity<VesselEntity> {
    public BargeDockTileEntity(BlockPos pos, BlockState state) {
        super(ModTileEntitiesTypes.BARGE_DOCK.get(), pos, state);
    }

    @Override
    protected List<BlockPos> getTargetBlockPos(){
        if (isExtract()) {
            return List.of(this.getBlockPos()
                    .below()
                    .relative(this.getBlockState().getValue(DockingBlockStates.FACING)));
        } else return List.of(this.getBlockPos().above());
    }

    @Override
    protected boolean checkBadDirCondition(Direction direction) {
        return !getBlockState().getValue(DockingBlockStates.FACING).getOpposite().equals(direction);
    }
}
