package dev.murad.shipping.entity.render.train;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import dev.murad.shipping.entity.custom.barge.FluidTankBargeEntity;
import dev.murad.shipping.entity.custom.train.wagon.FluidTankCarEntity;
import dev.murad.shipping.util.FluidRenderUtil;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

import java.util.function.Function;

public class FluidTankCarRenderer extends TrainCarRenderer<FluidTankCarEntity>{
    public FluidTankCarRenderer(EntityRendererProvider.Context context, Function<ModelPart, EntityModel> baseModel, ModelLayerLocation layerLocation, String baseTexture) {
        super(context, baseModel, layerLocation, baseTexture);
    }

    @Override
    public void render(FluidTankCarEntity vesselEntity, float p_225623_2_, float p_225623_3_, PoseStack matrixStack, MultiBufferSource buffer, int p_225623_6_) {
        super.render(vesselEntity, p_225623_2_, p_225623_3_, matrixStack, buffer, p_225623_6_);

    }

    protected void renderAdditional(FluidTankCarEntity entity, float pEntityYaw, float partialTicks, PoseStack matrixStackIn, MultiBufferSource bufferIn, int pPackedLight) {
        FluidStack fluid = entity.getFluidStack();
        if (fluid == null) return;

        Fluid renderFluid = fluid.getFluid();
        if (renderFluid == null) return;

        matrixStackIn.pushPose();
        matrixStackIn.mulPose(Vector3f.ZN.rotationDegrees(180));
        matrixStackIn.translate(-0.22, -1.05, -0.11);
        matrixStackIn.scale(0.9f, 0.9f, 0.83f);
        FluidRenderUtil.renderCubeUsingQuads(FluidTankBargeEntity.CAPACITY, fluid, partialTicks, matrixStackIn, bufferIn, pPackedLight, pPackedLight);

        matrixStackIn.popPose();
    }
}
