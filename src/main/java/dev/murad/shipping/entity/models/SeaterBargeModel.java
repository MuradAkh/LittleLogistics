package dev.murad.shipping.entity.models;
// Made with Blockbench 4.1.1
// Exported for Minecraft version 1.15 - 1.16 with Mojang mappings
// Paste this class into your mod and generate all required imports


import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;

public class SeaterBargeModel extends EntityModel<Entity> {
	private final ModelPart bb_main;
	private final ModelPart cube_r1;
	private final ModelPart bb_main2;

	public SeaterBargeModel() {
		texWidth = 128;
		texHeight = 128;

		bb_main = new ModelRenderer(this);
		bb_main.setPos(0.0F, 23.0F, 0.0F);
		bb_main.texOffs(0, 0).addBox(-6.0F, -27.0F, -7.0F, 12.0F, 5.0F, 14.0F, 0.0F, false);
		bb_main.texOffs(38, 5).addBox(-8.0F, -29.0F, -7.0F, 2.0F, 4.0F, 14.0F, 0.0F, false);
		bb_main.texOffs(28, 43).addBox(-6.0F, -29.0F, -9.0F, 11.0F, 4.0F, 2.0F, 0.0F, false);
		bb_main.texOffs(0, 41).addBox(-6.0F, -29.0F, 7.0F, 11.0F, 4.0F, 2.0F, 0.0F, false);

		cube_r1 = new ModelRenderer(this);
		cube_r1.setPos(0.0F, 0.0F, 0.0F);
		bb_main.addChild(cube_r1);
		setRotationAngle(cube_r1, 0.0F, -1.5708F, 0.0F);


		bb_main2 = new ModelRenderer(this);
		bb_main2.setPos(0.0F, 24.0F, 0.0F);
		bb_main2.texOffs(0, 19).addBox(-5.0F, -29.0F, -5.0F, 9.0F, 1.0F, 10.0F, 0.0F, false);
		bb_main2.texOffs(9, 22).addBox(-5.0F, -35.0F, -5.0F, 1.0F, 6.0F, 10.0F, 0.0F, false);
		bb_main2.texOffs(11, 23).addBox(-4.0F, -31.0F, -5.0F, 8.0F, 2.0F, 1.0F, 0.0F, false);
		bb_main2.texOffs(11, 24).addBox(-4.0F, -31.0F, 4.0F, 8.0F, 2.0F, 1.0F, 0.0F, false);
		bb_main2.texOffs(0, 49).addBox(-4.0F, -32.0F, -6.0F, 8.0F, 1.0F, 2.0F, 0.0F, false);
		bb_main2.texOffs(0, 49).addBox(-4.0F, -32.0F, 4.0F, 8.0F, 1.0F, 2.0F, 0.0F, false);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch){
		//previously the render function, render code was moved to a method below
	}

	@Override
	public void renderToBuffer(MatrixStack matrixStack, IVertexBuilder buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha){
		bb_main.render(matrixStack, buffer, packedLight, packedOverlay);
		bb_main2.render(matrixStack, buffer, packedLight, packedOverlay);
	}

	public void setRotationAngle(ModelRenderer modelRenderer, float x, float y, float z) {
		modelRenderer.xRot = x;
		modelRenderer.yRot = y;
		modelRenderer.zRot = z;
	}
}