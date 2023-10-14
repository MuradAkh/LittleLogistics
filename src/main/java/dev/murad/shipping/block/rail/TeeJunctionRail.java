package dev.murad.shipping.block.rail;

import dev.murad.shipping.util.RailShapeUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class TeeJunctionRail extends BaseRailBlock implements MultiShapeRail {
    // for compatibility issues
    public static final EnumProperty<RailShape> RAIL_SHAPE = RailShapeUtil.RAIL_SHAPE_STRAIGHT_FLAT;
    // facing denotes direction of straight out
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    // moving right is default non-powered direction
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    @Getter
    private final boolean automaticSwitching;

    public TeeJunctionRail(BlockBehaviour.Properties pProperties, boolean automaticSwitching) {
        super(false, pProperties);
        this.automaticSwitching = automaticSwitching;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        FluidState fluidstate = pContext.getLevel().getFluidState(pContext.getClickedPos());
        boolean flag = fluidstate.getType() == Fluids.WATER;
        BlockState blockstate = super.defaultBlockState();
        return setFacing(blockstate, pContext.getHorizontalDirection())
                .setValue(WATERLOGGED, flag)
                .setValue(POWERED, !automaticSwitching && pContext.getLevel().hasNeighborSignal(pContext.getClickedPos()));
    }

    @Override
    protected BlockState updateState(BlockState pState, Level pLevel, BlockPos pPos, boolean pIsMoving) {
        return pState;
    }

    private RailShape getRailShapeFromFacing(Direction facing) {
        return facing.getAxis() == Direction.Axis.X ? RailShape.EAST_WEST : RailShape.NORTH_SOUTH;
    }

    public BlockState setFacing(BlockState state, Direction facing) {
        return state
                .setValue(RAIL_SHAPE, getRailShapeFromFacing(facing))
                .setValue(FACING, facing);
    }

    @Deprecated
    @Override
    public Property<RailShape> getShapeProperty() {
        return RAIL_SHAPE;
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return FLAT_AABB;
    }

    @Override
    public Set<Direction> getPriorityDirectionsToCheck(BlockState state, Direction entrance) {
        BranchingRailConfiguration c = getRailConfiguration(state);
        return entrance.equals(c.getPoweredDirection()) ? Set.of(c.getUnpoweredDirection()) : Set.of();
    }

    @Override
    public boolean canMakeSlopes(BlockState state, BlockGetter world, BlockPos pos) {
        return false;
    }

    private BranchingRailConfiguration getRailConfiguration(BlockState state) {
        Direction facing = state.getValue(FACING);
        Direction unpoweredDirection = facing.getClockWise();
        Direction poweredDirection = facing.getCounterClockWise();
        Direction rootDirection = facing.getOpposite();

        return new BranchingRailConfiguration(rootDirection, unpoweredDirection, poweredDirection);
    }

    @Override
    public RailShape getRailDirection(BlockState state, BlockGetter world, BlockPos pos, @Nullable AbstractMinecart cart) {
        BranchingRailConfiguration c = getRailConfiguration(state);
        Direction outDirection = state.getValue(POWERED) ? c.getPoweredDirection() : c.getUnpoweredDirection();
        return RailShapeUtil.getRailShape(c.getRootDirection(), outDirection);
    }

    @Override
    public boolean setRailState(BlockState state, Level world, BlockPos pos, Direction in, Direction out) {
        BranchingRailConfiguration c = getRailConfiguration(state);
        Set<Direction> possibilities = getPossibleOutputDirections(state, in);

        if (!automaticSwitching) {
            return possibilities.contains(out);
        }

        if (!possibilities.contains(out)) return false;

        if (in == c.getRootDirection()) {
            if (out == c.getPoweredDirection()) {
                world.setBlock(pos, state.setValue(POWERED, true), 2);
                return true;
            } else if (out == c.getUnpoweredDirection()) {
                world.setBlock(pos, state.setValue(POWERED, false), 2);
                return true;
            }
            return false;
        }

        if (in == c.getUnpoweredDirection() && out == c.getRootDirection()) {
            world.setBlock(pos, state.setValue(POWERED, false), 2);
            return true;
        }

        if (in == c.getPoweredDirection() && out == c.getRootDirection()) {
            world.setBlock(pos, state.setValue(POWERED, true), 2);
            return true;
        }

        return false;
    }

    @Override
    public Set<Direction> getPossibleOutputDirections(BlockState state, Direction inputSide) {
        BranchingRailConfiguration c = getRailConfiguration(state);
        boolean powered = state.getValue(POWERED);
        Set<Direction> poss = c.getPossibleDirections(inputSide, automaticSwitching, powered);
        return poss;
    }

    @Override
    public RailShape getVanillaRailShapeFromDirection(BlockState state, BlockPos pos, Level level, Direction direction) {
        return getRailDirection(state, level, pos, null);
    }

    public BlockState rotate(BlockState pState, Rotation pRot) {
        return setFacing(pState, pRot.rotate(pState.getValue(FACING)));
    }

    public BlockState mirror(BlockState pState, Mirror pMirror) {
        return pState;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(WATERLOGGED, FACING, RAIL_SHAPE, POWERED);
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block p_220069_4_, BlockPos p_220069_5_, boolean p_220069_6_) {
        super.neighborChanged(state, world, pos, p_220069_4_, p_220069_5_, p_220069_6_);
        if (automaticSwitching) return;

        if (!world.isClientSide) {
            boolean flag = state.getValue(POWERED);
            if (flag != world.hasNeighborSignal(pos)) {
                world.setBlock(pos, state.cycle(POWERED), 2);
            }
        }
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter world, BlockPos pos, @javax.annotation.Nullable Direction side) {
        return true;
    }

    @Deprecated
    @Override
    public boolean isValidRailShape(RailShape shape) {
        return RAIL_SHAPE.getPossibleValues().contains(shape);
    }
}