package dev.murad.shipping.entity.render;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public record ModelPack<T extends Entity> (
        ModelSupplier<T> supplier,
        ModelLayerLocation location,
        ResourceLocation texture) {
}