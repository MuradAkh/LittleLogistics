package dev.murad.shipping.block.rail;

import dev.murad.shipping.util.RailShapeUtil;
import dev.murad.shipping.util.RailHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class JunctionRail extends BaseRailBlock implements MultiShapeRail {
    private static final Logger log = LogManager.getLogger(JunctionRail.class);
    // for compatibilty issues
    public static final EnumProperty<RailShape> RAIL_SHAPE = RailShapeUtil.RAIL_SHAPE_STRAIGHT_FLAT;

    public JunctionRail(Properties pProperties) {
        super(true, pProperties);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        FluidState fluidstate = pContext.getLevel().getFluidState(pContext.getClickedPos());
        boolean flag = fluidstate.getType() == Fluids.WATER;
        BlockState blockstate = super.defaultBlockState();
        return blockstate
                .setValue(WATERLOGGED, flag)
                .setValue(RAIL_SHAPE, RailShapeUtil.DEFAULT);
    }

    @Override
    protected BlockState updateState(BlockState pState, Level pLevel, BlockPos pPos, boolean pIsMoving) {
        return pState;
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

    @Override
    public RailShape getRailDirection(BlockState state, BlockGetter world, BlockPos pos, @Nullable AbstractMinecart cart) {
        if (cart == null) {
            return state.getValue(getShapeProperty());
        }

        return RailHelper.directionFromVelocity(cart.getDeltaMovement()).getAxis() == Direction.Axis.X ? RailShape.EAST_WEST : RailShape.NORTH_SOUTH;
    }

    public BlockState rotate(BlockState pState, Rotation pRot) {
        return pState;
    }

    public BlockState mirror(BlockState pState, Mirror pMirror) {
        return pState;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(WATERLOGGED, RAIL_SHAPE);
    }

    @Override
    public boolean setRailState(BlockState state, Level world, BlockPos pos, Direction in, Direction out) {
        return in.getAxis().isHorizontal() && in.getOpposite() == out;
    }

    static final Set<Direction> NO_POSSIBILITIES = Set.of();

    @Override
    public Set<Direction> getPossibleOutputDirections(BlockState state, Direction inputSide) {
        if (inputSide.getAxis().isHorizontal()) {
            return Set.of(inputSide.getOpposite());
        }
        return NO_POSSIBILITIES;
    }

    @Override
    public Set<Direction> getPriorityDirectionsToCheck(BlockState state, Direction entrance) {
        if(entrance.equals(Direction.EAST) || entrance.equals(Direction.WEST)){
            return Set.of(Direction.NORTH, Direction.SOUTH);
        } else return Set.of();
    }

    @Override
    public RailShape getVanillaRailShapeFromDirection(BlockState state, BlockPos pos, Level level, Direction direction) {
        if(direction.equals(Direction.EAST) || direction.equals(Direction.WEST)){
            return RailShape.EAST_WEST;
        } else return RailShape.NORTH_SOUTH;
    }

    @Override
    public boolean isAutomaticSwitching() {
        return false;
    }
}
