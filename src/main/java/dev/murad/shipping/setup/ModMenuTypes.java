package dev.murad.shipping.setup;

import dev.murad.shipping.entity.accessor.*;
import dev.murad.shipping.entity.container.*;
import dev.murad.shipping.item.container.TugRouteContainer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {

    private static SimpleContainerData makeIntArray(FriendlyByteBuf buffer) {
        int size = (buffer.readableBytes() + 1) / 4;
        SimpleContainerData arr = new SimpleContainerData(size);
        for (int i = 0; i < size; i++) {
            arr.set(i, buffer.readInt());
        }
        return arr;
    }

    public static final RegistryObject<MenuType<SteamTugContainer>> TUG_CONTAINER =
            Registration.CONTAINERS.register("tug_container",
                    () -> IForgeMenuType.create(
                            (windowId, inv, data) ->
                                    new SteamTugContainer(windowId, inv.player.level, new SteamTugDataAccessor(makeIntArray(data)), inv, inv.player)));

    public static final RegistryObject<MenuType<EnergyTugContainer>> ENERGY_TUG_CONTAINER =
            Registration.CONTAINERS.register("energy_tug_container",
                    () -> IForgeMenuType.create(
                            (windowId, inv, data) ->
                                    new EnergyTugContainer(windowId, inv.player.level, new EnergyTugDataAccessor(makeIntArray(data)), inv, inv.player)));

    public static final RegistryObject<MenuType<SteamLocomotiveContainer>> STEAM_LOCOMOTIVE_CONTAINER =
            Registration.CONTAINERS.register("steam_locomotive_container",
                    () -> IForgeMenuType.create(
                            (windowId, inv, data) ->
                                    new SteamLocomotiveContainer(windowId, inv.player.level, new SteamLocomotiveDataAccessor(makeIntArray(data)), inv, inv.player)));

    public static final RegistryObject<MenuType<EnergyLocomotiveContainer>> ENERGY_LOCOMOTIVE_CONTAINER =
            Registration.CONTAINERS.register("energy_locomotive_container",
                    () -> IForgeMenuType.create(
                            (windowId, inv, data) ->
                                    new EnergyLocomotiveContainer(windowId, inv.player.level, new EnergyLocomotiveDataAccessor(makeIntArray(data)), inv, inv.player)));

    public static final RegistryObject<MenuType<FishingBargeContainer>> FISHING_BARGE_CONTAINER =
            Registration.CONTAINERS.register("fishing_barge_container",
                    () -> IForgeMenuType.create(
                            (windowId, inv, data) ->
                                    new FishingBargeContainer(windowId, inv.player.level, data.readInt(), inv, inv.player)));

    public static final RegistryObject<MenuType<TugRouteContainer>> TUG_ROUTE_CONTAINER =
            Registration.CONTAINERS.register("tug_route_container",
                    () -> IForgeMenuType.create(
                            (windowId, inv, data) ->
                                    new TugRouteContainer(windowId, inv.player.level, new TugRouteScreenDataAccessor(makeIntArray(data)), inv, inv.player)));




    public static void register () {}
}
