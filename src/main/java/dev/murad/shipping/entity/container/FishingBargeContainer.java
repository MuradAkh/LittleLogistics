package dev.murad.shipping.entity.container;

import dev.murad.shipping.entity.custom.vessel.barge.FishingBargeEntity;
import dev.murad.shipping.setup.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class FishingBargeContainer extends AbstractItemHandlerContainer {
    private final FishingBargeEntity fishingBargeEntity;

    public FishingBargeContainer(int windowId, Level world, int entityId,
                                    Inventory playerInventory, Player player) {
        super(ModMenuTypes.FISHING_BARGE_CONTAINER.get(), windowId, playerInventory, player);
        this.fishingBargeEntity = (FishingBargeEntity) world.getEntity(entityId);
        layoutPlayerInventorySlots(8, 49 + 18 * 2);

        if(fishingBargeEntity != null) {
            fishingBargeEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(h -> {
                for(int l = 0; l < 3; ++l) {
                    for (int k = 0; k < 9; ++k) {
                        this.addSlot(new SlotItemHandler(h, l * 9 + k, 8 + k * 18, 18 * (l + 1) ));
                    }
                }
            });
        }
    }

    @Override
    protected int getSlotNum() {
        return 27;
    }

    @Override
    public boolean stillValid(Player p_75145_1_) {
        return fishingBargeEntity.stillValid(p_75145_1_);
    }
}
