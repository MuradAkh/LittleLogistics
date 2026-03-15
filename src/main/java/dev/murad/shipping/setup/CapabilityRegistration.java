package dev.murad.shipping.setup;

import dev.murad.shipping.ShippingMod;
import dev.murad.shipping.block.energy.VesselChargerTileEntity;
import dev.murad.shipping.block.fluid.FluidHopperTileEntity;
import dev.murad.shipping.entity.custom.vessel.tug.SteamTugEntity;
import dev.murad.shipping.entity.custom.vessel.tug.EnergyTugEntity;
import dev.murad.shipping.entity.custom.vessel.barge.FluidTankBargeEntity;
import dev.murad.shipping.entity.custom.train.locomotive.SteamLocomotiveEntity;
import dev.murad.shipping.entity.custom.train.locomotive.EnergyLocomotiveEntity;
import dev.murad.shipping.entity.custom.train.wagon.ChestCarEntity;
import dev.murad.shipping.entity.custom.train.wagon.FluidTankCarEntity;
import dev.murad.shipping.capability.StallingCapability;
import dev.murad.shipping.item.creative.CreativeCapacitor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = ShippingMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class CapabilityRegistration {

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {

        // === Entity capabilities: ITEM_HANDLER ===

        // SteamTugEntity -> fuel item handler
        event.registerEntity(
            Capabilities.ItemHandler.ENTITY,
            ModEntityTypes.STEAM_TUG.get(),
            (entity, ctx) -> entity.getFuelItemHandler()
        );

        // EnergyTugEntity -> energy item handler
        event.registerEntity(
            Capabilities.ItemHandler.ENTITY,
            ModEntityTypes.ENERGY_TUG.get(),
            (entity, ctx) -> entity.getItemHandler()
        );

        // SteamLocomotiveEntity -> fuel item handler
        // Note: STEAM_LOCOMOTIVE is typed as EntityType<AbstractLocomotiveEntity>, cast needed
        event.registerEntity(
            Capabilities.ItemHandler.ENTITY,
            ModEntityTypes.STEAM_LOCOMOTIVE.get(),
            (entity, ctx) -> ((SteamLocomotiveEntity) entity).getFuelItemHandler()
        );

        // EnergyLocomotiveEntity -> energy item handler
        // Note: ENERGY_LOCOMOTIVE is typed as EntityType<AbstractLocomotiveEntity>, cast needed
        event.registerEntity(
            Capabilities.ItemHandler.ENTITY,
            ModEntityTypes.ENERGY_LOCOMOTIVE.get(),
            (entity, ctx) -> ((EnergyLocomotiveEntity) entity).getEnergyItemHandler()
        );

        // ChestCarEntity -> chest item handler
        event.registerEntity(
            Capabilities.ItemHandler.ENTITY,
            ModEntityTypes.CHEST_CAR.get(),
            (entity, ctx) -> entity.getRawHandler()
        );

        // BarrelCarEntity -> chest item handler (same class as ChestCarEntity)
        event.registerEntity(
            Capabilities.ItemHandler.ENTITY,
            ModEntityTypes.BARREL_CAR.get(),
            (entity, ctx) -> entity.getRawHandler()
        );

        // === Entity capabilities: ENERGY ===

        // EnergyTugEntity -> internal battery
        event.registerEntity(
            Capabilities.EnergyStorage.ENTITY,
            ModEntityTypes.ENERGY_TUG.get(),
            (entity, ctx) -> entity.getInternalBattery()
        );

        // EnergyLocomotiveEntity -> internal battery
        // Note: ENERGY_LOCOMOTIVE is typed as EntityType<AbstractLocomotiveEntity>, cast needed
        event.registerEntity(
            Capabilities.EnergyStorage.ENTITY,
            ModEntityTypes.ENERGY_LOCOMOTIVE.get(),
            (entity, ctx) -> ((EnergyLocomotiveEntity) entity).getInternalBattery()
        );

        // === Entity capabilities: FLUID_HANDLER ===

        // FluidTankBargeEntity -> fluid tank
        event.registerEntity(
            Capabilities.FluidHandler.ENTITY,
            ModEntityTypes.FLUID_TANK_BARGE.get(),
            (entity, ctx) -> entity.getTank()
        );

        // FluidTankCarEntity -> fluid tank
        event.registerEntity(
            Capabilities.FluidHandler.ENTITY,
            ModEntityTypes.FLUID_CAR.get(),
            (entity, ctx) -> entity.getTank()
        );

        // === BlockEntity capabilities ===

        // VesselChargerTileEntity -> energy storage
        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK,
            ModTileEntitiesTypes.VESSEL_CHARGER.get(),
            (blockEntity, direction) -> blockEntity.getInternalBattery()
        );

        // FluidHopperTileEntity -> fluid handler
        event.registerBlockEntity(
            Capabilities.FluidHandler.BLOCK,
            ModTileEntitiesTypes.FLUID_HOPPER.get(),
            (blockEntity, direction) -> blockEntity.getTank()
        );

        // === Item capabilities ===

        // CreativeCapacitor -> infinite energy
        event.registerItem(
            Capabilities.EnergyStorage.ITEM,
            (stack, ctx) -> new CreativeCapacitor.CreativeEnergyStorage(),
            ModItems.CREATIVE_CAPACITOR.get()
        );

        // === Entity capabilities: STALLING ===

        // Head vehicles — tugs
        event.registerEntity(StallingCapability.ENTITY_CAPABILITY, ModEntityTypes.STEAM_TUG.get(), (entity, ctx) -> entity);
        event.registerEntity(StallingCapability.ENTITY_CAPABILITY, ModEntityTypes.ENERGY_TUG.get(), (entity, ctx) -> entity);

        // Head vehicles — locomotives (typed as EntityType<AbstractLocomotiveEntity>)
        event.registerEntity(StallingCapability.ENTITY_CAPABILITY, ModEntityTypes.STEAM_LOCOMOTIVE.get(), (entity, ctx) -> (StallingCapability) entity);
        event.registerEntity(StallingCapability.ENTITY_CAPABILITY, ModEntityTypes.ENERGY_LOCOMOTIVE.get(), (entity, ctx) -> (StallingCapability) entity);

        // Tail vehicles — barges
        event.registerEntity(StallingCapability.ENTITY_CAPABILITY, ModEntityTypes.CHEST_BARGE.get(), (entity, ctx) -> entity);
        event.registerEntity(StallingCapability.ENTITY_CAPABILITY, ModEntityTypes.BARREL_BARGE.get(), (entity, ctx) -> entity);
        event.registerEntity(StallingCapability.ENTITY_CAPABILITY, ModEntityTypes.CHUNK_LOADER_BARGE.get(), (entity, ctx) -> entity);
        event.registerEntity(StallingCapability.ENTITY_CAPABILITY, ModEntityTypes.FISHING_BARGE.get(), (entity, ctx) -> entity);
        event.registerEntity(StallingCapability.ENTITY_CAPABILITY, ModEntityTypes.FLUID_TANK_BARGE.get(), (entity, ctx) -> entity);
        event.registerEntity(StallingCapability.ENTITY_CAPABILITY, ModEntityTypes.SEATER_BARGE.get(), (entity, ctx) -> entity);
        event.registerEntity(StallingCapability.ENTITY_CAPABILITY, ModEntityTypes.VACUUM_BARGE.get(), (entity, ctx) -> entity);

        // Tail vehicles — wagons
        event.registerEntity(StallingCapability.ENTITY_CAPABILITY, ModEntityTypes.CHEST_CAR.get(), (entity, ctx) -> entity);
        event.registerEntity(StallingCapability.ENTITY_CAPABILITY, ModEntityTypes.BARREL_CAR.get(), (entity, ctx) -> entity);
        event.registerEntity(StallingCapability.ENTITY_CAPABILITY, ModEntityTypes.SEATER_CAR.get(), (entity, ctx) -> entity);
        event.registerEntity(StallingCapability.ENTITY_CAPABILITY, ModEntityTypes.FLUID_CAR.get(), (entity, ctx) -> entity);
        event.registerEntity(StallingCapability.ENTITY_CAPABILITY, ModEntityTypes.CHUNK_LOADER_CAR.get(), (entity, ctx) -> entity);
    }
}
