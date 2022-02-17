package dev.murad.shipping.entity.render;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.tug.SteamTugEntity;
import dev.murad.shipping.entity.models.SteamTugModel;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.model.EntityModel;
import net.minecraft.resources.ResourceLocation;

public class SteamTugRenderer extends VesselRenderer<SteamTugEntity> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ShippingMod.MOD_ID, "textures/entity/tug.png");


    private final EntityModel model = new SteamTugModel();

    public SteamTugRenderer(EntityRenderDispatcher p_i46179_1_) {
        super(p_i46179_1_);
    }

    @Override
    public ResourceLocation getTextureLocation(SteamTugEntity p_110775_1_) {
        return TEXTURE;
    }

    @Override
    EntityModel getModel(SteamTugEntity entity) {
        return model;
    }

    @Override
    protected double getModelYoffset() {
        return 1.45D;
    }

    @Override
    protected float getModelYrot() {
        return 0;
    }

}
