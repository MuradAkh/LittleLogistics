package dev.murad.shipping.entity.render;

import dev.murad.shipping.entity.models.SteamTugModel;
import net.minecraft.client.renderer.entity.EntityRendererManager;

public class SteamTugRenderer extends AbstractTugRenderer<SteamTugModel> {
    public SteamTugRenderer(EntityRendererManager p_i46179_1_) {
        super(p_i46179_1_, new SteamTugModel());
    }
}
