package dev.murad.shipping.entity.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.barge.AbstractBargeEntity;
import dev.murad.shipping.entity.models.ChainExtendedModel;
import dev.murad.shipping.entity.models.ChainModel;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;

public abstract class AbstractBargeRenderer<T extends AbstractBargeEntity> extends EntityRenderer<T> {


    private static final ResourceLocation CHAIN_TEXTURE =
            new ResourceLocation(ShippingMod.MOD_ID, "textures/entity/chain.png");

    private static ChainModel chainModel = new ChainModel();
    private static ChainExtendedModel chainExtendedModel = new ChainExtendedModel();


    public AbstractBargeRenderer(EntityRendererManager p_i46179_1_) {
        super(p_i46179_1_);
    }

    public void render(T bargeEntity, float p_225623_2_, float p_225623_3_, MatrixStack matrixStack, IRenderTypeBuffer buffer, int p_225623_6_) {
        matrixStack.pushPose();
        matrixStack.translate(0.0D, 0.375D, 0.0D);
        matrixStack.mulPose(Vector3f.YP.rotationDegrees(180.0F - p_225623_2_));


        matrixStack.scale(-1.0F, -1.0F, 1.0F);
        matrixStack.mulPose(Vector3f.YP.rotationDegrees(90.0F));

        IVertexBuilder ivertexbuilder = buffer.getBuffer(getModel(bargeEntity).renderType(this.getTextureLocation(bargeEntity)));
        getModel(bargeEntity).renderToBuffer(matrixStack, ivertexbuilder, p_225623_6_, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        if(bargeEntity.getDominant().isPresent()) {
            double dist = ((Entity)bargeEntity.getDominant().get().getFirst()).distanceToSqr(bargeEntity);
            if (dist > 1.1) {
                IVertexBuilder ivertexbuilderChain = buffer.getBuffer(chainModel.renderType(CHAIN_TEXTURE));
                chainModel.renderToBuffer(matrixStack, ivertexbuilderChain, p_225623_6_, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            }
            if (dist > 3.7){
                IVertexBuilder ivertexbuilderChainE = buffer.getBuffer(chainExtendedModel.renderType(CHAIN_TEXTURE));
                chainExtendedModel.renderToBuffer(matrixStack, ivertexbuilderChainE, p_225623_6_, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            }

        }

        matrixStack.popPose();
        super.render(bargeEntity, p_225623_2_, p_225623_3_, matrixStack, buffer, p_225623_6_);
    }

    abstract EntityModel getModel(T entity);


}
