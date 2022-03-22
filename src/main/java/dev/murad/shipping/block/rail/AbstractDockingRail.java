package dev.murad.shipping.block.rail;

import dev.murad.shipping.block.dock.DockingBlockStates;
import dev.murad.shipping.setup.ModBlocks;
import dev.murad.shipping.util.RailShapeUtil;
import dev.murad.shipping.util.RailUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class AbstractDockingRail extends BaseRailBlock implements EntityBlock {
    public static final EnumProperty<RailShape> RAIL_SHAPE = RailShapeUtil.RAIL_SHAPE_STRAIGHT_FLAT;
    // facing denotes direction of straight out

    protected AbstractDockingRail(Properties pProperties) {
        super(true, pProperties);
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


    protected RailShape getRailShapeFromFacing(Direction facing) {
        return facing.getAxis() == Direction.Axis.X ? RailShape.EAST_WEST : RailShape.NORTH_SOUTH;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        FluidState fluidstate = pContext.getLevel().getFluidState(pContext.getClickedPos());
        boolean flag = fluidstate.getType() == Fluids.WATER;
        BlockState blockstate = super.defaultBlockState();
        return blockstate
                .setValue(RAIL_SHAPE, getRailShapeFromFacing(pContext.getHorizontalDirection()))
                .setValue(WATERLOGGED, flag);
    }

    @Override
    public boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        if (pLevel.getBlockState(pPos.below()).is(ModBlocks.FLUID_HOPPER.get())){
            return true;
        }
        else return super.canSurvive(pState, pLevel, pPos);
    }

    protected static void fixHopperPos(BlockState state, Level level, BlockPos pos){
        var dirs = state.getValue(RAIL_SHAPE).equals(RailShape.EAST_WEST) ?
                List.of(Direction.NORTH, Direction.SOUTH) :
                List.of(Direction.EAST, Direction.WEST);

        dirs.forEach(d ->  DockingBlockStates.fixHopperPos(state, level, pos, d, d.getOpposite()));
    }

    @Override
    public void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
        if (!pLevel.isClientSide && pLevel.getBlockState(pPos).is(this)) {
            if (!canSurvive(pState, pLevel, pPos)) {
                dropResources(pState, pLevel, pPos);
                pLevel.removeBlock(pPos, pIsMoving);
            } else {
                fixHopperPos(pState, pLevel, pPos);
                this.updateState(pState, pLevel, pPos, pBlock);
            }
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(WATERLOGGED, RAIL_SHAPE);
    }
}
