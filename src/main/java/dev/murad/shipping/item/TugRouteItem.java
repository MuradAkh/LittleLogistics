package dev.murad.shipping.item;

import dev.murad.shipping.entity.accessor.TugRouteScreenDataAccessor;
import dev.murad.shipping.item.container.TugRouteContainer;
import dev.murad.shipping.util.LegacyTugRouteUtil;
import dev.murad.shipping.util.TugRoute;
import dev.murad.shipping.util.TugRouteNode;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.network.NetworkHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class TugRouteItem extends Item {
    private static final Logger LOGGER = LogManager.getLogger(TugRouteItem.class);

    private static final String ROUTE_NBT = "route";
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
            public AbstractContainerMenu createMenu(int i, Inventory playerInventory, Player Player) {
                return new TugRouteContainer(i, Player.level, getDataAccessor(Player, hand), playerInventory, Player);
            }
        };
    }

    public TugRouteScreenDataAccessor getDataAccessor(Player entity, InteractionHand hand) {
        return new TugRouteScreenDataAccessor.Builder(entity.getId())
                .withOffHand(hand == InteractionHand.OFF_HAND.OFF_HAND)
                .build();
    }

    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        if(!player.level.isClientSide){
            if (player.isShiftKeyDown()) {
                NetworkHooks.openGui((ServerPlayer) player, createContainerProvider(hand), getDataAccessor(player, hand)::write);
            } else {
                int x = (int) Math.floor(player.getX());
                int z = (int) Math.floor(player.getZ());
                if (!tryRemoveSpecific(itemstack, x, z)) {
                    player.displayClientMessage(Component.translatable("item.littlelogistics.tug_route.added", x, z), false);
                    pushRoute(itemstack, x, z);
                } else {
                    player.displayClientMessage(Component.translatable("item.littlelogistics.tug_route.removed", x, z), false);
                }
            }
        }

        return InteractionResultHolder.pass(itemstack);
    }

    @Override
    public void verifyTagAfterLoad(@Nonnull CompoundTag nbt) {
        super.verifyTagAfterLoad(nbt);
        // convert old nbt format of route: "" into compound format
        // Precond: nbt is non-null, and nbt.tag is nonnull type 10
        CompoundTag tag = nbt.getCompound("tag");
        if (tag.contains(ROUTE_NBT, 8)) {
            LOGGER.info("Found legacy tug route tag, replacing now");
            String routeString = tag.getString(ROUTE_NBT);
            List<Vec2> legacyRoute = LegacyTugRouteUtil.parseLegacyRouteString(routeString);
            TugRoute route = LegacyTugRouteUtil.convertLegacyRoute(legacyRoute);
            tag.put(ROUTE_NBT, route.toNBT());
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        tooltip.add(Component.translatable("item.littlelogistics.tug_route.description"));
        tooltip.add(
                Component.translatable("item.littlelogistics.tug_route.num_nodes", getRoute(stack).size())
                        .setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
    }

    public static TugRoute getRoute(ItemStack itemStack) {
        CompoundTag nbt = getTag(itemStack);
        if(nbt == null || !nbt.contains(ROUTE_NBT, 10)) {
            // don't write tag from client side, just return empty route
            return new TugRoute();
        }

        return TugRoute.fromNBT(nbt.getCompound(ROUTE_NBT));
    }

    public static boolean popRoute(ItemStack itemStack) {
        TugRoute route = getRoute(itemStack);
        if(route.size() == 0) {
            return false;
        }
        route.remove(route.size() - 1);
        saveRoute(route, itemStack);
        return true;
    }

    public static boolean tryRemoveSpecific(ItemStack itemStack, int x, int z) {
        TugRoute route = getRoute(itemStack);
        if(route.size() == 0) {
            return false;
        }
        boolean removed = route.removeIf(v -> v.getX() == x && v.getZ() == z);
        saveRoute(route, itemStack);
        return removed;
    }

    public static void pushRoute(ItemStack itemStack, int x, int y) {
        TugRoute route = getRoute(itemStack);
        route.add(new TugRouteNode(x, y));
        saveRoute(route, itemStack);
    }

    // should only be called server side
    public static void saveRoute(TugRoute route, ItemStack itemStack){
        CompoundTag nbt = getTag(itemStack);
        if (nbt == null) {
            nbt = new CompoundTag();
            itemStack.setTag(nbt);
        }
        nbt.put(ROUTE_NBT, route.toNBT());
    }

    @Nullable
    private static CompoundTag getTag(ItemStack stack)  {
        return stack.getTag();
    }
}
