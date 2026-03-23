package dev.murad.shipping.block.dockingstation;

import dev.murad.shipping.network.SetDockConfigPacket;
import dev.murad.shipping.network.SetDockRedstoneModePacket;
import dev.murad.shipping.network.SetStationNamePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.function.LongFunction;

public class DockingStationScreen extends AbstractContainerScreen<DockingStationMenu> {

    private static final int BACKGROUND_COLOR = 0xFF2B2B2B;
    private static final int BORDER_COLOR      = 0xFF555555;
    private static final int HEADER_COLOR      = 0xFF3A3A3A;
    private static final int TEXT_COLOR        = 0xFFDDDDDD;
    private static final int LABEL_COLOR       = 0xFF999999;

    private static final String[] MODE_LABELS    = {"Ignore", "Hold (on)", "Off (on)"};
    private static final String[] TIMEOUT_LABELS = {"1s", "2s", "5s", "10s", "20s"};

    private EditBox nameField;
    private int currentModeOrd;
    private int currentPresetIdx;

    public DockingStationScreen(DockingStationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth  = 176;
        this.imageHeight = 136;
    }

    @Override
    protected void init() {
        super.init();

        currentModeOrd   = menu.getRedstoneModeOrdinal();
        currentPresetIdx = presetIndexFor(menu.getIdleTimeoutTicks());

        // Station name — borderless EditBox lives inside the header band.
        // Clicking it shows a cursor; Enter or closing the screen saves.
        nameField = new EditBox(this.font,
                this.leftPos + 4, this.topPos + 3,
                168, 11,
                Component.empty());
        nameField.setBordered(false);
        nameField.setMaxLength(64);
        nameField.setValue(menu.getStationName());
        nameField.setTextColor(0xDDDDDD);
        addRenderableWidget(nameField);

        // Redstone mode cycle buttons  (row at absolute y = topPos + 97)
        int modeY = this.topPos + 97;
        addRenderableWidget(Button.builder(Component.literal("<"),
                btn -> {
                    currentModeOrd = (currentModeOrd + MODE_LABELS.length - 1) % MODE_LABELS.length;
                    PacketDistributor.sendToServer(
                            new SetDockRedstoneModePacket(menu.getControllerPos(), currentModeOrd));
                })
                .pos(this.leftPos + 60, modeY).size(14, 14).build());

        addRenderableWidget(Button.builder(Component.literal(">"),
                btn -> {
                    currentModeOrd = (currentModeOrd + 1) % MODE_LABELS.length;
                    PacketDistributor.sendToServer(
                            new SetDockRedstoneModePacket(menu.getControllerPos(), currentModeOrd));
                })
                .pos(this.leftPos + 148, modeY).size(14, 14).build());

        // Idle timeout cycle buttons  (row at absolute y = topPos + 114)
        int timeoutY = this.topPos + 114;
        addRenderableWidget(Button.builder(Component.literal("<"),
                btn -> {
                    if (currentPresetIdx > 0) {
                        currentPresetIdx--;
                        PacketDistributor.sendToServer(
                                new SetDockConfigPacket(menu.getControllerPos(), -1));
                    }
                })
                .pos(this.leftPos + 60, timeoutY).size(14, 14).build());

        addRenderableWidget(Button.builder(Component.literal(">"),
                btn -> {
                    if (currentPresetIdx < TIMEOUT_LABELS.length - 1) {
                        currentPresetIdx++;
                        PacketDistributor.sendToServer(
                                new SetDockConfigPacket(menu.getControllerPos(), 1));
                    }
                })
                .pos(this.leftPos + 148, timeoutY).size(14, 14).build());
    }

