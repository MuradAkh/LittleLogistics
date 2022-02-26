package dev.murad.shipping.block.rapidhopper;

import dev.murad.shipping.setup.ModTileEntitiesTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class RapidHopperBlock extends HopperBlock {
    public RapidHopperBlock(Properties p_54039_) {
        super(p_54039_);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new RapidHopperTileEntity(pPos, pState);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        return pLevel.isClientSide ? null : createTickerHelper(pBlockEntityType, ModTileEntitiesTypes.RAPID_HOPPER.get(), RapidHopperTileEntity::pushItemsTick);
    }

}
