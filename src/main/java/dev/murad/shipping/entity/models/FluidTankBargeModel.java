package dev.murad.shipping.entity.models;// Made with Blockbench 4.1.1
// Exported for Minecraft version 1.15 - 1.16 with Mojang mappings
// Paste this class into your mod and generate all required imports


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.barge.FluidTankBargeEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;

public class FluidTankBargeModel extends EntityModel<FluidTankBargeEntity> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(ShippingMod.MOD_ID, "fluidtankbargemodel"), "main");
	private final ModelPart bb_main;

	public FluidTankBargeModel(ModelPart root) {
		this.bb_main = root.getChild("bb_main");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition bb_main = partdefinition.addOrReplaceChild("bb_main", CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -27.0F, -7.0F, 12.0F, 5.0F, 14.0F, new CubeDeformation(0.0F))
				.texOffs(38, 5).addBox(-8.0F, -29.0F, -7.0F, 2.0F, 4.0F, 14.0F, new CubeDeformation(0.0F))
				.texOffs(28, 43).addBox(-6.0F, -29.0F, -9.0F, 12.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(26, 25).addBox(6.0F, -29.0F, -7.0F, 2.0F, 4.0F, 14.0F, new CubeDeformation(0.0F))
				.texOffs(0, 41).addBox(-6.0F, -29.0F, 7.0F, 12.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(16, 49).addBox(-6.0F, -28.0F, -5.0F, 12.0F, 1.0F, 12.0F, new CubeDeformation(0.0F))
				.texOffs(0, 0).addBox(-6.0F, -38.0F, -6.0F, 4.0F, 10.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(0, 0).addBox(2.0F, -38.0F, -6.0F, 4.0F, 10.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(38, 0).addBox(-2.0F, -38.0F, -4.0F, 4.0F, 10.0F, 0.0F, new CubeDeformation(0.0F))
				.texOffs(38, 0).addBox(-2.0F, -38.0F, 5.0F, 4.0F, 10.0F, 0.0F, new CubeDeformation(0.0F))
				.texOffs(0, 0).addBox(2.0F, -38.0F, 4.0F, 4.0F, 10.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(0, 0).addBox(4.0F, -38.0F, 2.0F, 2.0F, 10.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(38, -4).addBox(5.0F, -38.0F, -2.0F, 0.0F, 10.0F, 4.0F, new CubeDeformation(0.0F))
				.texOffs(38, -4).addBox(-5.0F, -38.0F, -2.0F, 0.0F, 10.0F, 4.0F, new CubeDeformation(0.0F))
				.texOffs(0, 0).addBox(4.0F, -38.0F, -4.0F, 2.0F, 10.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(0, 0).addBox(-6.0F, -38.0F, -4.0F, 2.0F, 10.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(0, 0).addBox(-6.0F, -38.0F, 2.0F, 2.0F, 10.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(0, 0).addBox(-6.0F, -38.0F, 4.0F, 4.0F, 10.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(16, 49).addBox(-6.0F, -39.0F, -6.0F, 12.0F, 1.0F, 12.0F, new CubeDeformation(0.0F))
				.texOffs(72, 0).addBox(-4.0F, -40.0F, -4.0F, 8.0F, 1.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 23.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void setupAnim(FluidTankBargeEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		bb_main.render(poseStack, buffer, packedLight, packedOverlay);
	}
}