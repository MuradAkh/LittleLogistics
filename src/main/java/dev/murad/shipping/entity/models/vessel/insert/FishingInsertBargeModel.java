package dev.murad.shipping.entity.models.vessel.insert;// Made with Blockbench 4.1.1
// Exported for Minecraft version 1.15 - 1.16 with Mojang mappings
// Paste this class into your mod and generate all required imports


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.vessel.barge.AbstractBargeEntity;
import dev.murad.shipping.entity.custom.vessel.barge.FishingBargeEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class FishingInsertBargeModel<T extends AbstractBargeEntity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation STASHED_LOCATION = new ModelLayerLocation(new ResourceLocation(ShippingMod.MOD_ID, "fishing_insert_barge_model_stashed"), "main");
	public static final ModelLayerLocation TRANSITION_LOCATION = new ModelLayerLocation(new ResourceLocation(ShippingMod.MOD_ID, "fishing_insert_barge_model_transition"), "main");
	public static final ModelLayerLocation DEPLOYED_LOCATION = new ModelLayerLocation(new ResourceLocation(ShippingMod.MOD_ID, "fishing_insert_barge_model_deployed"), "main");
	private final ModelPart bone;
	private final ModelPart bone3;

	public FishingInsertBargeModel(ModelPart root) {
		this.bone = root.getChild("bone");
		this.bone3 = root.getChild("bone3");
	}

	public static LayerDefinition createBodyLayer(FishingBargeEntity.Status status) {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		// TODO: most of this is duplicated
		if (status == FishingBargeEntity.Status.STASHED) {
			PartDefinition bone = partdefinition.addOrReplaceChild("bone", CubeListBuilder.create().texOffs(6, 0).addBox(-6.0F, -10.0F, -1.0F, 1.0F, 9.0F, 2.0F)
					.texOffs(0, 0).addBox(5.0F, -10.0F, -1.0F, 1.0F, 9.0F, 2.0F),
					PartPose.offset(0.0F, -3.0F, -4.0F));

			PartDefinition bone2 = bone.addOrReplaceChild("bone2", CubeListBuilder.create().texOffs(36, 19).addBox(-5.0F, -1.0F, -4.0F, 10.0F, 4.0F, 7.0F)
					.texOffs(38, 8).addBox(-5.0F, -1.0F, -1.0F, 1.0F, 4.0F, 2.0F),
					PartPose.offset(0.0F, -7.0F, 0.0F));

			PartDefinition cube_r1 = bone2.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(38, 8).addBox(-0.5F, -2.0F, -1.0F, 1.0F, 4.0F, 2.0F),
					PartPose.offsetAndRotation(4.5F, 1.0F, 0.0F, 0.0F, 3.1416F, 0.0F));

			PartDefinition bone3 = partdefinition.addOrReplaceChild("bone3", CubeListBuilder.create().texOffs(6, 0).addBox(-6.0F, -10.0F, -1.0F, 1.0F, 9.0F, 2.0F)
					.texOffs(0, 0).addBox(5.0F, -10.0F, -1.0F, 1.0F, 9.0F, 2.0F),
					PartPose.offset(0.0F, -3.0F, 4.0F));

			PartDefinition bone4 = bone3.addOrReplaceChild("bone4", CubeListBuilder.create().texOffs(36, 19).addBox(-5.0F, -1.0F, -3.0F, 10.0F, 4.0F, 7.0F)
					.texOffs(38, 8).addBox(-5.0F, -1.0F, -1.0F, 1.0F, 4.0F, 2.0F),
					PartPose.offset(0.0F, -7.0F, 0.0F));

			PartDefinition cube_r2 = bone4.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(38, 8).addBox(-0.5F, -2.0F, -1.0F, 1.0F, 4.0F, 2.0F),
					PartPose.offsetAndRotation(4.5F, 1.0F, 0.0F, 0.0F, -3.1416F, 0.0F));

		} else if (status == FishingBargeEntity.Status.TRANSITION) {
			PartDefinition bone = partdefinition.addOrReplaceChild("bone", CubeListBuilder.create().texOffs(6, 0).addBox(-6.0F, -9.8192F, -0.4264F, 1.0F, 9.0F, 2.0F)
					.texOffs(0, 0).addBox(5.0F, -9.8192F, -0.4264F, 1.0F, 9.0F, 2.0F),
					PartPose.offsetAndRotation(0.0F, -3.0F, -4.0F, 0.6109F, 0.0F, 0.0F));

			PartDefinition bone2 = bone.addOrReplaceChild("bone2", CubeListBuilder.create().texOffs(36, 19).addBox(-5.0F, -1.0F, -4.0F, 10.0F, 4.0F, 7.0F)
					.texOffs(38, 8).addBox(-5.0F, -1.0F, -1.0F, 1.0F, 4.0F, 2.0F),
					PartPose.offsetAndRotation(0.0F, -7.0F, 0.0F, -0.6109F, 0.0F, 0.0F));

			PartDefinition cube_r1 = bone2.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(44, 8).mirror().addBox(-0.5F, -3.0F, -1.0F, 1.0F, 4.0F, 2.0F).mirror(false),
					PartPose.offsetAndRotation(4.5F, 2.0F, 0.0F, 0.0F, 3.1416F, 0.0F));

			PartDefinition bone3 = partdefinition.addOrReplaceChild("bone3", CubeListBuilder.create().texOffs(6, 0).addBox(-6.0F, -9.8192F, -1.5736F, 1.0F, 9.0F, 2.0F)
					.texOffs(0, 0).addBox(5.0F, -9.8192F, -1.5736F, 1.0F, 9.0F, 2.0F),
					PartPose.offsetAndRotation(0.0F, -3.0F, 4.0F, -0.6109F, 0.0F, 0.0F));

			PartDefinition bone4 = bone3.addOrReplaceChild("bone4", CubeListBuilder.create().texOffs(36, 19).addBox(-5.0F, -1.0F, -3.0F, 10.0F, 4.0F, 7.0F)
					.texOffs(38, 8).addBox(-5.0F, -1.0F, -1.0F, 1.0F, 4.0F, 2.0F),
					PartPose.offsetAndRotation(0.0F, -7.0F, 0.0F, 0.6109F, 0.0F, 0.0F));

			PartDefinition cube_r2 = bone4.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(38, 8).addBox(-0.5F, -3.0F, -1.0F, 1.0F, 4.0F, 2.0F),
					PartPose.offsetAndRotation(4.5F, 2.0F, 0.0F, 0.0F, -3.1416F, 0.0F));

		} else {
			PartDefinition bone = partdefinition.addOrReplaceChild("bone", CubeListBuilder.create().texOffs(6, 0).addBox(-6.0F, -9.0F, 0.0F, 1.0F, 9.0F, 2.0F)
					.texOffs(0, 0).addBox(5.0F, -9.0F, 0.0F, 1.0F, 9.0F, 2.0F),
					PartPose.offsetAndRotation(0.0F, -3.0F, -4.0F, 1.5708F, 0.0F, 0.0F));

			PartDefinition bone2 = bone.addOrReplaceChild("bone2", CubeListBuilder.create().texOffs(36, 19).addBox(-5.0F, -1.0F, -4.0F, 10.0F, 4.0F, 7.0F)
					.texOffs(38, 8).addBox(-5.0F, -1.0F, -1.0F, 1.0F, 4.0F, 2.0F),
					PartPose.offsetAndRotation(0.0F, -7.0F, -1.0F, -1.5708F, 0.0F, 0.0F));

			PartDefinition cube_r1 = bone2.addOrReplaceChild("cube_r1", CubeListBuilder.create()
							.texOffs(44, 8).mirror().addBox(-0.5F, -3.0F, -1.0F, 1.0F, 4.0F, 2.0F).mirror(false),
					PartPose.offsetAndRotation(4.5F, 2.0F, 0.0F, 0.0F, 3.1416F, 0.0F));

			PartDefinition bone3 = partdefinition.addOrReplaceChild("bone3", CubeListBuilder.create().texOffs(6, 0).addBox(-6.0F, -9.0F, -2.0F, 1.0F, 9.0F, 2.0F)
					.texOffs(0, 0).addBox(5.0F, -9.0F, -2.0F, 1.0F, 9.0F, 2.0F),
					PartPose.offsetAndRotation(0.0F, -3.0F, 4.0F, -1.5708F, 0.0F, 0.0F));

			PartDefinition bone4 = bone3.addOrReplaceChild("bone4", CubeListBuilder.create().texOffs(36, 19).addBox(-5.0F, -1.0F, -3.0F, 10.0F, 4.0F, 7.0F)
					.texOffs(38, 8).addBox(-5.0F, -1.0F, -1.0F, 1.0F, 4.0F, 2.0F),
					PartPose.offsetAndRotation(0.0F, -7.0F, 1.0F, 1.5708F, 0.0F, 0.0F));

			PartDefinition cube_r2 = bone4.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(38, 8).addBox(-0.5F, -3.0F, -1.0F, 1.0F, 4.0F, 2.0F),
					PartPose.offsetAndRotation(4.5F, 2.0F, 0.0F, 0.0F, -3.1416F, 0.0F));

		}

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		bone.render(poseStack, buffer, packedLight, packedOverlay);
		bone3.render(poseStack, buffer, packedLight, packedOverlay);
	}
}