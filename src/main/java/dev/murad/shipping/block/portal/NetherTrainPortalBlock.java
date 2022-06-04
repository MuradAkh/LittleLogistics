package dev.murad.shipping.block.portal;

import dev.murad.shipping.setup.ModTileEntitiesTypes;
import dev.murad.shipping.util.TickerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.jetbrains.annotations.Nullable;

public class NetherTrainPortalBlock extends Block implements EntityBlock, IPortalBlock {

    public NetherTrainPortalBlock(Properties p_49795_) {
        super(p_49795_);
    }

    @Override
    public boolean checkValidDimension(Level level) {
        return level.dimension().equals(Level.NETHER) || level.dimension().equals(Level.OVERWORLD);
    };

    @Override
    public boolean checkValidLinkPair(Level level, ItemStack stack, BlockPos pos, ResourceKey<Level> dimension){
        // TODO: check range
        return (dimension.equals(Level.NETHER) && level.dimension().equals(Level.OVERWORLD))
                || (dimension.equals(Level.OVERWORLD) && level.dimension().equals(Level.NETHER));

    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context){
        return this.defaultBlockState()
                .setValue(IPortalBlock.PORTAL_MODE, PortalMode.UNLINKED);

    }


    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(IPortalBlock.PORTAL_MODE);
    }



    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return ModTileEntitiesTypes.NETHER_TRAIN_SENDER.get().create(pPos, pState);
    }

    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : TickerUtil.createTickerHelper(type, ModTileEntitiesTypes.NETHER_TRAIN_SENDER.get(), NetherTrainPortalTileEntity::serverTick);
    }
}
