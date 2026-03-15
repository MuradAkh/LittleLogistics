package dev.murad.shipping.block.dock;

import com.mojang.serialization.MapCodec;
import dev.murad.shipping.setup.ModTileEntitiesTypes;
import dev.murad.shipping.util.RailShapeUtil;
import dev.murad.shipping.util.TickerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Universal rail dock block that replaces LocomotiveDockingRail and TrainCarDockingRail.
 * Uses {@link DockBlockEntity} for capability proxying and hold logic.
 */
public class DockRail extends BaseRailBlock implements EntityBlock {

    public static final MapCodec<DockRail> CODEC = simpleCodec(DockRail::new);

    public static final EnumProperty<RailShape> RAIL_SHAPE = RailShapeUtil.RAIL_SHAPE_STRAIGHT_FLAT;

    public DockRail(Properties properties) {
        super(true, properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(RAIL_SHAPE, RailShape.NORTH_SOUTH)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<? extends BaseRailBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WATERLOGGED, RAIL_SHAPE);
    }

    @Override
    public Property<RailShape> getShapeProperty() {
        return RAIL_SHAPE;
    }

    @Override
    public boolean canMakeSlopes(BlockState state, BlockGetter level, BlockPos pos) {
        return false;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        boolean waterlogged = fluidState.getType() == Fluids.WATER;
        Direction facing = context.getHorizontalDirection();
        RailShape shape = facing.getAxis() == Direction.Axis.X ? RailShape.EAST_WEST : RailShape.NORTH_SOUTH;
        return this.defaultBlockState()
                .setValue(RAIL_SHAPE, shape)
                .setValue(WATERLOGGED, waterlogged);
    }

    @Override
    protected BlockState updateState(BlockState state, Level level, BlockPos pos, boolean isMoving) {
        // Prevent rail shape from auto-updating; dock rails have a fixed shape.
        return state;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FLAT_AABB;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        // TODO: ModTileEntitiesTypes.DOCK will be registered in Task 6
        return ModTileEntitiesTypes.DOCK.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : TickerUtil.createTickerHelper(type, ModTileEntitiesTypes.DOCK.get(), DockBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof DockBlockEntity dockBE) {
                dockBE.cycleRedstoneMode();
                player.displayClientMessage(
                        Component.literal("Redstone mode: " + dockBE.getRedstoneMode().name()),
                        true
                );
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction side) {
        return true;
    }
}
