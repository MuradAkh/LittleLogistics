package dev.murad.shipping;

import dev.murad.shipping.block.fluid.render.FluidHopperTileEntityRenderer;
import dev.murad.shipping.entity.container.EnergyTugContainer;
import dev.murad.shipping.entity.container.EnergyTugScreen;
import dev.murad.shipping.entity.container.FishingBargeScreen;
import dev.murad.shipping.entity.container.SteamTugScreen;

import dev.murad.shipping.entity.render.*;
import dev.murad.shipping.item.SpringItem;
import dev.murad.shipping.item.container.TugRouteScreen;
import dev.murad.shipping.setup.*;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.item.ItemModelsProperties;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
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

        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        // Register the enqueueIMC method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::enqueueIMC);
        // Register the processIMC method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::processIMC);
        // Register the doClientStuff method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);

        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ShippingConfig.Client.SPEC, "littlelogistics-client.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ShippingConfig.Server.SPEC, "littlelogistics-server.toml");

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event)
    {
        // some preinit code
        LOGGER.info("HELLO FROM PREINIT");
        LOGGER.info("DIRT BLOCK >> {}", Blocks.DIRT.getRegistryName());
    }

    private void doClientStuff(final FMLClientSetupEvent event) {
        // do something that can only be done on the client
        LOGGER.info("Got game settings {}", event.getMinecraftSupplier().get().options);
        RenderingRegistry.registerEntityRenderingHandler(ModEntityTypes.CHEST_BARGE.get(), ChestBargeRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntityTypes.CHUNK_LOADER_BARGE.get(), ChunkLoaderBargeRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntityTypes.FISHING_BARGE.get(), FishingBargeRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntityTypes.FLUID_TANK_BARGE.get(), FluidTankBargeRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntityTypes.SEATER_BARGE.get(), SeaterBargeRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntityTypes.SPRING.get(), DummyEntityRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntityTypes.TUG_DUMMY_HITBOX.get(), DummyEntityRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntityTypes.STEAM_TUG.get(), SteamTugRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntityTypes.ENERGY_TUG.get(), EnergyTugRenderer::new);

        ClientRegistry.bindTileEntityRenderer(ModTileEntitiesTypes.FLUID_HOPPER.get(), FluidHopperTileEntityRenderer::new);

        ScreenManager.register(ModContainerTypes.TUG_CONTAINER.get(), SteamTugScreen::new);
        ScreenManager.register(ModContainerTypes.ENERGY_TUG_CONTAINER.get(), EnergyTugScreen::new);
        ScreenManager.register(ModContainerTypes.FISHING_BARGE_CONTAINER.get(), FishingBargeScreen::new);

        ScreenManager.register(ModContainerTypes.TUG_ROUTE_CONTAINER.get(), TugRouteScreen::new);

        event.enqueueWork(ModItemModelProperties::register);
    }

    private void enqueueIMC(final InterModEnqueueEvent event)
    {
        // some example code to dispatch IMC to another mod
        InterModComms.sendTo("examplemod", "helloworld", () -> { LOGGER.info("Hello world from the MDK"); return "Hello world";});
    }

    private void processIMC(final InterModProcessEvent event)
    {
        // some example code to receive and process InterModComms from other mods
        LOGGER.info("Got IMC {}", event.getIMCStream().
                map(m->m.getMessageSupplier().get()).
                collect(Collectors.toList()));
    }
    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
        // do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // You can use EventBusSubscriber to automatically subscribe events on the contained class (this is subscribing to the MOD
    // Event bus for receiving Registry Events)
    @Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents {
        @SubscribeEvent
        public static void onBlocksRegistry(final RegistryEvent.Register<Block> blockRegistryEvent) {
            // register a new block here
            LOGGER.info("HELLO from Register Block");
        }
    }
}
