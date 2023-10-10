package dev.murad.shipping.event;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.block.fluid.render.FluidHopperTileEntityRenderer;
import dev.murad.shipping.entity.custom.vessel.barge.FishingBargeEntity;
import dev.murad.shipping.entity.models.train.*;
import dev.murad.shipping.entity.models.vessel.*;
import dev.murad.shipping.entity.models.vessel.insert.*;
import dev.murad.shipping.entity.render.barge.FishingBargeRenderer;
import dev.murad.shipping.entity.render.barge.FluidTankBargeRenderer;
import dev.murad.shipping.entity.render.barge.MultipartVesselRenderer;
import dev.murad.shipping.entity.render.train.FluidTankCarRenderer;
import dev.murad.shipping.entity.render.train.TrainCarRenderer;
import dev.murad.shipping.entity.render.barge.StaticVesselRenderer;
import dev.murad.shipping.setup.ModBlocks;
import dev.murad.shipping.setup.ModEntityTypes;
import dev.murad.shipping.setup.ModItems;
import dev.murad.shipping.setup.ModTileEntitiesTypes;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;


/**
 * Mod-specific event bus
 */
@Mod.EventBusSubscriber(modid = ShippingMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModClientEventHandler {

    @SubscribeEvent
    public static void onRenderTypeSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.FLUID_HOPPER.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.VESSEL_CHARGER.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.JUNCTION_RAIL.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.SWITCH_RAIL.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.AUTOMATIC_SWITCH_RAIL.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.TEE_JUNCTION_RAIL.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.AUTOMATIC_TEE_JUNCTION_RAIL.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.CAR_DOCK_RAIL.get(), RenderType.cutoutMipped());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.LOCOMOTIVE_DOCK_RAIL.get(), RenderType.cutoutMipped());
        });
    }

    @SubscribeEvent
    public static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Barges
        event.registerEntityRenderer(ModEntityTypes.CHEST_BARGE.get(),
                (ctx) -> new MultipartVesselRenderer.Builder<>(ctx)
                        .baseModel(BaseBargeModel::new, BaseBargeModel.CLOSED_LOCATION,
                                ShippingMod.entityTexture("barge/base.png"))
                        .insertModel(CubeInsertBargeModel::new, CubeInsertBargeModel.LAYER_LOCATION,
                                ShippingMod.entityTexture("barge/chest_insert.png"))
                        .trimModel(TrimBargeModel::new, TrimBargeModel.CLOSED_LOCATION,
                                ShippingMod.entityTexture("barge/trim.png"))
                        .build());

        event.registerEntityRenderer(ModEntityTypes.BARREL_BARGE.get(),
                (ctx) -> new MultipartVesselRenderer.Builder<>(ctx)
                        .baseModel(BaseBargeModel::new, BaseBargeModel.CLOSED_LOCATION,
                                ShippingMod.entityTexture("barge/base.png"))
                        .insertModel(CubeInsertBargeModel::new, CubeInsertBargeModel.LAYER_LOCATION,
                                ShippingMod.entityTexture("barge/barrel_insert.png"))
                        .trimModel(TrimBargeModel::new, TrimBargeModel.CLOSED_LOCATION,
                                ShippingMod.entityTexture("barge/trim.png"))
                        .build());

        event.registerEntityRenderer(ModEntityTypes.CHUNK_LOADER_BARGE.get(),
                (ctx) -> new MultipartVesselRenderer.Builder<>(ctx)
                        .baseModel(BaseBargeModel::new, BaseBargeModel.CLOSED_LOCATION,
                                ShippingMod.entityTexture("barge/base.png"))
                        .insertModel(RingsInsertBargeModel::new, RingsInsertBargeModel.LAYER_LOCATION,
                                ShippingMod.entityTexture("barge/chunk_loader_insert.png"))
                        .trimModel(TrimBargeModel::new, TrimBargeModel.CLOSED_LOCATION,
                                ShippingMod.entityTexture("barge/trim.png"))
                        .build());

        event.registerEntityRenderer(ModEntityTypes.SEATER_BARGE.get(),
                (ctx) -> new MultipartVesselRenderer.Builder<>(ctx)
                        .baseModel(BaseBargeModel::new, BaseBargeModel.OPEN_FRONT_LOCATION,
                                ShippingMod.entityTexture("barge/base.png"))
                        .insertModel(SeaterInsertBargeModel::new, SeaterInsertBargeModel.LAYER_LOCATION,
                                ShippingMod.entityTexture("barge/seater_insert.png"))
                        .trimModel(TrimBargeModel::new, TrimBargeModel.OPEN_FRONT_LOCATION,
                                ShippingMod.entityTexture("barge/trim.png"))
                        .build());

        event.registerEntityRenderer(ModEntityTypes.VACUUM_BARGE.get(),
                (ctx) -> new MultipartVesselRenderer.Builder<>(ctx)
                        .baseModel(BaseBargeModel::new, BaseBargeModel.CLOSED_LOCATION,
                                ShippingMod.entityTexture("barge/base.png"))
                        .insertModel(RingsInsertBargeModel::new, RingsInsertBargeModel.LAYER_LOCATION,
                                ShippingMod.entityTexture("barge/vacuum_insert.png"))
                        .trimModel(TrimBargeModel::new, TrimBargeModel.CLOSED_LOCATION,
                                ShippingMod.entityTexture("barge/trim.png"))
                        .build());

        event.registerEntityRenderer(ModEntityTypes.FLUID_TANK_BARGE.get(),
                (ctx) -> new FluidTankBargeRenderer.Builder<>(ctx)
                        .baseModel(BaseBargeModel::new, BaseBargeModel.CLOSED_LOCATION,
                                ShippingMod.entityTexture("barge/base.png"))
                        .insertModel(FluidTankInsertBargeModel::new, FluidTankInsertBargeModel.LAYER_LOCATION,
                                ShippingMod.entityTexture("barge/fluid_tank_insert.png"))
                        .trimModel(TrimBargeModel::new, TrimBargeModel.CLOSED_LOCATION,
                                ShippingMod.entityTexture("barge/trim.png"))
                        .build());

        event.registerEntityRenderer(ModEntityTypes.FISHING_BARGE.get(),
                (ctx) -> new FishingBargeRenderer.Builder<>(ctx)
                        .transitionInsertModel(FishingInsertBargeModel::new, FishingInsertBargeModel.TRANSITION_LOCATION,
                                ShippingMod.entityTexture("barge/fishing_insert.png"))
                        .deployedInsertModel(FishingInsertBargeModel::new, FishingInsertBargeModel.DEPLOYED_LOCATION,
                                ShippingMod.entityTexture("barge/fishing_insert.png"))
                        .baseModel(BaseBargeModel::new, BaseBargeModel.OPEN_SIDES_LOCATION,
                                ShippingMod.entityTexture("barge/base.png"))
                        .insertModel(FishingInsertBargeModel::new, FishingInsertBargeModel.STASHED_LOCATION,
                                ShippingMod.entityTexture("barge/fishing_insert.png"))
                        .trimModel(TrimBargeModel::new, TrimBargeModel.OPEN_SIDES_LOCATION,
                                ShippingMod.entityTexture("barge/trim.png"))
                        .build());

        // Tugs
