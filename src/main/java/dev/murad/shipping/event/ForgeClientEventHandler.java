package dev.murad.shipping.event;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import dev.murad.shipping.ShippingConfig;
import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.item.LocoRouteItem;
import dev.murad.shipping.item.TugRouteItem;
import dev.murad.shipping.network.client.EntityPosition;
import dev.murad.shipping.network.client.VehicleTrackerPacketHandler;
import dev.murad.shipping.setup.EntityItemMap;
import dev.murad.shipping.setup.ModItems;
import dev.murad.shipping.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import javax.annotation.Nullable;
import java.util.OptionalDouble;

/**
 * Forge-wide event bus
 */
@EventBusSubscriber(modid = ShippingMod.MOD_ID, value = Dist.CLIENT)
public class ForgeClientEventHandler {

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

        public static final RenderType MARKER_TRIANGLES = create(
                "marker_triangles",
                DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.TRIANGLES,
                256, false, false,
                RenderType.CompositeState.builder()
                        .setShaderState(POSITION_COLOR_SHADER)
                        .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                        .setOutputState(ITEM_ENTITY_TARGET)
                        .setWriteMaskState(COLOR_DEPTH_WRITE)
                        .setCullState(NO_CULL)
                        .createCompositeState(false));

        public ModRenderType(String pName, VertexFormat pFormat, VertexFormat.Mode pMode, int pBufferSize, boolean pAffectsCrumbling, boolean pSortOnUpload, Runnable pSetupState, Runnable pClearState) {
            super(pName, pFormat, pMode, pBufferSize, pAffectsCrumbling, pSortOnUpload, pSetupState, pClearState);
        }
    }

    @SubscribeEvent
    public static void onWorldUnload(LevelEvent.Unload event) {
        VehicleTrackerPacketHandler.flush();
    }

    /**
     * Returns whether we rendered a route here. Empty route also returns true
     */
    private static boolean renderRouteOnStack(RenderLevelStageEvent event, Player player, ItemStack stack) {

        if (stack.getItem().equals(ModItems.LOCO_ROUTE.get())) {
            if (ShippingConfig.Client.DISABLE_ROUTE_MARKERS.get()) {
                return false;
            }
            var camera = Minecraft.getInstance().getEntityRenderDispatcher().camera;
            var camPos = camera.getPosition();
            var pose = event.getPoseStack();
            var buffer = MultiBufferSource.immediate(new ByteBufferBuilder(1536));

            int index = 0;
            for (var node : LocoRouteItem.getRoute(stack)) {
                var block = node.toBlockPos();
                double wx = block.getX() + 0.5;
                double wz = block.getZ() + 0.5;
                float alpha = RouteMarkerRenderer.computeAlpha(new Vec3(wx, block.getY(), wz), camPos);
                if (alpha <= 0.0f) { index++; continue; }

                // Stem + diamond marker (yellow)
                var lineBuffer = buffer.getBuffer(ModRenderType.LINES);
                RouteMarkerRenderer.renderStem(pose, lineBuffer, camPos, wx, block.getY(), wz,
                        1.0f, 1.0f, 0.3f, alpha);
                var triBuffer = buffer.getBuffer(ModRenderType.MARKER_TRIANGLES);
                RouteMarkerRenderer.renderMarker(pose, triBuffer, camera, camPos, wx, block.getY(), wz,
                        1.0f, 1.0f, 0.3f, alpha);

                // Rail surface box (keep existing rail shape rendering)
                pose.pushPose();
                {
                    var shape = RailHelper.getRail(block, player.level())
                            .map(pos -> RailHelper.getShape(pos, player.level()))
                            .orElse(RailShape.EAST_WEST);
                    double baseY = (shape.isAscending() ? 0.1 : 0);
                    double baseX = 0;
                    double baseZ = 0;
                    var rotation = Axis.ZP.rotationDegrees(0);
                    switch (shape) {
                        case ASCENDING_EAST -> {
                            baseX = 0.2;
                            rotation = Axis.ZP.rotationDegrees(45);
                        }
                        case ASCENDING_WEST -> {
                            baseX = 0.1;
                            baseY += 0.7;
                            rotation = Axis.ZP.rotationDegrees(-45);
                        }
                        case ASCENDING_NORTH -> {
                            baseZ = 0.1;
                            baseY += 0.7;
                            rotation = Axis.XP.rotationDegrees(45);
                        }
                        case ASCENDING_SOUTH -> {
                            baseZ = 0.2;
                            rotation = Axis.XP.rotationDegrees(-45);
                        }
                    }

                    pose.translate(block.getX() + baseX - camPos.x, block.getY() + baseY - camPos.y, block.getZ() + baseZ - camPos.z);
                    pose.mulPose(rotation);

                    AABB a = new AABB(0, 0, 0, 1, 0.2, 1);
                    LevelRenderer.renderLineBox(pose, buffer.getBuffer(ModRenderType.LINES), a, 1.0f, 1.0f, 0.3f, 0.5f * alpha);
                }
                pose.popPose();

                // Label
                String label = node.hasCustomName() ? node.getName() : String.valueOf(index + 1);
                RouteMarkerRenderer.renderLabel(pose, buffer, camera, camPos, wx, block.getY(), wz, label, alpha);

                index++;
            }

            buffer.endBatch();
        } else if (stack.getItem().equals(ModItems.TUG_ROUTE.get())){
            if(ShippingConfig.Client.DISABLE_ROUTE_MARKERS.get()){
                return false;
            }

            var camera = Minecraft.getInstance().getEntityRenderDispatcher().camera;
            var camPos = camera.getPosition();
            var pose = event.getPoseStack();
            var buffer = MultiBufferSource.immediate(new ByteBufferBuilder(1536));

            TugRoute route = TugRouteItem.getRoute(stack);
            double baseY = player.level().getSeaLevel();

            // Draw connecting lines between consecutive waypoints
            var lineBuffer = buffer.getBuffer(ModRenderType.LINES);
            for (int i = 0, routeSize = route.size(); i < routeSize; i++) {
                TugRouteNode from = route.get(i);
                TugRouteNode to = route.get((i + 1) % routeSize);
                double fx = from.getX() + 0.5, fz = from.getZ() + 0.5;
                double tx = to.getX() + 0.5, tz = to.getZ() + 0.5;
                Vec3 midpoint = new Vec3((fx + tx) / 2.0, baseY, (fz + tz) / 2.0);
                float connAlpha = RouteMarkerRenderer.computeAlpha(midpoint, camPos);
                // Last-to-first closing segment is dimmer
                if (i == route.size() - 1) connAlpha *= 0.4f;
                RouteMarkerRenderer.renderConnection(pose, lineBuffer, camPos,
                        fx, baseY, fz, tx, baseY, tz,
                        1.0f, 0.6f, 0.2f, connAlpha);
            }

            // Draw markers for each waypoint
            for (int i = 0, routeSize = route.size(); i < routeSize; i++) {
                TugRouteNode node = route.get(i);
                double wx = node.getX() + 0.5;
                double wz = node.getZ() + 0.5;
                float alpha = RouteMarkerRenderer.computeAlpha(new Vec3(wx, baseY, wz), camPos);
                if (alpha <= 0.0f) continue;

                // Stem
                RouteMarkerRenderer.renderStem(pose, buffer.getBuffer(ModRenderType.LINES), camPos,
                        wx, baseY, wz, 1.0f, 0.6f, 0.2f, alpha);
                // Diamond marker
                RouteMarkerRenderer.renderMarker(pose, buffer.getBuffer(ModRenderType.MARKER_TRIANGLES), camera, camPos,
                        wx, baseY, wz, 1.0f, 0.6f, 0.2f, alpha);
                // Label
                RouteMarkerRenderer.renderLabel(pose, buffer, camera, camPos,
                        wx, baseY, wz, node.getDisplayName(i), alpha);
            }

            buffer.endBatch();
        } else {
            return false;
        }
        return true;
    }

    @SubscribeEvent
    public static void onRenderWorldLast(RenderLevelStageEvent event) {
        if(!event.getStage().equals(RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS)){
            return;
        }

        Player player = Minecraft.getInstance().player;

        ItemStack mainStack = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offStack = player.getItemInHand(InteractionHand.OFF_HAND);

        // TODO: figure out if we want to disable offstack rendering when mainstack is rendered successfully.
        renderRouteOnStack(event, player, mainStack);
        renderRouteOnStack(event, player, offStack);

        // Only render registered vehicles when conductors wrench is on the mainhand
        if (mainStack.getItem().equals(ModItems.CONDUCTORS_WRENCH.get()) && player.level().dimension().toString().equals(VehicleTrackerPacketHandler.toRenderDimension)){
            MultiBufferSource.BufferSource renderTypeBuffer = MultiBufferSource.immediate(new ByteBufferBuilder(1536));
            var camera = Minecraft.getInstance().getEntityRenderDispatcher().camera;
            Vec3 camPos = camera.getPosition();

            for(EntityPosition position : VehicleTrackerPacketHandler.toRender){
                @Nullable
                Entity entity = player.level().getEntity(position.id());

                Vec3 entityPos = entity != null ? entity.getPosition(event.getPartialTick().getGameTimeDeltaPartialTick(false)) : position.pos();
                Vec3 iconRenderPos = computeFixedDistance(entityPos, camPos, 1.0);
                Vec3 textRenderPos = computeFixedDistance(entityPos, camPos, 0.9);
                PoseStack matrixStack = event.getPoseStack();

                matrixStack.pushPose();
                {
                    matrixStack.translate(iconRenderPos.x - camPos.x, iconRenderPos.y  - camPos.y, iconRenderPos.z - camPos.z);
                    matrixStack.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
                    matrixStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));

                    Minecraft.getInstance().getItemRenderer().renderStatic(
                            new ItemStack(EntityItemMap.get(position.type())),
                            ItemDisplayContext.GROUND,
                            150,
                            OverlayTexture.NO_OVERLAY,
                            matrixStack,
                            renderTypeBuffer,
                            player.level(),
                            position.id());
                }
                matrixStack.popPose();
                matrixStack.pushPose();
                {
                    matrixStack.translate(textRenderPos.x - camPos.x, textRenderPos.y - camPos.y, textRenderPos.z - camPos.z);
                    matrixStack.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
                    matrixStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));

                    matrixStack.scale(-0.025F, -0.025F, -0.025F);

                    Font fontRenderer = Minecraft.getInstance().font;
                    String text = String.format("%.1fm", position.pos().distanceTo(player.position()));

                    fontRenderer.drawInBatch(text,
                            (-fontRenderer.width(text) / (float) 2), 0.0F,
                            -1, true,
                            matrixStack.last().pose(), renderTypeBuffer,
                            Font.DisplayMode.NORMAL,
                            0, 15728880);

                    if (entity != null && entity.hasCustomName()) {
                        var name = entity.getCustomName();
                        matrixStack.translate(0, -20, 0);
                        fontRenderer.drawInBatch(name,
                                (-fontRenderer.width(name) / (float) 2), 0.0F,
                                -1, true,
                                matrixStack.last().pose(), renderTypeBuffer,
                                Font.DisplayMode.NORMAL,
                                0, 15728880);
                    }
                }
                matrixStack.popPose();
            }

            renderTypeBuffer.endBatch();
        }
    }

    private static Vec3 computeFixedDistance(Vec3 target, Vec3 position, double scale){
        target = target.add(0, 2, 0);
        Vec3 delta = position.vectorTo(target);

        // The distance from the player camera to render the element
        var dist = Math.min(5, delta.length());
        return position.add(delta.normalize().scale(dist * scale));
    }
}
