package dev.murad.shipping.entity.container;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.murad.shipping.ShippingMod;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;

public class SteamTugScreen extends AbstractTugScreen<SteamTugContainer> {
    private static final ResourceLocation GUI = new ResourceLocation(ShippingMod.MOD_ID, "textures/container/steam_tug.png");

    public SteamTugScreen(SteamTugContainer menu, Inventory inventory, Component label) {
        super(menu, inventory, label);
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    @Override
    protected void renderBg(PoseStack matrixStack, float partialTicks, int x, int y) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, GUI);
        int i = this.getGuiLeft();
        int j = this.getGuiTop();
        this.blit(matrixStack, i, j, 0, 0, this.getXSize(), this.getYSize());
        if(menu.isLit()) {
            int k = this.menu.getBurnProgress();
            this.blit(matrixStack, i + 43, j + 23 + 12 - k, 176, 12 - k, 14, k + 1);
        }
    }
}