//        event.registerEntityRenderer(ModEntityTypes.ENERGY_TUG.get(),
//                (ctx) -> new StaticVesselRenderer<>(ctx, EnergyTugModel::new, EnergyTugModel.LAYER_LOCATION,
//                        new ResourceLocation(ShippingMod.MOD_ID, "textures/entity/energy_tug.png")) {
//                    // todo: fix in models itself
//                    @Override
//                    protected double getModelYoffset() {
//                        return 1.55D;
//                    }
//
//                    @Override
//                    protected float getModelYrot() {
//                        return 0.0F;
//                    }
//                });


//        event.registerEntityRenderer(ModEntityTypes.STEAM_TUG.get(),
//                (ctx) -> new StaticVesselRenderer<>(ctx, SteamTugModel::new, SteamTugModel.LAYER_LOCATION,
//                        new ResourceLocation(ShippingMod.MOD_ID, "textures/entity/tug.png")) {
//                    // todo: fix in models itself
//                    @Override
//                    protected double getModelYoffset() {
//                        return 1.45D;
//                    }
//
//                    @Override
//                    protected float getModelYrot() {
//                        return 0;
//                    }
//                });

        event.registerEntityRenderer(ModEntityTypes.ENERGY_TUG.get(),
                (ctx) -> new MultipartVesselRenderer.Builder<>(ctx)
                        .baseModel(EnergyTugModel::new, EnergyTugModel.LAYER_LOCATION,
                                ShippingMod.entityTexture("barge/energy_tug_base.png"))
                        .emptyInsert()
                        .trimModel(EnergyTugModel::new, EnergyTugModel.LAYER_LOCATION,
                                ShippingMod.entityTexture("barge/energy_tug_trim.png"))
                        .build()
                        // TODO: this is a hack
                        .derotate());

        event.registerEntityRenderer(ModEntityTypes.STEAM_TUG.get(),
                (ctx) -> new MultipartVesselRenderer.Builder<>(ctx)
                        .baseModel(SteamTugModel::new, SteamTugModel.LAYER_LOCATION,
                                ShippingMod.entityTexture("barge/steam_tug_base.png"))
                        .emptyInsert()
                        .trimModel(SteamTugModel::new, SteamTugModel.LAYER_LOCATION,
                                ShippingMod.entityTexture("barge/steam_tug_trim.png"))
                        .build()
                        .derotate());

        event.registerEntityRenderer(ModEntityTypes.STEAM_LOCOMOTIVE.get(), ctx -> new TrainCarRenderer<>(ctx,
                SteamLocomotiveModel::new,
                SteamLocomotiveModel.LAYER_LOCATION,
                "textures/entity/steam_locomotive.png"));
        event.registerEntityRenderer(ModEntityTypes.ENERGY_LOCOMOTIVE.get(), ctx -> new TrainCarRenderer<>(ctx,
                EnergyLocomotiveModel::new,
                EnergyLocomotiveModel.LAYER_LOCATION,
                "textures/entity/energy_locomotive.png"));
        event.registerEntityRenderer(ModEntityTypes.CHEST_CAR.get(), ctx -> new TrainCarRenderer<>(ctx,
                ChestCarModel::new,
                ChestCarModel.LAYER_LOCATION,
                "textures/entity/chest_car.png"));
        event.registerEntityRenderer(ModEntityTypes.FLUID_CAR.get(), ctx -> new FluidTankCarRenderer(ctx,
                FluidTankCarModel::new,
                FluidTankCarModel.LAYER_LOCATION,
                "textures/entity/fluid_car.png"));
        event.registerEntityRenderer(ModEntityTypes.CHUNK_LOADER_CAR.get(), ctx -> new TrainCarRenderer<>(ctx,
                ChunkLoaderCarModel::new,
                ChunkLoaderCarModel.LAYER_LOCATION,
                "textures/entity/chunk_loader_car.png"));
        event.registerEntityRenderer(ModEntityTypes.SEATER_CAR.get(), ctx -> new TrainCarRenderer<>(ctx,
                SeaterCarModel::new,
                SeaterCarModel.LAYER_LOCATION,
                "textures/entity/chest_car.png"));

        event.registerBlockEntityRenderer(ModTileEntitiesTypes.FLUID_HOPPER.get(), FluidHopperTileEntityRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ChainExtendedModel.LAYER_LOCATION, ChainExtendedModel::createBodyLayer);
        event.registerLayerDefinition(ChainModel.LAYER_LOCATION, ChainModel::createBodyLayer);

        event.registerLayerDefinition(EmptyModel.LAYER_LOCATION, EmptyModel::createBodyLayer);

        event.registerLayerDefinition(BaseBargeModel.CLOSED_LOCATION, () -> BaseBargeModel.createBodyLayer(true, true));
        event.registerLayerDefinition(BaseBargeModel.OPEN_FRONT_LOCATION, () -> BaseBargeModel.createBodyLayer(false, true));
        event.registerLayerDefinition(BaseBargeModel.OPEN_SIDES_LOCATION, () -> BaseBargeModel.createBodyLayer(true, false));

        event.registerLayerDefinition(TrimBargeModel.CLOSED_LOCATION, () -> TrimBargeModel.createBodyLayer(true, true));
        event.registerLayerDefinition(TrimBargeModel.OPEN_FRONT_LOCATION, () -> TrimBargeModel.createBodyLayer(false, true));
        event.registerLayerDefinition(TrimBargeModel.OPEN_SIDES_LOCATION, () -> TrimBargeModel.createBodyLayer(true, false));

        event.registerLayerDefinition(CubeInsertBargeModel.LAYER_LOCATION, CubeInsertBargeModel::createBodyLayer);
        event.registerLayerDefinition(RingsInsertBargeModel.LAYER_LOCATION, RingsInsertBargeModel::createBodyLayer);
        event.registerLayerDefinition(SeaterInsertBargeModel.LAYER_LOCATION, SeaterInsertBargeModel::createBodyLayer);
        event.registerLayerDefinition(FluidTankInsertBargeModel.LAYER_LOCATION, FluidTankInsertBargeModel::createBodyLayer);

        event.registerLayerDefinition(FishingInsertBargeModel.STASHED_LOCATION, () -> FishingInsertBargeModel.createBodyLayer(FishingBargeEntity.Status.STASHED));
        event.registerLayerDefinition(FishingInsertBargeModel.TRANSITION_LOCATION, () -> FishingInsertBargeModel.createBodyLayer(FishingBargeEntity.Status.TRANSITION));
        event.registerLayerDefinition(FishingInsertBargeModel.DEPLOYED_LOCATION, () -> FishingInsertBargeModel.createBodyLayer(FishingBargeEntity.Status.DEPLOYED));

        event.registerLayerDefinition(EnergyTugModel.LAYER_LOCATION, EnergyTugModel::createBodyLayer);
        event.registerLayerDefinition(SteamTugModel.LAYER_LOCATION, SteamTugModel::createBodyLayer);

        event.registerLayerDefinition(SteamLocomotiveModel.LAYER_LOCATION, SteamLocomotiveModel::createBodyLayer);
        event.registerLayerDefinition(EnergyLocomotiveModel.LAYER_LOCATION, EnergyLocomotiveModel::createBodyLayer);
        event.registerLayerDefinition(ChestCarModel.LAYER_LOCATION, ChestCarModel::createBodyLayer);
        event.registerLayerDefinition(FluidTankCarModel.LAYER_LOCATION, FluidTankCarModel::createBodyLayer);
        event.registerLayerDefinition(ChunkLoaderCarModel.LAYER_LOCATION, ChunkLoaderCarModel::createBodyLayer);
        event.registerLayerDefinition(SeaterCarModel.LAYER_LOCATION, SeaterCarModel::createBodyLayer);
    }

    /**
     * Subscribe to event when building each creative mode tab. Items are added to tabs here.
     * @param event The creative tab currently being built
     */
    @SubscribeEvent
    public static void buildTabContents(BuildCreativeModeTabContentsEvent event) {
        ModBlocks.buildCreativeTab(event);
        ModItems.buildCreativeTab(event);
    }
}
