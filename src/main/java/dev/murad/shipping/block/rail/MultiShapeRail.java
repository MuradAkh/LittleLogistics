package dev.murad.shipping.block.rail;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;

import java.util.List;
import java.util.Set;

public interface MultiShapeRail {
    /**
     * Set the automatic rail state of this rail
     * @param state current blockstate of the rail
     * @return if state was set automatically (or in the case of manual rail,
     *         if the state conforms to the inputs already)
     */
    boolean setRailState(BlockState state, Level world, BlockPos pos, Direction in, Direction out);

    Set<Direction> getPossibleOutputDirections(BlockState state, Direction inputSide);

    /**
     * @param direction Direction of travel for the train
     */
    RailShape getVanillaRailShapeFromDirection(BlockState state, BlockPos pos, Level level, Direction direction);

    boolean isAutomaticSwitching();
}
