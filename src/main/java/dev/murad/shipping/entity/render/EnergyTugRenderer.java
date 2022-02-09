package dev.murad.shipping.entity.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.VesselEntity;
import dev.murad.shipping.entity.custom.tug.AbstractTugEntity;
import dev.murad.shipping.entity.custom.tug.EnergyTugEntity;
import dev.murad.shipping.entity.models.EnergyTugModel;
import dev.murad.shipping.entity.models.SteamTugModel;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.util.ResourceLocation;

public class EnergyTugRenderer extends VesselRenderer<EnergyTugEntity> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ShippingMod.MOD_ID, "textures/entity/energy_tug.png");

    private final EntityModel model = new EnergyTugModel();

    public EnergyTugRenderer(EntityRendererManager p_i46179_1_) {
        super(p_i46179_1_);
    }

    @Override
    public void render(EnergyTugEntity boatEntity, float p_225623_2_, float p_225623_3_, MatrixStack matrixStack, IRenderTypeBuffer buffer, int p_225623_6_) {
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
