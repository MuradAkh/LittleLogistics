package dev.murad.shipping.entity.render;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.barge.SeaterBargeEntity;
import dev.murad.shipping.entity.models.SeaterBargeModel;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.util.ResourceLocation;

public class SeaterBargeRenderer extends VesselRenderer<SeaterBargeEntity> {
    private static final ResourceLocation BARGE_TEXTURE =
            new ResourceLocation(ShippingMod.MOD_ID, "textures/entity/seater_barge.png");

    private final EntityModel model = new SeaterBargeModel();


    public SeaterBargeRenderer(EntityRendererManager p_i46179_1_) {
        super(p_i46179_1_);
    }

    @Override
    public ResourceLocation getTextureLocation(SeaterBargeEntity entity) {
        return BARGE_TEXTURE;
    }

    @Override
    public EntityModel getModel(SeaterBargeEntity entity) {
        return model;
    }
}
