package dev.murad.shipping.block.portal;

import dev.murad.shipping.ShippingConfig;
import dev.murad.shipping.setup.ModTileEntitiesTypes;
import dev.murad.shipping.util.CrossDimensionalUtil;
import dev.murad.shipping.util.TickerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AdvancedTrainPortalBlock extends Block implements IPortalBlock, EntityBlock  {
    VoxelShape SHAPE_N = Stream.of(
                    Block.box(15, 0, 0, 16, 16, 2),
                    Block.box(1, 0, 0, 15, 1, 2),
                    Block.box(1, 15, 0, 15, 16, 2),
                    Block.box(0, 0, 0, 1, 16, 2),
                    Block.box(1, 1, 2, 15, 15, 4))
            .reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();

    VoxelShape SHAPE_S = Stream.of(
                    Block.box(0, 0, 14, 1, 16, 16),
                    Block.box(1, 0, 14, 15, 1, 16),
                    Block.box(1, 15, 14, 15, 16, 16),
                    Block.box(15, 0, 14, 16, 16, 16),
                    Block.box(1, 1, 12, 15, 15, 14))
            .reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();

    VoxelShape SHAPE_E = Stream.of(
                    Block.box(14, 0, 15, 16, 16, 16),
                    Block.box(14, 0, 1, 16, 1, 15),
                    Block.box(14, 15, 1, 16, 16, 15),
                    Block.box(14, 0, 0, 16, 16, 1),
                    Block.box(12, 1, 1, 14, 15, 15))
            .reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();

    VoxelShape SHAPE_W = Stream.of(
                    Block.box(0, 0, 0, 2, 16, 1),
                    Block.box(0, 0, 1, 2, 1, 15),
                    Block.box(0, 15, 1, 2, 16, 15),
                    Block.box(0, 0, 15, 2, 16, 16),
                    Block.box(2, 1, 1, 4, 15, 15))
            .reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();

    public AdvancedTrainPortalBlock(Properties p_49795_) {
        super(p_49795_);
    }

    @Override
    public void destroy(LevelAccessor pLevel, BlockPos pPos, BlockState pState) {
        if(pState.getValue(IPortalBlock.PORTAL_MODE).equals(PortalMode.LINKED) && pLevel.isClientSide()){
            NetherTrainPortalTileEntity.showParticles(pLevel, pState, pPos);
        }
        super.destroy(pLevel, pPos, pState);
    }

    @Override
    public int linkRadius()   {
        return ShippingConfig.Server.ADVANCED_PORTAL_OVERWORLD_RANGE.get();
    }

    @Override
    public boolean checkValidLinkPair(Level destinationLevel, BlockPos savedPos, BlockPos clickedPos, ResourceKey<Level> dimension, double scale){
        boolean valid_dims = checkValidDimension(destinationLevel) &&
                (!destinationLevel.dimension().equals(dimension) || ShippingConfig.Server.ALLOW_ADVANCED_PORTAL_WITHIN_DIMENSION.get());

        if(!valid_dims){
            return false;
        }

        return ShippingConfig.Server.DISABLE_ADVANCED_PORTAL_RANGE_CHECK.get()
                || IPortalBlock.super.checkValidLinkPair(destinationLevel, savedPos, clickedPos, dimension, scale);
    }

    public static ResourceKey<Level> makeDimension(String name){
        return ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(name));
    }

    @Override
    public Set<ResourceKey<Level>> validDims(){
        return ShippingConfig.Server.ADVANCED_PORTAL_DIMENSIONS.get().stream().map(AdvancedTrainPortalBlock::makeDimension).collect(Collectors.toSet());
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(IPortalBlock.FACING, rot.rotate(state.getValue(IPortalBlock.FACING)));
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.getValue(IPortalBlock.FACING)));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context){
        return this.defaultBlockState()
                .setValue(IPortalBlock.PORTAL_MODE, PortalMode.UNLINKED)
                .setValue(IPortalBlock.FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(IPortalBlock.PORTAL_MODE, IPortalBlock.FACING);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return ModTileEntitiesTypes.NETHER_TRAIN_PORTAL.get().create(pPos, pState);
    }

    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : TickerUtil.createTickerHelper(type, ModTileEntitiesTypes.NETHER_TRAIN_PORTAL.get(), NetherTrainPortalTileEntity::serverTick);
    }

    @Override
    public VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext context) {
        switch (blockState.getValue(FACING)){
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
}
