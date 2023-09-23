package dev.murad.shipping.entity.custom;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.items.ItemStackHandler;

import java.util.UUID;

public interface HeadVehicle  {

    void setEngineOn(boolean state);

    ItemStackHandler getRouteItemHandler();

    boolean isValid(Player pPlayer);

    boolean hasOwner();

    ResourceLocation getRouteIcon();

    void enroll(UUID uuid);

    String owner();
}
