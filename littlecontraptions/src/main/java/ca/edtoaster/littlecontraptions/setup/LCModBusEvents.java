package ca.edtoaster.littlecontraptions.setup;

import ca.edtoaster.littlecontraptions.LCMod;
import ca.edtoaster.littlecontraptions.entity.ContraptionStorageHandler;
import dev.murad.shipping.entity.custom.vessel.VesselEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@EventBusSubscriber(modid = LCMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class LCModBusEvents {

    @SubscribeEvent
    public static void addEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(LCEntityTypes.CONTRAPTION_BARGE.get(), VesselEntity.setCustomAttributes().build());
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // ContraptionBargeEntity: no existing item/fluid capability in the main mod.
        event.registerEntity(Capabilities.ItemHandler.ENTITY,
                LCEntityTypes.CONTRAPTION_BARGE.get(),
                (entity, ctx) -> new ContraptionStorageHandler.ItemHandler(entity));
        event.registerEntity(Capabilities.FluidHandler.ENTITY,
                LCEntityTypes.CONTRAPTION_BARGE.get(),
                (entity, ctx) -> new ContraptionStorageHandler.FluidHandler(entity));

        // SeaterCarEntity: the only wagon that allows passengers (canAddPassenger returns true),
        // so the only wagon that can actually host an OrientedContraptionEntity. No conflicting
        // capability registrations exist for this type in the main mod.
        event.registerEntity(Capabilities.ItemHandler.ENTITY,
                dev.murad.shipping.setup.ModEntityTypes.SEATER_CAR.get(),
                (entity, ctx) -> new ContraptionStorageHandler.ItemHandler(entity));
        event.registerEntity(Capabilities.FluidHandler.ENTITY,
                dev.murad.shipping.setup.ModEntityTypes.SEATER_CAR.get(),
                (entity, ctx) -> new ContraptionStorageHandler.FluidHandler(entity));

        // Note: CHEST_CAR, BARREL_CAR, FLUID_CAR already have item/fluid capabilities
        // registered in the main mod for their own built-in inventories. Those types
        // also cannot host contraption passengers (canAddPassenger returns false).
        // No energy capability: Create 6.0 has no aggregate energy storage at the
        // contraption level.
    }
}
