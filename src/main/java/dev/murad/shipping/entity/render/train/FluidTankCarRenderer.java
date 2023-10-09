package dev.murad.shipping.entity.render.train;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.murad.shipping.entity.custom.vessel.barge.FluidTankBargeEntity;
import dev.murad.shipping.entity.custom.train.wagon.FluidTankCarEntity;
import dev.murad.shipping.util.FluidRenderUtil;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class FluidTankCarRenderer extends TrainCarRenderer<FluidTankCarEntity>{
    public FluidTankCarRenderer(EntityRendererProvider.Context context, Function<ModelPart, EntityModel<FluidTankCarEntity>> baseModel, ModelLayerLocation layerLocation, String baseTexture) {
        super(context, baseModel, layerLocation, baseTexture);
    }

    protected void renderAdditional(FluidTankCarEntity entity, float pEntityYaw, float partialTicks, PoseStack matrixStackIn, MultiBufferSource bufferIn, int pPackedLight) {
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
}
