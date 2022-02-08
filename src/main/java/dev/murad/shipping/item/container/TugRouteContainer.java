package dev.murad.shipping.item.container;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;

public class TugRouteContainer extends Container {
    private ItemStack itemStack;

    protected TugRouteContainer(@Nullable ContainerType<?> containerType, int windowsId,
                                PlayerInventory playerInventory, PlayerEntity player) {
        super(containerType, windowsId);
        this.itemStack = itemStack;
    }

    @Override
    public boolean stillValid(PlayerEntity player) {
        return true;
    }
}
