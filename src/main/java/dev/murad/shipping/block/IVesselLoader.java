package dev.murad.shipping.block;

import dev.murad.shipping.block.fluid.FluidHopperTileEntity;
import dev.murad.shipping.entity.custom.VesselEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;

import java.util.List;
import java.util.Optional;

public interface IVesselLoader {
    enum Mode {
        EXPORT,
        IMPORT
    }

    static <T> Optional<T> getEntityCapability(BlockPos pos, Capability<T> capability, World level){
        List<Entity> fluidEntities = level.getEntities((Entity) null,
                getSearchBox(pos),
                (e -> entityPredicate(e, pos, capability))
        );

        if(fluidEntities.isEmpty()){
            return Optional.empty();
        } else {
            Entity entity = fluidEntities.get(0);
            return entity.getCapability(capability).resolve();
        }
    }

    static boolean entityPredicate(Entity entity, BlockPos pos, Capability<?> capability) {
        return entity.getCapability(capability).resolve().map(cap -> {
            if (entity instanceof VesselEntity){
                VesselEntity vessel = (VesselEntity) entity;
                return vessel.allowDockInterface() && (vessel.getBlockPos().getX() == pos.getX() && vessel.getBlockPos().getZ() == pos.getZ());
            } else {
                return true;
            }
        }).orElse(false);
    }

    static AxisAlignedBB getSearchBox(BlockPos pos) {
        return new AxisAlignedBB(
                pos.getX() - 0.5D,
                pos.getY() - 0.5D,
                pos.getZ() - 0.5D,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D);
    }

    boolean holdVessel(VesselEntity vessel, Mode mode);
}
