package dev.murad.shipping.block.energy;

import dev.murad.shipping.block.fluid.FluidHopperTileEntity;
import dev.murad.shipping.setup.ModTileEntitiesTypes;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.stream.Stream;

public class VesselChargerBlock extends Block {
    public static final DirectionProperty FACING = HorizontalBlock.FACING;
    private static final VoxelShape SHAPE_N = Stream.of(
            Block.box(3, 2, 3, 13, 13, 13),
            Block.box(0, 0, 0, 16, 2, 16),
            Block.box(2, 13, 2, 14, 15, 14),
            Block.box(7, 4, 0, 9, 6, 3),
            Block.box(7, 4, 0, 9, 6, 3),
            Block.box(6, 15, 6, 10, 16, 10)
    ).reduce((v1, v2) -> VoxelShapes.join(v1, v2, IBooleanFunction.OR)).get();;
    private static final VoxelShape SHAPE_W = Stream.of(
            Block.box(3, 2, 3, 13, 13, 13),
            Block.box(0, 0, 0, 16, 2, 16),
            Block.box(2, 13, 2, 14, 15, 14),
            Block.box(0, 4, 7, 3, 6, 9),
            Block.box(0, 4, 7, 3, 6, 9),
            Block.box(6, 15, 6, 10, 16, 10)
    ).reduce((v1, v2) -> VoxelShapes.join(v1, v2, IBooleanFunction.OR)).get();;
    private static final VoxelShape SHAPE_E = Stream.of(
            Block.box(3, 2, 3, 13, 13, 13),
            Block.box(0, 0, 0, 16, 2, 16),
            Block.box(2, 13, 2, 14, 15, 14),
            Block.box(13, 4, 7, 16, 6, 9),
            Block.box(13, 4, 7, 16, 6, 9),
            Block.box(6, 15, 6, 10, 16, 10)
    ).reduce((v1, v2) -> VoxelShapes.join(v1, v2, IBooleanFunction.OR)).get();;
    private static final VoxelShape SHAPE_S = Stream.of(
            Block.box(3, 2, 3, 13, 13, 13),
            Block.box(0, 0, 0, 16, 2, 16),
            Block.box(2, 13, 2, 14, 15, 14),
            Block.box(7, 4, 13, 9, 6, 16),
            Block.box(7, 4, 13, 9, 6, 16),
            Block.box(6, 15, 6, 10, 16, 10)
    ).reduce((v1, v2) -> VoxelShapes.join(v1, v2, IBooleanFunction.OR)).get();;

    public VesselChargerBlock(Properties p_i48440_1_) {
        super(p_i48440_1_);
    }

    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult rayTraceResult) {
        if (!world.isClientSide){
            TileEntity entity = world.getBlockEntity(pos);
            if(entity instanceof VesselChargerTileEntity){
                ((VesselChargerTileEntity) entity).use(player, hand);
                return ActionResultType.CONSUME;

            }
        }
        return ActionResultType.SUCCESS;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return ModTileEntitiesTypes.VESSEL_CHARGER.get().create();
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
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
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public VoxelShape getShape(BlockState p_220053_1_, IBlockReader p_220053_2_, BlockPos p_220053_3_, ISelectionContext p_220053_4_) {
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
    public BlockState getStateForPlacement(BlockItemUseContext context){
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());

    }

}
