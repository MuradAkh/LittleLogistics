package dev.murad.shipping.block.rail;

import dev.murad.shipping.block.dock.DockingBlockStates;
import dev.murad.shipping.setup.ModTileEntitiesTypes;
import dev.murad.shipping.util.InteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class TrainCarDockingRail extends AbstractDockingRail{
    public TrainCarDockingRail(Properties pProperties) {
        super(pProperties);
    }
    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (InteractionUtil.doConfigure(pPlayer, pHand)) {
            DockingBlockStates.cycleDockingMode(pLevel, pPos, pState);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(DockingBlockStates.INVERTED);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return ModTileEntitiesTypes.CAR_DOCK.get().create(pPos, pState);
    }
}
