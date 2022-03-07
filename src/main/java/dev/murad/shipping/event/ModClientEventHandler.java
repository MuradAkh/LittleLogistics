package dev.murad.shipping.event;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.block.fluid.render.FluidHopperTileEntityRenderer;
import dev.murad.shipping.entity.models.*;
import dev.murad.shipping.entity.render.*;
import dev.murad.shipping.entity.render.TrainCarRenderer;
import dev.murad.shipping.entity.render.barge.FishingBargeRenderer;
import dev.murad.shipping.entity.render.barge.FluidTankBargeRenderer;
import dev.murad.shipping.entity.render.barge.StaticVesselRenderer;
import dev.murad.shipping.setup.ModBlocks;
import dev.murad.shipping.setup.ModEntityTypes;
import dev.murad.shipping.setup.ModTileEntitiesTypes;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;


/**
 * Mod-specific event bus
 */
@Mod.EventBusSubscriber(modid = ShippingMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModClientEventHandler {
    public static final ResourceLocation EMPTY_TUG_ROUTE = new ResourceLocation(ShippingMod.MOD_ID, "item/empty_tug_route");
    public static final ResourceLocation EMPTY_ENERGY = new ResourceLocation(ShippingMod.MOD_ID, "item/empty_energy");
    public static final ResourceLocation EMPTY_ATLAS_LOC = InventoryMenu.BLOCK_ATLAS;

    @SubscribeEvent
    public static void onTextureStitchEventPre(TextureStitchEvent.Pre event) {
        if (event.getAtlas().location() != EMPTY_ATLAS_LOC) return;
        event.addSprite(EMPTY_TUG_ROUTE);
        event.addSprite(EMPTY_ENERGY);
    }

    @SubscribeEvent
    public static void onRenderTypeSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.FLUID_HOPPER.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.VESSEL_CHARGER.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.JUNCTION_RAIL.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.SWITCH_RAIL.get(), RenderType.cutoutMipped());
        });
    }

    @SubscribeEvent
    public static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Barges
        event.registerEntityRenderer(ModEntityTypes.CHEST_BARGE.get(),
                (ctx) -> new StaticVesselRenderer<>(ctx, ChestBargeModel::new, ChestBargeModel.LAYER_LOCATION,
                        new ResourceLocation(ShippingMod.MOD_ID, "textures/entity/barge.png")));
        event.registerEntityRenderer(ModEntityTypes.CHUNK_LOADER_BARGE.get(),
                (ctx) -> new StaticVesselRenderer<>(ctx, ChunkLoaderBargeModel::new, ChunkLoaderBargeModel.LAYER_LOCATION,
                        new ResourceLocation(ShippingMod.MOD_ID, "textures/entity/chunk_loader_barge.png")));

        event.registerEntityRenderer(ModEntityTypes.SEATER_BARGE.get(),
                (ctx) -> new StaticVesselRenderer<>(ctx, SeaterBargeModel::new, SeaterBargeModel.LAYER_LOCATION,
                        new ResourceLocation(ShippingMod.MOD_ID, "textures/entity/seater_barge.png")));

        event.registerEntityRenderer(ModEntityTypes.FISHING_BARGE.get(), FishingBargeRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.FLUID_TANK_BARGE.get(), FluidTankBargeRenderer::new);

        // Tugs
        event.registerEntityRenderer(ModEntityTypes.ENERGY_TUG.get(),
                (ctx) -> new StaticVesselRenderer<>(ctx, EnergyTugModel::new, EnergyTugModel.LAYER_LOCATION,
                        new ResourceLocation(ShippingMod.MOD_ID, "textures/entity/energy_tug.png")) {
                    // todo: fix in models itself
                    @Override
                    protected double getModelYoffset() {
                        return 1.55D;
                    }

                    @Override
                    protected float getModelYrot() {
                        return 0.0F;
                    }
                });


        event.registerEntityRenderer(ModEntityTypes.STEAM_TUG.get(),
                (ctx) -> new StaticVesselRenderer<>(ctx, SteamTugModel::new, SteamTugModel.LAYER_LOCATION,
                        new ResourceLocation(ShippingMod.MOD_ID, "textures/entity/tug.png")) {
                    // todo: fix in models itself
                    @Override
                    protected double getModelYoffset() {
                        return 1.45D;
                    }

                    @Override
                    protected float getModelYrot() {
                        return 0;
                    }
                });

        event.registerEntityRenderer(ModEntityTypes.SPRING.get(), DummyEntityRenderer::new);
        event.registerBlockEntityRenderer(ModTileEntitiesTypes.FLUID_HOPPER.get(), FluidHopperTileEntityRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.STEAM_LOCOMOTIVE.get(), ctx -> new TrainCarRenderer(ctx,
                SteamLocomotiveModel::new,
                SteamLocomotiveModel.LAYER_LOCATION,
                "textures/entity/steam_locomotive.png"));
        event.registerEntityRenderer(ModEntityTypes.ENERGY_LOCOMOTIVE.get(), ctx -> new TrainCarRenderer(ctx,
                EnergyLocomotiveModel::new,
                EnergyLocomotiveModel.LAYER_LOCATION,
                "textures/entity/energy_locomotive.png"));
        event.registerEntityRenderer(ModEntityTypes.CHEST_CAR.get(), ctx -> new TrainCarRenderer(ctx,
                ChestCarModel::new,
                ChestCarModel.LAYER_LOCATION,
                "textures/entity/chest_car.png"));
    }

    @SubscribeEvent
    public static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ChainExtendedModel.LAYER_LOCATION, ChainExtendedModel::createBodyLayer);
        event.registerLayerDefinition(ChainModel.LAYER_LOCATION, ChainModel::createBodyLayer);
        event.registerLayerDefinition(ChestBargeModel.LAYER_LOCATION, ChestBargeModel::createBodyLayer);
        event.registerLayerDefinition(ChunkLoaderBargeModel.LAYER_LOCATION, ChunkLoaderBargeModel::createBodyLayer);
        event.registerLayerDefinition(EnergyTugModel.LAYER_LOCATION, EnergyTugModel::createBodyLayer);
        event.registerLayerDefinition(FishingBargeDeployedModel.LAYER_LOCATION, FishingBargeDeployedModel::createBodyLayer);
        event.registerLayerDefinition(FishingBargeModel.LAYER_LOCATION, FishingBargeModel::createBodyLayer);
        event.registerLayerDefinition(FishingBargeTransitionModel.LAYER_LOCATION, FishingBargeTransitionModel::createBodyLayer);
        event.registerLayerDefinition(FluidTankBargeModel.LAYER_LOCATION, FluidTankBargeModel::createBodyLayer);
        event.registerLayerDefinition(SeaterBargeModel.LAYER_LOCATION, SeaterBargeModel::createBodyLayer);
        event.registerLayerDefinition(SteamTugModel.LAYER_LOCATION, SteamTugModel::createBodyLayer);
        event.registerLayerDefinition(SteamLocomotiveModel.LAYER_LOCATION, SteamLocomotiveModel::createBodyLayer);
        event.registerLayerDefinition(EnergyLocomotiveModel.LAYER_LOCATION, EnergyLocomotiveModel::createBodyLayer);
        event.registerLayerDefinition(ChestCarModel.LAYER_LOCATION, ChestCarModel::createBodyLayer);

    }
}
