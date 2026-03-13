package dev.murad.shipping.block;

import dev.murad.shipping.util.LinkableEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.EntityCapability;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public interface IVesselLoader {
    enum Mode {
        EXPORT,
        IMPORT
    }

    static <T> Optional<T> getEntityCapability(BlockPos pos, EntityCapability<T, @Nullable Void> capability, Level level){
        List<Entity> entities = level.getEntities((Entity) null,
                getSearchBox(pos),
                (e -> entityPredicate(e, pos, capability))
        );

        if(entities.isEmpty()){
            return Optional.empty();
        } else {
            Entity entity = entities.get(0);
            return Optional.ofNullable(entity.getCapability(capability, null));
        }
    }

    static <T> boolean entityPredicate(Entity entity, BlockPos pos, EntityCapability<T, @Nullable Void> capability) {
        T cap = entity.getCapability(capability, null);
        if (cap == null) return false;
        if (entity instanceof LinkableEntity<?> l) {
            return l.allowDockInterface() && (l.getBlockPos().getX() == pos.getX() && l.getBlockPos().getZ() == pos.getZ());
        }
        return true;
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

    <T extends Entity & LinkableEntity<T>> boolean hold(T vehicle, Mode mode);

}
