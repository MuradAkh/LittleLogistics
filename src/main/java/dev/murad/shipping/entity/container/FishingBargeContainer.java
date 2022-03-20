package dev.murad.shipping.entity.container;

import dev.murad.shipping.entity.custom.barge.FishingBargeEntity;
import dev.murad.shipping.setup.ModContainerTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class FishingBargeContainer extends AbstractItemHandlerContainer {
    private final FishingBargeEntity fishingBargeEntity;

    public FishingBargeContainer(int windowId, World world, int entityId,
                                    PlayerInventory playerInventory, PlayerEntity player) {
        super(ModContainerTypes.FISHING_BARGE_CONTAINER.get(), windowId, playerInventory, player);
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
    public boolean stillValid(PlayerEntity p_75145_1_) {
        return fishingBargeEntity.stillValid(p_75145_1_);
    }
}
