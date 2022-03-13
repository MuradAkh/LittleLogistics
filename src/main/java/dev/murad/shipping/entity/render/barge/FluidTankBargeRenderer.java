package dev.murad.shipping.entity.render.barge;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.barge.FluidTankBargeEntity;
import dev.murad.shipping.entity.models.FluidTankBargeModel;
import dev.murad.shipping.util.FluidRenderUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;

public class FluidTankBargeRenderer extends AbstractVesselRenderer<FluidTankBargeEntity> {
    private static final ResourceLocation BARGE_TEXTURE =
            new ResourceLocation(ShippingMod.MOD_ID, "textures/entity/fluid_barge.png");

    private final EntityModel model;


    public FluidTankBargeRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new FluidTankBargeModel(context.bakeLayer(FluidTankBargeModel.LAYER_LOCATION));
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
    public void render(FluidTankBargeEntity vesselEntity, float p_225623_2_, float p_225623_3_, PoseStack matrixStack, MultiBufferSource buffer, int p_225623_6_) {
        super.render(vesselEntity, p_225623_2_, p_225623_3_, matrixStack, buffer, p_225623_6_);
        renderFluid(vesselEntity, p_225623_2_, p_225623_3_, matrixStack, buffer, 0, p_225623_6_);

    }


    public void renderFluid(FluidTankBargeEntity entity, float yaw, float partialTicks, PoseStack matrixStackIn,
                            MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn) {
        FluidStack fluid = entity.getFluidStack();
        if (fluid == null) return;

        Fluid renderFluid = fluid.getFluid();
        if (renderFluid == null) return;

        matrixStackIn.pushPose();
        matrixStackIn.mulPose(Vector3f.YP.rotationDegrees(180.0F - yaw));
        matrixStackIn.translate(-0.3, 0.3, -0.25);
        matrixStackIn.scale(1f, 1.2f, 1f);
        FluidRenderUtil.renderCubeUsingQuads(FluidTankBargeEntity.CAPACITY, fluid, partialTicks, matrixStackIn, bufferIn, combinedLightIn, combinedOverlayIn);

        matrixStackIn.popPose();
    }
}
