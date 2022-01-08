package dev.murad.shipping.entity.container;

import dev.murad.shipping.entity.custom.tug.AbstractTugEntity;
import dev.murad.shipping.setup.ModContainerTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

public class SteamTugContainer extends AbstractItemHandlerContainer {
    private final AbstractTugEntity tugEntity;

    public SteamTugContainer(int windowId, World world, int entityId,
                             PlayerInventory playerInventory, PlayerEntity player) {
        super(ModContainerTypes.TUG_CONTAINER.get(), windowId, playerInventory, player);
        this.tugEntity = (AbstractTugEntity) world.getEntity(entityId);
        layoutPlayerInventorySlots(8, 84);

        if(tugEntity != null) {
            tugEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(h -> {
                addSlot(new SlotItemHandler(h, 0, 116, 35));
                addSlot(new SlotItemHandler(h, 1, 42, 40));
            });
        }
    }


    public int getBurnProgress(){
        return tugEntity.getBurnProgress();
    }

    public boolean isLit(){
        return tugEntity.isLit();
    }


    @Override
    protected int getSlotNum() {
        return 2;
    }
}
