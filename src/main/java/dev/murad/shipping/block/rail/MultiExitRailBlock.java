package dev.murad.shipping.block.rail;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;

public interface MultiExitRailBlock {
    RailShape getRailShapeFromDirection(BlockState state, BlockPos pos, Level level, Direction direction);
}
