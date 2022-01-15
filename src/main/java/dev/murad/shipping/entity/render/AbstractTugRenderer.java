package dev.murad.shipping.entity.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.tug.AbstractTugEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;

public abstract class AbstractTugRenderer<T extends EntityModel<AbstractTugEntity>> extends MobRenderer<AbstractTugEntity, T> {


    public AbstractTugRenderer(EntityRendererManager p_i46179_1_, T model) {
        super(p_i46179_1_, model,  0.7f);
        this.model = model;
    }

    public void render(AbstractTugEntity boatEntity, float p_225623_2_, float p_225623_3_, MatrixStack matrixStack, IRenderTypeBuffer buffer, int p_225623_6_) {
        matrixStack.pushPose();
        matrixStack.translate(0.0D, 0.375D, 0.0D);
        matrixStack.mulPose(Vector3f.YP.rotationDegrees(180.0F - p_225623_2_));
        matrixStack.scale(-1.0F, -1.0F, 1.0F);
        matrixStack.mulPose(Vector3f.YP.rotationDegrees(0.0F));

        matrixStack.popPose();
        super.render(boatEntity, p_225623_2_, p_225623_3_, matrixStack, buffer, p_225623_6_);
    }




}
