package dev.murad.shipping.event;

import com.mojang.blaze3d.matrix.MatrixStack;
import dev.murad.shipping.ShippingConfig;
import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.item.TugRouteItem;
import dev.murad.shipping.setup.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.tileentity.BeaconTileEntityRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.item.DyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Forge-wide event bus
 */
@Mod.EventBusSubscriber(modid = ShippingMod.MOD_ID, value = Dist.CLIENT)
public class ForgeClientEventHandler {
    public static final ResourceLocation BEAM_LOCATION = new ResourceLocation(ShippingMod.MOD_ID, "textures/entity/beacon_beam.png");

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        if(ShippingConfig.disable_tug_route_beacons.get()){
            return;
        }
        PlayerEntity player = Minecraft.getInstance().player;
        ItemStack stack = player.getItemInHand(Hand.MAIN_HAND);
        if (stack.getItem().equals(ModItems.TUG_ROUTE.get())){
            Vector3d vector3d = Minecraft.getInstance().getEntityRenderDispatcher().camera.getPosition();
            double d0 = vector3d.x();
            double d1 = vector3d.y();
            double d2 = vector3d.z();
            IRenderTypeBuffer.Impl renderTypeBuffer = Minecraft.getInstance().renderBuffers().bufferSource();
            List<Vector2f> route = TugRouteItem.getRoute(stack);
            for (int i = 0, routeSize = route.size(); i < routeSize; i++) {
                Vector2f v = route.get(i);
                MatrixStack matrixStack = event.getMatrixStack();

                matrixStack.pushPose();
                matrixStack.translate(v.x - d0 - (v.x < 0 ? 1 : 0), 1 - d1, v.y - d2 - (v.y < 0 ? 1 : 0));

                BeaconTileEntityRenderer.renderBeaconBeam(matrixStack, renderTypeBuffer, BEAM_LOCATION, event.getPartialTicks(),
                        1F, player.level.getGameTime(), 0, 1024,
                        DyeColor.RED.getTextureDiffuseColors(), 0.2F, 0.25F);
                matrixStack.popPose();
                matrixStack.pushPose();
                matrixStack.translate(v.x - d0 , player.getY() + 2 - d1, v.y - d2 );
                matrixStack.scale(-0.025F, -0.025F, -0.025F);

                matrixStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());

                Matrix4f matrix4f = matrixStack.last().pose();

                float opacity = (Minecraft.getInstance()).options.getBackgroundOpacity(0.25F);
                FontRenderer fontRenderer = Minecraft.getInstance().font;
//                int alpha = (int) (opacity * 255.0F) << 24;
                String text = String.format("%s %d.", I18n.get("item.shipping.tug_route.node"), i);
                float width = (-fontRenderer.width(text) / 2);

//                fontRenderer.drawInBatch(text, width, 0.0F, 553648127, false, matrix4f, renderTypeBuffer, false, alpha, 15728880);
                fontRenderer.drawInBatch(text, width, 0.0F, -1, true, matrix4f, renderTypeBuffer, true, 0, 15728880);
                matrixStack.popPose();

            }
        }
    }

}
