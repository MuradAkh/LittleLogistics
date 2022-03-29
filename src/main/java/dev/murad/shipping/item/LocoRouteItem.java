package dev.murad.shipping.item;

import dev.murad.shipping.util.*;
import lombok.extern.log4j.Log4j2;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.BaseRailBlock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Log4j2
public class LocoRouteItem extends Item {
    private static final Logger LOGGER = LogManager.getLogger(LocoRouteItem.class);

    private static final String ROUTE_NBT = "route";

    public LocoRouteItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        if(pContext.getLevel().isClientSide) return InteractionResult.SUCCESS;

        // item used on block
        ItemStack stack = pContext.getItemInHand();
        if (stack.getItem() == this) {
            BlockPos target = pContext.getClickedPos();

            LocoRoute route = getRoute(stack);

            boolean removed = route.removeIf(n -> n.getX() == target.getX() && n.getY() == target.getY() && n.getZ() == target.getZ());
            if (removed) {
                // removed. target block doesn't have to be a rail block for removal
                // todo: remove log
                log.info("REMOVED " + target.toString());
            } else if (pContext.getLevel().getBlockState(target).getBlock() instanceof BaseRailBlock) {
                // blockpos should be a railtype, either our custom rail or vanilla.
                // Though for pathfinding purposes, it is not guaranteed to be a rail, as the
                // world can change

                // add
                route.add(LocoRouteNode.fromBlocKPos(target));
                log.info("ADDED " + target.toString());
            }

            // save route
            saveRoute(stack, route);
            log.info("SAVED ROUTE " + route.size());
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

}
