package dev.murad.shipping.entity.models;// Made with Blockbench 4.1.1
// Exported for Minecraft version 1.15 - 1.16 with Mojang mappings
// Paste this class into your mod and generate all required imports


import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import dev.murad.shipping.entity.custom.barge.FluidTankBargeEntity;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.Entity;

public class FluidTankBargeModel extends EntityModel<FluidTankBargeEntity> {
	private final ModelRenderer bb_main;

	public FluidTankBargeModel() {
		texWidth = 128;
		texHeight = 128;
		bb_main = new ModelRenderer(this);
		bb_main.setPos(0.0F, 23.0F, 0.0F);
		bb_main.texOffs(0, 0).addBox(-6.0F, -27.0F, -7.0F, 12.0F, 5.0F, 14.0F, 0.0F, false);
		bb_main.texOffs(0, 32).addBox(-8.0F, -29.0F, -7.0F, 2.0F, 4.0F, 14.0F, 0.0F, false);
		bb_main.texOffs(38, 0).addBox(-6.0F, -29.0F, -9.0F, 12.0F, 4.0F, 2.0F, 0.0F, false);
		bb_main.texOffs(30, 19).addBox(6.0F, -29.0F, -7.0F, 2.0F, 4.0F, 14.0F, 0.0F, false);
		bb_main.texOffs(18, 37).addBox(-6.0F, -29.0F, 7.0F, 12.0F, 4.0F, 2.0F, 0.0F, false);
		bb_main.texOffs(48, 37).addBox(1.0F, -35.0F, 5.0F, 3.0F, 8.0F, 1.0F, 0.0F, false);
		bb_main.texOffs(48, 19).addBox(-4.0F, -35.0F, 5.0F, 3.0F, 8.0F, 1.0F, 0.0F, false);
		bb_main.texOffs(40, 43).addBox(-4.0F, -35.0F, -6.0F, 3.0F, 8.0F, 1.0F, 0.0F, false);
		bb_main.texOffs(32, 43).addBox(1.0F, -35.0F, -6.0F, 3.0F, 8.0F, 1.0F, 0.0F, false);
		bb_main.texOffs(32, 19).addBox(4.0F, -35.0F, -6.0F, 1.0F, 8.0F, 4.0F, 0.0F, false);
		bb_main.texOffs(0, 19).addBox(-5.0F, -35.0F, -6.0F, 1.0F, 8.0F, 4.0F, 0.0F, false);
		bb_main.texOffs(38, 6).addBox(-2.0F, -37.0F, -2.0F, 4.0F, 1.0F, 4.0F, 0.0F, false);
		bb_main.texOffs(0, 19).addBox(-5.0F, -36.0F, -6.0F, 10.0F, 1.0F, 12.0F, 0.0F, false);
		bb_main.texOffs(0, 0).addBox(-5.0F, -35.0F, 2.0F, 1.0F, 8.0F, 4.0F, 0.0F, false);
		bb_main.texOffs(0, 32).addBox(4.0F, -35.0F, 2.0F, 1.0F, 8.0F, 4.0F, 0.0F, false);
		bb_main.texOffs(11, 22).addBox(4.0F, -30.0F, -2.0F, 1.0F, 3.0F, 4.0F, 0.0F, false);
		bb_main.texOffs(13, 22).addBox(-5.0F, -30.0F, -2.0F, 1.0F, 3.0F, 4.0F, 0.0F, false);
		bb_main.texOffs(11, 22).addBox(-1.0F, -30.0F, 5.0F, 2.0F, 3.0F, 1.0F, 0.0F, false);
		bb_main.texOffs(11, 22).addBox(-1.0F, -30.0F, -6.0F, 2.0F, 3.0F, 1.0F, 0.0F, false);

	}

	@Override
	public void setupAnim(FluidTankBargeEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch){
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