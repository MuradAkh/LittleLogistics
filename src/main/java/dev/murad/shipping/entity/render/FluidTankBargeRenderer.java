package dev.murad.shipping.entity.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.barge.FluidTankBargeEntity;
import dev.murad.shipping.entity.models.FluidTankBargeModel;
import dev.murad.shipping.util.FluidRenderUtil;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.fluid.Fluid;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.fluids.FluidStack;

public class FluidTankBargeRenderer extends AbstractBargeRenderer<FluidTankBargeEntity> {
    private static final ResourceLocation BARGE_TEXTURE =
            new ResourceLocation(ShippingMod.MOD_ID, "textures/entity/fluid_barge.png");

    private final EntityModel model = new FluidTankBargeModel();


    public FluidTankBargeRenderer(EntityRendererManager p_i46179_1_) {
        super(p_i46179_1_);
    }

    @Override
    EntityModel getModel(FluidTankBargeEntity entity) {
        return model;
    }

    @Override
    public ResourceLocation getTextureLocation(FluidTankBargeEntity p_110775_1_) {
        return BARGE_TEXTURE;
    }

    @Override
    public void render(FluidTankBargeEntity bargeEntity, float p_225623_2_, float p_225623_3_, MatrixStack matrixStack, IRenderTypeBuffer buffer, int p_225623_6_) {
        super.render(bargeEntity, p_225623_2_, p_225623_3_, matrixStack, buffer, p_225623_6_);
        renderFluid(bargeEntity, p_225623_2_, p_225623_3_, matrixStack, buffer, 0, p_225623_6_);

    }


    public void renderFluid(FluidTankBargeEntity entity, float yaw, float partialTicks, MatrixStack matrixStackIn,
                            IRenderTypeBuffer bufferIn, int combinedLightIn, int combinedOverlayIn) {
        FluidStack fluid = entity.getFluidStack();
        if (fluid == null) return;

        Fluid renderFluid = fluid.getFluid();
        if (renderFluid == null) return;

        matrixStackIn.pushPose();
        matrixStackIn.mulPose(Vector3f.YP.rotationDegrees(180.0F - yaw));
        matrixStackIn.translate(-0.3, 0.5, -0.25);
        matrixStackIn.scale(1f, 1.2f, 1f);
        FluidRenderUtil.renderCubeUsingQuads(FluidTankBargeEntity.CAPACITY, fluid, partialTicks, matrixStackIn, bufferIn, combinedLightIn, combinedOverlayIn);

        matrixStackIn.popPose();
    }
}
