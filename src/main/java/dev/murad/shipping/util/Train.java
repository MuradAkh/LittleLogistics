package dev.murad.shipping.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class Train <V extends LinkableEntity<V>> {
    private final Optional<V> tug;
    private V tail;
    private V head;

    public Train(V entity){
        head = entity;
        tail = entity;
        this.tug = entity instanceof LinkableEntityHead ? Optional.of(entity) : Optional.empty();
    }

    public Optional<V> getTug() {
        return tug;
    }

    public V getTail() {
        return tail;
    }

    public void setTail(V tail) {
        this.tail = tail;
    }

    public V getHead() {
        return head;
    }

    public List<V> asListOfTugged(){
        if(this.head.checkNoLoopsDominated()) {
            // just in case - to avoid crashing the world.
            this.head.removeDominated();
            this.head.getDominated().ifPresent(LinkableEntity::removeDominant);
            return new ArrayList<>();
        }
        return tug.map(tugEntity -> {
            List<V> barges = new ArrayList<>();
            for (Optional<V> barge = getNext(tugEntity); barge.isPresent(); barge = getNext(barge.get())){
                barges.add(barge.get());
            }
            return barges;
        }).orElse(new ArrayList<>());
    }

    public List<V> asList(){
        if(this.head.checkNoLoopsDominated()) {
            // just in case - to avoid crashing the world.
            this.head.removeDominated();
            this.head.getDominated().ifPresent(LinkableEntity::removeDominant);
            return new ArrayList<>();
        }

        List<V> barges = new ArrayList<>();
        for (Optional<V> barge = Optional.of(head); barge.isPresent(); barge = getNext(barge.get())){
            barges.add(barge.get());
        }
        return barges;
    }

    public Optional<V> getNext(V entity){
        return entity.getDominated().map(t -> (V) t);
    }

    public void setHead(V head) {
        this.head = head;
    }

    public Stream<V> asStream() {
        //noinspection OptionalGetWithoutIsPresent
        return Stream.iterate(head, v -> v.getDominated().isPresent(), v -> v.getDominated().get());
    }
}
