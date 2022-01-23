package dev.murad.shipping.block.fluid;


import dev.murad.shipping.setup.ModTileEntitiesTypes;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.entity.Pose;
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

public class FluidHopperBlock extends Block {
    public static final DirectionProperty FACING = HorizontalBlock.FACING;

    VoxelShape SHAPE_N = Stream.of(
            Block.box(15, 0, 4, 16, 7, 8),
            Block.box(15, 0, 8, 16, 3, 12),
            Block.box(11, 0, 15, 15, 7, 16),
            Block.box(0, 7, 4, 16, 8, 16),
            Block.box(1, 0, 5, 15, 1, 15),
            Block.box(15, 0, 12, 16, 7, 16),
            Block.box(6, 8, 6, 10, 11, 10),
            Block.box(0, 13, 14, 16, 16, 16),
            Block.box(0, 13, 2, 2, 16, 14),
            Block.box(14, 13, 2, 16, 16, 14),
            Block.box(0, 13, 0, 16, 16, 2),
            Block.box(2, 11, 2, 14, 13, 14),
            Block.box(6, 0, 1, 10, 3, 4),
            Block.box(7, 0, 0, 9, 2, 1),
            Block.box(0, 0, 15, 5, 7, 16),
            Block.box(5, 0, 15, 11, 4, 16),
            Block.box(1, 0, 4, 6, 7, 5),
            Block.box(10, 0, 4, 15, 7, 5),
            Block.box(6, 0, 4, 10, 4, 5),
            Block.box(0, 0, 4, 1, 7, 8),
            Block.box(0, 0, 8, 1, 3, 12),
            Block.box(0, 0, 12, 1, 7, 15)
    ).reduce((v1, v2) -> VoxelShapes.join(v1, v2, IBooleanFunction.OR)).get();;

    VoxelShape SHAPE_W = Stream.of(
            Block.box(4, 0, 0, 8, 7, 1),
            Block.box(8, 0, 0, 12, 3, 1),
            Block.box(15, 0, 1, 16, 7, 5),
            Block.box(4, 7, 0, 16, 8, 16),
            Block.box(5, 0, 1, 15, 1, 15),
            Block.box(12, 0, 0, 16, 7, 1),
            Block.box(6, 8, 6, 10, 11, 10),
            Block.box(14, 13, 0, 16, 16, 16),
            Block.box(2, 13, 14, 14, 16, 16),
            Block.box(2, 13, 0, 14, 16, 2),
            Block.box(0, 13, 0, 2, 16, 16),
            Block.box(2, 11, 2, 14, 13, 14),
            Block.box(1, 0, 6, 4, 3, 10),
            Block.box(0, 0, 7, 1, 2, 9),
            Block.box(15, 0, 11, 16, 7, 16),
            Block.box(15, 0, 5, 16, 4, 11),
            Block.box(4, 0, 10, 5, 7, 15),
            Block.box(4, 0, 1, 5, 7, 6),
            Block.box(4, 0, 6, 5, 4, 10),
            Block.box(4, 0, 15, 8, 7, 16),
            Block.box(8, 0, 15, 12, 3, 16),
            Block.box(12, 0, 15, 15, 7, 16)
    ).reduce((v1, v2) -> VoxelShapes.join(v1, v2, IBooleanFunction.OR)).get();

    VoxelShape SHAPE_S = Stream.of(
            Block.box(0, 0, 8, 1, 7, 12),
            Block.box(0, 0, 4, 1, 3, 8),
            Block.box(1, 0, 0, 5, 7, 1),
            Block.box(0, 7, 0, 16, 8, 12),
            Block.box(1, 0, 1, 15, 1, 11),
            Block.box(0, 0, 0, 1, 7, 4),
            Block.box(6, 8, 6, 10, 11, 10),
            Block.box(0, 13, 0, 16, 16, 2),
            Block.box(14, 13, 2, 16, 16, 14),
            Block.box(0, 13, 2, 2, 16, 14),
            Block.box(0, 13, 14, 16, 16, 16),
            Block.box(2, 11, 2, 14, 13, 14),
            Block.box(6, 0, 12, 10, 3, 15),
            Block.box(7, 0, 15, 9, 2, 16),
            Block.box(11, 0, 0, 16, 7, 1),
            Block.box(5, 0, 0, 11, 4, 1),
            Block.box(10, 0, 11, 15, 7, 12),
            Block.box(1, 0, 11, 6, 7, 12),
            Block.box(6, 0, 11, 10, 4, 12),
            Block.box(15, 0, 8, 16, 7, 12),
            Block.box(15, 0, 4, 16, 3, 8),
            Block.box(15, 0, 1, 16, 7, 4)
    ).reduce((v1, v2) -> VoxelShapes.join(v1, v2, IBooleanFunction.OR)).get();;

    VoxelShape SHAPE_E = Stream.of(
            Block.box(8, 0, 15, 12, 7, 16),
            Block.box(4, 0, 15, 8, 3, 16),
            Block.box(0, 0, 11, 1, 7, 15),
            Block.box(0, 7, 0, 12, 8, 16),
            Block.box(1, 0, 1, 11, 1, 15),
            Block.box(0, 0, 15, 4, 7, 16),
            Block.box(6, 8, 6, 10, 11, 10),
            Block.box(0, 13, 0, 2, 16, 16),
            Block.box(2, 13, 0, 14, 16, 2),
            Block.box(2, 13, 14, 14, 16, 16),
            Block.box(14, 13, 0, 16, 16, 16),
            Block.box(2, 11, 2, 14, 13, 14),
            Block.box(12, 0, 6, 15, 3, 10),
            Block.box(15, 0, 7, 16, 2, 9),
            Block.box(0, 0, 0, 1, 7, 5),
            Block.box(0, 0, 5, 1, 4, 11),
            Block.box(11, 0, 1, 12, 7, 6),
            Block.box(11, 0, 10, 12, 7, 15),
            Block.box(11, 0, 6, 12, 4, 10),
            Block.box(8, 0, 0, 12, 7, 1),
            Block.box(4, 0, 0, 8, 3, 1),
            Block.box(1, 0, 0, 4, 7, 1)
    ).reduce((v1, v2) -> VoxelShapes.join(v1, v2, IBooleanFunction.OR)).get();;

    public FluidHopperBlock(Properties p_i48440_1_) {
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
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context){
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());

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

    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult rayTraceResult) {
        if (!world.isClientSide){
            TileEntity entity = world.getBlockEntity(pos);
            if(entity instanceof FluidHopperTileEntity){
                if(((FluidHopperTileEntity) entity).use(player, hand)){
                    return ActionResultType.CONSUME;
                }
            }
        }
        return ActionResultType.SUCCESS;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return ModTileEntitiesTypes.FLUID_HOPPER.get().create();
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }
}
