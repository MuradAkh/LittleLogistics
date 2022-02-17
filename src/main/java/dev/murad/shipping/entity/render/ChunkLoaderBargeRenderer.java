package dev.murad.shipping.entity.render;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.barge.ChunkLoaderBargeEntity;
import dev.murad.shipping.entity.models.ChunkLoaderBargeModel;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.model.EntityModel;
import net.minecraft.resources.ResourceLocation;

public class ChunkLoaderBargeRenderer extends VesselRenderer<ChunkLoaderBargeEntity> {
    private static final ResourceLocation BARGE_TEXTURE =
            new ResourceLocation(ShippingMod.MOD_ID, "textures/entity/chunk_loader_barge.png");

    private final EntityModel model = new ChunkLoaderBargeModel();


    public ChunkLoaderBargeRenderer(EntityRenderDispatcher p_i46179_1_) {
        super(p_i46179_1_);
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
