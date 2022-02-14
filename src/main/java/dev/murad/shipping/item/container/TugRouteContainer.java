package dev.murad.shipping.item.container;

import dev.murad.shipping.entity.accessor.TugRouteScreenDataAccessor;
import dev.murad.shipping.setup.ModContainerTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TugRouteContainer extends Container {
    private static final Logger LOGGER = LogManager.getLogger(TugRouteContainer.class);

    private boolean isOffHand;
    private ItemStack itemStack;

    public TugRouteContainer(int windowId, World level, TugRouteScreenDataAccessor data, PlayerInventory playerInventory, PlayerEntity player) {
        super(ModContainerTypes.TUG_ROUTE_CONTAINER.get(), windowId);

        this.isOffHand = data.isOffHand();
        this.itemStack = player.getItemInHand(isOffHand ? Hand.OFF_HAND : Hand.MAIN_HAND);
        LOGGER.debug("Got item stack {} in {}hand", itemStack.toString(), isOffHand ? "off" : "main");
    }

    public boolean isOffHand() {
        return isOffHand;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    @Override
    public boolean stillValid(PlayerEntity player) {
        return true;
    }
}
