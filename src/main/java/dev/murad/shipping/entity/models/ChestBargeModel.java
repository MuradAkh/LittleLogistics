package dev.murad.shipping.entity.models;// Made with Blockbench 4.0.5
// Exported for Minecraft version 1.15 - 1.16 with Mojang mappings
// Paste this class into your mod and generate all required imports

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import dev.murad.shipping.entity.custom.barge.ChestBargeEntity;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.model.ModelRenderer;

public class ChestBargeModel extends EntityModel<ChestBargeEntity> {
	private final ModelRenderer bb_main;

	public ChestBargeModel() {
		texWidth = 128;
		texHeight = 128;

		bb_main = new ModelRenderer(this);
		bb_main.setPos(0.0F, 24.0F, 0.0F);
		bb_main.texOffs(0, 23).addBox(-7.0F, -25.0F, -7.0F, 14.0F, 3.0F, 14.0F, 0.0F, false);
		bb_main.texOffs(40, 24).addBox(-8.0F, -27.0F, -8.0F, 1.0F, 3.0F, 16.0F, 0.0F, false);
		bb_main.texOffs(38, 4).addBox(-7.0F, -27.0F, -8.0F, 14.0F, 3.0F, 1.0F, 0.0F, false);
		bb_main.texOffs(0, 40).addBox(7.0F, -27.0F, -8.0F, 1.0F, 3.0F, 16.0F, 0.0F, false);
		bb_main.texOffs(38, 0).addBox(-7.0F, -27.0F, 7.0F, 14.0F, 3.0F, 1.0F, 0.0F, false);
		bb_main.texOffs(0, 0).addBox(-6.0F, -34.0F, -7.0F, 12.0F, 9.0F, 14.0F, 0.0F, false);
	}

	@Override
	public void setupAnim(ChestBargeEntity p_225597_1_, float p_225597_2_, float p_225597_3_, float p_225597_4_, float p_225597_5_, float p_225597_6_) {

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