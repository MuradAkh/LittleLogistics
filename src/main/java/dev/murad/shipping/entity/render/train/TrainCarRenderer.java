package dev.murad.shipping.entity.render.train;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Axis;
import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.train.AbstractTrainCarEntity;
import dev.murad.shipping.entity.models.train.ChainModel;
import dev.murad.shipping.entity.render.RenderWithAttachmentPoints;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.function.Function;


public class TrainCarRenderer<T extends AbstractTrainCarEntity> extends EntityRenderer<T> implements RenderWithAttachmentPoints<T> {
    private final EntityModel<T> entityModel;
    private final ResourceLocation texture;

    private static final ResourceLocation CHAIN_TEXTURE =
            new ResourceLocation(ShippingMod.MOD_ID, "textures/entity/chain.png");

    private final ChainModel chainModel;

    public TrainCarRenderer(EntityRendererProvider.Context context, Function<ModelPart, EntityModel<T>> baseModel, ModelLayerLocation layerLocation, String baseTexture) {
        super(context);
        chainModel = new ChainModel(context.bakeLayer(ChainModel.LAYER_LOCATION));
        entityModel = baseModel.apply(context.bakeLayer(layerLocation));
        texture = new ResourceLocation(ShippingMod.MOD_ID, baseTexture);
    }

    public void render(T car, float yaw, float pPartialTicks, PoseStack pose, MultiBufferSource buffer, int pPackedLight) {
        //getAndRenderChain(car, pose, buffer, pPackedLight);
        if (car.getLeader().isPresent()) return;

        pose.pushPose();

        // render
        AbstractTrainCarEntity t = car;
        Pair<Vec3, Vec3> attachmentPoints = renderCarAndGetAttachmentPoints(car, yaw, pPartialTicks, pose, buffer, pPackedLight);

        while (t.getFollower().isPresent()) {
            AbstractTrainCarEntity nextT = t.getFollower().get();
            EntityRenderer<? super AbstractTrainCarEntity> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(nextT);
            if (renderer instanceof RenderWithAttachmentPoints) {
                @SuppressWarnings("unchecked")
                RenderWithAttachmentPoints<AbstractTrainCarEntity> attachmentRenderer = (RenderWithAttachmentPoints<AbstractTrainCarEntity>) renderer;

                // translate to next train location
                Vec3 nextTPos = nextT.getPosition(pPartialTicks), tPos = t.getPosition(pPartialTicks);
                Vec3 offset = nextTPos.subtract(tPos);
                pose.translate(offset.x, offset.y, offset.z);
                Pair<Vec3, Vec3> newAttachmentPoints = attachmentRenderer.renderCarAndGetAttachmentPoints(nextT, nextT.getYRot(), pPartialTicks, pose, buffer, pPackedLight);
                Vec3 from = newAttachmentPoints.getFirst();
                Vec3 to = attachmentPoints.getSecond();

                // translate to "from" position
                pose.pushPose();
                offset = from.subtract(nextTPos);
                pose.translate(offset.x, offset.y, offset.z);
                getAndRenderChain(nextT, from, to, pose, buffer, pPackedLight);
                pose.popPose();

                attachmentPoints = newAttachmentPoints;
            }

            t = nextT;
        }

        pose.popPose();
    }

