package dev.murad.shipping.block.guide_rail;

import dev.murad.shipping.block.dock.TugDockBlock;
import dev.murad.shipping.entity.custom.VesselEntity;
import dev.murad.shipping.setup.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.Pose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.logging.Level;

public class GuideRailBlock extends Block {
    protected static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 15.0D, 15.0D);

    public static final DirectionProperty FACING = HorizontalBlock.FACING;
    public static final BooleanProperty INVERTED = BlockStateProperties.INVERTED;


    public GuideRailBlock(Properties p_i48440_1_) {
        super(p_i48440_1_);
    }

    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult rayTraceResult) {
        if(player.getPose().equals(Pose.CROUCHING)){
            world.setBlockAndUpdate(pos, state.setValue(TugDockBlock.INVERTED, !state.getValue(INVERTED)));
            return ActionResultType.SUCCESS;
        }

        return super.use(state, world, pos, player, hand, rayTraceResult);
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        builder.add(INVERTED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context){
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(INVERTED, false);

    }

    public static Direction getArrowsDirection(BlockState state){
        Direction facing = state.getValue(GuideRailBlock.FACING);
        return state.getValue(GuideRailBlock.INVERTED) ? facing.getClockWise() : facing.getCounterClockWise();
    }

    @Override
    public void entityInside(BlockState state, World level, BlockPos pos, Entity entity){
        Direction facing = state.getValue(GuideRailBlock.FACING);
        if(!entity.getDirection().equals(facing.getOpposite()) || !(entity instanceof VesselEntity)){
            return;
        }

        Direction arrows = getArrowsDirection(state);
        entity.setDeltaMovement(entity.getDeltaMovement().add(
                new Vector3d(
                        (facing.getOpposite().getStepX() + arrows.getStepX()) * 0.1,
                        0,
                        (facing.getOpposite().getStepZ() + arrows.getStepZ()) * 0.1)));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState p_220071_1_, IBlockReader p_220071_2_, BlockPos p_220071_3_, ISelectionContext p_220071_4_) {
        return SHAPE;
    }

}
