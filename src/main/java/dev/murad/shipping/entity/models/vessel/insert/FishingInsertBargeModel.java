package dev.murad.shipping.entity.models.vessel.insert;

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

public class FishingInsertBargeModel<T extends AbstractBargeEntity> extends EntityModel<T> {
    // This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
    public static final ModelLayerLocation STASHED_LOCATION = new ModelLayerLocation(new ResourceLocation(ShippingMod.MOD_ID, "fishing_insert_barge_model_stashed"), "main");
    public static final ModelLayerLocation TRANSITION_LOCATION = new ModelLayerLocation(new ResourceLocation(ShippingMod.MOD_ID, "fishing_insert_barge_model_transition"), "main");
    public static final ModelLayerLocation DEPLOYED_LOCATION = new ModelLayerLocation(new ResourceLocation(ShippingMod.MOD_ID, "fishing_insert_barge_model_deployed"), "main");
    private final ModelPart armsLeft;
    private final ModelPart armsRight;

    public FishingInsertBargeModel(ModelPart root) {
        this.armsLeft = root.getChild("arms_left");
        this.armsRight = root.getChild("arms_right");
    }

    public static LayerDefinition createBodyLayer(FishingBargeEntity.Status status) {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        float armAngle = switch (status) {
            case STASHED -> 0.0F;
            case TRANSITION -> 0.6109F;
            case DEPLOYED -> 1.5708F;
        };

        float armY = switch (status) {
            case STASHED -> -10.0F;
            case TRANSITION -> -9.8192F;
            case DEPLOYED -> -9.0F;
        };

        float armZ = switch (status) {
            case STASHED -> 0.0F;
            case TRANSITION -> 0.5736F;
            case DEPLOYED -> 1.0F;
        };

        float netOffsetZ = switch(status) {
            case STASHED, TRANSITION -> 0.0F;
            case DEPLOYED -> 1.0F;
        };

        partdefinition.addOrReplaceChild("arms_left", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-6.0F, armY, -1 + armZ, 1.0F, 9.0F, 2.0F)
                        .texOffs(0, 0).addBox(5.0F, armY, -1 + armZ, 1.0F, 9.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -3.0F, -4.0F, armAngle, 0.0F, 0.0F))
                .addOrReplaceChild("net_left", CubeListBuilder.create()
                        .texOffs(12, 11).addBox(-5.0F, -1.0F, -4.0F, 10.0F, 4.0F, 7.0F)
                        .texOffs(6, 0).addBox(-5.0F, -1.0F, -1.0F, 1.0F, 4.0F, 2.0F)
                        .texOffs(6, 0).addBox(4.0F, -1.0F, -1.0F, 1.0F, 4.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -7.0F, -netOffsetZ, -armAngle, 0.0F, 0.0F));

        partdefinition.addOrReplaceChild("arms_right", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-6.0F, armY, -1 - armZ, 1.0F, 9.0F, 2.0F)
                        .texOffs(0, 0).addBox(5.0F, armY, -1 - armZ, 1.0F, 9.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -3.0F, 4.0F, -armAngle, 0.0F, 0.0F))
                .addOrReplaceChild("net_right", CubeListBuilder.create()
                        .texOffs(12, 0).addBox(-5.0F, -1.0F, -3.0F, 10.0F, 4.0F, 7.0F)
                        .texOffs(6, 0).addBox(-5.0F, -1.0F, -1.0F, 1.0F, 4.0F, 2.0F)
                        .texOffs(6, 0).addBox(4.0F, -1.0F, -1.0F, 1.0F, 4.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -7.0F, netOffsetZ, armAngle, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        armsLeft.render(poseStack, buffer, packedLight, packedOverlay);
        armsRight.render(poseStack, buffer, packedLight, packedOverlay);
    }
}