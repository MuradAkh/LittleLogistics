package dev.murad.shipping.item;

import dev.murad.shipping.entity.accessor.TugRouteScreenDataAccessor;
import dev.murad.shipping.item.container.TugRouteContainer;
import dev.murad.shipping.util.LegacyTugRouteUtil;
import dev.murad.shipping.util.TugRoute;
import dev.murad.shipping.util.TugRouteNode;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TugRouteItem extends Item {
    private static final Logger LOGGER = LogManager.getLogger(TugRouteItem.class);

    private static final String ROUTE_NBT = "route";
    public TugRouteItem(Properties p_i48487_1_) {
        super(p_i48487_1_);
    }

    protected INamedContainerProvider createContainerProvider(Hand hand) {
        return new INamedContainerProvider() {
            @Override
            public ITextComponent getDisplayName() {
                return new TranslationTextComponent("screen.littlelogistics.tug_route");
            }

            @Nullable
            @Override
            public Container createMenu(int i, PlayerInventory playerInventory, PlayerEntity playerEntity) {
                return new TugRouteContainer(i, playerEntity.level, getDataAccessor(playerEntity, hand), playerInventory, playerEntity);
            }
        };
    }

    public TugRouteScreenDataAccessor getDataAccessor(PlayerEntity entity, Hand hand) {
        return new TugRouteScreenDataAccessor.Builder(entity.getId())
                .withOffHand(hand == Hand.OFF_HAND)
                .build();
    }

    public ActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        if(!player.level.isClientSide){
            if (player.isShiftKeyDown()) {
                NetworkHooks.openGui((ServerPlayerEntity) player, createContainerProvider(hand), getDataAccessor(player, hand)::write);
            } else {
                int x = (int) Math.floor(player.getX());
                int z = (int) Math.floor(player.getZ());
                if (!tryRemoveSpecific(itemstack, x, z)) {
                    player.displayClientMessage(new TranslationTextComponent("item.littlelogistics.tug_route.added", x, z), false);
                    pushRoute(itemstack, x, z);
                } else {
                    player.displayClientMessage(new TranslationTextComponent("item.littlelogistics.tug_route.removed", x, z), false);
                }
            }
        }

        return ActionResult.pass(itemstack);
    }

    @Override
    public boolean verifyTagAfterLoad(@Nonnull CompoundNBT nbt) {
        super.verifyTagAfterLoad(nbt);
        // convert old nbt format of route: "" into compound format
        // Precond: nbt is non-null, and nbt.tag is nonnull type 10
        CompoundNBT tag = nbt.getCompound("tag");
        if (tag.contains(ROUTE_NBT, 8)) {
            LOGGER.info("Found legacy tug route tag, replacing now");
            String routeString = tag.getString(ROUTE_NBT);
            List<Vector2f> legacyRoute = LegacyTugRouteUtil.parseLegacyRouteString(routeString);
            TugRoute route = LegacyTugRouteUtil.convertLegacyRoute(legacyRoute);
            tag.put(ROUTE_NBT, route.toNBT());
            return true;
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        tooltip.add(new TranslationTextComponent("item.littlelogistics.tug_route.description"));
        tooltip.add(
                new TranslationTextComponent("item.littlelogistics.tug_route.num_nodes", getRoute(stack).size())
                        .setStyle(Style.EMPTY.withColor(TextFormatting.YELLOW)));
    }

    public static TugRoute getRoute(ItemStack itemStack) {
        CompoundNBT nbt = getTag(itemStack);
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
        CompoundNBT nbt = getTag(itemStack);
        if (nbt == null) {
            nbt = new CompoundNBT();
            itemStack.setTag(nbt);
        }
        nbt.put(ROUTE_NBT, route.toNBT());
    }

    @Nullable
    private static CompoundNBT getTag(ItemStack stack)  {
        return stack.getTag();
    }
}
