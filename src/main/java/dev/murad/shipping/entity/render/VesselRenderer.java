package dev.murad.shipping.entity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.VesselEntity;
import dev.murad.shipping.entity.models.ChainModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.Entity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.world.LightType;

public abstract class VesselRenderer<T extends VesselEntity> extends EntityRenderer<T> {


    private static final ResourceLocation CHAIN_TEXTURE =
            new ResourceLocation(ShippingMod.MOD_ID, "textures/entity/chain.png");

    private static final ChainModel chainModel = new ChainModel();


    public VesselRenderer(EntityRenderDispatcher p_i46179_1_) {
        super(p_i46179_1_);
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

    private void renderModel(T vesselEntity, MatrixStack matrixStack, IRenderTypeBuffer buffer, int p_225623_6_) {
        IVertexBuilder ivertexbuilder = buffer.getBuffer(getModel(vesselEntity).renderType(this.getTextureLocation(vesselEntity)));
        getModel(vesselEntity).renderToBuffer(matrixStack, ivertexbuilder, p_225623_6_, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    protected double getModelYoffset() {
        return 0.275D;
    }

    protected float getModelYrot() {
        return 90.0F;
    }

    private void getAndRenderChain(T bargeEntity, MatrixStack matrixStack, IRenderTypeBuffer buffer, int p_225623_6_) {
        if(bargeEntity.getDominant().isPresent()) {
            double dist = ((Entity) bargeEntity.getDominant().get().getFirst()).distanceTo(bargeEntity);
            IVertexBuilder ivertexbuilderChain = buffer.getBuffer(chainModel.renderType(CHAIN_TEXTURE));
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

    private void getAndRenderLeash(T bargeEntity, float p_225623_2_, float p_225623_3_, MatrixStack matrixStack, IRenderTypeBuffer buffer, int p_225623_6_) {
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
    public boolean shouldRender(T p_225626_1_, ClippingHelper p_225626_2_, double p_225626_3_, double p_225626_5_, double p_225626_7_) {
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

    private <E extends Entity> void renderLeash(T p_229118_1_, float p_229118_2_, MatrixStack p_229118_3_, IRenderTypeBuffer p_229118_4_, E p_229118_5_) {
        p_229118_3_.pushPose();
        Vector3d vector3d = p_229118_5_.getRopeHoldPosition(p_229118_2_);
        double d0 = (double)(MathHelper.lerp(p_229118_2_, p_229118_1_.yBodyRot, p_229118_1_.yBodyRotO) * ((float)Math.PI / 180F)) + (Math.PI / 2D);
        Vector3d vector3d1 = p_229118_1_.getLeashOffset();
        double d1 = Math.cos(d0) * vector3d1.z + Math.sin(d0) * vector3d1.x;
        double d2 = Math.sin(d0) * vector3d1.z - Math.cos(d0) * vector3d1.x;
        double d3 = MathHelper.lerp(p_229118_2_, p_229118_1_.xo, p_229118_1_.getX()) + d1;
        double d4 = MathHelper.lerp(p_229118_2_, p_229118_1_.yo, p_229118_1_.getY()) + vector3d1.y;
        double d5 = MathHelper.lerp(p_229118_2_, p_229118_1_.zo, p_229118_1_.getZ()) + d2;
        p_229118_3_.translate(d1, vector3d1.y, d2);
        float f = (float)(vector3d.x - d3);
        float f1 = (float)(vector3d.y - d4);
        float f2 = (float)(vector3d.z - d5);
        IVertexBuilder ivertexbuilder = p_229118_4_.getBuffer(RenderType.leash());
        Matrix4f matrix4f = p_229118_3_.last().pose();
        float f4 = MathHelper.fastInvSqrt(f * f + f2 * f2) * 0.025F / 2.0F;
        float f5 = f2 * f4;
        float f6 = f * f4;
        BlockPos blockpos = new BlockPos(p_229118_1_.getEyePosition(p_229118_2_));
        BlockPos blockpos1 = new BlockPos(p_229118_5_.getEyePosition(p_229118_2_));
        int i = this.getBlockLightLevel(p_229118_1_, blockpos);
        int k = p_229118_1_.level.getBrightness(LightType.SKY, blockpos);
        int l = p_229118_1_.level.getBrightness(LightType.SKY, blockpos1);
        MobRenderer.renderSide(ivertexbuilder, matrix4f, f, f1, f2, i, i, k, l, 0.025F, 0.025F, f5, f6);
        MobRenderer.renderSide(ivertexbuilder, matrix4f, f, f1, f2, i, i, k, l, 0.025F, 0.0F, f5, f6);
        p_229118_3_.popPose();
    }


}
