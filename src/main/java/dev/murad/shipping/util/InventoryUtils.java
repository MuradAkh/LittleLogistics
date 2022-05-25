package dev.murad.shipping.util;

import dev.murad.shipping.entity.custom.vessel.tug.AbstractTugEntity;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.CapabilityItemHandler;
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
                boolean validSlot = e.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
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

    public static int findSlotFotItem(Container target, ItemStack itemStack) {
        for (int i = 0; i < target.getContainerSize(); i++) {
            ItemStack stack = target.getItem(i);
            if(stack.isEmpty() || stack.getItem().equals(Items.AIR)){
                return i;
            }
            else if (canMergeItems(stack, itemStack)) {
                return i;
            }
        }

        return -1;
    }

    public static boolean canMergeItems(ItemStack p_145894_0_, ItemStack p_145894_1_) {
        if (p_145894_0_.getItem() != p_145894_1_.getItem()) {
            return false;
        } else if (p_145894_0_.getDamageValue() != p_145894_1_.getDamageValue()) {
            return false;
        } else if (p_145894_0_.getCount() > p_145894_0_.getMaxStackSize()) {
            return false;
        } else {
            return ItemStack.tagMatches(p_145894_0_, p_145894_1_);
        }
    }

    @Nullable
    public static IEnergyStorage getEnergyCapabilityInSlot(int slot, ItemStackHandler handler) {
        ItemStack stack = handler.getStackInSlot(slot);
        if (!stack.isEmpty()) {
            LazyOptional<IEnergyStorage> capabilityLazyOpt = stack.getCapability(CapabilityEnergy.ENERGY);
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
