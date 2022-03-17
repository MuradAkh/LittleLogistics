package dev.murad.shipping.entity.container;

import dev.murad.shipping.entity.accessor.SteamTugDataAccessor;
import dev.murad.shipping.event.ModClientEventHandler;
import dev.murad.shipping.setup.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.SlotItemHandler;


public class SteamTugContainer extends AbstractTugContainer<SteamTugDataAccessor> {
    public SteamTugContainer(int windowId, Level world, SteamTugDataAccessor data,
                             Inventory playerInventory, Player player) {
        super(ModMenuTypes.TUG_CONTAINER.get(), windowId, world, data, playerInventory, player);

        if(tugEntity != null) {
            tugEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(h -> {
                addSlot(new SlotItemHandler(h, 0, 116, 35)
                        .setBackground(EMPTY_ATLAS_LOC, EMPTY_TUG_ROUTE));
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
    public boolean stillValid(Player p_75145_1_) {
        return tugEntity.stillValid(p_75145_1_);
    }

    @Override
    protected int getSlotNum() {
        return 2;
    }
}
