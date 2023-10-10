package dev.murad.shipping.entity.models.vessel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.Colorable;
import dev.murad.shipping.entity.custom.vessel.VesselEntity;
import dev.murad.shipping.entity.custom.vessel.barge.AbstractBargeEntity;
import dev.murad.shipping.entity.custom.vessel.tug.AbstractTugEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class SteamTugModel<T extends Entity & Colorable> extends EntityModel<T> {
    // This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation(ShippingMod.MOD_ID, "steam_tug_model"), "main");
    private final ModelPart bb_main;

    public SteamTugModel(ModelPart root) {
        this.bb_main = root.getChild("bb_main");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition bb_main = partdefinition.addOrReplaceChild("bb_main", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-7.0F, 13.0F, -19.0F, 14.0F, 6.0F, 24.0F)
                .texOffs(0, 50).addBox(-9.0F, 11.0F, -19.0F, 2.0F, 4.0F, 24.0F)
                .texOffs(44, 30).addBox(7.0F, 11.0F, -19.0F, 2.0F, 4.0F, 24.0F)
                .texOffs(60, 66).addBox(-7.0F, 11.0F, 5.0F, 14.0F, 4.0F, 2.0F)
                .texOffs(28, 66).addBox(-7.0F, 11.0F, -21.0F, 14.0F, 4.0F, 2.0F)
                .texOffs(53, 0).addBox(-6.0F, 5.0F, -13.0F, 12.0F, 8.0F, 14.0F)
                .texOffs(0, 30).addBox(-8.0F, 3.0F, -15.0F, 16.0F, 2.0F, 18.0F)
                .texOffs(0, 0).addBox(-2.0F, -3.0F, -5.0F, 4.0F, 6.0F, 4.0F)
                .texOffs(0, 10).addBox(-2.0F, -2.25F, -5.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.5F))
                .texOffs(28, 58).addBox(-9.0F, 12.0F, -21.0F, 18.0F, 2.0F, 6.0F, new CubeDeformation(0.25F))
                .texOffs(0, 0).addBox(-2.0F, -3.0F, -11.0F, 4.0F, 6.0F, 4.0F)
                .texOffs(0, 10).addBox(-2.0F, -2.25F, -11.0F, 4.0F, 2.0F, 4.0F, new CubeDeformation(0.5F))
                .texOffs(6, 5).addBox(-1.0F, 10.0F, 7.0F, 2.0F, 3.0F, 1.0F)
                .texOffs(2, 5).addBox(-1.0F, 10.0F, 6.0F, 2.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, -16.0F, 0.0F));

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