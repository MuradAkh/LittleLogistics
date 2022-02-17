package dev.murad.shipping.entity.models;
// Made with Blockbench 4.1.1
// Exported for Minecraft version 1.15 - 1.16 with Mojang mappings
// Paste this class into your mod and generate all required imports


import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;

public class FishingBargeDeployedModel extends EntityModel<Entity> {
	private final ModelPart bb_main;
	private final ModelPart bone;
	private final ModelPart bone2;
	private final ModelPart cube_r1;
	private final ModelPart bone3;
	private final ModelPart bone4;
	private final ModelPart cube_r2;

	public FishingBargeDeployedModel() {
		texWidth = 128;
		texHeight = 128;

		bb_main = new ModelRenderer(this);
		bb_main.setPos(0.0F, 24.0F, 0.0F);
		bb_main.texOffs(0, 0).addBox(-6.0F, -28.0F, -7.0F, 12.0F, 5.0F, 14.0F, 0.0F, false);
		bb_main.texOffs(18, 23).addBox(-8.0F, -30.0F, -7.0F, 2.0F, 4.0F, 14.0F, 0.0F, false);
		bb_main.texOffs(0, 19).addBox(6.0F, -30.0F, -7.0F, 2.0F, 4.0F, 14.0F, 0.0F, false);

		bone = new ModelRenderer(this);
		bone.setPos(0.0F, -3.0F, -4.0F);
		setRotationAngle(bone, 1.5708F, 0.0F, 0.0F);
		bone.texOffs(6, 0).addBox(-6.0F, -9.0F, 0.0F, 1.0F, 9.0F, 2.0F, 0.0F, false);
		bone.texOffs(0, 0).addBox(5.0F, -9.0F, 0.0F, 1.0F, 9.0F, 2.0F, 0.0F, false);

		bone2 = new ModelRenderer(this);
		bone2.setPos(0.0F, -7.0F, -1.0F);
		bone.addChild(bone2);
		setRotationAngle(bone2, -1.5708F, 0.0F, 0.0F);
		bone2.texOffs(36, 19).addBox(-5.0F, -1.0F, -4.0F, 10.0F, 4.0F, 7.0F, 0.0F, false);
		bone2.texOffs(38, 8).addBox(-5.0F, -1.0F, -1.0F, 1.0F, 4.0F, 2.0F, 0.0F, false);

		cube_r1 = new ModelRenderer(this);
		cube_r1.setPos(4.5F, 2.0F, 0.0F);
		bone2.addChild(cube_r1);
		setRotationAngle(cube_r1, 0.0F, 3.1416F, 0.0F);
		cube_r1.texOffs(44, 8).addBox(-0.5F, -3.0F, -1.0F, 1.0F, 4.0F, 2.0F, 0.0F, true);

		bone3 = new ModelRenderer(this);
		bone3.setPos(0.0F, -3.0F, 4.0F);
		setRotationAngle(bone3, -1.5708F, 0.0F, 0.0F);
		bone3.texOffs(6, 0).addBox(-6.0F, -9.0F, -2.0F, 1.0F, 9.0F, 2.0F, 0.0F, false);
		bone3.texOffs(0, 0).addBox(5.0F, -9.0F, -2.0F, 1.0F, 9.0F, 2.0F, 0.0F, false);

		bone4 = new ModelRenderer(this);
		bone4.setPos(0.0F, -7.0F, 1.0F);
		bone3.addChild(bone4);
		setRotationAngle(bone4, 1.5708F, 0.0F, 0.0F);
		bone4.texOffs(36, 19).addBox(-5.0F, -1.0F, -3.0F, 10.0F, 4.0F, 7.0F, 0.0F, false);
		bone4.texOffs(38, 8).addBox(-5.0F, -1.0F, -1.0F, 1.0F, 4.0F, 2.0F, 0.0F, false);

		cube_r2 = new ModelRenderer(this);
		cube_r2.setPos(4.5F, 2.0F, 0.0F);
		bone4.addChild(cube_r2);
		setRotationAngle(cube_r2, 0.0F, -3.1416F, 0.0F);
		cube_r2.texOffs(38, 8).addBox(-0.5F, -3.0F, -1.0F, 1.0F, 4.0F, 2.0F, 0.0F, false);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch){
		//previously the render function, render code was moved to a method below
	}

	@Override
	public void renderToBuffer(MatrixStack matrixStack, IVertexBuilder buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha){
		bb_main.render(matrixStack, buffer, packedLight, packedOverlay);
		bone.render(matrixStack, buffer, packedLight, packedOverlay);
		bone3.render(matrixStack, buffer, packedLight, packedOverlay);

	}

	public void setRotationAngle(ModelRenderer modelRenderer, float x, float y, float z) {
		modelRenderer.xRot = x;
		modelRenderer.yRot = y;
		modelRenderer.zRot = z;
	}
}