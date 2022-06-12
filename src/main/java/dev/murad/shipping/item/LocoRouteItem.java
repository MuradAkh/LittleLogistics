package dev.murad.shipping.item;

import dev.murad.shipping.util.LocoRoute;
import dev.murad.shipping.util.LocoRouteNode;
import lombok.extern.log4j.Log4j2;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Log4j2
public class LocoRouteItem extends Item {
    private static final String LEGACY_ROUTE_NBT = "route";
    private static final String ROUTE_NBT = "routeMap";

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
            LocoRoute route = getRoute(stack, pContext.getLevel().dimension());
            Player player = pContext.getPlayer();

            // target block
            Block targetBlock = pContext.getLevel().getBlockState(target).getBlock();
            boolean shouldCheckAboveOnRemove = !(targetBlock instanceof BaseRailBlock);

            if (!removeAndDisplay(player, route, target) && (!shouldCheckAboveOnRemove || !removeAndDisplay(player, route, target.above()))) {
                addAndDisplay(player, route, target, pContext.getLevel());
            }

            // save route
            saveRoute(stack, route, pContext.getLevel().dimension());
            return InteractionResult.SUCCESS;
        } else {
            return InteractionResult.PASS;
        }
    }

    private void saveRoute(ItemStack stack, LocoRoute route, ResourceKey<Level> dimension) {
        Map<ResourceKey<Level>, LocoRoute> routes = getRoutes(stack, dimension);
        routes.put(dimension, route);
        stack.getOrCreateTag().put(ROUTE_NBT, nbtFromMap(routes));
    }


    public static LocoRoute getRoute(ItemStack stack, ResourceKey<Level> level) {
        // check if it has nbt
        if (stack.getTag() != null) {
            if(stack.getTag().contains(LEGACY_ROUTE_NBT)) {
                return LocoRoute.fromNBT(stack.getTag().getCompound(LEGACY_ROUTE_NBT));
            } else  {
                return mapFromNbt(stack.getTag().getCompound(ROUTE_NBT)).getOrDefault(level, new LocoRoute());
            }
        }
        // empty route
        return new LocoRoute();
    }

    public static Map<ResourceKey<Level>, LocoRoute> getRoutes(ItemStack stack, ResourceKey<Level> defaultDimension) {
        // check if it has nbt
        if (stack.getTag() != null) {
            if(stack.getTag().contains(LEGACY_ROUTE_NBT)) {
                return Map.of(defaultDimension, LocoRoute.fromNBT(stack.getTag().getCompound(LEGACY_ROUTE_NBT)));
            } else if (stack.getTag().contains(ROUTE_NBT))  {
                return mapFromNbt(stack.getTag().getCompound(ROUTE_NBT));
            }
        }
        // empty route
        return new HashMap<>();
    }

    private static Map<ResourceKey<Level>, LocoRoute> mapFromNbt(CompoundTag compoundTag){
        Map<ResourceKey<Level>, LocoRoute> map = new HashMap<>();
        for (var key: compoundTag.getAllKeys()){
            map.put(ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(key)), LocoRoute.fromNBT(compoundTag.getCompound(key)));
        }
        return map;
    }

    private static CompoundTag nbtFromMap(Map<ResourceKey<Level>, LocoRoute> map){
        CompoundTag tag = new CompoundTag();
        map.forEach((levelResourceKey, locoRouteNodes) -> {
            tag.put(levelResourceKey.location().toString(), locoRouteNodes.toNBT());
        });
        return tag;
    }


    public static boolean isEmpty(ItemStack stack) {
        // check if it has nbt
        if (stack.getTag() != null) {
            if(stack.getTag().contains(LEGACY_ROUTE_NBT)) {
                return LocoRoute.fromNBT(stack.getTag().getCompound(LEGACY_ROUTE_NBT)).isEmpty();
            }else if (stack.getTag().contains(ROUTE_NBT)){
                return mapFromNbt(stack.getTag().getCompound(ROUTE_NBT)).entrySet()
                        .stream()
                        .reduce(0, (acc, curr) -> acc + curr.getValue().size(), Integer::sum) == 0;
            }
        }
        // empty route
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        if(worldIn == null){ // FIXME
            return;
        }
        tooltip.add(new TranslatableComponent("item.littlelogistics.locomotive_route.description"));
        tooltip.add(
                new TranslatableComponent("item.littlelogistics.locomotive_route.num_nodes", getRoute(stack, worldIn.dimension()).size())
                        .setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
    }
}
