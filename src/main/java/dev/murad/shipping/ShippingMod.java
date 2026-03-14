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
import dev.murad.shipping.util.MobileChunkLoader;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
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
        modEventBus.addListener(this::registerTicketControllers);

        modContainer.registerConfig(ModConfig.Type.COMMON, ShippingConfig.Common.SPEC, "littlelogistics-common.toml");
        modContainer.registerConfig(ModConfig.Type.CLIENT, ShippingConfig.Client.SPEC, "littlelogistics-client.toml");
        modContainer.registerConfig(ModConfig.Type.SERVER, ShippingConfig.Server.SPEC, "littlelogistics-server.toml");
    }

    private void doClientStuff(final FMLClientSetupEvent event) {
        event.enqueueWork(ModItemModelProperties::register);
    }

    private void registerTicketControllers(final RegisterTicketControllersEvent event) {
        event.register(MobileChunkLoader.TICKET_CONTROLLER);
    }

    @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void registerMenuScreens(RegisterMenuScreensEvent event) {
            event.register(ModMenuTypes.TUG_CONTAINER.get(), SteamHeadVehicleScreen<SteamTugEntity>::new);
            event.register(ModMenuTypes.STEAM_LOCOMOTIVE_CONTAINER.get(), SteamHeadVehicleScreen<SteamLocomotiveEntity>::new);
            event.register(ModMenuTypes.ENERGY_TUG_CONTAINER.get(), EnergyHeadVehicleScreen<EnergyTugEntity>::new);
            event.register(ModMenuTypes.ENERGY_LOCOMOTIVE_CONTAINER.get(), EnergyHeadVehicleScreen<EnergyLocomotiveEntity>::new);
            event.register(ModMenuTypes.TUG_ROUTE_CONTAINER.get(), TugRouteScreen::new);
        }
    }

    public static ResourceLocation entityTexture(String suffix) {
        return ResourceLocation.fromNamespaceAndPath(ShippingMod.MOD_ID, String.format("textures/entity/%s", suffix));
    }
}
