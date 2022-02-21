package dev.murad.shipping.entity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.TrainCar;
import dev.murad.shipping.entity.models.ChainModel;
import dev.murad.shipping.entity.models.ChestBargeModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class TrainCarRenderer extends EntityRenderer<TrainCar> {
    private static final ResourceLocation BARGE_TEXTURE =
            new ResourceLocation(ShippingMod.MOD_ID, "textures/entity/barge.png");

    private static final ResourceLocation CHAIN_TEXTURE =
            new ResourceLocation(ShippingMod.MOD_ID, "textures/entity/chain.png");

    private final ChainModel chainModel;
    private final EntityModel model;


    public TrainCarRenderer(EntityRendererProvider.Context context) {
        super(context);
        chainModel = new ChainModel(context.bakeLayer(ChainModel.LAYER_LOCATION));
        model = new ChestBargeModel(context.bakeLayer(ChestBargeModel.LAYER_LOCATION));

    }

    public void render(TrainCar vesselEntity, float p_225623_2_, float p_225623_3_, PoseStack matrixStack, MultiBufferSource buffer, int p_225623_6_) {
        matrixStack.pushPose();
        matrixStack.translate(0.0D, 0, 0.0D);
        matrixStack.mulPose(Vector3f.YP.rotationDegrees(180.0F - p_225623_2_));
        matrixStack.scale(-1.0F, -1.0F, 1.0F);
        matrixStack.mulPose(Vector3f.YP.rotationDegrees(90));
        renderModel(vesselEntity, matrixStack, buffer, p_225623_6_);
        getAndRenderChain(vesselEntity, matrixStack, buffer, p_225623_6_);
        matrixStack.popPose();
    }

    private void getAndRenderChain(TrainCar bargeEntity, PoseStack matrixStack, MultiBufferSource buffer, int p_225623_6_) {
        if(bargeEntity.getDominant().isPresent()) {
            double dist = ((Entity) bargeEntity.getDominant().get()).distanceTo(bargeEntity);
            VertexConsumer ivertexbuilderChain = buffer.getBuffer(chainModel.renderType(CHAIN_TEXTURE));
            int segments = (int) Math.ceil(dist * 4);
            matrixStack.pushPose();
            for (int i = 0; i < segments; i++) {
                matrixStack.pushPose();
                matrixStack.translate(i / 4.0, 0, 0);
                chainModel.renderToBuffer(matrixStack, ivertexbuilderChain, p_225623_6_, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
                matrixStack.popPose();
            }
            matrixStack.popPose();
        }
    }

    @Override
    public ResourceLocation getTextureLocation(TrainCar pEntity) {
        return BARGE_TEXTURE;
    }

    private void renderModel(TrainCar vesselEntity, PoseStack matrixStack, MultiBufferSource buffer, int p_225623_6_) {
        VertexConsumer ivertexbuilder = buffer.getBuffer(getModel(vesselEntity).renderType(this.getTextureLocation(vesselEntity)));
        getModel(vesselEntity).renderToBuffer(matrixStack, ivertexbuilder, p_225623_6_, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    private Model getModel(TrainCar vesselEntity) {
        return model;
    }
}
