package dev.murad.shipping.util;

import lombok.NonNull;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.*;

public class InventoryUtils {

    public static boolean mayMoveIntoInventory(Container target, Container source) {
        if (source.isEmpty()){
            return false;
        }

        HashMap<Item, List<ItemStack>> map = new HashMap<>();
        List<Integer> airList = new ArrayList<>();
        for (int i = 0; i < target.getContainerSize(); i++) {
            ItemStack stack = target.getItem(i);
            if((stack.isEmpty() || stack.getItem().equals(Items.AIR)) && target.canPlaceItem(i, stack)){
                airList.add(i);
            }
            else if (stack.getMaxStackSize() != stack.getCount() && target.canPlaceItem(i, stack)) {
                if (map.containsKey(stack.getItem())) {
                    map.get(stack.getItem()).add(stack);
                } else {
                    map.put(stack.getItem(), new ArrayList<>(Collections.singleton(stack)));
                }
            }
        }

        for (int i = 0; i < source.getContainerSize(); i++) {
            ItemStack stack = source.getItem(i);
            if (!stack.isEmpty() && map.containsKey(stack.getItem())) {
                for (ItemStack targetStack : map.get(stack.getItem())){
                    if (canMergeItems(targetStack, stack))
                        return true;
                }
            } else if (!airList.isEmpty() && target instanceof Entity){
                Entity e = (Entity) target;
                boolean validSlot = e.getCapability(ForgeCapabilities.ITEM_HANDLER)
                        .map(itemHandler -> airList.stream()
                                .map(j -> itemHandler.isItemValid(j, stack))
                                .reduce(false, Boolean::logicalOr)).orElse(true);
                if(validSlot) {
                    return true;
                }
            } else if (!airList.isEmpty()){
                return true;
            }
        }
        return false;
    }

    public static boolean isEmpty(ItemStackHandler itemHandler) {
        for (int i = 0; i < itemHandler.getSlots(); i++){
            if(!itemHandler.getStackInSlot(i).isEmpty() && !itemHandler.getStackInSlot(i).getItem().equals(Items.AIR)){
                return false;
            }
        }
        return true;
    }

    @NonNull
    public static ItemStack moveItemStackIntoHandler(ItemStackHandler handler, @NonNull ItemStack stack) {
        var slots = handler.getSlots();
        for (int i = 0; i < slots && !stack.isEmpty(); i++) {
            stack = handler.insertItem(i, stack, false);
        }
        return stack;
    }

    public static boolean canMergeItems(ItemStack stack1, ItemStack stack2) {
        if (stack1.getItem() != stack2.getItem()) {
            return false;
        } else if (stack1.getDamageValue() != stack2.getDamageValue()) {
            return false;
        } else if (stack1.getCount() > stack1.getMaxStackSize()) {
            return false;
        } else {
            return ItemStack.isSameItemSameTags(stack1, stack2);
        }
    }

    @Nullable
    public static IEnergyStorage getEnergyCapabilityInSlot(int slot, ItemStackHandler handler) {
        ItemStack stack = handler.getStackInSlot(slot);
        if (!stack.isEmpty()) {
            LazyOptional<IEnergyStorage> capabilityLazyOpt = stack.getCapability(ForgeCapabilities.ENERGY);
            if (capabilityLazyOpt.isPresent()) {
                Optional<IEnergyStorage> capabilityOpt = capabilityLazyOpt.resolve();
                if (capabilityOpt.isPresent()) {
                    return capabilityOpt.get();
                }
            }
        }
        return null;
    }
}
