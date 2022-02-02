package dev.murad.shipping.entity.container;

import dev.murad.shipping.entity.accessor.EnergyTugDataAccessor;
import dev.murad.shipping.event.ModClientEventHandler;
import dev.murad.shipping.setup.ModContainerTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class EnergyTugContainer extends AbstractTugContainer<EnergyTugDataAccessor> {
    public EnergyTugContainer(int windowId, World world, EnergyTugDataAccessor data,
                             PlayerInventory playerInventory, PlayerEntity player) {
        super(ModContainerTypes.ENERGY_TUG_CONTAINER.get(), windowId, world, data, playerInventory, player);

        if(tugEntity != null) {
            tugEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(h -> {
                addSlot(new SlotItemHandler(h, 0, 116, 35)
                        .setBackground(ModClientEventHandler.EMPTY_ATLAS_LOC, ModClientEventHandler.EMPTY_TUG_ROUTE));
                addSlot(new SlotItemHandler(h, 1, 32, 35)
                        .setBackground(ModClientEventHandler.EMPTY_ATLAS_LOC, ModClientEventHandler.EMPTY_ENERGY));
            });
        }
    }

    public int getEnergy() {
        return this.data.getEnergy();
    }

    public int getCapacity() {
        return this.data.getCapacity();
    }

    public double getEnergyCapacityRatio() {
        if (getCapacity() == 0) {
            return 1.0;
        }

        return (double) getEnergy() / getCapacity();
    }

    public boolean isLit() {
        return this.data.isLit();
    }

    @Override
    protected int getSlotNum() {
        return 2;
    }

    @Override
    public boolean stillValid(PlayerEntity p_75145_1_) {
        return tugEntity.stillValid(p_75145_1_);
    }
}
