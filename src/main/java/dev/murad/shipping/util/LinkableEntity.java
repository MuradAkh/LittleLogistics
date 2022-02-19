package dev.murad.shipping.util;

import com.mojang.datafixers.util.Pair;
import dev.murad.shipping.entity.custom.SpringEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

public interface LinkableEntity {

    Optional<Pair<LinkableEntity, SpringEntity>> getDominated();
    Optional<Pair<LinkableEntity, SpringEntity>> getDominant();
    void setDominated(LinkableEntity entity, SpringEntity spring);
    void setDominant(LinkableEntity entity, SpringEntity spring);
    void removeDominated();
    void removeDominant();
    Train getTrain();
    boolean linkEntities(Player player, Entity target);
    void setTrain(Train train);
    boolean hasWaterOnSides();

    default void handleSpringableKill(){
        this.getDominated().map(Pair::getFirst).ifPresent(LinkableEntity::removeDominant);
        this.getDominant().map(Pair::getFirst).ifPresent(LinkableEntity::removeDominated);
    }

    default boolean checkNoLoopsDominated(){
        return checkNoLoopsHelper(this, (entity -> entity.getDominated().map(Pair::getFirst)), new HashSet<>());
    }

    default boolean checkNoLoopsDominant(){
        return checkNoLoopsHelper(this, (entity -> entity.getDominant().map(Pair::getFirst)), new HashSet<>());
    }

    default boolean checkNoLoopsHelper(LinkableEntity entity, Function<LinkableEntity, Optional<LinkableEntity>> next, Set<LinkableEntity> set){
        if(set.contains(entity)){
            return true;
        }
        set.add(entity);
        Optional<LinkableEntity> nextEntity = next.apply(entity);
        return nextEntity.map(e -> this.checkNoLoopsHelper(e, next, set)).orElse(false);
    }

    default<U> Stream<U> applyWithAll(Function<LinkableEntity, U> function){
        return this.getTrain().getHead().applyWithDominated(function);
    }

    default<U> Stream<U> applyWithDominant(Function<LinkableEntity, U> function){
        Stream<U> ofThis = Stream.of(function.apply(this));

        return checkNoLoopsDominant() ? ofThis : this.getDominant().map(dom ->
                Stream.concat(ofThis, dom.getFirst().applyWithDominant(function))
        ).orElse(ofThis);

    }

    default<U> Stream<U> applyWithDominated(Function<LinkableEntity, U> function){
        Stream<U> ofThis = Stream.of(function.apply(this));

        return checkNoLoopsDominated() ? ofThis : this.getDominated().map(dom ->
                Stream.concat(ofThis, dom.getFirst().applyWithDominated(function))
        ).orElse(ofThis);

    }

    default void tickSpringAliveCheck(){
        this.getDominant().map(Pair::getSecond).map(Entity::isAlive).ifPresent(alive -> {
            if(!alive){
                this.removeDominant();
            }
        });

        this.getDominated().map(Pair::getSecond).map(Entity::isAlive).ifPresent(alive -> {
            if(!alive){
                this.removeDominated();
            }
        });


    }
}
