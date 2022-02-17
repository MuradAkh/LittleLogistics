package dev.murad.shipping.item;

import dev.murad.shipping.entity.custom.tug.EnergyTugEntity;
import dev.murad.shipping.entity.custom.tug.SteamTugEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.Level;
import net.minecraftforge.energy.IEnergyStorage;

import net.minecraft.world.item.Item.Properties;

public class EnergyTugItem extends AbstractEntityAddItem {
    public EnergyTugItem(Properties props) {
        super(props);
    }

    protected Entity getEntity(Level world, BlockHitResult raytraceresult) {
        return new EnergyTugEntity(world, raytraceresult.getLocation().x, raytraceresult.getLocation().y, raytraceresult.getLocation().z);
    }
}
