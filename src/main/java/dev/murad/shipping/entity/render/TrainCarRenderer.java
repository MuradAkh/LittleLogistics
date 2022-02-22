package dev.murad.shipping.entity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.train.TrainCar;
import dev.murad.shipping.entity.models.ChainModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public abstract class TrainCarRenderer extends EntityRenderer<TrainCar> {


    private static final ResourceLocation CHAIN_TEXTURE =
            new ResourceLocation(ShippingMod.MOD_ID, "textures/entity/chain.png");

    private final ChainModel chainModel;



    public TrainCarRenderer(EntityRendererProvider.Context context) {
        super(context);
        chainModel = new ChainModel(context.bakeLayer(ChainModel.LAYER_LOCATION));

    }

    public void render(TrainCar vesselEntity, float yaw, float p_225623_3_, PoseStack matrixStack, MultiBufferSource buffer, int p_225623_6_) {
        matrixStack.pushPose();
        renderModel(vesselEntity, yaw, matrixStack, buffer, p_225623_6_);
        matrixStack.popPose();
        matrixStack.pushPose();
        getAndRenderChain(vesselEntity, matrixStack, buffer, p_225623_6_);
        matrixStack.popPose();
    }

    private void getAndRenderChain(TrainCar bargeEntity, PoseStack matrixStack, MultiBufferSource buffer, int p_225623_6_) {
        bargeEntity.getDominant().ifPresent(linkableEntity -> {
            var parent = (TrainCar) linkableEntity;
            double dist = parent.distanceTo(bargeEntity);
            int segments = (int) Math.ceil(dist * 4);
            var vec = bargeEntity.position().vectorTo(parent.position());
            // TODO: fix pitch
            matrixStack.mulPose(Vector3f.ZP.rotation((float) (Math.asin(vec.y))));
            matrixStack.mulPose(Vector3f.YP.rotation(-(float) Math.atan2(vec.z, vec.x)));
            matrixStack.translate(0, 0.44, 0);
            matrixStack.pushPose();
            VertexConsumer ivertexbuilderChain = buffer.getBuffer(chainModel.renderType(CHAIN_TEXTURE));
            for (int i = 0; i < segments; i++) {
                matrixStack.pushPose();
                matrixStack.translate(i / 4.0, 0, 0);
                chainModel.renderToBuffer(matrixStack, ivertexbuilderChain, p_225623_6_, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
                matrixStack.popPose();
            }



            matrixStack.popPose();
        });
    }

    private void renderModel(TrainCar pEntity, float pEntityYaw, PoseStack pMatrixStack, MultiBufferSource buffer, int pPartialTicks) {
        long i = (long)pEntity.getId() * 493286711L;
        i = i * i * 4392167121L + i * 98761L;
        float f = (((float)(i >> 16 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
        float f1 = (((float)(i >> 20 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
        float f2 = (((float)(i >> 24 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
        pMatrixStack.translate((double)f, (double)f1, (double)f2);
        double d0 = Mth.lerp((double)pPartialTicks, pEntity.xOld, pEntity.getX());
        double d1 = Mth.lerp((double)pPartialTicks, pEntity.yOld, pEntity.getY());
        double d2 = Mth.lerp((double)pPartialTicks, pEntity.zOld, pEntity.getZ());
        double d3 = (double)0.3F;
        Vec3 vec3 = pEntity.getPos(d0, d1, d2);
        float f3 = Mth.lerp(pPartialTicks, pEntity.xRotO, pEntity.getXRot());
        if (vec3 != null) {
            Vec3 vec31 = pEntity.getPosOffs(d0, d1, d2, (double)0.3F);
            Vec3 vec32 = pEntity.getPosOffs(d0, d1, d2, (double)-0.3F);
            if (vec31 == null) {
                vec31 = vec3;
            }

            if (vec32 == null) {
                vec32 = vec3;
            }

            pMatrixStack.translate(vec3.x - d0, (vec31.y + vec32.y) / 2.0D - d1, vec3.z - d2);
            Vec3 vec33 = vec32.add(-vec31.x, -vec31.y, -vec31.z);
            if (vec33.length() != 0.0D) {
                vec33 = vec33.normalize();
                pEntityYaw = (float)(Math.atan2(vec33.z, vec33.x) * 180.0D / Math.PI);
                f3 = (float)(Math.atan(vec33.y) * 73.0D);
            }
        }

        pMatrixStack.translate(0.0D, 0.375D, 0.0D);
        pMatrixStack.mulPose(Vector3f.YP.rotationDegrees(90));
        pMatrixStack.mulPose(Vector3f.YP.rotationDegrees(-pEntity.getDirection().toYRot()));
        pMatrixStack.mulPose(Vector3f.ZP.rotationDegrees(-f3));
        float f5 = (float)pEntity.getHurtTime() - pPartialTicks;
        float f6 = pEntity.getDamage() - pPartialTicks;
        if (f6 < 0.0F) {
            f6 = 0.0F;
        }

        if (f5 > 0.0F) {
            pMatrixStack.mulPose(Vector3f.XP.rotationDegrees(Mth.sin(f5) * f5 * f6 / 10.0F * (float)pEntity.getHurtDir()));
        }

        pMatrixStack.scale(-1.0F, -1.0F, 1.0F);
        VertexConsumer ivertexbuilder = buffer.getBuffer(getModel(pEntity).renderType(this.getTextureLocation(pEntity)));
        getModel(pEntity).renderToBuffer(pMatrixStack, ivertexbuilder, pPartialTicks, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    abstract Model getModel(Entity entity);
}
