package dev.murad.shipping;

import dev.murad.shipping.entity.container.*;
import dev.murad.shipping.item.container.TugRouteScreen;
import dev.murad.shipping.setup.ModItemModelProperties;
import dev.murad.shipping.setup.ModMenuTypes;
import dev.murad.shipping.setup.Registration;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.stream.Collectors;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(ShippingMod.MOD_ID)
public class ShippingMod
{
    public static final String MOD_ID = "littlelogistics";
    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();

    public ShippingMod() {
        Registration.register();

        // Register the doClientStuff method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);

        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ShippingConfig.Client.SPEC, "littlelogistics-client.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ShippingConfig.Server.SPEC, "littlelogistics-server.toml");

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void doClientStuff(final FMLClientSetupEvent event) {
        MenuScreens.register(ModMenuTypes.TUG_CONTAINER.get(), SteamTugScreen::new);
        MenuScreens.register(ModMenuTypes.ENERGY_TUG_CONTAINER.get(), EnergyTugScreen::new);
        MenuScreens.register(ModMenuTypes.FISHING_BARGE_CONTAINER.get(), FishingBargeScreen::new);
        MenuScreens.register(ModMenuTypes.ENERGY_LOCOMOTIVE_CONTAINER.get(), EnergyLocomotiveScreen::new);
        MenuScreens.register(ModMenuTypes.STEAM_LOCOMOTIVE_CONTAINER.get(), SteamLocomotiveScreen::new);

        MenuScreens.register(ModMenuTypes.TUG_ROUTE_CONTAINER.get(), TugRouteScreen::new);

        event.enqueueWork(ModItemModelProperties::register);
    }
}
