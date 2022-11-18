package dev.murad.shipping.block.dock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class DockingBlockStates {
    public static final BooleanProperty INVERTED = BlockStateProperties.INVERTED;
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final EnumProperty<DockingMode> DOCKING_MODE = DockingMode.PROPERTY;
    public static void cycleDockingMode(Level world, BlockPos pos, BlockState state) {
        world.setBlockAndUpdate(pos,
                state.setValue(DockingBlockStates.DOCKING_MODE,
                        state.getValue(DockingBlockStates.DOCKING_MODE).nextState()));
    }

}
