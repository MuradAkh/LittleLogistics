package dev.murad.shipping.setup;

import dev.murad.shipping.entity.accessor.*;
import dev.murad.shipping.entity.container.*;
import dev.murad.shipping.entity.custom.train.locomotive.EnergyLocomotiveEntity;
import dev.murad.shipping.entity.custom.train.locomotive.SteamLocomotiveEntity;
import dev.murad.shipping.entity.custom.vessel.tug.EnergyTugEntity;
import dev.murad.shipping.entity.custom.vessel.tug.SteamTugEntity;
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

    public static final RegistryObject<MenuType<SteamHeadVehicleContainer<SteamTugEntity>>> TUG_CONTAINER =
            Registration.CONTAINERS.register("tug_container",
                    () -> IForgeMenuType.create(
                            (windowId, inv, data) ->
                                    new SteamHeadVehicleContainer<>(windowId, inv.player.level(), new SteamHeadVehicleDataAccessor(makeIntArray(data)), inv, inv.player)));

    public static final RegistryObject<MenuType<EnergyHeadVehicleContainer<EnergyTugEntity>>> ENERGY_TUG_CONTAINER =
            Registration.CONTAINERS.register("energy_tug_container",
                    () -> IForgeMenuType.create(
                            (windowId, inv, data) ->
                                    new EnergyHeadVehicleContainer<>(windowId, inv.player.level(), new EnergyHeadVehicleDataAccessor(makeIntArray(data)), inv, inv.player)));

    public static final RegistryObject<MenuType<SteamHeadVehicleContainer<SteamLocomotiveEntity>>> STEAM_LOCOMOTIVE_CONTAINER =
            Registration.CONTAINERS.register("steam_locomotive_container",
                    () -> IForgeMenuType.create(
                            (windowId, inv, data) ->
                                    new SteamHeadVehicleContainer<>(windowId, inv.player.level(), new SteamHeadVehicleDataAccessor(makeIntArray(data)), inv, inv.player)));

    public static final RegistryObject<MenuType<EnergyHeadVehicleContainer<EnergyLocomotiveEntity>>> ENERGY_LOCOMOTIVE_CONTAINER =
            Registration.CONTAINERS.register("energy_locomotive_container",
                    () -> IForgeMenuType.create(
                            (windowId, inv, data) ->
                                    new EnergyHeadVehicleContainer<>(windowId, inv.player.level(), new EnergyHeadVehicleDataAccessor(makeIntArray(data)), inv, inv.player)));

    public static final RegistryObject<MenuType<FishingBargeContainer>> FISHING_BARGE_CONTAINER =
            Registration.CONTAINERS.register("fishing_barge_container",
                    () -> IForgeMenuType.create(
                            (windowId, inv, data) ->
                                    new FishingBargeContainer(windowId, inv.player.level(), data.readInt(), inv, inv.player)));

    public static final RegistryObject<MenuType<TugRouteContainer>> TUG_ROUTE_CONTAINER =
            Registration.CONTAINERS.register("tug_route_container",
                    () -> IForgeMenuType.create(
                            (windowId, inv, data) ->
                                    new TugRouteContainer(windowId, inv.player.level(), new TugRouteScreenDataAccessor(makeIntArray(data)), inv, inv.player)));




    public static void register () {}
}
