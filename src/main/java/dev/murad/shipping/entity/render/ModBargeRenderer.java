package dev.murad.shipping.entity.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.ModBargeEntity;
import dev.murad.shipping.entity.models.BargeChainModel;
import dev.murad.shipping.entity.models.BargeModel;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.BoatEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3f;

public class ModBargeRenderer extends EntityRenderer<ModBargeEntity> {
    private static final ResourceLocation BOAT_TEXTURE =
            new ResourceLocation(ShippingMod.MOD_ID, "textures/entity/barge.png");

    private static final ResourceLocation CHAIN_TEXTURE =
            new ResourceLocation(ShippingMod.MOD_ID, "textures/entity/barge_chain.png");

    private static BargeModel model = new BargeModel();
    private static BargeChainModel chainModel = new BargeChainModel();

    private EntityModel<ModBargeEntity> getModel(ModBargeEntity entity){
        if(entity.getDominant().isPresent()){
            return chainModel;
        }else {
            return model;
        }
    }

    public ModBargeRenderer(EntityRendererManager p_i46179_1_) {
        super(p_i46179_1_);
    }

    public void render(ModBargeEntity boatEntity, float p_225623_2_, float p_225623_3_, MatrixStack matrixStack, IRenderTypeBuffer buffer, int p_225623_6_) {
        matrixStack.pushPose();
        matrixStack.translate(0.0D, 0.375D, 0.0D);
        matrixStack.mulPose(Vector3f.YP.rotationDegrees(180.0F - p_225623_2_));


        matrixStack.scale(-1.0F, -1.0F, 1.0F);
        matrixStack.mulPose(Vector3f.YP.rotationDegrees(90.0F));

        IVertexBuilder ivertexbuilder = buffer.getBuffer(getModel(boatEntity).renderType(this.getTextureLocation(boatEntity)));
        getModel(boatEntity).renderToBuffer(matrixStack, ivertexbuilder, p_225623_6_, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

        matrixStack.popPose();
        super.render(boatEntity, p_225623_2_, p_225623_3_, matrixStack, buffer, p_225623_6_);
    }

    @Override
    public ResourceLocation getTextureLocation(ModBargeEntity entity) {
        if(entity.getDominant().isPresent()){
            return CHAIN_TEXTURE;
        }else {
            return BOAT_TEXTURE;
        }
    }


}
