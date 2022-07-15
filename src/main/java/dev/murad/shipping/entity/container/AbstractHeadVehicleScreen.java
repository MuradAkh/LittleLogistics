package dev.murad.shipping.entity.container;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.custom.HeadVehicle;
import dev.murad.shipping.util.EnrollmentHandler;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;

import java.awt.font.FontRenderContext;

public abstract class AbstractHeadVehicleScreen<U extends Entity & HeadVehicle, T extends AbstractHeadVehicleContainer<?, U>> extends AbstractVehicleScreen<T>{
    private static final ResourceLocation REGISTRATION = new ResourceLocation(ShippingMod.MOD_ID, "textures/container/vehicle_registration.png");
    private Button on;
    private Button off;
    private Button register;

    public AbstractHeadVehicleScreen(T menu, Inventory inventory, Component p_i51105_3_) {
        super(menu, inventory, p_i51105_3_);
    }

    private Button.OnTooltip getTooltip(Component tooltip) {
        return (button, stack, x, y) -> renderTooltip(stack, tooltip, x, y);
    }

    @Override
    protected void init() {
        super.init();
        on = new Button(this.getGuiLeft() + 130, this.getGuiTop() + 25, 20, 20,
                Component.literal("\u23F5"),
                pButton -> menu.setEngine(true),
                getTooltip(Component.translatable("screen.littlelogistics.locomotive.on")));

        off = new Button(this.getGuiLeft() + 96, this.getGuiTop() + 25, 20, 20,
                Component.literal("\u23F8"),
                pButton -> menu.setEngine(false),
                getTooltip(Component.translatable("screen.littlelogistics.locomotive.off")));

        register = new Button(this.getGuiLeft() + 181, this.getGuiTop() + 20, 77, 20,
                Component.translatable("screen.littlelogistics.locomotive.register"),
                pButton -> menu.enroll(),
                getTooltip(Component.translatable("screen.littlelogistics.locomotive.register")));

        this.addRenderableWidget(off);
        this.addRenderableWidget(on);
        this.addRenderableWidget(register);

    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.font.draw(matrixStack, Component.translatable("screen.littlelogistics.locomotive.route"), this.getGuiLeft() + 120, this.getGuiTop() + 55, 4210752);

        this.font.drawShadow(matrixStack, Component.translatable("screen.littlelogistics.locomotive.registration"), this.getGuiLeft() + 180, this.getGuiTop() + 5, 16777215);
        var text = this.font.split(Component.translatable("screen.littlelogistics.locomotive.register_info"), 90);
        for (int i = 0; i < text.size(); i++) {
            this.font.draw(matrixStack, text.get(i), this.getGuiLeft() + 180, this.getGuiTop() + 48 + i * 10,16777215);
        }

        if(!menu.canMove()) {
            var frozen = this.font.split(Component.translatable("screen.littlelogistics.locomotive.frozen"), 90);
            for (int i = 0; i < frozen.size(); i++) {
                this.font.draw(matrixStack, frozen.get(i), this.getGuiLeft() + 180, this.getGuiTop() + 98 + i * 10, 16777215);
            }
        }

        this.font.draw(matrixStack, menu.getRouteText(), this.getGuiLeft() + 120, this.getGuiTop() + 65, 4210752);
    }

    @Override
    protected void renderBg(PoseStack pPoseStack, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, REGISTRATION);
        int i = this.getGuiLeft() + 175;
        int j = this.getGuiTop();
        this.blit(pPoseStack, i, j, 0, 0, this.getXSize(), this.getYSize());
        off.active = menu.isOn();
        on.active = !menu.isOn();
        register.active = menu.getOwner().equals("");
        if(!register.active) {
            register.setMessage(Component.literal(menu.getOwner()));
        }
    }
}
