package dev.murad.shipping.item;

import dev.murad.shipping.entity.custom.vessel.barge.ChestBargeEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;

public class ChestBargeItem extends AbstractEntityAddItem {
    public ChestBargeItem(Properties p_i48526_2_) {
        super( p_i48526_2_);
    }

    @Override
    protected Entity getEntity(Level world, BlockHitResult raytraceresult) {
        return new ChestBargeEntity(world, raytraceresult.getLocation().x, raytraceresult.getLocation().y, raytraceresult.getLocation().z);
    }
}
