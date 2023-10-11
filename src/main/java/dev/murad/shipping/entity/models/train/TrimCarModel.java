package dev.murad.shipping.entity.models.train;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.Colorable;
import dev.murad.shipping.entity.custom.train.AbstractTrainCarEntity;
import dev.murad.shipping.entity.custom.train.wagon.SeaterCarEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.vehicle.Minecart;


public class TrimCarModel<T extends AbstractTrainCarEntity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(ShippingMod.MOD_ID, "trim_car_model"), "main");
	private final ModelPart bb_main;

	public TrimCarModel(ModelPart root) {
		this.bb_main = root.getChild("bb_main");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition bb_main = partdefinition.addOrReplaceChild("bb_main", CubeListBuilder.create()
				// Long sides trim
				.texOffs(0, 18).addBox(-7.0F, -14.0F, -8.0F, 2.0F, 1F, 16.0F)
				.texOffs(0, 18).addBox(5.0F, -14.0F, -8.0F, 2.0F, 1F, 16.0F)

				// Short sides trim
				.texOffs(36, 18).addBox(-5.0F, -14.0F, -8.0F, 10.0F, 1F, 2.0F)
				.texOffs(36, 18).addBox(-5.0F, -14.0F, 6.0F, 10.0F, 1F, 2.0F)

				// Base
				.texOffs(0, 0).addBox(-7.0F, -4.0F, -8.0F, 14.0F, 2.0F, 16.0F),

				PartPose.offset(0.0F, 24.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		bb_main.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}