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
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
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
        this.imageWidth = 175;
        this.imageHeight = 232;

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

    @Override
    protected void init() {
        super.init();

        LOGGER.info("Initializing TugRouteScreen");

        this.addWidget(this.route.initializeWidget(TugRouteScreen.this.width, TugRouteScreen.this.height,
                topPos + 40, topPos + TugRouteScreen.this.imageHeight - 45, 20));

        // save button
        this.addButton(new Button(getRight() - 23, getBot() - 23, 20, 20,
                new StringTextComponent("x"),
                button -> route.deleteSelected()));

        this.addButton(new Button(getRight() - 46, getBot() - 23, 20, 20,
                new StringTextComponent("^"),
                button -> route.moveSelectedUp()));

        this.addButton(new Button(getRight() - 69, getBot() - 23, 20, 20,
                new StringTextComponent("v"),
                button -> route.moveSelectedDown()));

        this.addButton(new Button(getRight() - 91, getBot() - 23, 20, 20,
                new StringTextComponent("R"),
                button -> {
                    Optional<Pair<Integer, TugRouteNode>> selectedOpt = route.getSelected();
                    if (selectedOpt.isPresent()) {
                        Pair<Integer, TugRouteNode> selected = selectedOpt.get();
                        this.minecraft.pushGuiLayer(new StringInputScreen(selected.getSecond(), selected.getFirst(), this.route::renameSelected));
                    }
                }));
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
        int i = this.getGuiLeft();
        int j = this.getGuiTop();
        this.blit(matrixStack, i, j, 0, 0, this.getXSize(), this.getYSize());
    }

    // remove inventory tag
    protected void renderLabels(MatrixStack stack, int p_230451_2_, int p_230451_3_) {
        this.font.draw(stack, this.title, (float)this.titleLabelX, (float)this.titleLabelY, 4210752);
    }

    public FontRenderer getFont() {
        return font;
    }
}
