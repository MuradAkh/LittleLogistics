package dev.murad.shipping.entity.custom;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.world.level.ChunkPos;

public interface HeadVehicle  {

    void setEngineOn(boolean state);

    ItemStackHandler getRouteItemHandler();

    boolean isValid(Player pPlayer);

    boolean hasOwner();

    Optional<UUID> getOwnerUUID();

    void setOwner(UUID uuid);

    ResourceLocation getRouteIcon();

    boolean isManagedServiceActive();

    List<ChunkPos> getUpcomingRouteChunks(int maxSteps);
}
