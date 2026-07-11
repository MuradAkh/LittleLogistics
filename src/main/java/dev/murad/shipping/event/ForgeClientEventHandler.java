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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;

/**
 * Forge-wide event bus
 */
@EventBusSubscriber(modid = ShippingMod.MOD_ID, value = Dist.CLIENT)
public class ForgeClientEventHandler {
    private static final double TUG_ROUTE_SURFACE_Y_OFFSET = 1.015D;
    private static final double TUG_ROUTE_RAIL_OFFSET = 0.22D;
    private static final double TUG_ROUTE_ARROW_LENGTH = 0.34D;
    private static final double TUG_ROUTE_ARROW_WIDTH = 0.26D;
    private static final double TUG_ROUTE_ARROW_SPACING = 4.0D;
    private static final double TUG_ROUTE_LABEL_Y_OFFSET = 0.22D;
    private static final double TUG_ROUTE_NODE_CLIP_DISTANCE = 0.5D;
    private static final int TUG_ROUTE_PREVIEW_DEBOUNCE_TICKS = 3;
    private static final int TUG_ROUTE_PREVIEW_NODES_PER_TICK = 256;

    private record PreviewPoint(Vec3 position, boolean isNode, boolean isCorner) {
    }

    private record PreviewSegment(Vec3 from, Vec3 to, Vec3 forward, Vec3 side, double length) {
    }

    private record RailPort(Vec3 position, Vec3 side) {
    }

    private static final class PendingTugRoutePreview {
        private final Level level;
        private final TugRoute route;
        private final BlockPos target;
        private final long targetSince;
        @Nullable
        private TugRouteCompiler.PreviewPathfinder pathfinder;
        @Nullable
        private TugRouteSegment segment;
        private boolean finished;

        private PendingTugRoutePreview(Level level, TugRoute route, BlockPos target, long targetSince) {
            this.level = level;
            this.route = route.copy();
            this.target = target.immutable();
            this.targetSince = targetSince;
        }

        private boolean matches(Level level, TugRoute route, BlockPos target) {
            return this.level == level && this.route.equals(route) && this.target.equals(target);
        }
    }

    @Nullable
    private static PendingTugRoutePreview pendingTugRoutePreview;
    private static boolean renderedTugRouteTargetPreview;

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
        pendingTugRoutePreview = null;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Player player = Minecraft.getInstance().player;
        if (player == null || ShippingConfig.Client.DISABLE_ROUTE_MARKERS.get()) {
            pendingTugRoutePreview = null;
            return;
        }

        ItemStack routeStack = getHeldTugRoute(player);
        if (routeStack.isEmpty()) {
            pendingTugRoutePreview = null;
            return;
        }

        TugRoute route = TugRouteItem.getRoute(routeStack);
        Optional<BlockPos> target = TugRouteItem.getTargetedWaypoint(player.level(), player);
        if (route.isEmpty() || target.isEmpty() || route.stream().anyMatch(node -> node.isAt(target.get()))) {
            pendingTugRoutePreview = null;
            return;
        }

        BlockPos targetPos = target.get();
        if (pendingTugRoutePreview == null || !pendingTugRoutePreview.matches(player.level(), route, targetPos)) {
            pendingTugRoutePreview = new PendingTugRoutePreview(player.level(), route, targetPos, player.level().getGameTime());
            return;
        }

