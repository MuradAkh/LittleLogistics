package dev.murad.shipping.entity.container;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.murad.shipping.ShippingMod;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

public class SteamTugScreen extends ContainerScreen<SteamTugContainer> {
    private static final ResourceLocation GUI = new ResourceLocation(ShippingMod.MOD_ID, "textures/container/steam_tug.png");

    public SteamTugScreen(SteamTugContainer p_i51105_1_, PlayerInventory p_i51105_2_, ITextComponent p_i51105_3_) {
        super(p_i51105_1_, p_i51105_2_, p_i51105_3_);
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
//        this.renderLabels(matrixStack, mouseX, mouseY);
    }

    @Override
    protected void renderBg(MatrixStack matrixStack, float partialTicks, int x, int y) {
        RenderSystem.color4f(1f, 1f, 1f, 1f);
        this.minecraft.getTextureManager().bind(GUI);
        int i = this.getGuiLeft();
        int j = this.getGuiTop();
        this.blit(matrixStack, i, j, 0, 0, this.getXSize(), this.getYSize());
        if(menu.isLit()) {
            int k = this.menu.getBurnProgress();
            this.blit(matrixStack, i + 43, j + 23 + 12 - k, 176, 12 - k, 14, k + 1);
        }
    }
}
