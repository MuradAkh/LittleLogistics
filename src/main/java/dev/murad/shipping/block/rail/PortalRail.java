package dev.murad.shipping.block.rail;

import com.mojang.serialization.MapCodec;
import dev.murad.shipping.util.RailHelper;
import dev.murad.shipping.util.RailShapeUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.portal.PortalForcer;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class PortalRail extends BaseRailBlock {

    public static final MapCodec<PortalRail> CODEC = simpleCodec(PortalRail::new);
    public static final EnumProperty<RailShape> RAIL_SHAPE = RailShapeUtil.RAIL_SHAPE_STRAIGHT_FLAT;
    // Direction toward the adjacent nether portal; determines the model variant and is read by destroyLinkedRail to locate the paired rail in the other dimension.
    public static final DirectionProperty PORTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;

    @Override
    protected MapCodec<? extends BaseRailBlock> codec() {
        return CODEC;
    }

    public PortalRail(Properties properties) {
        super(true, properties);
        registerDefaultState(stateDefinition.any()
                .setValue(RAIL_SHAPE, RailShape.NORTH_SOUTH)
                .setValue(PORTAL_FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        RailShape shape = (facing == Direction.EAST || facing == Direction.WEST)
                ? RailShape.EAST_WEST : RailShape.NORTH_SOUTH;
        return defaultBlockState().setValue(RAIL_SHAPE, shape).setValue(PORTAL_FACING, facing);
    }

    @Override
    protected BlockState updateState(BlockState state, Level level, BlockPos pos, boolean moving) {
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (neighborState.getFluidState().is(FluidTags.WATER)) {
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos,
                                Block neighborBlock, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, neighborBlock, fromPos, isMoving);
        if (level.isClientSide()) return;
        if (!level.getBlockState(pos.relative(state.getValue(PORTAL_FACING))).is(Blocks.NETHER_PORTAL)) {
            level.destroyBlock(pos, false);
        }
    }

    // Returns empty so creative pick-block doesn't hand the player a PortalRail; it's placed automatically, not by hand.
    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }

    // Destroys the paired PortalRail in the other dimension so both sides always disappear together.
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        super.onRemove(state, level, pos, newState, isMoving);
        if (newState.is(this) || level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;

        destroyLinkedRail(serverLevel, state, pos);
    }

    private static void destroyLinkedRail(ServerLevel level, BlockState state, BlockPos pos) {
        Direction portalFacing = state.getValue(PORTAL_FACING);
        if (!level.getBlockState(pos.relative(portalFacing)).is(Blocks.NETHER_PORTAL)) return;

        ResourceKey<Level> thisDim = level.dimension();
        ResourceKey<Level> otherDim = thisDim == Level.NETHER ? Level.OVERWORLD : Level.NETHER;
        ServerLevel otherLevel = level.getServer().getLevel(otherDim);
        if (otherLevel == null) return;

        double scale = thisDim == Level.NETHER ? 8.0 : 0.125;
        BlockPos scaledPos = BlockPos.containing(pos.getX() * scale, pos.getY(), pos.getZ() * scale);

        Optional<BlockPos> exitPortal = otherLevel.getPortalForcer()
                .findClosestPortalPosition(scaledPos, otherDim == Level.NETHER, otherLevel.getWorldBorder());

        exitPortal.ifPresent(exitPos -> {
            BlockPos floor = exitPos;
            while (otherLevel.getBlockState(floor.below()).is(Blocks.NETHER_PORTAL)) {
                floor = floor.below();
            }
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos candidate = floor.relative(dir);
                if (otherLevel.getBlockState(candidate).getBlock() instanceof PortalRail) {
                    otherLevel.destroyBlock(candidate, false);
                    break;
                }
            }
        });
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return Fluids.EMPTY.defaultFluidState();
    }

    @Deprecated
    @Override
    public Property<RailShape> getShapeProperty() {
        return RAIL_SHAPE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FLAT_AABB;
    }

    @Override
    public boolean canMakeSlopes(BlockState state, BlockGetter world, BlockPos pos) {
        return false;
    }

    @Override
    public RailShape getRailDirection(BlockState state, BlockGetter world, BlockPos pos,
                                      @Nullable AbstractMinecart cart) {
        if (cart == null) return state.getValue(RAIL_SHAPE);
        return RailHelper.directionFromVelocity(cart.getDeltaMovement()).getAxis() == Direction.Axis.X
                ? RailShape.EAST_WEST : RailShape.NORTH_SOUTH;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state;
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(RAIL_SHAPE, PORTAL_FACING);
    }
}