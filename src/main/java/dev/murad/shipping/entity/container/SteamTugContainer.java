package dev.murad.shipping.entity.container;

import dev.murad.shipping.data.accessor.SteamTugDataAccessor;
import dev.murad.shipping.event.ModClientEventHandler;
import dev.murad.shipping.setup.ModContainerTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.SlotItemHandler;


public class SteamTugContainer extends AbstractTugContainer<SteamTugDataAccessor> {
    public SteamTugContainer(int windowId, World world, SteamTugDataAccessor data,
                             PlayerInventory playerInventory, PlayerEntity player) {
        super(ModContainerTypes.TUG_CONTAINER.get(), windowId, world, data, playerInventory, player);

        if(tugEntity != null) {
            tugEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(h -> {
                addSlot(new SlotItemHandler(h, 0, 116, 35)
                        .setBackground(ModClientEventHandler.EMPTY_ATLAS_LOC, ModClientEventHandler.EMPTY_TUG_ROUTE));
                addSlot(new SlotItemHandler(h, 1, 42, 40));
            });
        }
        this.addDataSlots(data.getRawData());
    }


    public int getBurnProgress(){
        return data.getBurnProgress();
    }

    public boolean isLit(){
        return data.isLit();
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
