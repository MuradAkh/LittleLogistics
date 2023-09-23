package dev.murad.shipping.item.container;

import dev.murad.shipping.entity.accessor.TugRouteScreenDataAccessor;
import dev.murad.shipping.setup.ModMenuTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class TugRouteContainer extends AbstractContainerMenu {
    private static final Logger LOGGER = LogManager.getLogger(TugRouteContainer.class);

    private final boolean isOffHand;
    private final ItemStack itemStack;

    public TugRouteContainer(int windowId, Level level, TugRouteScreenDataAccessor data, Inventory playerInventory, Player player) {
        super(ModMenuTypes.TUG_ROUTE_CONTAINER.get(), windowId);

        this.isOffHand = data.isOffHand();
        this.itemStack = player.getItemInHand(isOffHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        LOGGER.debug("Got item stack {} in {}hand", itemStack.toString(), isOffHand ? "off" : "main");
    }

    public boolean isOffHand() {
        return isOffHand;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    /**
     * Tug route container has no inventory so this will never actually be called.
     */
    @Override
    @NotNull
    public ItemStack quickMoveStack(@NotNull Player player, int p_38942_) {
        //noinspection DataFlowIssue
        return null;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }
}
