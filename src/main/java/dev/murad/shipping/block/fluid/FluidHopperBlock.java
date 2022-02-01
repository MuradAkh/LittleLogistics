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
            Block.box(0, 0, 3, 16, 1, 16),
            Block.box(0, 12, 3, 16, 13, 16),
            Block.box(6.5, 13, 8, 9.5, 15, 11),
            Block.box(6.5, 13, 8, 9.5, 15, 11),
            Block.box(0, 1, 12, 2, 12, 16),
            Block.box(2, 1, 7, 2, 12, 12),
            Block.box(14, 1, 7, 14, 12, 12),
            Block.box(0, 1, 3, 3, 12, 7),
            Block.box(14, 1, 12, 16, 12, 16),
            Block.box(13, 1, 3, 16, 12, 7),
            Block.box(3, 1, 3, 5, 12, 5),
            Block.box(5, 1, 4, 11, 12, 4),
            Block.box(5, 1, 15, 11, 12, 15),
            Block.box(11, 1, 3, 13, 12, 5),
            Block.box(11, 1, 14, 14, 12, 16),
            Block.box(2, 1, 14, 5, 12, 16),
            Block.box(6.5, 2, 0, 9.5, 5, 5),
            Block.box(6.5, 2, 0, 9.5, 5, 5)
    ).reduce((v1, v2) -> VoxelShapes.join(v1, v2, IBooleanFunction.OR)).get();

    VoxelShape SHAPE_E = Stream.of(
            Block.box(0, 0, 0, 13, 1, 16),
            Block.box(0, 12, 0, 13, 13, 16),
            Block.box(5, 13, 6.5, 8, 15, 9.5),
            Block.box(5, 13, 6.5, 8, 15, 9.5),
            Block.box(0, 1, 0, 4, 12, 2),
            Block.box(4, 1, 2, 9, 12, 2),
            Block.box(4, 1, 14, 9, 12, 14),
            Block.box(9, 1, 0, 13, 12, 3),
            Block.box(0, 1, 14, 4, 12, 16),
            Block.box(9, 1, 13, 13, 12, 16),
            Block.box(11, 1, 3, 13, 12, 5),
            Block.box(12, 1, 5, 12, 12, 11),
            Block.box(1, 1, 5, 1, 12, 11),
            Block.box(11, 1, 11, 13, 12, 13),
            Block.box(0, 1, 11, 2, 12, 14),
            Block.box(0, 1, 2, 2, 12, 5),
            Block.box(11, 2, 6.5, 16, 5, 9.5),
            Block.box(11, 2, 6.5, 16, 5, 9.5)
            ).reduce((v1, v2) -> VoxelShapes.join(v1, v2, IBooleanFunction.OR)).get();;

    VoxelShape SHAPE_S = Stream.of(
            Block.box(0, 0, 0, 16, 1, 13),
            Block.box(0, 12, 0, 16, 13, 13),
            Block.box(6.5, 13, 5, 9.5, 15, 8),
            Block.box(6.5, 13, 5, 9.5, 15, 8),
            Block.box(14, 1, 0, 16, 12, 4),
            Block.box(14, 1, 4, 14, 12, 9),
            Block.box(2, 1, 4, 2, 12, 9),
            Block.box(13, 1, 9, 16, 12, 13),
            Block.box(0, 1, 0, 2, 12, 4),
            Block.box(0, 1, 9, 3, 12, 13),
            Block.box(11, 1, 11, 13, 12, 13),
            Block.box(5, 1, 12, 11, 12, 12),
            Block.box(5, 1, 1, 11, 12, 1),
            Block.box(3, 1, 11, 5, 12, 13),
            Block.box(2, 1, 0, 5, 12, 2),
            Block.box(11, 1, 0, 14, 12, 2),
            Block.box(6.5, 2, 11, 9.5, 5, 16),
            Block.box(6.5, 2, 11, 9.5, 5, 16)
    ).reduce((v1, v2) -> VoxelShapes.join(v1, v2, IBooleanFunction.OR)).get();

    VoxelShape SHAPE_W = Stream.of(
            Block.box(3, 0, 0, 16, 1, 16),
            Block.box(3, 12, 0, 16, 13, 16),
            Block.box(8, 13, 6.5, 11, 15, 9.5),
            Block.box(8, 13, 6.5, 11, 15, 9.5),
            Block.box(12, 1, 14, 16, 12, 16),
            Block.box(7, 1, 14, 12, 12, 14),
            Block.box(7, 1, 2, 12, 12, 2),
            Block.box(3, 1, 13, 7, 12, 16),
            Block.box(12, 1, 0, 16, 12, 2),
            Block.box(3, 1, 0, 7, 12, 3),
            Block.box(3, 1, 11, 5, 12, 13),
            Block.box(4, 1, 5, 4, 12, 11),
            Block.box(15, 1, 5, 15, 12, 11),
            Block.box(3, 1, 3, 5, 12, 5),
            Block.box(14, 1, 2, 16, 12, 5),
            Block.box(14, 1, 11, 16, 12, 14),
            Block.box(0, 2, 6.5, 5, 5, 9.5),
            Block.box(0, 2, 6.5, 5, 5, 9.5)
    ).reduce((v1, v2) -> VoxelShapes.join(v1, v2, IBooleanFunction.OR)).get();

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
