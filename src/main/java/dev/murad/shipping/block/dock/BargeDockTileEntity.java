package dev.murad.shipping.block.dock;

import dev.murad.shipping.entity.custom.VesselEntity;
import dev.murad.shipping.setup.ModTileEntitiesTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class BargeDockTileEntity extends AbstractTailDockTileEntity<VesselEntity> {
    public BargeDockTileEntity(BlockPos pos, BlockState state) {
        super(ModTileEntitiesTypes.BARGE_DOCK.get(), pos, state);
    }

    protected BlockPos getTargetBlockPos(){
        if (isExtract()) {
            return this.getBlockPos()
                    .below()
                    .relative(this.getBlockState().getValue(DockingBlockStates.FACING));
        } else return this.getBlockPos().above();
    }
}
