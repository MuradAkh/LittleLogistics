package dev.murad.shipping.block.dock;

import dev.murad.shipping.entity.custom.tug.TugEntity;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.HopperTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;


public abstract class AbstractDockTileEntity extends TileEntity {
    public AbstractDockTileEntity(TileEntityType<?> p_i48289_1_) {
        super(p_i48289_1_);
    }

    public abstract boolean holdVessel(IInventory vessel, Direction direction);

    protected static boolean mayMoveIntoInventory(IInventory target, IInventory source) {
        HashMap<Item, List<ItemStack>> map = new HashMap<>();
        int init = target instanceof TugEntity ? 1 : 0;
        for (int i = init; i < target.getContainerSize() + init; i++) {
            ItemStack stack = target.getItem(i);
            if (stack.getMaxStackSize() != stack.getCount()) {
                if (map.containsKey(stack.getItem())) {
                    map.get(stack.getItem()).add(stack);
                } else {
                    map.put(stack.getItem(), Collections.singletonList(stack));
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

            }
        }
        return false;
    }

    public Optional<HopperTileEntity> getHopper(){
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
