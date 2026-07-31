package dev.murad.shipping.event;

import dev.murad.shipping.block.dockingstation.DockingStationBlock;
import dev.murad.shipping.block.dockingstation.DockingStationBlockEntity;
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

        if (!(state.getBlock() instanceof DockingStationBlock)) return;

        // For extension parts, delegate to the controller BE
        DockingStationBlockEntity dock = DockingStationBlock.getControllerBE(state, player.level(), pos);
        if (dock == null) return;

        DockingStationBlockEntity.RedstoneMode mode = dock.getRedstoneMode();
        boolean occupied = dock.isOccupied();

        String modeStr = formatRedstoneMode(mode);

        Font font = mc.font;
        int screenW = graphics.guiWidth();
        int screenH = graphics.guiHeight();

        int boxX = screenW / 2;
        int startY = screenH / 2 + 16;

        String line1 = occupied ? "Status: Vehicle docked" : "Status: Empty";
        String line2 = modeStr;
        String line3 = "Right-click to configure";

        int maxWidth = Math.max(font.width(line1), Math.max(font.width(line2), font.width(line3)));

        int padding = 4;
        int lineHeight = 11;
        int boxWidth  = maxWidth + padding * 2;
        int boxHeight = lineHeight * 3 + padding * 2;
        int left = boxX - boxWidth / 2;
        int top  = startY;

        graphics.fill(left, top, left + boxWidth, top + boxHeight, 0xAA000000);
        graphics.fill(left, top, left + boxWidth, top + 1, 0xFF555555);
        graphics.fill(left, top + boxHeight - 1, left + boxWidth, top + boxHeight, 0xFF555555);
        graphics.fill(left, top, left + 1, top + boxHeight, 0xFF555555);
        graphics.fill(left + boxWidth - 1, top, left + boxWidth, top + boxHeight, 0xFF555555);

        int textX = left + padding;
        int textY = top + padding;

        graphics.drawString(font, line1, textX, textY, occupied ? 0x55FF55 : 0xAAAAAA);
        textY += lineHeight;
        graphics.drawString(font, line2, textX, textY, 0xCCCCCC);
        textY += lineHeight;
        graphics.drawString(font, line3, textX, textY, 0x888888);
    }

    private static String formatRedstoneMode(DockingStationBlockEntity.RedstoneMode mode) {
        return mode.getDisplayName().getString();
    }
}
