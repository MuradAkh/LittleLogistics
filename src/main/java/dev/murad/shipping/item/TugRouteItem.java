package dev.murad.shipping.item;

import dev.murad.shipping.ShippingConfig;
import dev.murad.shipping.setup.ModDataComponents;
import dev.murad.shipping.util.TugRoute;
import dev.murad.shipping.util.TugRouteCompiler;
import dev.murad.shipping.util.TugRouteNode;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public class TugRouteItem extends Item {
    public TugRouteItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!world.isClientSide) {
            if (player.isShiftKeyDown()) {
                return InteractionResultHolder.pass(stack);
            } else {
                Optional<BlockPos> anchor = getTargetedWaypoint(world, player);
                if (anchor.isEmpty()) {
                    player.displayClientMessage(Component.literal("Point at navigable water to place a tug route waypoint."), true);
                } else {
                    editRouteAt(world, stack, anchor.get(), player);
                }
            }
        }

        return InteractionResultHolder.pass(stack);
    }

    /**
     * Gets the navigable-water block under the player's crosshair. The vanilla POV raycast
     * uses the player's normal block interaction range.
     */
    public static Optional<BlockPos> getTargetedWaypoint(Level level, Player player) {
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (hit.getType() != BlockHitResult.Type.BLOCK) {
            return Optional.empty();
        }

        BlockPos pos = hit.getBlockPos();
        return TugRouteCompiler.isNavigableWaypoint(level, pos) ? Optional.of(pos) : Optional.empty();
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("item.littlelogistics.tug_route.description"));
        tooltip.add(Component.translatable("item.littlelogistics.tug_route.num_nodes", getRoute(stack).size())
            .setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
    }

    public static TugRoute getRoute(ItemStack itemStack) {
        TugRoute route = itemStack.get(ModDataComponents.TUG_ROUTE);
        return route != null ? route.copy() : new TugRoute();
    }

    public static boolean popRoute(Level level, ItemStack itemStack) {
        TugRoute route = getRoute(itemStack);
        if (route.isEmpty()) {
            return false;
        }
        route.remove(route.size() - 1);
        route.beginAppending();
        return compileOpenAndSave(level, itemStack, route, null);
    }

    public static boolean tryRemoveSpecific(Level level, ItemStack itemStack, BlockPos pos) {
        TugRoute route = getRoute(itemStack);
        if (route.isEmpty()) {
            return false;
        }

        int index = findNodeIndex(route, pos);
        if (index < 0) {
            return false;
        }

        route.remove(index);
        if (route.isEmpty()) {
            saveRoute(route, itemStack);
            return true;
        }

        if (route.isComplete() && route.size() >= 2) {
            return compileAndSave(level, itemStack, route, null);
        }

        int nextIndex = route.isInProgress() ? route.getNextInsertionIndex() : route.size();
        if (index < nextIndex) {
            nextIndex--;
        }
        route.beginInsertion(nextIndex);
        return compileOpenAndSave(level, itemStack, route, null);
    }

    public static boolean pushRoute(Level level, ItemStack itemStack, BlockPos pos, @Nullable Player player) {
        TugRoute route = getRoute(itemStack);
        route.beginAppending();
        route.add(TugRouteNode.fromBlockPos(pos));
        route.beginAppending();
        return compileOpenAndSave(level, itemStack, route, player);
    }

    private static void editRouteAt(Level level, ItemStack itemStack, BlockPos pos, Player player) {
        TugRoute route = getRoute(itemStack);
        int nodeIndex = findNodeIndex(route, pos);

        if (route.isInProgress()) {
            if (nodeIndex == 0 && !route.isInserting() && route.getNextInsertionIndex() == route.size() && route.size() >= 2) {
                completeRoute(level, itemStack, route, player);
            } else if (nodeIndex >= 0) {
                tryRemoveSpecific(level, itemStack, pos);
            } else if (route.isInserting()) {
                route.add(route.getNextInsertionIndex(), TugRouteNode.fromBlockPos(pos));
                compileAndSave(level, itemStack, route, player);
            } else {
                route.add(TugRouteNode.fromBlockPos(pos));
                route.beginAppending();
                compileOpenAndSave(level, itemStack, route, player);
            }
            return;
        }

        if (route.isComplete()) {
            if (nodeIndex >= 0) {
                tryRemoveSpecific(level, itemStack, pos);
                return;
            }

            OptionalInt segment = findSegmentAt(route, pos);
            if (segment.isPresent()) {
                route.beginInsertion(segment.getAsInt() + 1);
                saveRoute(route, itemStack);
            }
            return;
        }

        route.add(TugRouteNode.fromBlockPos(pos));
        route.beginAppending();
        compileOpenAndSave(level, itemStack, route, player);
    }

    private static boolean completeRoute(Level level, ItemStack itemStack, TugRoute route, Player player) {
        if (route.size() < 2) {
            player.displayClientMessage(Component.literal("Add at least two waypoints before completing a tug route."), true);
            return false;
        }
        return compileAndSave(level, itemStack, route, player);
    }

    public static OptionalInt findSegmentAt(TugRoute route, BlockPos pos) {
        if (!route.isComplete()) {
            return OptionalInt.empty();
        }

        for (int segmentIndex = 0; segmentIndex < route.getSegments().size(); segmentIndex++) {
            for (var point : route.getSegments().get(segmentIndex).getPoints()) {
                if (point.toBlockPos().equals(pos)) {
                    return OptionalInt.of(segmentIndex);
                }
            }
        }
        return OptionalInt.empty();
    }

    private static int findNodeIndex(TugRoute route, BlockPos pos) {
        for (int i = 0; i < route.size(); i++) {
            if (route.get(i).isAt(pos)) {
                return i;
            }
        }
        return -1;
    }

    public static boolean compileAndSave(Level level, ItemStack itemStack, TugRoute route, @Nullable Player player) {
        if (route.isEmpty()) {
            itemStack.remove(ModDataComponents.TUG_ROUTE);
            return true;
        }

        TugRouteCompiler.CompileResult result = TugRouteCompiler.compile(
            level,
            route,
            ShippingConfig.Server.TUG_ROUTE_MAX_SEGMENT_LENGTH.get()
        );

        if (!result.success()) {
            if (player != null && result.error() != null) {
                player.displayClientMessage(Component.literal(result.error()), true);
            }
            return false;
        }

        saveRoute(result.route(), itemStack);
        return true;
    }

    public static boolean compileOpenAndSave(Level level, ItemStack itemStack, TugRoute route, @Nullable Player player) {
        if (route.isEmpty()) {
            itemStack.remove(ModDataComponents.TUG_ROUTE);
            return true;
        }

        TugRouteCompiler.CompileResult result = TugRouteCompiler.compileOpen(
            level,
            route,
            ShippingConfig.Server.TUG_ROUTE_MAX_SEGMENT_LENGTH.get()
        );
        if (!result.success()) {
            if (player != null && result.error() != null) {
                player.displayClientMessage(Component.literal(result.error()), true);
            }
            return false;
        }

        saveRoute(result.route(), itemStack);
        return true;
    }

    public static void saveRoute(TugRoute route, ItemStack itemStack) {
        if (route.isEmpty()) {
            itemStack.remove(ModDataComponents.TUG_ROUTE);
        } else {
            itemStack.set(ModDataComponents.TUG_ROUTE, route);
        }
    }
}
