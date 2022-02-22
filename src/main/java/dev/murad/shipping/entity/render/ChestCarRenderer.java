package dev.murad.shipping.entity.render;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.train.TrainCar;
import dev.murad.shipping.entity.models.ChestBargeModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class ChestCarRenderer extends TrainCarRenderer{
    private static final ResourceLocation BARGE_TEXTURE =
            new ResourceLocation(ShippingMod.MOD_ID, "textures/entity/barge.png");
    private final EntityModel model;


    public ChestCarRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new ChestBargeModel(context.bakeLayer(ChestBargeModel.LAYER_LOCATION));

    }

    protected Model getModel(Entity vesselEntity) {
        return model;
    }


    @Override
    public ResourceLocation getTextureLocation(TrainCar pEntity) {
        return BARGE_TEXTURE;
    }

}