    private void getAndRenderChain(AbstractTrainCarEntity car, Vec3 from, Vec3 to, PoseStack matrixStack, MultiBufferSource buffer, int p_225623_6_) {
        matrixStack.pushPose();
        var vec = from.vectorTo(to);
        double dist = vec.length();
        int segments = (int) Math.ceil(dist * 4);

        // TODO: fix pitch
        matrixStack.mulPose(Axis.YP.rotation(-(float) Math.atan2(vec.z, vec.x)));
        matrixStack.mulPose(Axis.ZP.rotation((float) (Math.asin(vec.y / dist))));
        matrixStack.pushPose();
        VertexConsumer ivertexbuilderChain = buffer.getBuffer(chainModel.renderType(CHAIN_TEXTURE));
        for (int i = 1; i < segments; i++) {
            matrixStack.pushPose();
            matrixStack.translate(i / 4.0, 0, 0);
            chainModel.renderToBuffer(matrixStack, ivertexbuilderChain, p_225623_6_, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            matrixStack.popPose();
        }

        matrixStack.popPose();
        matrixStack.popPose();
    }

    // First - front anchor point
    // Second - back anchor point
    // Override this to change anchor points for larger or smaller cars
    public Pair<Vec3, Vec3> getAttachmentPoints(Vec3 chainCentre, Vec3 trackDirection) {
        return new Pair<>(chainCentre.add(trackDirection.scale(.2)), chainCentre.add(trackDirection.scale(-.2)));
    }

    @Override
    public boolean shouldRender(T entity, Frustum pCamera, double pCamX, double pCamY, double pCamZ) {
        return true;
    }

    public Pair<Vec3, Vec3> renderCarAndGetAttachmentPoints(T car, float yaw, float partialTicks, PoseStack pose, MultiBufferSource buffer, int packedLight) {
        Pair<Vec3, Vec3> attach = new Pair<>(car.getPosition(partialTicks).add(0, .44, 0), car.getPosition(partialTicks).add(0, .44, 0));

        pose.pushPose();
        long i = (long)car.getId() * 493286711L;
        i = i * i * 4392167121L + i * 98761L;
        float f = (((float)(i >> 16 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
        float f1 = (((float)(i >> 20 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
        float f2 = (((float)(i >> 24 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
        pose.translate((double)f, (double)f1, (double)f2);
        double d0 = Mth.lerp((double)partialTicks, car.xOld, car.getX());
        double d1 = Mth.lerp((double)partialTicks, car.yOld, car.getY());
        double d2 = Mth.lerp((double)partialTicks, car.zOld, car.getZ());
        double d3 = (double)0.3F;
        Vec3 pos = car.getPos(d0, d1, d2);
        float pitch = Mth.lerp(partialTicks, car.xRotO, car.getXRot());
        if (pos != null) {
            Vec3 forwardDir = car.getPosOffs(d0, d1, d2, (double)0.3F);
            Vec3 backDir = car.getPosOffs(d0, d1, d2, (double)-0.3F);
            if (forwardDir == null) {
                forwardDir = pos;
            }

            if (backDir == null) {
                backDir = pos;
            }

            Vec3 centre = new Vec3(pos.x, (forwardDir.y + backDir.y) / 2.0D, pos.z);
            Vec3 offset = centre.subtract(d0, d1, d2);

            pose.translate(offset.x, offset.y, offset.z);
            Vec3 trackDirection = forwardDir.subtract(backDir);
            if (trackDirection.length() != 0.0D) {
                trackDirection = trackDirection.normalize();
                yaw = (float)(Math.atan2(-trackDirection.z, -trackDirection.x) * 180.0D / Math.PI + 90);
                pitch = (float)(Math.atan(-trackDirection.y) * 73.0D);
            }

            Vec3 chainCentre = centre.add(0, .22, 0);
            attach = getAttachmentPoints(chainCentre, trackDirection);
        }

        pose.translate(0.0D, 0.375D, 0.0D);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        pose.mulPose(Axis.XN.rotationDegrees(pitch));
        float f5 = (float)car.getHurtTime() - partialTicks;
        float f6 = car.getDamage() - partialTicks;
        if (f6 < 0.0F) {
            f6 = 0.0F;
        }

        if (f5 > 0.0F) {
            pose.mulPose(Axis.XP.rotationDegrees(Mth.sin(f5) * f5 * f6 / 10.0F * (float)car.getHurtDir()));
        }

        pose.translate(0, 1.1, 0);

        pose.scale(-1.0F, -1.0F, 1.0F);
        this.entityModel.setupAnim(car, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        VertexConsumer vertexconsumer = buffer.getBuffer(this.entityModel.renderType(this.getTextureLocation(car)));
        this.entityModel.renderToBuffer(pose, vertexconsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        renderAdditional(car, yaw, partialTicks, pose, buffer, packedLight);
        pose.popPose();

        if (car.hasCustomName()) {
            this.renderNameTag(car, car.getCustomName(), pose, buffer, packedLight);
        }

        return attach;
    }

    protected void renderAdditional(T pEntity, float pEntityYaw, float pPartialTicks, PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight) {

    }

    protected Model getModel(T entity){
        return entityModel;
    };

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }
}
