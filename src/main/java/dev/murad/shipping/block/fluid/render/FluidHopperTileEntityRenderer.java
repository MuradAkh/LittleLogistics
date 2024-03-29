package dev.murad.shipping.block.fluid.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.murad.shipping.block.fluid.FluidHopperBlock;
import dev.murad.shipping.block.fluid.FluidHopperTileEntity;
import dev.murad.shipping.util.FluidRenderUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public class FluidHopperTileEntityRenderer implements BlockEntityRenderer<FluidHopperTileEntity> {
    public FluidHopperTileEntityRenderer(BlockEntityRendererProvider.Context context) {

    }

    @Override
    public void render(FluidHopperTileEntity fluidHopperTileEntity, float p_225616_2_, PoseStack matrixStack, MultiBufferSource buffer, int p_225616_5_, int p_225616_6_) {
        matrixStack.pushPose();
        Direction direction = fluidHopperTileEntity.getBlockState().getValue(FluidHopperBlock.FACING);
        matrixStack.translate(0.5f, 0, 0.5f);
        switch (direction) {
            case NORTH:
                matrixStack.mulPose(Axis.YP.rotationDegrees(Direction.SOUTH.toYRot()));
                break;
            case SOUTH:
                matrixStack.mulPose(Axis.YP.rotationDegrees(Direction.NORTH.toYRot()));
                break;
            default:
                matrixStack.mulPose(Axis.YP.rotationDegrees(direction.toYRot()));
        }
        matrixStack.scale(1.45f, 1f, 1.2f);
        matrixStack.translate(-0.25f, 0, -0.15f);

        FluidRenderUtil.renderCubeUsingQuads(FluidHopperTileEntity.CAPACITY,
                fluidHopperTileEntity.getTank().getFluid(),
                p_225616_2_, matrixStack, buffer, p_225616_5_, p_225616_6_
                );

        matrixStack.popPose();
    }
}
