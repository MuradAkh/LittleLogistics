package dev.murad.shipping.entity.custom;

import dev.murad.shipping.util.Train;
import javafx.util.Pair;

import java.util.Optional;

public interface ISpringableEntity {

    Optional<Pair<ISpringableEntity, SpringEntity>> getDominated();
    Optional<Pair<ISpringableEntity, SpringEntity>> getDominant();
    void setDominated(ISpringableEntity entity, SpringEntity spring);
    void setDominant(ISpringableEntity entity, SpringEntity spring);
    void removeDominated();
    void removeDominant();
    Train getTrain();
    void setTrain(Train train);

    default void handleSpringableKill(){
        this.getDominated().flatMap(pair -> Optional.of(pair.getKey())).ifPresent(dominated -> {
            dominated.setTrain(new Train(dominated));
            dominated.removeDominant();
        });
        this.getDominant().flatMap(pair -> Optional.of(pair.getKey())).ifPresent(ISpringableEntity::removeDominated);
    }
}
