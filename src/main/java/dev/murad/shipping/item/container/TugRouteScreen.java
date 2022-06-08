package dev.murad.shipping.item.container;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.item.TugRouteItem;
import dev.murad.shipping.util.TugRouteNode;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

public class TugRouteScreen extends AbstractContainerScreen<TugRouteContainer> {
    private static final Logger LOGGER = LogManager.getLogger(TugRouteScreen.class);
    public static final ResourceLocation GUI = new ResourceLocation(ShippingMod.MOD_ID, "textures/container/tug_route.png");

    private final ItemStack stack;
    private final TugRouteClientHandler route;

    public TugRouteScreen(TugRouteContainer menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 256;
        this.imageHeight = 233;

        this.stack = this.menu.getItemStack();
        this.route = new TugRouteClientHandler(this, this.minecraft, TugRouteItem.getRoute(stack), menu.isOffHand());
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

    private Button.OnTooltip getTooltip(Component tooltip) {
        return (button, stack, x, y) -> renderTooltip(stack, tooltip, x, y);
    }

    @Override
    protected void init() {
        super.init();

        LOGGER.info("Initializing TugRouteScreen");

        this.addRenderableWidget(this.route.initializeWidget(TugRouteScreen.this.width, TugRouteScreen.this.height,
                topPos + 40, topPos + TugRouteScreen.this.imageHeight - 45, 20));

        this.addRenderableWidget(new Button(getRight() - 92, getBot() - 24, 20, 20,
                Component.literal("..\uA56F").withStyle(ChatFormatting.BOLD),
                button -> {
                    Optional<Pair<Integer, TugRouteNode>> selectedOpt = route.getSelected();
                    if (selectedOpt.isPresent()) {
                        Pair<Integer, TugRouteNode> selected = selectedOpt.get();
                        this.minecraft.pushGuiLayer(new StringInputScreen(selected.getSecond(), selected.getFirst(), this.route::renameSelected));
                    }
                },
                getTooltip(Component.translatable("screen.littlelogistics.tug_route.rename_button"))));

        this.addRenderableWidget(new Button(getRight() - 70, getBot() - 24, 20, 20,
                Component.literal("\u25B2"),
                button -> route.moveSelectedUp(),
                getTooltip(Component.translatable("screen.littlelogistics.tug_route.up_button"))));

        this.addRenderableWidget(new Button(getRight() - 47, getBot() - 24, 20, 20,
                Component.literal("\u25BC"),
                button -> route.moveSelectedDown(),
                getTooltip(Component.translatable("screen.littlelogistics.tug_route.down_button"))));

        this.addRenderableWidget(new Button(getRight() - 24, getBot() - 24, 20, 20,
                Component.literal("\u2718"),
                button -> route.deleteSelected(),
                getTooltip(Component.translatable("screen.littlelogistics.tug_route.delete_button"))));
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.renderTooltip(matrixStack, mouseX, mouseY);
    }

    @Override
    protected void renderBg(PoseStack matrixStack, float partialTicks, int x, int y) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, GUI);
        int left = this.getGuiLeft();
        int top = this.getGuiTop();
        int right = this.getRight();
        int bot = this.getBot();

        // topleft
        this.blit(matrixStack, left, top, 0, 0, 4, 4);
        // topright
        this.blit(matrixStack, getRight() - 4, top, 8, 0, 4, 4);
        // botleft
        this.blit(matrixStack, left, getBot() - 4, 0, 8, 4, 4);
        // botright
        this.blit(matrixStack, getRight() - 4, getBot() - 4, 8, 8, 4, 4);

        int zoom = 1000;
        // top
        blit(matrixStack, left + 4, top, this.getBlitOffset(),
                4 * zoom, 0,
                getXSize() - 8, 4,
                256 * zoom, 256);

        // bottom
        blit(matrixStack, left + 4, bot - 4, this.getBlitOffset(),
                4 * zoom, 8,
                getXSize() - 8, 4,
                256 * zoom, 256);

        // left
        blit(matrixStack, left, top + 4, this.getBlitOffset(),
                0, 4 * zoom,
                4, getYSize() - 8,
                256, 256 * zoom);

        // right
        blit(matrixStack, right - 4, top + 4, this.getBlitOffset(),
                8, 4 * zoom,
                4, getYSize() - 8,
                256, 256 * zoom);

        // middle
        blit(matrixStack, left + 4, top + 4, this.getBlitOffset(), 4 * zoom, 4 * zoom, getXSize() - 8, getYSize() - 8, 256 * zoom, 256 * zoom);
    }

    // remove inventory tag
    protected void renderLabels(PoseStack stack, int p_230451_2_, int p_230451_3_) {
        this.font.draw(stack, this.title, (float)this.titleLabelX, (float)this.titleLabelY, 4210752);
    }

    public Font getFont() {
        return font;
    }
}
