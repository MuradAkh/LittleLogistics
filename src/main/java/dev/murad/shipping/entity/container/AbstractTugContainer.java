package dev.murad.shipping.entity.container;

import dev.murad.shipping.data.accessor.DataAccessor;
import dev.murad.shipping.entity.custom.tug.AbstractTugEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public abstract class AbstractTugContainer<T extends DataAccessor> extends AbstractItemHandlerContainer {
    protected T data;
    protected AbstractTugEntity tugEntity;

    public AbstractTugContainer(@Nullable ContainerType<?> containerType, int windowId, World world, T data,
                                PlayerInventory playerInventory, PlayerEntity player) {
        super(containerType, windowId, playerInventory, player);
        this.tugEntity = (AbstractTugEntity) world.getEntity(data.getEntityUUID());
        this.data = data;
        layoutPlayerInventorySlots(8, 84);
        this.addDataSlots(data);
    }
}
