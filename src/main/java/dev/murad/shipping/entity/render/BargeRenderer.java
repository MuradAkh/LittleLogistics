package dev.murad.shipping.entity.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.BargeEntity;
import dev.murad.shipping.entity.models.BargeModel;
import dev.murad.shipping.entity.models.ChainModel;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;

public class BargeRenderer extends EntityRenderer<BargeEntity> {
    private static final ResourceLocation BARGE_TEXTURE =
            new ResourceLocation(ShippingMod.MOD_ID, "textures/entity/barge.png");

    private static final ResourceLocation CHAIN_TEXTURE =
            new ResourceLocation(ShippingMod.MOD_ID, "textures/entity/chain.png");

    private static BargeModel model = new BargeModel();
    private static ChainModel chainModel = new ChainModel();


    public BargeRenderer(EntityRendererManager p_i46179_1_) {
        super(p_i46179_1_);
    }

    public void render(BargeEntity boatEntity, float p_225623_2_, float p_225623_3_, MatrixStack matrixStack, IRenderTypeBuffer buffer, int p_225623_6_) {
        matrixStack.pushPose();
        matrixStack.translate(0.0D, 0.375D, 0.0D);
        matrixStack.mulPose(Vector3f.YP.rotationDegrees(180.0F - p_225623_2_));


        matrixStack.scale(-1.0F, -1.0F, 1.0F);
        matrixStack.mulPose(Vector3f.YP.rotationDegrees(90.0F));

        IVertexBuilder ivertexbuilder = buffer.getBuffer(model.renderType(this.getTextureLocation(boatEntity)));
        model.renderToBuffer(matrixStack, ivertexbuilder, p_225623_6_, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        if(boatEntity.getDominant().isPresent()) {
            IVertexBuilder ivertexbuilderChain = buffer.getBuffer(model.renderType(CHAIN_TEXTURE));
            chainModel.renderToBuffer(matrixStack, ivertexbuilderChain, p_225623_6_, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        }

        matrixStack.popPose();
        super.render(boatEntity, p_225623_2_, p_225623_3_, matrixStack, buffer, p_225623_6_);
    }

    @Override
    public ResourceLocation getTextureLocation(BargeEntity entity) {
        return BARGE_TEXTURE;
    }


}
