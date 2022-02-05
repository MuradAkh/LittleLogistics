package dev.murad.shipping.item;

import dev.murad.shipping.entity.custom.barge.SeaterBargeEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

public class SeaterBargeItem extends AbstractEntityAddItem {
    public SeaterBargeItem(Properties p_i48526_2_) {
        super(p_i48526_2_);
    }


    @Override
    protected Entity getEntity(World world, RayTraceResult raytraceresult) {
        return new SeaterBargeEntity(world, raytraceresult.getLocation().x, raytraceresult.getLocation().y, raytraceresult.getLocation().z);
    }
}
