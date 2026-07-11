package dev.murad.shipping.setup;

import dev.murad.shipping.block.dockingstation.DockingStationMenu;
import dev.murad.shipping.entity.accessor.*;
import dev.murad.shipping.entity.container.EnergyHeadVehicleContainer;
import dev.murad.shipping.entity.container.SteamHeadVehicleContainer;
import dev.murad.shipping.entity.custom.train.locomotive.EnergyLocomotiveEntity;
import dev.murad.shipping.entity.custom.train.locomotive.SteamLocomotiveEntity;
import dev.murad.shipping.entity.custom.vessel.tug.EnergyTugEntity;
import dev.murad.shipping.entity.custom.vessel.tug.SteamTugEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModMenuTypes {

    private static SimpleContainerData makeIntArray(FriendlyByteBuf buffer) {
        int size = (buffer.readableBytes() + 1) / 4;
        SimpleContainerData arr = new SimpleContainerData(size);
        for (int i = 0; i < size; i++) {
            arr.set(i, buffer.readInt());
        }
        return arr;
    }

    public static final DeferredHolder<MenuType<?>, MenuType<SteamHeadVehicleContainer<SteamTugEntity>>> TUG_CONTAINER =
            Registration.CONTAINERS.register("tug_container",
                    () -> IMenuTypeExtension.create(
                            (windowId, inv, data) ->
                                    new SteamHeadVehicleContainer<>(windowId, inv.player.level(), new SteamHeadVehicleDataAccessor(makeIntArray(data)), inv, inv.player)));

    public static final DeferredHolder<MenuType<?>, MenuType<EnergyHeadVehicleContainer<EnergyTugEntity>>> ENERGY_TUG_CONTAINER =
            Registration.CONTAINERS.register("energy_tug_container",
                    () -> IMenuTypeExtension.create(
                            (windowId, inv, data) ->
                                    new EnergyHeadVehicleContainer<>(windowId, inv.player.level(), new EnergyHeadVehicleDataAccessor(makeIntArray(data)), inv, inv.player)));

    public static final DeferredHolder<MenuType<?>, MenuType<SteamHeadVehicleContainer<SteamLocomotiveEntity>>> STEAM_LOCOMOTIVE_CONTAINER =
            Registration.CONTAINERS.register("steam_locomotive_container",
                    () -> IMenuTypeExtension.create(
                            (windowId, inv, data) ->
                                    new SteamHeadVehicleContainer<>(windowId, inv.player.level(), new SteamHeadVehicleDataAccessor(makeIntArray(data)), inv, inv.player)));

    public static final DeferredHolder<MenuType<?>, MenuType<EnergyHeadVehicleContainer<EnergyLocomotiveEntity>>> ENERGY_LOCOMOTIVE_CONTAINER =
            Registration.CONTAINERS.register("energy_locomotive_container",
                    () -> IMenuTypeExtension.create(
                            (windowId, inv, data) ->
                                    new EnergyHeadVehicleContainer<>(windowId, inv.player.level(), new EnergyHeadVehicleDataAccessor(makeIntArray(data)), inv, inv.player)));

    public static final DeferredHolder<MenuType<?>, MenuType<DockingStationMenu>> DOCKING_STATION =
            Registration.CONTAINERS.register("docking_station",
                    () -> IMenuTypeExtension.create(DockingStationMenu::new));

    public static void register () {}
}
