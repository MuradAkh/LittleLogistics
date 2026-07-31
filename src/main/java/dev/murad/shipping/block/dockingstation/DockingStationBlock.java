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
import net.minecraft.world.phys.shapes.Shapes;
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
import java.util.EnumMap;
import java.util.Map;

/**
 * The single placeable block for a docking station.
 * When placed as CONTROLLER, automatically forms a 2-block gantry crane structure.
 * Breaking either part of the structure removes both blocks and drops one item.
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
                BlockState controllerState = level.getBlockState(controllerPos);
                boolean changed = controllerState.is(this)
                        && controllerState.getValue(COLOR) != color.getId();
                if (changed) {
                    for (DockingStationPart part : DockingStationPart.values()) {
                        BlockPos partPos = part.partPos(controllerPos, facing);
                        BlockState partState = level.getBlockState(partPos);
                        if (partState.is(this)) {
                            level.setBlock(partPos, partState.setValue(COLOR, color.getId()), Block.UPDATE_ALL);
                        }
                    }
                    BlockEntity controller = level.getBlockEntity(controllerPos);
                    if (controller instanceof DockingStationBlockEntity dock) {
                        dock.revalidateOccupant();
                    }
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
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
    // Shape — refined per part to follow the crane model instead of full cubes.
    // The whole crane is drawn by the CONTROLLER model (docking_station_crane);
    // shapes below are authored for the un-rotated (FACING=SOUTH) model and
    // rotated to match the block state, which maps SOUTH=0/WEST=90/NORTH=180/EAST=270.
    // =========================================================================

    /** CONTROLLER cell: base plates, corner legs, longitudinal beam, central column, rear socket. */
    private static final VoxelShape CONTROLLER_SOUTH = Shapes.or(
            Block.box(1, 0, 0, 15, 4, 16),        // base plate (full depth)
            Block.box(4, 0, 0, 12, 8, 3),         // front legs
            Block.box(4, 0, 13, 12, 8, 16),       // back legs
            Block.box(6, 4, 0, 10, 9, 16),        // longitudinal beam
            Block.box(5, 4, 5, 11, 16, 11),       // central column
            Block.box(4.5, 4, 10, 11.5, 11.5, 16) // rear socket panel
    );

    /** LEFT_TOP cell: top of the column, the swing housing, the mast, and the jib stub. */
    private static final VoxelShape LEFT_TOP_SOUTH = Shapes.or(
            Block.box(5, 0, 5, 11, 3, 11),        // column top
            Block.box(4, 2, 4, 12, 9, 16),        // swing housing
            Block.box(6, 3, 6, 10, 15, 9),        // mast
            Block.box(7, 4, 0, 9, 7, 8)           // jib stub
    );

    private static final Map<Direction, VoxelShape> CONTROLLER_SHAPES = facingShapes(CONTROLLER_SOUTH);
    private static final Map<Direction, VoxelShape> LEFT_TOP_SHAPES = facingShapes(LEFT_TOP_SOUTH);

    /** Rotate a SOUTH-authored shape into all four horizontal facings (SOUTH=identity). */
    private static Map<Direction, VoxelShape> facingShapes(VoxelShape south) {
        Map<Direction, VoxelShape> map = new EnumMap<>(Direction.class);
        for (Direction d : Direction.Plane.HORIZONTAL) {
            map.put(d, rotateY(south, d.get2DDataValue()));
        }
        return map;
    }

    /** Rotate a shape clockwise (viewed from above) by quarterTurns * 90°, matching block-state y. */
    private static VoxelShape rotateY(VoxelShape shape, int quarterTurns) {
        VoxelShape result = shape;
        for (int i = 0; i < quarterTurns; i++) {
            final VoxelShape src = result;
            final VoxelShape[] acc = {Shapes.empty()};
            src.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) ->
                    acc[0] = Shapes.or(acc[0], Shapes.box(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX)));
            result = acc[0];
        }
        return result;
    }

    private VoxelShape shapeFor(BlockState state) {
        return switch (state.getValue(PART)) {
            case CONTROLLER -> CONTROLLER_SHAPES.get(state.getValue(FACING));
            case LEFT_TOP -> LEFT_TOP_SHAPES.get(state.getValue(FACING));
        };
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state);
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
