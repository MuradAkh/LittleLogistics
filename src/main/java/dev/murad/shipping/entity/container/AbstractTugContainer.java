package dev.murad.shipping.entity.container;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.accessor.DataAccessor;
import dev.murad.shipping.entity.custom.vessel.tug.AbstractTugEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public abstract class AbstractTugContainer<T extends DataAccessor> extends AbstractItemHandlerContainer {
    public static final ResourceLocation EMPTY_TUG_ROUTE = new ResourceLocation(ShippingMod.MOD_ID, "item/empty_tug_route");
    public static final ResourceLocation EMPTY_ENERGY = new ResourceLocation(ShippingMod.MOD_ID, "item/empty_energy");
    public static final ResourceLocation EMPTY_ATLAS_LOC = InventoryMenu.BLOCK_ATLAS;

    protected T data;
    protected AbstractTugEntity tugEntity;

    public AbstractTugContainer(@Nullable MenuType<?> containerType, int windowId, Level world, T data,
                                Inventory playerInventory, Player player) {
        super(containerType, windowId, playerInventory, player);
        this.tugEntity = (AbstractTugEntity) world.getEntity(data.getEntityUUID());
        this.data = data;
        layoutPlayerInventorySlots(8, 84);
        this.addDataSlots(data);
    }
}
