package dev.murad.shipping.entity.models.vessel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.Colorable;
import dev.murad.shipping.entity.custom.vessel.barge.AbstractBargeEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class BaseBargeModel<T extends Entity & Colorable> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation CLOSED_LOCATION = new ModelLayerLocation(new ResourceLocation(ShippingMod.MOD_ID, "base_barge_model_closed"), "main");
	public static final ModelLayerLocation OPEN_FRONT_LOCATION = new ModelLayerLocation(new ResourceLocation(ShippingMod.MOD_ID, "base_barge_model_open_front"), "main");
	public static final ModelLayerLocation OPEN_SIDES_LOCATION = new ModelLayerLocation(new ResourceLocation(ShippingMod.MOD_ID, "base_barge_model_open_sides"), "main");

	private final ModelPart bb_main;

	public BaseBargeModel(ModelPart root) {
		this.bb_main = root.getChild("bb_main");
	}

	public static LayerDefinition createBodyLayer(boolean closedFront, boolean closedSides) {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		var listBuilder = CubeListBuilder.create()
				// Main
				.texOffs(0, 0).addBox(-6.0F, -4F, -7.0F, 12.0F, 5.0F, 14.0F)
				// Back Side
				.texOffs(0, 19).addBox(-8.0F, -4F, -7.0F, 2.0F, 2.0F, 14.0F);

		if (closedFront) {
			// Front Side
			listBuilder.texOffs(0, 19).addBox(6.0F, -4F, -7.0F, 2.0F, 2.0F, 14.0F);
		}

		if (closedSides) {
			// Short Sides
			listBuilder.texOffs(19, 21).addBox(-6.0F, -4F, -9.0F, 12.0F, 2.0F, 2.0F)
					.texOffs(19, 21).addBox(-6.0F, -4F, 7.0F, 12.0F, 2.0F, 2.0F);
		}

		PartDefinition bb_main = partdefinition.addOrReplaceChild("bb_main", listBuilder, PartPose.ZERO);

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		bb_main.render(poseStack, buffer, packedLight, packedOverlay);
	}
}