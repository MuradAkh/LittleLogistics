package dev.murad.shipping.entity.custom;

import dev.murad.shipping.entity.custom.tug.AbstractTugEntity;
import dev.murad.shipping.util.Train;
import javafx.util.Pair;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public interface ISpringableEntity {

    Optional<Pair<ISpringableEntity, SpringEntity>> getDominated();
    Optional<Pair<ISpringableEntity, SpringEntity>> getDominant();
    void setDominated(ISpringableEntity entity, SpringEntity spring);
    void setDominant(ISpringableEntity entity, SpringEntity spring);
    void removeDominated();
    void removeDominant();
    Train getTrain();
    void setTrain(Train train);
    boolean hasWaterOnSides();

    default void handleSpringableKill(){
        this.getDominated().flatMap(pair -> Optional.of(pair.getKey())).ifPresent(dominated -> {
            dominated.removeDominant();
        });
        this.getDominant().flatMap(pair -> Optional.of(pair.getKey())).ifPresent(ISpringableEntity::removeDominated);
    }

    default<U> Stream<U> applyWithDominated(Function<ISpringableEntity, U> function){
        Stream<U> ofThis = Stream.of(function.apply(this));
        return this.getDominated().map(dom -> Stream.concat(ofThis, dom.getKey().applyWithDominated(function))).orElse(ofThis);
    }

    default<U> Stream<U> applyWithAll(Function<ISpringableEntity, U> function){
        return this.getTrain().getHead().applyWithDominated(function);
    }

    default<U> Stream<U> applyWithDominant(Function<ISpringableEntity, U> function){
        Stream<U> ofThis = Stream.of(function.apply(this));
        return this.getDominant().map(dom -> Stream.concat(ofThis, dom.getKey().applyWithDominant(function))).orElse(ofThis);
    }
}
