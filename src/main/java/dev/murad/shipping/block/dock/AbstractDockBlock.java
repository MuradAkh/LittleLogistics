package dev.murad.shipping.block.dock;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Optional;

public abstract class AbstractDockBlock extends Block {
    public static final DirectionProperty FACING = HorizontalBlock.FACING;

    public AbstractDockBlock(Properties p_i48440_1_) {
        super(p_i48440_1_);
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    protected Optional<AbstractDockTileEntity> getTileEntity(World world, BlockPos pos){
        TileEntity tileEntity = world.getBlockEntity(pos);
        if (tileEntity instanceof AbstractDockTileEntity)
            return Optional.of((AbstractDockTileEntity) tileEntity);
        else
            return Optional.empty();

    }

    @Deprecated
    public void neighborChanged(BlockState state, World world, BlockPos p_220069_3_, Block p_220069_4_, BlockPos p_220069_5_, boolean p_220069_6_) {
        super.neighborChanged(state, world, p_220069_3_, p_220069_4_, p_220069_5_, p_220069_6_);
        getTileEntity(world, p_220069_3_).flatMap(AbstractDockTileEntity::getHopper).ifPresent(te -> {
            if (te.getBlockPos().equals(p_220069_3_.above())){
                world.setBlockAndUpdate(te.getBlockPos(), te.getBlockState().setValue(HopperBlock.FACING, state.getValue(FACING)));
            }
        });

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
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context){
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());

    }
}
