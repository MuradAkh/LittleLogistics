package dev.murad.shipping.block.fluid;

import dev.murad.shipping.block.IVesselLoader;
import dev.murad.shipping.setup.ModTileEntitiesTypes;
import dev.murad.shipping.util.FluidDisplayUtil;
import dev.murad.shipping.util.LinkableEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public class FluidHopperTileEntity extends BlockEntity implements IVesselLoader {
    public static final int CAPACITY = FluidAttributes.BUCKET_VOLUME * 10;
    private int cooldownTime = 0;

    public FluidHopperTileEntity(BlockPos pos, BlockState state) {
        super(ModTileEntitiesTypes.FLUID_HOPPER.get(), pos, state);
    }

    protected FluidTank tank = new FluidTank(CAPACITY) {
        @Override
        protected void onContentsChanged() {
            BlockState state = level.getBlockState(worldPosition);
            level.sendBlockUpdated(worldPosition, state, state, 3);
            setChanged();
        };
    };

    private final LazyOptional<IFluidHandler> holder = LazyOptional.of(() -> tank);



    public boolean use(Player player, InteractionHand hand){
        boolean result = FluidUtil.interactWithFluidHandler(player, hand, tank);
        player.displayClientMessage(FluidDisplayUtil.getFluidDisplay(tank), false);
        return result;
    }


    @Override
    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
            return holder.cast();
        return super.getCapability(capability, facing);
    }

    public FluidTank getTank() {
        return this.tank;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.getTank().readFromNBT(tag);
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        this.getTank().writeToNBT(tag);
    }

    @Nonnull
    @Override
    public CompoundTag getUpdateTag() {
        var tag = new CompoundTag();
        saveAdditional(tag);    // okay to send entire inventory on chunk load
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet) {
        this.load(packet.getTag());
    }

    private void serverTickInternal() {
        if (this.level != null) {
            --this.cooldownTime;
            if (this.cooldownTime <= 0) {
                // do not short-circuit
                this.cooldownTime = Boolean.logicalOr(this.tryExportFluid(), tryImportFluid()) ? 0 : 10;
            }
        }
    }

    private Optional<IFluidHandler> getExternalFluidHandler(BlockPos pos){
        return Optional.ofNullable(this.level.getBlockEntity(pos))
                .map(tile -> tile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY))
                .flatMap(LazyOptional::resolve)
                .map(Optional::of).orElseGet(() -> IVesselLoader.getEntityCapability(pos, CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, this.level));

    }

    private boolean tryImportFluid() {
        return getExternalFluidHandler(this.getBlockPos().above()).map(iFluidHandler ->
           !FluidUtil.tryFluidTransfer(this.tank, iFluidHandler, 50, true).isEmpty()
        ).orElse(false);
    }

    private boolean tryExportFluid() {
        return getExternalFluidHandler(this.getBlockPos().relative(this.getBlockState().getValue(FluidHopperBlock.FACING)))
                .map(iFluidHandler ->
            !FluidUtil.tryFluidTransfer(iFluidHandler, this.tank, 50, true).isEmpty()
        ).orElse(false);
    }

    @Override
    public<T extends Entity & LinkableEntity<T>> boolean hold(T vehicle, Mode mode) {
        return vehicle.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY).map(iFluidHandler -> {
            switch (mode) {
                case IMPORT:
                    return !FluidUtil.tryFluidTransfer(this.tank, iFluidHandler, 1, false).isEmpty();
                case EXPORT:
                    return !FluidUtil.tryFluidTransfer(iFluidHandler, this.tank, 1, false).isEmpty();
                default:
                    return false;
            }
        }).orElse(false);
    }

    public static void serverTick(Level level, BlockPos blockPos, BlockState blockState, FluidHopperTileEntity e) {
        e.serverTickInternal();
    }
}
