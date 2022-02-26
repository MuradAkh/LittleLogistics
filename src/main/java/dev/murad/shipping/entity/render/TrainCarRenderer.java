package dev.murad.shipping.entity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.train.AbstractTrainCar;
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

public abstract class TrainCarRenderer extends EntityRenderer<AbstractTrainCar> {

    private static final ResourceLocation CHAIN_TEXTURE =
            new ResourceLocation(ShippingMod.MOD_ID, "textures/entity/chain.png");

    private final ChainModel chainModel;

    public TrainCarRenderer(EntityRendererProvider.Context context) {
        super(context);
        chainModel = new ChainModel(context.bakeLayer(ChainModel.LAYER_LOCATION));

    }

    public void render(AbstractTrainCar vesselEntity, float yaw, float p_225623_3_, PoseStack matrixStack, MultiBufferSource buffer, int p_225623_6_) {
        matrixStack.pushPose();
        getAndRenderChain(vesselEntity, matrixStack, buffer, p_225623_6_);
        matrixStack.popPose();
        matrixStack.pushPose();
        renderModel(vesselEntity, yaw, matrixStack, buffer, p_225623_6_);
        matrixStack.popPose();
    }

    private void getAndRenderChain(AbstractTrainCar car, PoseStack matrixStack, MultiBufferSource buffer, int p_225623_6_) {
        car.getDominant().ifPresent(linkableEntity -> {
            var parent = (AbstractTrainCar) linkableEntity;
            double dist = parent.distanceTo(car);
            int segments = (int) Math.ceil(dist * 4);
            var vec = car.position()
                    .subtract(0, 0.2, 0)
                    .vectorTo(parent.position()
                            .subtract(0, 0.2, 0));
            // TODO: fix pitch
            matrixStack.translate(car.getDirection().getStepX() * 0.25, 0.44, car.getDirection().getStepZ() * 0.25);
            matrixStack.mulPose(Vector3f.YP.rotation(-(float) Math.atan2(vec.z, vec.x)));
            matrixStack.mulPose(Vector3f.ZP.rotation((float) (Math.asin(vec.y))));
            matrixStack.pushPose();
            VertexConsumer ivertexbuilderChain = buffer.getBuffer(chainModel.renderType(CHAIN_TEXTURE));
            for (int i = 1; i < segments; i++) {
                matrixStack.pushPose();
                matrixStack.translate(i / 4.0, 0, 0);
                chainModel.renderToBuffer(matrixStack, ivertexbuilderChain, p_225623_6_, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
                matrixStack.popPose();
            }

            matrixStack.popPose();
        });
    }

    private void renderModel(AbstractTrainCar car, float pEntityYaw, PoseStack pMatrixStack, MultiBufferSource buffer, int pPartialTicks) {
        long i = (long)car.getId() * 493286711L;
        i = i * i * 4392167121L + i * 98761L;
        float f = (((float)(i >> 16 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
        float f1 = (((float)(i >> 20 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
        float f2 = (((float)(i >> 24 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
        pMatrixStack.translate((double)f, (double)f1, (double)f2);
        double d0 = Mth.lerp((double)pPartialTicks, car.xOld, car.getX());
        double d1 = Mth.lerp((double)pPartialTicks, car.yOld, car.getY());
        double d2 = Mth.lerp((double)pPartialTicks, car.zOld, car.getZ());
        double d3 = (double)0.3F;
        Vec3 vec3 = car.getPos(d0, d1, d2);
        float f3 = Mth.lerp(pPartialTicks, car.xRotO, car.getXRot());
        if (vec3 != null) {
            Vec3 vec31 = car.getPosOffs(d0, d1, d2, (double)0.3F);
            Vec3 vec32 = car.getPosOffs(d0, d1, d2, (double)-0.3F);
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
        pMatrixStack.mulPose(Vector3f.YP.rotationDegrees(-car.getDirection().toYRot()));
        pMatrixStack.mulPose(Vector3f.ZP.rotationDegrees(-f3));
        float f5 = (float)car.getHurtTime() - pPartialTicks;
        float f6 = car.getDamage() - pPartialTicks;
        if (f6 < 0.0F) {
            f6 = 0.0F;
        }

        if (f5 > 0.0F) {
            pMatrixStack.mulPose(Vector3f.XP.rotationDegrees(Mth.sin(f5) * f5 * f6 / 10.0F * (float)car.getHurtDir()));
        }

        pMatrixStack.scale(-1.0F, -1.0F, 1.0F);
        VertexConsumer ivertexbuilder = buffer.getBuffer(getModel(car).renderType(this.getTextureLocation(car)));
        getModel(car).renderToBuffer(pMatrixStack, ivertexbuilder, pPartialTicks, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    abstract Model getModel(Entity entity);
}
