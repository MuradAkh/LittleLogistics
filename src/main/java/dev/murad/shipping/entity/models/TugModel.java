package dev.murad.shipping.entity.models;// Made with Blockbench 4.0.5
// Exported for Minecraft version 1.15 - 1.16 with Mojang mappings
// Paste this class into your mod and generate all required imports


import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import dev.murad.shipping.entity.custom.tug.TugEntity;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.model.ModelRenderer;

public class TugModel extends EntityModel<TugEntity> {
    private final ModelRenderer bb_main;

    public TugModel() {
        texWidth = 128;
        texHeight = 128;
        bb_main = new ModelRenderer(this);
        bb_main.setPos(0.0F, 24.0F, 0.0F);
        bb_main.texOffs(0, 0).addBox(-7.0F, -30.0F, -19.0F, 14.0F, 6.0F, 24.0F, 0.0F, false);
        bb_main.texOffs(0, 50).addBox(-9.0F, -32.0F, -19.0F, 2.0F, 4.0F, 24.0F, 0.0F, false);
        bb_main.texOffs(44, 30).addBox(7.0F, -32.0F, -19.0F, 2.0F, 4.0F, 24.0F, 0.0F, false);
        bb_main.texOffs(60, 66).addBox(-7.0F, -32.0F, 5.0F, 14.0F, 4.0F, 2.0F, 0.0F, false);
        bb_main.texOffs(28, 66).addBox(-7.0F, -32.0F, -21.0F, 14.0F, 4.0F, 2.0F, 0.0F, false);
        bb_main.texOffs(53, 0).addBox(-6.0F, -38.0F, -13.0F, 12.0F, 8.0F, 14.0F, 0.0F, false);
        bb_main.texOffs(0, 30).addBox(-8.0F, -40.0F, -15.0F, 16.0F, 2.0F, 18.0F, 0.0F, false);
        bb_main.texOffs(0, 0).addBox(-2.0F, -46.0F, -5.0F, 4.0F, 6.0F, 4.0F, 0.0F, false);
        bb_main.texOffs(0, 10).addBox(-2.0F, -45.25F, -5.0F, 4.0F, 2.0F, 4.0F, 0.5F, false);
        bb_main.texOffs(28, 58).addBox(-9.0F, -31.0F, -21.0F, 18.0F, 2.0F, 6.0F, 0.25F, false);
        bb_main.texOffs(0, 0).addBox(-2.0F, -46.0F, -11.0F, 4.0F, 6.0F, 4.0F, 0.0F, false);
        bb_main.texOffs(0, 10).addBox(-2.0F, -45.25F, -11.0F, 4.0F, 2.0F, 4.0F, 0.5F, false);
        }

    @Override
    public void setupAnim(TugEntity p_225597_1_, float p_225597_2_, float p_225597_3_, float p_225597_4_, float p_225597_5_, float p_225597_6_) {

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