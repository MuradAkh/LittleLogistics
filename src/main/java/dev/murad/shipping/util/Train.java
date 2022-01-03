package dev.murad.shipping.util;

import dev.murad.shipping.entity.custom.barge.AbstractBargeEntity;
import dev.murad.shipping.entity.custom.ISpringableEntity;
import dev.murad.shipping.entity.custom.tug.AbstractTugEntity;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Train {
    private final Optional<AbstractTugEntity> tug;
    private ISpringableEntity tail;
    private ISpringableEntity head;

    public Train(ISpringableEntity entity){
        head = entity;
        tail = entity;
        this.tug = entity instanceof AbstractTugEntity ? Optional.of((AbstractTugEntity) entity) : Optional.empty();
    }

    public Optional<AbstractTugEntity> getTug() {
        return tug;
    }

    public ISpringableEntity getTail() {
        return tail;
    }

    public void setTail(ISpringableEntity tail) {
        this.tail = tail;
    }

    public ISpringableEntity getHead() {
        return head;
    }

    public List<AbstractBargeEntity> getBarges(){
        return tug.map(tugEntity -> {
            List<AbstractBargeEntity> barges = new ArrayList<>();
            for (Optional<AbstractBargeEntity> barge = getNextBarge(tugEntity); barge.isPresent(); barge = getNextBarge(barge.get())){
                barges.add(barge.get());
            }
            return barges;
        }).orElse(new ArrayList<>());
    }

    public Optional<AbstractBargeEntity> getNextBarge(ISpringableEntity entity){
        return entity.getDominated().map(Pair::getKey).map(e -> (AbstractBargeEntity) e);
    }

    public void setHead(ISpringableEntity head) {
        this.head = head;
    }
}
