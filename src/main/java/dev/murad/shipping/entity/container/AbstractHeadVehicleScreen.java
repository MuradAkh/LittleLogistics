package dev.murad.shipping.entity.container;

import dev.murad.shipping.entity.custom.HeadVehicle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractHeadVehicleScreen<U extends Entity & HeadVehicle, T extends AbstractHeadVehicleContainer<?, U>> extends AbstractVehicleScreen<T>{
    private Button on;
    private Button off;

    public AbstractHeadVehicleScreen(T menu, Inventory inventory, Component p_i51105_3_) {
        super(menu, inventory, p_i51105_3_);
    }

    private Tooltip tooltipOf(String translatableString) {
        return Tooltip.create(Component.translatable(translatableString));
    }

    @Override
    protected void init() {
        super.init();
        on = new Button.Builder(
                    Component.literal(">"),
                    pButton -> menu.setEngine(true))
                .pos(this.getGuiLeft() + 130, this.getGuiTop() + 25)
                .size(20, 20)
                .tooltip(tooltipOf("screen.littlelogistics.locomotive.on"))
                .build();

        off = new Button.Builder(
                    Component.literal("||"),
                    pButton -> menu.setEngine(false))
                .pos(this.getGuiLeft() + 96, this.getGuiTop() + 25)
                .size(20, 20)
                .tooltip(tooltipOf("screen.littlelogistics.locomotive.off"))
                .build();

        this.addRenderableWidget(off);
        this.addRenderableWidget(on);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);

        off.active = menu.isOn();
        on.active = !menu.isOn();

        graphics.drawString(font, Component.translatable("screen.littlelogistics.locomotive.route"), this.getGuiLeft() + 120, this.getGuiTop() + 55, 4210752, false);
        graphics.drawString(font, menu.getRouteText(), this.getGuiLeft() + 120, this.getGuiTop() + 65, 4210752, false);
    }
}
