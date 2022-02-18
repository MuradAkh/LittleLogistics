package dev.murad.shipping.block;

import dev.murad.shipping.entity.custom.VesselEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;

import java.util.List;
import java.util.Optional;

public interface IVesselLoader {
    enum Mode {
        EXPORT,
        IMPORT
    }

    static <T> Optional<T> getEntityCapability(BlockPos pos, Capability<T> capability, Level level){
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

    static AABB getSearchBox(BlockPos pos) {
        return new AABB(
                pos.getX() ,
                pos.getY(),
                pos.getZ(),
                pos.getX() + 1D,
                pos.getY() + 1D,
                pos.getZ() + 1D);
    }

    boolean holdVessel(VesselEntity vessel, Mode mode);
}
