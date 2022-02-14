package dev.murad.shipping.block.vessel_detector;

import com.mojang.datafixers.util.Pair;
import dev.murad.shipping.setup.ModTileEntitiesTypes;
import dev.murad.shipping.util.MathUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.particles.RedstoneParticleData;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class VesselDetectorBlock extends Block {

    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    private static final RedstoneParticleData PARTICLE = new RedstoneParticleData(0.9f, 0.65f, 0.2f, 1.0f);

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return ModTileEntitiesTypes.VESSEL_DETECTOR.get().create();
    }


    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Override
    public boolean canConnectRedstone(BlockState state, IBlockReader world, BlockPos pos, @Nullable Direction side) {
        return state.getValue(FACING) == side;
    }

    @Override
    public int getSignal(BlockState state, IBlockReader reader, BlockPos blockPos, Direction direction) {
        return state.getValue(POWERED) && direction == state.getValue(FACING) ? 15 : 0;
    }


    @Override
    public int getDirectSignal(BlockState state, IBlockReader reader, BlockPos blockPos, Direction direction) {
        return state.getValue(POWERED) && direction == state.getValue(FACING) ? 15 : 0;
    }

    public VesselDetectorBlock(Properties p_i48440_1_) {
        super(p_i48440_1_);
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
        builder.add(POWERED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context){
        return this.defaultBlockState()
                .setValue(FACING, context.getNearestLookingDirection().getOpposite())
                .setValue(POWERED, false);
    }

    // client only
    private void showParticles(BlockPos pos, BlockState state, World level) {
        AxisAlignedBB bb = VesselDetectorTileEntity.getSearchBox(pos, state.getValue(FACING), level);
        List<Pair<Vector3d, Vector3d>> edges = MathUtil.getEdges(bb);

        for (Pair<Vector3d, Vector3d> edge : edges) {
            Vector3d from = edge.getFirst(), to = edge.getSecond();
            for (int i = 0; i < 10; i++) {
                Vector3d pPos = MathUtil.lerp(from, to, (float) i / 10);
                level.addParticle(PARTICLE, pPos.x, pPos.y, pPos.z, 0, 0, 0);
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public ActionResultType use(BlockState state, World level, BlockPos pos, PlayerEntity entity, Hand hand, BlockRayTraceResult hit) {
        if (level.isClientSide()) {
            showParticles(pos, state, entity.level);
        }

        return ActionResultType.SUCCESS;
    }
}
