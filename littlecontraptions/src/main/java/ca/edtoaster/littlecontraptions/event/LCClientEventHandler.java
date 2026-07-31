package ca.edtoaster.littlecontraptions.event;

import ca.edtoaster.littlecontraptions.LCMod;
import ca.edtoaster.littlecontraptions.setup.LCBlocks;
import ca.edtoaster.littlecontraptions.setup.LCEntityTypes;
import ca.edtoaster.littlecontraptions.setup.LCItems;
import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.entity.models.insert.CubeInsertBargeModel;
import dev.murad.shipping.entity.models.vessel.base.BaseBargeModel;
import dev.murad.shipping.entity.models.vessel.base.TrimBargeModel;
import dev.murad.shipping.entity.render.barge.MultipartVesselRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@EventBusSubscriber(modid = LCMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class LCClientEventHandler {

    @SubscribeEvent
    public static void onRenderTypeSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemBlockRenderTypes.setRenderLayer(LCBlocks.BARGE_ASSEMBLER.get(), RenderType.cutoutMipped()));
    }

    @SubscribeEvent
    public static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // The contraption barge is a VesselEntity (not an AbstractContraptionEntity), so it is
        // rendered with Little Logistics' MultipartVesselRenderer. The mounted contraption itself
        // is rendered by Create's OrientedContraptionEntityRenderer as a passenger.
        event.registerEntityRenderer(LCEntityTypes.CONTRAPTION_BARGE.get(),
                (ctx) -> new MultipartVesselRenderer.Builder<>(ctx)
                        .baseModel(BaseBargeModel::new, BaseBargeModel.CLOSED_LOCATION,
                                ShippingMod.entityTexture("barge/base.png"))
                        .insertModel(CubeInsertBargeModel::new, CubeInsertBargeModel.LAYER_LOCATION,
                                ResourceLocation.fromNamespaceAndPath(LCMod.MOD_ID, "textures/entity/contraption_barge.png"))
                        .trimModel(TrimBargeModel::new, TrimBargeModel.CLOSED_LOCATION,
                                ShippingMod.entityTexture("barge/trim.png"))
                        .build());
    }

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
    }

    /**
     * Subscribe to event when building each creative mode tab. Items are added to tabs here.
     * @param event The creative tab currently being built
     */
    @SubscribeEvent
    public static void buildTabContents(BuildCreativeModeTabContentsEvent event) {
        LCItems.buildTabContents(event);
    }
}
