package dev.murad.shipping.entity.custom;

import javafx.util.Pair;

import java.util.Optional;

public interface ISpringableEntity {

    public Optional<Pair<ISpringableEntity, SpringEntity>> getDominated();

    public void dominate(ISpringableEntity entity, SpringEntity spring);

    public void unDominate();
}
