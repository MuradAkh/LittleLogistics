package dev.murad.shipping.entity.models.vessel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.vessel.VesselEntity;
import dev.murad.shipping.entity.custom.vessel.barge.AbstractBargeEntity;
import dev.murad.shipping.entity.custom.vessel.barge.ChestBargeEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;

public class BaseBargeModel<T extends AbstractBargeEntity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(ShippingMod.MOD_ID, "base_barge_model"), "main");
	public static final ModelLayerLocation LAYER_LOCATION_OPEN = new ModelLayerLocation(new ResourceLocation(ShippingMod.MOD_ID, "base_barge_model_open"), "main");
	private final ModelPart bb_main;

	public BaseBargeModel(ModelPart root) {
		this.bb_main = root.getChild("bb_main");
	}

	public static LayerDefinition createBodyLayer(boolean closedFront) {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		var listBuilder = CubeListBuilder.create()
				// Main
				.texOffs(0, 0).addBox(-6.0F, -27.0F, -7.0F, 12.0F, 5.0F, 14.0F)
				// Long Side
				.texOffs(0, 19).addBox(-8.0F, -27.0F, -7.0F, 2.0F, 2.0F, 14.0F)
				// Short Side
				.texOffs(19, 21).addBox(-6.0F, -27.0F, -9.0F, 12.0F, 2.0F, 2.0F)
				.texOffs(19, 21).addBox(-6.0F, -27.0F, 7.0F, 12.0F, 2.0F, 2.0F);

		if (closedFront) {
			listBuilder.texOffs(0, 19).addBox(6.0F, -27.0F, -7.0F, 2.0F, 2.0F, 14.0F);
		}

		PartDefinition bb_main = partdefinition.addOrReplaceChild("bb_main", listBuilder, PartPose.offset(0.0F, 23.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void setupAnim(AbstractBargeEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		bb_main.render(poseStack, buffer, packedLight, packedOverlay);
	}
}