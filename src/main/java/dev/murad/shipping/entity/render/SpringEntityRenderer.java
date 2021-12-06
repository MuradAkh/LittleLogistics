package dev.murad.shipping.entity.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.SpringEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;

public class SpringEntityRenderer extends EntityRenderer<SpringEntity> {

    public SpringEntityRenderer(EntityRendererManager renderManager) {
        super(renderManager);
    }


    @Override
    public void render(SpringEntity entity, float entityYaw, float p_225623_3_, MatrixStack matrixStack, IRenderTypeBuffer buffers, int light) {
        if(entity.getDominant() != null && entity.getDominated() != null) {
            matrixStack.pushPose();

            Vector3d anchorThis = SpringEntity.calculateAnchorPosition(entity.getDominant(), SpringEntity.SpringSide.DOMINATED);
            matrixStack.translate(-entity.getX(), -entity.getY(), -entity.getZ());
            matrixStack.translate(anchorThis.x, anchorThis.y, anchorThis.z);
            matrixStack.mulPose(Vector3f.YP.rotationDegrees(-entityYaw));
            renderSpring(entity, matrixStack, buffers, light);
            matrixStack.popPose();
        }
        super.render(entity, entityYaw, p_225623_3_, matrixStack, buffers, light);

    }

    @Override
    public ResourceLocation getTextureLocation(SpringEntity entity) {
        return  new ResourceLocation(ShippingMod.MOD_ID, "textures/entity/spring.png");
    }

    private void renderSpring(SpringEntity spring, MatrixStack matrixStack, IRenderTypeBuffer buffers, int light) {
        Vector3d anchorThis = SpringEntity.calculateAnchorPosition(spring.getDominant(), SpringEntity.SpringSide.DOMINATED);
        Vector3d anchorOther = SpringEntity.calculateAnchorPosition(spring.getDominated(), SpringEntity.SpringSide.DOMINANT);
        double offsetX = anchorOther.x - anchorThis.x;
        double offsetY = anchorOther.y - anchorThis.y;
        double offsetZ = anchorOther.z - anchorThis.z;

        matrixStack.pushPose();
        IVertexBuilder bufferbuilder = buffers.getBuffer(RenderType.LINES);
        int l = 32;

        for (int i1 = 1; i1 <= l; ++i1)
        {
            float step = (float)i1 / l;
            float stepMinus1 = (float)(i1-1) / l;
            line(matrixStack, offsetX, offsetY, offsetZ, bufferbuilder, i1-1, stepMinus1);
            line(matrixStack, offsetX, offsetY, offsetZ, bufferbuilder, i1, step);
        }
        matrixStack.popPose();
    }

    private void line(MatrixStack matrixStack, double offsetX, double offsetY, double offsetZ, IVertexBuilder bufferbuilder, int i1, float step) {
        bufferbuilder
                .vertex(matrixStack.last().pose(), (float)(offsetX * (double)step), (float)(offsetY * (double)(step * step + step) * 0.5D + 0.25D), (float)(offsetZ * (double)step));
        if(i1 % 2 == 0) {
            bufferbuilder.color(0x80, (int)((1f-step)*0x80), (int)((1f-step)*0x80), 255);
        } else {
            bufferbuilder.color(0x20, (int)((1f-step)*0x20), (int)((1f-step)*0x20), 255);
        }
        bufferbuilder.endVertex();
    }
}
