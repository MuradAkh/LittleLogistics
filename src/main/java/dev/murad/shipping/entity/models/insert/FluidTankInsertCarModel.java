package dev.murad.shipping.entity.models.insert;// Made with Blockbench 4.1.1
// Exported for Minecraft version 1.17 with Mojang mappings
// Paste this class into your mod and generate all required imports


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.train.AbstractTrainCarEntity;
import dev.murad.shipping.entity.custom.train.wagon.FluidTankCarEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;


public class FluidTankInsertCarModel<T extends AbstractTrainCarEntity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(ShippingMod.MOD_ID, "fluid_tank_insert_car_model"), "main");
	private final ModelPart bb_main;

	public FluidTankInsertCarModel(ModelPart root) {
		this.bb_main = root.getChild("bb_main");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition bb_main = partdefinition.addOrReplaceChild("bb_main", CubeListBuilder.create().texOffs(20, 0).addBox(-6.0F, -28.0F, -5.0F, 12.0F, 1.0F, 12.0F)
		.texOffs(32, 40).addBox(-2.0F, -38.0F, -2.0F, 4.0F, 10.0F, 0.0F)
		.texOffs(32, 40).addBox(-2.0F, -38.0F, 5.0F, 4.0F, 10.0F, 0.0F)
		.texOffs(55, 30).addBox(-4.0F, -38.0F, -1.0F, 0.0F, 10.0F, 5.0F)
		.texOffs(55, 30).addBox(4.0F, -38.0F, -1.0F, 0.0F, 10.0F, 5.0F)
		.texOffs(20, 0).addBox(2.0F, -38.0F, -3.0F, 3.0F, 10.0F, 2.0F)
		.texOffs(20, 0).addBox(-5.0F, -38.0F, -3.0F, 3.0F, 10.0F, 2.0F)
		.texOffs(20, 0).addBox(-5.0F, -38.0F, 4.0F, 3.0F, 10.0F, 2.0F)
		.texOffs(20, 0).addBox(2.0F, -38.0F, 4.0F, 3.0F, 10.0F, 2.0F)
		.texOffs(20, 13).addBox(-5.0F, -39.0F, -3.0F, 10.0F, 1.0F, 2.0F), PartPose.offset(0.0F, 45.0F, 0.0F));

		PartDefinition cube_r1 = bb_main.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(24, 32).addBox(-4.0F, 2.0F, -5.0F, 1.0F, 1.0F, 7.0F)
		.texOffs(54, 24).addBox(-5.0F, 3.0F, -4.0F, 1.0F, 1.0F, 5.0F)
		.texOffs(7, 37).addBox(4.0F, 3.0F, -4.0F, 1.0F, 1.0F, 5.0F)
		.texOffs(24, 32).addBox(3.0F, 2.0F, -5.0F, 1.0F, 1.0F, 7.0F)
		.texOffs(20, 13).addBox(-5.0F, 3.0F, -6.0F, 10.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -42.0F, 0.0F, 3.1416F, 0.0F, -3.1416F));

		PartDefinition cube_r2 = bb_main.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(33, 32).addBox(-2.0F, 2.0F, -3.0F, 1.0F, 1.0F, 6.0F)
		.texOffs(33, 32).addBox(4.0F, 2.0F, -3.0F, 1.0F, 1.0F, 6.0F), PartPose.offsetAndRotation(0.0F, -42.0F, 0.0F, 0.0F, -1.5708F, 0.0F));

		PartDefinition cube_r3 = bb_main.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(40, 39).addBox(-3.0F, -4.0F, -39.0F, 6.0F, 5.0F, 0.0F)
		.texOffs(40, 39).addBox(-3.0F, -4.0F, -39.0F, 6.0F, 5.0F, 0.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -1.5708F, 0.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		bb_main.render(poseStack, buffer, packedLight, packedOverlay);
	}
}