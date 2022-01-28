package dev.murad.shipping.block.dock;

import dev.murad.shipping.block.IVesselLoader;
import dev.murad.shipping.entity.custom.VesselEntity;
import net.minecraft.tileentity.HopperTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;


public abstract class AbstractDockTileEntity extends TileEntity {
    public AbstractDockTileEntity(TileEntityType<?> p_i48289_1_) {
        super(p_i48289_1_);
    }

    public abstract boolean holdVessel(VesselEntity vessel, Direction direction);

    public Optional<HopperTileEntity> getHopper(){
        TileEntity mayBeHopper = this.level.getBlockEntity(this.getTargetBlockPos());
        if (mayBeHopper instanceof HopperTileEntity) {
            return Optional.of((HopperTileEntity) mayBeHopper);
        }
        else return Optional.empty();
    }

    public Optional<IVesselLoader> getVesselLoader(){
        TileEntity mayBeHopper = this.level.getBlockEntity(this.getTargetBlockPos());
        if (mayBeHopper instanceof IVesselLoader) {
            return Optional.of((IVesselLoader) mayBeHopper);
        }
        else return Optional.empty();
    }

    protected abstract BlockPos getTargetBlockPos();

}
