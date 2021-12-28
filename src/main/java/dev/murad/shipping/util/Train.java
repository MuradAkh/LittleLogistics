package dev.murad.shipping.util;

import dev.murad.shipping.entity.custom.ISpringableEntity;
import dev.murad.shipping.entity.custom.tug.TugEntity;

import java.util.Optional;

public class Train {
    private Optional<TugEntity> tug;
    private ISpringableEntity tail;
    private ISpringableEntity head;

    public Train(ISpringableEntity entity){
        head = entity;
        tail = entity;
        this.tug = entity instanceof TugEntity ? Optional.of((TugEntity) entity) : Optional.empty();
    }

    public Optional<TugEntity> getTug() {
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

    public void setHead(ISpringableEntity head) {
        this.head = head;
    }
}
