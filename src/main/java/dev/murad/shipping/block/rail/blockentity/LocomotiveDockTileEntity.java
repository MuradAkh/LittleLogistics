package dev.murad.shipping.block.rail.blockentity;

import dev.murad.shipping.block.IVesselLoader;
import dev.murad.shipping.block.dock.AbstractHeadDockTileEntity;
import dev.murad.shipping.block.dock.DockingBlockStates;
import dev.murad.shipping.entity.custom.train.AbstractTrainCarEntity;
import dev.murad.shipping.setup.ModTileEntitiesTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LocomotiveDockTileEntity extends AbstractHeadDockTileEntity<AbstractTrainCarEntity> {
    public LocomotiveDockTileEntity(BlockPos pos, BlockState state) {
        super(ModTileEntitiesTypes.LOCOMOTIVE_DOCK.get(), pos, state);
    }

    @Override
    protected boolean canDockFacingDirection(@NotNull AbstractTrainCarEntity tug, Direction direction) {
        return tug.getDirection().equals(getBlockState().getValue(DockingBlockStates.FACING));
    }

    @Override
    protected Direction getRowDirection(Direction facing) {
        return facing.getOpposite();
    }
}
