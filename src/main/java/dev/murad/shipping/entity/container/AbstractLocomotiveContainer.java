package dev.murad.shipping.entity.container;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.accessor.DataAccessor;
import dev.murad.shipping.entity.custom.train.locomotive.AbstractLocomotiveEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public class AbstractLocomotiveContainer <T extends DataAccessor> extends AbstractItemHandlerContainer{
    public static final ResourceLocation EMPTY_ENERGY = new ResourceLocation(ShippingMod.MOD_ID, "item/empty_energy");
    protected T data;
    protected AbstractLocomotiveEntity locomotiveEntity;

    public AbstractLocomotiveContainer(@Nullable MenuType<?> containerType, int windowId, Level world, T data,
                                Inventory playerInventory, Player player) {
        super(containerType, windowId, playerInventory, player);
        this.locomotiveEntity = (AbstractLocomotiveEntity) world.getEntity(data.getEntityUUID());
        this.data = data;
        layoutPlayerInventorySlots(8, 84);
        this.addDataSlots(data);
    }

    @Override
    protected int getSlotNum() {
        return 1;
    }

    @Override
    public boolean stillValid(Player p_75145_1_) {
        return locomotiveEntity.stillValid(p_75145_1_);
    }
}
