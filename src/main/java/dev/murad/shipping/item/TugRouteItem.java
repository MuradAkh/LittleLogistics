package dev.murad.shipping.item;

import dev.murad.shipping.ShippingConfig;
import dev.murad.shipping.entity.accessor.TugRouteScreenDataAccessor;
import dev.murad.shipping.item.container.TugRouteContainer;
import dev.murad.shipping.setup.ModDataComponents;
import dev.murad.shipping.util.TugRoute;
import dev.murad.shipping.util.TugRouteCompiler;
import dev.murad.shipping.util.TugRouteNode;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class TugRouteItem extends Item {
    public TugRouteItem(Properties properties) {
        super(properties);
    }

    protected MenuProvider createContainerProvider(InteractionHand hand) {
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("screen.littlelogistics.tug_route");
            }

            @Nullable
            @Override
            public AbstractContainerMenu createMenu(int i, Inventory playerInventory, Player player) {
                return new TugRouteContainer(i, player.level(), getDataAccessor(player, hand), playerInventory, player);
            }
        };
    }

    public TugRouteScreenDataAccessor getDataAccessor(Player entity, InteractionHand hand) {
        return new TugRouteScreenDataAccessor.Builder(entity.getId())
            .withOffHand(hand == InteractionHand.OFF_HAND)
            .build();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!world.isClientSide) {
            if (player.isShiftKeyDown()) {
                ((ServerPlayer) player).openMenu(createContainerProvider(hand), getDataAccessor(player, hand)::write);
            } else {
                Optional<BlockPos> anchor = getTargetedWaypoint(world, player);
                if (anchor.isEmpty()) {
                    player.displayClientMessage(Component.literal("Point at navigable water to place a tug route waypoint."), true);
                } else if (!tryRemoveSpecific(world, stack, anchor.get())) {
                    pushRoute(world, stack, anchor.get(), player);
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
        return compileAndSave(level, itemStack, route, null);
    }

    public static boolean tryRemoveSpecific(Level level, ItemStack itemStack, BlockPos pos) {
        TugRoute route = getRoute(itemStack);
        if (route.isEmpty()) {
            return false;
        }

        boolean removed = route.removeIf(node -> node.isAt(pos));
        return removed && compileAndSave(level, itemStack, route, null);
    }

    public static boolean pushRoute(Level level, ItemStack itemStack, BlockPos pos, @Nullable Player player) {
        TugRoute route = getRoute(itemStack);
        route.add(TugRouteNode.fromBlockPos(pos));
        return compileAndSave(level, itemStack, route, player);
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

    public static void saveRoute(TugRoute route, ItemStack itemStack) {
        if (route.isEmpty()) {
            itemStack.remove(ModDataComponents.TUG_ROUTE);
        } else {
            itemStack.set(ModDataComponents.TUG_ROUTE, route);
        }
    }
}
