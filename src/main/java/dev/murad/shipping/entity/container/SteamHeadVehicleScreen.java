package dev.murad.shipping.entity.container;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.HeadVehicle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class SteamHeadVehicleScreen<T extends Entity & HeadVehicle> extends AbstractHeadVehicleScreen<T, SteamHeadVehicleContainer<T>> {
    private static final ResourceLocation GUI = ResourceLocation.fromNamespaceAndPath(ShippingMod.MOD_ID, "textures/container/steam_locomotive.png");

    public SteamHeadVehicleScreen(SteamHeadVehicleContainer menu, Inventory inventory, Component p_i51105_3_) {
        super(menu, inventory, p_i51105_3_);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);

        if (inBounds(mouseX - leftPos, mouseY - topPos, 43, 23, 57, 37)) {
            int totalSeconds = menu.getBurnTime();
            Component line = menu.isLit()
                    ? Component.translatable("screen.littlelogistics.steam.fuel_time",
                            totalSeconds / 60, String.format("%02d", totalSeconds % 60))
                    : Component.translatable("screen.littlelogistics.steam.no_fuel");
            graphics.renderTooltip(font, List.of(line), Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTicks, int x, int y) {
        int i = this.getGuiLeft();
        int j = this.getGuiTop();

        graphics.blit(GUI, i, j, 0, 0, this.getXSize(), this.getYSize());
        if(menu.isLit()) {
            int k = this.menu.getBurnProgress();
            graphics.blit(GUI, i + 43, j + 23 + 12 - k, 176, 12 - k, 14, k + 1);
        }
    }


}
