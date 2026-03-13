package dev.murad.shipping;

import dev.murad.shipping.entity.container.*;
import dev.murad.shipping.entity.custom.train.locomotive.EnergyLocomotiveEntity;
import dev.murad.shipping.entity.custom.train.locomotive.SteamLocomotiveEntity;
import dev.murad.shipping.entity.custom.vessel.tug.EnergyTugEntity;
import dev.murad.shipping.entity.custom.vessel.tug.SteamTugEntity;
import dev.murad.shipping.item.container.TugRouteScreen;
import dev.murad.shipping.setup.ModItemModelProperties;
import dev.murad.shipping.setup.ModMenuTypes;
import dev.murad.shipping.setup.Registration;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(ShippingMod.MOD_ID)
public class ShippingMod
{
    public static final String MOD_ID = "littlelogistics";
    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();

    public ShippingMod(IEventBus modEventBus, ModContainer modContainer) {
        Registration.register(modEventBus);

        // Register the doClientStuff method for modloading
        modEventBus.addListener(this::doClientStuff);

        modContainer.registerConfig(ModConfig.Type.COMMON, ShippingConfig.Common.SPEC, "littlelogistics-common.toml");
        modContainer.registerConfig(ModConfig.Type.CLIENT, ShippingConfig.Client.SPEC, "littlelogistics-client.toml");
        modContainer.registerConfig(ModConfig.Type.SERVER, ShippingConfig.Server.SPEC, "littlelogistics-server.toml");
    }

    private void doClientStuff(final FMLClientSetupEvent event) {
        MenuScreens.register(ModMenuTypes.TUG_CONTAINER.get(), SteamHeadVehicleScreen<SteamTugEntity>::new);
        MenuScreens.register(ModMenuTypes.STEAM_LOCOMOTIVE_CONTAINER.get(), SteamHeadVehicleScreen<SteamLocomotiveEntity>::new);
        MenuScreens.register(ModMenuTypes.ENERGY_TUG_CONTAINER.get(),  EnergyHeadVehicleScreen<EnergyTugEntity>::new);
        MenuScreens.register(ModMenuTypes.ENERGY_LOCOMOTIVE_CONTAINER.get(), EnergyHeadVehicleScreen<EnergyLocomotiveEntity>::new);
        MenuScreens.register(ModMenuTypes.FISHING_BARGE_CONTAINER.get(), FishingBargeScreen::new);

        MenuScreens.register(ModMenuTypes.TUG_ROUTE_CONTAINER.get(), TugRouteScreen::new);

        event.enqueueWork(ModItemModelProperties::register);
    }

    public static ResourceLocation entityTexture(String suffix) {
        return new ResourceLocation(ShippingMod.MOD_ID, String.format("textures/entity/%s", suffix));
    }
}
