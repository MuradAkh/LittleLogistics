package dev.murad.shipping.entity.render.barge;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.murad.shipping.entity.custom.vessel.barge.AbstractBargeEntity;
import dev.murad.shipping.entity.custom.vessel.barge.FluidTankBargeEntity;
import dev.murad.shipping.entity.render.ModelPack;
import dev.murad.shipping.util.FluidRenderUtil;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

public class FluidTankBargeRenderer<T extends FluidTankBargeEntity> extends MultipartVesselRenderer<T> {

    protected FluidTankBargeRenderer(
            EntityRendererProvider.Context context,
            ModelPack<T> baseModelPack,
            ModelPack<T> insertModelPack,
            ModelPack<T> trimModelPack) {
        super(context, baseModelPack, insertModelPack, trimModelPack);
    }

    @Override
    public void render(@NotNull T entity, float yaw, float partialTick, PoseStack matrixStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, yaw, partialTick, matrixStack, buffer, packedLight);
        renderFluid(entity, yaw, partialTick, matrixStack, buffer, 0, packedLight);
    }

    public void renderFluid(FluidTankBargeEntity entity, float yaw, float partialTicks, PoseStack matrixStackIn,
                            MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn) {
        FluidStack fluid = entity.getFluidStack();
        if (fluid == null) return;

        Fluid renderFluid = fluid.getFluid();
        if (renderFluid == null) return;

        matrixStackIn.pushPose();
        matrixStackIn.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        matrixStackIn.translate(-0.3, 0.4, -0.25);
        matrixStackIn.scale(1f, 1.2f, 1f);
        FluidRenderUtil.renderCubeUsingQuads(FluidTankBargeEntity.CAPACITY, fluid, partialTicks, matrixStackIn, bufferIn, combinedLightIn, combinedOverlayIn);

        matrixStackIn.popPose();
    }

    public static class Builder<T extends FluidTankBargeEntity> extends MultipartVesselRenderer.Builder<T> {

        public Builder(EntityRendererProvider.Context context) {
            super(context);
        }

        public FluidTankBargeRenderer<T> build() {
            return new FluidTankBargeRenderer<>(context, baseModelPack, insertModelPack, trimModelPack);
        }
    }
}
