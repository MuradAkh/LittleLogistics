package dev.murad.shipping.entity.container;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.accessor.DataAccessor;
import dev.murad.shipping.entity.custom.HeadVehicle;
import dev.murad.shipping.global.PlayerTrainChunkManager;
import dev.murad.shipping.network.EnrollVehiclePacket;
import dev.murad.shipping.network.VehiclePacketHandler;
import dev.murad.shipping.network.SetEnginePacket;
import dev.murad.shipping.util.EnrollmentHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nullable;

public abstract class AbstractHeadVehicleContainer<T extends DataAccessor, U extends Entity & HeadVehicle> extends AbstractItemHandlerContainer{
    public static final ResourceLocation EMPTY_ATLAS_LOC = InventoryMenu.BLOCK_ATLAS;
    protected T data;
    protected U entity;
    protected Player player;

    public AbstractHeadVehicleContainer(@Nullable MenuType<?> containerType, int windowId, Level world, T data,
                                        Inventory playerInventory, Player player) {
        super(containerType, windowId, playerInventory, player);
        this.entity = (U) world.getEntity(data.getEntityUUID());
        this.data = data;
        this.player = playerInventory.player;
        layoutPlayerInventorySlots(8, 84);
        this.addDataSlots(data);

        addSlot(new SlotItemHandler(entity.getRouteItemHandler(),
                0, 98, 57).setBackground(EMPTY_ATLAS_LOC, entity.getRouteIcon()));
    }

    @Override
    protected int getSlotNum() {
        return 2;
    }

    public abstract boolean isOn();
    public abstract int routeSize();
    public abstract int visitedSize();
    public abstract EnrollmentHandler.Enrollment getEnrollment();

    public void setEngine(boolean state){
        VehiclePacketHandler.INSTANCE.sendToServer(new SetEnginePacket(entity.getId(), state));
    }

    public void enroll(){
        VehiclePacketHandler.INSTANCE.sendToServer(new EnrollVehiclePacket(entity.getId()));
    }


    public String getRouteText(){
        return  visitedSize() + "/" + routeSize();
    }

    @Override
    public boolean stillValid(Player p_75145_1_) {
        return entity.isValid(p_75145_1_);
    }
}
