package dev.murad.shipping.entity.container;

import dev.murad.shipping.entity.accessor.SteamHeadVehicleDataAccessor;
import dev.murad.shipping.entity.custom.HeadVehicle;
import dev.murad.shipping.setup.ModMenuTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.capabilities.Capabilities;

public class SteamHeadVehicleContainer<T extends Entity & HeadVehicle> extends AbstractHeadVehicleContainer<SteamHeadVehicleDataAccessor, T> {
    public SteamHeadVehicleContainer(int windowId, Level world, SteamHeadVehicleDataAccessor data,
                                     Inventory playerInventory, net.minecraft.world.entity.player.Player player) {
        super(ModMenuTypes.STEAM_LOCOMOTIVE_CONTAINER.get(), windowId, world, data, playerInventory, player);

        if(entity != null) {
            var h = entity.getCapability(Capabilities.ItemHandler.ENTITY, null);
            if (h != null) {
                addSlot(new SlotItemHandler(h, 0, 42, 40));
            }
        }
        this.addDataSlots(data.getRawData());
    }

    public int getBurnProgress(){
        return data.getBurnProgress();
    }

}
