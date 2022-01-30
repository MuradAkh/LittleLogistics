package dev.murad.shipping.item;

import dev.murad.shipping.entity.custom.tug.EnergyTugEntity;
import dev.murad.shipping.entity.custom.tug.SteamTugEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.energy.IEnergyStorage;

public class EnergyTugItem extends AbstractEntityAddItem implements IEnergyStorage {
    public EnergyTugItem(Properties props) {
        super(props);
    }

    protected Entity getEntity(World world, RayTraceResult raytraceresult) {
        return new EnergyTugEntity(world, raytraceresult.getLocation().x, raytraceresult.getLocation().y, raytraceresult.getLocation().z);
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return maxReceive;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return maxExtract;
    }

    @Override
    public int getEnergyStored() {
        return 10;
    }

    @Override
    public int getMaxEnergyStored() {
        return 20;
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    @Override
    public boolean canReceive() {
        return true;
    }
}
