package dev.murad.shipping.item;

import dev.murad.shipping.entity.custom.barge.FluidTankBargeEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.Level;

import net.minecraft.world.item.Item.Properties;

public class FluidTankBargeItem extends AbstractEntityAddItem{
    public FluidTankBargeItem(Properties p_i48526_2_) {
        super(p_i48526_2_);
    }

    @Override
    protected Entity getEntity(Level world, BlockHitResult raytraceresult) {
        return new FluidTankBargeEntity(world, raytraceresult.getLocation().x, raytraceresult.getLocation().y, raytraceresult.getLocation().z);
    }
}
