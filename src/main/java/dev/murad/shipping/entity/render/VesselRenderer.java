package dev.murad.shipping.entity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3d;
import com.mojang.math.Vector3f;
import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.VesselEntity;
import dev.murad.shipping.entity.models.ChainModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

public abstract class VesselRenderer<T extends VesselEntity> extends EntityRenderer<T> {


    private static final ResourceLocation CHAIN_TEXTURE =
            new ResourceLocation(ShippingMod.MOD_ID, "textures/entity/chain.png");

    private final ChainModel chainModel;


    public VesselRenderer(EntityRendererProvider.Context context) {
        super(context);
        chainModel = new ChainModel(context.bakeLayer(ChainModel.LAYER_LOCATION));
    }

    public void render(T vesselEntity, float p_225623_2_, float p_225623_3_, PoseStack matrixStack, MultiBufferSource buffer, int p_225623_6_) {
        matrixStack.pushPose();
        matrixStack.translate(0.0D, getModelYoffset(), 0.0D);
        matrixStack.mulPose(Vector3f.YP.rotationDegrees(180.0F - p_225623_2_));
        matrixStack.scale(-1.0F, -1.0F, 1.0F);
        matrixStack.mulPose(Vector3f.YP.rotationDegrees(getModelYrot()));
        renderModel(vesselEntity, matrixStack, buffer, p_225623_6_);
        getAndRenderChain(vesselEntity, matrixStack, buffer, p_225623_6_);
        matrixStack.popPose();

        getAndRenderLeash(vesselEntity, p_225623_2_, p_225623_3_, matrixStack, buffer, p_225623_6_);

    }

