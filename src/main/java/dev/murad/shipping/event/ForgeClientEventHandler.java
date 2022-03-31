package dev.murad.shipping.event;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import dev.murad.shipping.ShippingConfig;
import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.item.LocoRouteItem;
import dev.murad.shipping.item.TugRouteItem;
import dev.murad.shipping.setup.ModItems;
import dev.murad.shipping.util.LocoRoute;
import dev.murad.shipping.util.LocoRouteNode;
import dev.murad.shipping.util.RailHelper;
import dev.murad.shipping.util.TugRouteNode;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderBlockOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLevelLastEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

/**
 * Forge-wide event bus
 */
@Mod.EventBusSubscriber(modid = ShippingMod.MOD_ID, value = Dist.CLIENT)
public class ForgeClientEventHandler {
    public static final ResourceLocation BEAM_LOCATION = new ResourceLocation(ShippingMod.MOD_ID, "textures/entity/beacon_beam.png");
    public static final ResourceLocation EMPTY_BEAM = new ResourceLocation(ShippingMod.MOD_ID, "textures/entity/empty_beam.png");

    public static class ModRenderType extends RenderType {
        public static final RenderType LINES = create("lines", DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.LINES, 256, false, false,
                RenderType.CompositeState.builder()
                        .setShaderState(RENDERTYPE_LINES_SHADER)
                        .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.empty()))
                        .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                        .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                        .setOutputState(ITEM_ENTITY_TARGET)
                        .setWriteMaskState(COLOR_DEPTH_WRITE)
                        .setCullState(NO_CULL).createCompositeState(false));

        public ModRenderType(String pName, VertexFormat pFormat, VertexFormat.Mode pMode, int pBufferSize, boolean pAffectsCrumbling, boolean pSortOnUpload, Runnable pSetupState, Runnable pClearState) {
            super(pName, pFormat, pMode, pBufferSize, pAffectsCrumbling, pSortOnUpload, pSetupState, pClearState);
        }
    }


    @SubscribeEvent
    public static void onRenderWorldLast(RenderLevelLastEvent event) {
        Player player = Minecraft.getInstance().player;
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);


        if (stack.getItem().equals(ModItems.LOCO_ROUTE.get())) {
            MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
            PoseStack pose = event.getPoseStack();
            Vec3 cameraOff = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            Vec3 cameraOffRev = cameraOff.reverse();

            LocoRoute route = LocoRouteItem.getRoute(stack);
            var list = route.stream().map(LocoRouteNode::toBlockPos).collect(Collectors.toList());
            for (BlockPos block : list) {
                pose.pushPose();
                pose.translate(block.getX() - cameraOff.x, 1 - cameraOff.y, block.getZ() - cameraOff.z);
                BeaconRenderer.renderBeaconBeam(pose, buffer, BEAM_LOCATION, event.getPartialTick(),
                        1F, player.level.getGameTime(), player.level.getMinBuildHeight() + 1, 1024,
                        DyeColor.RED.getTextureDiffuseColors(), 0F, 0.25F);

                pose.popPose();
            }

            for (BlockPos block : list) {
                pose.pushPose();
                var shape = RailHelper.getShape(block, player.level);
                double baseY = block.getY() + (shape.getName().contains("ascending") ? 0.4 : 0);
                pose.translate(cameraOffRev.x, cameraOffRev.y, cameraOffRev.z);
                AABB a = new AABB(block.getX(), baseY, block.getZ(), block.getX() + 1, baseY + 0.2, block.getZ() + 1).deflate(0.2, 0, 0.2);
                LevelRenderer.renderLineBox(pose, buffer.getBuffer(ModRenderType.LINES), a, 1.0f, 0.3f, 0.3f, 0.5f);

                pose.popPose();

            }



            buffer.endBatch();
        }

        if (stack.getItem().equals(ModItems.TUG_ROUTE.get())){
            if(ShippingConfig.Client.DISABLE_TUG_ROUTE_BEACONS.get()){
                return;
            }
            Vec3 vector3d = Minecraft.getInstance().getEntityRenderDispatcher().camera.getPosition();
            double d0 = vector3d.x();
            double d1 = vector3d.y();
            double d2 = vector3d.z();
            MultiBufferSource.BufferSource renderTypeBuffer = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
            List<TugRouteNode> route = TugRouteItem.getRoute(stack);
            for (int i = 0, routeSize = route.size(); i < routeSize; i++) {
                TugRouteNode node = route.get(i);
                PoseStack matrixStack = event.getPoseStack();

                matrixStack.pushPose();
                matrixStack.translate(node.getX() - d0, 1 - d1, node.getZ() - d2);

                BeaconRenderer.renderBeaconBeam(matrixStack, renderTypeBuffer, BEAM_LOCATION, event.getPartialTick(),
                        1F, player.level.getGameTime(), player.level.getMinBuildHeight(), 1024,
                        DyeColor.RED.getTextureDiffuseColors(), 0.2F, 0.25F);
                matrixStack.popPose();
                matrixStack.pushPose();
                matrixStack.translate(node.getX() - d0 , player.getY() + 2 - d1, node.getZ() - d2 );
                matrixStack.scale(-0.025F, -0.025F, -0.025F);

                matrixStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());

                Matrix4f matrix4f = matrixStack.last().pose();

                Font fontRenderer = Minecraft.getInstance().font;
                String text = node.getDisplayName(i);
                float width = (-fontRenderer.width(text) / (float) 2);
                fontRenderer.drawInBatch(text, width, 0.0F, -1, true, matrix4f, renderTypeBuffer, true, 0, 15728880);
                matrixStack.popPose();

            }
            renderTypeBuffer.endBatch();
        }
    }
}
