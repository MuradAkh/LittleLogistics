package dev.murad.shipping.entity.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;

@FunctionalInterface
public interface ModelSupplier<T extends Entity> {
    EntityModel<T> supply(ModelPart root);
}