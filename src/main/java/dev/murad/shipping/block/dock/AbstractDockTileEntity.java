package dev.murad.shipping.block.dock;

import dev.murad.shipping.entity.custom.tug.TugEntity;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.tileentity.HopperTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraftforge.items.CapabilityItemHandler;

import java.util.*;


public abstract class AbstractDockTileEntity extends TileEntity {
    public AbstractDockTileEntity(TileEntityType<?> p_i48289_1_) {
        super(p_i48289_1_);
    }

    public abstract boolean holdVessel(IInventory vessel, Direction direction);

    protected static boolean mayMoveIntoInventory(IInventory target, IInventory source) {
        if (source.isEmpty()){
            return false;
        }

        HashMap<Item, List<ItemStack>> map = new HashMap<>();
        List<Integer> airList = new ArrayList<>();
        int init = target instanceof TugEntity ? 1 : 0;
        for (int i = init; i < target.getContainerSize(); i++) {
            ItemStack stack = target.getItem(i);
            if(stack.isEmpty() || stack.getItem().equals(Items.AIR)){
                if(!(target instanceof Entity)){
                    return true;
                }
                airList.add(i);
            }
            else if (stack.getMaxStackSize() != stack.getCount()) {
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
            } else if (!airList.isEmpty()){
                Entity e = (Entity) target;
                boolean validSlot = e.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
                        .map(itemHandler -> airList.stream()
                                .map(j -> itemHandler.isItemValid(j, stack))
                                .reduce(false, Boolean::logicalOr)).orElse(true);
                if(validSlot) {
                    return true;
                }
            }
        }
        return false;
    }

    public Optional<HopperTileEntity> getInsertHopper(){
        TileEntity mayBeHopper = this.level.getBlockEntity(this.getBlockPos().above());
        if (mayBeHopper instanceof HopperTileEntity) {
            return Optional.of((HopperTileEntity) mayBeHopper);
        }
        else return Optional.empty();
    }

    private static boolean canMergeItems(ItemStack p_145894_0_, ItemStack p_145894_1_) {
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

}
