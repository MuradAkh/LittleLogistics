package dev.murad.shipping.block.dock;

import dev.murad.shipping.entity.custom.VesselEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.Optional;

public abstract class AbstractDockBlock extends Block implements EntityBlock {
    public AbstractDockBlock(Properties p_i48440_1_) {
        super(p_i48440_1_);
    }

    protected Optional<AbstractDockTileEntity<VesselEntity>> getTileEntity(Level world, BlockPos pos){
        BlockEntity tileEntity = world.getBlockEntity(pos);
        if (tileEntity instanceof AbstractDockTileEntity)
            return Optional.of((AbstractDockTileEntity<VesselEntity>) tileEntity);
        else
            return Optional.empty();

    }

    @Deprecated
    public void neighborChanged(BlockState state, Level world, BlockPos p_220069_3_, Block p_220069_4_, BlockPos p_220069_5_, boolean p_220069_6_) {
        super.neighborChanged(state, world, p_220069_3_, p_220069_4_, p_220069_5_, p_220069_6_);
        fixHopperPos(state, world, p_220069_3_);

    }

    protected void fixHopperPos(BlockState state, Level world, BlockPos p_220069_3_) {
        getTileEntity(world, p_220069_3_).flatMap(AbstractDockTileEntity::getHopper).ifPresent(te -> {
            if (te.getBlockPos().equals(p_220069_3_.above())){
                world.setBlockAndUpdate(te.getBlockPos(), te.getBlockState().setValue(HopperBlock.FACING, state.getValue(DockingBlockStates.FACING)));
            }
        });
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(DockingBlockStates.FACING, rot.rotate(state.getValue(DockingBlockStates.FACING)));
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.getValue(DockingBlockStates.FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DockingBlockStates.FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context){
        return this.defaultBlockState()
                .setValue(DockingBlockStates.FACING, context.getHorizontalDirection().getOpposite());

    }
}
