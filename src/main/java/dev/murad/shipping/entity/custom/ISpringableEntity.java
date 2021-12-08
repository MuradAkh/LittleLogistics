package dev.murad.shipping.entity.custom;

import dev.murad.shipping.util.Train;
import javafx.util.Pair;

import javax.swing.text.html.Option;
import java.util.Optional;

public interface ISpringableEntity {

    public Optional<Pair<ISpringableEntity, SpringEntity>> getDominated();
    public Optional<Pair<ISpringableEntity, SpringEntity>> getDominant();

    public void setDominated(ISpringableEntity entity, SpringEntity spring);

    public void setDominant(ISpringableEntity entity, SpringEntity spring);

    public void removeDominated();

    public void removeDominant();

    public Train getTrain();
    public void setTrain(Train train);

    default void handleKill(){
        this.getDominated().flatMap(pair -> Optional.of(pair.getKey())).ifPresent(dominated -> {
            dominated.setTrain(new Train(dominated));
            dominated.removeDominant();
        });
        this.getDominant().flatMap(pair -> Optional.of(pair.getKey())).ifPresent(ISpringableEntity::removeDominated);
    }
}
