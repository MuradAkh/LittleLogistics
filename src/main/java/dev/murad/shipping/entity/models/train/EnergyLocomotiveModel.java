package dev.murad.shipping.entity.models.train;// Made with Blockbench 4.1.1
// Exported for Minecraft version 1.17 with Mojang mappings
// Paste this class into your mod and generate all required imports


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.train.locomotive.AbstractLocomotiveEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;


public class EnergyLocomotiveModel extends EntityModel<AbstractLocomotiveEntity> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(ShippingMod.MOD_ID, "energylocomotivemodel"), "main");
	private final ModelPart bone;
	private final ModelPart bone2;
	private final ModelPart bone3;
	private final ModelPart bone4;
	private final ModelPart bb_main;

	public EnergyLocomotiveModel(ModelPart root) {
		this.bone = root.getChild("bone");
		this.bone2 = root.getChild("bone2");
		this.bone3 = root.getChild("bone3");
		this.bone4 = root.getChild("bone4");
		this.bb_main = root.getChild("bb_main");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition bone = partdefinition.addOrReplaceChild("bone", CubeListBuilder.create().texOffs(0, 0).addBox(4.0F, -4.0F, -9.0F, 2.0F, 2.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(11, 4).addBox(4.0F, -3.0F, -6.0F, 1.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(9, 11).addBox(4.0F, -3.0F, -10.0F, 1.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition bone2 = partdefinition.addOrReplaceChild("bone2", CubeListBuilder.create().texOffs(0, 0).addBox(4.0F, -4.0F, -9.0F, 2.0F, 2.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(11, 4).addBox(4.0F, -3.0F, -6.0F, 1.0F, 3.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(9, 11).addBox(4.0F, -3.0F, -10.0F, 1.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 11.0F));

		PartDefinition bone3 = partdefinition.addOrReplaceChild("bone3", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-6.0F, -4.0F, -9.0F, 2.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(11, 4).mirror().addBox(-5.0F, -3.0F, -6.0F, 1.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(9, 11).mirror().addBox(-5.0F, -3.0F, -10.0F, 1.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(0.0F, 24.0F, 11.0F));

		PartDefinition bone4 = partdefinition.addOrReplaceChild("bone4", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-6.0F, -4.0F, -9.0F, 2.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(11, 4).mirror().addBox(-5.0F, -3.0F, -6.0F, 1.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(9, 11).mirror().addBox(-5.0F, -3.0F, -10.0F, 1.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition bb_main = partdefinition.addOrReplaceChild("bb_main", CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -13.0F, -12.0F, 12.0F, 9.0F, 20.0F, new CubeDeformation(0.0F))
		.texOffs(0, 29).addBox(-6.0F, -16.0F, -9.0F, 12.0F, 3.0F, 17.0F, new CubeDeformation(0.0F))
		.texOffs(39, 30).addBox(-4.0F, -4.0F, -11.0F, 8.0F, 3.0F, 19.0F, new CubeDeformation(0.0F))
		.texOffs(0, 14).addBox(-1.0F, -17.0F, -10.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(9, 0).addBox(-1.0F, -17.0F, -10.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.25F))
		.texOffs(0, 7).addBox(4.0F, -4.0F, -3.0F, 2.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(0, 29).addBox(-1.0F, -5.0F, 8.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(0, 7).addBox(-6.0F, -4.0F, -3.0F, 2.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition cube_r1 = bb_main.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(0, 30).addBox(0.0F, 0.0F, -9.5F, 0.0F, 1.0F, 19.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.0F, -4.0F, -1.5F, 0.0F, 0.0F, -0.0436F));

		PartDefinition cube_r2 = bb_main.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(44, 0).addBox(-6.0F, 0.0F, 0.0F, 12.0F, 3.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -4.0F, -12.0F, -0.7854F, 0.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void setupAnim(AbstractLocomotiveEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		bone.render(poseStack, buffer, packedLight, packedOverlay);
		bone2.render(poseStack, buffer, packedLight, packedOverlay);
		bone3.render(poseStack, buffer, packedLight, packedOverlay);
		bone4.render(poseStack, buffer, packedLight, packedOverlay);
		bb_main.render(poseStack, buffer, packedLight, packedOverlay);
	}
}