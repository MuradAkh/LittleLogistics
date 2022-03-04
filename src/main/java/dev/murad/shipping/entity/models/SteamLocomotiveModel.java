package dev.murad.shipping.entity.models;// Made with Blockbench 4.1.1
// Exported for Minecraft version 1.17 with Mojang mappings
// Paste this class into your mod and generate all required imports


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.train.LocomotiveEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class SteamLocomotiveModel extends EntityModel<LocomotiveEntity> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(ShippingMod.MOD_ID, "steamlocomotivemodel"), "main");
	private final ModelPart bb_main;

	public SteamLocomotiveModel(ModelPart root) {
		this.bb_main = root.getChild("bb_main");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition bb_main = partdefinition.addOrReplaceChild("bb_main", CubeListBuilder.create().texOffs(0, 46).addBox(-4.0F, -14.0F, -14.0F, 8.0F, 8.0F, 15.0F, new CubeDeformation(0.0F))
		.texOffs(46, 0).addBox(-6.0F, -16.0F, 1.0F, 12.0F, 10.0F, 7.0F, new CubeDeformation(0.0F))
		.texOffs(32, 23).addBox(-5.0F, -17.0F, 2.0F, 10.0F, 1.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-6.0F, -6.0F, -14.0F, 12.0F, 1.0F, 22.0F, new CubeDeformation(0.0F))
		.texOffs(0, 23).addBox(-3.0F, -5.0F, -12.0F, 6.0F, 3.0F, 20.0F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-5.0F, -9.0F, -9.0F, 0.0F, 3.0F, 10.0F, new CubeDeformation(0.0F))
		.texOffs(37, 31).addBox(-4.0F, -14.0F, -14.0F, 8.0F, 8.0F, 15.0F, new CubeDeformation(0.25F))
		.texOffs(0, 19).addBox(-1.0F, -15.0F, -15.0F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(0, 30).addBox(-1.5F, -18.0F, -12.0F, 3.0F, 4.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(0, 4).addBox(-1.0F, -16.0F, -7.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(0, 23).addBox(-1.5F, -18.0F, -12.0F, 3.0F, 4.0F, 3.0F, new CubeDeformation(0.25F))
		.texOffs(31, 50).addBox(-4.0F, -4.0F, -11.0F, 1.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(0, 46).addBox(-4.0F, -4.0F, -2.0F, 1.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(38, 33).addBox(-4.0F, -4.0F, 3.0F, 1.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(14, 16).addBox(-1.0F, -15.0F, -15.0F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.25F))
		.texOffs(8, 33).addBox(3.0F, -4.0F, 3.0F, 1.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(32, 29).addBox(3.0F, -4.0F, -2.0F, 1.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(12, 0).addBox(3.0F, -4.0F, -11.0F, 1.0F, 4.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(0, 54).addBox(3.0F, -5.0F, -6.0F, 2.0F, 4.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(7, 7).addBox(-1.0F, -5.0F, 8.0F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(0, 16).addBox(-3.0F, -7.0F, -15.0F, 6.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(5.0F, -9.0F, -9.0F, 0.0F, 3.0F, 10.0F, new CubeDeformation(0.0F))
		.texOffs(0, 54).mirror().addBox(-5.0F, -5.0F, -6.0F, 2.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition cube_r1 = bb_main.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, 0.0F, 0.0F, 8.0F, 4.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -5.0F, -13.0F, -0.7854F, 0.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void setupAnim(LocomotiveEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		bb_main.render(poseStack, buffer, packedLight, packedOverlay);
	}
}