        advancePendingTugRoutePreview(pendingTugRoutePreview);
    }

    private static ItemStack getHeldTugRoute(Player player) {
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (mainHand.getItem().equals(ModItems.TUG_ROUTE.get())) {
            return mainHand;
        }

        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
        return offHand.getItem().equals(ModItems.TUG_ROUTE.get()) ? offHand : ItemStack.EMPTY;
    }

    private static void advancePendingTugRoutePreview(PendingTugRoutePreview preview) {
        if (preview.finished || preview.level.getGameTime() - preview.targetSince < TUG_ROUTE_PREVIEW_DEBOUNCE_TICKS) {
            return;
        }

        if (preview.pathfinder == null) {
            BlockPos start = preview.route.getLast().toBlockPos();
            double maxSegmentLength = ShippingConfig.Server.TUG_ROUTE_MAX_SEGMENT_LENGTH.get();
            if (Math.sqrt(start.distSqr(preview.target)) > maxSegmentLength
                || Math.sqrt(preview.route.getFirst().toBlockPos().distSqr(preview.target)) > maxSegmentLength) {
                preview.finished = true;
                return;
            }

            preview.pathfinder = TugRouteCompiler.createPreviewPathfinder(
                preview.level,
                start,
                preview.target,
                getOccupiedPreviewBlocks(preview.route),
                getBlockedPreviewWaypoints(preview.route, start)
            );
        }

        TugRouteCompiler.PreviewPathStatus status = preview.pathfinder.advance(TUG_ROUTE_PREVIEW_NODES_PER_TICK);
        if (status == TugRouteCompiler.PreviewPathStatus.FOUND) {
            preview.segment = preview.pathfinder.getResult().orElse(null);
            preview.finished = true;
        } else if (status == TugRouteCompiler.PreviewPathStatus.FAILED) {
            preview.finished = true;
        }
    }

    private static Set<BlockPos> getOccupiedPreviewBlocks(TugRoute route) {
        Set<BlockPos> occupied = new HashSet<>();
        int retainedSegmentCount = Math.max(0, route.getSegments().size() - 1);
        for (int segmentIndex = 0; segmentIndex < retainedSegmentCount; segmentIndex++) {
            for (TugRoutePoint point : route.getSegments().get(segmentIndex).getPoints()) {
                occupied.add(point.toBlockPos());
            }
        }
        return occupied;
    }

    private static Set<BlockPos> getBlockedPreviewWaypoints(TugRoute route, BlockPos start) {
        Set<BlockPos> blocked = getTugRouteNodePositions(route);
        blocked.remove(start);
        return blocked;
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

            renderTugRoutePreview(event, stack);
        } else {
            return false;
        }
        return true;
    }

    private static void renderTugRoutePreview(RenderLevelStageEvent event, ItemStack stack) {
        var camera = Minecraft.getInstance().getEntityRenderDispatcher().camera;
        var camPos = camera.getPosition();
        var pose = event.getPoseStack();
        var buffer = MultiBufferSource.immediate(new ByteBufferBuilder(1536));
        TugRoute route = TugRouteItem.getRoute(stack);
        List<PreviewPoint> previewPoints = flattenTugRoutePreviewPoints(route);

        double travelled = 0.0D;
        double nextArrowDistance = TUG_ROUTE_ARROW_SPACING * 0.5D;
        for (int pointIndex = 1; pointIndex < previewPoints.size(); pointIndex++) {
            PreviewPoint from = previewPoints.get(pointIndex - 1);
            PreviewPoint to = previewPoints.get(pointIndex);
            PreviewSegment segment = trimPreviewSegment(from, to);
            if (segment == null) {
                travelled += from.position().distanceTo(to.position());
                continue;
            }

            Vec3 leftFrom = segment.from().add(segment.side().scale(TUG_ROUTE_RAIL_OFFSET));
            Vec3 leftTo = segment.to().add(segment.side().scale(TUG_ROUTE_RAIL_OFFSET));
            Vec3 rightFrom = segment.from().subtract(segment.side().scale(TUG_ROUTE_RAIL_OFFSET));
            Vec3 rightTo = segment.to().subtract(segment.side().scale(TUG_ROUTE_RAIL_OFFSET));
            float railAlpha = RouteMarkerRenderer.computeAlpha(segment.from().add(segment.to()).scale(0.5D), camPos);
            if (railAlpha > 0.0f) {
                var lineBuffer = buffer.getBuffer(ModRenderType.LINES);
                RouteMarkerRenderer.renderLine(pose, lineBuffer, camPos, leftFrom, leftTo, 1.0f, 0.6f, 0.2f, railAlpha);
                RouteMarkerRenderer.renderLine(pose, lineBuffer, camPos, rightFrom, rightTo, 1.0f, 0.6f, 0.2f, railAlpha);
            }

            double segmentLength = segment.length();
            while (segmentLength > 1.0E-6D && travelled + segmentLength >= nextArrowDistance) {
                double ratio = (nextArrowDistance - travelled) / segmentLength;
                Vec3 arrowCenter = segment.from().lerp(segment.to(), ratio);
                float arrowAlpha = RouteMarkerRenderer.computeAlpha(arrowCenter, camPos);
                if (arrowAlpha > 0.0f && isArrowClearOfCorners(arrowCenter, segment.forward(), segment.side(), previewPoints)) {
                    renderTugRouteArrow(pose, buffer.getBuffer(ModRenderType.LINES), camPos, arrowCenter, segment.forward(), segment.side(), 1.0f, 0.6f, 0.2f, arrowAlpha);
                }
                nextArrowDistance += TUG_ROUTE_ARROW_SPACING;
            }

            travelled += segmentLength;
        }

        renderNonNodeCorners(pose, buffer, camPos, previewPoints, 1.0f, 0.6f, 0.2f);

        for (int i = 0, routeSize = route.size(); i < routeSize; i++) {
            TugRouteNode node = route.get(i);
            Vec3 nodePos = toWaterSurface(Vec3.atCenterOf(node.toBlockPos()));
            float alpha = RouteMarkerRenderer.computeAlpha(nodePos, camPos);
            if (alpha <= 0.0f) {
                continue;
            }
            var lineBuffer = buffer.getBuffer(ModRenderType.LINES);
            RouteMarkerRenderer.renderStem(pose, lineBuffer, camPos,
                    nodePos.x, node.toBlockPos().getY(), nodePos.z, 1.0f, 0.6f, 0.2f, alpha);
            renderTugRouteNodeBounds(pose, lineBuffer, camPos, node.toBlockPos(), alpha);
            RouteMarkerRenderer.renderLabelAtY(pose, buffer, camera, camPos,
                    nodePos.x, nodePos.y + TUG_ROUTE_LABEL_Y_OFFSET, nodePos.z,
                    node.getDisplayName(i), alpha);
        }

        if (!renderedTugRouteTargetPreview) {
            renderTargetedTugRouteWater(pose, buffer, camPos, route);
            renderedTugRouteTargetPreview = true;
        }

        buffer.endBatch();
    }

    private static void renderTargetedTugRouteWater(PoseStack pose, MultiBufferSource.BufferSource buffer, Vec3 camPos,
                                                     TugRoute route) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        TugRouteItem.getTargetedWaypoint(player.level(), player).ifPresent(pos -> {
            var lineBuffer = buffer.getBuffer(ModRenderType.LINES);
            if (route.stream().anyMatch(node -> node.isAt(pos))) {
                renderTugRouteRemovalTarget(pose, lineBuffer, camPos, pos);
                return;
            }

            boolean invalidPreview = isPendingTugRoutePreviewInvalid(player.level(), route, pos);
            float red = 1.0f;
            float green = invalidPreview ? 0.1f : 1.0f;
            float blue = invalidPreview ? 0.1f : 0.3f;
            double y = pos.getY() + TUG_ROUTE_SURFACE_Y_OFFSET;
            Vec3 northWest = new Vec3(pos.getX(), y, pos.getZ());
            Vec3 northEast = new Vec3(pos.getX() + 1.0D, y, pos.getZ());
            Vec3 southEast = new Vec3(pos.getX() + 1.0D, y, pos.getZ() + 1.0D);
            Vec3 southWest = new Vec3(pos.getX(), y, pos.getZ() + 1.0D);
            RouteMarkerRenderer.renderLine(pose, lineBuffer, camPos, northWest, northEast, red, green, blue, 1.0f);
            RouteMarkerRenderer.renderLine(pose, lineBuffer, camPos, northEast, southEast, red, green, blue, 1.0f);
            RouteMarkerRenderer.renderLine(pose, lineBuffer, camPos, southEast, southWest, red, green, blue, 1.0f);
            RouteMarkerRenderer.renderLine(pose, lineBuffer, camPos, southWest, northWest, red, green, blue, 1.0f);

            getPendingTugRouteSegment(player.level(), route, pos).ifPresent(segment ->
                renderPendingTugRouteSegment(pose, buffer, camPos, segment)
            );
        });
    }

    private static boolean isPendingTugRoutePreviewInvalid(Level level, TugRoute route, BlockPos target) {
        return pendingTugRoutePreview != null
            && pendingTugRoutePreview.matches(level, route, target)
            && pendingTugRoutePreview.finished
            && pendingTugRoutePreview.segment == null;
    }

    private static void renderTugRouteRemovalTarget(PoseStack pose, com.mojang.blaze3d.vertex.VertexConsumer lineBuffer,
                                                     Vec3 camPos, BlockPos pos) {
        double y = pos.getY() + TUG_ROUTE_SURFACE_Y_OFFSET;
        Vec3 northWest = new Vec3(pos.getX(), y, pos.getZ());
        Vec3 northEast = new Vec3(pos.getX() + 1.0D, y, pos.getZ());
        Vec3 southEast = new Vec3(pos.getX() + 1.0D, y, pos.getZ() + 1.0D);
        Vec3 southWest = new Vec3(pos.getX(), y, pos.getZ() + 1.0D);
        RouteMarkerRenderer.renderLine(pose, lineBuffer, camPos, northWest, northEast, 0.0f, 0.0f, 0.0f, 1.0f);
        RouteMarkerRenderer.renderLine(pose, lineBuffer, camPos, northEast, southEast, 0.0f, 0.0f, 0.0f, 1.0f);
        RouteMarkerRenderer.renderLine(pose, lineBuffer, camPos, southEast, southWest, 0.0f, 0.0f, 0.0f, 1.0f);
        RouteMarkerRenderer.renderLine(pose, lineBuffer, camPos, southWest, northWest, 0.0f, 0.0f, 0.0f, 1.0f);

        Vec3 crossNorthWest = new Vec3(pos.getX() + 0.25D, y, pos.getZ() + 0.25D);
        Vec3 crossNorthEast = new Vec3(pos.getX() + 0.75D, y, pos.getZ() + 0.25D);
        Vec3 crossSouthEast = new Vec3(pos.getX() + 0.75D, y, pos.getZ() + 0.75D);
        Vec3 crossSouthWest = new Vec3(pos.getX() + 0.25D, y, pos.getZ() + 0.75D);
        RouteMarkerRenderer.renderLine(pose, lineBuffer, camPos, crossNorthWest, crossSouthEast, 0.0f, 0.0f, 0.0f, 1.0f);
        RouteMarkerRenderer.renderLine(pose, lineBuffer, camPos, crossNorthEast, crossSouthWest, 0.0f, 0.0f, 0.0f, 1.0f);
    }

    private static Optional<TugRouteSegment> getPendingTugRouteSegment(Level level, TugRoute route, BlockPos target) {
        if (route.isEmpty() || route.stream().anyMatch(node -> node.isAt(target))) {
            return Optional.empty();
        }

        if (pendingTugRoutePreview == null || !pendingTugRoutePreview.matches(level, route, target)) {
            return Optional.empty();
        }
        return Optional.ofNullable(pendingTugRoutePreview.segment);
    }

    private static void renderPendingTugRouteSegment(PoseStack pose, MultiBufferSource.BufferSource buffer, Vec3 camPos,
                                                      TugRouteSegment segment) {
        List<TugRoutePoint> points = segment.getPoints();
        if (points.size() < 2) {
            return;
        }

        List<PreviewPoint> previewPoints = new ArrayList<>(points.size());
        for (int i = 0; i < points.size(); i++) {
            TugRoutePoint point = points.get(i);
            boolean isEndpoint = i == 0 || i == points.size() - 1;
            previewPoints.add(new PreviewPoint(toWaterSurface(point.toVec3Center()), isEndpoint, false));
        }
        previewPoints = markCornerPoints(previewPoints);

        for (int pointIndex = 1; pointIndex < previewPoints.size(); pointIndex++) {
            PreviewSegment previewSegment = trimPreviewSegment(previewPoints.get(pointIndex - 1), previewPoints.get(pointIndex));
            if (previewSegment == null) {
                continue;
            }

            Vec3 leftFrom = previewSegment.from().add(previewSegment.side().scale(TUG_ROUTE_RAIL_OFFSET));
            Vec3 leftTo = previewSegment.to().add(previewSegment.side().scale(TUG_ROUTE_RAIL_OFFSET));
            Vec3 rightFrom = previewSegment.from().subtract(previewSegment.side().scale(TUG_ROUTE_RAIL_OFFSET));
            Vec3 rightTo = previewSegment.to().subtract(previewSegment.side().scale(TUG_ROUTE_RAIL_OFFSET));
            float alpha = RouteMarkerRenderer.computeAlpha(previewSegment.from().add(previewSegment.to()).scale(0.5D), camPos);
            if (alpha > 0.0f) {
                var lineBuffer = buffer.getBuffer(ModRenderType.LINES);
                RouteMarkerRenderer.renderLine(pose, lineBuffer, camPos, leftFrom, leftTo, 1.0f, 1.0f, 0.3f, alpha);
                RouteMarkerRenderer.renderLine(pose, lineBuffer, camPos, rightFrom, rightTo, 1.0f, 1.0f, 0.3f, alpha);
            }
        }

        renderNonNodeCorners(pose, buffer, camPos, previewPoints, 1.0f, 1.0f, 0.3f);
    }

    private static List<PreviewPoint> flattenTugRoutePreviewPoints(TugRoute route) {
        Set<BlockPos> nodePositions = getTugRouteNodePositions(route);
        List<PreviewPoint> rawPoints = new ArrayList<>();
        if (!route.getSegments().isEmpty()) {
            for (TugRouteSegment segment : route.getSegments()) {
                List<TugRoutePoint> points = segment.getPoints();
                for (int i = 0; i < points.size(); i++) {
                    if (!rawPoints.isEmpty() && i == 0) {
                        continue;
                    }
                    TugRoutePoint point = points.get(i);
                    rawPoints.add(new PreviewPoint(
                            toWaterSurface(point.toVec3Center()),
                            nodePositions.contains(point.toBlockPos()),
                            false
                    ));
                }
            }
            return markCornerPoints(rawPoints);
        }

        for (TugRouteNode node : route) {
            rawPoints.add(new PreviewPoint(toWaterSurface(Vec3.atCenterOf(node.toBlockPos())), true, false));
        }
        if (rawPoints.size() > 1) {
            rawPoints.add(rawPoints.getFirst());
        }
        return markCornerPoints(rawPoints);
    }

    private static void renderNonNodeCorners(PoseStack pose, MultiBufferSource.BufferSource buffer, Vec3 camPos,
                                             List<PreviewPoint> previewPoints, float red, float green, float blue) {
        for (int i = 1; i < previewPoints.size() - 1; i++) {
            PreviewPoint corner = previewPoints.get(i);
            if (!corner.isCorner() || corner.isNode()) {
                continue;
            }

            RailPort incomingPort = getCornerEntryPort(previewPoints.get(i - 1), corner);
            RailPort outgoingPort = getCornerExitPort(corner, previewPoints.get(i + 1));
            if (incomingPort == null || outgoingPort == null) {
                continue;
            }

            Vec3 incomingLeft = incomingPort.position().add(incomingPort.side().scale(TUG_ROUTE_RAIL_OFFSET));
            Vec3 incomingRight = incomingPort.position().subtract(incomingPort.side().scale(TUG_ROUTE_RAIL_OFFSET));
            Vec3 outgoingLeft = outgoingPort.position().add(outgoingPort.side().scale(TUG_ROUTE_RAIL_OFFSET));
            Vec3 outgoingRight = outgoingPort.position().subtract(outgoingPort.side().scale(TUG_ROUTE_RAIL_OFFSET));
            float alpha = RouteMarkerRenderer.computeAlpha(corner.position(), camPos);
            if (alpha <= 0.0f) {
                continue;
            }

            var lineBuffer = buffer.getBuffer(ModRenderType.LINES);
            RouteMarkerRenderer.renderLine(pose, lineBuffer, camPos, incomingLeft, outgoingLeft, red, green, blue, alpha);
            RouteMarkerRenderer.renderLine(pose, lineBuffer, camPos, incomingRight, outgoingRight, red, green, blue, alpha);
        }
    }

    private static List<PreviewPoint> markCornerPoints(List<PreviewPoint> points) {
        if (points.size() < 3) {
            return points;
        }

        List<PreviewPoint> marked = new ArrayList<>(points.size());
        for (int i = 0; i < points.size(); i++) {
            PreviewPoint point = points.get(i);
            boolean isCorner = false;
            if (i > 0 && i < points.size() - 1 && !point.isNode()) {
                isCorner = isTurnPoint(points.get(i - 1).position(), point.position(), points.get(i + 1).position());
            }
            marked.add(new PreviewPoint(point.position(), point.isNode(), isCorner));
        }
        return marked;
    }

    private static boolean isTurnPoint(Vec3 previous, Vec3 current, Vec3 next) {
        Vec3 incoming = current.subtract(previous);
        Vec3 outgoing = next.subtract(current);
        Vec3 incomingHorizontal = new Vec3(incoming.x, 0.0D, incoming.z);
        Vec3 outgoingHorizontal = new Vec3(outgoing.x, 0.0D, outgoing.z);
        if (incomingHorizontal.lengthSqr() <= 1.0E-4D || outgoingHorizontal.lengthSqr() <= 1.0E-4D) {
            return false;
        }
        return incomingHorizontal.normalize().dot(outgoingHorizontal.normalize()) < 0.999D;
    }

    private static Set<BlockPos> getTugRouteNodePositions(TugRoute route) {
        Set<BlockPos> nodePositions = new HashSet<>();
        for (TugRouteNode node : route) {
            nodePositions.add(node.toBlockPos());
        }
        return nodePositions;
    }

    private static Vec3 toWaterSurface(Vec3 centerPoint) {
        return new Vec3(centerPoint.x, Math.floor(centerPoint.y) + TUG_ROUTE_SURFACE_Y_OFFSET, centerPoint.z);
    }

    @Nullable
    private static RailPort getCornerEntryPort(PreviewPoint previous, PreviewPoint corner) {
        Vec3 forward = horizontalDirection(previous.position(), corner.position());
        if (forward == null) {
            return null;
        }
        return new RailPort(
                corner.position().subtract(forward.scale(TUG_ROUTE_NODE_CLIP_DISTANCE)),
                new Vec3(-forward.z, 0.0D, forward.x)
        );
    }

    @Nullable
    private static RailPort getCornerExitPort(PreviewPoint corner, PreviewPoint next) {
        Vec3 forward = horizontalDirection(corner.position(), next.position());
        if (forward == null) {
            return null;
        }
        return new RailPort(
                corner.position().add(forward.scale(TUG_ROUTE_NODE_CLIP_DISTANCE)),
                new Vec3(-forward.z, 0.0D, forward.x)
        );
    }

    private static boolean isArrowClearOfCorners(Vec3 center, Vec3 forward, Vec3 side, List<PreviewPoint> previewPoints) {
        Vec3 tip = center.add(forward.scale(TUG_ROUTE_ARROW_LENGTH * 0.5D));
        Vec3 base = center.subtract(forward.scale(TUG_ROUTE_ARROW_LENGTH * 0.5D));
        Vec3 left = base.add(side.scale(TUG_ROUTE_ARROW_WIDTH * 0.5D));
        Vec3 right = base.subtract(side.scale(TUG_ROUTE_ARROW_WIDTH * 0.5D));
        return isOutsideCornerExclusion(center, previewPoints)
                && isOutsideCornerExclusion(tip, previewPoints)
                && isOutsideCornerExclusion(left, previewPoints)
                && isOutsideCornerExclusion(right, previewPoints);
    }

    private static boolean isOutsideCornerExclusion(Vec3 position, List<PreviewPoint> previewPoints) {
        int blockX = (int) Math.floor(position.x);
        int blockZ = (int) Math.floor(position.z);
        for (PreviewPoint point : previewPoints) {
            if (!point.isCorner() || point.isNode()) {
                continue;
            }

            int cornerX = (int) Math.floor(point.position().x);
            int cornerZ = (int) Math.floor(point.position().z);
            if (Math.abs(blockX - cornerX) + Math.abs(blockZ - cornerZ) <= 1) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    private static Vec3 horizontalDirection(Vec3 from, Vec3 to) {
        Vec3 delta = to.subtract(from);
        Vec3 horizontal = new Vec3(delta.x, 0.0D, delta.z);
        return horizontal.lengthSqr() <= 1.0E-4D ? null : horizontal.normalize();
    }

    @Nullable
    private static PreviewSegment trimPreviewSegment(PreviewPoint rawFrom, PreviewPoint rawTo) {
        Vec3 delta = rawTo.position().subtract(rawFrom.position());
        Vec3 horizontal = new Vec3(delta.x, 0.0D, delta.z);
        if (horizontal.lengthSqr() <= 1.0E-4D) {
            return null;
        }

        Vec3 forward = horizontal.normalize();
        Vec3 side = new Vec3(-forward.z, 0.0D, forward.x);
        Vec3 from = rawFrom.position();
        Vec3 to = rawTo.position();
        if (rawFrom.isNode() || rawFrom.isCorner()) {
            from = from.add(forward.scale(TUG_ROUTE_NODE_CLIP_DISTANCE));
        }
        if (rawTo.isNode() || rawTo.isCorner()) {
            to = to.subtract(forward.scale(TUG_ROUTE_NODE_CLIP_DISTANCE));
        }

        double length = from.distanceTo(to);
        if (length <= 1.0E-4D) {
            return null;
        }
        return new PreviewSegment(from, to, forward, side, length);
    }

    private static void renderTugRouteNodeBounds(PoseStack pose, com.mojang.blaze3d.vertex.VertexConsumer lineBuffer,
                                                 Vec3 camPos, BlockPos pos, float alpha) {
        double y = pos.getY() + TUG_ROUTE_SURFACE_Y_OFFSET;
        Vec3 nw = new Vec3(pos.getX(), y, pos.getZ());
        Vec3 ne = new Vec3(pos.getX() + 1.0D, y, pos.getZ());
        Vec3 se = new Vec3(pos.getX() + 1.0D, y, pos.getZ() + 1.0D);
        Vec3 sw = new Vec3(pos.getX(), y, pos.getZ() + 1.0D);
        RouteMarkerRenderer.renderLine(pose, lineBuffer, camPos, nw, ne, 1.0f, 0.6f, 0.2f, alpha);
        RouteMarkerRenderer.renderLine(pose, lineBuffer, camPos, ne, se, 1.0f, 0.6f, 0.2f, alpha);
        RouteMarkerRenderer.renderLine(pose, lineBuffer, camPos, se, sw, 1.0f, 0.6f, 0.2f, alpha);
        RouteMarkerRenderer.renderLine(pose, lineBuffer, camPos, sw, nw, 1.0f, 0.6f, 0.2f, alpha);
    }

    private static void renderTugRouteArrow(PoseStack pose, com.mojang.blaze3d.vertex.VertexConsumer lineBuffer, Vec3 camPos,
                                            Vec3 center, Vec3 forward, Vec3 side, float red, float green, float blue, float alpha) {
        Vec3 tip = center.add(forward.scale(TUG_ROUTE_ARROW_LENGTH * 0.5D));
        Vec3 base = center.subtract(forward.scale(TUG_ROUTE_ARROW_LENGTH * 0.5D));
        Vec3 left = base.add(side.scale(TUG_ROUTE_ARROW_WIDTH * 0.5D));
        Vec3 right = base.subtract(side.scale(TUG_ROUTE_ARROW_WIDTH * 0.5D));
        RouteMarkerRenderer.renderLine(pose, lineBuffer, camPos, left, tip, red, green, blue, alpha);
        RouteMarkerRenderer.renderLine(pose, lineBuffer, camPos, right, tip, red, green, blue, alpha);
    }

    @SubscribeEvent
    public static void onRenderWorldLast(RenderLevelStageEvent event) {
        if(!event.getStage().equals(RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS)){
            return;
        }

        Player player = Minecraft.getInstance().player;

        renderedTugRouteTargetPreview = false;

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
