package dev.murad.shipping.entity.container;

import dev.murad.shipping.entity.accessor.EnergyHeadVehicleDataAccessor;
import dev.murad.shipping.entity.custom.HeadVehicle;
import dev.murad.shipping.setup.ModItems;
import dev.murad.shipping.setup.ModMenuTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.SlotItemHandler;
import net.neoforged.neoforge.capabilities.Capabilities;

public class EnergyHeadVehicleContainer<T extends Entity & HeadVehicle> extends AbstractHeadVehicleContainer<EnergyHeadVehicleDataAccessor, T> {
    public EnergyHeadVehicleContainer(int windowId, Level world, EnergyHeadVehicleDataAccessor data,
                                      Inventory playerInventory, Player player) {
        super(ModMenuTypes.ENERGY_LOCOMOTIVE_CONTAINER.get(), windowId, world, data, playerInventory, player);

        if(entity != null) {
            var h = entity.getCapability(Capabilities.ItemHandler.ENTITY, null);
            if (h != null) {
                addSlot(new SlotItemHandler(h, 0, 32, 35)
                        .setBackground(EMPTY_ATLAS_LOC, ModItems.EMPTY_ENERGY));
            }
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
}
