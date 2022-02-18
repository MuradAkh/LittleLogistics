package dev.murad.shipping.entity.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.barge.ChunkLoaderBargeEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;

public class ChunkLoaderBargeModel extends EntityModel<ChunkLoaderBargeEntity> {
    // This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(ShippingMod.MOD_ID, "chunkloaderbargemodel"), "main");
    private final ModelPart bb_main;

    public ChunkLoaderBargeModel(ModelPart root) {
        this.bb_main = root.getChild("bb_main");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition bb_main = partdefinition.addOrReplaceChild("bb_main", CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -27.0F, -7.0F, 12.0F, 5.0F, 14.0F, new CubeDeformation(0.0F))
                .texOffs(18, 23).addBox(-8.0F, -29.0F, -7.0F, 2.0F, 4.0F, 14.0F, new CubeDeformation(0.0F))
                .texOffs(36, 25).addBox(-6.0F, -29.0F, -9.0F, 12.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(0, 19).addBox(6.0F, -29.0F, -7.0F, 2.0F, 4.0F, 14.0F, new CubeDeformation(0.0F))
                .texOffs(36, 19).addBox(-6.0F, -29.0F, 7.0F, 12.0F, 4.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(0, 37).addBox(-2.0F, -33.0F, -2.0F, 4.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 23.0F, 0.0F));

        PartDefinition ring = bb_main.addOrReplaceChild("ring", CubeListBuilder.create().texOffs(38, 0).addBox(-5.0F, -29.0F, -5.0F, 10.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(36, 31).addBox(-5.0F, -29.0F, 3.0F, 10.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(10, 41).addBox(3.0F, -29.0F, -3.0F, 2.0F, 2.0F, 6.0F, new CubeDeformation(0.0F))
                .texOffs(38, 4).addBox(-5.0F, -29.0F, -3.0F, 2.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition ring2 = bb_main.addOrReplaceChild("ring2", CubeListBuilder.create().texOffs(38, 0).addBox(-5.0F, -29.0F, -5.0F, 10.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(36, 31).addBox(-5.0F, -29.0F, 3.0F, 10.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(10, 41).addBox(3.0F, -29.0F, -3.0F, 2.0F, 2.0F, 6.0F, new CubeDeformation(0.0F))
                .texOffs(38, 4).addBox(-5.0F, -29.0F, -3.0F, 2.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -3.0F, 0.0F));

        PartDefinition ring3 = bb_main.addOrReplaceChild("ring3", CubeListBuilder.create().texOffs(38, 0).addBox(-5.0F, -29.0F, -5.0F, 10.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(36, 31).addBox(-5.0F, -29.0F, 3.0F, 10.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(10, 41).addBox(3.0F, -29.0F, -3.0F, 2.0F, 2.0F, 6.0F, new CubeDeformation(0.0F))
                .texOffs(38, 4).addBox(-5.0F, -29.0F, -3.0F, 2.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -7.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(ChunkLoaderBargeEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        bb_main.render(poseStack, buffer, packedLight, packedOverlay);
    }
}
