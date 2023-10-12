package dev.murad.shipping.entity.render.train;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.murad.shipping.entity.custom.vessel.barge.FluidTankBargeEntity;
import dev.murad.shipping.entity.custom.train.wagon.FluidTankCarEntity;
import dev.murad.shipping.entity.render.ModelPack;
import dev.murad.shipping.entity.render.barge.MultipartVesselRenderer;
import dev.murad.shipping.util.FluidRenderUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

public class FluidTankCarRenderer<T extends FluidTankCarEntity> extends MultipartCarRenderer<T>{
    protected FluidTankCarRenderer(EntityRendererProvider.Context context,
                                   ModelPack<T> baseModelPack,
                                   ModelPack<T> insertModelPack,
                                   ModelPack<T> trimModelPack) {
        super(context, baseModelPack, insertModelPack, trimModelPack);
    }

    @Override
    protected void renderInsertModel(T entity, PoseStack matrixStack, MultiBufferSource buffer, float partialTicks, int packedLight, int overlay) {
        super.renderInsertModel(entity, matrixStack, buffer, partialTicks, packedLight, overlay);
        renderFluid(entity, partialTicks, matrixStack, buffer, packedLight);
    }

    protected void renderFluid(FluidTankCarEntity entity, float partialTicks, PoseStack matrixStackIn, MultiBufferSource bufferIn, int pPackedLight) {
        FluidStack fluid = entity.getFluidStack();
        if (fluid == null) return;

        Fluid renderFluid = fluid.getFluid();
        if (renderFluid == null) return;

        matrixStackIn.pushPose();
        matrixStackIn.mulPose(Axis.ZN.rotationDegrees(180));
        matrixStackIn.translate(-0.22, -1.05, -0.11);
        matrixStackIn.scale(0.9f, 0.9f, 0.83f);
        FluidRenderUtil.renderCubeUsingQuads(FluidTankBargeEntity.CAPACITY, fluid, partialTicks, matrixStackIn, bufferIn, pPackedLight, pPackedLight);

        matrixStackIn.popPose();
    }


    public static class Builder<T extends FluidTankCarEntity> extends MultipartCarRenderer.Builder<T> {

        public Builder(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public FluidTankCarRenderer<T> build() {
            return new FluidTankCarRenderer<>(context, baseModelPack, insertModelPack, trimModelPack);
        }
    }
}
