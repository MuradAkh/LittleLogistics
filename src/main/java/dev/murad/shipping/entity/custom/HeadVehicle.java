package dev.murad.shipping.entity.custom;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.items.ItemStackHandler;

public interface HeadVehicle  {

    void setEngineOn(boolean state);

    ItemStackHandler getRouteItemHandler();

    boolean isValid(Player pPlayer);

    ResourceLocation getRouteIcon();
}
