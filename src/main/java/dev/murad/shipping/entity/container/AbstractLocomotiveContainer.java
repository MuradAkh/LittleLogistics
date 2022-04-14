package dev.murad.shipping.entity.container;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.accessor.DataAccessor;
import dev.murad.shipping.entity.custom.train.locomotive.AbstractLocomotiveEntity;
import dev.murad.shipping.network.LocomotivePacketHandler;
import dev.murad.shipping.network.SetLocomotiveEnginePacket;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nullable;

public abstract class AbstractLocomotiveContainer <T extends DataAccessor> extends AbstractItemHandlerContainer{
    public static final ResourceLocation EMPTY_LOCO_ROUTE = new ResourceLocation(ShippingMod.MOD_ID, "item/empty_loco_route");
    public static final ResourceLocation EMPTY_ENERGY = new ResourceLocation(ShippingMod.MOD_ID, "item/empty_energy");
    public static final ResourceLocation EMPTY_ATLAS_LOC = InventoryMenu.BLOCK_ATLAS;
    protected T data;
    protected AbstractLocomotiveEntity locomotiveEntity;

    public AbstractLocomotiveContainer(@Nullable MenuType<?> containerType, int windowId, Level world, T data,
                                Inventory playerInventory, Player player) {
        super(containerType, windowId, playerInventory, player);
        this.locomotiveEntity = (AbstractLocomotiveEntity) world.getEntity(data.getEntityUUID());
        this.data = data;
        layoutPlayerInventorySlots(8, 84);
        this.addDataSlots(data);

        addSlot(new SlotItemHandler(locomotiveEntity.getLocoRouteItemHandler(),
                0, 98, 57).setBackground(EMPTY_ATLAS_LOC, EMPTY_LOCO_ROUTE));
    }



    @Override
    protected int getSlotNum() {
        return 2;
    }

    public abstract boolean isOn();
    public abstract int routeSize();
    public abstract int visitedSize();

    public void setEngine(boolean state){
        LocomotivePacketHandler.INSTANCE.sendToServer(new SetLocomotiveEnginePacket(locomotiveEntity.getId(), state));
    }

    public String getRouteText(){
        return  visitedSize() + "/" + routeSize();
    }

    @Override
    public boolean stillValid(Player p_75145_1_) {
        return locomotiveEntity.stillValid(p_75145_1_);
    }
}
