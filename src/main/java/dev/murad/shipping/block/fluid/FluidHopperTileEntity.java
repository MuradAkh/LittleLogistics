package dev.murad.shipping.block.fluid;

import dev.murad.shipping.block.IVesselLoader;
import dev.murad.shipping.entity.custom.VesselEntity;
import dev.murad.shipping.setup.ModTileEntitiesTypes;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
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

public class FluidHopperTileEntity extends TileEntity implements ITickableTileEntity, IVesselLoader {
    public static final int CAPACITY = FluidAttributes.BUCKET_VOLUME * 10;
    private int cooldownTime = 0;

    public FluidHopperTileEntity() {
        super(ModTileEntitiesTypes.FLUID_HOPPER.get());
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

    public boolean use(PlayerEntity player, Hand hand){
        boolean result = FluidUtil.interactWithFluidHandler(player, hand, tank);
        player.displayClientMessage(new StringTextComponent(tank.getFluidAmount() + "/" + tank.getCapacity() + "ml"), false);
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
    public void load(BlockState state, CompoundNBT tag) {
        super.load(state, tag);
        this.getTank().readFromNBT(tag);
    }

    @Override
    public CompoundNBT save(CompoundNBT tag) {
        tag = super.save(tag);
        this.getTank().writeToNBT(tag);
        return tag;
    }

    @Nonnull
    @Override
    public CompoundNBT getUpdateTag() {
        return save(new CompoundNBT());    // okay to send entire inventory on chunk load
    }

    @Override
    public SUpdateTileEntityPacket getUpdatePacket() {
        return new SUpdateTileEntityPacket(getBlockPos(), 1, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket packet) {
        this.load(null,packet.getTag());
    }

    @Override
    public void tick() {
        if (this.level != null && !this.level.isClientSide) {
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
    public boolean holdVessel(VesselEntity vessel, Mode mode) {
        return vessel.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY).map(iFluidHandler -> {
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
}