    private void renderModel(T vesselEntity, PoseStack matrixStack, MultiBufferSource buffer, int p_225623_6_) {
        VertexConsumer ivertexbuilder = buffer.getBuffer(getModel(vesselEntity).renderType(this.getTextureLocation(vesselEntity)));
        getModel(vesselEntity).renderToBuffer(matrixStack, ivertexbuilder, p_225623_6_, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    protected double getModelYoffset() {
        return 0.275D;
    }

    protected float getModelYrot() {
        return 90.0F;
    }

    private void getAndRenderChain(T bargeEntity, PoseStack matrixStack, MultiBufferSource buffer, int p_225623_6_) {
        if(bargeEntity.getDominant().isPresent()) {
            double dist = ((Entity) bargeEntity.getDominant().get().getFirst()).distanceTo(bargeEntity);
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

    private void getAndRenderLeash(T bargeEntity, float p_225623_2_, float p_225623_3_, PoseStack matrixStack, MultiBufferSource buffer, int p_225623_6_) {
        matrixStack.pushPose();
        Entity entity = bargeEntity.getLeashHolder();
        super.render(bargeEntity, p_225623_2_, p_225623_3_, matrixStack, buffer, p_225623_6_);
        if (entity != null) {
            matrixStack.pushPose();
            this.renderLeash(bargeEntity, p_225623_3_, matrixStack, buffer, entity);
            matrixStack.popPose();
        }
        matrixStack.popPose();
    }

    @Override
    public boolean shouldRender(T p_225626_1_, Frustum p_225626_2_, double p_225626_3_, double p_225626_5_, double p_225626_7_) {
        if(p_225626_1_.getDominant().isPresent()){
            if(((Entity) p_225626_1_.getDominant().get().getFirst()).shouldRender(p_225626_3_, p_225626_5_, p_225626_7_)){
                return true;
            }
            if(p_225626_1_.getDominant().get().getSecond().shouldRender(p_225626_3_, p_225626_5_, p_225626_7_)){
                return true;
            }
        }
        return super.shouldRender(p_225626_1_, p_225626_2_, p_225626_3_, p_225626_5_, p_225626_7_);
    }


    abstract EntityModel getModel(T entity);


    private <E extends Entity> void renderLeash(T p_229118_1_, float p_229118_2_, PoseStack p_229118_3_, MultiBufferSource p_229118_4_, E p_229118_5_) {
        p_229118_3_.pushPose();
        Vec3 vector3d = p_229118_5_.getRopeHoldPosition(p_229118_2_);
        double d0 = (double)(Mth.lerp(p_229118_2_, p_229118_1_.yBodyRot, p_229118_1_.yBodyRotO) * ((float)Math.PI / 180F)) + (Math.PI / 2D);
        Vec3 vector3d1 = p_229118_1_.getLeashOffset();
        double d1 = Math.cos(d0) * vector3d1.z + Math.sin(d0) * vector3d1.x;
        double d2 = Math.sin(d0) * vector3d1.z - Math.cos(d0) * vector3d1.x;
        double d3 = Mth.lerp(p_229118_2_, p_229118_1_.xo, p_229118_1_.getX()) + d1;
        double d4 = Mth.lerp(p_229118_2_, p_229118_1_.yo, p_229118_1_.getY()) + vector3d1.y;
        double d5 = Mth.lerp(p_229118_2_, p_229118_1_.zo, p_229118_1_.getZ()) + d2;
        p_229118_3_.translate(d1, vector3d1.y, d2);
        float f = (float)(vector3d.x - d3);
        float f1 = (float)(vector3d.y - d4);
        float f2 = (float)(vector3d.z - d5);
        VertexConsumer ivertexbuilder = p_229118_4_.getBuffer(RenderType.leash());
        Matrix4f matrix4f = p_229118_3_.last().pose();
        float f4 = Mth.fastInvSqrt(f * f + f2 * f2) * 0.025F / 2.0F;
        float f5 = f2 * f4;
        float f6 = f * f4;
        BlockPos blockpos = new BlockPos(p_229118_1_.getEyePosition(p_229118_2_));
        BlockPos blockpos1 = new BlockPos(p_229118_5_.getEyePosition(p_229118_2_));
        int i = this.getBlockLightLevel(p_229118_1_, blockpos);
        int k = p_229118_1_.level.getBrightness(LightLayer.SKY, blockpos);
        int l = p_229118_1_.level.getBrightness(LightLayer.SKY, blockpos1);
        renderSide(ivertexbuilder, matrix4f, f, f1, f2, i, i, k, l, 0.025F, 0.025F, f5, f6);
        renderSide(ivertexbuilder, matrix4f, f, f1, f2, i, i, k, l, 0.025F, 0.0F, f5, f6);
        p_229118_3_.popPose();
    }

    /*
        Shamelessly stolen from LuaX's fabric port of this mod
     */
    public static void renderSide(VertexConsumer p_229119_0_, Matrix4f p_229119_1_, float p_229119_2_, float p_229119_3_, float p_229119_4_, int p_229119_5_, int p_229119_6_, int p_229119_7_, int p_229119_8_, float p_229119_9_, float p_229119_10_, float p_229119_11_, float p_229119_12_) {
        int i = 24;

        for(int j = 0; j < 24; ++j) {
            float f = (float)j / 23.0F;
            int k = (int)Mth.lerp(f, (float)p_229119_5_, (float)p_229119_6_);
            int l = (int)Mth.lerp(f, (float)p_229119_7_, (float)p_229119_8_);
            int i1 = LightTexture.pack(k, l);
            addVertexPair(p_229119_0_, p_229119_1_, i1, p_229119_2_, p_229119_3_, p_229119_4_, p_229119_9_, p_229119_10_, 24, j, false, p_229119_11_, p_229119_12_);
            addVertexPair(p_229119_0_, p_229119_1_, i1, p_229119_2_, p_229119_3_, p_229119_4_, p_229119_9_, p_229119_10_, 24, j + 1, true, p_229119_11_, p_229119_12_);
        }

    }
    public static void addVertexPair(VertexConsumer p_229120_0_, Matrix4f p_229120_1_, int lightUV, float p_229120_3_, float p_229120_4_, float p_229120_5_, float p_229120_6_, float p_229120_7_, int p_229120_8_, int p_229120_9_, boolean p_229120_10_, float p_229120_11_, float p_229120_12_) {
        float f = 0.5F;
        float f1 = 0.4F;
        float f2 = 0.3F;
        if (p_229120_9_ % 2 == 0) {
            f *= 0.7F;
            f1 *= 0.7F;
            f2 *= 0.7F;
        }

        float f3 = (float)p_229120_9_ / (float)p_229120_8_;
        float f4 = p_229120_3_ * f3;
        float f5 = p_229120_4_ > 0.0F ? p_229120_4_ * f3 * f3 : p_229120_4_ - p_229120_4_ * (1.0F - f3) * (1.0F - f3);
        float f6 = p_229120_5_ * f3;
        if (!p_229120_10_) {
            p_229120_0_.vertex(p_229120_1_, f4 + p_229120_11_, f5 + p_229120_6_ - p_229120_7_, f6 - p_229120_12_).color(f, f1, f2, 1.0F).uv2(lightUV).endVertex();
        }

        p_229120_0_.vertex(p_229120_1_, f4 - p_229120_11_, f5 + p_229120_7_, f6 + p_229120_12_).color(f, f1, f2, 1.0F).uv2(lightUV).endVertex();
        if (p_229120_10_) {
            p_229120_0_.vertex(p_229120_1_, f4 + p_229120_11_, f5 + p_229120_6_ - p_229120_7_, f6 - p_229120_12_).color(f, f1, f2, 1.0F).uv2(lightUV).endVertex();
        }

    }
}
