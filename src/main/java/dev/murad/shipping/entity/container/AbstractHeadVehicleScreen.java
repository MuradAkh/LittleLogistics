package dev.murad.shipping.entity.container;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.HeadVehicle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractHeadVehicleScreen<U extends Entity & HeadVehicle, T extends AbstractHeadVehicleContainer<?, U>> extends AbstractVehicleScreen<T>{
    private static final ResourceLocation REGISTRATION = new ResourceLocation(ShippingMod.MOD_ID, "textures/container/vehicle_registration.png");
    private Button on;
    private Button off;
    private Button register;

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
                    Component.literal("⏵"),
                    pButton -> menu.setEngine(true))
                .pos(this.getGuiLeft() + 130, this.getGuiTop() + 25)
                .size(20, 20)
                .tooltip(tooltipOf("screen.littlelogistics.locomotive.on"))
                .build();

        off = new Button.Builder(
                    Component.literal("⏸"),
                    pButton -> menu.setEngine(false))
                .pos(this.getGuiLeft() + 96, this.getGuiTop() + 25)
                .size(20, 20)
                .tooltip(tooltipOf("screen.littlelogistics.locomotive.off"))
                .build();

        register = new Button.Builder(
                    Component.translatable("screen.littlelogistics.locomotive.register"),
                    pButton -> menu.enroll())
                .pos(this.getGuiLeft() + 181, this.getGuiTop() + 20)
                .size(77, 20)
                .tooltip(tooltipOf("screen.littlelogistics.locomotive.register"))
                .build();

        this.addRenderableWidget(off);
        this.addRenderableWidget(on);
        this.addRenderableWidget(register);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);

        graphics.drawString(font, Component.translatable("screen.littlelogistics.locomotive.route"), this.getGuiLeft() + 120, this.getGuiTop() + 55, 4210752, false);
        graphics.drawString(font, Component.translatable("screen.littlelogistics.locomotive.registration"), this.getGuiLeft() + 180, this.getGuiTop() + 5, 16777215);

        var text = this.font.split(Component.translatable("screen.littlelogistics.locomotive.register_info"), 90);
        for (int i = 0; i < text.size(); i++) {
            graphics.drawString(font, text.get(i), this.getGuiLeft() + 180, this.getGuiTop() + 48 + i * 10,16777215);
        }

        if(!menu.canMove()) {
            var frozen = this.font.split(Component.translatable("screen.littlelogistics.locomotive.frozen"), 90);
            for (int i = 0; i < frozen.size(); i++) {
                graphics.drawString(font, frozen.get(i), this.getGuiLeft() + 180, this.getGuiTop() + 98 + i * 10, 16777215);
            }
        }

        graphics.drawString(font, menu.getRouteText(), this.getGuiLeft() + 120, this.getGuiTop() + 65, 4210752, false);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float pPartialTick, int pMouseX, int pMouseY) {
//        RenderSystem.setShader(GameRenderer::getPositionTexShader);
//        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
//        RenderSystem.setShaderTexture(0, REGISTRATION);
        int i = this.getGuiLeft() + 175;
        int j = this.getGuiTop();
        graphics.blit(REGISTRATION, i, j, 0, 0, this.getXSize(), this.getYSize());
        off.active = menu.isOn();
        on.active = !menu.isOn();
        register.active = menu.getOwner().equals("");
        if(!register.active) {
            register.setMessage(Component.literal(menu.getOwner()));
        }
    }
}
