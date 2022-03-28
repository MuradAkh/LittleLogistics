package dev.murad.shipping.block.rail;

import dev.murad.shipping.util.RailShapeUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SwitchRail extends BaseRailBlock implements MultiShapeRail {
    public enum OutDirection implements StringRepresentable {
        LEFT("left"), RIGHT("right");

        final String serializedName;
        OutDirection(String name) {
            this.serializedName = name;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }

        public Direction getOutDirection(Direction inDirection) {
            return this == RIGHT ? inDirection.getCounterClockWise() : inDirection.getClockWise();
        }

        public OutDirection opposite() {
            return this == LEFT ? RIGHT : LEFT;
        }
    }

    @Getter
    @RequiredArgsConstructor
    private class RailConfiguration {
        private final Direction rootDirection;
        private final Direction unpoweredDirection;
        private final Direction poweredDirection;
    }



    // for compatibilty issues
    public static final EnumProperty<RailShape> RAIL_SHAPE = RailShapeUtil.RAIL_SHAPE_STRAIGHT_FLAT;
    // facing denotes direction of straight out
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<OutDirection> OUT_DIRECTION = EnumProperty.create("out_direction", OutDirection.class);
    // is this rail track engaged?
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    @Getter
    private final boolean automaticSwitching;

    public SwitchRail(Properties pProperties, boolean automaticSwitching) {
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
                .setValue(POWERED, !automaticSwitching && pContext.getLevel().hasNeighborSignal(pContext.getClickedPos()))
                .setValue(OUT_DIRECTION, OutDirection.RIGHT);
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
    public boolean canMakeSlopes(BlockState state, BlockGetter world, BlockPos pos) {
        return false;
    }

    private RailConfiguration getRailConfiguration(BlockState state) {
        OutDirection out = state.getValue(OUT_DIRECTION);

        Direction unpoweredDirection = state.getValue(FACING);
        Direction rootDirection = unpoweredDirection.getOpposite();
        Direction poweredDirection = out.getOutDirection(rootDirection);

        return new RailConfiguration(rootDirection, unpoweredDirection, poweredDirection);
    }

    @Override
    public RailShape getRailDirection(BlockState state, BlockGetter world, BlockPos pos, @Nullable AbstractMinecart cart) {
        Direction facing = state.getValue(FACING);
        OutDirection out = state.getValue(OUT_DIRECTION);

        Direction inDirection = facing.getOpposite();
        Direction turnDirection = out.getOutDirection(inDirection);
        Direction outDirection = state.getValue(POWERED) ? turnDirection : facing;

        if (cart != null && cart.getMotionDirection().getOpposite() == facing) {
            outDirection = facing;
        }

        RailShape shape = RailShapeUtil.getRailShape(inDirection, outDirection);
        return shape;
    }



    @Override
    public boolean setRailState(BlockState state, Direction in, Direction out) {
        List<Direction> possibilities = getPossibleOutputDirections(state, in);

        if (!automaticSwitching) {
            return possibilities.contains(out);
        }

        if (!possibilities.contains(out)) return false;

        // we are a possibility!
    }

    private static final List<Direction> NO_POSSIBILITIES = List.of();

    @Override
    public List<Direction> getPossibleOutputDirections(BlockState state, Direction inputSide) {
        RailConfiguration configuration = getRailConfiguration(state);
        boolean powered = state.getValue(POWERED);

        if (inputSide == configuration.getRootDirection()) {
            if (automaticSwitching) {
                return List.of(configuration.getUnpoweredDirection(), configuration.getPoweredDirection());
            } else {
                return powered ? List.of(configuration.getPoweredDirection()) : List.of(configuration.getUnpoweredDirection());
            }
        }

        if (inputSide == configuration.getUnpoweredDirection()) {
            if (automaticSwitching) {
                return List.of(configuration.getRootDirection());
            } else {
                return powered ? NO_POSSIBILITIES : List.of(configuration.getRootDirection());
            }
        }

        if (inputSide == configuration.getPoweredDirection()) {
            if (automaticSwitching) {
                return List.of(configuration.getRootDirection());
            } else {
                return powered ? List.of(configuration.getRootDirection()) : NO_POSSIBILITIES;
            }
        }

        return NO_POSSIBILITIES;
    }

    @Override
    public RailShape getVanillaRailShapeFromDirection(BlockState state, BlockPos pos, Level level, Direction direction) {
        RailConfiguration configuration = getRailConfiguration(state);
        Direction outDirection = state.getValue(POWERED) ? configuration.getPoweredDirection() : configuration.getUnpoweredDirection();

        if (direction == configuration.getRootDirection()) {
            outDirection = configuration.getUnpoweredDirection();
        }

        RailShape shape = RailShapeUtil.getRailShape(configuration.getRootDirection(), outDirection);
        return shape;
    }

    public BlockState rotate(BlockState pState, Rotation pRot) {
        return setFacing(pState, pRot.rotate(pState.getValue(FACING)));
    }

    public BlockState mirror(BlockState pState, Mirror pMirror) {
        if (pMirror == Mirror.LEFT_RIGHT)
            return pState.setValue(OUT_DIRECTION, pState.getValue(OUT_DIRECTION).opposite());
        else if (pMirror == Mirror.FRONT_BACK)
            return rotate(pState, pMirror.getRotation(pState.getValue(FACING)));
        return pState;
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (pPlayer.isShiftKeyDown()) {
            pLevel.setBlockAndUpdate(pPos, this.mirror(pState, Mirror.LEFT_RIGHT));
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(WATERLOGGED, FACING, RAIL_SHAPE, OUT_DIRECTION, POWERED);
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block p_220069_4_, BlockPos p_220069_5_, boolean p_220069_6_) {
        super.neighborChanged(state, world, pos, p_220069_4_, p_220069_5_, p_220069_6_);
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
}
