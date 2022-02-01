package dev.murad.shipping.event;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.setup.ModBlocks;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
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
    public static final ResourceLocation EMPTY_ATLAS_LOC = PlayerContainer.BLOCK_ATLAS;

    @SubscribeEvent
    public static void onTextureStitchEventPre(TextureStitchEvent.Pre event) {
        if (event.getMap().location() != EMPTY_ATLAS_LOC) return;
        event.addSprite(EMPTY_TUG_ROUTE);
        event.addSprite(EMPTY_ENERGY);
    }

    @SubscribeEvent
    public static void onRenderTypeSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            RenderTypeLookup.setRenderLayer(ModBlocks.FLUID_HOPPER.get(), RenderType.cutoutMipped());
            RenderTypeLookup.setRenderLayer(ModBlocks.VESSEL_CHARGER.get(), RenderType.cutoutMipped());
        });
    }
}
