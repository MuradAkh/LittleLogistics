package dev.murad.shipping.block.guiderail;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public class TugGuideRailBlock extends Block {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public TugGuideRailBlock(Properties p_i48440_1_) {
        super(p_i48440_1_);
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult rayTraceResult) {
        if(player.getPose().equals(Pose.CROUCHING)){
            world.setBlockAndUpdate(pos, state.setValue(FACING, state.getValue(FACING).getClockWise()));
            return InteractionResult.SUCCESS;
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
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context){
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection());

    }

    public static Direction getArrowsDirection(BlockState state){
        return state.getValue(CornerGuideRailBlock.FACING);
    }

}
