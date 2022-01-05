package dev.murad.shipping.entity.models;// Made with Blockbench 4.1.1
// Exported for Minecraft version 1.15 - 1.16 with Mojang mappings
// Paste this class into your mod and generate all required imports


import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.Entity;

public class FishingBargeModel extends EntityModel<Entity> {
	private final ModelRenderer bb_main;
	private final ModelRenderer bone;
	private final ModelRenderer bone4;
	private final ModelRenderer bone3;
	private final ModelRenderer bone2;
	private final ModelRenderer bone6;
	private final ModelRenderer bone5;
	private final ModelRenderer bone7;

	public FishingBargeModel() {
		texWidth = 64;
		texHeight = 64;

		bb_main = new ModelRenderer(this);
		bb_main.setPos(0.0F, 24.0F, 0.0F);
		bb_main.texOffs(0, 0).addBox(-7.0F, -25.0F, -7.0F, 14.0F, 3.0F, 14.0F, 0.0F, false);
		bb_main.texOffs(16, 20).addBox(-8.0F, -27.0F, -7.0F, 1.0F, 3.0F, 14.0F, 0.0F, false);
		bb_main.texOffs(0, 17).addBox(7.0F, -27.0F, -7.0F, 1.0F, 3.0F, 14.0F, 0.0F, false);

		bone = new ModelRenderer(this);
		bone.setPos(0.0F, 24.0F, 0.0F);


		bone4 = new ModelRenderer(this);
		bone4.setPos(27.0F, 24.0F, 0.0F);
		bone4.texOffs(12, 34).addBox(-21.0F, -35.0F, 3.0F, 1.0F, 9.0F, 1.0F, 0.0F, false);
		bone4.texOffs(8, 34).addBox(-34.0F, -35.0F, 3.0F, 1.0F, 9.0F, 1.0F, 0.0F, false);

		bone3 = new ModelRenderer(this);
		bone3.setPos(34.0F, 24.0F, 0.0F);
		bone3.texOffs(4, 34).addBox(-28.0F, -35.0F, -4.0F, 1.0F, 9.0F, 1.0F, 0.0F, false);
		bone3.texOffs(0, 34).addBox(-41.0F, -35.0F, -4.0F, 1.0F, 9.0F, 1.0F, 0.0F, false);

		bone2 = new ModelRenderer(this);
		bone2.setPos(17.0F, 14.0F, 0.0F);
		bone2.texOffs(16, 19).addBox(-12.0F, -25.0F, -4.0F, 1.0F, 3.0F, 1.0F, 0.0F, false);
		bone2.texOffs(0, 17).addBox(-23.0F, -25.0F, -4.0F, 1.0F, 3.0F, 1.0F, 0.0F, false);
		bone2.texOffs(32, 23).addBox(-23.0F, -22.0F, -1.0F, 12.0F, 1.0F, 1.0F, 0.0F, false);
		bone2.texOffs(16, 19).addBox(-23.0F, -22.0F, -6.0F, 1.0F, 1.0F, 5.0F, 0.0F, false);
		bone2.texOffs(32, 21).addBox(-23.0F, -22.0F, -7.0F, 12.0F, 1.0F, 1.0F, 0.0F, false);
		bone2.texOffs(0, 17).addBox(-12.0F, -22.0F, -6.0F, 1.0F, 1.0F, 5.0F, 0.0F, false);

		bone6 = new ModelRenderer(this);
		bone6.setPos(-17.0F, 28.0F, -17.0F);
		bone2.addChild(bone6);
		bone6.texOffs(36, 37).addBox(1.0F, -49.0F, 15.0F, 1.0F, 1.0F, 1.0F, 0.0F, false);
		bone6.texOffs(23, 21).addBox(3.0F, -49.0F, 15.0F, 2.0F, 1.0F, 1.0F, 0.0F, false);
		bone6.texOffs(23, 19).addBox(-5.0F, -49.0F, 15.0F, 2.0F, 1.0F, 1.0F, 0.0F, false);
		bone6.texOffs(32, 37).addBox(-2.0F, -49.0F, 15.0F, 1.0F, 1.0F, 1.0F, 0.0F, false);
		bone6.texOffs(28, 37).addBox(-5.0F, -49.0F, 14.0F, 1.0F, 1.0F, 1.0F, 0.0F, false);
		bone6.texOffs(24, 37).addBox(-5.0F, -49.0F, 12.0F, 1.0F, 1.0F, 1.0F, 0.0F, false);
		bone6.texOffs(20, 37).addBox(4.0F, -49.0F, 12.0F, 1.0F, 1.0F, 1.0F, 0.0F, false);
		bone6.texOffs(16, 37).addBox(4.0F, -49.0F, 14.0F, 1.0F, 1.0F, 1.0F, 0.0F, false);
		bone6.texOffs(35, 32).addBox(-2.0F, -49.0F, 11.0F, 1.0F, 1.0F, 1.0F, 0.0F, false);
		bone6.texOffs(35, 30).addBox(-3.0F, -48.0F, 12.0F, 1.0F, 1.0F, 1.0F, 0.0F, false);
		bone6.texOffs(35, 28).addBox(-4.0F, -48.0F, 13.0F, 1.0F, 1.0F, 1.0F, 0.0F, false);
		bone6.texOffs(35, 26).addBox(3.0F, -48.0F, 13.0F, 1.0F, 1.0F, 1.0F, 0.0F, false);
		bone6.texOffs(6, 23).addBox(-1.0F, -48.0F, 12.0F, 2.0F, 1.0F, 1.0F, 0.0F, false);
		bone6.texOffs(32, 31).addBox(-2.0F, -47.0F, 13.0F, 1.0F, 1.0F, 1.0F, 0.0F, false);
		bone6.texOffs(32, 29).addBox(1.0F, -47.0F, 13.0F, 1.0F, 1.0F, 1.0F, 0.0F, false);
		bone6.texOffs(0, 23).addBox(-1.0F, -48.0F, 14.0F, 2.0F, 1.0F, 1.0F, 0.0F, false);
		bone6.texOffs(32, 27).addBox(2.0F, -48.0F, 12.0F, 1.0F, 1.0F, 1.0F, 0.0F, false);
		bone6.texOffs(32, 25).addBox(-3.0F, -48.0F, 14.0F, 1.0F, 1.0F, 1.0F, 0.0F, false);
		bone6.texOffs(24, 29).addBox(2.0F, -48.0F, 14.0F, 1.0F, 1.0F, 1.0F, 0.0F, false);
		bone6.texOffs(20, 29).addBox(1.0F, -49.0F, 11.0F, 1.0F, 1.0F, 1.0F, 0.0F, false);
		bone6.texOffs(7, 19).addBox(3.0F, -49.0F, 11.0F, 2.0F, 1.0F, 1.0F, 0.0F, false);
		bone6.texOffs(7, 17).addBox(-5.0F, -49.0F, 11.0F, 2.0F, 1.0F, 1.0F, 0.0F, false);

		bone5 = new ModelRenderer(this);
		bone5.setPos(17.0F, 14.0F, 7.0F);
		bone5.texOffs(0, 6).addBox(-12.0F, -25.0F, -4.0F, 1.0F, 3.0F, 1.0F, 0.0F, false);
		bone5.texOffs(0, 0).addBox(-23.0F, -25.0F, -4.0F, 1.0F, 3.0F, 1.0F, 0.0F, false);
		bone5.texOffs(32, 19).addBox(-23.0F, -22.0F, -1.0F, 12.0F, 1.0F, 1.0F, 0.0F, false);
		bone5.texOffs(0, 6).addBox(-23.0F, -22.0F, -6.0F, 1.0F, 1.0F, 5.0F, 0.0F, false);
		bone5.texOffs(16, 17).addBox(-23.0F, -22.0F, -7.0F, 12.0F, 1.0F, 1.0F, 0.0F, false);
		bone5.texOffs(0, 0).addBox(-12.0F, -22.0F, -6.0F, 1.0F, 1.0F, 5.0F, 0.0F, false);

		bone7 = new ModelRenderer(this);
		bone7.setPos(-17.0F, 28.0F, -17.0F);
		bone5.addChild(bone7);
		bone7.texOffs(16, 29).addBox(1.0F, -49.0F, 15.0F, 1.0F, 1.0F, 1.0F, 0.0F, false);
		bone7.texOffs(6, 12).addBox(3.0F, -49.0F, 15.0F, 2.0F, 1.0F, 1.0F, 0.0F, false);
		bone7.texOffs(0, 12).addBox(-5.0F, -49.0F, 15.0F, 2.0F, 1.0F, 1.0F, 0.0F, false);
		bone7.texOffs(8, 29).addBox(-2.0F, -49.0F, 15.0F, 1.0F, 1.0F, 1.0F, 0.0F, false);
		bone7.texOffs(4, 29).addBox(-5.0F, -49.0F, 14.0F, 1.0F, 1.0F, 1.0F, 0.0F, false);
		bone7.texOffs(0, 29).addBox(-5.0F, -49.0F, 12.0F, 1.0F, 1.0F, 1.0F, 0.0F, false);
		bone7.texOffs(24, 27).addBox(4.0F, -49.0F, 12.0F, 1.0F, 1.0F, 1.0F, 0.0F, false);
		bone7.texOffs(20, 27).addBox(4.0F, -49.0F, 14.0F, 1.0F, 1.0F, 1.0F, 0.0F, false);
		bone7.texOffs(16, 27).addBox(-2.0F, -49.0F, 11.0F, 1.0F, 1.0F, 1.0F, 0.0F, false);
		bone7.texOffs(8, 27).addBox(-3.0F, -48.0F, 12.0F, 1.0F, 1.0F, 1.0F, 0.0F, false);
		bone7.texOffs(4, 27).addBox(-4.0F, -48.0F, 13.0F, 1.0F, 1.0F, 1.0F, 0.0F, false);
		bone7.texOffs(0, 27).addBox(3.0F, -48.0F, 13.0F, 1.0F, 1.0F, 1.0F, 0.0F, false);
		bone7.texOffs(7, 8).addBox(-1.0F, -48.0F, 12.0F, 2.0F, 1.0F, 1.0F, 0.0F, false);
		bone7.texOffs(24, 25).addBox(-2.0F, -47.0F, 13.0F, 1.0F, 1.0F, 1.0F, 0.0F, false);
		bone7.texOffs(20, 25).addBox(1.0F, -47.0F, 13.0F, 1.0F, 1.0F, 1.0F, 0.0F, false);
		bone7.texOffs(7, 6).addBox(-1.0F, -48.0F, 14.0F, 2.0F, 1.0F, 1.0F, 0.0F, false);
		bone7.texOffs(16, 25).addBox(2.0F, -48.0F, 12.0F, 1.0F, 1.0F, 1.0F, 0.0F, false);
		bone7.texOffs(8, 25).addBox(-3.0F, -48.0F, 14.0F, 1.0F, 1.0F, 1.0F, 0.0F, false);
		bone7.texOffs(4, 25).addBox(2.0F, -48.0F, 14.0F, 1.0F, 1.0F, 1.0F, 0.0F, false);
		bone7.texOffs(0, 25).addBox(1.0F, -49.0F, 11.0F, 1.0F, 1.0F, 1.0F, 0.0F, false);
		bone7.texOffs(7, 2).addBox(3.0F, -49.0F, 11.0F, 2.0F, 1.0F, 1.0F, 0.0F, false);
		bone7.texOffs(7, 0).addBox(-5.0F, -49.0F, 11.0F, 2.0F, 1.0F, 1.0F, 0.0F, false);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch){
		//previously the render function, render code was moved to a method below
	}

	@Override
	public void renderToBuffer(MatrixStack matrixStack, IVertexBuilder buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha){
		bb_main.render(matrixStack, buffer, packedLight, packedOverlay);
		bone.render(matrixStack, buffer, packedLight, packedOverlay);
		bone4.render(matrixStack, buffer, packedLight, packedOverlay);
		bone3.render(matrixStack, buffer, packedLight, packedOverlay);
		bone2.render(matrixStack, buffer, packedLight, packedOverlay);
		bone5.render(matrixStack, buffer, packedLight, packedOverlay);
	}

	public void setRotationAngle(ModelRenderer modelRenderer, float x, float y, float z) {
		modelRenderer.xRot = x;
		modelRenderer.yRot = y;
		modelRenderer.zRot = z;
	}
}