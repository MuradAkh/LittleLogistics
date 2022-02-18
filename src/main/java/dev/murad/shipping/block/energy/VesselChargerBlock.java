package dev.murad.shipping.block.energy;

import dev.murad.shipping.block.vessel_detector.VesselDetectorTileEntity;
import dev.murad.shipping.setup.ModTileEntitiesTypes;
import dev.murad.shipping.util.TickerUtil;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.stream.Stream;

public class VesselChargerBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape SHAPE_N = Stream.of(
            Block.box(3, 2, 3, 13, 13, 13),
            Block.box(0, 0, 0, 16, 2, 16),
            Block.box(2, 13, 2, 14, 15, 14),
            Block.box(7, 4, 0, 9, 6, 3),
            Block.box(7, 4, 0, 9, 6, 3),
            Block.box(6, 15, 6, 10, 16, 10)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();;
    private static final VoxelShape SHAPE_W = Stream.of(
            Block.box(3, 2, 3, 13, 13, 13),
            Block.box(0, 0, 0, 16, 2, 16),
            Block.box(2, 13, 2, 14, 15, 14),
            Block.box(0, 4, 7, 3, 6, 9),
            Block.box(0, 4, 7, 3, 6, 9),
            Block.box(6, 15, 6, 10, 16, 10)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();;
    private static final VoxelShape SHAPE_E = Stream.of(
            Block.box(3, 2, 3, 13, 13, 13),
            Block.box(0, 0, 0, 16, 2, 16),
            Block.box(2, 13, 2, 14, 15, 14),
            Block.box(13, 4, 7, 16, 6, 9),
            Block.box(13, 4, 7, 16, 6, 9),
            Block.box(6, 15, 6, 10, 16, 10)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();;
    private static final VoxelShape SHAPE_S = Stream.of(
            Block.box(3, 2, 3, 13, 13, 13),
            Block.box(0, 0, 0, 16, 2, 16),
            Block.box(2, 13, 2, 14, 15, 14),
            Block.box(7, 4, 13, 9, 6, 16),
            Block.box(7, 4, 13, 9, 6, 16),
            Block.box(6, 15, 6, 10, 16, 10)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();;

    public VesselChargerBlock(Properties p_i48440_1_) {
        super(p_i48440_1_);
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult rayTraceResult) {
        if (!world.isClientSide){
            BlockEntity entity = world.getBlockEntity(pos);
            if(entity instanceof VesselChargerTileEntity){
                ((VesselChargerTileEntity) entity).use(player, hand);
                return InteractionResult.CONSUME;

            }
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModTileEntitiesTypes.VESSEL_CHARGER.get().create(pos, state);
    }

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
    }

    @Override
    public VoxelShape getShape(BlockState p_220053_1_, BlockGetter p_220053_2_, BlockPos p_220053_3_, CollisionContext p_220053_4_) {
        switch (p_220053_1_.getValue(FACING)){
            case SOUTH:
                return SHAPE_S;
            case WEST:
                return SHAPE_W;
            case EAST:
                return SHAPE_E;
            case NORTH:
            default:
                return SHAPE_N;
        }
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context){
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());

    }

    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : TickerUtil.createTickerHelper(type, ModTileEntitiesTypes.VESSEL_CHARGER.get(), VesselChargerTileEntity::serverTick);
    }
}


