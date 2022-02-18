package dev.murad.shipping.entity.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.murad.shipping.ShippingMod;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class SeaterBargeModel extends EntityModel<Entity> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(ShippingMod.MOD_ID, "seaterbargemodel"), "main");
	private final ModelPart bb_main;
	private final ModelPart bb_main2;

	public SeaterBargeModel(ModelPart root) {
		this.bb_main = root.getChild("bb_main");
		this.bb_main2 = root.getChild("bb_main2");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition bb_main = partdefinition.addOrReplaceChild("bb_main", CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -27.0F, -7.0F, 12.0F, 5.0F, 14.0F, new CubeDeformation(0.0F))
				.texOffs(38, 5).addBox(-8.0F, -29.0F, -7.0F, 2.0F, 4.0F, 14.0F, new CubeDeformation(0.0F))
				.texOffs(28, 43).addBox(-6.0F, -29.0F, -9.0F, 11.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(0, 41).addBox(-6.0F, -29.0F, 7.0F, 11.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 23.0F, 0.0F));

		PartDefinition cube_r1 = bb_main.addOrReplaceChild("cube_r1", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -1.5708F, 0.0F));

		PartDefinition bb_main2 = partdefinition.addOrReplaceChild("bb_main2", CubeListBuilder.create().texOffs(0, 19).addBox(-5.0F, -29.0F, -5.0F, 9.0F, 1.0F, 10.0F, new CubeDeformation(0.0F))
				.texOffs(9, 22).addBox(-5.0F, -35.0F, -5.0F, 1.0F, 6.0F, 10.0F, new CubeDeformation(0.0F))
				.texOffs(11, 23).addBox(-4.0F, -31.0F, -5.0F, 8.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(11, 24).addBox(-4.0F, -31.0F, 4.0F, 8.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
				.texOffs(0, 49).addBox(-4.0F, -32.0F, -6.0F, 8.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
				.texOffs(0, 49).addBox(-4.0F, -32.0F, 4.0F, 8.0F, 1.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		bb_main.render(poseStack, buffer, packedLight, packedOverlay);
		bb_main2.render(poseStack, buffer, packedLight, packedOverlay);
	}
}