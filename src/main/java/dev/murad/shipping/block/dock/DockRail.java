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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
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

import net.neoforged.neoforge.capabilities.Capabilities;

import javax.annotation.Nullable;

/**
 * Universal rail dock block that replaces LocomotiveDockingRail and TrainCarDockingRail.
 * Uses {@link DockBlockEntity} for capability proxying and hold logic.
 */
public class DockRail extends BaseRailBlock implements EntityBlock {

    public static final MapCodec<DockRail> CODEC = simpleCodec(DockRail::new);

    public static final EnumProperty<RailShape> RAIL_SHAPE = RailShapeUtil.RAIL_SHAPE_STRAIGHT_FLAT;
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");

    public DockRail(Properties properties) {
        super(true, properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(RAIL_SHAPE, RailShape.NORTH_SOUTH)
                .setValue(WATERLOGGED, false)
                .setValue(NORTH, false)
                .setValue(SOUTH, false)
                .setValue(EAST, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false));
    }

    @Override
    protected MapCodec<? extends BaseRailBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WATERLOGGED, RAIL_SHAPE, NORTH, SOUTH, EAST, WEST, UP, DOWN);
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

    // --- Neighbor capability detection ---

    private static boolean isEligibleSide(Direction direction, RailShape shape) {
        if (direction == Direction.UP || direction == Direction.DOWN) {
            return true;
        }
        if (shape == RailShape.NORTH_SOUTH) {
            return direction == Direction.EAST || direction == Direction.WEST;
        }
        if (shape == RailShape.EAST_WEST) {
            return direction == Direction.NORTH || direction == Direction.SOUTH;
        }
        return false;
    }

    private static boolean hasCapability(Level level, BlockPos neighborPos, Direction queryDirection) {
        // queryDirection is the direction FROM the neighbor TOWARD the dock rail
        if (level.getBlockEntity(neighborPos) == null) return false;
        if (level.getCapability(Capabilities.ItemHandler.BLOCK, neighborPos, queryDirection) != null) return true;
        if (level.getCapability(Capabilities.FluidHandler.BLOCK, neighborPos, queryDirection) != null) return true;
        if (level.getCapability(Capabilities.EnergyStorage.BLOCK, neighborPos, queryDirection) != null) return true;
        return false;
    }

    private static BooleanProperty propertyForDirection(Direction dir) {
        return switch (dir) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            case UP -> UP;
            case DOWN -> DOWN;
        };
    }

    private static BlockState updatePanelState(BlockState state, Level level, BlockPos pos) {
        RailShape shape = state.getValue(RAIL_SHAPE);
        for (Direction dir : Direction.values()) {
            BooleanProperty prop = propertyForDirection(dir);
            if (isEligibleSide(dir, shape)) {
                BlockPos neighborPos = pos.relative(dir);
                boolean connected = hasCapability(level, neighborPos, dir.getOpposite());
                state = state.setValue(prop, connected);
            } else {
                state = state.setValue(prop, false);
            }
        }
        return state;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide) {
            BlockState updated = updatePanelState(state, level, pos);
            if (updated != state) {
                level.setBlock(pos, updated, 3);
            }
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide) {
            BlockState updated = updatePanelState(state, level, pos);
            if (updated != state) {
                level.setBlock(pos, updated, 3);
            }
        }
    }
}
