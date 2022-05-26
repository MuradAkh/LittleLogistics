package dev.murad.shipping.entity.container;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.murad.shipping.entity.custom.HeadVehicle;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;

public abstract class AbstractHeadVehicleScreen<U extends Entity & HeadVehicle, T extends AbstractHeadVehicleContainer<?, U>> extends AbstractVehicleScreen<T>{
    private Button on;
    private Button off;
    public AbstractHeadVehicleScreen(T menu, Inventory inventory, Component p_i51105_3_) {
        super(menu, inventory, p_i51105_3_);
    }

    private Button.OnTooltip getTooltip(TranslatableComponent tooltip) {
        return (button, stack, x, y) -> renderTooltip(stack, tooltip, x, y);
    }

    @Override
    protected void init() {
        super.init();
        on = new Button(this.getGuiLeft() + 130, this.getGuiTop() + 25, 20, 20,
                new TextComponent("\u23F5"),
                pButton -> menu.setEngine(true),
                getTooltip(new TranslatableComponent("screen.littlelogistics.locomotive.on")));

        off = new Button(this.getGuiLeft() + 96, this.getGuiTop() + 25, 20, 20,
                new TextComponent("\u23F8"),
                pButton -> menu.setEngine(false),
                getTooltip(new TranslatableComponent("screen.littlelogistics.locomotive.off")));


        this.addRenderableWidget(off);
        this.addRenderableWidget(on);

    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.font.draw(matrixStack, new TranslatableComponent("screen.littlelogistics.locomotive.route"), this.getGuiLeft() + 120, this.getGuiTop() + 55, 4210752);
        this.font.draw(matrixStack, menu.getRouteText(), this.getGuiLeft() + 120, this.getGuiTop() + 65, 4210752);
    }

    @Override
    protected void renderBg(PoseStack pPoseStack, float pPartialTick, int pMouseX, int pMouseY) {
        off.active = menu.isOn();
        on.active = !menu.isOn();
    }
}
