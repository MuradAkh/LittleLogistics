package dev.murad.shipping.util;

import com.mojang.datafixers.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Train {
    private final Optional<LinkableEntityHead> tug;
    private LinkableEntity tail;
    private LinkableEntity head;

    public Train(LinkableEntity entity){
        head = entity;
        tail = entity;
        this.tug = entity instanceof LinkableEntityHead ? Optional.of((LinkableEntityHead) entity) : Optional.empty();
    }

    public Optional<LinkableEntityHead> getTug() {
        return tug;
    }

    public LinkableEntity getTail() {
        return tail;
    }

    public void setTail(LinkableEntity tail) {
        this.tail = tail;
    }

    public LinkableEntity getHead() {
        return head;
    }

    public List<LinkableEntity> getNonTugList(){
        if(this.head.checkNoLoopsDominated()) {
            // just in case - to avoid crashing the world.
            this.head.removeDominated();
            this.head.getDominated().map(Pair::getFirst).ifPresent(LinkableEntity::removeDominant);
            return new ArrayList<>();
        }
        return tug.map(tugEntity -> {
            List<LinkableEntity> barges = new ArrayList<>();
            for (Optional<LinkableEntity> barge = getNext(tugEntity); barge.isPresent(); barge = getNext(barge.get())){
                barges.add(barge.get());
            }
            return barges;
        }).orElse(new ArrayList<>());
    }

    public Optional<LinkableEntity> getNext(LinkableEntity entity){
        return entity.getDominated().map(Pair::getFirst).map(e -> e);
    }

    public void setHead(LinkableEntity head) {
        this.head = head;
    }
}
