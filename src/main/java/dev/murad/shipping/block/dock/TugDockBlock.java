package dev.murad.shipping.block.dock;

import dev.murad.shipping.setup.ModBlocks;
import dev.murad.shipping.setup.ModTileEntitiesTypes;
import dev.murad.shipping.util.InteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.ToolAction;

import javax.annotation.Nullable;

public class TugDockBlock extends AbstractDockBlock {

    public TugDockBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModTileEntitiesTypes.TUG_DOCK.get().create(pos, state);
    }

    @SuppressWarnings("deprecation")
    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult rayTraceResult) {
        if(InteractionUtil.doConfigure(player, hand)){
            world.setBlockAndUpdate(pos, state.setValue(DockingBlockStates.INVERTED, !state.getValue(DockingBlockStates.INVERTED)));
            return InteractionResult.SUCCESS;
        }

        return super.use(state, world, pos, player, hand, rayTraceResult);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context){
        return super.getStateForPlacement(context)
                .setValue(DockingBlockStates.INVERTED, false)
                .setValue(DockingBlockStates.POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(DockingBlockStates.INVERTED, DockingBlockStates.POWERED);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block p_220069_4_, BlockPos p_220069_5_, boolean p_220069_6_) {
        super.neighborChanged(state, world, pos, p_220069_4_, p_220069_5_, p_220069_6_);
        if (!world.isClientSide) {
            boolean flag = state.getValue(DockingBlockStates.POWERED);
            if (flag != world.hasNeighborSignal(pos)) {
                world.setBlock(pos, state.cycle(DockingBlockStates.POWERED), 2);
            }
            adjustInverted(state, world, pos);
        }
    }

    private void adjustInverted(BlockState state, Level level, BlockPos pos){
        Direction facing = state.getValue(DockingBlockStates.FACING);
        Direction dockdir = state.getValue(DockingBlockStates.INVERTED) ? facing.getCounterClockWise() : facing.getClockWise();
        var tarpos = pos.relative(dockdir);
        if (level.getBlockState(tarpos).is(ModBlocks.BARGE_DOCK.get())){
           level.setBlock(pos, state.setValue(DockingBlockStates.INVERTED, !state.getValue(DockingBlockStates.INVERTED)), 2);
        }
    }


    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter world, BlockPos pos, @Nullable Direction side) {
        return true;
    }
}
