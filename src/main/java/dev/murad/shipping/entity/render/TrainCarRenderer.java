package dev.murad.shipping.entity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Vector3f;
import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.train.AbstractTrainCarEntity;
import dev.murad.shipping.entity.custom.train.locomotive.AbstractLocomotiveEntity;
import dev.murad.shipping.entity.custom.train.wagon.ChestCarEntity;
import dev.murad.shipping.entity.models.ChainModel;
import dev.murad.shipping.entity.models.ChestCarModel;
import dev.murad.shipping.setup.ModEntityTypes;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.function.Function;


public class TrainCarRenderer<T extends AbstractTrainCarEntity> extends EntityRenderer<T> {
    private final EntityModel<T> entityModel;
    private final ResourceLocation texture;

    private static final ResourceLocation CHAIN_TEXTURE =
            new ResourceLocation(ShippingMod.MOD_ID, "textures/entity/chain.png");

    private final ChainModel chainModel;

    public TrainCarRenderer(EntityRendererProvider.Context context, Function<ModelPart, EntityModel> baseModel, ModelLayerLocation layerLocation, String baseTexture) {
        super(context);
        chainModel = new ChainModel(context.bakeLayer(ChainModel.LAYER_LOCATION));
        entityModel = baseModel.apply(context.bakeLayer(layerLocation));
        texture = new ResourceLocation(ShippingMod.MOD_ID, baseTexture);
    }

    public void render(T vesselEntity, float yaw, float pPartialTicks, PoseStack matrixStack, MultiBufferSource buffer, int pPackedLight) {
        getAndRenderChain(vesselEntity, matrixStack, buffer, pPackedLight);
        renderModel(vesselEntity, yaw, pPartialTicks, matrixStack, buffer, pPackedLight);
    }


    private void getAndRenderChain(T car, PoseStack matrixStack, MultiBufferSource buffer, int p_225623_6_) {
        matrixStack.pushPose();
        car.getDominant().ifPresent(linkableEntity -> {
            var parent = (AbstractTrainCarEntity) linkableEntity;
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
        matrixStack.popPose();
    }

    private void renderModel(T pEntity, float pEntityYaw, float pPartialTicks, PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight) {
        pMatrixStack.pushPose();
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
        Vec3 pos = pEntity.getPos(d0, d1, d2);
        float pitch = Mth.lerp(pPartialTicks, pEntity.xRotO, pEntity.getXRot());
        if (pos != null) {
            Vec3 forwardDir = pEntity.getPosOffs(d0, d1, d2, (double)0.5F);
            Vec3 backDir = pEntity.getPosOffs(d0, d1, d2, (double)-0.5F);
            if (forwardDir == null) {
                forwardDir = pos;
            }

            if (backDir == null) {
                backDir = pos;
            }

            pMatrixStack.translate(pos.x - d0, (forwardDir.y + backDir.y) / 2.0D - d1, pos.z - d2);
            Vec3 trackDirection = backDir.add(-forwardDir.x, -forwardDir.y, -forwardDir.z);
            if (trackDirection.length() != 0.0D) {
                trackDirection = trackDirection.normalize();
                pEntityYaw = (float)(Math.atan2(trackDirection.z, trackDirection.x) * 180.0D / Math.PI + 90);
                pitch = (float)(Math.atan(trackDirection.y) * 73.0D);
            }
        }

        pMatrixStack.translate(0.0D, 0.375D, 0.0D);
        pMatrixStack.mulPose(Vector3f.YP.rotationDegrees(180.0F - pEntityYaw));
        pMatrixStack.mulPose(Vector3f.XN.rotationDegrees(pitch));
        float f5 = (float)pEntity.getHurtTime() - pPartialTicks;
        float f6 = pEntity.getDamage() - pPartialTicks;
        if (f6 < 0.0F) {
            f6 = 0.0F;
        }

        if (f5 > 0.0F) {
            pMatrixStack.mulPose(Vector3f.XP.rotationDegrees(Mth.sin(f5) * f5 * f6 / 10.0F * (float)pEntity.getHurtDir()));
        }

        pMatrixStack.translate(0, 1.1, 0);

        pMatrixStack.scale(-1.0F, -1.0F, 1.0F);
        this.entityModel.setupAnim(pEntity, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        VertexConsumer vertexconsumer = pBuffer.getBuffer(this.entityModel.renderType(this.getTextureLocation(pEntity)));
        this.entityModel.renderToBuffer(pMatrixStack, vertexconsumer, pPackedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        pMatrixStack.popPose();
    }

    protected Model getModel(T entity){
        return entityModel;
    };

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }
}