    /** Sends the name packet only if the value actually changed. */
    private void saveNameIfChanged() {
        String trimmed = nameField.getValue().trim();
        if (!trimmed.isEmpty() && !trimmed.equals(menu.getStationName())) {
            PacketDistributor.sendToServer(
                    new SetStationNamePacket(menu.getControllerPos(), trimmed));
        }
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == 257 /* Enter */ && nameField.isFocused()) {
            saveNameIfChanged();
            nameField.setFocused(false);
            return true;
        }
        if (key == 256 /* Escape */ && nameField.isFocused()) {
            // Cancel edit: restore original value and unfocus
            nameField.setValue(menu.getStationName());
            nameField.setFocused(false);
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        saveNameIfChanged();
        super.onClose();
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(g, mouseX, mouseY, partialTicks);
        super.render(g, mouseX, mouseY, partialTicks);
        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics g, float partialTicks, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        int w = this.imageWidth;
        int h = this.imageHeight;

        // Main background
        g.fill(x, y, x + w, y + h, BACKGROUND_COLOR);
        // Outer border
        g.fill(x,         y,         x + w,     y + 1,     BORDER_COLOR);
        g.fill(x,         y + h - 1, x + w,     y + h,     BORDER_COLOR);
        g.fill(x,         y,         x + 1,     y + h,     BORDER_COLOR);
        g.fill(x + w - 1, y,         x + w,     y + h,     BORDER_COLOR);
        // Header band (name lives here)
        g.fill(x + 1, y + 1, x + w - 1, y + 16, HEADER_COLOR);
        // Separator below header
        g.fill(x + 4, y + 16, x + w - 4, y + 17, BORDER_COLOR);
        // Separator above config section
        g.fill(x + 4, y + 91, x + w - 4, y + 92, BORDER_COLOR);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics g, int mouseX, int mouseY) {
        // Skip default title/inventory labels — name is the live EditBox in the header.

        DockingStationMenu m = this.menu;
        int lx = 8;
        int y  = 20;

        y = drawStatIf(g, lx, y, "Items loaded:",    m.getItemsLoaded(),    v -> v + "");
        y = drawStatIf(g, lx, y, "Items unloaded:",  m.getItemsUnloaded(),  v -> v + "");
        y = drawStatIf(g, lx, y, "Fluid loaded:",    m.getFluidLoaded(),    v -> v + " mB");
        y = drawStatIf(g, lx, y, "Fluid unloaded:",  m.getFluidUnloaded(),  v -> v + " mB");
        y = drawStatIf(g, lx, y, "Energy loaded:",   m.getEnergyLoaded(),   v -> v + " FE");
        y = drawStatIf(g, lx, y, "Energy unloaded:", m.getEnergyUnloaded(), v -> v + " FE");
        y = drawStatIf(g, lx, y, "Vehicles docked:", m.getVehiclesDocked(), v -> v + "");

        if (y == 20) {
            // All stats are zero
            g.drawString(this.font, "No activity yet", lx, y, LABEL_COLOR, false);
        }

        // Dock count in the header, right-aligned
        String dockLabel = m.getDockCount() + " dock station";
        int labelX = this.imageWidth - 8 - this.font.width(dockLabel);
        g.drawString(this.font, dockLabel, labelX, 5, LABEL_COLOR, false);

        // Config labels + current values (centred between the < > buttons at x=60-162)
        g.drawString(this.font, "Redstone:", lx, 100, LABEL_COLOR, false);
        drawCycleValue(g, modeLabel(currentModeOrd), 100);

        g.drawString(this.font, "Timeout:", lx, 117, LABEL_COLOR, false);
        drawCycleValue(g, TIMEOUT_LABELS[currentPresetIdx], 117);
    }

    /**
     * Draws a stat row only if {@code value != 0}. Returns the next y position
     * (advanced by 10) if drawn, or the same y if skipped.
     */
    private int drawStatIf(GuiGraphics g, int x, int y, String key, long value,
                            LongFunction<String> formatter) {
        if (value == 0) return y;
        g.drawString(this.font, key, x, y, LABEL_COLOR, false);
        g.drawString(this.font, formatter.apply(value), x + 92, y, TEXT_COLOR, false);
        return y + 10;
    }

    /** Draws a value string centred in the space between the < and > buttons (x=74..148). */
    private void drawCycleValue(GuiGraphics g, String value, int y) {
        int availX = 74; // width of the gap: from (60+14)=74 to 148
        int valW   = this.font.width(value);
        int valX   = 74 + (availX - valW) / 2;
        g.drawString(this.font, value, valX, y, TEXT_COLOR, false);
    }

    private static String modeLabel(int ord) {
        return (ord >= 0 && ord < MODE_LABELS.length) ? MODE_LABELS[ord] : "?";
    }

    private static int presetIndexFor(int ticks) {
        int[] presets = DockingStationMenu.TIMEOUT_PRESETS;
        for (int i = 0; i < presets.length; i++) {
            if (presets[i] == ticks) return i;
        }
        return 2; // default: 5s (100 ticks)
    }
}
