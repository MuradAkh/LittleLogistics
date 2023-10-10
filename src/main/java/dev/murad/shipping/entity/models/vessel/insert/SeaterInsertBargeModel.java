package dev.murad.shipping.entity.models.vessel.insert;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.vessel.barge.AbstractBargeEntity;
import dev.murad.shipping.entity.custom.vessel.barge.SeaterBargeEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;

public class SeaterInsertBargeModel extends EntityModel<AbstractBargeEntity> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(ShippingMod.MOD_ID, "seater_insert_barge_model"), "main");
	private final ModelPart bb_main;

	public SeaterInsertBargeModel(ModelPart root) {
		this.bb_main = root.getChild("bb_main");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		partdefinition.addOrReplaceChild("bb_main", CubeListBuilder.create()
				.texOffs(0, 19).addBox(-5.0F, -29.0F, -5.0F, 9.0F, 1.0F, 10.0F)
				.texOffs(9, 22).addBox(-5.0F, -35.0F, -5.0F, 1.0F, 6.0F, 10.0F)
				.texOffs(11, 23).addBox(-4.0F, -31.0F, -5.0F, 8.0F, 2.0F, 1.0F)
				.texOffs(11, 24).addBox(-4.0F, -31.0F, 4.0F, 8.0F, 2.0F, 1.0F)
				.texOffs(0, 49).addBox(-4.0F, -32.0F, -6.0F, 8.0F, 1.0F, 2.0F)
				.texOffs(0, 49).addBox(-4.0F, -32.0F, 4.0F, 8.0F, 1.0F, 2.0F),
					PartPose.offset(0.0F, 24.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		bb_main.render(poseStack, buffer, packedLight, packedOverlay);
	}

	@Override
	public void setupAnim(AbstractBargeEntity pEntity, float pLimbSwing, float pLimbSwingAmount, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {

	}
}