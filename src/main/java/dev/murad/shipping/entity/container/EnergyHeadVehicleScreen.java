package dev.murad.shipping.entity.container;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.HeadVehicle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;

public class EnergyHeadVehicleScreen<T extends Entity & HeadVehicle> extends AbstractHeadVehicleScreen<T, EnergyHeadVehicleContainer<T>> {
    private static final ResourceLocation GUI = new ResourceLocation(ShippingMod.MOD_ID, "textures/container/energy_locomotive.png");

    public EnergyHeadVehicleScreen(EnergyHeadVehicleContainer menu, Inventory inventory, Component p_i51105_3_) {
        super(menu, inventory, p_i51105_3_);
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        super.render(matrixStack, mouseX, mouseY, partialTicks);

        if (inBounds(mouseX - leftPos, mouseY - topPos, 56, 17, 68, 67)) {
            this.renderTooltip(matrixStack,
                    Component.translatable("screen.littlelogistics.energy_tug.energy",
                            getMenu().getEnergy(),
                            getMenu().getCapacity()),
                    mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(PoseStack matrixStack, float partialTicks, int x, int y) {
        super.renderBg(matrixStack, partialTicks, x, y);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, GUI);
        int i = this.getGuiLeft();
        int j = this.getGuiTop();
        this.blit(matrixStack, i, j, 0, 0, this.getXSize(), this.getYSize());

        double r = this.menu.getEnergyCapacityRatio();
        int k = (int) (r * 50);
        this.blit(matrixStack, i + 56, j + 17 + 50 - k, 176, 50 - k, 12, k);
    }
}
