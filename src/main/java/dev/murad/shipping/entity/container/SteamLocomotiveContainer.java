package dev.murad.shipping.entity.container;

import dev.murad.shipping.entity.accessor.SteamLocomotiveDataAccessor;
import dev.murad.shipping.setup.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class SteamLocomotiveContainer extends AbstractLocomotiveContainer<SteamLocomotiveDataAccessor> {
    public SteamLocomotiveContainer(int windowId, Level world, SteamLocomotiveDataAccessor data,
                             Inventory playerInventory, net.minecraft.world.entity.player.Player player) {
        super(ModMenuTypes.STEAM_LOCOMOTIVE_CONTAINER.get(), windowId, world, data, playerInventory, player);

        if(locomotiveEntity != null) {
            locomotiveEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(h -> {
                addSlot(new SlotItemHandler(h, 0, 42, 40));
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
    public boolean isOn(){
        return data.isOn();
    }

    @Override
    public int routeSize() {
        return data.routeSize();
    }

    @Override
    public int visitedSize() {
        return data.visitedSize();
    }

}
