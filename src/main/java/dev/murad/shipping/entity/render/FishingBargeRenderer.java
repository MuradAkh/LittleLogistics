package dev.murad.shipping.entity.render;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.barge.FishingBargeEntity;
import dev.murad.shipping.entity.models.FishingBargeDeployedModel;
import dev.murad.shipping.entity.models.FishingBargeModel;
import dev.murad.shipping.entity.models.FishingBargeTransitionModel;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.model.EntityModel;
import net.minecraft.resources.ResourceLocation;

public class FishingBargeRenderer extends VesselRenderer<FishingBargeEntity> {

    private static final ResourceLocation BARGE_TEXTURE =
            new ResourceLocation(ShippingMod.MOD_ID, "textures/entity/fishing_barge.png");

    private final EntityModel stashed = new FishingBargeModel();
    private final EntityModel transition = new FishingBargeTransitionModel();
    private final EntityModel deployed = new FishingBargeDeployedModel();

    public FishingBargeRenderer(EntityRenderDispatcher p_i46179_1_) {
        super(p_i46179_1_);
    }

    @Override
    EntityModel getModel(FishingBargeEntity entity) {
        switch (entity.getStatus()) {
            case STASHED:
                return stashed;
            case DEPLOYED:
                return deployed;
            case TRANSITION:
                return transition;
            default:
                throw new IllegalStateException("Unexpected value: " + entity.getStatus());
        }
    }

    @Override
    public ResourceLocation getTextureLocation(FishingBargeEntity p_110775_1_) {
        return BARGE_TEXTURE;
    }
}
