package dev.murad.shipping.entity.models;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import dev.murad.shipping.entity.custom.barge.ChunkLoaderBargeEntity;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.model.ModelRenderer;

public class ChunkLoaderBargeModel extends EntityModel<ChunkLoaderBargeEntity> {

    private final ModelRenderer bb_main;
    private final ModelRenderer ring;
    private final ModelRenderer ring2;
    private final ModelRenderer ring3;

    public ChunkLoaderBargeModel() {
        texWidth = 64;
        texHeight = 64;

        bb_main = new ModelRenderer(this);
        bb_main.setPos(0.0F, 23.0F, 0.0F);
        bb_main.texOffs(0, 0).addBox(-6.0F, -27.0F, -7.0F, 12.0F, 5.0F, 14.0F, 0.0F, false);
        bb_main.texOffs(18, 23).addBox(-8.0F, -29.0F, -7.0F, 2.0F, 4.0F, 14.0F, 0.0F, false);
        bb_main.texOffs(36, 25).addBox(-6.0F, -29.0F, -9.0F, 12.0F, 4.0F, 2.0F, 0.0F, false);
        bb_main.texOffs(0, 19).addBox(6.0F, -29.0F, -7.0F, 2.0F, 4.0F, 14.0F, 0.0F, false);
        bb_main.texOffs(36, 19).addBox(-6.0F, -29.0F, 7.0F, 12.0F, 4.0F, 2.0F, 0.0F, false);
        bb_main.texOffs(0, 37).addBox(-2.0F, -33.0F, -2.0F, 4.0F, 4.0F, 4.0F, 0.0F, false);

        ring = new ModelRenderer(this);
        ring.setPos(0.0F, 0.0F, 0.0F);
        bb_main.addChild(ring);
        ring.texOffs(38, 0).addBox(-5.0F, -29.0F, -5.0F, 10.0F, 2.0F, 2.0F, 0.0F, false);
        ring.texOffs(36, 31).addBox(-5.0F, -29.0F, 3.0F, 10.0F, 2.0F, 2.0F, 0.0F, false);
        ring.texOffs(10, 41).addBox(3.0F, -29.0F, -3.0F, 2.0F, 2.0F, 6.0F, 0.0F, false);
        ring.texOffs(38, 4).addBox(-5.0F, -29.0F, -3.0F, 2.0F, 2.0F, 6.0F, 0.0F, false);

        ring2 = new ModelRenderer(this);
        ring2.setPos(0.0F, -3.0F, 0.0F);
        bb_main.addChild(ring2);
        ring2.texOffs(38, 0).addBox(-5.0F, -29.0F, -5.0F, 10.0F, 2.0F, 2.0F, 0.0F, false);
        ring2.texOffs(36, 31).addBox(-5.0F, -29.0F, 3.0F, 10.0F, 2.0F, 2.0F, 0.0F, false);
        ring2.texOffs(10, 41).addBox(3.0F, -29.0F, -3.0F, 2.0F, 2.0F, 6.0F, 0.0F, false);
        ring2.texOffs(38, 4).addBox(-5.0F, -29.0F, -3.0F, 2.0F, 2.0F, 6.0F, 0.0F, false);

        ring3 = new ModelRenderer(this);
        ring3.setPos(0.0F, -7.0F, 0.0F);
        bb_main.addChild(ring3);
        ring3.texOffs(38, 0).addBox(-5.0F, -29.0F, -5.0F, 10.0F, 2.0F, 2.0F, 0.0F, false);
        ring3.texOffs(36, 31).addBox(-5.0F, -29.0F, 3.0F, 10.0F, 2.0F, 2.0F, 0.0F, false);
        ring3.texOffs(10, 41).addBox(3.0F, -29.0F, -3.0F, 2.0F, 2.0F, 6.0F, 0.0F, false);
        ring3.texOffs(38, 4).addBox(-5.0F, -29.0F, -3.0F, 2.0F, 2.0F, 6.0F, 0.0F, false);

    }

    @Override
    public void setupAnim(ChunkLoaderBargeEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch){
        //previously the render function, render code was moved to a method below
    }

    @Override
    public void renderToBuffer(MatrixStack matrixStack, IVertexBuilder buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha){
        bb_main.render(matrixStack, buffer, packedLight, packedOverlay);

    }

    public void setRotationAngle(ModelRenderer modelRenderer, float x, float y, float z) {
        modelRenderer.xRot = x;
        modelRenderer.yRot = y;
        modelRenderer.zRot = z;
    }
}
