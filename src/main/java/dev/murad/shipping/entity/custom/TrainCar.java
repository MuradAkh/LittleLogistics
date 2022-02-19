package dev.murad.shipping.entity.custom;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.extensions.IForgeAbstractMinecart;

public class TrainCar extends AbstractMinecart implements IForgeAbstractMinecart {

    public TrainCar(EntityType<?> p_38087_, Level p_38088_) {
        super(p_38087_, p_38088_);
    }

    public TrainCar(Level level, Double aDouble, Double aDouble1, Double aDouble2) {
        super(EntityType.MINECART, level, aDouble, aDouble1, aDouble2);
    }

    @Override
    public Type getMinecartType() {
        // Why does this even exist
        return Type.CHEST;
    }
}
