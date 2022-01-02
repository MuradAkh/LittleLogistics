package dev.murad.shipping.item;

import dev.murad.shipping.entity.custom.BargeEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

public class BargeItem extends AbstractEntityAddItem {
    public BargeItem(Properties p_i48526_2_) {
        super( p_i48526_2_);
    }

    @Override
    protected Entity getEntity(World world, RayTraceResult raytraceresult) {
        return new BargeEntity(world, raytraceresult.getLocation().x, raytraceresult.getLocation().y, raytraceresult.getLocation().z);
    }
}
