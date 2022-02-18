package dev.murad.shipping.item.container;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.util.TugRouteNode;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Consumer;

public class StringInputScreen extends Screen {
    private static final Logger LOGGER = LogManager.getLogger(StringInputScreen.class);
    public static final ResourceLocation GUI = new ResourceLocation(ShippingMod.MOD_ID, "textures/container/tug_route_rename.png");

    private String text;
    private EditBox textFieldWidget;
    private Consumer<String> callback;

    public StringInputScreen(TugRouteNode node, int index, Consumer<String> callback) {
        super(new TranslatableComponent("screen.littlelogistics.tug_route.rename", node.getDisplayName(index)));

        this.callback = callback;
        this.text = node.hasCustomName() ? node.getName() : "";
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();

        LOGGER.info("Initializing StringInputScreen");

        int w = 156, h = 65;
        int left = (this.width - w) / 2;
        int top = (this.height - h) / 2;

        // x, y, width, height
        this.textFieldWidget = new EditBox(this.font, left + 10, top + 10, 135, 20, new TextComponent(text));
        this.textFieldWidget.setValue(text);
        this.textFieldWidget.setMaxLength(20);
        this.textFieldWidget.setResponder((s) -> text = s);
        this.addRenderableWidget(textFieldWidget);

        // add button
        this.addRenderableWidget(new Button(left + 105, top + 37, 40, 20, new TranslatableComponent("screen.littlelogistics.tug_route.confirm"), (b) -> {
            LOGGER.info("Setting to {}", text);
            callback.accept(text.isEmpty() ? null : text);
            this.minecraft.popGuiLayer();
        }));
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    public void renderBackground(PoseStack p_230446_1_) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, GUI);

        int w = 156, h = 65;
        int i = (this.width - w) / 2;
        int j = (this.height - h) / 2;
        this.blit(p_230446_1_, i, j, 0, 0, w, h);
    }
}
