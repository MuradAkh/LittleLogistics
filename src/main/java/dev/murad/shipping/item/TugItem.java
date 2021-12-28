package dev.murad.shipping.item;

import dev.murad.shipping.entity.custom.tug.TugEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

public class TugItem extends AbstractEntityAddItem {
    public TugItem(Properties p_i48526_2_) {
        super(p_i48526_2_);
    }

    protected Entity getEntity(World world, RayTraceResult raytraceresult) {
        return new TugEntity(world, raytraceresult.getLocation().x, raytraceresult.getLocation().y, raytraceresult.getLocation().z);
    }

}
