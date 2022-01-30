package dev.murad.shipping.entity.render;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.tug.AbstractTugEntity;
import dev.murad.shipping.entity.models.SteamTugModel;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;

public class EnergyTugRenderer extends AbstractTugRenderer<SteamTugModel> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ShippingMod.MOD_ID, "textures/entity/tug.png");

    public EnergyTugRenderer(EntityRendererManager p_i46179_1_) {
        super(p_i46179_1_, new SteamTugModel());
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractTugEntity p_110775_1_) {
        return TEXTURE;
    }
}
