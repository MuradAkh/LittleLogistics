package dev.murad.shipping.block.dock;

import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.HopperTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;

import java.util.Optional;


public abstract class AbstractDockTileEntity extends TileEntity {
    public AbstractDockTileEntity(TileEntityType<?> p_i48289_1_) {
        super(p_i48289_1_);
    }

    public abstract boolean holdVessel(Entity vessel, Direction direction);

    public Optional<HopperTileEntity> getInsertHopper(){
        TileEntity mayBeHopper = this.level.getBlockEntity(this.getBlockPos().above());
        if (mayBeHopper instanceof HopperTileEntity) {
            return Optional.of((HopperTileEntity) mayBeHopper);
        }
        else return Optional.empty();
    }

}
