package dev.murad.shipping.block.dock;

import dev.murad.shipping.setup.ModTileEntitiesTypes;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.entity.Pose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.network.DebugPacketSender;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Optional;

public class TugDockBlock extends AbstractDockBlock {
    public static final BooleanProperty INVERTED = BlockStateProperties.INVERTED;


    public TugDockBlock(Properties properties) {
        super(properties);
    }



    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return ModTileEntitiesTypes.TUG_DOCK.get().create();
    }

    @SuppressWarnings("deprecation")
    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult rayTraceResult) {
        if(player.getPose().equals(Pose.CROUCHING)){
            world.setBlockAndUpdate(pos, state.setValue(TugDockBlock.INVERTED, !state.getValue(INVERTED)));
            return ActionResultType.SUCCESS;
        }

        return super.use(state, world, pos, player, hand, rayTraceResult);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context){
        return super.getStateForPlacement(context)
                .setValue(INVERTED, false);
    }

    @Deprecated
    public void neighborChanged(BlockState state, World world, BlockPos p_220069_3_, Block p_220069_4_, BlockPos p_220069_5_, boolean p_220069_6_) {
        super.neighborChanged(state, world, p_220069_3_, p_220069_4_, p_220069_5_, p_220069_6_);
        getTileEntity(world, p_220069_3_).flatMap(AbstractDockTileEntity::getHopper).ifPresent(te -> {
            world.setBlockAndUpdate(te.getBlockPos(), te.getBlockState().setValue(HopperBlock.FACING, state.getValue(FACING)));
        });

    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(INVERTED);
    }
}
