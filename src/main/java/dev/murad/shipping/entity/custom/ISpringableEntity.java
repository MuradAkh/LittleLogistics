package dev.murad.shipping.entity.custom;

import javafx.util.Pair;

import java.util.Optional;

public interface ISpringableEntity {

    public Optional<Pair<ISpringableEntity, SpringEntity>> getDominated();
    public Optional<Pair<ISpringableEntity, SpringEntity>> getDominant();

    public void setDominated(ISpringableEntity entity, SpringEntity spring);

    public void setDominant(ISpringableEntity entity, SpringEntity spring);

    public void removeDominated();

    public void removeDominant();
}
