package dev.murad.shipping.entity.models;// Made with Blockbench 4.1.1
// Exported for Minecraft version 1.15 - 1.16 with Mojang mappings
// Paste this class into your mod and generate all required imports


import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import dev.murad.shipping.entity.custom.tug.AbstractTugEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;

public class EnergyTugModel extends EntityModel<AbstractTugEntity> {
	private final ModelPart bb_main;

	public EnergyTugModel() {
		texWidth = 128;
		texHeight = 128;

		bb_main = new ModelRenderer(this);
		bb_main.setPos(0.0F, 24.0F, 0.0F);
		bb_main.texOffs(0, 30).addBox(-7.0F, -9.0F, -19.0F, 14.0F, 6.0F, 24.0F, 0.0F, false);
		bb_main.texOffs(58, 53).addBox(-9.0F, -11.0F, -19.0F, 2.0F, 5.0F, 18.0F, 0.0F, false);
		bb_main.texOffs(52, 17).addBox(-7.0F, -11.0F, -21.0F, 14.0F, 5.0F, 2.0F, 0.0F, false);
		bb_main.texOffs(52, 30).addBox(7.0F, -11.0F, -19.0F, 2.0F, 5.0F, 18.0F, 0.0F, false);
		bb_main.texOffs(0, 60).addBox(-9.0F, -10.0F, -21.0F, 18.0F, 2.0F, 6.0F, 0.25F, false);
		bb_main.texOffs(52, 0).addBox(-6.0F, -14.0F, -17.0F, 12.0F, 5.0F, 12.0F, 0.0F, false);
		bb_main.texOffs(0, 0).addBox(-3.0F, -20.0F, -14.0F, 6.0F, 6.0F, 6.0F, 0.0F, false);
		bb_main.texOffs(40, 60).addBox(-4.0F, -21.0F, -15.0F, 8.0F, 1.0F, 8.0F, 0.0F, false);
		bb_main.texOffs(0, 0).addBox(-7.0F, -9.0F, -19.0F, 14.0F, 6.0F, 24.0F, 0.25F, false);
		bb_main.texOffs(52, 42).addBox(-0.5F, -27.0F, -11.0F, 1.0F, 6.0F, 0.0F, 0.0F, false);	}

	@Override
	public void setupAnim(AbstractTugEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch){
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