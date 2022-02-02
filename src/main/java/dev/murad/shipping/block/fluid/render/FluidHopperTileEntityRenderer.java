package dev.murad.shipping.block.fluid.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import dev.murad.shipping.block.fluid.FluidHopperBlock;
import dev.murad.shipping.block.fluid.FluidHopperTileEntity;
import dev.murad.shipping.util.FluidRenderUtil;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.util.Direction;
import net.minecraft.util.math.vector.Vector3f;

public class FluidHopperTileEntityRenderer extends TileEntityRenderer<FluidHopperTileEntity> {
    public FluidHopperTileEntityRenderer(TileEntityRendererDispatcher p_i226006_1_) {
        super(p_i226006_1_);
    }

    @Override
    public void render(FluidHopperTileEntity fluidHopperTileEntity, float p_225616_2_, MatrixStack matrixStack, IRenderTypeBuffer buffer, int p_225616_5_, int p_225616_6_) {
        matrixStack.pushPose();
        Direction direction = fluidHopperTileEntity.getBlockState().getValue(FluidHopperBlock.FACING);
        matrixStack.translate(0.5f, 0, 0.5f);
        switch (direction) {
            case NORTH:
                matrixStack.mulPose(Vector3f.YP.rotationDegrees(Direction.SOUTH.toYRot()));
                break;
            case SOUTH:
                matrixStack.mulPose(Vector3f.YP.rotationDegrees(Direction.NORTH.toYRot()));
                break;
            default:
                matrixStack.mulPose(Vector3f.YP.rotationDegrees(direction.toYRot()));
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
