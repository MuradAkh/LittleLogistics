package dev.murad.shipping.item;

import dev.murad.shipping.entity.custom.tug.EnergyTugEntity;
import dev.murad.shipping.entity.custom.tug.SteamTugEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.energy.IEnergyStorage;

public class EnergyTugItem extends AbstractEntityAddItem {
    public EnergyTugItem(Properties props) {
        super(props);
    }

    protected Entity getEntity(World world, RayTraceResult raytraceresult) {
        return new EnergyTugEntity(world, raytraceresult.getLocation().x, raytraceresult.getLocation().y, raytraceresult.getLocation().z);
    }
}
