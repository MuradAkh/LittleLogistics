package dev.murad.shipping.item;

import dev.murad.shipping.util.*;
import lombok.extern.log4j.Log4j2;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.List;

@Log4j2
public class LocoRouteItem extends Item {
    private static final String ROUTE_NBT = "route";

    public LocoRouteItem(Properties properties) {
        super(properties);
    }

    private boolean removeAndDisplay(@Nullable Player player, LocoRoute route, BlockPos pos) {
        boolean removed = route.removeIf(n -> n.isAt(pos));
        if (removed && player != null)
            player.displayClientMessage(new TranslatableComponent("item.littlelogistics.locomotive_route.removed",
                    pos.getX(), pos.getY(), pos.getZ()), false);
        return removed;
    }

    private void addAndDisplay(@Nullable Player player, LocoRoute route, BlockPos pos, Level level) {
        if (level.getBlockState(pos).getBlock() instanceof BaseRailBlock) {
            // blockpos should be a railtype, either our custom rail or vanilla.
            // Though for pathfinding purposes, it is not guaranteed to be a rail, as the
            // world can change
            if (player != null)
                player.displayClientMessage(new TranslatableComponent("item.littlelogistics.locomotive_route.added",
                        pos.getX(), pos.getY(), pos.getZ()), false);

            // add
            route.add(LocoRouteNode.fromBlocKPos(pos));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        if(pContext.getLevel().isClientSide) return InteractionResult.SUCCESS;

        // item used on block
        ItemStack stack = pContext.getItemInHand();
        if (stack.getItem() == this) {
            BlockPos target = pContext.getClickedPos();
            LocoRoute route = getRoute(stack);
            Player player = pContext.getPlayer();

            // target block
            Block targetBlock = pContext.getLevel().getBlockState(target).getBlock();
            boolean shouldCheckAboveOnRemove = !(targetBlock instanceof BaseRailBlock);

            if (!removeAndDisplay(player, route, target) && (!shouldCheckAboveOnRemove || !removeAndDisplay(player, route, target.above()))) {
                addAndDisplay(player, route, target, pContext.getLevel());
            }

            // save route
            saveRoute(stack, route);
            return InteractionResult.SUCCESS;
        } else {
            return InteractionResult.PASS;
        }
    }

    private void saveRoute(ItemStack stack, LocoRoute route) {
        if (route.isEmpty()) {
            // remove tag from stack
            stack.setTag(null);
        }

        CompoundTag tag = stack.getOrCreateTag();
        tag.put(ROUTE_NBT, route.toNBT());
    }

    public static LocoRoute getRoute(ItemStack stack) {
        // check if it has nbt
        if (stack.getTag() != null) {
            return LocoRoute.fromNBT(stack.getTag().getCompound(ROUTE_NBT));
        }
        // empty route
        return new LocoRoute();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        tooltip.add(new TranslatableComponent("item.littlelogistics.locomotive_route.description"));
        tooltip.add(
                new TranslatableComponent("item.littlelogistics.locomotive_route.num_nodes", getRoute(stack).size())
                        .setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
    }
}
