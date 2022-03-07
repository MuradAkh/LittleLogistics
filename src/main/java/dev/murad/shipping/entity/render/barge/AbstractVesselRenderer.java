package dev.murad.shipping.entity.render.barge;

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

public abstract class AbstractVesselRenderer<T extends VesselEntity> extends EntityRenderer<T> {


    private static final ResourceLocation CHAIN_TEXTURE =
            new ResourceLocation(ShippingMod.MOD_ID, "textures/entity/chain.png");

    private final ChainModel chainModel;


    public AbstractVesselRenderer(EntityRendererProvider.Context context) {
        super(context);
        chainModel = new ChainModel(context.bakeLayer(ChainModel.LAYER_LOCATION));
    }

    public void render(T vesselEntity, float p_225623_2_, float p_225623_3_, PoseStack matrixStack, MultiBufferSource buffer, int p_225623_6_) {
        matrixStack.pushPose();
        matrixStack.translate(0.0D, getModelYoffset(), 0.0D);
        matrixStack.translate(0.0D, 0.07, 0.0D);
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
            if(((Entity) p_225626_1_.getDominant().get()).shouldRender(p_225626_3_, p_225626_5_, p_225626_7_)){
                return true;
            }
            if(p_225626_1_.getDominantSpring().get().shouldRender(p_225626_3_, p_225626_5_, p_225626_7_)){
                return true;
            }
        }
        return super.shouldRender(p_225626_1_, p_225626_2_, p_225626_3_, p_225626_5_, p_225626_7_);
    }


    abstract EntityModel<T> getModel(T entity);


    private <E extends Entity> void renderLeash(T pEntityLiving, float pPartialTicks, PoseStack pMatrixStack, MultiBufferSource pBuffer, E pLeashHolder) {
        pMatrixStack.pushPose();
        Vec3 vec3 = pLeashHolder.getRopeHoldPosition(pPartialTicks);
        double d0 = (double)(Mth.lerp(pPartialTicks, pEntityLiving.yBodyRot, pEntityLiving.yBodyRotO) * ((float)Math.PI / 180F)) + (Math.PI / 2D);
        Vec3 vec31 = pEntityLiving.getLeashOffset();
        double d1 = Math.cos(d0) * vec31.z + Math.sin(d0) * vec31.x;
        double d2 = Math.sin(d0) * vec31.z - Math.cos(d0) * vec31.x;
        double d3 = Mth.lerp((double)pPartialTicks, pEntityLiving.xo, pEntityLiving.getX()) + d1;
        double d4 = Mth.lerp((double)pPartialTicks, pEntityLiving.yo, pEntityLiving.getY()) + vec31.y;
        double d5 = Mth.lerp((double)pPartialTicks, pEntityLiving.zo, pEntityLiving.getZ()) + d2;
        pMatrixStack.translate(d1, vec31.y, d2);
        float f = (float)(vec3.x - d3);
        float f1 = (float)(vec3.y - d4);
        float f2 = (float)(vec3.z - d5);
        float f3 = 0.025F;
        VertexConsumer vertexconsumer = pBuffer.getBuffer(RenderType.leash());
        Matrix4f matrix4f = pMatrixStack.last().pose();
        float f4 = Mth.fastInvSqrt(f * f + f2 * f2) * 0.025F / 2.0F;
        float f5 = f2 * f4;
        float f6 = f * f4;
        BlockPos blockpos = new BlockPos(pEntityLiving.getEyePosition(pPartialTicks));
        BlockPos blockpos1 = new BlockPos(pLeashHolder.getEyePosition(pPartialTicks));
        int i = this.getBlockLightLevel(pEntityLiving, blockpos);
        int j = i;
        int k = pEntityLiving.level.getBrightness(LightLayer.SKY, blockpos);
        int l = pEntityLiving.level.getBrightness(LightLayer.SKY, blockpos1);

        for(int i1 = 0; i1 <= 24; ++i1) {
            addVertexPair(vertexconsumer, matrix4f, f, f1, f2, i, j, k, l, 0.025F, 0.025F, f5, f6, i1, false);
        }

        for(int j1 = 24; j1 >= 0; --j1) {
            addVertexPair(vertexconsumer, matrix4f, f, f1, f2, i, j, k, l, 0.025F, 0.0F, f5, f6, j1, true);
        }

        pMatrixStack.popPose();
    }

    private static void addVertexPair(VertexConsumer p_174308_, Matrix4f p_174309_, float p_174310_, float p_174311_, float p_174312_, int p_174313_, int p_174314_, int p_174315_, int p_174316_, float p_174317_, float p_174318_, float p_174319_, float p_174320_, int p_174321_, boolean p_174322_) {
        float f = (float)p_174321_ / 24.0F;
        int i = (int)Mth.lerp(f, (float)p_174313_, (float)p_174314_);
        int j = (int)Mth.lerp(f, (float)p_174315_, (float)p_174316_);
        int k = LightTexture.pack(i, j);
        float f1 = p_174321_ % 2 == (p_174322_ ? 1 : 0) ? 0.7F : 1.0F;
        float f2 = 0.5F * f1;
        float f3 = 0.4F * f1;
        float f4 = 0.3F * f1;
        float f5 = p_174310_ * f;
        float f6 = p_174311_ > 0.0F ? p_174311_ * f * f : p_174311_ - p_174311_ * (1.0F - f) * (1.0F - f);
        float f7 = p_174312_ * f;
        p_174308_.vertex(p_174309_, f5 - p_174319_, f6 + p_174318_, f7 + p_174320_).color(f2, f3, f4, 1.0F).uv2(k).endVertex();
        p_174308_.vertex(p_174309_, f5 + p_174319_, f6 + p_174317_ - p_174318_, f7 - p_174320_).color(f2, f3, f4, 1.0F).uv2(k).endVertex();
    }
}
