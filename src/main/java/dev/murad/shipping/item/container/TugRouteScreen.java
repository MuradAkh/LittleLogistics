package dev.murad.shipping.item.container;

import com.mojang.datafixers.util.Pair;
import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.item.TugRouteItem;
import dev.murad.shipping.util.TugRouteNode;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class TugRouteScreen extends AbstractContainerScreen<TugRouteContainer> {
    private static final Logger LOGGER = LogManager.getLogger(TugRouteScreen.class);
    public static final ResourceLocation GUI = new ResourceLocation(ShippingMod.MOD_ID, "textures/container/tug_route.png");

    private final ItemStack stack;

    public TugRouteScreen(TugRouteContainer menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 256;
        this.imageHeight = 233;

        this.stack = this.menu.getItemStack();
    }

    private int getRight() {
        return this.leftPos + imageWidth;
    }

    private int getBot() {
        return this.topPos + imageHeight;
    }

    // https://github.com/ChAoSUnItY/EkiLib/blob/9b63591608cefafce32113a68bc8fd4b71972ece/src/main/java/com/chaos/eki_lib/gui/screen/StationSelectionScreen.java
    // https://github.com/ChAoSUnItY/EkiLib/blob/9b63591608cefafce32113a68bc8fd4b71972ece/src/main/java/com/chaos/eki_lib/utils/handlers/StationHandler.java#L21
    // https://github.com/ChAoSUnItY/EkiLib/blob/9b63591608cefafce32113a68bc8fd4b71972ece/src/main/java/com/chaos/eki_lib/utils/network/PacketInitStationHandler.java
    // https://github.com/ChAoSUnItY/EkiLib/blob/9b63591608cefafce32113a68bc8fd4b71972ece/src/main/java/com/chaos/eki_lib/utils/handlers/PacketHandler.java

    private Tooltip getTooltip(Component tooltip) {
        return Tooltip.create(tooltip);
    }

    private Button buildButton(int x, int y, int width, int height, MutableComponent msg, Button.OnPress onPress, Tooltip tooltip) {
        return Button.builder(msg, onPress)
                .pos(x, y)
                .size(width, height)
                .tooltip(tooltip).build();
    }

    @Override
    protected void init() {
        super.init();

        LOGGER.info("Initializing TugRouteScreen");

        var route = new TugRouteClientHandler(this, this.minecraft, TugRouteItem.getRoute(stack), menu.isOffHand());

        this.addRenderableWidget(route.initializeWidget(TugRouteScreen.this.width, TugRouteScreen.this.height,
                topPos + 40, topPos + TugRouteScreen.this.imageHeight - 45, 20));

        this.addRenderableWidget(buildButton(getRight() - 92, getBot() - 24, 20, 20,
                Component.literal("..ꕯ").withStyle(ChatFormatting.BOLD),
                button -> {
                    Optional<Pair<Integer, TugRouteNode>> selectedOpt = route.getSelected();
                    if (selectedOpt.isPresent()) {
                        Pair<Integer, TugRouteNode> selected = selectedOpt.get();
                        this.minecraft.pushGuiLayer(new StringInputScreen(selected.getSecond(), selected.getFirst(), route::renameSelected));
                    }
                },
                getTooltip(Component.translatable("screen.littlelogistics.tug_route.rename_button"))));

        this.addRenderableWidget(buildButton(getRight() - 70, getBot() - 24, 20, 20,
                Component.literal("▲"),
                button -> route.moveSelectedUp(),
                getTooltip(Component.translatable("screen.littlelogistics.tug_route.up_button"))));

        this.addRenderableWidget(buildButton(getRight() - 47, getBot() - 24, 20, 20,
                Component.literal("▼"),
                button -> route.moveSelectedDown(),
                getTooltip(Component.translatable("screen.littlelogistics.tug_route.down_button"))));

        this.addRenderableWidget(buildButton(getRight() - 24, getBot() - 24, 20, 20,
                Component.literal("✘"),
                button -> route.deleteSelected(),
                getTooltip(Component.translatable("screen.littlelogistics.tug_route.delete_button"))));
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    /**
     * Renders the tugroute background in 9 parts
     * 1. 4 Corners
     * 2. 4 Sides
     * 3. 1 Middle
     * This assumes the GUI texture is 12x12, with 4x4 chunks representing each of the chunks above.
     */
    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTicks, int x, int y) {
//        RenderSystem.setShader(GameRenderer::getPositionTexShader);
//        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
//        RenderSystem.setShaderTexture(0, GUI);
        int left = this.getGuiLeft();
        int top = this.getGuiTop();
        int right = this.getRight();
        int bot = this.getBot();

        // topleft
        graphics.blit(GUI,
                left, top,
                0, 0,
                4, 4);
        // topright
        graphics.blit(GUI,
                right - 4, top,
                8, 0,
                4, 4);
        // botleft
        graphics.blit(GUI,
                left,bot - 4,
                0,8,
                4, 4);
        // botright
        graphics.blit(GUI,
                right - 4, bot - 4,
                8, 8,
                4, 4);

        // top
        graphics.blitRepeating(GUI,
                left + 4, top,
                getXSize() - 8, 4,
                4, 0,
                4, 4);

        // bottom
        graphics.blitRepeating(GUI,
                left + 4, bot - 4,
                getXSize() - 8, 4,
                4, 8,
                4,4);

        // left
        graphics.blitRepeating(GUI,
                left, top + 4,
                4, getYSize() - 8,
                0, 4,
                4,4);

        // right
        graphics.blitRepeating(GUI,
                right - 4, top + 4,
                4, getYSize() - 8,
                8, 4,
                4,4);

        // middle
        graphics.blitRepeating(GUI,
                left + 4, top + 4,
                getXSize() - 8, getYSize() - 8,
                4, 4,
                4,4);
    }

    // remove inventory tag
    @Override
    protected void renderLabels(GuiGraphics graphics, int pMouseX, int pMouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
    }

    public Font getFont() {
        return font;
    }
}
