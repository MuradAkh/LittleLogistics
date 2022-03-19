package dev.murad.shipping.block.dock;

import dev.murad.shipping.entity.custom.VesselEntity;
import dev.murad.shipping.setup.ModTileEntitiesTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class TugDockTileEntity extends AbstractHeadDockTileEntity<VesselEntity> {
    public TugDockTileEntity(BlockPos pos, BlockState state) {
        super(ModTileEntitiesTypes.TUG_DOCK.get(), pos, state);
    }

    @Override
    protected BlockPos getTargetBlockPos() {
        return this.getBlockPos().above();
    }
}
