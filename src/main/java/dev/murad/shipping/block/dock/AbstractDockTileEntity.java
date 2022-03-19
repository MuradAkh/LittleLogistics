package dev.murad.shipping.block.dock;

import dev.murad.shipping.block.IVesselLoader;
import dev.murad.shipping.util.LinkableEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;


public abstract class AbstractDockTileEntity<T extends Entity & LinkableEntity<T>> extends BlockEntity {
    public AbstractDockTileEntity(BlockEntityType<?> p_i48289_1_, BlockPos pos, BlockState s) {
        super(p_i48289_1_, pos, s);
    }

    public abstract boolean hold(T vessel, Direction direction);

    public Optional<HopperBlockEntity> getHopper(){
        BlockEntity mayBeHopper = this.level.getBlockEntity(this.getTargetBlockPos());
        if (mayBeHopper instanceof HopperBlockEntity h) {
            return Optional.of(h);
        }
        else return Optional.empty();
    }

    public Optional<IVesselLoader> getVesselLoader(){
        BlockEntity mayBeHopper = this.level.getBlockEntity(this.getTargetBlockPos());
        if (mayBeHopper instanceof IVesselLoader l) {
            return Optional.of(l);
        }
        else return Optional.empty();
    }

    protected abstract BlockPos getTargetBlockPos();

}
