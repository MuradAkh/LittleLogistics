package dev.murad.shipping.block.create;

import com.simibubi.create.content.contraptions.components.structureMovement.AssemblyException;
import com.simibubi.create.content.contraptions.components.structureMovement.OrientedContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.mounted.CartAssemblerBlock;
import com.simibubi.create.content.contraptions.components.structureMovement.mounted.CartAssemblerTileEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.mounted.MountedContraption;
import dev.murad.shipping.entity.custom.barge.SeaterBargeEntity;
import dev.murad.shipping.setup.ModBlocks;
import dev.murad.shipping.setup.ModTileEntitiesTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class BargeAssemblerTileEntity extends BlockEntity {
    private static final int assemblyCooldown = 8;

    private int ticksSinceLastUpdate;
    protected AssemblyException lastException;

    public BargeAssemblerTileEntity(BlockPos pos, BlockState state) {
        super(ModTileEntitiesTypes.BARGE_ASSEMBLER.get(), pos, state);
        this.ticksSinceLastUpdate = 0;
    }

    private void serverTick() {
        // do cooldown checking
        if (ticksSinceLastUpdate < assemblyCooldown) {
            ticksSinceLastUpdate++;
            return;
        }

        List<SeaterBargeEntity> barges = level.getEntities(EntityTypeTest.forClass(SeaterBargeEntity.class),
                new AABB(getBlockPos()).deflate(0.4f),
                (e) -> true);

        if (barges.size() > 0)
            tryAssemble(barges.get(0));
    }


    public void tryAssemble(SeaterBargeEntity barge) {
        if (barge == null)
            return;

        if (!isUpdateValid())
            return;

        resetTicksSinceMinecartUpdate();

        BlockState state = level.getBlockState(worldPosition);
        if (state.getBlock() != ModBlocks.BARGE_ASSEMBLER.get())
            return;

        CartAssemblerBlock.CartAssemblerAction action = getBlockState().getValue(BargeAssemblerBlock.POWERED) ?
                CartAssemblerBlock.CartAssemblerAction.ASSEMBLE :
                CartAssemblerBlock.CartAssemblerAction.DISASSEMBLE;

        if (action.shouldAssemble())
            assemble(level, worldPosition, barge);
        if (action.shouldDisassemble())
            disassemble(level, worldPosition, barge);
//        if (action == CartAssemblerBlock.CartAssemblerAction.ASSEMBLE_ACCELERATE) {
//            if (barge.getDeltaMovement()
//                    .length() > 1 / 128f) {
//                Direction facing = barge.getMotionDirection();
//                RailShape railShape = state.getValue(CartAssemblerBlock.RAIL_SHAPE);
//                for (Direction d : Iterate.directionsInAxis(railShape == RailShape.EAST_WEST ? Direction.Axis.X : Direction.Axis.Z))
//                    if (level.getBlockState(worldPosition.relative(d))
//                            .isRedstoneConductor(level, worldPosition.relative(d)))
//                        facing = d.getOpposite();
//
//                float speed = block.getRailMaxSpeed(state, level, worldPosition, barge);
//                barge.setDeltaMovement(facing.getStepX() * speed, facing.getStepY() * speed, facing.getStepZ() * speed);
//            }
//        }
//        if (action == CartAssemblerBlock.CartAssemblerAction.ASSEMBLE_ACCELERATE_DIRECTIONAL) {
//            Vec3i accelerationVector =
//                    ControllerRailBlock.getAccelerationVector(AllBlocks.CONTROLLER_RAIL.getDefaultState()
//                            .setValue(ControllerRailBlock.SHAPE, state.getValue(CartAssemblerBlock.RAIL_SHAPE))
//                            .setValue(ControllerRailBlock.BACKWARDS, state.getValue(CartAssemblerBlock.BACKWARDS)));
//            float speed = block.getRailMaxSpeed(state, level, worldPosition, barge);
//            barge.setDeltaMovement(Vec3.atLowerCornerOf(accelerationVector)
//                    .scale(speed));
//        }
//        if (action == CartAssemblerBlock.CartAssemblerAction.DISASSEMBLE_BRAKE) {
//            Vec3 diff = VecHelper.getCenterOf(worldPosition)
//                    .subtract(barge.position());
//            barge.setDeltaMovement(diff.x / 16f, 0, diff.z / 16f);
//        }
    }

    protected void assemble(Level world, BlockPos pos, SeaterBargeEntity barge) {
        if (!barge.getPassengers()
                .isEmpty())
            return;

        CartAssemblerTileEntity.CartMovementMode mode = CartAssemblerTileEntity.CartMovementMode.ROTATE;

        MountedContraption contraption = new MountedContraption(mode);
        try {
            if (!contraption.assemble(world, pos)) {
                return;
            }

            lastException = null;
        } catch (AssemblyException e) {
            lastException = e;
            e.printStackTrace();
            return;
        }

        Direction initialOrientation = BargeAssemblerBlock.getHorizontalDirection(getBlockState());

        contraption.removeBlocksFromWorld(world, BlockPos.ZERO);
        contraption.startMoving(world);
        contraption.expandBoundsAroundAxis(Direction.Axis.Y);

        OrientedContraptionEntity entity = OrientedContraptionEntity.create(world, contraption, initialOrientation);

        entity.setPos(pos.getX(), pos.getY(), pos.getZ());
        world.addFreshEntity(entity);
        entity.startRiding(barge);
    }

    protected void disassemble(Level world, BlockPos pos, SeaterBargeEntity barge) {
        if (barge.getPassengers()
                .isEmpty())
            return;
        Entity entity = barge.getPassengers()
                .get(0);
        if (!(entity instanceof OrientedContraptionEntity))
            return;
        OrientedContraptionEntity contraption = (OrientedContraptionEntity) entity;

        contraption.yaw = BargeAssemblerBlock.getHorizontalDirection(getBlockState())
                .toYRot();
        barge.ejectPassengers();
    }

    public void resetTicksSinceMinecartUpdate() {
        ticksSinceLastUpdate = 0;
    }

    private boolean isUpdateValid() {
        return ticksSinceLastUpdate >= assemblyCooldown;
    }

    public static void serverTick(Level pLevel, BlockPos pPos, BlockState pState, BargeAssemblerTileEntity e) {
        e.serverTick();
    }

}
