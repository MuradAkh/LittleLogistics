package dev.murad.shipping.entity.container;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class FishingBargeScreen extends AbstractContainerScreen<FishingBargeContainer> {
    private static final ResourceLocation CONTAINER_BACKGROUND = new ResourceLocation("textures/gui/container/generic_54.png");
    private final int containerRows;

    public FishingBargeScreen(FishingBargeContainer menu, Inventory inventory, Component title) {
        super(menu, inventory, title);

        // not sure what this ever did
//        this.passEvents = false;

        this.containerRows = 3;
        this.imageHeight = 114 + this.containerRows * 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int x, int y, float partialTicks) {
        this.renderBackground(graphics);
        super.render(graphics, x, y, partialTicks);
        this.renderTooltip(graphics, x, y);
    }

    protected void renderBg(GuiGraphics graphics, float p_230450_2_, int p_230450_3_, int p_230450_4_) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        graphics.blit(CONTAINER_BACKGROUND, i, j, 0, 0, this.imageWidth, this.containerRows * 18 + 17);
        graphics.blit(CONTAINER_BACKGROUND, i, j + this.containerRows * 18 + 17, 0, 126, this.imageWidth, 96);
    }
}
