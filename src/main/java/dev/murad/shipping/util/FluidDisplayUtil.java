package dev.murad.shipping.util;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.capability.templates.FluidTank;

public class FluidDisplayUtil {
    public static Component getFluidDisplay(FluidTank tank) {
        Fluid fluid = tank.getFluid().getFluid();
        return fluid.equals(Fluids.EMPTY) ?
                Component.translatable("block.littlelogistics.fluid_hopper.capacity_empty", tank.getCapacity()) :
                Component.translatable("block.littlelogistics.fluid_hopper.capacity", tank.getFluid().getDisplayName().getString(),
                        tank.getFluidAmount(), tank.getCapacity());
    }
}
