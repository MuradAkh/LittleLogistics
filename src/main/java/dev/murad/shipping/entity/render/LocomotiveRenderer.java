package dev.murad.shipping.entity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.train.AbstractTrainCar;
import dev.murad.shipping.entity.models.SteamLocomotiveModel;
import dev.murad.shipping.entity.models.SteamTugModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class LocomotiveRenderer extends TrainCarRenderer{
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ShippingMod.MOD_ID, "textures/entity/steam_locomotive.png");


    private final EntityModel model;

    public LocomotiveRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new SteamLocomotiveModel(context.bakeLayer(SteamLocomotiveModel.LAYER_LOCATION));
    }

    @Override
    public void render(AbstractTrainCar vesselEntity, float yaw, float p_225623_3_, PoseStack matrixStack, MultiBufferSource buffer, int p_225623_6_) {
        matrixStack.pushPose();
        super.render(vesselEntity, yaw, p_225623_3_, matrixStack, buffer, p_225623_6_);
        matrixStack.popPose();
    }

    @Override
    protected Model getModel(Entity vesselEntity) {
        return model;
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractTrainCar pEntity) {
        return TEXTURE;
    }

}
