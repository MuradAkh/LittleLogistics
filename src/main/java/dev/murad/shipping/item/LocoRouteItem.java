package dev.murad.shipping.item;

import dev.murad.shipping.util.*;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Log4j2
public class LocoRouteItem extends Item {
    private static final String ROUTE_NBT = "route";

    public LocoRouteItem(Properties properties) {
        super(properties);
    }

    private boolean removeAndDisplay(@Nullable Player player, Level level, LocoRoutes route, BlockPos pos) {
        boolean removed = route.getNodesForDimension(level).removeIf(n -> n.isAt(pos));
        if (removed && player != null)
            player.displayClientMessage(new TranslatableComponent("item.littlelogistics.locomotive_route.removed",
                    pos.getX(), pos.getY(), pos.getZ()), false);
        return removed;
    }

    private void addAndDisplay(@Nullable Player player, Level level, LocoRoutes route, BlockPos pos) {
        if (level.getBlockState(pos).getBlock() instanceof BaseRailBlock) {
            // blockpos should be a railtype, either our custom rail or vanilla.
            // Though for pathfinding purposes, it is not guaranteed to be a rail, as the
            // world can change
            if (player != null)
                player.displayClientMessage(new TranslatableComponent("item.littlelogistics.locomotive_route.added",
                        pos.getX(), pos.getY(), pos.getZ()), false);

            // add
            route.getNodesForDimension(level).add(LocoRouteNode.fromBlocKPos(pos));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        if(pContext.getLevel().isClientSide) return InteractionResult.SUCCESS;

        // item used on block
        ItemStack stack = pContext.getItemInHand();
        if (stack.getItem() == this) {
            BlockPos target = pContext.getClickedPos();
            Level level = pContext.getLevel();
            LocoRoutes route = getRoute(stack, level);
            Player player = pContext.getPlayer();

            // target block
            Block targetBlock = pContext.getLevel().getBlockState(target).getBlock();
            boolean shouldCheckAboveOnRemove = !(targetBlock instanceof BaseRailBlock);

            if (!removeAndDisplay(player, level, route, target) && (!shouldCheckAboveOnRemove || !removeAndDisplay(player, level, route, target.above()))) {
                addAndDisplay(player, level, route, target);
            }

            // save route
            saveRoute(stack, route);
            return InteractionResult.SUCCESS;
        } else {
            return InteractionResult.PASS;
        }
    }

    private void saveRoute(ItemStack stack, LocoRoutes route) {
        route.cleanup();

        if (route.isEmpty()) {
            // remove tag from stack
            stack.setTag(null);
        }

        CompoundTag tag = stack.getOrCreateTag();
        tag.put(ROUTE_NBT, route.toNBT());
    }

    public static LocoRoutes getRoute(ItemStack stack, @Nullable Level defaultLevel) {
        // check if it has nbt
        if (stack.getTag() != null) {
            return LocoRoutes.fromNBT(stack.getTag().getCompound(ROUTE_NBT), defaultLevel);
        }
        // empty route
        return new LocoRoutes();
    }

    @NonNull
    public static Set<LocoRouteNode> getRouteInDimension(ItemStack stack, @NonNull Level level) {
        ResourceLocation key = level.dimension().location();
        return getRoute(stack, level).getNodes().getOrDefault(key, new HashSet<>());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, level, tooltip, flagIn);
        if(level == null){ // FIXME
            return;
        }
        tooltip.add(new TranslatableComponent("item.littlelogistics.locomotive_route.description"));
        tooltip.add(
                new TranslatableComponent("item.littlelogistics.locomotive_route.num_nodes", getRoute(stack, level).getNodesForDimension(level).size())
                        .setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
    }
}
