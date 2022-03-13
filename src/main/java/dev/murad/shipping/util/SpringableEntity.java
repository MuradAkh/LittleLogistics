package dev.murad.shipping.util;


import dev.murad.shipping.entity.custom.SpringEntity;
import dev.murad.shipping.entity.custom.VesselEntity;
import net.minecraft.world.entity.Entity;

import java.util.Optional;


public interface SpringableEntity extends LinkableEntity<VesselEntity> {
    void setDominatedSpring(SpringEntity s);
    void setDominantSpring(SpringEntity s);

    Optional<SpringEntity> getDominatedSpring();
    Optional<SpringEntity> getDominantSpring();


    @Override
    default void handleShearsCut() {
        getDominantSpring().ifPresent(Entity::kill);
    }

    default void tickSpringAliveCheck(){
        this.getDominantSpring().map(Entity::isAlive).ifPresent(alive -> {
            if(!alive){
                this.removeDominant();
            }
        });

        this.getDominatedSpring().map(Entity::isAlive).ifPresent(alive -> {
            if(!alive){
                this.removeDominated();
            }
        });
    }

    default void setDominated(VesselEntity entity, SpringEntity spring) {
        setDominated(entity);
        setDominatedSpring(spring);
    }

    default void setDominant(VesselEntity entity, SpringEntity spring) {
        setDominant(entity);
        setDominantSpring(spring);
    }

}
