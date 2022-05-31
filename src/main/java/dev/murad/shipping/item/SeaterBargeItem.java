package dev.murad.shipping.item;

import dev.murad.shipping.entity.custom.vessel.barge.SeaterBargeEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

public class SeaterBargeItem extends AbstractEntityAddItem {
    public SeaterBargeItem(Properties p_i48526_2_) {
        super(p_i48526_2_);
    }


    @Override
    protected Entity getEntity(Level world, BlockHitResult raytraceresult) {
        return new SeaterBargeEntity(world, raytraceresult.getLocation().x, raytraceresult.getLocation().y, raytraceresult.getLocation().z);
    }
}
