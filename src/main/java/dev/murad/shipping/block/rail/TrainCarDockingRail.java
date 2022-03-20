package dev.murad.shipping.block.rail;

import dev.murad.shipping.block.dock.DockingBlockStates;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

public class TrainCarDockingRail extends AbstractDockingRail{
    public TrainCarDockingRail(Properties pProperties) {
        super(pProperties);
    }
    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (pPlayer.isShiftKeyDown()) {
            pLevel.setBlockAndUpdate(pPos, pState.setValue(DockingBlockStates.INVERTED, !pState.getValue(DockingBlockStates.INVERTED)));
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(DockingBlockStates.INVERTED);
    }
}
