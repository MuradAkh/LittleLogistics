package dev.murad.shipping.entity.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.SpringEntity;
import dev.murad.shipping.entity.models.ChainModel;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

public class DummyEntityRenderer extends EntityRenderer<Entity> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(ShippingMod.MOD_ID, "textures/entity/chain.png");

    private static ChainModel model = new ChainModel();
    public DummyEntityRenderer(EntityRendererManager p_i46179_1_) {
        super(p_i46179_1_);
    }

    public void render(SpringEntity entity, float p_225623_2_, float p_225623_3_, MatrixStack matrixStack, IRenderTypeBuffer buffer, int p_225623_6_) {

    }

    @Override
    public ResourceLocation getTextureLocation(Entity p_110775_1_) {
        return TEXTURE;
    }

}
