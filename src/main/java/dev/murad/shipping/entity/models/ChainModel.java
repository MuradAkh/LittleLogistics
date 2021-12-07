package dev.murad.shipping.entity.models;// Made with Blockbench 4.0.5
// Exported for Minecraft version 1.15 - 1.16 with Mojang mappings
// Paste this class into your mod and generate all required imports


import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import dev.murad.shipping.entity.custom.SpringEntity;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.model.ModelRenderer;

public class ChainModel extends EntityModel<SpringEntity> {
	private final ModelRenderer bb_main;
	private final ModelRenderer cube_r1;

	public ChainModel() {
		texWidth = 64;
		texHeight = 64;

		bb_main = new ModelRenderer(this);
		bb_main.setPos(0.0F, 24.0F, 0.0F);


		cube_r1 = new ModelRenderer(this);
		cube_r1.setPos(0.0F, 0.0F, 0.0F);
		bb_main.addChild(cube_r1);
		setRotationAngle(cube_r1, 0.0F, -1.5708F, 0.0F);
		cube_r1.texOffs(0, 0).addBox(0.0F, -25.0F, -7.0F, 1.0F, 1.0F, 1.0F, 0.0F, false);
		cube_r1.texOffs(0, 2).addBox(0.0F, -25.0F, -3.0F, 1.0F, 1.0F, 1.0F, 0.0F, false);
		cube_r1.texOffs(3, 1).addBox(-1.0F, -25.0F, -5.0F, 1.0F, 1.0F, 1.0F, 0.0F, false);
		cube_r1.texOffs(3, 3).addBox(0.0F, -25.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F, false);
		cube_r1.texOffs(0, 4).addBox(-1.0F, -25.0F, -1.0F, 1.0F, 1.0F, 1.0F, 0.0F, false);
		cube_r1.texOffs(3, 5).addBox(-1.0F, -25.0F, 7.0F, 1.0F, 1.0F, 1.0F, 0.0F, false);
		cube_r1.texOffs(0, 6).addBox(0.0F, -25.0F, 5.0F, 1.0F, 1.0F, 1.0F, 0.0F, false);
		cube_r1.texOffs(6, 0).addBox(-1.0F, -25.0F, 3.0F, 1.0F, 1.0F, 1.0F, 0.0F, false);
		cube_r1.texOffs(0, 0).addBox(-1.0F, -26.0F, -7.0F, 2.0F, 1.0F, 15.0F, 0.0F, false);
		cube_r1.texOffs(0, 16).addBox(-1.0F, -24.0F, -7.0F, 2.0F, 1.0F, 15.0F, 0.0F, false);
	}

	@Override
	public void setupAnim(SpringEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch){
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