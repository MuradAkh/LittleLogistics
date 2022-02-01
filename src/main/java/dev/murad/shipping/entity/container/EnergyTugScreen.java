package dev.murad.shipping.entity.container;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.murad.shipping.ShippingMod;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextComponent;
import net.minecraft.util.text.TranslationTextComponent;

// Todo: consolidate tug screen code
public class EnergyTugScreen extends ContainerScreen<EnergyTugContainer> {
    private static final ResourceLocation GUI = new ResourceLocation(ShippingMod.MOD_ID, "textures/container/energy_tug.png");

    public EnergyTugScreen(EnergyTugContainer menu, PlayerInventory playerInventory, ITextComponent p_i51105_3_) {
        super(menu, playerInventory, p_i51105_3_);
    }

    private boolean inBounds(int mouseX, int mouseY, int x1, int y1, int x2, int y2) {
        return (mouseX >= x1) && (mouseX < x2) && (mouseY >= y1) && (mouseY < y2);
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);

        if (inBounds(mouseX - leftPos, mouseY - topPos, 56, 17, 68, 67)) {
            this.renderTooltip(matrixStack,
                    new TranslationTextComponent("screen.littlelogistics.energy_tug.energy",
                            getMenu().getEnergy(),
                            getMenu().getCapacity()),
                    mouseX, mouseY);
        }
//        this.renderLabels(matrixStack, mouseX, mouseY);
    }

    @Override
    protected void renderBg(MatrixStack matrixStack, float partialTicks, int x, int y) {
        RenderSystem.color4f(1f, 1f, 1f, 1f);
        this.minecraft.getTextureManager().bind(GUI);
        int i = this.getGuiLeft();
        int j = this.getGuiTop();
        this.blit(matrixStack, i, j, 0, 0, this.getXSize(), this.getYSize());

        double r = this.menu.getEnergyCapacityRatio();
        int k = (int) (r * 50);
        this.blit(matrixStack, i + 56, j + 17 + 50 - k, 176, 50 - k, 12, k);
    }
}
