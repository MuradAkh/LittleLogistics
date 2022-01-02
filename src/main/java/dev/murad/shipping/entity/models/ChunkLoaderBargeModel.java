package dev.murad.shipping.entity.models;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import dev.murad.shipping.entity.custom.barge.ChunkLoaderBargeEntity;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.model.ModelRenderer;

public class ChunkLoaderBargeModel extends EntityModel<ChunkLoaderBargeEntity> {

    private final ModelRenderer bb_main;
    private final ModelRenderer bone;
    private final ModelRenderer bone3;
    private final ModelRenderer cube_r1;
    private final ModelRenderer cube_r2;
    private final ModelRenderer bone2;
    private final ModelRenderer cube_r3;
    private final ModelRenderer cube_r4;

    public ChunkLoaderBargeModel() {
        texWidth = 128;
        texHeight = 128;

        bb_main = new ModelRenderer(this);
        bb_main.setPos(0.0F, 24.0F, 0.0F);
        bb_main.texOffs(0, 0).addBox(-7.0F, -25.0F, -7.0F, 14.0F, 3.0F, 14.0F, 0.0F, false);
        bb_main.texOffs(18, 20).addBox(-8.0F, -27.0F, -8.0F, 1.0F, 3.0F, 16.0F, 0.0F, false);
        bb_main.texOffs(36, 32).addBox(-7.0F, -27.0F, -8.0F, 14.0F, 3.0F, 1.0F, 0.0F, false);
        bb_main.texOffs(0, 17).addBox(7.0F, -27.0F, -8.0F, 1.0F, 3.0F, 16.0F, 0.0F, false);
        bb_main.texOffs(36, 28).addBox(-7.0F, -27.0F, 7.0F, 14.0F, 3.0F, 1.0F, 0.0F, false);

        bone = new ModelRenderer(this);
        bone.setPos(0.0F, 24.0F, 0.0F);


        bone3 = new ModelRenderer(this);
        bone3.setPos(0.0F, 0.0F, 0.0F);
        bone.addChild(bone3);
        bone3.texOffs(42, 8).addBox(-5.0F, -28.0F, -5.0F, 10.0F, 1.0F, 1.0F, 0.0F, false);
        bone3.texOffs(42, 4).addBox(-5.0F, -28.0F, 4.0F, 10.0F, 1.0F, 1.0F, 0.0F, false);
        bone3.texOffs(0, 43).addBox(-5.0F, -26.0F, -5.0F, 10.0F, 1.0F, 1.0F, 0.0F, false);
        bone3.texOffs(22, 43).addBox(-5.0F, -26.0F, 4.0F, 10.0F, 1.0F, 1.0F, 0.0F, false);

        cube_r1 = new ModelRenderer(this);
        cube_r1.setPos(0.0F, 0.0F, 0.0F);
        bone3.addChild(cube_r1);
        setRotationAngle(cube_r1, 0.0F, -1.5708F, 0.0F);
        cube_r1.texOffs(42, 12).addBox(-5.0F, -27.0F, 4.0F, 10.0F, 1.0F, 1.0F, 0.0F, false);
        cube_r1.texOffs(42, 10).addBox(-5.0F, -27.0F, -5.0F, 10.0F, 1.0F, 1.0F, 0.0F, false);

        cube_r2 = new ModelRenderer(this);
        cube_r2.setPos(0.0F, -2.0F, 0.0F);
        bone3.addChild(cube_r2);
        setRotationAngle(cube_r2, 0.0F, -1.5708F, 0.0F);
        cube_r2.texOffs(42, 2).addBox(-5.0F, -27.0F, 4.0F, 10.0F, 1.0F, 1.0F, 0.0F, false);
        cube_r2.texOffs(42, 6).addBox(-5.0F, -27.0F, -5.0F, 10.0F, 1.0F, 1.0F, 0.0F, false);

        bone2 = new ModelRenderer(this);
        bone2.setPos(0.0F, -4.0F, 0.0F);
        bone.addChild(bone2);
        bone2.texOffs(42, 0).addBox(-5.0F, -28.0F, -5.0F, 10.0F, 1.0F, 1.0F, 0.0F, false);
        bone2.texOffs(22, 41).addBox(-5.0F, -28.0F, 4.0F, 10.0F, 1.0F, 1.0F, 0.0F, false);
        bone2.texOffs(22, 39).addBox(-5.0F, -26.0F, -5.0F, 10.0F, 1.0F, 1.0F, 0.0F, false);
        bone2.texOffs(0, 39).addBox(-5.0F, -26.0F, 4.0F, 10.0F, 1.0F, 1.0F, 0.0F, false);
        bone2.texOffs(0, 0).addBox(-1.0F, -27.0F, -1.0F, 2.0F, 3.0F, 2.0F, 0.0F, false);

        cube_r3 = new ModelRenderer(this);
        cube_r3.setPos(0.0F, 0.0F, 0.0F);
        bone2.addChild(cube_r3);
        setRotationAngle(cube_r3, 0.0F, -1.5708F, 0.0F);
        cube_r3.texOffs(18, 17).addBox(-5.0F, -27.0F, 4.0F, 10.0F, 1.0F, 1.0F, 0.0F, false);
        cube_r3.texOffs(0, 41).addBox(-5.0F, -27.0F, -5.0F, 10.0F, 1.0F, 1.0F, 0.0F, false);

        cube_r4 = new ModelRenderer(this);
        cube_r4.setPos(0.0F, -2.0F, 0.0F);
        bone2.addChild(cube_r4);
        setRotationAngle(cube_r4, 0.0F, -1.5708F, 0.0F);
        cube_r4.texOffs(36, 17).addBox(-5.0F, -27.0F, -5.0F, 10.0F, 1.0F, 10.0F, 0.0F, false);
    }

    @Override
    public void setupAnim(ChunkLoaderBargeEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch){
        //previously the render function, render code was moved to a method below
    }

    @Override
    public void renderToBuffer(MatrixStack matrixStack, IVertexBuilder buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha){
        bb_main.render(matrixStack, buffer, packedLight, packedOverlay);
        bone.render(matrixStack, buffer, packedLight, packedOverlay);
    }

    public void setRotationAngle(ModelRenderer modelRenderer, float x, float y, float z) {
        modelRenderer.xRot = x;
        modelRenderer.yRot = y;
        modelRenderer.zRot = z;
    }
}
