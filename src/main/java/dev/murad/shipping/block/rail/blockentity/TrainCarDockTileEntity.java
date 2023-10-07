package dev.murad.shipping.block.rail.blockentity;

import dev.murad.shipping.block.dock.AbstractTailDockTileEntity;
import dev.murad.shipping.block.rail.AbstractDockingRail;
import dev.murad.shipping.entity.custom.train.AbstractTrainCarEntity;
import dev.murad.shipping.setup.ModTileEntitiesTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;

import java.util.List;

public class TrainCarDockTileEntity extends AbstractTailDockTileEntity<AbstractTrainCarEntity> {

    public TrainCarDockTileEntity(BlockPos pos, BlockState state) {
        super(ModTileEntitiesTypes.CAR_DOCK.get(), pos, state);
    }

    @Override
    protected boolean checkBadDirCondition(Direction direction) {
        return false;
    }
}
