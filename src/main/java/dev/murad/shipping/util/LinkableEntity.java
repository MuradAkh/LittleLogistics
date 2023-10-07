package dev.murad.shipping.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

public interface LinkableEntity<V extends LinkableEntity<V>> {

    Optional<V> getFollower();
    Optional<V> getLeader();
    void setDominated(V entity);
    void setDominant(V entity);
    void removeDominated();
    void removeDominant();
    void handleShearsCut();
    Train<V> getTrain();
    boolean linkEntities(Player player, Entity target);
    void setTrain(Train<V> train);
    boolean hasWaterOnSides();

    default void handleLinkableKill(){
        this.getFollower().ifPresent(LinkableEntity::removeDominant);
        this.getLeader().ifPresent(LinkableEntity::removeDominated);
    }

    default boolean checkNoLoopsDominated(){
        return checkNoLoopsHelper(this, (LinkableEntity::getFollower), new HashSet<>());
    }

    default boolean checkNoLoopsDominant(){
        return checkNoLoopsHelper(this, (LinkableEntity::getLeader), new HashSet<>());
    }

    default boolean checkNoLoopsHelper(LinkableEntity<V> entity, Function<LinkableEntity<V>, Optional<V>> next, Set<LinkableEntity<V>> set){
        if(set.contains(entity)){
            return true;
        }
        set.add(entity);
        Optional<V> nextEntity = next.apply(entity);
        return nextEntity.map(e -> this.checkNoLoopsHelper(e, next, set)).orElse(false);
    }

    default<U> Stream<U> applyWithAll(Function<LinkableEntity<V>, U> function){
        return this.getTrain().getHead().applyWithDominated(function);
    }

    default<U> Stream<U> applyWithDominant(Function<LinkableEntity<V>, U> function){
        Stream<U> ofThis = Stream.of(function.apply(this));

        return checkNoLoopsDominant() ? ofThis : this.getLeader().map(dom ->
                Stream.concat(ofThis, dom.applyWithDominant(function))
        ).orElse(ofThis);

    }

    default<U> Stream<U> applyWithDominated(Function<LinkableEntity<V>, U> function){
        Stream<U> ofThis = Stream.of(function.apply(this));

        return checkNoLoopsDominated() ? ofThis : this.getFollower().map(dom ->
                Stream.concat(ofThis, dom.applyWithDominated(function))
        ).orElse(ofThis);

    }

    boolean allowDockInterface();

    BlockPos getBlockPos();

    enum LinkSide {
        DOMINANT, DOMINATED
    }
}
