package dev.murad.shipping.entity.container;

import dev.murad.shipping.entity.accessor.EnergyLocomotiveDataAccessor;
import dev.murad.shipping.setup.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class EnergyLocomotiveContainer extends AbstractLocomotiveContainer<EnergyLocomotiveDataAccessor> {
    public EnergyLocomotiveContainer(int windowId, Level world, EnergyLocomotiveDataAccessor data,
                              Inventory playerInventory, Player player) {
        super(ModMenuTypes.ENERGY_LOCOMOTIVE_CONTAINER.get(), windowId, world, data, playerInventory, player);

        if(locomotiveEntity != null) {
            locomotiveEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(h -> {
                addSlot(new SlotItemHandler(h, 0, 32, 35)
                        .setBackground(EMPTY_ATLAS_LOC, EMPTY_ENERGY));
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
    public boolean isOn() {
        return this.data.isOn();
    }
}
