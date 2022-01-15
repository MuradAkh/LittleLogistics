package dev.murad.shipping.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Pose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class TugRouteItem extends Item {
    private static final String ROUTE_NBT = "route";
    public TugRouteItem(Properties p_i48487_1_) {
        super(p_i48487_1_);
    }

    public ActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        if(!player.level.isClientSide){
            int x = (int) player.getX();
            int z = (int) player.getZ();
            if (!player.getPose().equals(Pose.CROUCHING)) {
                player.displayClientMessage(new StringTextComponent(MessageFormat.format("Added Route Node X:{0} Z:{1}", x, z)), false);
                pushRoute(itemstack, x, z);
            } else {
                player.displayClientMessage(new StringTextComponent("Removed last node from path"), false);
                popRoute(itemstack);
            }

        }

        return ActionResult.pass(itemstack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        tooltip.add(new TranslationTextComponent("item.shipping.tug_route.description"));
        tooltip.add(new StringTextComponent(formatRoute(getRoute(stack))));
    }

    public static List<Vector2f> getRoute(ItemStack itemStack){
        CompoundNBT nbt = nbt(itemStack);
        if(!nbt.contains(ROUTE_NBT)){
            nbt.putString(ROUTE_NBT, "");
        }

        return parseRoute(nbt.getString(ROUTE_NBT));
    }

    public static boolean popRoute(ItemStack itemStack){
        List<Vector2f> route = getRoute(itemStack);
        if(route.size() == 0) {
            return false;
        }
        route.remove(route.size() - 1);
        saveRoute(route, itemStack);
        return true;
    }

    public static void pushRoute(ItemStack itemStack, int x, int y){
        List<Vector2f> route = getRoute(itemStack);
        route.add(new Vector2f(x, y));
        saveRoute(route, itemStack);
    }

    private static void saveRoute(List<Vector2f> route, ItemStack itemStack){
        CompoundNBT nbt = nbt(itemStack);
        nbt.putString(ROUTE_NBT, serialiseRoute(route));
    }

    private static List<Vector2f> parseRoute(String route){
        if(route.equals("")){
            return new ArrayList<>();
        }

        return Arrays.stream(route.split(","))
                .map(string -> string.split(":"))
                .map(arr -> new Vector2f(Float.parseFloat(arr[0]), Float.parseFloat(arr[1])))
                .collect(Collectors.toList());

    }

    private static String serialiseRoute(List<Vector2f> route){
        return route
                .stream()
                .map(vector -> vector.x + ":" + vector.y)
                .reduce((acc, curr) -> acc + "," + curr)
                .orElse("");
    }

    private static String formatRoute(List<Vector2f> route){
        AtomicInteger index = new AtomicInteger();
        return route
                .stream()
                .map(vector -> String.format("Stop %d. X:%d, Y:%d", index.incrementAndGet(), (int) vector.x, (int) vector.y))
                .reduce((acc, curr) -> acc + "\n" + curr)
                .orElse("");
    }

    private static CompoundNBT nbt(ItemStack stack)  {
        if(stack.getTag() == null) {
            stack.setTag(new CompoundNBT());
        }
        return stack.getTag();
    }

}
