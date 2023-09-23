package dev.murad.shipping.item.container;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.util.TugRouteNode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class StringInputScreen extends Screen {
    private static final Logger LOGGER = LogManager.getLogger(StringInputScreen.class);
    public static final ResourceLocation GUI = new ResourceLocation(ShippingMod.MOD_ID, "textures/container/tug_route_rename.png");

    private String text;
    private EditBox textFieldWidget;
    private Consumer<String> callback;

    public StringInputScreen(TugRouteNode node, int index, Consumer<String> callback) {
        super(Component.translatable("screen.littlelogistics.tug_route.rename", node.getDisplayName(index)));

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
        this.textFieldWidget = new EditBox(this.font, left + 10, top + 10, 135, 20, Component.literal(text));
        this.textFieldWidget.setValue(text);
        this.textFieldWidget.setMaxLength(20);
        this.textFieldWidget.setResponder((s) -> text = s);
        this.addRenderableWidget(textFieldWidget);

        // add button
        this.addRenderableWidget(Button.builder(Component.translatable("screen.littlelogistics.tug_route.confirm"), (b) -> {
            LOGGER.info("Setting to {}", text);
            callback.accept(text.isEmpty() ? null : text);
            this.minecraft.popGuiLayer();
        }).pos(left + 105, top + 37).size(40, 20).build());
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    public void renderBackground(@NotNull GuiGraphics graphics) {
        int w = 156, h = 65;
        int i = (this.width - w) / 2;
        int j = (this.height - h) / 2;
        graphics.blit(GUI, i, j, 0, 0, w, h);
    }
}
