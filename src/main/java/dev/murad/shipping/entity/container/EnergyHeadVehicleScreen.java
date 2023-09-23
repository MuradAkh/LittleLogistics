package dev.murad.shipping.entity.container;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.HeadVehicle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class EnergyHeadVehicleScreen<T extends Entity & HeadVehicle> extends AbstractHeadVehicleScreen<T, EnergyHeadVehicleContainer<T>> {
    private static final ResourceLocation GUI = new ResourceLocation(ShippingMod.MOD_ID, "textures/container/energy_locomotive.png");

    public EnergyHeadVehicleScreen(EnergyHeadVehicleContainer<T> menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);

        if (inBounds(mouseX - leftPos, mouseY - topPos, 56, 17, 68, 67)) {
            graphics.renderTooltip(
                    font,
                    List.of(Component.translatable("screen.littlelogistics.energy_tug.energy",
                            getMenu().getEnergy(),
                            getMenu().getCapacity())),
                    Optional.empty(),
                    mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTicks, int x, int y) {
        super.renderBg(graphics, partialTicks, x, y);

        int i = this.getGuiLeft();
        int j = this.getGuiTop();

        graphics.blit(GUI, i, j, 0, 0, this.getXSize(), this.getYSize());
        double r = this.menu.getEnergyCapacityRatio();
        int k = (int) (r * 50);
        graphics.blit(GUI, i + 56, j + 17 + 50 - k, 176, 50 - k, 12, k);
    }
}
