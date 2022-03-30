package dev.murad.shipping.entity.container;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.ColorResolver;
import net.minecraftforge.client.event.RenderTooltipEvent;

import java.awt.*;

public abstract class AbstractLocomotiveScreen<T extends AbstractLocomotiveContainer<?>> extends AbstractVehicleScreen<T>{
    private Button on;
    private Button off;
    public AbstractLocomotiveScreen(T menu, Inventory inventory, Component p_i51105_3_) {
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
    protected void renderBg(PoseStack pPoseStack, float pPartialTick, int pMouseX, int pMouseY) {
        off.active = menu.isOn();
        on.active = !menu.isOn();
        drawCenteredString(pPoseStack, Minecraft.getInstance().font, "10", this.getGuiLeft() + 130, this.getGuiTop() + 25, Color.BLACK.getRGB());
    }
}
