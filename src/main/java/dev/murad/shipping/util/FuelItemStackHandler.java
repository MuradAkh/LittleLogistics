package dev.murad.shipping.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nonnull;

/**
 * Implementation of ItemStackHandler that doesn't change size when loaded from NBT.
 * Used in Steam and Locomotive tugs.
 */
public class FuelItemStackHandler extends ItemStackHandler {
    public FuelItemStackHandler() {
        super(1);
    }

    /**
     * Consume an item of fuel
     * @return number of base ticks the fuel burns for. This will be multiplied by the fuel multiplier in config
     */
    public int tryConsumeFuel() {
        var stack = getStackInSlot(0);
        var burnTime = stack.getBurnTime(null);

        if (burnTime > 0) {
            // shrink the stack and replace with byproducts (if exists)
            var byproduct = stack.getCraftingRemainingItem();
            stack.shrink(1);

            if (stack.isEmpty()) {
                // replace stack with byproduct
                // if somehow a stackable item has a byproduct, then we should call the police
                setStackInSlot(0, byproduct);
            }
        }

        return burnTime;
    }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        return FurnaceBlockEntity.isFuel(stack);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        var tag = super.serializeNBT(provider);
        tag.remove("Size");
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        nbt.remove("Size");
        super.deserializeNBT(provider, nbt);
    }
}