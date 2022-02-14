package dev.murad.shipping.item.container;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.item.TugRouteItem;
import dev.murad.shipping.util.TugRouteNode;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

public class TugRouteScreen extends ContainerScreen<TugRouteContainer> {
    private static final Logger LOGGER = LogManager.getLogger(TugRouteScreen.class);
    public static final ResourceLocation GUI = new ResourceLocation(ShippingMod.MOD_ID, "textures/container/tug_route.png");

    private final ItemStack stack;
    private final TugRouteClientHandler route;

    public TugRouteScreen(TugRouteContainer menu, PlayerInventory inventory, ITextComponent title) {
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

    private Button.ITooltip getTooltip(ITextComponent tooltip) {
        return (button, stack, x, y) -> renderTooltip(stack, tooltip, x, y);
    }

    @Override
    protected void init() {
        super.init();

        LOGGER.info("Initializing TugRouteScreen");

        this.addWidget(this.route.initializeWidget(TugRouteScreen.this.width, TugRouteScreen.this.height,
                topPos + 40, topPos + TugRouteScreen.this.imageHeight - 45, 20));

        this.addButton(new Button(getRight() - 92, getBot() - 24, 20, 20,
                new StringTextComponent("..\uA56F").withStyle(TextFormatting.BOLD),
                button -> {
                    Optional<Pair<Integer, TugRouteNode>> selectedOpt = route.getSelected();
                    if (selectedOpt.isPresent()) {
                        Pair<Integer, TugRouteNode> selected = selectedOpt.get();
                        this.minecraft.pushGuiLayer(new StringInputScreen(selected.getSecond(), selected.getFirst(), this.route::renameSelected));
                    }
                },
                getTooltip(new TranslationTextComponent("screen.littlelogistics.tug_route.rename_button"))));

        this.addButton(new Button(getRight() - 70, getBot() - 24, 20, 20,
                new StringTextComponent("\u25B2"),
                button -> route.moveSelectedUp(),
                getTooltip(new TranslationTextComponent("screen.littlelogistics.tug_route.up_button"))));

        this.addButton(new Button(getRight() - 47, getBot() - 24, 20, 20,
                new StringTextComponent("\u25BC"),
                button -> route.moveSelectedDown(),
                getTooltip(new TranslationTextComponent("screen.littlelogistics.tug_route.down_button"))));

        this.addButton(new Button(getRight() - 24, getBot() - 24, 20, 20,
                new StringTextComponent("\u2718"),
                button -> route.deleteSelected(),
                getTooltip(new TranslationTextComponent("screen.littlelogistics.tug_route.delete_button"))));
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.renderTooltip(matrixStack, mouseX, mouseY);

        // render panel
        this.route.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    @Override
    protected void renderBg(MatrixStack matrixStack, float partialTicks, int x, int y) {
        RenderSystem.color4f(1f, 1f, 1f, 1f);
        this.minecraft.getTextureManager().bind(GUI);
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
        correctBlit(matrixStack, left + 4, top, this.getBlitOffset(),
                4 * zoom, 0,
                getXSize() - 8, 4,
                256 * zoom, 256);

        // bottom
        correctBlit(matrixStack, left + 4, bot - 4, this.getBlitOffset(),
                4 * zoom, 8,
                getXSize() - 8, 4,
                256 * zoom, 256);

        // left
        correctBlit(matrixStack, left, top + 4, this.getBlitOffset(),
                0, 4 * zoom,
                4, getYSize() - 8,
                256, 256 * zoom);

        // right
        correctBlit(matrixStack, right - 4, top + 4, this.getBlitOffset(),
                8, 4 * zoom,
                4, getYSize() - 8,
                256, 256 * zoom);

        // middle
        correctBlit(matrixStack, left + 4, top + 4, this.getBlitOffset(), 4 * zoom, 4 * zoom, getXSize() - 8, getYSize() - 8, 256 * zoom, 256 * zoom);
    }

    // This is the correct version of Minecraft's Blit, without texH and texW being flipped...
    private void correctBlit(MatrixStack stack, int x, int y, int z, float u, float v, int w, int h, int texW, int texH) {
        blit(stack, x, y, z, u, v, w, h, texH, texW);
    }

    // remove inventory tag
    protected void renderLabels(MatrixStack stack, int p_230451_2_, int p_230451_3_) {
        this.font.draw(stack, this.title, (float)this.titleLabelX, (float)this.titleLabelY, 4210752);
    }

    public FontRenderer getFont() {
        return font;
    }
}
