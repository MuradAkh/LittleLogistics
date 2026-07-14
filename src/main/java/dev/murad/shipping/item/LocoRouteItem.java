package dev.murad.shipping.item;

import dev.murad.shipping.setup.ModDataComponents;
import dev.murad.shipping.util.LocoRoute;
import dev.murad.shipping.util.LocoRouteCompiler;
import dev.murad.shipping.util.LocoRouteNode;
import dev.murad.shipping.util.LocoRouteSegment;
import dev.murad.shipping.util.LocoRouteStep;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/** Server-authoritative editor for ordered, compiled locomotive routes. */
public class LocoRouteItem extends Item {
    public LocoRouteItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide) return InteractionResult.SUCCESS;
        ItemStack stack = context.getItemInHand();
        LocoRoute route = getRoute(stack);
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown() && route.isInserting()) {
            route.markComplete();
            saveRoute(route, stack);
            message(context.getPlayer(), Component.literal("Locomotive route segment edit cancelled."));
            return InteractionResult.SUCCESS;
        }
        Optional<BlockPos> target = getTargetedRail(context.getLevel(), context.getClickedPos());
        if (target.isEmpty()) return InteractionResult.PASS;
        editRouteAt(context.getLevel(), stack, route, target.get(), context.getPlayer());
        return InteractionResult.SUCCESS;
    }

    public static Optional<BlockPos> getTargetedRail(Level level, BlockPos clicked) {
        // The route UI must agree with Minecraft's highlighted block.  In particular,
        // do not promote an adjacent support block to the rail above or below it.
        return level.getBlockState(clicked).getBlock() instanceof BaseRailBlock
            ? Optional.of(clicked.immutable())
            : Optional.empty();
    }

    private static void editRouteAt(Level level, ItemStack stack, LocoRoute route, BlockPos target, @Nullable Player player) {
        int nodeIndex = findNodeIndex(route, target);

        if (route.isInserting()) {
            if (nodeIndex >= 0) {
                // Editing a segment is a temporary state.  A node click still has the
                // normal removal meaning, so restore the complete-route invariant first.
                route.markComplete();
                removeNode(level, stack, route, nodeIndex, player);
            } else {
                commitSegmentInsertion(level, stack, route, target, player);
            }
            return;
        }

        if (nodeIndex >= 0) {
            if (route.isInProgress() && !route.isInserting() && nodeIndex == 0 && route.size() >= 2
                && route.getNextInsertionIndex() == route.size()) {
                completeRoute(level, stack, route, player);
            } else {
                removeNode(level, stack, route, nodeIndex, player);
            }
            return;
        }

        if (route.isComplete()) {
            OptionalInt segmentIndex = findSegmentAt(route, target);
            if (segmentIndex.isPresent()) {
                route.beginInsertion(segmentIndex.getAsInt() + 1);
                saveRoute(route, stack);
                message(player, Component.literal("Select a rail to insert a locomotive route waypoint."));
            } else {
                message(player, Component.literal("Select a highlighted locomotive route segment to insert a waypoint."));
            }
            return;
        }

        appendWaypoint(level, stack, route, target, player);
    }

    private static void appendWaypoint(Level level, ItemStack stack, LocoRoute route, BlockPos target, @Nullable Player player) {
        if (route.isEmpty()) {
            route.add(LocoRouteNode.fromBlockPos(target));
            route.beginAppending();
            saveRoute(route, stack);
            message(player, Component.translatable("item.littlelogistics.locomotive_route.added", target.getX(), target.getY(), target.getZ()));
            return;
        }

        BlockPos start = route.getLast().toBlockPos();
        var incoming = route.getSegments().isEmpty() ? null : route.getSegments().getLast().getArrivalDirection();
        LocoRouteCompiler.CompileResult result = LocoRouteCompiler.compile(level, start, target, incoming, null);
        if (!result.success()) {
            message(player, Component.literal(result.error()));
            return;
        }

        route.add(LocoRouteNode.fromBlockPos(target));
        route.getSegments().add(result.segment());
        route.beginAppending();
        route.setDimension(level.dimension().location().toString());
        saveRoute(route, stack);
        message(player, Component.translatable("item.littlelogistics.locomotive_route.added", target.getX(), target.getY(), target.getZ()));
    }

    private static void completeRoute(Level level, ItemStack stack, LocoRoute route, @Nullable Player player) {
        if (route.size() < 2 || route.getSegments().size() != route.size() - 1) {
            message(player, Component.literal("Add at least two connected waypoints before completing a locomotive route."));
            return;
        }
        LocoRouteSegment firstSegment = route.getSegments().getFirst();
        LocoRouteSegment previousSegment = route.getSegments().getLast();
        LocoRouteCompiler.CompileResult result = LocoRouteCompiler.compile(level,
            route.getLast().toBlockPos(), route.getFirst().toBlockPos(),
            previousSegment.getArrivalDirection(), firstSegment.getStartIncomingDirection());
        if (!result.success()) {
            message(player, Component.literal(result.error()));
            return;
        }
        route.getSegments().add(result.segment());
        route.markComplete();
        route.setDimension(level.dimension().location().toString());
        saveRoute(route, stack);
        message(player, Component.literal("Locomotive route compiled."));
    }

    /** Replaces the selected segment with two compiled segments after the second editor click. */
    private static void commitSegmentInsertion(Level level, ItemStack stack, LocoRoute route, BlockPos target,
                                               @Nullable Player player) {
        int segmentIndex = route.getNextInsertionIndex() - 1;
        if (segmentIndex < 0 || segmentIndex >= route.getSegments().size()) {
            message(player, Component.literal("The selected locomotive route segment is no longer valid."));
            return;
        }
        LocoRouteSegment original = route.getSegments().get(segmentIndex);
        BlockPos start = route.get(segmentIndex).toBlockPos();
        BlockPos end = route.get((segmentIndex + 1) % route.size()).toBlockPos();
        LocoRouteCompiler.ReplacementCompileResult result = LocoRouteCompiler.compileReplacement(
            level, start, target, end, original);
        if (!result.success()) {
            message(player, Component.literal(result.error()));
            return;
        }

        route.add(segmentIndex + 1, LocoRouteNode.fromBlockPos(target));
        route.getSegments().set(segmentIndex, result.before());
        route.getSegments().add(segmentIndex + 1, result.after());
        route.markComplete();
        saveRoute(route, stack);
        message(player, Component.translatable("item.littlelogistics.locomotive_route.added", target.getX(), target.getY(), target.getZ()));
    }

    private static void removeNode(Level level, ItemStack stack, LocoRoute route, int index, @Nullable Player player) {
        BlockPos removed = route.get(index).toBlockPos();
        if (route.size() == 1) {
            route.clear();
            route.setSegments(List.of());
            saveRoute(route, stack);
            message(player, Component.translatable("item.littlelogistics.locomotive_route.removed", removed.getX(), removed.getY(), removed.getZ()));
            return;
        }

        if (route.isComplete() && route.size() > 2) {
            int segmentCount = route.getSegments().size();
            int previous = Math.floorMod(index - 1, segmentCount);
            int following = index;
            LocoRouteSegment merged = merge(route.getSegments().get(previous), route.getSegments().get(following));
            if (merged == null) {
                message(player, Component.literal("The compiled segment directions no longer connect; route was not changed."));
                return;
            }
            if (index == 0) {
                route.getSegments().set(segmentCount - 1, merged);
                route.getSegments().remove(0);
            } else {
                route.getSegments().set(previous, merged);
                route.getSegments().remove(following);
            }
            route.remove(index);
        } else if (route.isComplete()) {
            // A one-way draft is the only meaningful representation after reducing a loop to one node.
            route.remove(index);
            route.setSegments(List.of());
            route.beginAppending();
        } else {
            if (!removeOpenNode(route, index)) {
                message(player, Component.literal("The compiled segment directions no longer connect; route was not changed."));
                return;
            }
        }

        route.setDimension(level.dimension().location().toString());
        saveRoute(route, stack);
        message(player, Component.translatable("item.littlelogistics.locomotive_route.removed", removed.getX(), removed.getY(), removed.getZ()));
    }

    private static boolean removeOpenNode(LocoRoute route, int index) {
        int size = route.size();
        if (index == 0) {
            route.remove(0);
            if (!route.getSegments().isEmpty()) route.getSegments().remove(0);
        } else if (index == size - 1) {
            route.remove(index);
            if (!route.getSegments().isEmpty()) route.getSegments().remove(route.getSegments().size() - 1);
        } else {
            LocoRouteSegment merged = merge(route.getSegments().get(index - 1), route.getSegments().get(index));
            if (merged == null) return false;
            route.getSegments().set(index - 1, merged);
            route.getSegments().remove(index);
            route.remove(index);
        }
        route.beginAppending();
        return true;
    }

    @Nullable
    private static LocoRouteSegment merge(LocoRouteSegment before, LocoRouteSegment after) {
        if (before.getArrivalDirection() != after.getStartIncomingDirection()) return null;
        List<LocoRouteStep> steps = new ArrayList<>(before.getSteps());
        steps.addAll(after.getSteps());
        return new LocoRouteSegment(steps, after.getArrivalDirection());
    }

    public static OptionalInt findSegmentAt(LocoRoute route, BlockPos pos) {
        for (int segmentIndex = 0; segmentIndex < route.getSegments().size(); segmentIndex++) {
            for (LocoRouteStep step : route.getSegments().get(segmentIndex).getSteps()) {
                if (step.railPos().equals(pos)) return OptionalInt.of(segmentIndex);
            }
        }
        return OptionalInt.empty();
    }

    private static int findNodeIndex(LocoRoute route, BlockPos target) {
        for (int index = 0; index < route.size(); index++) {
            if (route.get(index).isAt(target)) return index;
        }
        return -1;
    }

    private static void message(@Nullable Player player, Component message) {
        if (player != null) player.displayClientMessage(message, true);
    }

    public static void saveRoute(LocoRoute route, ItemStack stack) {
        if (route.isEmpty()) stack.remove(ModDataComponents.LOCO_ROUTE);
        else stack.set(ModDataComponents.LOCO_ROUTE, route);
    }

    public static LocoRoute getRoute(ItemStack stack) {
        LocoRoute route = stack.get(ModDataComponents.LOCO_ROUTE);
        return route != null ? route.copy() : new LocoRoute();
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("item.littlelogistics.locomotive_route.description"));
        tooltip.add(Component.translatable("item.littlelogistics.locomotive_route.num_nodes", getRoute(stack).size())
            .setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
        if (getRoute(stack).isInProgress()) {
            tooltip.add(Component.translatable("item.littlelogistics.locomotive_route.in_progress")
                .setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)));
        }
    }
}
