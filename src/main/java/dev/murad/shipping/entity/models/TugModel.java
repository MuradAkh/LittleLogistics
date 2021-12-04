package dev.murad.shipping.entity.models;// Made with Blockbench 4.0.5
// Exported for Minecraft version 1.15 - 1.16 with Mojang mappings
// Paste this class into your mod and generate all required imports


import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import dev.murad.shipping.entity.custom.TugEntity;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.Entity;

public class TugModel extends EntityModel<TugEntity> {
    private final ModelRenderer bb_main;

    public TugModel() {
        texWidth = 128;
        texHeight = 128;

        bb_main = new ModelRenderer(this);
        bb_main.setPos(0.0F, 24.0F, 0.0F);
        bb_main.texOffs(0, 0).addBox(-7.0F, -27.0F, -19.0F, 14.0F, 3.0F, 24.0F, 0.0F, false);
        bb_main.texOffs(0, 4).addBox(-5.0F, -28.0F, -21.0F, 10.0F, 2.0F, 2.0F, 0.0F, false);
        bb_main.texOffs(0, 0).addBox(-5.0F, -28.0F, 5.0F, 10.0F, 2.0F, 2.0F, 0.0F, false);
        bb_main.texOffs(24, 30).addBox(-8.0F, -29.0F, -18.0F, 1.0F, 3.0F, 22.0F, 0.0F, false);
        bb_main.texOffs(0, 27).addBox(-4.0F, -30.0F, -22.0F, 8.0F, 3.0F, 1.0F, 0.0F, false);
        bb_main.texOffs(0, 27).addBox(7.0F, -29.0F, -18.0F, 1.0F, 3.0F, 22.0F, 0.0F, false);
        bb_main.texOffs(0, 19).addBox(-4.0F, -30.0F, 7.0F, 8.0F, 3.0F, 1.0F, 0.0F, false);
        bb_main.texOffs(48, 27).addBox(-6.0F, -36.0F, -12.0F, 12.0F, 9.0F, 11.0F, 0.0F, false);
        bb_main.texOffs(0, 52).addBox(-3.0F, -33.0F, -8.0F, 6.0F, 6.0F, 12.0F, 0.0F, false);
        bb_main.texOffs(52, 0).addBox(-3.0F, -33.0F, -18.0F, 6.0F, 6.0F, 11.0F, 0.0F, false);
        bb_main.texOffs(0, 8).addBox(-2.0F, -41.0F, -8.0F, 4.0F, 7.0F, 4.0F, 0.0F, false);
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