package dev.murad.shipping.entity.container;

import dev.murad.shipping.entity.custom.tug.AbstractTugEntity;
import dev.murad.shipping.setup.ModContainerTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.IIntArray;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.SlotItemHandler;


public class SteamTugContainer extends AbstractItemHandlerContainer {
    private final AbstractTugEntity tugEntity;
    private IIntArray data;

    public SteamTugContainer(int windowId, World world, IIntArray data,
                             PlayerInventory playerInventory, PlayerEntity player) {
        super(ModContainerTypes.TUG_CONTAINER.get(), windowId, playerInventory, player);
        this.tugEntity = (AbstractTugEntity) world.getEntity(data.get(0));
        this.data = data;
        layoutPlayerInventorySlots(8, 84);

        if(tugEntity != null) {
            tugEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(h -> {
                addSlot(new SlotItemHandler(h, 0, 116, 35));
                addSlot(new SlotItemHandler(h, 1, 42, 40));
            });
        }
        this.addDataSlots(data);
    }


    public int getBurnProgress(){
        return data.get(1);
    }

    public boolean isLit(){
        return data.get(2) > 0;
    }

    @Override
    public boolean stillValid(PlayerEntity p_75145_1_) {
        return tugEntity.stillValid(p_75145_1_);
    }

    @Override
    protected int getSlotNum() {
        return 2;
    }
}
