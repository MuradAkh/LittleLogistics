package dev.murad.shipping.entity.render;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.barge.ChunkLoaderBargeEntity;
import dev.murad.shipping.entity.models.ChunkLoaderBargeModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class ChunkLoaderBargeRenderer extends VesselRenderer<ChunkLoaderBargeEntity> {
    private static final ResourceLocation BARGE_TEXTURE =
            new ResourceLocation(ShippingMod.MOD_ID, "textures/entity/chunk_loader_barge.png");

    private final EntityModel model;


    public ChunkLoaderBargeRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new ChunkLoaderBargeModel(context.bakeLayer(ChunkLoaderBargeModel.LAYER_LOCATION));
    }

    @Override
    public ResourceLocation getTextureLocation(ChunkLoaderBargeEntity entity) {
        return BARGE_TEXTURE;
    }

    @Override
    public EntityModel getModel(ChunkLoaderBargeEntity entity) {
        return model;
    }
}
