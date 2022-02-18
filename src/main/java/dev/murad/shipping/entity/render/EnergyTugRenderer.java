package dev.murad.shipping.entity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.tug.EnergyTugEntity;
import dev.murad.shipping.entity.models.EnergyTugModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class EnergyTugRenderer extends VesselRenderer<EnergyTugEntity> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ShippingMod.MOD_ID, "textures/entity/energy_tug.png");

    private final EntityModel model;

    public EnergyTugRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new EnergyTugModel(context.bakeLayer(EnergyTugModel.LAYER_LOCATION));
    }

    @Override
    public void render(EnergyTugEntity boatEntity, float p_225623_2_, float p_225623_3_, PoseStack matrixStack, MultiBufferSource buffer, int p_225623_6_) {
        super.render(boatEntity, p_225623_2_, p_225623_3_, matrixStack, buffer, p_225623_6_);
    }

    @Override
    public ResourceLocation getTextureLocation(EnergyTugEntity p_110775_1_) {
        return TEXTURE;
    }

    @Override
    protected double getModelYoffset() {
        return 1.55D;
    }

    @Override
    protected float getModelYrot() {
        return 0.0F;
    }

    @Override
    EntityModel getModel(EnergyTugEntity entity) {
        return model;
    }
}
