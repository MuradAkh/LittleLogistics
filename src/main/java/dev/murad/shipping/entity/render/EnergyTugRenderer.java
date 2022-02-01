package dev.murad.shipping.entity.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.tug.AbstractTugEntity;
import dev.murad.shipping.entity.models.EnergyTugModel;
import dev.murad.shipping.entity.models.SteamTugModel;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;

public class EnergyTugRenderer extends AbstractTugRenderer<EnergyTugModel> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ShippingMod.MOD_ID, "textures/entity/energy_tug.png");

    public EnergyTugRenderer(EntityRendererManager p_i46179_1_) {
        super(p_i46179_1_, new EnergyTugModel());
    }

    @Override
    public void render(AbstractTugEntity boatEntity, float p_225623_2_, float p_225623_3_, MatrixStack matrixStack, IRenderTypeBuffer buffer, int p_225623_6_) {
        matrixStack.translate(0, 0.2f, 0);
        super.render(boatEntity, p_225623_2_, p_225623_3_, matrixStack, buffer, p_225623_6_);
    }

        @Override
    public ResourceLocation getTextureLocation(AbstractTugEntity p_110775_1_) {
        return TEXTURE;
    }
}
