package ca.edtoaster.littlecontraptions.block;

import ca.edtoaster.littlecontraptions.entity.ContraptionBargeEntity;
import ca.edtoaster.littlecontraptions.setup.LCBlockEntityTypes;
import ca.edtoaster.littlecontraptions.setup.LCBlocks;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.OrientedContraptionEntity;
import com.simibubi.create.content.contraptions.mounted.CartAssemblerBlock;
import com.simibubi.create.content.contraptions.mounted.CartAssemblerBlockEntity;
import com.simibubi.create.content.contraptions.mounted.MountedContraption;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class BargeAssemblerBlockEntity extends SmartBlockEntity {
    private static final int assemblyCooldown = 8;

    // TODO: this isn't set on reload
//    protected ScrollOptionBehaviour<CartAssemblerTileEntity.CartMovementMode> movementMode;
    private int ticksSinceLastUpdate;
    protected AssemblyException lastException;

    public BargeAssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(LCBlockEntityTypes.BARGE_ASSEMBLER.get(), pos, state);
        this.ticksSinceLastUpdate = 0;
    }

    private void serverTick() {
        this.tick();
        // do cooldown checking
        if (ticksSinceLastUpdate < assemblyCooldown) {
            ticksSinceLastUpdate++;
            return;
        }

        if (level == null) return;

        List<ContraptionBargeEntity> barges = level.getEntities(EntityTypeTest.forClass(ContraptionBargeEntity.class),
                new AABB(getBlockPos()).deflate(0.3f),
                (e) -> true);

        if (barges.size() > 0)
            tryApplyActions(barges.get(0));
    }


    public void tryApplyActions(ContraptionBargeEntity barge) {
        if (barge == null || level == null || !canUpdate())
            return;

        setUpdated();

        BlockState state = level.getBlockState(worldPosition);
        if (state.getBlock() != LCBlocks.BARGE_ASSEMBLER.get())
            return;

        CartAssemblerBlock.CartAssemblerAction action = getBlockState().getValue(BargeAssemblerBlock.POWERED) ?
                CartAssemblerBlock.CartAssemblerAction.ASSEMBLE :
                CartAssemblerBlock.CartAssemblerAction.DISASSEMBLE;

        if (action.shouldAssemble())
            assemble(level, worldPosition, barge);
        if (action.shouldDisassemble())
            disassemble(level, worldPosition, barge);
    }

    protected void assemble(Level world, BlockPos pos, ContraptionBargeEntity barge) {
        if (!barge.getPassengers()
                .isEmpty())
            return;

        CartAssemblerBlockEntity.CartMovementMode mode = CartAssemblerBlockEntity.CartMovementMode.ROTATE;

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

        entity.setPos(barge.getRiderPosition());
        world.addFreshEntity(entity);
        entity.startRiding(barge);

        if (contraption.containsBlockBreakers()) {
            this.award(AllAdvancements.CONTRAPTION_ACTORS);
        }
    }

    protected void disassemble(Level world, BlockPos pos, ContraptionBargeEntity barge) {
        if (barge.getPassengers()
                .isEmpty())
            return;
        Entity entity = barge.getPassengers()
                .get(0);
        if (!(entity instanceof OrientedContraptionEntity contraption))
            return;

        contraption.yaw = BargeAssemblerBlock.getHorizontalDirection(getBlockState())
                .toYRot();
        barge.ejectPassengers();
    }

    public void setUpdated() {
        ticksSinceLastUpdate = 0;
    }

    private boolean canUpdate() {
        return ticksSinceLastUpdate >= assemblyCooldown;
    }

    public static void serverTick(Level pLevel, BlockPos pPos, BlockState pState, BargeAssemblerBlockEntity e) {
        e.serverTick();
    }

    private class BargeAssemblerValueBoxTransform extends CenteredSideValueBoxTransform {

        public BargeAssemblerValueBoxTransform() {
            super((state, d) -> {
                if (d.getAxis()
                        .isVertical())
                    return false;
                if (!state.hasProperty(BargeAssemblerBlock.FACING))
                    return false;
                return state.getValue(BargeAssemblerBlock.FACING).getAxis() != d.getAxis();
            });
        }

        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 8, 18);
        }

    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        AssemblyException.write(compound, registries, lastException);
        super.write(compound, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        lastException = AssemblyException.read(compound, registries);
        super.read(compound, registries, clientPacket);
    }

    protected ValueBoxTransform getMovementModeSlot() {
        return new BargeAssemblerValueBoxTransform();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
//        movementMode = new ScrollOptionBehaviour<>(CartAssemblerTileEntity.CartMovementMode.class,
//                Lang.translate("contraptions.cart_movement_mode"), this, getMovementModeSlot());
//        movementMode.requiresWrench();
//        behaviours.add(movementMode);
    }
}
