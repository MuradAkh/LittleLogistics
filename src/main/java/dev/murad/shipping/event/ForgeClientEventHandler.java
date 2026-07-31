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
import dev.murad.shipping.network.client.TugRouteTrackerData;
import dev.murad.shipping.network.client.LocoRouteTrackerData;
import dev.murad.shipping.network.client.VehicleTrackerPacketHandler;
import dev.murad.shipping.setup.EntityItemMap;
import dev.murad.shipping.setup.ModItems;
import dev.murad.shipping.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.BlockHitResult;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    private static final int MAX_TRACKED_TUG_ROUTE_VERTICES_PER_FRAME = 16_000;
    private static final int LOCO_ROUTE_PREVIEW_DEBOUNCE_TICKS = 3;
    private static final int LOCO_ROUTE_PREVIEW_NODES_PER_TICK = 256;
    private static final double LOCO_ROUTE_HEIGHT = 0.18D;
    private static final double LOCO_ROUTE_CORNER_CLIP_DISTANCE = 0.20D;

    private record PreviewPoint(Vec3 position, boolean isNode, boolean isCorner) {
    }

    private record PreviewSegment(Vec3 from, Vec3 to, Vec3 forward, Vec3 side, double length) {
    }

    private record RailPort(Vec3 position, Vec3 side) {
    }

    private record RouteColour(float red, float green, float blue) {
    }

    private record CompositeStrokeKey(Vec3 first, Vec3 second, boolean doubleLine) {
        private static CompositeStrokeKey of(Vec3 from, Vec3 to, boolean doubleLine) {
            return comparePositions(from, to) <= 0
                ? new CompositeStrokeKey(from, to, doubleLine)
                : new CompositeStrokeKey(to, from, doubleLine);
        }
    }

    private static final class CompositeStrokeMember {
        private final RouteColour colour;
        private boolean arrowForward;
        private boolean arrowReverse;

        private CompositeStrokeMember(RouteColour colour) {
            this.colour = colour;
        }
    }

    private static final class CompositeStroke {
        private final CompositeStrokeKey key;
        private final Map<Integer, CompositeStrokeMember> members = new HashMap<>();

        private CompositeStroke(CompositeStrokeKey key) {
            this.key = key;
        }
    }

    private record LocoRenderPoint(Vec3 position, boolean isWaypoint) {
    }

    private enum LocoPreviewMode {
        APPEND,
        COMPLETE,
        INSERT
    }

    private static final class PendingTugRoutePreview {
        private final Level level;
        private final TugRoute route;
        private final BlockPos target;
        private final long targetSince;
        @Nullable
        private TugRouteCompiler.PreviewPathfinder pathfinder;
        @Nullable
        private TugRouteCompiler.RouteCompileSession compileSession;
        @Nullable
        private TugRouteCompiler.PreviewPathfinder returnPathfinder;
        @Nullable
        private TugRouteSegment segment;
        @Nullable
        private TugRouteSegment returnSegment;
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

    private static final class PendingLocoRoutePreview {
        private final Level level;
        private final LocoRoute route;
        private final BlockPos target;
        private final long targetSince;
        private final LocoPreviewMode mode;
        private final int segmentIndex;
        @Nullable private LocoRouteCompiler.PreviewPathfinder pathfinder;
        private final List<LocoReplacementPreview> replacements = new ArrayList<>();
        @Nullable private LocoRouteSegment before;
        @Nullable private LocoRouteSegment after;
        private boolean initialized;
        private boolean finished;

        private PendingLocoRoutePreview(Level level, LocoRoute route, BlockPos target, long targetSince,
                                        LocoPreviewMode mode, int segmentIndex) {
            this.level = level;
            this.route = route.copy();
            this.target = target.immutable();
            this.targetSince = targetSince;
            this.mode = mode;
            this.segmentIndex = segmentIndex;
        }

        private boolean matches(Level level, LocoRoute route, BlockPos target, LocoPreviewMode mode, int segmentIndex) {
            return this.level == level && this.route.equals(route) && this.target.equals(target)
                && this.mode == mode && this.segmentIndex == segmentIndex;
        }
    }

    private static final class LocoReplacementPreview {
        private final Direction midpoint;
        private final LocoRouteCompiler.PreviewPathfinder before;
        @Nullable private LocoRouteCompiler.PreviewPathfinder after;

        private LocoReplacementPreview(Direction midpoint, LocoRouteCompiler.PreviewPathfinder before) {
            this.midpoint = midpoint;
            this.before = before;
        }

        private boolean isTerminal() {
            return before.getStatus() == LocoRouteCompiler.PreviewStatus.FAILED
                || after != null && after.getStatus() != LocoRouteCompiler.PreviewStatus.SEARCHING;
        }
    }

    @Nullable
    private static PendingTugRoutePreview pendingTugRoutePreview;
    @Nullable
    private static PendingLocoRoutePreview pendingLocoRoutePreview;
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
        pendingLocoRoutePreview = null;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Player player = Minecraft.getInstance().player;
        if (player == null || ShippingConfig.Client.DISABLE_ROUTE_MARKERS.get()) {
            pendingTugRoutePreview = null;
            pendingLocoRoutePreview = null;
            return;
        }

        tickTugRoutePreview(player);
        tickLocoRoutePreview(player);
    }

    private static void tickTugRoutePreview(Player player) {
        ItemStack routeStack = getHeldTugRoute(player);
        if (routeStack.isEmpty()) {
            pendingTugRoutePreview = null;
            return;
        }

        TugRoute route = TugRouteItem.getRoute(routeStack);
        Optional<BlockPos> target = TugRouteItem.getTargetedWaypoint(player.level(), player);
        boolean completing = target.isPresent() && isCompletionTarget(route, target.get());
        if (!route.isInProgress() || route.isEmpty() || target.isEmpty()
            || (route.stream().anyMatch(node -> node.isAt(target.get())) && !completing)) {
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

    private static void tickLocoRoutePreview(Player player) {
        ItemStack routeStack = getHeldLocoRoute(player);
        if (routeStack.isEmpty()) {
            pendingLocoRoutePreview = null;
            return;
        }
        Optional<BlockPos> target = getReachableLocoTarget(player);
        if (target.isEmpty()) {
            pendingLocoRoutePreview = null;
            return;
        }

        LocoRoute route = LocoRouteItem.getRoute(routeStack);
        int nodeIndex = findLocoNodeIndex(route, target.get());
        LocoPreviewMode mode;
        int segmentIndex;
        if (route.isInserting()) {
            if (nodeIndex >= 0) {
                pendingLocoRoutePreview = null;
                return;
            }
            mode = LocoPreviewMode.INSERT;
            segmentIndex = route.getNextInsertionIndex() - 1;
            if (segmentIndex < 0 || segmentIndex >= route.getSegments().size()) {
                pendingLocoRoutePreview = null;
                return;
            }
        } else if (isLocoCompletionTarget(route, target.get())) {
            mode = LocoPreviewMode.COMPLETE;
            segmentIndex = route.size() - 1;
        } else if (route.isInProgress() && nodeIndex < 0 && !route.isEmpty()) {
            mode = LocoPreviewMode.APPEND;
            segmentIndex = route.getSegments().size();
        } else {
            pendingLocoRoutePreview = null;
            return;
        }

        BlockPos targetPos = target.get();
        if (pendingLocoRoutePreview == null
            || !pendingLocoRoutePreview.matches(player.level(), route, targetPos, mode, segmentIndex)) {
            pendingLocoRoutePreview = new PendingLocoRoutePreview(player.level(), route, targetPos,
                player.level().getGameTime(), mode, segmentIndex);
            return;
        }
        advancePendingLocoRoutePreview(pendingLocoRoutePreview);
    }

    private static ItemStack getHeldTugRoute(Player player) {
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (mainHand.getItem().equals(ModItems.TUG_ROUTE.get())) {
            return mainHand;
        }

        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
        return offHand.getItem().equals(ModItems.TUG_ROUTE.get()) ? offHand : ItemStack.EMPTY;
    }

    private static ItemStack getHeldLocoRoute(Player player) {
        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (mainHand.getItem().equals(ModItems.LOCO_ROUTE.get())) return mainHand;
        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
        return offHand.getItem().equals(ModItems.LOCO_ROUTE.get()) ? offHand : ItemStack.EMPTY;
    }

    /** Keeps visual targets within the exact block-interaction range used for a use-on-block action. */
    private static Optional<BlockPos> getReachableLocoTarget(Player player) {
        if (!(Minecraft.getInstance().hitResult instanceof BlockHitResult hit)
            || !player.canInteractWithBlock(hit.getBlockPos(), 0.0D)) {
            return Optional.empty();
        }
        return LocoRouteItem.getTargetedRail(player.level(), hit.getBlockPos());
    }

    private static int findLocoNodeIndex(LocoRoute route, BlockPos target) {
        for (int index = 0; index < route.size(); index++) {
            if (route.get(index).isAt(target)) return index;
        }
        return -1;
    }

    private static boolean isLocoCompletionTarget(LocoRoute route, BlockPos target) {
        return route.isInProgress() && !route.isInserting() && route.size() >= 2
            && route.getNextInsertionIndex() == route.size() && route.getFirst().isAt(target);
    }

    private static void advancePendingLocoRoutePreview(PendingLocoRoutePreview preview) {
        if (preview.finished || preview.level.getGameTime() - preview.targetSince < LOCO_ROUTE_PREVIEW_DEBOUNCE_TICKS) return;
        if (!preview.initialized) initializePendingLocoRoutePreview(preview);
        if (preview.finished) return;

        if (preview.mode == LocoPreviewMode.APPEND || preview.mode == LocoPreviewMode.COMPLETE) {
            LocoRouteCompiler.PreviewStatus status = preview.pathfinder.advance(LOCO_ROUTE_PREVIEW_NODES_PER_TICK);
            if (status == LocoRouteCompiler.PreviewStatus.FOUND) {
                preview.before = preview.pathfinder.getResult().orElse(null);
                preview.finished = true;
            } else if (status == LocoRouteCompiler.PreviewStatus.FAILED) {
                preview.finished = true;
            }
            return;
        }

        int budgetPerSearch = Math.max(1, LOCO_ROUTE_PREVIEW_NODES_PER_TICK / (preview.replacements.size() * 2));
        for (LocoReplacementPreview candidate : preview.replacements) {
            if (candidate.before.getStatus() == LocoRouteCompiler.PreviewStatus.SEARCHING) {
                LocoRouteCompiler.PreviewStatus status = candidate.before.advance(budgetPerSearch);
                if (status == LocoRouteCompiler.PreviewStatus.FOUND) {
                    BlockPos end = preview.route.get((preview.segmentIndex + 1) % preview.route.size()).toBlockPos();
                    LocoRouteSegment original = preview.route.getSegments().get(preview.segmentIndex);
                    candidate.after = LocoRouteCompiler.createPreviewPathfinder(preview.level, preview.target, end,
                        candidate.midpoint, original.getArrivalDirection());
                }
            }
            if (candidate.after != null && candidate.after.getStatus() == LocoRouteCompiler.PreviewStatus.SEARCHING) {
                candidate.after.advance(budgetPerSearch);
            }
        }

        if (preview.replacements.stream().allMatch(LocoReplacementPreview::isTerminal)) {
            LocoReplacementPreview best = null;
            for (LocoReplacementPreview candidate : preview.replacements) {
                if (candidate.after == null || candidate.after.getStatus() != LocoRouteCompiler.PreviewStatus.FOUND) continue;
                if (best == null || candidate.before.getResult().orElseThrow().getSteps().size()
                    + candidate.after.getResult().orElseThrow().getSteps().size()
                    < best.before.getResult().orElseThrow().getSteps().size()
                    + best.after.getResult().orElseThrow().getSteps().size()) {
                    best = candidate;
                }
            }
            if (best != null) {
                preview.before = best.before.getResult().orElseThrow();
                preview.after = best.after.getResult().orElseThrow();
            }
            preview.finished = true;
        }
    }

    private static void initializePendingLocoRoutePreview(PendingLocoRoutePreview preview) {
        preview.initialized = true;
        if (preview.mode == LocoPreviewMode.APPEND) {
            BlockPos start = preview.route.getLast().toBlockPos();
            Direction incoming = preview.route.getSegments().isEmpty()
                ? null : preview.route.getSegments().getLast().getArrivalDirection();
            preview.pathfinder = LocoRouteCompiler.createPreviewPathfinder(preview.level, start, preview.target, incoming, null);
            return;
        }
        if (preview.mode == LocoPreviewMode.COMPLETE) {
            LocoRouteSegment first = preview.route.getSegments().getFirst();
            LocoRouteSegment last = preview.route.getSegments().getLast();
            preview.pathfinder = LocoRouteCompiler.createPreviewPathfinder(preview.level,
                preview.route.getLast().toBlockPos(), preview.target, last.getArrivalDirection(),
                first.getStartIncomingDirection());
            return;
        }

        LocoRouteSegment original = preview.route.getSegments().get(preview.segmentIndex);
        Optional<LocoRouteCompiler.SegmentSplit> split = LocoRouteCompiler.splitAt(original, preview.target);
        if (split.isPresent()) {
            preview.before = split.get().before();
            preview.after = split.get().after();
            preview.finished = true;
            return;
        }
        BlockPos start = preview.route.get(preview.segmentIndex).toBlockPos();
        for (Direction midpoint : List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)) {
            preview.replacements.add(new LocoReplacementPreview(midpoint,
                LocoRouteCompiler.createPreviewPathfinder(preview.level, start, preview.target,
                    original.getStartIncomingDirection(), midpoint)));
        }
    }

    private static void advancePendingTugRoutePreview(PendingTugRoutePreview preview) {
        if (preview.finished || preview.level.getGameTime() - preview.targetSince < TUG_ROUTE_PREVIEW_DEBOUNCE_TICKS) {
            return;
        }

        if (preview.route.isInserting() || isCompletionTarget(preview.route, preview.target)) {
            advanceFullRoutePreview(preview);
            return;
        }

        if (preview.pathfinder == null) {
            int insertionIndex = preview.route.getNextInsertionIndex();
            boolean insertingIntoSegment = preview.route.isInserting();
            BlockPos start = preview.route.get(insertionIndex > 0 ? insertionIndex - 1 : preview.route.size() - 1).toBlockPos();
            BlockPos next = insertingIntoSegment
                ? preview.route.get(insertionIndex < preview.route.size() ? insertionIndex : 0).toBlockPos()
                : null;
            double maxSegmentLength = ShippingConfig.Server.TUG_ROUTE_MAX_SEGMENT_LENGTH.get();
            if (Math.sqrt(start.distSqr(preview.target)) > maxSegmentLength
                || (next != null && Math.sqrt(next.distSqr(preview.target)) > maxSegmentLength)) {
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
            if (insertingIntoSegment) {
                preview.returnPathfinder = TugRouteCompiler.createPreviewPathfinder(
                    preview.level,
                    preview.target,
                    next,
                    getOccupiedPreviewBlocks(preview.route),
                    getBlockedPreviewWaypoints(preview.route, preview.target, next)
                );
            }
        }

        int pathBudget = preview.returnPathfinder == null ? TUG_ROUTE_PREVIEW_NODES_PER_TICK : TUG_ROUTE_PREVIEW_NODES_PER_TICK / 2;
        TugRouteCompiler.PreviewPathStatus status = preview.pathfinder.advance(pathBudget);
        if (status == TugRouteCompiler.PreviewPathStatus.FOUND) {
            if (preview.returnPathfinder == null) {
                preview.segment = preview.pathfinder.getResult().orElse(null);
                preview.finished = true;
                return;
            }
        } else if (status == TugRouteCompiler.PreviewPathStatus.FAILED) {
            preview.finished = true;
            return;
        }

        if (preview.returnPathfinder != null) {
            TugRouteCompiler.PreviewPathStatus returnStatus = preview.returnPathfinder.advance(pathBudget);
            if (returnStatus == TugRouteCompiler.PreviewPathStatus.FAILED) {
                preview.finished = true;
            } else if (status == TugRouteCompiler.PreviewPathStatus.FOUND
                && returnStatus == TugRouteCompiler.PreviewPathStatus.FOUND) {
                preview.segment = preview.pathfinder.getResult().orElse(null);
                preview.returnSegment = preview.returnPathfinder.getResult().orElse(null);
                preview.finished = true;
            }
        }
    }

    private static void advanceFullRoutePreview(PendingTugRoutePreview preview) {
        boolean completing = isCompletionTarget(preview.route, preview.target);
        if (preview.compileSession == null) {
            TugRoute candidate = preview.route.copy();
            if (!completing) {
                candidate.add(candidate.getNextInsertionIndex(), TugRouteNode.fromBlockPos(preview.target));
            }
            preview.compileSession = TugRouteCompiler.createCompileSession(
                preview.level,
                candidate,
                ShippingConfig.Server.TUG_ROUTE_MAX_SEGMENT_LENGTH.get()
            );
        }

        TugRouteCompiler.PreviewPathStatus status = preview.compileSession.advance(TUG_ROUTE_PREVIEW_NODES_PER_TICK);
        if (status == TugRouteCompiler.PreviewPathStatus.SEARCHING) {
            return;
        }
        if (status == TugRouteCompiler.PreviewPathStatus.FAILED) {
            preview.finished = true;
            return;
        }

        TugRoute compiled = preview.compileSession.getResult().route();
        if (completing) {
            preview.segment = compiled.getSegments().get(compiled.size() - 1);
            preview.finished = true;
            return;
        }
        int insertionIndex = preview.route.getNextInsertionIndex();
        preview.segment = compiled.getSegments().get(insertionIndex - 1);
        preview.returnSegment = compiled.getSegments().get(insertionIndex);
        preview.finished = true;
    }

    private static Set<BlockPos> getOccupiedPreviewBlocks(TugRoute route) {
        Set<BlockPos> occupied = new HashSet<>();
        int replacedSegment = route.isInserting() && route.getNextInsertionIndex() > 0
            ? route.getNextInsertionIndex() - 1
            : -1;
        for (int segmentIndex = 0; segmentIndex < route.getSegments().size(); segmentIndex++) {
            if (segmentIndex == replacedSegment) {
                continue;
            }
            for (TugRoutePoint point : route.getSegments().get(segmentIndex).getPoints()) {
                occupied.add(point.toBlockPos());
            }
        }
        return occupied;
    }

    private static Set<BlockPos> getBlockedPreviewWaypoints(TugRoute route, BlockPos... allowed) {
        Set<BlockPos> blocked = getTugRouteNodePositions(route);
        for (BlockPos pos : allowed) {
            blocked.remove(pos);
        }
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
            var camPos = Minecraft.getInstance().getEntityRenderDispatcher().camera.getPosition();
            var pose = event.getPoseStack();
            try (ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(16_384)) {
                var buffer = MultiBufferSource.immediate(byteBufferBuilder);
                LocoRoute locoRoute = LocoRouteItem.getRoute(stack);

                renderLocoRoutePath(pose, buffer, camPos, player.level(), locoRoute);
                renderLocoRouteNodes(pose, buffer, camPos, player.level(), locoRoute, new RouteColour(1.0F, 0.6F, 0.2F));
                renderPendingLocoRoutePreview(pose, buffer, camPos, locoRoute, player.level());
                renderLocoRouteTarget(pose, buffer, camPos, player, locoRoute);

                buffer.endBatch();
            }
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

    /** Renders stored rail traversal as the same double-line, directional language as tug routes. */
    private static void renderLocoRoutePath(PoseStack pose, MultiBufferSource.BufferSource buffer, Vec3 camPos,
                                            Level level, LocoRoute route) {
        if (route.getSegments().isEmpty() || route.isEmpty()) return;
        int selectedSegment = route.isInserting() ? route.getNextInsertionIndex() - 1 : -1;
        for (int segmentIndex = 0; segmentIndex < route.getSegments().size(); segmentIndex++) {
            RouteColour colour = segmentIndex == selectedSegment
                ? new RouteColour(0.0F, 0.0F, 0.0F) : new RouteColour(1.0F, 0.6F, 0.2F);
            renderLocoSegment(pose, buffer, camPos, level, route.get(segmentIndex % route.size()).toBlockPos(),
                route.getSegments().get(segmentIndex), route.get((segmentIndex + 1) % route.size()).toBlockPos(), colour);
        }
    }

    private static void renderLocoSegment(PoseStack pose, MultiBufferSource.BufferSource buffer, Vec3 camPos,
                                          Level level, BlockPos start, LocoRouteSegment segment, BlockPos end,
                                          RouteColour colour) {
        List<LocoRenderPoint> points = new ArrayList<>();
        if (segment.getSteps().isEmpty()) {
            addLocoRenderPoint(points, locoRailCenter(level, start), true);
        }
        for (int index = 0; index < segment.getSteps().size(); index++) {
            LocoRouteStep step = segment.getSteps().get(index);
            Direction highDirection = getAscendingHighDirection(level, step.railPos());
            boolean firstStep = index == 0;
            if (highDirection != null) {
                addLocoRenderPoint(points, locoRailPort(step.railPos(), step.incomingDirection().getOpposite(), highDirection), firstStep);
                addLocoRenderPoint(points, locoRailPort(step.railPos(), step.outgoingDirection(), highDirection), false);
            } else {
                addLocoRenderPoint(points, locoRailCenter(level, step.railPos()), firstStep);
            }
        }
        Direction endHighDirection = getAscendingHighDirection(level, end);
        Vec3 endPoint = endHighDirection == null
            ? locoRailCenter(level, end)
            : locoRailPort(end, segment.getArrivalDirection().getOpposite(), endHighDirection);
        addLocoRenderPoint(points, endPoint, true);
        renderLocoRoutePolyline(pose, buffer, camPos, points, colour);
    }

    /** Draws a compact miter at each ordinary rail turn, leaving route waypoints as hard boundaries. */
    private static void renderLocoRoutePolyline(PoseStack pose, MultiBufferSource.BufferSource buffer, Vec3 camPos,
                                                List<LocoRenderPoint> points, RouteColour colour) {
        List<PreviewSegment> segments = new ArrayList<>(Math.max(0, points.size() - 1));
        for (int index = 1; index < points.size(); index++) {
            PreviewSegment segment = trimLocoRouteSegment(points, index);
            segments.add(segment);
            if (segment != null) {
                renderLocoRouteLink(pose, buffer, camPos, segment, colour, index % 4 == 0);
            }
        }
        for (int pointIndex = 1; pointIndex < points.size() - 1; pointIndex++) {
            if (!isLocoRouteCorner(points, pointIndex)) continue;
            PreviewSegment incoming = segments.get(pointIndex - 1);
            PreviewSegment outgoing = segments.get(pointIndex);
            if (incoming == null || outgoing == null) continue;
            float alpha = RouteMarkerRenderer.computeAlpha(points.get(pointIndex).position(), camPos);
            if (alpha <= 0.0F) continue;
            var lines = buffer.getBuffer(ModRenderType.LINES);
            RouteMarkerRenderer.renderLine(pose, lines, camPos,
                incoming.to().add(incoming.side().scale(TUG_ROUTE_RAIL_OFFSET)),
                outgoing.from().add(outgoing.side().scale(TUG_ROUTE_RAIL_OFFSET)),
                colour.red(), colour.green(), colour.blue(), alpha);
            RouteMarkerRenderer.renderLine(pose, lines, camPos,
                incoming.to().subtract(incoming.side().scale(TUG_ROUTE_RAIL_OFFSET)),
                outgoing.from().subtract(outgoing.side().scale(TUG_ROUTE_RAIL_OFFSET)),
                colour.red(), colour.green(), colour.blue(), alpha);
        }
    }

    @Nullable
    private static PreviewSegment trimLocoRouteSegment(List<LocoRenderPoint> points, int endIndex) {
        LocoRenderPoint rawFrom = points.get(endIndex - 1);
        LocoRenderPoint rawTo = points.get(endIndex);
        Vec3 delta = rawTo.position().subtract(rawFrom.position());
        Vec3 horizontal = new Vec3(delta.x, 0.0D, delta.z);
        if (delta.lengthSqr() <= 1.0E-6D || horizontal.lengthSqr() <= 1.0E-6D) return null;
        Vec3 forward = delta.normalize();
        Vec3 side = new Vec3(-horizontal.z, 0.0D, horizontal.x).normalize();
        double clip = Math.min(LOCO_ROUTE_CORNER_CLIP_DISTANCE, delta.length() * 0.30D);
        Vec3 from = isLocoRouteCorner(points, endIndex - 1) ? rawFrom.position().add(forward.scale(clip)) : rawFrom.position();
        Vec3 to = isLocoRouteCorner(points, endIndex) ? rawTo.position().subtract(forward.scale(clip)) : rawTo.position();
        double length = from.distanceTo(to);
        return length <= 1.0E-4D ? null : new PreviewSegment(from, to, forward, side, length);
    }

    private static boolean isLocoRouteCorner(List<LocoRenderPoint> points, int pointIndex) {
        if (pointIndex <= 0 || pointIndex >= points.size() - 1 || points.get(pointIndex).isWaypoint()) return false;
        Vec3 incoming = points.get(pointIndex).position().subtract(points.get(pointIndex - 1).position());
        Vec3 outgoing = points.get(pointIndex + 1).position().subtract(points.get(pointIndex).position());
        Vec3 incomingHorizontal = new Vec3(incoming.x, 0.0D, incoming.z);
        Vec3 outgoingHorizontal = new Vec3(outgoing.x, 0.0D, outgoing.z);
        return incomingHorizontal.lengthSqr() > 1.0E-4D && outgoingHorizontal.lengthSqr() > 1.0E-4D
            && incomingHorizontal.normalize().dot(outgoingHorizontal.normalize()) < 0.999D;
    }

    private static void renderLocoRouteLink(PoseStack pose, MultiBufferSource.BufferSource buffer, Vec3 camPos,
                                            PreviewSegment segment, RouteColour colour, boolean drawArrow) {
        float alpha = RouteMarkerRenderer.computeAlpha(segment.from().add(segment.to()).scale(0.5D), camPos);
        if (alpha <= 0.0F) return;
        var lines = buffer.getBuffer(ModRenderType.LINES);
        RouteMarkerRenderer.renderLine(pose, lines, camPos, segment.from().add(segment.side().scale(TUG_ROUTE_RAIL_OFFSET)),
            segment.to().add(segment.side().scale(TUG_ROUTE_RAIL_OFFSET)), colour.red(), colour.green(), colour.blue(), alpha);
        RouteMarkerRenderer.renderLine(pose, lines, camPos, segment.from().subtract(segment.side().scale(TUG_ROUTE_RAIL_OFFSET)),
            segment.to().subtract(segment.side().scale(TUG_ROUTE_RAIL_OFFSET)), colour.red(), colour.green(), colour.blue(), alpha);
        if (drawArrow) {
            renderTugRouteArrow(pose, lines, camPos, segment.from().lerp(segment.to(), 0.5D), segment.forward(), segment.side(),
                colour.red(), colour.green(), colour.blue(), alpha);
        }
    }

    private static void addLocoRenderPoint(List<LocoRenderPoint> points, Vec3 position, boolean waypoint) {
        if (!points.isEmpty() && points.getLast().position().distanceToSqr(position) < 1.0E-8D) {
            if (waypoint && !points.getLast().isWaypoint()) {
                points.set(points.size() - 1, new LocoRenderPoint(position, true));
            }
            return;
        }
        points.add(new LocoRenderPoint(position, waypoint));
    }

    @Nullable
    private static Direction getAscendingHighDirection(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos) || !(level.getBlockState(pos).getBlock() instanceof BaseRailBlock)) return null;
        RailShape shape = RailHelper.getShape(pos, level);
        return switch (shape) {
            case ASCENDING_EAST -> Direction.EAST;
            case ASCENDING_WEST -> Direction.WEST;
            case ASCENDING_NORTH -> Direction.NORTH;
            case ASCENDING_SOUTH -> Direction.SOUTH;
            default -> null;
        };
    }

    private static Vec3 locoRailCenter(Level level, BlockPos pos) {
        double slopeMidpoint = getAscendingHighDirection(level, pos) == null ? 0.0D : 0.5D;
        return new Vec3(pos.getX() + 0.5D, pos.getY() + LOCO_ROUTE_HEIGHT + slopeMidpoint, pos.getZ() + 0.5D);
    }

    private static Vec3 locoRailPort(BlockPos pos, Direction edge, Direction highDirection) {
        return new Vec3(pos.getX() + 0.5D + edge.getStepX() * 0.5D,
            pos.getY() + LOCO_ROUTE_HEIGHT + (edge == highDirection ? 1.0D : 0.0D),
            pos.getZ() + 0.5D + edge.getStepZ() * 0.5D);
    }

    private static List<LocoRenderPoint> getTrackedLocoRenderPoints(Level level, List<BlockPos> vertices,
                                                                      List<BlockPos> waypoints) {
        Set<BlockPos> waypointPositions = new HashSet<>(waypoints);
        List<LocoRenderPoint> points = new ArrayList<>();
        for (int index = 0; index < vertices.size(); index++) {
            BlockPos pos = vertices.get(index);
            Direction incoming = index > 0 ? getHorizontalDirection(vertices.get(index - 1), pos) : null;
            Direction outgoing = index + 1 < vertices.size() ? getHorizontalDirection(pos, vertices.get(index + 1)) : null;
            Direction highDirection = getAscendingHighDirection(level, pos);
            boolean waypoint = waypointPositions.contains(pos);
            if (highDirection != null && incoming != null && outgoing != null) {
                addLocoRenderPoint(points, locoRailPort(pos, incoming.getOpposite(), highDirection), waypoint);
                addLocoRenderPoint(points, locoRailPort(pos, outgoing, highDirection), waypoint);
            } else {
                addLocoRenderPoint(points, locoRailCenter(level, pos), waypoint);
            }
        }
        return points;
    }

    @Nullable
    private static Direction getHorizontalDirection(BlockPos from, BlockPos to) {
        int deltaX = to.getX() - from.getX();
        int deltaZ = to.getZ() - from.getZ();
        if (Math.abs(deltaX) > Math.abs(deltaZ)) return deltaX > 0 ? Direction.EAST : Direction.WEST;
        if (Math.abs(deltaZ) > 0) return deltaZ > 0 ? Direction.SOUTH : Direction.NORTH;
        return null;
    }

    private static void renderLocoRouteNodes(PoseStack pose, MultiBufferSource.BufferSource buffer, Vec3 camPos,
                                             Level level, LocoRoute route, RouteColour colour) {
        for (LocoRouteNode node : route) {
            renderLocoRouteNodeBounds(pose, buffer.getBuffer(ModRenderType.LINES), camPos, level, node.toBlockPos(), colour);
        }
    }

    private static void renderLocoRouteTarget(PoseStack pose, MultiBufferSource.BufferSource buffer,
                                              Vec3 camPos, Player player, LocoRoute route) {
        getReachableLocoTarget(player).ifPresent(target -> {
            boolean node = route.stream().anyMatch(routeNode -> routeNode.isAt(target));
            boolean segment = !node && LocoRouteItem.findSegmentAt(route, target).isPresent();
            boolean completion = isLocoCompletionTarget(route, target);
            RouteColour colour;
            if (node) colour = completion ? new RouteColour(0.2F, 1.0F, 0.2F) : new RouteColour(0.0F, 0.0F, 0.0F);
            else if (route.isInserting() || route.isInProgress()) colour = new RouteColour(1.0F, 1.0F, 0.2F);
            else if (segment) colour = new RouteColour(1.0F, 1.0F, 0.2F);
            else return;
            renderLocoRouteNodeBounds(pose, buffer.getBuffer(ModRenderType.LINES), camPos, player.level(), target, colour);
        });
    }

    private static void renderPendingLocoRoutePreview(PoseStack pose, MultiBufferSource.BufferSource buffer, Vec3 camPos,
                                                       LocoRoute route, Level level) {
        PendingLocoRoutePreview preview = pendingLocoRoutePreview;
        if (preview == null || !preview.finished || preview.before == null || preview.level != level
            || !preview.route.equals(route)) return;
        RouteColour colour = preview.mode == LocoPreviewMode.COMPLETE
            ? new RouteColour(0.2F, 1.0F, 0.2F) : new RouteColour(1.0F, 1.0F, 0.2F);
        if (preview.mode == LocoPreviewMode.APPEND || preview.mode == LocoPreviewMode.COMPLETE) {
            renderLocoSegment(pose, buffer, camPos, level, route.getLast().toBlockPos(), preview.before, preview.target, colour);
            return;
        }
        BlockPos start = route.get(preview.segmentIndex).toBlockPos();
        BlockPos end = route.get((preview.segmentIndex + 1) % route.size()).toBlockPos();
        renderLocoSegment(pose, buffer, camPos, level, start, preview.before, preview.target, colour);
        if (preview.after != null) renderLocoSegment(pose, buffer, camPos, level, preview.target, preview.after, end, colour);
    }

    private static void renderLocoRouteNodeBounds(PoseStack pose, com.mojang.blaze3d.vertex.VertexConsumer lines,
                                                   Vec3 camPos, Level level, BlockPos pos, RouteColour colour) {
        double y = locoRailCenter(level, pos).y + 0.01D;
        Vec3 northWest = new Vec3(pos.getX() + 0.1D, y, pos.getZ() + 0.1D);
        Vec3 northEast = new Vec3(pos.getX() + 0.9D, y, pos.getZ() + 0.1D);
        Vec3 southEast = new Vec3(pos.getX() + 0.9D, y, pos.getZ() + 0.9D);
        Vec3 southWest = new Vec3(pos.getX() + 0.1D, y, pos.getZ() + 0.9D);
        RouteMarkerRenderer.renderLine(pose, lines, camPos, northWest, northEast, colour.red(), colour.green(), colour.blue(), 1.0F);
        RouteMarkerRenderer.renderLine(pose, lines, camPos, northEast, southEast, colour.red(), colour.green(), colour.blue(), 1.0F);
        RouteMarkerRenderer.renderLine(pose, lines, camPos, southEast, southWest, colour.red(), colour.green(), colour.blue(), 1.0F);
        RouteMarkerRenderer.renderLine(pose, lines, camPos, southWest, northWest, colour.red(), colour.green(), colour.blue(), 1.0F);
    }

    private static void renderTugRoutePreview(RenderLevelStageEvent event, ItemStack stack) {
        var camera = Minecraft.getInstance().getEntityRenderDispatcher().camera;
        var camPos = camera.getPosition();
        var pose = event.getPoseStack();
        try (ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(1536)) {
            var buffer = MultiBufferSource.immediate(byteBufferBuilder);
            TugRoute route = TugRouteItem.getRoute(stack);
            List<PreviewPoint> previewPoints = flattenTugRoutePreviewPoints(route);
            Set<BlockPos> replacedSegmentPoints = getReplacedSegmentPoints(route);
            boolean completionTarget = isCompletionTarget(route);
            float routeRed = completionTarget ? 0.2f : 1.0f;
            float routeGreen = completionTarget ? 1.0f : 0.6f;
            float routeBlue = 0.2f;

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
                boolean isReplacedSegment = replacedSegmentPoints.contains(toBlockPos(from.position()))
                    && replacedSegmentPoints.contains(toBlockPos(to.position()));
                float segmentRed = isReplacedSegment ? 0.0f : routeRed;
                float segmentGreen = isReplacedSegment ? 0.0f : routeGreen;
                float segmentBlue = isReplacedSegment ? 0.0f : routeBlue;
                float railAlpha = RouteMarkerRenderer.computeAlpha(segment.from().add(segment.to()).scale(0.5D), camPos);
                if (railAlpha > 0.0f) {
                    var lineBuffer = buffer.getBuffer(ModRenderType.LINES);
                    RouteMarkerRenderer.renderLine(pose, lineBuffer, camPos, leftFrom, leftTo, segmentRed, segmentGreen, segmentBlue, railAlpha);
                    RouteMarkerRenderer.renderLine(pose, lineBuffer, camPos, rightFrom, rightTo, segmentRed, segmentGreen, segmentBlue, railAlpha);
                }

                double segmentLength = segment.length();
                while (segmentLength > 1.0E-6D && travelled + segmentLength >= nextArrowDistance) {
                    double ratio = (nextArrowDistance - travelled) / segmentLength;
                    Vec3 arrowCenter = segment.from().lerp(segment.to(), ratio);
                    float arrowAlpha = RouteMarkerRenderer.computeAlpha(arrowCenter, camPos);
                    if (arrowAlpha > 0.0f && isArrowClearOfCorners(arrowCenter, segment.forward(), segment.side(), previewPoints)) {
                        renderTugRouteArrow(pose, buffer.getBuffer(ModRenderType.LINES), camPos, arrowCenter, segment.forward(), segment.side(), segmentRed, segmentGreen, segmentBlue, arrowAlpha);
                    }
                    nextArrowDistance += TUG_ROUTE_ARROW_SPACING;
                }

                travelled += segmentLength;
            }

            renderNonNodeCorners(pose, buffer, camPos, previewPoints, routeRed, routeGreen, routeBlue);

            if (route.isInserting() && route.getNextInsertionIndex() > 0) {
                int replacedSegmentIndex = route.getNextInsertionIndex() - 1;
                if (replacedSegmentIndex < route.getSegments().size()) {
                    renderTugRouteSegment(pose, buffer, camPos, route.getSegments().get(replacedSegmentIndex), 0.0f, 0.0f, 0.0f);
                }
            }

            for (int i = 0, routeSize = route.size(); i < routeSize; i++) {
                TugRouteNode node = route.get(i);
                Vec3 nodePos = toWaterSurface(Vec3.atCenterOf(node.toBlockPos()));
                float alpha = RouteMarkerRenderer.computeAlpha(nodePos, camPos);
                if (alpha <= 0.0f) {
                    continue;
                }
                var lineBuffer = buffer.getBuffer(ModRenderType.LINES);
                renderTugRouteNodeBounds(pose, lineBuffer, camPos, node.toBlockPos(), routeRed, routeGreen, routeBlue, alpha);
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
    }

    private static boolean isCompletionTarget(TugRoute route) {
        Player player = Minecraft.getInstance().player;
        return player != null
            && TugRouteItem.getTargetedWaypoint(player.level(), player)
                .map(pos -> isCompletionTarget(route, pos))
                .orElse(false);
    }

    private static boolean isCompletionTarget(TugRoute route, BlockPos target) {
        return route.isInProgress()
            && !route.isInserting()
            && route.getNextInsertionIndex() == route.size()
            && route.size() >= 2
            && route.getFirst().isAt(target);
    }

    private static Set<BlockPos> getReplacedSegmentPoints(TugRoute route) {
        if (!route.isInserting() || route.getNextInsertionIndex() <= 0) {
            return Set.of();
        }

        int segmentIndex = route.getNextInsertionIndex() - 1;
        if (segmentIndex >= route.getSegments().size()) {
            return Set.of();
        }

        Set<BlockPos> points = new HashSet<>();
        for (TugRoutePoint point : route.getSegments().get(segmentIndex).getPoints()) {
            points.add(point.toBlockPos());
        }
        return points;
    }

    private static BlockPos toBlockPos(Vec3 position) {
        return new BlockPos(
            (int) Math.floor(position.x),
            (int) Math.floor(position.y - TUG_ROUTE_SURFACE_Y_OFFSET),
            (int) Math.floor(position.z)
        );
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
                if (route.isInProgress() && !route.isInserting() && route.getNextInsertionIndex() == route.size() && route.size() >= 2
                    && route.getFirst().isAt(pos)) {
                    renderTugRouteNodeBounds(pose, lineBuffer, camPos, pos, 0.2f, 1.0f, 0.2f, 1.0f);
                    getPendingTugRouteSegment(player.level(), route, pos).ifPresent(segment ->
                        renderTugRouteSegment(pose, buffer, camPos, segment, 0.2f, 1.0f, 0.2f)
                    );
                    return;
                }
                renderTugRouteRemovalTarget(pose, lineBuffer, camPos, pos);
                return;
            }

            if (route.isComplete()) {
                TugRouteItem.findSegmentAt(route, pos).ifPresent(segmentIndex ->
                    renderPendingTugRouteSegment(pose, buffer, camPos, route.getSegments().get(segmentIndex))
                );
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

            if (isPendingTugRoutePreviewLoading(player.level(), route, pos)) {
                renderTugRouteLoadingSpinner(pose, lineBuffer, camPos, pos, player.level().getGameTime());
            }

            getPendingTugRouteSegment(player.level(), route, pos).ifPresent(segment ->
                renderPendingTugRouteSegment(pose, buffer, camPos, segment)
            );
            getPendingTugRouteReturnSegment(player.level(), route, pos).ifPresent(segment ->
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

    private static boolean isPendingTugRoutePreviewLoading(Level level, TugRoute route, BlockPos target) {
        return pendingTugRoutePreview != null
            && pendingTugRoutePreview.matches(level, route, target)
            && pendingTugRoutePreview.compileSession != null
            && pendingTugRoutePreview.compileSession.getStatus() == TugRouteCompiler.PreviewPathStatus.SEARCHING;
    }

    private static void renderTugRouteLoadingSpinner(PoseStack pose, com.mojang.blaze3d.vertex.VertexConsumer lineBuffer,
                                                      Vec3 camPos, BlockPos pos, long gameTime) {
        double y = pos.getY() + TUG_ROUTE_SURFACE_Y_OFFSET + 0.002D;
        Vec3 center = new Vec3(pos.getX() + 0.5D, y, pos.getZ() + 0.5D);
        int activeSpoke = (int) ((gameTime / 2L) % 8L);
        for (int spoke = 0; spoke < 8; spoke++) {
            double angle = spoke * (Math.PI / 4.0D);
            Vec3 direction = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
            float brightness = spoke == activeSpoke ? 1.0f : 0.35f;
            RouteMarkerRenderer.renderLine(pose, lineBuffer, camPos,
                center.add(direction.scale(0.10D)), center.add(direction.scale(0.28D)),
                1.0f, 1.0f, 0.3f, brightness);
        }
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
        if (route.isEmpty() || (route.stream().anyMatch(node -> node.isAt(target)) && !isCompletionTarget(route, target))) {
            return Optional.empty();
        }

        if (pendingTugRoutePreview == null || !pendingTugRoutePreview.matches(level, route, target)) {
            return Optional.empty();
        }
        return Optional.ofNullable(pendingTugRoutePreview.segment);
    }

    private static Optional<TugRouteSegment> getPendingTugRouteReturnSegment(Level level, TugRoute route, BlockPos target) {
        if (pendingTugRoutePreview == null || !pendingTugRoutePreview.matches(level, route, target)) {
            return Optional.empty();
        }
        return Optional.ofNullable(pendingTugRoutePreview.returnSegment);
    }

    private static void renderPendingTugRouteSegment(PoseStack pose, MultiBufferSource.BufferSource buffer, Vec3 camPos,
                                                      TugRouteSegment segment) {
        renderTugRouteSegment(pose, buffer, camPos, segment, 1.0f, 1.0f, 0.3f);
    }

    private static void renderTugRouteSegment(PoseStack pose, MultiBufferSource.BufferSource buffer, Vec3 camPos,
                                              TugRouteSegment segment, float red, float green, float blue) {
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
                RouteMarkerRenderer.renderLine(pose, lineBuffer, camPos, leftFrom, leftTo, red, green, blue, alpha);
                RouteMarkerRenderer.renderLine(pose, lineBuffer, camPos, rightFrom, rightTo, red, green, blue, alpha);
            }
        }

        renderNonNodeCorners(pose, buffer, camPos, previewPoints, red, green, blue);
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
        if (route.isComplete() && rawPoints.size() > 1) {
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
        return isOutsideMarkerExclusion(center, previewPoints)
                && isOutsideMarkerExclusion(tip, previewPoints)
                && isOutsideMarkerExclusion(left, previewPoints)
                && isOutsideMarkerExclusion(right, previewPoints);
    }

    private static boolean isOutsideMarkerExclusion(Vec3 position, List<PreviewPoint> previewPoints) {
        int blockX = (int) Math.floor(position.x);
        int blockZ = (int) Math.floor(position.z);
        for (PreviewPoint point : previewPoints) {
            if (!point.isCorner() && !point.isNode()) {
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
                                                 Vec3 camPos, BlockPos pos, float red, float green, float blue, float alpha) {
        double y = pos.getY() + TUG_ROUTE_SURFACE_Y_OFFSET;
        Vec3 nw = new Vec3(pos.getX(), y, pos.getZ());
        Vec3 ne = new Vec3(pos.getX() + 1.0D, y, pos.getZ());
        Vec3 se = new Vec3(pos.getX() + 1.0D, y, pos.getZ() + 1.0D);
        Vec3 sw = new Vec3(pos.getX(), y, pos.getZ() + 1.0D);
        RouteMarkerRenderer.renderLine(pose, lineBuffer, camPos, nw, ne, red, green, blue, alpha);
        RouteMarkerRenderer.renderLine(pose, lineBuffer, camPos, ne, se, red, green, blue, alpha);
        RouteMarkerRenderer.renderLine(pose, lineBuffer, camPos, se, sw, red, green, blue, alpha);
        RouteMarkerRenderer.renderLine(pose, lineBuffer, camPos, sw, nw, red, green, blue, alpha);
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

    private static int comparePositions(Vec3 first, Vec3 second) {
        int comparison = Double.compare(first.x, second.x);
        if (comparison != 0) return comparison;
        comparison = Double.compare(first.y, second.y);
        return comparison != 0 ? comparison : Double.compare(first.z, second.z);
    }

    private static void addCompositeStroke(Map<CompositeStrokeKey, CompositeStroke> strokes,
                                           Vec3 from, Vec3 to, boolean doubleLine, int entityId,
                                           RouteColour colour, boolean drawArrow) {
        if (from.distanceToSqr(to) <= 1.0E-8D) return;
        CompositeStrokeKey key = CompositeStrokeKey.of(from, to, doubleLine);
        CompositeStroke stroke = strokes.computeIfAbsent(key, CompositeStroke::new);
        CompositeStrokeMember member = stroke.members.computeIfAbsent(entityId,
            ignored -> new CompositeStrokeMember(colour));
        if (drawArrow) {
            if (from.equals(key.first())) member.arrowForward = true;
            else member.arrowReverse = true;
        }
    }

    private static void addCompositeTugRoute(Map<CompositeStrokeKey, CompositeStroke> strokes,
                                             TugRouteTrackerData route, RouteColour colour) {
        Set<BlockPos> waypoints = new HashSet<>(route.waypointPositions());
        List<PreviewPoint> points = RouteOverlayPath.expandStraightSegments(route.pathVertices()).stream()
            .map(point -> new PreviewPoint(toWaterSurface(Vec3.atCenterOf(point)), waypoints.contains(point), false))
            .toList();
        points = markCornerPoints(points);

        double travelled = 0.0D;
        double nextArrowDistance = TUG_ROUTE_ARROW_SPACING * 0.5D;
        for (int index = 1; index < points.size(); index++) {
            PreviewSegment segment = trimPreviewSegment(points.get(index - 1), points.get(index));
            if (segment == null) continue;
            double length = segment.length();
            boolean drawArrow = length > 1.0E-6D && travelled + length >= nextArrowDistance;
            addCompositeStroke(strokes, segment.from(), segment.to(), true, route.entityId(), colour, drawArrow);
            travelled += length;
            while (nextArrowDistance <= travelled) nextArrowDistance += TUG_ROUTE_ARROW_SPACING;
        }

        for (int index = 1; index < points.size() - 1; index++) {
            PreviewPoint corner = points.get(index);
            if (!corner.isCorner() || corner.isNode()) continue;
            RailPort incoming = getCornerEntryPort(points.get(index - 1), corner);
            RailPort outgoing = getCornerExitPort(corner, points.get(index + 1));
            if (incoming == null || outgoing == null) continue;
            addCompositeStroke(strokes,
                incoming.position().add(incoming.side().scale(TUG_ROUTE_RAIL_OFFSET)),
                outgoing.position().add(outgoing.side().scale(TUG_ROUTE_RAIL_OFFSET)),
                false, route.entityId(), colour, false);
            addCompositeStroke(strokes,
                incoming.position().subtract(incoming.side().scale(TUG_ROUTE_RAIL_OFFSET)),
                outgoing.position().subtract(outgoing.side().scale(TUG_ROUTE_RAIL_OFFSET)),
                false, route.entityId(), colour, false);
        }

        for (BlockPos waypoint : route.waypointPositions()) {
            addTugWaypointStrokes(strokes, waypoint, route.entityId(), colour);
        }
    }

    private static void addCompositeLocoRoute(Map<CompositeStrokeKey, CompositeStroke> strokes,
                                              List<LocoRenderPoint> points, int entityId, RouteColour colour) {
        List<PreviewSegment> segments = new ArrayList<>(Math.max(0, points.size() - 1));
        for (int index = 1; index < points.size(); index++) {
            PreviewSegment segment = trimLocoRouteSegment(points, index);
            segments.add(segment);
            if (segment != null) {
                addCompositeStroke(strokes, segment.from(), segment.to(), true, entityId, colour, index % 4 == 0);
            }
        }

        for (int pointIndex = 1; pointIndex < points.size() - 1; pointIndex++) {
            if (!isLocoRouteCorner(points, pointIndex)) continue;
            PreviewSegment incoming = segments.get(pointIndex - 1);
            PreviewSegment outgoing = segments.get(pointIndex);
            if (incoming == null || outgoing == null) continue;
            addCompositeStroke(strokes,
                incoming.to().add(incoming.side().scale(TUG_ROUTE_RAIL_OFFSET)),
                outgoing.from().add(outgoing.side().scale(TUG_ROUTE_RAIL_OFFSET)),
                false, entityId, colour, false);
            addCompositeStroke(strokes,
                incoming.to().subtract(incoming.side().scale(TUG_ROUTE_RAIL_OFFSET)),
                outgoing.from().subtract(outgoing.side().scale(TUG_ROUTE_RAIL_OFFSET)),
                false, entityId, colour, false);
        }
    }

    private static void addTugWaypointStrokes(Map<CompositeStrokeKey, CompositeStroke> strokes,
                                              BlockPos pos, int entityId, RouteColour colour) {
        double y = pos.getY() + TUG_ROUTE_SURFACE_Y_OFFSET;
        Vec3 northWest = new Vec3(pos.getX(), y, pos.getZ());
        Vec3 northEast = new Vec3(pos.getX() + 1.0D, y, pos.getZ());
        Vec3 southEast = new Vec3(pos.getX() + 1.0D, y, pos.getZ() + 1.0D);
        Vec3 southWest = new Vec3(pos.getX(), y, pos.getZ() + 1.0D);
        addCompositeStroke(strokes, northWest, northEast, false, entityId, colour, false);
        addCompositeStroke(strokes, northEast, southEast, false, entityId, colour, false);
        addCompositeStroke(strokes, southEast, southWest, false, entityId, colour, false);
        addCompositeStroke(strokes, southWest, northWest, false, entityId, colour, false);
    }

    private static void addLocoWaypointStrokes(Map<CompositeStrokeKey, CompositeStroke> strokes,
                                               Level level, BlockPos pos, int entityId, RouteColour colour) {
        double y = locoRailCenter(level, pos).y + 0.01D;
        Vec3 northWest = new Vec3(pos.getX() + 0.1D, y, pos.getZ() + 0.1D);
        Vec3 northEast = new Vec3(pos.getX() + 0.9D, y, pos.getZ() + 0.1D);
        Vec3 southEast = new Vec3(pos.getX() + 0.9D, y, pos.getZ() + 0.9D);
        Vec3 southWest = new Vec3(pos.getX() + 0.1D, y, pos.getZ() + 0.9D);
        addCompositeStroke(strokes, northWest, northEast, false, entityId, colour, false);
        addCompositeStroke(strokes, northEast, southEast, false, entityId, colour, false);
        addCompositeStroke(strokes, southEast, southWest, false, entityId, colour, false);
        addCompositeStroke(strokes, southWest, northWest, false, entityId, colour, false);
    }

    private static int renderCompositeStrokes(PoseStack pose, MultiBufferSource.BufferSource buffer, Vec3 camPos,
                                              Map<CompositeStrokeKey, CompositeStroke> strokes, int remainingVertices) {
        List<CompositeStroke> ordered = new ArrayList<>(strokes.values());
        ordered.sort((first, second) -> {
            double firstDistance = first.key.first().lerp(first.key.second(), 0.5D).distanceToSqr(camPos);
            double secondDistance = second.key.first().lerp(second.key.second(), 0.5D).distanceToSqr(camPos);
            int comparison = Double.compare(firstDistance, secondDistance);
            if (comparison != 0) return comparison;
            comparison = comparePositions(first.key.first(), second.key.first());
            if (comparison != 0) return comparison;
            comparison = comparePositions(first.key.second(), second.key.second());
            if (comparison != 0) return comparison;
            return Boolean.compare(first.key.doubleLine(), second.key.doubleLine());
        });

        var lineBuffer = buffer.getBuffer(ModRenderType.LINES);
        for (CompositeStroke stroke : ordered) {
            if (remainingVertices < 2) break;
            Vec3 from = stroke.key.first();
            Vec3 to = stroke.key.second();
            float alpha = RouteMarkerRenderer.computeAlpha(from.lerp(to, 0.5D), camPos);
            if (alpha <= 0.0F) continue;

            List<Map.Entry<Integer, CompositeStrokeMember>> members = new ArrayList<>(stroke.members.entrySet());
            members.sort(Map.Entry.comparingByKey());
            Vec3 side = null;
            if (stroke.key.doubleLine()) {
                Vec3 delta = to.subtract(from);
                Vec3 horizontal = new Vec3(delta.x, 0.0D, delta.z);
                if (horizontal.lengthSqr() <= 1.0E-6D) continue;
                Vec3 forward = horizontal.normalize();
                side = new Vec3(-forward.z, 0.0D, forward.x);
            }

            for (int index = 0; index < members.size(); index++) {
                int vertices = stroke.key.doubleLine() ? 4 : 2;
                if (remainingVertices < vertices) return remainingVertices;
                double start = index / (double) members.size();
                double end = (index + 1) / (double) members.size();
                Vec3 bandFrom = from.lerp(to, start);
                Vec3 bandTo = from.lerp(to, end);
                RouteColour colour = members.get(index).getValue().colour;
                if (stroke.key.doubleLine()) {
                    Vec3 offset = side.scale(TUG_ROUTE_RAIL_OFFSET);
                    RouteMarkerRenderer.renderLine(pose, lineBuffer, camPos,
                        bandFrom.add(offset), bandTo.add(offset), colour.red(), colour.green(), colour.blue(), alpha);
                    RouteMarkerRenderer.renderLine(pose, lineBuffer, camPos,
                        bandFrom.subtract(offset), bandTo.subtract(offset), colour.red(), colour.green(), colour.blue(), alpha);
                } else {
                    RouteMarkerRenderer.renderLine(pose, lineBuffer, camPos, bandFrom, bandTo,
                        colour.red(), colour.green(), colour.blue(), alpha);
                }
                remainingVertices -= vertices;
            }

            if (!stroke.key.doubleLine() || from.distanceTo(to) < 0.5D || remainingVertices < 4) continue;
            boolean forwardArrow = members.stream().anyMatch(entry -> entry.getValue().arrowForward);
            boolean reverseArrow = members.stream().anyMatch(entry -> entry.getValue().arrowReverse);
            if (!forwardArrow && !reverseArrow) continue;
            Vec3 forward = to.subtract(from).normalize();
            if (forwardArrow && remainingVertices >= 4) {
                RouteColour arrowColour = members.stream()
                    .map(Map.Entry::getValue)
                    .filter(member -> member.arrowForward)
                    .findFirst()
                    .orElseThrow()
                    .colour;
                renderTugRouteArrow(pose, lineBuffer, camPos, from.lerp(to, reverseArrow ? 0.35D : 0.5D),
                    forward, side, arrowColour.red(), arrowColour.green(), arrowColour.blue(), alpha);
                remainingVertices -= 4;
            }
            if (reverseArrow && remainingVertices >= 4) {
                RouteColour arrowColour = members.stream()
                    .map(Map.Entry::getValue)
                    .filter(member -> member.arrowReverse)
                    .findFirst()
                    .orElseThrow()
                    .colour;
                renderTugRouteArrow(pose, lineBuffer, camPos, from.lerp(to, forwardArrow ? 0.65D : 0.5D),
                    forward.scale(-1.0D), side, arrowColour.red(), arrowColour.green(), arrowColour.blue(), alpha);
                remainingVertices -= 4;
            }
        }
        return remainingVertices;
    }

    /** Draws cached, render-only route snapshots. The route-item editor overlay remains separate. */
    private static void renderTrackedTugRoutes(RenderLevelStageEvent event, Player player, Vec3 camPos) {
        if (!ShippingConfig.Client.SHOW_WRENCH_TUG_ROUTES.get()
            || !player.level().dimension().toString().equals(VehicleTrackerPacketHandler.tugRouteDimension)
            || VehicleTrackerPacketHandler.tugRoutes.isEmpty()) {
            return;
        }

        Map<Integer, EntityPosition> positions = new HashMap<>();
        for (EntityPosition position : VehicleTrackerPacketHandler.toRender) {
            positions.put(position.id(), position);
        }

        List<TugRouteTrackerData> routes = new ArrayList<>();
        for (TugRouteTrackerData route : VehicleTrackerPacketHandler.tugRoutes.values()) {
            if (positions.containsKey(route.entityId()) && isTrackedRouteNearCamera(route, camPos)) {
                routes.add(route);
            }
        }
        routes.sort(Comparator.comparingDouble(route -> positions.get(route.entityId()).pos().distanceToSqr(camPos)));

        try (ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(16_384)) {
            MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(byteBufferBuilder);
            Map<CompositeStrokeKey, CompositeStroke> strokes = new HashMap<>();
            for (TugRouteTrackerData route : routes) {
                int colourValue = DyeColor.byId(route.dyeColorId()).getTextureDiffuseColor();
                RouteColour colour = new RouteColour(
                    ((colourValue >> 16) & 0xFF) / 255.0F,
                    ((colourValue >> 8) & 0xFF) / 255.0F,
                    (colourValue & 0xFF) / 255.0F
                );
                addCompositeTugRoute(strokes, route, colour);
            }
            renderCompositeStrokes(event.getPoseStack(), buffer, camPos, strokes, MAX_TRACKED_TUG_ROUTE_VERTICES_PER_FRAME);
            buffer.endBatch();
        }
    }

    private static boolean isTrackedRouteNearCamera(TugRouteTrackerData route, Vec3 camPos) {
        if (route.pathVertices().isEmpty()) {
            return false;
        }

        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;
        for (BlockPos point : route.pathVertices()) {
            minX = Math.min(minX, point.getX());
            minY = Math.min(minY, point.getY());
            minZ = Math.min(minZ, point.getZ());
            maxX = Math.max(maxX, point.getX() + 1.0D);
            maxY = Math.max(maxY, point.getY() + 1.0D);
            maxZ = Math.max(maxZ, point.getZ() + 1.0D);
        }
        double dx = Math.max(minX - camPos.x, Math.max(0.0D, camPos.x - maxX));
        double dy = Math.max(minY - camPos.y, Math.max(0.0D, camPos.y - maxY));
        double dz = Math.max(minZ - camPos.z, Math.max(0.0D, camPos.z - maxZ));
        return dx * dx + dy * dy + dz * dz <= 128.0D * 128.0D;
    }

    private static void renderTrackedLocoRoutes(RenderLevelStageEvent event, Vec3 camPos) {
        if (!ShippingConfig.Client.SHOW_WRENCH_LOCO_ROUTES.get()
            || !Minecraft.getInstance().level.dimension().toString().equals(VehicleTrackerPacketHandler.locoRouteDimension)
            || VehicleTrackerPacketHandler.locoRoutes.isEmpty()) {
            return;
        }
        try (ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(16_384)) {
            MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(byteBufferBuilder);
            Map<CompositeStrokeKey, CompositeStroke> strokes = new HashMap<>();
            List<LocoRouteTrackerData> routes = new ArrayList<>(VehicleTrackerPacketHandler.locoRoutes.values());
            routes.sort(Comparator.comparingInt(LocoRouteTrackerData::entityId));
            Level level = Minecraft.getInstance().level;
            for (LocoRouteTrackerData route : routes) {
                int colour = DyeColor.byId(route.dyeColorId()).getTextureDiffuseColor();
                RouteColour routeColour = new RouteColour(
                    ((colour >> 16) & 0xFF) / 255.0F,
                    ((colour >> 8) & 0xFF) / 255.0F,
                    (colour & 0xFF) / 255.0F
                );
                addCompositeLocoRoute(strokes,
                    getTrackedLocoRenderPoints(level, route.pathVertices(), route.waypointPositions()),
                    route.entityId(), routeColour);
                for (BlockPos waypoint : route.waypointPositions()) {
                    addLocoWaypointStrokes(strokes, level, waypoint, route.entityId(), routeColour);
                }
            }
            renderCompositeStrokes(event.getPoseStack(), buffer, camPos, strokes, Integer.MAX_VALUE);
            buffer.endBatch();
        }
    }

    @SubscribeEvent
    public static void onRenderWorldLast(RenderLevelStageEvent event) {
        if(!event.getStage().equals(RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS)){
            return;
        }

        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        renderedTugRouteTargetPreview = false;

        ItemStack mainStack = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offStack = player.getItemInHand(InteractionHand.OFF_HAND);

        // TODO: figure out if we want to disable offstack rendering when mainstack is rendered successfully.
        renderRouteOnStack(event, player, mainStack);
        renderRouteOnStack(event, player, offStack);

        // Render registered vehicles when the conductor's wrench is held in either hand
        boolean holdingWrench = mainStack.is(ModItems.CONDUCTORS_WRENCH.get())
                || offStack.is(ModItems.CONDUCTORS_WRENCH.get());
        if (holdingWrench && player.level().dimension().toString().equals(VehicleTrackerPacketHandler.toRenderDimension)){
            var camera = Minecraft.getInstance().getEntityRenderDispatcher().camera;
            Vec3 camPos = camera.getPosition();
            renderTrackedTugRoutes(event, player, camPos);
            renderTrackedLocoRoutes(event, camPos);

            try (ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(1536)) {
                MultiBufferSource.BufferSource renderTypeBuffer = MultiBufferSource.immediate(byteBufferBuilder);

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
    }

    private static Vec3 computeFixedDistance(Vec3 target, Vec3 position, double scale){
        target = target.add(0, 2, 0);
        Vec3 delta = position.vectorTo(target);

        // The distance from the player camera to render the element
        var dist = Math.min(5, delta.length());
        return position.add(delta.normalize().scale(dist * scale));
    }
}
