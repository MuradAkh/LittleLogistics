package dev.murad.shipping.event;

import dev.murad.shipping.block.dock.DockBlock;
import dev.murad.shipping.block.dock.DockBlockEntity;
import dev.murad.shipping.block.dock.DockRail;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class DockOverlayRenderer {

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        HitResult hitResult = mc.hitResult;
        if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) return;

        BlockPos pos = ((BlockHitResult) hitResult).getBlockPos();
        BlockState state = player.level().getBlockState(pos);

        if (!(state.getBlock() instanceof DockBlock) && !(state.getBlock() instanceof DockRail)) return;

        BlockEntity be = player.level().getBlockEntity(pos);
        if (!(be instanceof DockBlockEntity dock)) return;

        int timeoutTicks = dock.getIdleTimeoutTicks();
        DockBlockEntity.RedstoneMode mode = dock.getRedstoneMode();
        boolean occupied = dock.isOccupied();

        String timeoutStr = formatTimeout(timeoutTicks);
        String modeStr = formatRedstoneMode(mode);

        Font font = mc.font;
        int screenW = graphics.guiWidth();
        int screenH = graphics.guiHeight();

        // Position: below crosshair, offset down
        int boxX = screenW / 2;
        int startY = screenH / 2 + 16;

        // Build lines
        boolean sneaking = player.isShiftKeyDown();
        String line1 = "Timeout: " + timeoutStr + (sneaking ? "  [Scroll]" : "");
        String line2 = "Redstone: " + modeStr + "  [Right-click]";
        String line3 = occupied ? "Vehicle docked" : "Empty";
        String hint = sneaking ? "Scroll to adjust timeout" : "Sneak + Scroll to adjust timeout";

        int maxWidth = Math.max(font.width(line1),
                Math.max(font.width(line2),
                        Math.max(font.width(line3), font.width(hint))));

        int padding = 4;
        int lineHeight = 11;
        int boxWidth = maxWidth + padding * 2;
        int boxHeight = lineHeight * 4 + padding * 2;
        int left = boxX - boxWidth / 2;
        int top = startY;

        // Semi-transparent background
        graphics.fill(left, top, left + boxWidth, top + boxHeight, 0xAA000000);

        // Border
        graphics.fill(left, top, left + boxWidth, top + 1, 0xFF555555);
        graphics.fill(left, top + boxHeight - 1, left + boxWidth, top + boxHeight, 0xFF555555);
        graphics.fill(left, top, left + 1, top + boxHeight, 0xFF555555);
        graphics.fill(left + boxWidth - 1, top, left + boxWidth, top + boxHeight, 0xFF555555);

        int textX = left + padding;
        int textY = top + padding;

        graphics.drawString(font, line1, textX, textY, 0xFFFFFF);
        textY += lineHeight;
        graphics.drawString(font, line2, textX, textY, 0xCCCCCC);
        textY += lineHeight;
        graphics.drawString(font, line3, textX, textY, occupied ? 0x55FF55 : 0xAAAAAA);
        textY += lineHeight;
        graphics.drawString(font, hint, textX, textY, 0x888888);
    }

    private static String formatTimeout(int ticks) {
        double seconds = ticks / 20.0;
        if (seconds == (int) seconds) {
            return (int) seconds + "s";
        }
        return String.format("%.1fs", seconds);
    }

    private static String formatRedstoneMode(DockBlockEntity.RedstoneMode mode) {
        return switch (mode) {
            case IGNORE -> "Ignore";
            case HOLD_WHILE_POWERED -> "Hold";
            case DISABLE_WHILE_POWERED -> "Disable";
        };
    }
}
