package dev.murad.shipping.block.dockingstation;

import dev.murad.shipping.setup.ModTileEntitiesTypes;
import dev.murad.shipping.util.TickerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/**
 * The single placeable block for a docking station.
 * When placed as CONTROLLER, automatically forms a 5-block gantry crane structure.
 * Breaking any part of the structure removes all 5 blocks and drops one item.
 */
public class DockingStationBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DockingStationPart> PART =
            EnumProperty.create("part", DockingStationPart.class);
    /** DyeColor id (0-15). Stored in block state so clients re-render immediately on change. */
    public static final IntegerProperty COLOR = IntegerProperty.create("color", 0, 15);

    public DockingStationBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, DockingStationPart.CONTROLLER)
                .setValue(COLOR, DyeColor.RED.getId()));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, COLOR);
    }

    // =========================================================================
    // Placement
    // =========================================================================

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockState state = defaultBlockState()
                .setValue(FACING, facing)
                .setValue(PART, DockingStationPart.CONTROLLER);

        // Pre-check: all extension positions must be replaceable.
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        for (DockingStationPart part : DockingStationPart.values()) {
            if (part == DockingStationPart.CONTROLLER) continue;
            BlockPos extPos = part.partPos(pos, facing);
            if (!level.getBlockState(extPos).canBeReplaced(context)) {
                if (context.getPlayer() != null) {
                    context.getPlayer().displayClientMessage(
                            Component.translatable("block.littlelogistics.docking_station.no_space"), true);
                }
                return null; // Fails placement; item is not consumed
            }
        }
        return state;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos,
                           BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && state.getValue(PART) == DockingStationPart.CONTROLLER) {
            formStructure(state, level, pos);
        }
    }

    private void formStructure(BlockState controllerState, Level level, BlockPos controllerPos) {
        Direction facing = controllerState.getValue(FACING);
        int color = controllerState.getValue(COLOR);
        for (DockingStationPart part : DockingStationPart.values()) {
            if (part == DockingStationPart.CONTROLLER) continue;
            BlockPos extPos = part.partPos(controllerPos, facing);
            level.setBlock(extPos, defaultBlockState()
                    .setValue(FACING, facing)
                    .setValue(PART, part)
                    .setValue(COLOR, color), Block.UPDATE_ALL);
        }
    }

    // =========================================================================
    // Break / removal
    // =========================================================================

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos,
                            BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide
                && state.getBlock() == this
                && newState.getBlock() != this) {
            // Unregister from station before dismantling
            if (state.getValue(PART) == DockingStationPart.CONTROLLER) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof DockingStationBlockEntity dockBE) {
                    dockBE.leaveStation();
                }
            }
            dismantleStructure(state, level, pos);
        }
        // Always call super so the block entity is properly removed for CONTROLLER
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private void dismantleStructure(BlockState state, Level level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        DockingStationPart thisPart = state.getValue(PART);
        BlockPos controllerPos = thisPart.controllerPos(pos, facing);

        for (DockingStationPart part : DockingStationPart.values()) {
            BlockPos pPos = part.partPos(controllerPos, facing);
            // Skip the block already being removed (pos) and anything already gone
            if (!pPos.equals(pos) && level.getBlockState(pPos).getBlock() == this) {
                level.setBlock(pPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    // =========================================================================
    // Block entity (CONTROLLER only)
    // =========================================================================

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (state.getValue(PART) == DockingStationPart.CONTROLLER) {
            return ModTileEntitiesTypes.DOCKING_STATION.get().create(pos, state);
        }
        return null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (state.getValue(PART) != DockingStationPart.CONTROLLER) return null;
        return level.isClientSide() ? null : TickerUtil.createTickerHelper(
                type, ModTileEntitiesTypes.DOCKING_STATION.get(),
                DockingStationBlockEntity::serverTick);
    }

    // =========================================================================
    // Interaction
    // =========================================================================

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        DyeColor color = DyeColor.getColor(stack);
        if (color != null) {
            if (!level.isClientSide) {
                Direction facing = state.getValue(FACING);
                BlockPos controllerPos = state.getValue(PART).controllerPos(pos, facing);
                for (DockingStationPart part : DockingStationPart.values()) {
                    BlockPos partPos = part.partPos(controllerPos, facing);
                    BlockState partState = level.getBlockState(partPos);
                    if (partState.is(this)) {
                        level.setBlock(partPos, partState.setValue(COLOR, color.getId()), Block.UPDATE_ALL);
                    }
                }
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            DockingStationBlockEntity be = getControllerBE(state, level, pos);
            if (be != null) {
                be.openMenu(sp);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    public static DockingStationBlockEntity getControllerBE(BlockState state, BlockGetter level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        DockingStationPart part = state.getValue(PART);
        BlockPos controllerPos = part.controllerPos(pos, facing);
        BlockEntity be = level.getBlockEntity(controllerPos);
        return be instanceof DockingStationBlockEntity dbe ? dbe : null;
    }

    // =========================================================================
    // Shape (BRIDGE has a raised floor so vehicles can pass underneath)
    // =========================================================================

    /** Crossbeam cap starts at y=12/16; hanging strut below is visual-only with no collision. */
    private static final VoxelShape BRIDGE_SHAPE = Block.box(0, 12, 0, 16, 16, 16);

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(PART) == DockingStationPart.BRIDGE) return BRIDGE_SHAPE;
        return super.getShape(state, level, pos, context);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(PART) == DockingStationPart.BRIDGE) return BRIDGE_SHAPE;
        return super.getCollisionShape(state, level, pos, context);
    }

    // =========================================================================
    // Misc
    // =========================================================================

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos,
                                      @Nullable Direction side) {
        return true;
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
}
