package dev.murad.shipping.block.vessel_detector;

import com.mojang.datafixers.util.Pair;
import com.mojang.math.Vector3f;
import dev.murad.shipping.setup.ModTileEntitiesTypes;
import dev.murad.shipping.util.MathUtil;
import dev.murad.shipping.util.TickerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

public class VesselDetectorBlock extends Block implements EntityBlock {

    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    private static final DustParticleOptions PARTICLE = new DustParticleOptions(new Vector3f(0.9f, 0.65f, 0.2f), 1.0f);

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModTileEntitiesTypes.VESSEL_DETECTOR.get().create(pos, state);
    }


    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter world, BlockPos pos, @Nullable Direction side) {
        return state.getValue(FACING) == side;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter reader, BlockPos blockPos, Direction direction) {
        return state.getValue(POWERED) && direction == state.getValue(FACING) ? 15 : 0;
    }


    @Override
    public int getDirectSignal(BlockState state, BlockGetter reader, BlockPos blockPos, Direction direction) {
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
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        builder.add(POWERED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context){
        return this.defaultBlockState()
                .setValue(FACING, context.getNearestLookingDirection().getOpposite())
                .setValue(POWERED, false);
    }

    // client only
    private void showParticles(BlockPos pos, BlockState state, Level level) {
        AABB bb = VesselDetectorTileEntity.getSearchBox(pos, state.getValue(FACING), level);
        List<Pair<Vec3, Vec3>> edges = MathUtil.getEdges(bb);

        for (Pair<Vec3, Vec3> edge : edges) {
            Vec3 from = edge.getFirst(), to = edge.getSecond();
            for (int i = 0; i < 10; i++) {
                Vec3 pPos = MathUtil.lerp(from, to, (float) i / 10);
                level.addParticle(PARTICLE, pPos.x, pPos.y, pPos.z, 0, 0, 0);
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player entity, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            showParticles(pos, state, entity.level);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : TickerUtil.createTickerHelper(type, ModTileEntitiesTypes.VESSEL_DETECTOR.get(), VesselDetectorTileEntity::serverTick);
    }
}
